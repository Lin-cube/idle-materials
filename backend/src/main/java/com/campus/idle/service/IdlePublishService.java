package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.BusinessException;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.UserContext;
import com.campus.idle.entity.IdlePublish;
import com.campus.idle.entity.Item;
import com.campus.idle.entity.Notification;
import com.campus.idle.entity.User;
import com.campus.idle.mapper.IdlePublishMapper;
import com.campus.idle.mapper.ItemMapper;
import com.campus.idle.mapper.NotificationMapper;
import com.campus.idle.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 闲置上架服务
 * 流程: 各部门提交闲置物资 -> 仓库老师审核 -> 通过后自动生成物资进入物资大厅
 */
@Service
@RequiredArgsConstructor
public class IdlePublishService {

    private final IdlePublishMapper publishMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;

    /** 提交闲置物资上架申请(任何登录用户都可为本部门提交) */
    @Transactional
    public void submit(IdlePublish publish) {
        // ===== 必填校验: 名称/联系人/联系电话/图片 =====
        if (!org.springframework.util.StringUtils.hasText(publish.getName())) {
            throw new BusinessException("物资名称为必填项");
        }
        if (!org.springframework.util.StringUtils.hasText(publish.getContactName())) {
            throw new BusinessException("联系人为必填项");
        }
        if (!org.springframework.util.StringUtils.hasText(publish.getContactPhone())) {
            throw new BusinessException("联系电话为必填项");
        }
        // 手机号格式校验: 1开头的11位数字
        if (!publish.getContactPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("联系电话格式不正确, 请填写11位手机号");
        }
        if (!org.springframework.util.StringUtils.hasText(publish.getImage())) {
            throw new BusinessException("物资图片为必填项, 请先上传照片");
        }
        publish.setPublisherId(UserContext.getUserId());
        publish.setStatus("PENDING");
        publishMapper.insert(publish);
        // 通知仓库老师审核
        User wh = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "WAREHOUSE").last("LIMIT 1"));
        if (wh != null) {
            Notification n = new Notification();
            n.setUserId(wh.getId());
            n.setTitle("有新的闲置物资待审核");
            n.setContent("「" + publish.getName() + "」已提交上架申请, 请审核");
            n.setType("APPROVE");
            n.setRelatedId(publish.getId());
            n.setIsRead(0);
            notificationMapper.insert(n);
        }
    }

    /** 仓库老师审核 */
    @Transactional
    public void review(Long id, boolean pass, String remark) {
        if (!"WAREHOUSE".equals(UserContext.get().getRole())) {
            throw new BusinessException("只有仓库管理员才能审核");
        }
        IdlePublish publish = publishMapper.selectById(id);
        if (publish == null || !"PENDING".equals(publish.getStatus())) {
            throw new BusinessException("申请不存在或已审核");
        }
        publish.setReviewerId(UserContext.getUserId());
        publish.setReviewRemark(remark);
        publish.setReviewTime(LocalDateTime.now());
        if (pass) {
            // 审核通过 -> 自动生成物资进入物资大厅
            User publisher = userMapper.selectById(publish.getPublisherId());
            Item item = new Item();
            item.setAssetNo("IDLE" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase());   // 闲置物资自动编号
            item.setName(publish.getName());
            item.setCategory(publish.getCategory());
            item.setPrice(java.math.BigDecimal.ZERO);
            item.setUnit("件");
            item.setImage(publish.getImage());
            item.setDept(publisher == null ? null : publisher.getDept());   // 提交人所属部门
            item.setLocation(publish.getLocation());
            // 联系人/联系电话: 优先用表单填写的联系人, 没有则回退为提交人
            item.setOwnerName(org.springframework.util.StringUtils.hasText(publish.getContactName())
                    ? publish.getContactName()
                    : (publisher == null ? null : publisher.getRealName()));
            item.setOwnerPhone(publish.getContactPhone());
            item.setItemType("IDLE");
            item.setDescription(publish.getDescription());
            item.setStatus(0);
            itemMapper.insert(item);

            publish.setItemId(item.getId());
            publish.setStatus("PASS");
        } else {
            publish.setStatus("REJECT");
        }
        publishMapper.updateById(publish);
        // 通知提交人审核结果
        notify(publish.getPublisherId(), "闲置物资审核" + (pass ? "通过" : "未通过"),
                "您提交的「" + publish.getName() + "」" + (pass ? "已上架到物资大厅" : "未通过审核: " + remark),
                "APPROVE", id);
    }

    /** 待审核列表(仓库视角) */
    public Page<IdlePublish> pendingList(PageQuery query) {
        return publishMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<IdlePublish>()
                        .eq(IdlePublish::getStatus, "PENDING")
                        .orderByDesc(IdlePublish::getCreateTime));
    }

    /** 我的提交记录 */
    public Page<IdlePublish> myList(PageQuery query) {
        return publishMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<IdlePublish>()
                        .eq(IdlePublish::getPublisherId, UserContext.getUserId())
                        .orderByDesc(IdlePublish::getCreateTime));
    }

    /** 发送通知 */
    private void notify(Long userId, String title, String content, String type, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }
}
