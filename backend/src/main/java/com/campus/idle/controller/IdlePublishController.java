package com.campus.idle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.Result;
import com.campus.idle.entity.IdlePublish;
import com.campus.idle.service.IdlePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 闲置上架接口: 各部门提交 / 仓库审核 / 查询
 */
@RestController
@RequestMapping("/api/idle")
@RequiredArgsConstructor
public class IdlePublishController {

    private final IdlePublishService idlePublishService;

    /** 提交闲置物资上架申请 */
    @PostMapping
    public Result<Void> submit(@RequestBody IdlePublish publish) {
        idlePublishService.submit(publish);
        return Result.ok("提交成功, 请等待仓库老师审核", null);
    }

    /** 审核闲置上架(仓库管理员) */
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String remark = (String) body.getOrDefault("remark", "");
        idlePublishService.review(id, pass, remark);
        return Result.ok(pass ? "已通过并上架" : "已驳回", null);
    }

    /** 待审核列表(仓库视角) */
    @GetMapping("/pending")
    public Result<Page<IdlePublish>> pending(PageQuery query) {
        return Result.ok(idlePublishService.pendingList(query));
    }

    /** 我的提交记录 */
    @GetMapping("/my")
    public Result<Page<IdlePublish>> my(PageQuery query) {
        return Result.ok(idlePublishService.myList(query));
    }
}
