// Vite 配置: Vue插件 + 开发服务器代理
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // Vue3 单文件组件支持
  plugins: [vue()],
  server: {
    port: 5173,               // 前端开发服务器端口
    // 代理: 所有 /api 和 /files 请求转发到后端8081, 避免跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
