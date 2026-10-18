package com.campus.idle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.Result;
import com.campus.idle.entity.ReturnCheck;
import com.campus.idle.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 归还接口: 发起归还 / 仓库核对 / 赔偿确认 / 查询
 */
@RestController
@RequestMapping("/api/return")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /** 借用人发起归还(线上 + 线下拍照) */
    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody Map<String, Object> body) {
        Long recordId = Long.valueOf(body.get("recordId").toString());
        Integer returnQty = Integer.valueOf(body.get("returnQty").toString());
        String photoUrls = (String) body.get("photoUrls");
        String damage = (String) body.getOrDefault("damage", "NONE");
        String damageDesc = (String) body.get("damageDesc");
        returnService.apply(recordId, returnQty, photoUrls, damage, damageDesc);
        return Result.ok("归还申请已提交, 请把物资送到仓库待老师核对", null);
    }

    /** 仓库核对确认(完好->归库; 损坏->登记赔偿) */
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String remark = (String) body.getOrDefault("remark", "");
        BigDecimal compensate = body.get("compensate") == null
                ? BigDecimal.ZERO : new BigDecimal(body.get("compensate").toString());
        returnService.confirm(id, remark, compensate);
        return Result.ok("核对完成", null);
    }

    /** 赔偿完成确认(仓库登记已赔偿后关闭流程) */
    @PostMapping("/{id}/compensated")
    public Result<Void> compensated(@PathVariable Long id) {
        returnService.confirmCompensated(id);
        return Result.ok("赔偿流程已完成", null);
    }

    /** 待核对列表(仓库视角) */
    @GetMapping("/pending")
    public Result<Page<ReturnCheck>> pending(PageQuery query) {
        return Result.ok(returnService.pendingList(query));
    }

    /** 我的归还记录 */
    @GetMapping("/my")
    public Result<Page<ReturnCheck>> my(PageQuery query) {
        return Result.ok(returnService.myList(query));
    }
}
