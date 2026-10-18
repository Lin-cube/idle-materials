<!-- 物资大厅: 浏览所有可借物资, 发起借用申请 -->
<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <div class="bar">
        <el-input v-model="query.keyword" placeholder="搜索物资名称/资产编号" style="width: 260px" clearable @keyup.enter="load" />
        <el-select v-model="query.category" placeholder="全部分类" style="width: 150px" clearable @change="load">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="load">搜索</el-button>
        <el-checkbox v-model="showAll" @change="load" style="margin-left: 8px">显示已借出</el-checkbox>
        <span style="color: #999; font-size: 12px">物资借出后自动下架, 归还核对后自动重新上架</span>
      </div>
    </el-card>

    <!-- 物资卡片网格 -->
    <div v-loading="loading" class="grid">
      <el-card v-for="item in items" :key="item.id" class="item-card" shadow="hover">
        <!-- 物资图片(无图显示占位图标) -->
        <div class="img-box">
          <img v-if="item.image" :src="item.image" class="img" />
          <el-icon v-else :size="48" color="#c0c4cc"><Box /></el-icon>
        </div>
        <div class="info">
          <div class="name">{{ item.name }}</div>
          <div class="meta">编号: {{ item.assetNo }}</div>
          <div class="meta">存放: {{ item.location || '-' }}</div>
          <div class="meta">联系人: {{ item.ownerName || '-' }} {{ item.ownerPhone || '' }}</div>
          <div class="row">
            <el-tag v-if="item.status === 0" type="success" size="small">在库可借</el-tag>
            <el-tag v-else-if="item.status === 1" type="danger" size="small">已借出</el-tag>
            <el-tag v-else-if="item.status === 2" type="warning" size="small">维修中</el-tag>
            <el-tag v-else type="info" size="small">已下架</el-tag>
            <el-tag size="small" style="margin-left: 6px">{{ item.category }}</el-tag>
          </div>
          <el-button type="primary" size="small" style="width: 100%; margin-top: 10px"
                     :disabled="item.status !== 0" @click="openApply(item)">
            申请借用
          </el-button>
        </div>
      </el-card>
      <el-empty v-if="!loading && items.length === 0" description="没有找到物资" style="grid-column: 1/-1" />
    </div>

    <!-- 分页 -->
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize"
                   :total="total" layout="total, prev, pager, next" style="margin-top: 16px; justify-content: center"
                   @current-change="load" />

    <!-- 借用申请弹窗 -->
    <el-dialog v-model="applyVisible" :title="'申请借用 - ' + (currentItem?.name || '')" width="500px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="申请数量">
          <el-input-number v-model="applyForm.quantity" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="借用用途" required>
          <el-input v-model="applyForm.purpose" type="textarea" :rows="2"
                    placeholder="如: 社团活动使用 / 教学实践" />
        </el-form-item>
        <el-form-item label="借用日期" required>
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始日期" end-placeholder="归还日期" />
        </el-form-item>
        <el-alert type="info" :closable="false" title="提示: 提交后由物资负责老师审批, 学生需到仓库完成证件抵押后领用" />
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getItems, getCategories, applyBorrow } from '../api'

const loading = ref(false)
const items = ref([])
const categories = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 12, keyword: '', category: '' })

// 申请相关
const applyVisible = ref(false)
const submitting = ref(false)
const currentItem = ref(null)
const dateRange = ref([])
const applyForm = reactive({ quantity: 1, purpose: '' })

// 是否显示已借出的物资(默认否: 借出即下架)
const showAll = ref(false)

// 加载物资列表
// available=true: 只显示在库可借的物资, 已借出的自动下架不再展示
const load = async () => {
  loading.value = true
  try {
    const res = await getItems({ ...query, available: !showAll.value })
    items.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

// 加载分类筛选项
const loadCategories = async () => {
  const res = await getCategories()
  categories.value = res.data
}

// 打开申请弹窗
const openApply = (item) => {
  currentItem.value = item
  applyForm.quantity = 1
  applyForm.purpose = ''
  dateRange.value = []
  applyVisible.value = true
}

// 提交申请
const submitApply = async () => {
  if (!applyForm.purpose) { ElMessage.warning('请填写借用用途'); return }
  if (!dateRange.value || dateRange.value.length !== 2) { ElMessage.warning('请选择借用日期'); return }
  submitting.value = true
  try {
    await applyBorrow({
      itemId: currentItem.value.id,
      quantity: applyForm.quantity,
      purpose: applyForm.purpose,
      useStart: dateRange.value[0],
      useEnd: dateRange.value[1]
    })
    ElMessage.success('申请已提交')
    applyVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(() => { load(); loadCategories() })
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.bar { display: flex; gap: 12px; align-items: center; }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.item-card :deep(.el-card__body) { padding: 0; }
.img-box {
  height: 140px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.img { width: 100%; height: 100%; object-fit: cover; }
.info { padding: 12px; }
.name { font-weight: 600; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { font-size: 12px; color: #999; margin-bottom: 4px; }
.row { margin-top: 6px; }
</style>
