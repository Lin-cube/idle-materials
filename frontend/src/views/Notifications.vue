<!-- 消息通知: 列表 + 未读标记 -->
<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <b>消息通知</b>
        <el-button size="small" @click="readAll">全部已读</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="rows" stripe @row-click="onRowClick">
      <el-table-column width="50">
        <template #default="{ row }">
          <el-badge is-dot :hidden="row.isRead === 1"><el-icon><ChatDotRound /></el-icon></el-badge>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="220">
        <template #default="{ row }">
          <b :style="{ fontWeight: row.isRead === 0 ? 600 : 400 }">{{ row.title }}</b>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="typeColor(row.type)">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total"
                   layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifications, readNotification, readAllNotifications } from '../api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 15 })

const typeLabel = (t) => ({ SYSTEM: '系统', APPROVE: '审批', RETURN: '归还', OVERDUE: '逾期' }[t] || t)
const typeColor = (t) => ({ SYSTEM: 'info', APPROVE: 'primary', RETURN: 'success', OVERDUE: 'danger' }[t] || 'info')

const load = async () => {
  loading.value = true
  try {
    const res = await getNotifications(query.value)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

// 点击行标记已读
const onRowClick = async (row) => {
  if (row.isRead === 0) {
    await readNotification(row.id)
    row.isRead = 1
  }
}

// 全部已读
const readAll = async () => {
  await readAllNotifications()
  ElMessage.success('已全部标记为已读')
  load()
}

onMounted(load)
</script>
