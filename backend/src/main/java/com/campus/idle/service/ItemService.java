package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.BusinessException;
import com.campus.idle.common.PageQuery;
import com.campus.idle.entity.Item;
import com.campus.idle.entity.User;
import com.campus.idle.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 物资服务: 物资大厅查询、详情、管理员增删改
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;

    /**
     * 分页查询物资(物资大厅)
     * 支持按名称/资产编号关键字搜索、按分类筛选
     *
     * @param onlyAvailable true=只查在库可借(已借出/维修/下架的自动隐藏, 即"借出即下架")
     *                      false=查全部(物资管理页用)
     */
    public Page<Item> page(PageQuery query, String category, Boolean onlyAvailable) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        // 关键字: 匹配物资名称或资产编号
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Item::getName, query.getKeyword())
                    .or().like(Item::getAssetNo, query.getKeyword()));
        }
        // 分类筛选
        if (StringUtils.hasText(category)) {
            wrapper.eq(Item::getCategory, category);
        }
        // 只显示在库可借的物资(status=0): 借出/维修/下架的自动从大厅下架
        if (Boolean.TRUE.equals(onlyAvailable)) {
            wrapper.eq(Item::getStatus, 0);
        }
        // 按创建时间倒序
        wrapper.orderByDesc(Item::getCreateTime);
        return itemMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    /** 物资详情 */
    public Item detail(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("物资不存在");
        }
        return item;
    }

    /** 新增物资(仓库管理员) */
    public void add(Item item) {
        // 资产编号不能重复
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getAssetNo, item.getAssetNo()));
        if (count > 0) {
            throw new BusinessException("资产编号已存在: " + item.getAssetNo());
        }
        if (item.getStatus() == null) {
            item.setStatus(0);   // 默认在库可借
        }
        itemMapper.insert(item);
    }

    /** 修改物资(仓库管理员) */
    public void update(Item item) {
        if (item.getId() == null || itemMapper.selectById(item.getId()) == null) {
            throw new BusinessException("物资不存在");
        }
        itemMapper.updateById(item);
    }

    /** 删除物资(仓库管理员) */
    public void delete(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("物资不存在");
        }
        if (item.getStatus() == 1) {
            throw new BusinessException("该物资正在借出中, 不能删除");
        }
        itemMapper.deleteById(id);
    }

    /** 分类列表(物资大厅的筛选项, 取现有物资的去重分类) */
    public java.util.List<String> categories() {
        return itemMapper.selectList(null).stream()
                .map(Item::getCategory)
                .distinct()
                .toList();
    }
}
