package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.BusinessException;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.UserContext;
import com.campus.idle.entity.*;
import com.campus.idle.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 归还服务: 线上归还 + 仓库核对 + 赔偿登记
 *
 * 流程(对应流程图):
 *   1. 借用人线上发起归还(填归还数量, 与借出数量对应) + 线下实物拍照
 *   2. 仓库老师核对: 数量对上、实物完好 -> 确认归还(库存+1, 记录关闭)
 *   3. 损坏/丢失 -> 按学校规定登记赔偿金额, 赔偿完成才算结束
 */
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnCheckMapper returnCheckMapper;
    private final BorrowRecordMapper recordMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;

    /**
     * 借用人发起归还申请
     */
    @Transactional
    public void apply(Long recordId, Integer returnQty, String photoUrls,
                      String damage, String damageDesc) {
        // 1. 找到借用记录并校验
        BorrowRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (!UserContext.getUserId().equals(record.getBorrowerId())) {
            throw new BusinessException("只有借用人本人才能发起归还");
        }
        if (!"BORROWED".equals(record.getStatus()) && !"OVERDUE".equals(record.getStatus())) {
            throw new BusinessException("该记录不在借出状态, 无需归还");
        }
        // 2. 归还数量必须与借出数量一致(份数要对上)
        if (!record.getQuantity().equals(returnQty)) {
            throw new BusinessException("归还数量(" + returnQty + ")与借出数量(" + record.getQuantity() + ")不一致, 请核对");
        }
        // 3. 是否已有进行中的归还核对
        Long pending = returnCheckMapper.selectCount(new LambdaQueryWrapper<ReturnCheck>()
                .eq(ReturnCheck::getRecordId, recordId)
                .eq(ReturnCheck::getStatus, "PENDING"));
        if (pending > 0) {
            throw new BusinessException("该记录已有待核对的归还申请, 请耐心等待仓库老师确认");
        }
        // 4. 创建归还核对单
        ReturnCheck rc = new ReturnCheck();
        rc.setRecordId(recordId);
        rc.setReturnerId(UserContext.getUserId());
        rc.setReturnQty(returnQty);
        rc.setPhotoUrls(photoUrls);
        rc.setDamage(damage == null ? "NONE" : damage);
        rc.setDamageDesc(damageDesc);
        rc.setCompensate(BigDecimal.ZERO);
        rc.setCompensated(0);
        rc.setStatus("PENDING");
        returnCheckMapper.insert(rc);
        // 5. 通知仓库老师
        User wh = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "WAREHOUSE").last("LIMIT 1"));
        if (wh != null) {
            notify(wh.getId(), "有待核对的归还申请",
                    "单号 " + record.getRequestNo() + " 借用人已发起归还, 请线下核对实物",
                    "RETURN", rc.getId());
        }
    }

    /**
     * 仓库老师核对确认
     * 完好: 直接确认, 库存回库+解冻检查
     * 损坏/丢失: 登记赔偿金额, 赔偿完成后归库
     */
    @Transactional
    public void confirm(Long checkId, String checkRemark, BigDecimal compensate) {
        if (!"WAREHOUSE".equals(UserContext.get().getRole())) {
            throw new BusinessException("只有仓库管理员才能核对归还");
        }
        ReturnCheck rc = returnCheckMapper.selectById(checkId);
        if (rc == null || !"PENDING".equals(rc.getStatus())) {
            throw new BusinessException("核对单不存在或已处理");
        }
        BorrowRecord record = recordMapper.selectById(rc.getRecordId());

        // ---- 情况1: 实物完好 -> 确认归还 ----
        if ("NONE".equals(rc.getDamage())) {
            rc.setStatus("CONFIRMED");
            rc.setCheckerId(UserContext.getUserId());
            rc.setCheckRemark(checkRemark);
            rc.setCheckTime(LocalDateTime.now());
            returnCheckMapper.updateById(rc);

            // 借用记录关闭
            record.setStatus("RETURNED");
            record.setReturnDate(LocalDateTime.now());
            recordMapper.updateById(record);

            // 库存实时更新: 物资回到"在库可借"
            Item item = itemMapper.selectById(record.getItemId());
            item.setStatus(0);
            itemMapper.updateById(item);

            // 归还完成 -> 如果借用人没有其他逾期记录, 自动解冻账号
            unfreezeIfNoOverdue(record.getBorrowerId());
            // 通知借用人归还完成(证件可取回)
            notify(record.getBorrowerId(), "归还核对完成",
                    "单号 " + record.getRequestNo() + " 已确认归还, 请到仓库取回抵押证件",
                    "RETURN", rc.getId());
        } else {
            // ---- 情况2: 损坏/丢失 -> 登记赔偿, 赔偿后归库 ----
            rc.setCompensate(compensate == null ? BigDecimal.ZERO : compensate);
            rc.setCheckRemark(checkRemark);
            rc.setCheckerId(UserContext.getUserId());
            // 仍保持PENDING, 等赔偿完成后再确认; 但先给借用人通知
            returnCheckMapper.updateById(rc);
            notify(record.getBorrowerId(), "归还核对发现异常",
                    "单号 " + record.getRequestNo() + " 核对发现" +
                            ("DAMAGED".equals(rc.getDamage()) ? "损坏" : "丢失") +
                            ", 按学校规定需赔偿 ¥" + compensate + ", 赔偿后请联系仓库老师确认",
                    "RETURN", rc.getId());
        }
    }

    /**
     * 赔偿完成确认: 仓库老师登记已赔偿, 完成归还流程
     */
    @Transactional
    public void confirmCompensated(Long checkId) {
        if (!"WAREHOUSE".equals(UserContext.get().getRole())) {
            throw new BusinessException("只有仓库管理员才能确认赔偿");
        }
        ReturnCheck rc = returnCheckMapper.selectById(checkId);
        if (rc == null || !"PENDING".equals(rc.getStatus())) {
            throw new BusinessException("核对单不存在或已处理");
        }
        // 标记已赔偿并关闭核对单
        rc.setCompensated(1);
        rc.setStatus("CONFIRMED");
        if (rc.getCheckTime() == null) {
            rc.setCheckTime(LocalDateTime.now());
        }
        returnCheckMapper.updateById(rc);

        // 关闭借用记录(损坏/丢失的物资不能再回库, 记录直接关闭)
        BorrowRecord record = recordMapper.selectById(rc.getRecordId());
        record.setStatus("RETURNED");
        record.setReturnDate(LocalDateTime.now());
        recordMapper.updateById(record);

        // 丢失的物资下架; 损坏的标记维修中
        Item item = itemMapper.selectById(record.getItemId());
        if ("LOST".equals(rc.getDamage())) {
            item.setStatus(3);   // 已下架
        } else {
            item.setStatus(2);   // 维修中
        }
        itemMapper.updateById(item);

        unfreezeIfNoOverdue(record.getBorrowerId());
        notify(record.getBorrowerId(), "赔偿流程完成",
                "单号 " + record.getRequestNo() + " 赔偿已确认, 流程结束, 账号已解冻(如无其他逾期)",
                "RETURN", rc.getId());
    }

    /** 待核对的归还单(仓库视角) */
    public Page<ReturnCheck> pendingList(PageQuery query) {
        return returnCheckMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<ReturnCheck>()
                        .eq(ReturnCheck::getStatus, "PENDING")
                        .orderByDesc(ReturnCheck::getCreateTime));
    }

    /** 我的归还记录(借用人视角) */
    public Page<ReturnCheck> myList(PageQuery query) {
        return returnCheckMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<ReturnCheck>()
                        .eq(ReturnCheck::getReturnerId, UserContext.getUserId())
                        .orderByDesc(ReturnCheck::getCreateTime));
    }

    /** 如果用户没有其他逾期未还记录, 解冻账号 */
    private void unfreezeIfNoOverdue(Long userId) {
        Long overdue = recordMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getBorrowerId, userId)
                .eq(BorrowRecord::getStatus, "OVERDUE"));
        if (overdue == 0) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                user.setStatus(0);
                userMapper.updateById(user);
                notify(userId, "账号已解冻", "您的逾期物资已全部处理完毕, 借用资格已恢复", "SYSTEM", userId);
            }
        }
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
