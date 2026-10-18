package com.campus.idle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.Result;
import com.campus.idle.entity.Notification;
import com.campus.idle.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息通知接口: 列表 / 未读数 / 已读
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 我的消息(分页) */
    @GetMapping
    public Result<Page<Notification>> my(PageQuery query) {
        return Result.ok(notificationService.myList(query));
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(notificationService.unreadCount());
    }

    /** 标记单条已读 */
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    /** 全部标记已读 */
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        notificationService.markAllRead();
        return Result.ok();
    }
}
