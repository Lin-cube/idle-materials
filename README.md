# 🏫 校园闲置物资管理平台

基于你提供的手绘流程图与《资产信息查询.xlsx》搭建的校园闲置物资管理系统。

## 一、技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 后端 | Java 17 + Spring Boot 3.3 | 纯手写业务代码, 逐行中文注释 |
| 安全 | Spring Security + JWT | 无状态Token认证 |
| ORM | MyBatis-Plus 3.5 | 单表CRUD免写SQL |
| 数据库 | MySQL 8.4 (idle_assets库) | 建库脚本见 `sql/init.sql` |
| 缓存 | Redis 7 | 预留(当前JWT无状态, 后续可存会话) |
| 前端 | Vue 3 + Element Plus + Vite | 前后端分离 |

## 二、目录结构

```
idle-assets-platform/
├── sql/init.sql              # 建库建表 + 11条Excel资产种子数据
├── backend/                  # Spring Boot 后端
│   ├── pom.xml
│   ├── uploads/              # 物资图片(4张实物照片已配好)
│   └── src/main/java/com/campus/idle/
│       ├── controller/       # 接口层(8个Controller)
│       ├── service/          # 业务层(借用审批流/归还核对/逾期任务)
│       ├── mapper/           # 数据层
│       ├── entity/           # 实体(对齐数据库表)
│       ├── config/           # Security/JWT/CORS/Redis/分页配置
│       └── common/           # 统一返回/全局异常/JWT工具
└── frontend/                 # Vue3 前端
    └── src/views/            # 10个页面
```

## 三、启动步骤(共4步)

### ① 初始化数据库(首次运行一次即可)

```bash
mysql -uroot -p < sql/init.sql
```

> 若你的MySQL密码不是 `Wcy@2026abc`, 需同时修改
> `backend/src/main/resources/application.yml` 中的 `spring.datasource.password`

### ② 启动后端(保持Redis运行)

```bash
# 确保Redis已启动: brew services start redis
cd backend
mvn spring-boot:run
```

看到 "校园闲置物资管理平台后端启动成功!" 即成功, 接口地址 `http://localhost:8081`

### ③ 启动前端(新开一个终端)

```bash
cd frontend
npm install     # 仅首次需要
npm run dev
```

浏览器打开控制台显示的地址(默认 `http://localhost:5173`,
若被占用会自动顺延为 5174, 以控制台输出为准)

### ④ 登录体验

演示账号(密码均为 `123456`):

| 账号 | 角色 | 能做什么 |
|------|------|----------|
| student | 学生(张同学) | 物资大厅申请借用 / 发起归还 / 闲置上架 |
| teacher | 教师(李老师) | 作为物资负责老师审批学生申请 / 以教师身份申请(走双审批) |
| warehouse | 仓库管理员(王管理) | 仓库确认+证件抵押 / 归还核对 / 闲置审核 / 物资管理 |
| asset | 资产中心(赵资产) | 教师申请的第一道审批 |
| leader | 领导(钱领导) | 教师申请的第二道审批 |

## 四、功能与流程图对照

| 流程图要求 | 实现 |
|-----------|------|
| ① 库存实时更新 | 领走→物资变"已借出"; 仓库核对归还→自动回库 |
| ② 逾期提醒 | 每天早8点定时任务: 到期前3天提醒; 逾期→标记OVERDUE+冻结账号(不能再借用), 归还后自动解冻 |
| ③ 审批流程(不同物资不同负责老师) | 学生申请→物资负责老师审批→仓库老师线下确认(登记证件抵押)→领走; 教师申请→资产中心老师→领导→仓库确认 |
| ④ 归还核对 | 线上填数量(必须与借出数一致)+实物拍照; 仓库核对; 损坏/丢失按学校规定登记赔偿金额, 赔缴后关闭 |
| ⑤ 闲置物资上架 | 各部门提交闲置物资→仓库老师审核→自动生成物资进入物资大厅 |
| ⑥ 借出即下架 / 归还即上架 | 领走后自动从物资大厅下架, 仓库核对归还后自动重新上架(大厅顶部可勾选"显示已借出"查看全部) |

### 闲置资产必填信息

提交闲置物资时以下为**必填**(前端校验 + 后端二次校验):

- 物资名称、物资分类、数量
- **联系人**、**联系电话**(11位手机号格式)
- **物资图片**(需先上传)
- 存放地点、物资简介(选填)

> 注: Excel原始数据中无电话, 现有11条资产的联系电话为编造的演示号码
> (`1380013800x`), 上线前请替换为真实号码, 见 `sql/init.sql` 第12节

## 五、主要接口一览

```
POST /api/auth/login | register            登录/注册
GET  /api/items                            物资大厅(分页/搜索/分类)
POST /api/borrow/apply                     提交借用申请
GET  /api/borrow/my | todo | all | records 我的申请/待办/全量/借用记录
POST /api/borrow/{id}/teacher-approve      老师审批
POST /api/borrow/{id}/leader-approve       领导审批
POST /api/borrow/{id}/warehouse-confirm    仓库确认(证件抵押)
POST /api/borrow/{id}/take | cancel        领走/取消
POST /api/return/apply                     发起归还
POST /api/return/{id}/confirm | compensated 核对/赔偿完成
POST /api/idle + /{id}/review              闲置上架/审核
GET  /api/notifications                    消息通知
POST /api/files/upload                     图片上传
```

## 六、常见问题

1. **8081端口被占用**: 改 `application.yml` 的 `server.port`, 并同步改 `frontend/vite.config.js` 代理目标
2. **5173被占用**: Vite会自动顺延到5174, 打开控制台显示的地址即可
3. **登录报"网络异常"**: 先确认后端已启动、Redis在运行
4. **忘记密码/想重置数据**: 重新执行 `mysql -uroot -p < sql/init.sql` 即可全部重置
5. **Excel资产导入**: 种子数据已包含《资产信息查询.xlsx》全部11条资产, 新增资产走"物资管理"页面
