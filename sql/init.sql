-- =====================================================================
-- 校园闲置物资管理平台 · 数据库初始化脚本
-- 数据库: MySQL 8.4  |  字符集: utf8mb4
-- 使用方法:
--   mysql -uroot -p < sql/init.sql
-- =====================================================================

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS idle_assets DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE idle_assets;

-- =====================================================================
-- 2. 用户表 sys_user
--    角色 role: STUDENT学生 / TEACHER教师 / WAREHOUSE仓库管理员 / ASSET资产中心老师 / LEADER领导
--    状态 status: 0正常 1冻结(逾期未归还, 冻结后不允许再借用)
-- =====================================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username      VARCHAR(50)  NOT NULL                COMMENT '登录账号(学号/工号)',
    password      VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt加密)',
    real_name     VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
    role          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
    phone         VARCHAR(20)  DEFAULT NULL            COMMENT '联系电话',
    dept          VARCHAR(100) DEFAULT NULL            COMMENT '所属部门/学院/社团',
    status        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0正常 1冻结',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户表';

-- =====================================================================
-- 3. 物资表 item
--    来源: 资产信息查询.xlsx (资产编号/名称/单价/计量单位/管理部门/存放地点/型号/规格)
--    状态 status: 0在库可借 1已借出 2维修中 3已下架
--    类型 item_type: IDLE(闲置物资, 各部门挂载) / ASSET(在管资产, 可出借)
-- =====================================================================
DROP TABLE IF EXISTS item;
CREATE TABLE item (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '物资ID',
    asset_no       VARCHAR(50)   NOT NULL                COMMENT '资产编号',
    name           VARCHAR(200)  NOT NULL                COMMENT '物资名称',
    category       VARCHAR(50)   DEFAULT '其他'          COMMENT '物资分类(家具/电子设备/搬运设备...)',
    price          DECIMAL(10,2) DEFAULT 0               COMMENT '单价(元)',
    unit           VARCHAR(20)   DEFAULT '件'            COMMENT '计量单位(台/张/辆...)',
    model          VARCHAR(200)  DEFAULT NULL            COMMENT '型号',
    spec           VARCHAR(200)  DEFAULT NULL            COMMENT '规格',
    image          VARCHAR(500)  DEFAULT NULL            COMMENT '物资图片URL',
    dept           VARCHAR(100)  DEFAULT NULL            COMMENT '使用/管理部门',
    location       VARCHAR(200)  DEFAULT NULL            COMMENT '存放地点',
    owner_name     VARCHAR(50)   DEFAULT NULL            COMMENT '使用人',
    owner_phone    VARCHAR(20)   DEFAULT NULL            COMMENT '使用人电话',
    item_type      VARCHAR(20)   NOT NULL DEFAULT 'ASSET' COMMENT '类型: ASSET在管资产 / IDLE闲置物资',
    manager_id     BIGINT        DEFAULT NULL            COMMENT '该物资对应负责老师ID(审批时按此找老师)',
    description    VARCHAR(500)  DEFAULT NULL            COMMENT '物资简介',
    status         TINYINT       NOT NULL DEFAULT 0      COMMENT '状态: 0在库可借 1已借出 2维修中 3已下架',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_no (asset_no),
    KEY idx_status (status),
    KEY idx_category (category)
) ENGINE=InnoDB COMMENT='物资表';

