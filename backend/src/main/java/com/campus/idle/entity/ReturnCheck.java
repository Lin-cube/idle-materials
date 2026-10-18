package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 归还核对实体, 对应表 return_check
 */
@Data
@TableName("return_check")
public class ReturnCheck implements Serializable {

    /** 核对ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 借用记录ID */
    private Long recordId;

    /** 归还人ID */
    private Long returnerId;

    /** 归还数量 */
    private Integer returnQty;

    /** 实物拍照URL(多个逗号分隔) */
    private String photoUrls;

    /** 损坏情况: NONE/DAMAGED/LOST */
    private String damage;

    /** 损坏说明 */
    private String damageDesc;

    /** 赔偿金额(元) */
    private BigDecimal compensate;

    /** 是否已赔偿: 0否 1是 */
    private Integer compensated;

    /** 仓库核对人ID */
    private Long checkerId;

    /** 核对备注 */
    private String checkRemark;

    /** 核对时间 */
    private LocalDateTime checkTime;

    /** 状态: PENDING/CONFIRMED */
    private String status;

    /** 发起时间 */
    private LocalDateTime createTime;
}
