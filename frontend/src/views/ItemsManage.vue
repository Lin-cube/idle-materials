<!-- 物资管理: 仓库管理员增删改物资 -->
<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <b>物资管理</b>
        <el-button type="primary" size="small" @click="openAdd">新增物资</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="assetNo" label="资产编号" width="130" />
      <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="90" />
      <el-table-column prop="price" label="单价" width="90" />
      <el-table-column prop="unit" label="单位" width="60" />
      <el-table-column prop="dept" label="管理部门" min-width="130" show-overflow-tooltip />
      <el-table-column prop="location" label="存放地点" min-width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['success', 'danger', 'warning', 'info'][row.status]" size="small">
            {{ ['在库可借', '已借出', '维修中', '已下架'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total"
                   layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editRow ? '编辑物资' : '新增物资'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="资产编号" required>
          <el-input v-model="form.assetNo" :disabled="!!editRow" placeholder="如 20260004SS" />
        </el-form-item>
        <el-form-item label="物资名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in ['家具', '电子设备', '搬运设备', '办公设备', '其他']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价/单位">
          <el-input-number v-model="form.price" :min="0" style="width: 140px" />
          <el-input v-model="form.unit" style="width: 80px; margin-left: 8px" placeholder="台/张" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="管理部门">
          <el-input v-model="form.dept" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.ownerName" placeholder="该物资对接人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.ownerPhone" placeholder="11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="存放地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="物资图片">
          <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*">
            <el-button type="primary" plain size="small">上传图片</el-button>
          </el-upload>
          <div v-if="form.image" style="width: 100%; margin-top: 8px">
            <el-image :src="form.image" style="width: 100px; height: 100px" fit="cover" />
          </div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getItems, addItem, updateItem, deleteItem, uploadFile } from '../api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const editRow = ref(null)

const emptyForm = {
  assetNo: '', name: '', category: '家具', price: 0, unit: '件',
  model: '', dept: '', ownerName: '', ownerPhone: '', location: '', image: '', description: ''
}
const form = reactive({ ...emptyForm })

const load = async () => {
  loading.value = true
  try {
    const res = await getItems(query.value)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editRow.value = null
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

const openEdit = (row) => {
  editRow.value = row
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  if (!form.assetNo || !form.name) { ElMessage.warning('请填写资产编号和名称'); return }
  if (editRow.value) {
    await updateItem(form)
    ElMessage.success('修改成功')
  } else {
    await addItem(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除「${row.name}」?`, '删除确认', { type: 'warning' })
  await deleteItem(row.id)
  ElMessage.success('删除成功')
  load()
}

const doUpload = async (options) => {
  const fd = new FormData()
  fd.append('file', options.file)
  const res = await uploadFile(fd)
  form.image = res.data.url
}

onMounted(load)
</script>
