package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.idle.entity.BorrowRecord;
import com.campus.idle.entity.Notification;
import com.campus.idle.entity.User;
import com.campus.idle.mapper.BorrowRecordMapper;
import com.campus.idle.mapper.NotificationMapper;
import com.campus.idle.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 逾期提醒定时任务(对应流程图功能②)
 *
 * 规则:
 *   1. 到期前3天内未归还 -> 每天发一次"即将到期"提醒
 *   2. 已到期未归还      -> 标记为逾期(OVERDUE) + 冻结账号(不能再借用, 归还后解冻)
 *
 * @EnableScheduling 在启动类所在包被本组件的 @Scheduled 触发, 需在启动类开启
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueTask {

    private final BorrowRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;

    /** 每天早上8点执行(cron: 秒 分 时 日 月 周) */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkOverdue() {
        log.info("开始执行逾期检查任务...");
        LocalDate today = LocalDate.now();

        // ---- 1. 查出所有借出中的记录 ----
        List<BorrowRecord> records = recordMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, "BORROWED"));

        for (BorrowRecord record : records) {
            long daysLeft = ChronoUnit.DAYS.between(today, record.getDueDate());

            if (daysLeft < 0) {
                // ---- 2a. 已逾期: 标记OVERDUE + 冻结账号 ----
                record.setStatus("OVERDUE");
                recordMapper.updateById(record);
                // 冻结借用人账号(归还后自动解冻)
                User user = userMapper.selectById(record.getBorrowerId());
                if (user != null && (user.getStatus() == null || user.getStatus() == 0)) {
                    user.setStatus(1);
                    userMapper.updateById(user);
                }
                // 逾期通知
                if (user != null) {
                    notify(user.getId(), "逾期提醒: 账号已冻结",
                            "您的借用(单号 " + record.getRequestNo() + ")已逾期 "
                                    + Math.abs(daysLeft) + " 天未归还, 账号已被冻结不能再借用, 请尽快归还物资",
                            "OVERDUE", record.getId());
                }
                log.info("记录 {} 已逾期, 借用人 {} 已被冻结", record.getId(), record.getBorrowerId());
            } else if (daysLeft <= 3) {
                // ---- 2b. 3天内到期: 发一次提醒(避免重复, 只在remindCount=0时发) ----
                if (record.getRemindCount() == 0) {
                    record.setRemindCount(1);
                    recordMapper.updateById(record);
                    notify(record.getBorrowerId(), "归还提醒: 物资即将到期",
                            "您的借用(单号 " + record.getRequestNo() + ")将于 " + record.getDueDate()
                                    + " 到期(还有 " + daysLeft + " 天), 请按时归还",
                            "OVERDUE", record.getId());
                    log.info("记录 {} 即将到期, 已发送提醒", record.getId());
                }
            }
        }
        log.info("逾期检查任务执行完毕, 共处理 {} 条借出记录", records.size());
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
