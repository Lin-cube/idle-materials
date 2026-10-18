<!-- 我的借用: 申请列表 + 领用/取消操作 -->
<template>
  <el-card shadow="never">
    <template #header><b>我的借用申请</b></template>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="requestNo" label="单号" width="190" />
      <el-table-column label="物资" min-width="140">
        <template #default="{ row }">{{ itemNames[row.itemId] || ('#' + row.itemId) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="60" />
      <el-table-column prop="purpose" label="用途" min-width="120" show-overflow-tooltip />
      <el-table-column label="借用期" width="190">
        <template #default="{ row }">{{ row.useStart }} ~ {{ row.useEnd }}</template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <!-- 仓库确认后可领用 -->
          <el-button v-if="row.status === 'WAREHOUSE_CONFIRMED'" type="success" size="small" @click="take(row)">确认领用</el-button>
          <!-- 待审批可取消 -->
          <el-button v-if="row.status === 'PENDING'" type="danger" plain size="small" @click="cancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total"
                   layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myBorrows, takeBorrow, cancelBorrow, getItems } from '../api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10 })
const itemNames = ref({})

// 状态文案与颜色
const statusLabel = (s) => ({
  PENDING: '待老师审批', WAIT_LEADER: '待领导审批', WAIT_WAREHOUSE: '待仓库确认',
  WAREHOUSE_CONFIRMED: '可领用', TAKEN: '已领用', REJECTED: '已驳回', CANCELLED: '已取消'
}[s] || s)
const statusType = (s) => ({
  PENDING: 'warning', WAIT_LEADER: 'warning', WAIT_WAREHOUSE: 'warning',
  WAREHOUSE_CONFIRMED: 'success', TAKEN: 'primary', REJECTED: 'danger', CANCELLED: 'info'
}[s] || 'info')

// 加载我的申请
const load = async () => {
  loading.value = true
  try {
    const res = await myBorrows(query.value)
    rows.value = res.data.records
    total.value = Number(res.data.total)
    // 批量查物资名称用于显示
    const ids = [...new Set(rows.value.map(r => r.itemId))]
    if (ids.length) {
      const itemsRes = await getItems({ pageNum: 1, pageSize: 100 })
      itemsRes.data.records.forEach(i => { itemNames.value[i.id] = i.name })
    }
  } finally {
    loading.value = false
  }
}

// 确认领用(线下完成证件抵押后点击)
const take = async (row) => {
  await ElMessageBox.confirm('确认已到仓库完成证件抵押并领走物资?', '确认领用')
  await takeBorrow(row.id)
  ElMessage.success('领用成功, 请按时归还')
  load()
}

// 取消申请
const cancel = async (row) => {
  await ElMessageBox.confirm('确认取消该申请?', '提示')
  await cancelBorrow(row.id)
  ElMessage.success('已取消')
  load()
}

onMounted(load)
</script>
