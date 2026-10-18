# 🚀 部署方案说明

## 一、结论先行

| 平台 | 能放什么 | 能不能跑后端 |
|------|---------|-------------|
| **GitHub** | 只存代码(备份/版本管理/给老师看源码) | ❌ 不运行 |
| **GitHub Pages** | 静态页面(frontend/dist) | ❌ 和 EdgeOne 一样, 只托管静态文件 |
| **EdgeOne Pages** | 静态页面(frontend/dist) | ❌ 只托管静态文件 |
| **腾讯云轻量应用服务器** | 全部(后端 jar + MySQL + Redis + 前端) | ✅ 唯一能完整运行的地方 |

**核心原理**: 你的项目是"前端 + Java后端 + 数据库"三层架构。
后端需要一台**一直开着的服务器**跑 Java 进程, 静态托管平台做不到这件事。

## 二、三种方案, 按你的需求选

### 方案A: 只交作业/课堂演示(0元, 推荐)

**不部署, 本机运行即可** —— 答辩时现场启动给别人看, 最稳定。

```bash
cd backend && mvn spring-boot:run     # 后端
cd frontend && npm run dev            # 前端
```

### 方案B: 代码存 GitHub + 公网能访问(约10元/月起)

1. **代码传到 GitHub**(免费): 用本目录下的 `.gitignore` 已自动排除
   `node_modules`、`target`、**含密码的 application.yml**
2. **后端部署到腾讯云轻量应用服务器**(学生优惠约9.9~24元/月):
   - 装 JDK17 / MySQL8 / Redis
   - `mvn package` 打出 jar 上传
   - 数据库执行 `sql/init.sql`
   - 复制 `application.yml.example` 为 `application.yml` 并填服务器密码
3. **前端部署到 EdgeOne / GitHub Pages**(免费): 只传 `frontend/dist` 的 21 个文件
   - 改 `frontend/vite.config.js` 的 proxy target 为服务器公网IP后重新 `npm run build`

### 方案C: 完全免费(有代价)

- 前端: GitHub Pages / EdgeOne(免费)
- 后端: Render / Railway 免费额度 —— 但**国内访问慢**、容器会**休眠**(首次访问要等几十秒冷启动)、配置复杂
- 适合学习, 不适合给老师演示

## 三、上传 GitHub 前的检查清单

- [x] `.gitignore` 已创建, 自动排除 node_modules / target / application.yml
- [x] `application.yml.example` 已提供(不含真实密码)
- [ ] 确认 `application.yml` 没有被提交: `git status` 中不应出现它
- [ ] 如误传过密码, 立即改 MySQL 密码

上传命令:

```bash
cd /Users/wcy/闲置物资/idle-assets-platform
git init
git add .
git status        # 确认没有 node_modules 和 application.yml
git commit -m "校园闲置物资管理平台"
git remote add origin 你的仓库地址
git push -u origin main
```

## 四、常见问题

**Q: 我把数据库也传上去, 电脑会变肉鸡吗?**
A: 不会。"肉鸡"指电脑被木马远程控制, 传文件到托管平台不会发生这种事。
真正的风险是**密码泄露**——所以要靠 `.gitignore` 挡住含密码的配置文件。

**Q: 只传前端到 EdgeOne 会怎样?**
A: 页面能打开, 但登录/借用/审批都会失败, 因为 `/api` 请求没有后端接收。
要么把后端部署到服务器, 要么只在本地演示。
