package com.campus.idle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.Result;
import com.campus.idle.entity.Item;
import com.campus.idle.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物资接口: 物资大厅 / 详情 / 管理员维护
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * 分页查询物资
     * @param available true=物资大厅模式, 只返回在库可借的(借出后自动下架); 不传=返回全部(物资管理用)
     */
    @GetMapping
    public Result<Page<Item>> page(PageQuery query,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) Boolean available) {
        return Result.ok(itemService.page(query, category, available));
    }

    /** 物资详情 */
    @GetMapping("/{id}")
    public Result<Item> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id));
    }

    /** 分类列表(筛选项) */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(itemService.categories());
    }

    /** 新增物资(仓库管理员) */
    @PostMapping
    public Result<Void> add(@RequestBody Item item) {
        itemService.add(item);
        return Result.ok("新增成功", null);
    }

    /** 修改物资(仓库管理员) */
    @PutMapping
    public Result<Void> update(@RequestBody Item item) {
        itemService.update(item);
        return Result.ok("修改成功", null);
    }

    /** 删除物资(仓库管理员) */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return Result.ok("删除成功", null);
    }
}
