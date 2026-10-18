package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借用申请实体, 对应表 borrow_request
 */
@Data
@TableName("borrow_request")
public class BorrowRequest implements Serializable {

    /** 申请ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请单号 BR+时间戳 */
    private String requestNo;

    /** 物资ID */
    private Long itemId;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人类型: STUDENT/TEACHER */
    private String applicantType;

    /** 申请数量 */
    private Integer quantity;

    /** 借用用途 */
    private String purpose;

    /** 借用开始日期 */
    private LocalDate useStart;

    /** 预计归还日期 */
    private LocalDate useEnd;

    /** 第一审批老师ID */
    private Long teacherId;

    /** 老师审批意见 */
    private String teacherRemark;

    /** 老师审批时间 */
    private LocalDateTime teacherTime;

    /** 领导审批ID */
    private Long leaderId;

    /** 领导审批意见 */
    private String leaderRemark;

    /** 领导审批时间 */
    private LocalDateTime leaderTime;

    /** 仓库确认人ID */
    private Long warehouseId;

    /** 仓库确认备注 */
    private String warehouseRemark;

    /** 证件抵押说明 */
    private String depositDesc;

    /** 审批状态 */
    private String status;

    /** 申请时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
