// Pinia 用户状态管理: 登录信息在页面间共享
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    // 从localStorage恢复, 刷新页面不丢失
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    // 是否已登录
    isLoggedIn: (state) => !!state.token,
    // 当前角色快捷访问
    role: (state) => state.user?.role || ''
  },
  actions: {
    // 登录成功后保存
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    // 退出登录
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
