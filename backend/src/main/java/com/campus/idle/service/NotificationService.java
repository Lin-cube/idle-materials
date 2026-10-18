package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.UserContext;
import com.campus.idle.entity.Notification;
import com.campus.idle.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知服务: 我的消息列表、未读数、标记已读
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /** 我的消息(分页) */
    public Page<Notification> myList(PageQuery query) {
        return notificationMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, UserContext.getUserId())
                        .orderByDesc(Notification::getCreateTime));
    }

    /** 未读消息数量(前端顶部小红点用) */
    public Map<String, Long> unreadCount() {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.getUserId())
                .eq(Notification::getIsRead, 0));
        Map<String, Long> map = new HashMap<>();
        map.put("count", count);
        return map;
    }

    /** 标记单条已读 */
    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n != null && UserContext.getUserId().equals(n.getUserId())) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    /** 全部标记已读 */
    public void markAllRead() {
        Notification patch = new Notification();
        patch.setIsRead(1);
        notificationMapper.update(patch, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.getUserId())
                .eq(Notification::getIsRead, 0));
    }
}
