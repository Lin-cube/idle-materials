<!-- 闲置审核: 仓库老师审核各部门提交的闲置物资 -->
<template>
  <el-card shadow="never">
    <template #header><b>待审核闲置物资</b></template>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="物资名称" min-width="140" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="quantity" label="数量" width="70" />
      <el-table-column label="照片" width="90">
        <template #default="{ row }">
          <el-image v-if="row.image" :src="row.image" :preview-src-list="[row.image]"
                    style="width: 60px; height: 60px" fit="cover" />
          <span v-else style="color: #999; font-size: 12px">无图</span>
        </template>
      </el-table-column>
      <el-table-column label="联系人" width="90">
        <template #default="{ row }">{{ row.contactName || '-' }}</template>
      </el-table-column>
      <el-table-column label="联系电话" width="120">
        <template #default="{ row }">{{ row.contactPhone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="140" show-overflow-tooltip />
      <el-table-column prop="location" label="存放地点" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="review(row, true)">通过上架</el-button>
          <el-button type="danger" size="small" @click="review(row, false)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && rows.length === 0" description="暂无待审核的闲置物资" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pendingIdle, reviewIdle } from '../api'

const loading = ref(false)
const rows = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await pendingIdle({ pageNum: 1, pageSize: 50 })
    rows.value = res.data.records
  } finally {
    loading.value = false
  }
}

// 审核: 通过后自动生成物资进入物资大厅
const review = async (row, pass) => {
  const { value } = await ElMessageBox.prompt('审核意见(选填)', pass ? '通过上架' : '驳回', {})
  await reviewIdle(row.id, { pass, remark: value || '' })
  ElMessage.success(pass ? '已通过, 物资已上架到物资大厅' : '已驳回')
  load()
}

onMounted(load)
</script>
