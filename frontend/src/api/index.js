// API 接口集中定义: 每个后端接口对应一个函数
import request from '../utils/request'

// ---------- 认证 ----------
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// ---------- 物资 ----------
export const getItems = (params) => request.get('/items', { params })
export const getItem = (id) => request.get(`/items/${id}`)
export const getCategories = () => request.get('/items/categories')
export const addItem = (data) => request.post('/items', data)
export const updateItem = (data) => request.put('/items', data)
export const deleteItem = (id) => request.delete(`/items/${id}`)

// ---------- 借用 ----------
export const applyBorrow = (data) => request.post('/borrow/apply', data)
export const myBorrows = (params) => request.get('/borrow/my', { params })
export const myRecords = (params) => request.get('/borrow/records', { params })
export const todoBorrows = (params) => request.get('/borrow/todo', { params })
export const allBorrows = (params) => request.get('/borrow/all', { params })
export const teacherApprove = (id, data) => request.post(`/borrow/${id}/teacher-approve`, data)
export const leaderApprove = (id, data) => request.post(`/borrow/${id}/leader-approve`, data)
export const warehouseConfirm = (id, data) => request.post(`/borrow/${id}/warehouse-confirm`, data)
export const takeBorrow = (id) => request.post(`/borrow/${id}/take`)
export const cancelBorrow = (id) => request.post(`/borrow/${id}/cancel`)

// ---------- 归还 ----------
export const applyReturn = (data) => request.post('/return/apply', data)
export const pendingReturns = (params) => request.get('/return/pending', { params })
export const myReturns = (params) => request.get('/return/my', { params })
export const confirmReturn = (id, data) => request.post(`/return/${id}/confirm`, data)
export const confirmCompensated = (id) => request.post(`/return/${id}/compensated`)

// ---------- 闲置上架 ----------
export const submitIdle = (data) => request.post('/idle', data)
export const pendingIdle = (params) => request.get('/idle/pending', { params })
export const myIdle = (params) => request.get('/idle/my', { params })
export const reviewIdle = (id, data) => request.post(`/idle/${id}/review`, data)

// ---------- 消息通知 ----------
export const getNotifications = (params) => request.get('/notifications', { params })
export const unreadCount = () => request.get('/notifications/unread-count')
export const readNotification = (id) => request.post(`/notifications/${id}/read`)
export const readAllNotifications = () => request.post('/notifications/read-all')

// ---------- 文件上传 ----------
export const uploadFile = (formData) => request.post('/files/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
