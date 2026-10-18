package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.idle.common.BusinessException;
import com.campus.idle.common.PageQuery;
import com.campus.idle.common.UserContext;
import com.campus.idle.entity.*;
import com.campus.idle.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 借用服务: 申请提交与整个审批流
 *
 * 审批流(对应手绘流程图):
 *   学生/社团: 申请 -> 物资负责老师审批 -> 仓库老师线下确认(证件抵押) -> 申请人领走
 *   教师:      申请 -> 资产中心老师审批 -> 领导审批 -> 仓库老师确认 -> 领走
 *
 * 状态机: PENDING -> WAIT_LEADER(教师流程) / WAIT_WAREHOUSE(学生流程)
 *         -> WAREHOUSE_CONFIRMED -> TAKEN
 * 任一环节可 REJECTED; 申请人可 CANCELLED
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRequestMapper requestMapper;
    private final BorrowRecordMapper recordMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;

    /**
     * 提交借用申请
     */
    @Transactional
    public void apply(BorrowRequest req) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);

        // ---- 校验1: 冻结用户(有逾期未还)不能申请 ----
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("您的账号已因逾期未归还而被冻结, 归还物资后自动解冻");
        }
        // ---- 校验2: 物资必须存在且在库可借 ----
        Item item = itemMapper.selectById(req.getItemId());
        if (item == null) {
            throw new BusinessException("物资不存在");
        }
        if (item.getStatus() != 0) {
            throw new BusinessException("该物资当前不可借用(已借出/维修中/已下架)");
        }
        // ---- 校验3: 日期必须合法 ----
        if (req.getUseStart() == null || req.getUseEnd() == null
                || req.getUseEnd().isBefore(req.getUseStart())) {
            throw new BusinessException("借用日期不合法: 归还日期不能早于开始日期");
        }

        // ---- 组装申请单 ----
        req.setRequestNo("BR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", userId % 1000));   // 单号=BR+时间+用户尾号
        req.setApplicantId(userId);
        req.setApplicantType("TEACHER".equals(user.getRole()) ? "TEACHER" : "STUDENT");

        // ---- 按申请人类型确定第一审批老师 ----
        if ("TEACHER".equals(req.getApplicantType())) {
            // 教师申请 -> 找资产中心老师(role=ASSET)审批
            User assetTeacher = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ASSET").last("LIMIT 1"));
            if (assetTeacher == null) {
                throw new BusinessException("系统中未配置资产中心老师, 无法提交申请");
            }
            req.setTeacherId(assetTeacher.getId());
        } else {
            // 学生/社团申请 -> 找该物资对应的负责老师
            if (item.getManagerId() == null) {
                throw new BusinessException("该物资暂未设置负责老师, 请联系仓库管理员");
            }
            req.setTeacherId(item.getManagerId());
        }
        req.setStatus("PENDING");
        requestMapper.insert(req);

        // ---- 通知老师有新申请待审批 ----
        notify(req.getTeacherId(), "新的借用申请待审批",
                user.getRealName() + " 申请借用「" + item.getName() + "」, 单号 " + req.getRequestNo(),
                "APPROVE", req.getId());
    }

    /**
     * 老师审批(物资负责老师/资产中心老师)
     * @param approve true通过 false驳回
     */
    @Transactional
    public void teacherApprove(Long requestId, boolean approve, String remark) {
        BorrowRequest req = getAndCheck(requestId, "PENDING");
        Long teacherId = UserContext.getUserId();
        // 只有被指定的老师才能审批
        if (!teacherId.equals(req.getTeacherId())) {
            throw new BusinessException("只有该申请对应的负责老师才能审批");
        }
        req.setTeacherId(teacherId);
        req.setTeacherRemark(remark);
        req.setTeacherTime(LocalDateTime.now());

        if (approve) {
            if ("TEACHER".equals(req.getApplicantType())) {
                // 教师流程: 转给领导审批
                req.setStatus("WAIT_LEADER");
                User leader = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "LEADER").last("LIMIT 1"));
                if (leader != null) {
                    notify(leader.getId(), "教师借用申请待领导审批",
                            "单号 " + req.getRequestNo() + " 已通过资产中心老师审批, 请领导审批",
                            "APPROVE", req.getId());
                }
            } else {
                // 学生流程: 直接转仓库确认
                req.setStatus("WAIT_WAREHOUSE");
                notifyWarehouse(req, "学生借用申请待仓库确认");
            }
        } else {
            req.setStatus("REJECTED");
            // 驳回后通知申请人
            notify(req.getApplicantId(), "借用申请被驳回",
                    "您的申请 " + req.getRequestNo() + " 未通过老师审批: " + remark,
                    "APPROVE", req.getId());
        }
        requestMapper.updateById(req);
    }

    /**
     * 领导审批(仅教师流程, WAIT_LEADER状态)
     */
    @Transactional
    public void leaderApprove(Long requestId, boolean approve, String remark) {
        BorrowRequest req = getAndCheck(requestId, "WAIT_LEADER");
        if (!"LEADER".equals(UserContext.get().getRole())) {
            throw new BusinessException("只有领导角色才能进行此审批");
        }
        req.setLeaderId(UserContext.getUserId());
        req.setLeaderRemark(remark);
        req.setLeaderTime(LocalDateTime.now());
        if (approve) {
            req.setStatus("WAIT_WAREHOUSE");
            notifyWarehouse(req, "教师借用申请待仓库确认");
        } else {
            req.setStatus("REJECTED");
            notify(req.getApplicantId(), "借用申请被驳回",
                    "您的申请 " + req.getRequestNo() + " 未通过领导审批: " + remark,
                    "APPROVE", req.getId());
        }
        requestMapper.updateById(req);
    }

    /**
     * 仓库老师线下确认: 核对实物 + 登记证件抵押
     */
    @Transactional
    public void warehouseConfirm(Long requestId, String remark, String depositDesc) {
        BorrowRequest req = getAndCheck(requestId, "WAIT_WAREHOUSE");
        checkRole("WAREHOUSE");
        req.setWarehouseId(UserContext.getUserId());
        req.setWarehouseRemark(remark);
        req.setDepositDesc(depositDesc);
        req.setStatus("WAREHOUSE_CONFIRMED");
        requestMapper.updateById(req);
        // 通知申请人来领
        notify(req.getApplicantId(), "申请已通过, 请到仓库领用",
                "单号 " + req.getRequestNo() + " 仓库已确认, 请线下完成证件抵押并领走物资",
                "APPROVE", req.getId());
    }

    /**
     * 申请人领走物资: 生成借用记录, 库存实时更新(物资状态->已借出)
     */
    @Transactional
    public void take(Long requestId) {
        BorrowRequest req = getAndCheck(requestId, "WAREHOUSE_CONFIRMED");
        // 只有申请人本人能确认领走
        if (!UserContext.getUserId().equals(req.getApplicantId())) {
            throw new BusinessException("只有申请人本人才能确认领用");
        }
        req.setStatus("TAKEN");
        requestMapper.updateById(req);

        // 生成借用记录(逾期提醒以此表为依据)
        BorrowRecord record = new BorrowRecord();
        record.setRequestId(req.getId());
        record.setRequestNo(req.getRequestNo());
        record.setItemId(req.getItemId());
        record.setBorrowerId(req.getApplicantId());
        record.setQuantity(req.getQuantity());
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(req.getUseEnd());
        record.setStatus("BORROWED");
        record.setRemindCount(0);
        recordMapper.insert(record);

        // 库存实时更新: 物资状态改为已借出
        Item item = itemMapper.selectById(req.getItemId());
        item.setStatus(1);
        itemMapper.updateById(item);
    }

    /**
     * 申请人取消申请
     */
    @Transactional
    public void cancel(Long requestId) {
        BorrowRequest req = requestMapper.selectById(requestId);
        if (req == null) {
            throw new BusinessException("申请不存在");
        }
        if (!UserContext.getUserId().equals(req.getApplicantId())) {
            throw new BusinessException("只有申请人本人才能取消");
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new BusinessException("当前状态不可取消, 如已进入审批请联系老师");
        }
        req.setStatus("CANCELLED");
        requestMapper.updateById(req);
    }

    // ==================== 查询方法 ====================

    /** 我的申请(申请人视角, 分页) */
    public Page<BorrowRequest> myRequests(PageQuery query) {
        return requestMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<BorrowRequest>()
                        .eq(BorrowRequest::getApplicantId, UserContext.getUserId())
                        .orderByDesc(BorrowRequest::getCreateTime));
    }

    /**
     * 我的借用记录(借出中/已归还/逾期)
     * 与"我的申请"区分: 申请是审批单据, 记录是实际领走后才生成的借用台账
     * 归还页面用这个接口拿到 recordId 发起归还
     */
    public Page<BorrowRecord> myRecords(PageQuery query) {
        return recordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getBorrowerId, UserContext.getUserId())
                        .orderByDesc(BorrowRecord::getCreateTime));
    }

    /** 待我审批的申请(老师/领导/仓库 视角, 分页) */
    public Page<BorrowRequest> todoList(PageQuery query) {
        User user = UserContext.get();
        LambdaQueryWrapper<BorrowRequest> wrapper = new LambdaQueryWrapper<>();
        switch (user.getRole()) {
            case "ASSET", "TEACHER" -> wrapper.eq(BorrowRequest::getStatus, "PENDING")
                    .eq(BorrowRequest::getTeacherId, user.getId());
            case "LEADER" -> wrapper.eq(BorrowRequest::getStatus, "WAIT_LEADER");
            case "WAREHOUSE" -> wrapper.in(BorrowRequest::getStatus, "WAIT_WAREHOUSE");
            default -> wrapper.eq(BorrowRequest::getId, -1L);   // 学生没有审批列表
        }
        wrapper.orderByDesc(BorrowRequest::getCreateTime);
        return requestMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    /** 全部申请(仓库管理员可看全量, 分页) */
    public Page<BorrowRequest> allRequests(PageQuery query) {
        return requestMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<BorrowRequest>()
                        .orderByDesc(BorrowRequest::getCreateTime));
    }

    // ==================== 私有工具方法 ====================

    /** 查询申请并校验状态 */
    private BorrowRequest getAndCheck(Long id, String expectedStatus) {
        BorrowRequest req = requestMapper.selectById(id);
        if (req == null) {
            throw new BusinessException("申请不存在");
        }
        if (!expectedStatus.equals(req.getStatus())) {
            throw new BusinessException("当前状态不允许该操作(状态: " + req.getStatus() + ")");
        }
        return req;
    }

    /** 校验当前用户角色 */
    private void checkRole(String role) {
        if (!role.equals(UserContext.get().getRole())) {
            throw new BusinessException("没有权限执行该操作");
        }
    }

    /** 给仓库管理员发通知 */
    private void notifyWarehouse(BorrowRequest req, String title) {
        User wh = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "WAREHOUSE").last("LIMIT 1"));
        if (wh != null) {
            notify(wh.getId(), title, "单号 " + req.getRequestNo() + " 审批已通过, 请线下核对实物并确认",
                    "APPROVE", req.getId());
        }
    }

    /** 发送通知 */
    private void notify(Long userId, String title, String content, String type, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }
}