-- =====================================================================
-- 4. 借用申请表 borrow_request
--    审批状态 status:
--      PENDING             待第一老师审批(学生找物资负责老师 / 教师找资产中心老师)
--      WAIT_LEADER         老师已通过, 待领导审批(仅教师流程)
--      WAIT_WAREHOUSE      待仓库老师确认(线下核对实物)
--      WAREHOUSE_CONFIRMED 仓库已确认+登记证件抵押, 待申请人领走
--      TAKEN               已领走(生成借用记录 borrow_record)
--      REJECTED            已驳回(见 teacher_remark/leader_remark)
--      CANCELLED           申请人取消
--    申请人类型 applicant_type: STUDENT学生(社团) / TEACHER教师
-- =====================================================================
DROP TABLE IF EXISTS borrow_request;
CREATE TABLE borrow_request (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    request_no     VARCHAR(32)  NOT NULL                COMMENT '申请单号 BR+yyyyMMddHHmmss+序号',
    item_id        BIGINT       NOT NULL                COMMENT '物资ID',
    applicant_id   BIGINT       NOT NULL                COMMENT '申请人ID',
    applicant_type VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '申请人类型: STUDENT/TEACHER',
    quantity       INT          NOT NULL DEFAULT 1      COMMENT '申请数量',
    purpose        VARCHAR(500) NOT NULL                COMMENT '借用用途(社团活动/教学...)',
    use_start      DATE         NOT NULL                COMMENT '借用开始日期',
    use_end        DATE         NOT NULL                COMMENT '预计归还日期',
    teacher_id     BIGINT       DEFAULT NULL            COMMENT '第一审批老师ID(物资负责老师/资产中心老师)',
    teacher_remark VARCHAR(500) DEFAULT NULL            COMMENT '老师审批意见',
    teacher_time   DATETIME     DEFAULT NULL            COMMENT '老师审批时间',
    leader_id      BIGINT       DEFAULT NULL            COMMENT '领导审批ID',
    leader_remark  VARCHAR(500) DEFAULT NULL            COMMENT '领导审批意见',
    leader_time    DATETIME     DEFAULT NULL            COMMENT '领导审批时间',
    warehouse_id   BIGINT       DEFAULT NULL            COMMENT '仓库确认人ID',
    warehouse_remark VARCHAR(500) DEFAULT NULL          COMMENT '仓库确认备注(线下实物核对)',
    deposit_desc   VARCHAR(200) DEFAULT NULL            COMMENT '证件抵押说明(学生证/身份证)',
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_no (request_no),
    KEY idx_item (item_id),
    KEY idx_applicant (applicant_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='借用申请表';

-- =====================================================================
-- 5. 借用记录表 borrow_record  (申请领走后生成)
--    状态 status: BORROWED借出中 / RETURNED已归还 / OVERDUE逾期
--    库存实时更新的依据: 领走 item.status->1, 归还 item.status->0
-- =====================================================================
DROP TABLE IF EXISTS borrow_record;
CREATE TABLE borrow_record (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    request_id     BIGINT      NOT NULL                COMMENT '关联申请ID',
    request_no     VARCHAR(32) NOT NULL                COMMENT '申请单号',
    item_id        BIGINT      NOT NULL                COMMENT '物资ID',
    borrower_id    BIGINT      NOT NULL                COMMENT '借用人ID',
    quantity       INT         NOT NULL DEFAULT 1      COMMENT '借出数量',
    borrow_date    DATETIME    NOT NULL                COMMENT '实际领走时间',
    due_date       DATE        NOT NULL                COMMENT '应还日期',
    return_date    DATETIME    DEFAULT NULL            COMMENT '实际归还时间',
    status         VARCHAR(20) NOT NULL DEFAULT 'BORROWED' COMMENT '状态: BORROWED/RETURNED/OVERDUE',
    remind_count   INT         NOT NULL DEFAULT 0      COMMENT '逾期提醒已发送次数',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_item (item_id),
    KEY idx_borrower (borrower_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='借用记录表';

-- =====================================================================
-- 6. 归还核对表 return_check
--    流程: 借用人线上发起归还(填数量) + 线下实物拍照 -> 仓库老师核对确认
--    损坏情况 damage: NONE完好 / DAMAGED损坏 / LOST丢失
--    损坏/丢失按学校规定登记赔偿金额, 赔偿后由仓库老师标记已赔偿
-- =====================================================================
DROP TABLE IF EXISTS return_check;
CREATE TABLE return_check (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '核对ID',
    record_id      BIGINT        NOT NULL                COMMENT '借用记录ID',
    returner_id    BIGINT        NOT NULL                COMMENT '归还人ID',
    return_qty     INT           NOT NULL DEFAULT 1      COMMENT '归还数量(需与借出数量对上)',
    photo_urls     VARCHAR(1000) DEFAULT NULL            COMMENT '实物拍照URL(多个逗号分隔)',
    damage         VARCHAR(20)   NOT NULL DEFAULT 'NONE' COMMENT '损坏情况: NONE/DAMAGED/LOST',
    damage_desc    VARCHAR(500)  DEFAULT NULL            COMMENT '损坏说明',
    compensate     DECIMAL(10,2) DEFAULT 0               COMMENT '赔偿金额(元), 按学校规定',
    compensated    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已赔偿: 0否 1是',
    checker_id     BIGINT        DEFAULT NULL            COMMENT '仓库核对人ID',
    check_remark   VARCHAR(500)  DEFAULT NULL            COMMENT '核对备注',
    check_time     DATETIME      DEFAULT NULL            COMMENT '核对时间',
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待核对 / CONFIRMED已确认',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    PRIMARY KEY (id),
    KEY idx_record (record_id)
) ENGINE=InnoDB COMMENT='归还核对表';

-- =====================================================================
-- 7. 闲置上架表 idle_publish
--    流程: 各部门提交闲置物资 -> 仓库老师审核 -> 通过后进入物资大厅可被借用
--    状态 status: PENDING待审核 / PASS通过 / REJECT驳回
-- =====================================================================
DROP TABLE IF EXISTS idle_publish;
CREATE TABLE idle_publish (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '上架ID',
    publisher_id   BIGINT        NOT NULL                COMMENT '提交人ID',
    contact_name   VARCHAR(50)   DEFAULT NULL            COMMENT '联系人(必填)',
    contact_phone  VARCHAR(20)   DEFAULT NULL            COMMENT '联系电话(必填)',
    name           VARCHAR(200)  NOT NULL                COMMENT '物资名称',
    category       VARCHAR(50)   DEFAULT '其他'          COMMENT '物资分类',
    quantity       INT           NOT NULL DEFAULT 1      COMMENT '数量',
    image          VARCHAR(500)  DEFAULT NULL            COMMENT '物资图片URL',
    description    VARCHAR(500)  DEFAULT NULL            COMMENT '物资简介',
    location       VARCHAR(200)  DEFAULT NULL            COMMENT '存放地点',
    reviewer_id    BIGINT        DEFAULT NULL            COMMENT '审核人(仓库老师)ID',
    review_remark  VARCHAR(500)  DEFAULT NULL            COMMENT '审核意见',
    review_time    DATETIME      DEFAULT NULL            COMMENT '审核时间',
    item_id        BIGINT        DEFAULT NULL            COMMENT '审核通过后生成的物资ID',
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PASS/REJECT',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='闲置上架表';

-- =====================================================================
-- 8. 消息通知表 notification
--    用途: 审批结果通知 / 逾期提醒(到期前3天+逾期) / 归还确认通知
--    已读 is_read: 0未读 1已读
-- =====================================================================
DROP TABLE IF EXISTS notification;
CREATE TABLE notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    user_id     BIGINT       NOT NULL                COMMENT '接收人ID',
    title       VARCHAR(100) NOT NULL                COMMENT '通知标题',
    content     VARCHAR(500) NOT NULL                COMMENT '通知内容',
    type        VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型: SYSTEM/APPROVE/RETURN/OVERDUE',
    related_id  BIGINT       DEFAULT NULL            COMMENT '关联业务ID(申请/记录ID)',
    is_read     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读: 0未读 1已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_read (is_read)
) ENGINE=InnoDB COMMENT='消息通知表';

-- =====================================================================
-- 9. 种子数据 · 用户
--    初始密码统一为 123456 (下方为BCrypt密文)
-- =====================================================================
INSERT INTO sys_user (id, username, password, real_name, role, phone, dept) VALUES
(1, 'student',  '$2b$10$J487sHPqr4kPK/GvW7MOeebjfRiO/XmEPhEWLqvmyKuXMUP2egmpm', '张同学', 'STUDENT',   '13800000001', '机器人社团'),
(2, 'teacher',  '$2b$10$J487sHPqr4kPK/GvW7MOeebjfRiO/XmEPhEWLqvmyKuXMUP2egmpm', '李老师', 'TEACHER',   '13800000002', '电子及自动化实验教学中心'),
(3, 'warehouse','$2b$10$J487sHPqr4kPK/GvW7MOeebjfRiO/XmEPhEWLqvmyKuXMUP2egmpm', '王管理', 'WAREHOUSE', '13800000003', '仓库管理科'),
(4, 'asset',    '$2b$10$J487sHPqr4kPK/GvW7MOeebjfRiO/XmEPhEWLqvmyKuXMUP2egmpm', '赵资产', 'ASSET',     '13800000004', '资产中心'),
(5, 'leader',   '$2b$10$J487sHPqr4kPK/GvW7MOeebjfRiO/XmEPhEWLqvmyKuXMUP2egmpm', '钱领导', 'LEADER',    '13800000005', '后勤处'),
(6, 'club',     '$2b$10$J487sHPqr4kPK/GvW7MOeebjfRiO/XmEPhEWLqvmyKuXMUP2egmpm', '孙社长', 'STUDENT',   '13800000006', '电子协会');

-- =====================================================================
-- 10. 种子数据 · 物资 (来自 资产信息查询.xlsx)
--     manager_id: 物资对应的负责老师(仓库确认找王管理id=3; 教学设备老师审批找李老师id=2)
-- =====================================================================
INSERT INTO item (id, asset_no, name, category, price, unit, model, spec, dept, location, owner_name, item_type, manager_id, description, status) VALUES
(1,  '20260005J3', '单人床（带床垫）',            '家具',     1040.00, '张', '林英牌',                              '1100*2000mm',                    '留学生公寓',             '红场宿舍C区402',        '李梓妍', 'ASSET', 3, '宿舍闲置单人床, 带床垫', 0),
(2,  '20260004SS', '联想笔记本电脑Thinkbook14+', '电子设备', 5750.00, '台', 'Thinkbook14+',                        'I4+00CD R7-H260/32G/1T/W11/3.0K','科研处',                 'S01行政楼7层',          '袁广林', 'ASSET', 2, '高配笔记本电脑, 可供短借用', 0),
(3,  '20260004J3', '单人床（带床垫）',            '家具',     1040.00, '张', '林英牌',                              '1100*2000mm',                    '留学生公寓',             '红场宿舍C区401',        '李梓妍', 'ASSET', 3, '宿舍闲置单人床, 带床垫', 0),
(4,  '20260003SS', '白色美式档案柜',              '家具',     590.00,  '个', NULL,                                  '白色 4抽',                       '国际教育学院',           '国教院办公室',          '丁明慧', 'IDLE',  3, '四抽档案柜, 部门闲置, 完好可用', 0),
(5,  '20260003J3', '单人床（带床垫）',            '家具',     1040.00, '张', '林英牌',                              '1100*2000mm',                    '留学生公寓',             '红场宿舍C区401',        '李梓妍', 'ASSET', 3, '宿舍闲置单人床, 带床垫', 0),
(6,  '20260002SS', '联想电脑酷310',               '电子设备', 3500.00, '台', '酷310 I7-13620H/16G/512G/27寸显示器', NULL,                             '继续教育学院',           'T27号教学楼315',        '于迎春', 'ASSET', 2, '台式电脑一套(含27寸显示器)', 0),
(7,  '20260002J3', '单人床（带床垫）',            '家具',     1040.00, '张', '林英牌',                              '1100*2000mm',                    '留学生公寓',             '红场宿舍C区219',        '李梓妍', 'ASSET', 3, '宿舍闲置单人床, 带床垫', 0),
(8,  '20260001SS', '联想电脑酷310',               '电子设备', 3500.00, '台', '酷310 I7-13620H/16G/512G/27寸显示器', NULL,                             '继续教育学院',           'T27号教学楼315',        '马磊',   'ASSET', 2, '台式电脑一套(含27寸显示器)', 0),
(9,  '20260001J3', '单人床（带床垫）',            '家具',     1040.00, '张', '林英牌',                              '1100*2000mm',                    '留学生公寓',             '红场宿舍C区219',        '李梓妍', 'ASSET', 3, '宿舍闲置单人床, 带床垫', 0),
(10, '20253107SS', '地牛搬运车',                  '搬运设备', 870.00,  '辆', '3000Kg',                              NULL,                             '电子及自动化实验教学中心','T28工训中心203',       '李瑞光', 'ASSET', 2, '3吨液压搬运车(地牛), 搬运重物用', 0),
(11, '20253106SS', '液压堆高车',                  '搬运设备', 3000.00, '台', NULL,                                  '液压堆高车',                     '电子及自动化实验教学中心','T28工训中心203',       '李瑞光', 'IDLE',  2, '液压堆高车, 中心闲置可外借', 0);

-- 11. 物资示例图片(图片文件位于 backend/uploads/, 需与后端一起部署)
UPDATE item SET image='/files/laptop.jpg'       WHERE id=2;    -- 联想笔记本电脑
UPDATE item SET image='/files/cabinet.jpg'      WHERE id=4;    -- 白色美式档案柜
UPDATE item SET image='/files/pallet-truck.jpg' WHERE id=10;   -- 地牛搬运车
UPDATE item SET image='/files/stacker.jpg'      WHERE id=11;   -- 液压堆高车
UPDATE item SET image='/files/bed.png'          WHERE name LIKE '单人床%';   -- 单人床(带床垫), 共5条
UPDATE item SET image='/files/lenovo310.png'    WHERE name='联想电脑酷310';  -- 联想电脑酷310, 共2条

-- 12. 联系人联系电话(Excel中无电话, 以下为编造的演示号码, 上线前请替换为真实号码)
UPDATE item SET owner_phone='13800138001' WHERE owner_name='李梓妍';   -- 单人床
UPDATE item SET owner_phone='13800138002' WHERE owner_name='袁广林';   -- Thinkbook14+
UPDATE item SET owner_phone='13800138003' WHERE owner_name='丁明慧';   -- 白色美式档案柜
UPDATE item SET owner_phone='13800138004' WHERE owner_name='于迎春';   -- 联想电脑酷310
UPDATE item SET owner_phone='13800138005' WHERE owner_name='马磊';     -- 联想电脑酷310
UPDATE item SET owner_phone='13800138006' WHERE owner_name='李瑞光';   -- 地牛搬运车/液压堆高车
