// 路由配置: 页面路径与组件映射 + 登录守卫
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),   // 带侧边栏的主布局
    children: [
      { path: '', redirect: '/hall' },
      { path: 'hall', name: 'Hall', component: () => import('../views/Hall.vue'), meta: { title: '物资大厅' } },
      { path: 'my-borrow', name: 'MyBorrow', component: () => import('../views/MyBorrow.vue'), meta: { title: '我的借用' } },
      { path: 'my-return', name: 'MyReturn', component: () => import('../views/MyReturn.vue'), meta: { title: '归还管理' } },
      { path: 'my-idle', name: 'MyIdle', component: () => import('../views/MyIdle.vue'), meta: { title: '闲置上架' } },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/Notifications.vue'), meta: { title: '消息通知' } },
      { path: 'approvals', name: 'Approvals', component: () => import('../views/Approvals.vue'), meta: { title: '审批中心' } },
      { path: 'return-check', name: 'ReturnCheck', component: () => import('../views/ReturnCheck.vue'), meta: { title: '归还核对' } },
      { path: 'idle-review', name: 'IdleReview', component: () => import('../views/IdleReview.vue'), meta: { title: '闲置审核' } },
      { path: 'items-manage', name: 'ItemsManage', component: () => import('../views/ItemsManage.vue'), meta: { title: '物资管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫: 未登录一律跳转登录页
router.beforeEach((to) => {
  const store = useUserStore()
  if (to.path !== '/login' && to.path !== '/register' && !store.isLoggedIn) {
    return '/login'
  }
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '校园闲置物资管理平台'
})

export default router
