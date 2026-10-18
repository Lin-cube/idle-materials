package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借用记录实体, 对应表 borrow_record
 * 申请领走后生成, 是库存实时更新与逾期提醒的依据
 */
@Data
@TableName("borrow_record")
public class BorrowRecord implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联申请ID */
    private Long requestId;

    /** 申请单号 */
    private String requestNo;

    /** 物资ID */
    private Long itemId;

    /** 借用人ID */
    private Long borrowerId;

    /** 借出数量 */
    private Integer quantity;

    /** 实际领走时间 */
    private LocalDateTime borrowDate;

    /** 应还日期 */
    private LocalDate dueDate;

    /** 实际归还时间 */
    private LocalDateTime returnDate;

    /** 状态: BORROWED/RETURNED/OVERDUE */
    private String status;

    /** 逾期提醒已发送次数 */
    private Integer remindCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
