package com.campus.idle.common;

import lombok.Data;

/**
 * 分页查询请求参数
 * 前端传 pageNum(第几页, 从1开始) 和 pageSize(每页条数)
 */
@Data
public class PageQuery {

    /** 第几页, 默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数, 默认10条 */
    private Integer pageSize = 10;

    /** 搜索关键字(物资名称/资产编号等, 可选) */
    private String keyword;
}
