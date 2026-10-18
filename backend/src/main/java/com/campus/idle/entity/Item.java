package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物资实体, 对应表 item
 * 字段来源: 资产信息查询.xlsx
 */
@Data
@TableName("item")
public class Item implements Serializable {

    /** 物资ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产编号 */
    private String assetNo;

    /** 物资名称 */
    private String name;

    /** 物资分类(家具/电子设备/搬运设备...) */
    private String category;

    /** 单价(元) */
    private BigDecimal price;

    /** 计量单位(台/张/辆...) */
    private String unit;

    /** 型号 */
    private String model;

    /** 规格 */
    private String spec;

    /** 物资图片URL */
    private String image;

    /** 使用/管理部门 */
    private String dept;

    /** 存放地点 */
    private String location;

    /** 使用人 */
    private String ownerName;

    /** 使用人电话 */
    private String ownerPhone;

    /** 类型: ASSET在管资产 / IDLE闲置物资 */
    private String itemType;

    /** 该物资对应负责老师ID(学生申请时找该老师审批) */
    private Long managerId;

    /** 物资简介 */
    private String description;

    /** 状态: 0在库可借 1已借出 2维修中 3已下架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
