<!-- 归还核对: 仓库老师核对归还申请 -->
<template>
  <el-card shadow="never">
    <template #header><b>待核对归还单</b></template>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="核对单" width="80" />
      <el-table-column prop="recordId" label="借用记录" width="90" />
      <el-table-column prop="returnQty" label="归还数量" width="90" />
      <el-table-column label="实物照片" width="100">
        <template #default="{ row }">
          <el-image v-if="row.photoUrls" :src="row.photoUrls.split(',')[0]"
                    :preview-src-list="row.photoUrls.split(',')" style="width: 60px; height: 60px" fit="cover" />
          <span v-else style="color: #999; font-size: 12px">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="损坏情况" width="90">
        <template #default="{ row }">
          <el-tag :type="{ NONE: 'success', DAMAGED: 'warning', LOST: 'danger' }[row.damage]" size="small">
            {{ { NONE: '完好', DAMAGED: '损坏', LOST: '丢失' }[row.damage] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="damageDesc" label="说明" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <!-- 完好: 直接核对确认 -->
          <el-button v-if="row.damage === 'NONE'" type="success" size="small" @click="confirmOk(row)">核对通过</el-button>
          <!-- 损坏/丢失: 登记赔偿 -->
          <template v-else>
            <el-button type="warning" size="small" @click="confirmDamage(row)">登记赔偿</el-button>
            <el-button type="primary" size="small" @click="markCompensated(row)">赔偿已缴</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && rows.length === 0" description="暂无待核对的归还单" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pendingReturns, confirmReturn, confirmCompensated } from '../api'

const loading = ref(false)
const rows = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await pendingReturns({ pageNum: 1, pageSize: 50 })
    rows.value = res.data.records
  } finally {
    loading.value = false
  }
}

// 完好: 核对通过(库存回库, 通知取回证件)
const confirmOk = async (row) => {
  await ElMessageBox.confirm('确认实物数量核对无误、状态完好?', '核对通过')
  await confirmReturn(row.id, { remark: '实物核对无误' })
  ElMessage.success('核对完成, 物资已回库')
  load()
}

// 损坏/丢失: 登记赔偿金额(按学校规定)
const confirmDamage = async (row) => {
  const { value } = await ElMessageBox.prompt(
    '按学校规定填写赔偿金额(元)', row.damage === 'LOST' ? '丢失赔偿' : '损坏赔偿',
    { inputPattern: /^\d+(\.\d{1,2})?$/, inputErrorMessage: '请输入正确金额' }
  )
  await confirmReturn(row.id, { remark: '需赔偿', compensate: value })
  ElMessage.success('已登记赔偿金额, 等待借用人赔偿')
  load()
}

// 赔偿已缴: 关闭流程
const markCompensated = async (row) => {
  await ElMessageBox.confirm('确认借用人已缴清赔偿并完成流程?', '确认赔偿')
  await confirmCompensated(row.id)
  ElMessage.success('赔偿流程已完成')
  load()
}

onMounted(load)
</script>
