// 应用入口: 创建Vue实例, 注册路由/状态管理/Element Plus
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 创建应用实例
const app = createApp(App)

// 全局注册所有Element Plus图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册插件
app.use(createPinia())      // 状态管理
app.use(router)             // 路由
app.use(ElementPlus, { locale: zhCn })   // UI组件库(中文)

// 挂载到 #app
app.mount('#app')
