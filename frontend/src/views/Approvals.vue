<!-- 审批中心: 老师/领导/仓库 按角色处理待办申请 -->
<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <b>待我处理 ({{ roleTitle }})</b>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="requestNo" label="单号" width="190" />
      <el-table-column label="物资" min-width="130">
        <template #default="{ row }">{{ itemNames[row.itemId] || ('#' + row.itemId) }}</template>
      </el-table-column>
      <el-table-column label="申请人" width="90">
        <template #default="{ row }">{{ userNames[row.applicantId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" width="70">
        <template #default="{ row }">
          <el-tag size="small" :type="row.applicantType === 'TEACHER' ? 'danger' : 'primary'">
            {{ row.applicantType === 'TEACHER' ? '教师' : '学生' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="purpose" label="用途" min-width="120" show-overflow-tooltip />
      <el-table-column label="借用期" width="180">
        <template #default="{ row }">{{ row.useStart }} ~ {{ row.useEnd }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <!-- 老师审批 / 领导审批 -->
          <template v-if="['PENDING', 'WAIT_LEADER'].includes(row.status)">
            <el-button type="success" size="small" @click="approve(row, true)">通过</el-button>
            <el-button type="danger" size="small" @click="approve(row, false)">驳回</el-button>
          </template>
          <!-- 仓库确认 -->
          <el-button v-if="row.status === 'WAIT_WAREHOUSE'" type="warning" size="small" @click="confirm(row)">
            仓库确认
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total"
                   layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { todoBorrows, teacherApprove, leaderApprove, warehouseConfirm, getItems } from '../api'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10 })
const itemNames = ref({})
const userNames = ref({})

// 角色对应的环节标题
const roleTitle = computed(() => ({
  TEACHER: '老师审批', ASSET: '资产中心老师审批', LEADER: '领导审批', WAREHOUSE: '仓库线下确认'
}[store.role] || '审批'))

const load = async () => {
  loading.value = true
  try {
    const res = await todoBorrows(query.value)
    rows.value = res.data.records
    total.value = Number(res.data.total)
    // 补充物资/用户名称用于展示
    const itemsRes = await getItems({ pageNum: 1, pageSize: 100 })
    itemsRes.data.records.forEach(i => { itemNames.value[i.id] = i.name })
    rows.value.forEach(r => { userNames.value[r.applicantId] = r.applicantId })
  } finally {
    loading.value = false
  }
}

// 审批(通过/驳回), 自动区分老师和领导接口
const approve = async (row, pass) => {
  const { value } = await ElMessageBox.prompt(pass ? '审批意见(选填)' : '驳回原因(必填)', pass ? '通过申请' : '驳回申请', {
    inputValue: pass ? '同意借用' : ''
  })
  const body = { approve: pass, remark: value || (pass ? '同意' : '不符合借用条件') }
  if (row.status === 'WAIT_LEADER') {
    await leaderApprove(row.id, body)
  } else {
    await teacherApprove(row.id, body)
  }
  ElMessage.success('处理完成')
  load()
}

// 仓库确认: 登记证件抵押信息
const confirm = async (row) => {
  const { value } = await ElMessageBox.prompt('证件抵押说明(如: 学生证一张)', '仓库确认 - 线下核对实物', {
    inputValue: '学生证一张'
  })
  await warehouseConfirm(row.id, { remark: '实物已核对', depositDesc: value })
  ElMessage.success('已确认, 通知申请人领用')
  load()
}

onMounted(load)
</script>
