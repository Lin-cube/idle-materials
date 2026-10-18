package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知实体, 对应表 notification
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 类型: SYSTEM/APPROVE/RETURN/OVERDUE */
    private String type;

    /** 关联业务ID */
    private Long relatedId;

    /** 是否已读: 0未读 1已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
