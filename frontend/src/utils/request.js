// Axios 封装: 统一携带Token、统一处理响应和错误
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',            // 所有请求自动加 /api 前缀(经vite代理到后端)
  timeout: 10000              // 超时10秒
})

// 请求拦截器: 自动携带登录Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 响应拦截器: 统一处理业务码
request.interceptors.response.use(
  response => {
    const res = response.data
    // 后端Result结构: code=200成功, 401未登录, 其他失败
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      // 登录过期: 清除本地状态并跳转登录页
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.warning('请先登录')
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    }
    // 业务失败提示
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg))
  },
  error => {
    ElMessage.error('网络异常, 请检查后端服务是否启动')
    return Promise.reject(error)
  }
)

export default request
