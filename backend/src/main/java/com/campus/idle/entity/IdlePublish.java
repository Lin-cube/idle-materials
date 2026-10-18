package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 闲置上架实体, 对应表 idle_publish
 * 各部门提交闲置物资 -> 仓库老师审核 -> 上架物资大厅
 */
@Data
@TableName("idle_publish")
public class IdlePublish implements Serializable {

    /** 上架ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交人ID */
    private Long publisherId;

    /** 联系人(该闲置物资的对接人姓名) */
    private String contactName;

    /** 联系电话(联系人的手机号, 必填) */
    private String contactPhone;

    /** 物资名称 */
    private String name;

    /** 物资分类 */
    private String category;

    /** 数量 */
    private Integer quantity;

    /** 物资图片URL */
    private String image;

    /** 物资简介 */
    private String description;

    /** 存放地点 */
    private String location;

    /** 审核人(仓库老师)ID */
    private Long reviewerId;

    /** 审核意见 */
    private String reviewRemark;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核通过后生成的物资ID */
    private Long itemId;

    /** 状态: PENDING/PASS/REJECT */
    private String status;

    /** 提交时间 */
    private LocalDateTime createTime;
}
