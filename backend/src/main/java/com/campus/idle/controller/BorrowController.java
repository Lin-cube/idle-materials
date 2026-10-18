package com.campus.idle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.Result;
import com.campus.idle.entity.BorrowRecord;
import com.campus.idle.entity.BorrowRequest;
import com.campus.idle.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 借用接口: 申请 / 审批 / 仓库确认 / 领走 / 取消 / 查询
 */
@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /** 提交借用申请 */
    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody BorrowRequest req) {
        borrowService.apply(req);
        return Result.ok("申请已提交, 请等待老师审批", null);
    }

    /** 我的申请(申请人视角) */
    @GetMapping("/my")
    public Result<Page<BorrowRequest>> my(PageQuery query) {
        return Result.ok(borrowService.myRequests(query));
    }

    /**
     * 我的借用记录(借出中/已归还/逾期)
     * 归还页面用它拿 recordId 发起归还
     */
    @GetMapping("/records")
    public Result<Page<BorrowRecord>> records(PageQuery query) {
        return Result.ok(borrowService.myRecords(query));
    }

    /** 待我处理的申请(老师/领导/仓库视角) */
    @GetMapping("/todo")
    public Result<Page<BorrowRequest>> todo(PageQuery query) {
        return Result.ok(borrowService.todoList(query));
    }

    /** 全部申请(仓库管理员视角) */
    @GetMapping("/all")
    public Result<Page<BorrowRequest>> all(PageQuery query) {
        return Result.ok(borrowService.allRequests(query));
    }

    /** 老师审批 */
    @PostMapping("/{id}/teacher-approve")
    public Result<Void> teacherApprove(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String remark = (String) body.getOrDefault("remark", "");
        borrowService.teacherApprove(id, approve, remark);
        return Result.ok(approve ? "已通过" : "已驳回", null);
    }

    /** 领导审批(教师流程) */
    @PostMapping("/{id}/leader-approve")
    public Result<Void> leaderApprove(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String remark = (String) body.getOrDefault("remark", "");
        borrowService.leaderApprove(id, approve, remark);
        return Result.ok(approve ? "已通过" : "已驳回", null);
    }

    /** 仓库线下确认(核对实物+登记证件抵押) */
    @PostMapping("/{id}/warehouse-confirm")
    public Result<Void> warehouseConfirm(@PathVariable Long id, @RequestBody Map<String, String> body) {
        borrowService.warehouseConfirm(id, body.getOrDefault("remark", ""), body.get("depositDesc"));
        return Result.ok("已确认, 通知申请人领用", null);
    }

    /** 申请人确认领走(生成借用记录, 库存更新为已借出) */
    @PostMapping("/{id}/take")
    public Result<Void> take(@PathVariable Long id) {
        borrowService.take(id);
        return Result.ok("领用成功, 请按时归还", null);
    }

    /** 取消申请 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        borrowService.cancel(id);
        return Result.ok("已取消", null);
    }
}
