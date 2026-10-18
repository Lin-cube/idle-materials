<!-- 主布局: 顶部导航 + 左侧菜单 + 内容区 -->
<template>
  <el-container class="layout">
    <!-- 顶部栏 -->
    <el-header class="header">
      <div class="logo">🏫 校园闲置物资管理平台</div>
      <div class="right">
        <!-- 消息铃铛(带未读红点) -->
        <el-badge :value="unread" :hidden="unread === 0" class="bell" @click="$router.push('/notifications')">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            {{ store.user?.realName }}
            <el-tag size="small" style="margin-left: 6px">{{ roleLabel }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧菜单: 按角色动态显示 -->
      <el-aside width="200px" class="aside">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/hall"><el-icon><Grid /></el-icon>物资大厅</el-menu-item>
          <el-menu-item index="/my-borrow"><el-icon><Tickets /></el-icon>我的借用</el-menu-item>
          <el-menu-item index="/my-return"><el-icon><RefreshLeft /></el-icon>归还管理</el-menu-item>
          <el-menu-item index="/my-idle"><el-icon><Upload /></el-icon>闲置上架</el-menu-item>
          <el-menu-item index="/notifications"><el-icon><ChatDotRound /></el-icon>消息通知</el-menu-item>
          <!-- 审批中心: 老师/领导/仓库可见 -->
          <el-menu-item v-if="canApprove" index="/approvals">
            <el-icon><Stamp /></el-icon>审批中心
          </el-menu-item>
          <!-- 归还核对: 仓库可见 -->
          <el-menu-item v-if="store.role === 'WAREHOUSE'" index="/return-check">
            <el-icon><Checked /></el-icon>归还核对
          </el-menu-item>
          <!-- 闲置审核: 仓库可见 -->
          <el-menu-item v-if="store.role === 'WAREHOUSE'" index="/idle-review">
            <el-icon><View /></el-icon>闲置审核
          </el-menu-item>
          <!-- 物资管理: 仓库可见 -->
          <el-menu-item v-if="store.role === 'WAREHOUSE'" index="/items-manage">
            <el-icon><Setting /></el-icon>物资管理
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { unreadCount } from '../api'

const store = useUserStore()
const router = useRouter()
const unread = ref(0)
let timer = null

// 角色中文标签
const roleLabel = computed(() => ({
  STUDENT: '学生', TEACHER: '教师', WAREHOUSE: '仓库管理员',
  ASSET: '资产中心', LEADER: '领导'
}[store.role] || store.role))

// 是否显示审批中心: 老师/资产中心/领导/仓库
const canApprove = computed(() =>
  ['TEACHER', 'ASSET', 'LEADER', 'WAREHOUSE'].includes(store.role)
)

// 拉取未读消息数
const loadUnread = async () => {
  try {
    const res = await unreadCount()
    unread.value = res.data.count
  } catch (e) { /* 静默失败 */ }
}

// 退出登录
const onCommand = (cmd) => {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}

// 每30秒刷新一次未读数
onMounted(() => {
  loadUnread()
  timer = setInterval(loadUnread, 30000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.layout { height: 100%; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
}
.logo { font-size: 17px; font-weight: 600; }
.right { display: flex; align-items: center; gap: 20px; }
.bell { cursor: pointer; display: flex; align-items: center; }
.user-info { cursor: pointer; display: flex; align-items: center; }
.aside { background: #fff; border-right: 1px solid #e4e7ed; }
.aside .el-menu { border-right: none; }
.main { padding: 20px; overflow-y: auto; }
</style>
