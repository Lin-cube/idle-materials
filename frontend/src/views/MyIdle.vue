<!-- 闲置上架: 各部门提交闲置物资, 查看审核进度 -->
<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header><b>提交闲置物资上架</b></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 560px">
        <el-form-item label="物资名称" prop="name">
          <el-input v-model="form.name" placeholder="如: 白色美式档案柜" />
        </el-form-item>
        <el-form-item label="物资分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in ['家具', '电子设备', '搬运设备', '办公设备', '其他']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="该物资的对接人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="存放地点">
          <el-input v-model="form.location" placeholder="如: 国教院办公室" />
        </el-form-item>
        <el-form-item label="物资简介">
          <el-input v-model="form.description" type="textarea" :rows="2"
                    placeholder="描述物资状态、成色, 让其他部门了解能否使用" />
        </el-form-item>
        <el-form-item label="物资照片" prop="image">
          <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*">
            <el-button type="primary" plain size="small">上传照片(必填)</el-button>
          </el-upload>
          <div v-if="form.image" style="width: 100%; margin-top: 8px">
            <el-image :src="form.image" style="width: 100px; height: 100px" fit="cover" />
          </div>
        </el-form-item>
        <el-button type="primary" @click="submit">提交审核</el-button>
        <span style="margin-left: 12px; color: #999; font-size: 12px">提交后由仓库老师审核, 通过后上架到物资大厅</span>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><b>我的提交记录</b></template>
      <el-table :data="rows" stripe>
        <el-table-column prop="name" label="物资名称" min-width="140" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="联系人" width="90">
          <template #default="{ row }">{{ row.contactName || '-' }}</template>
        </el-table-column>
        <el-table-column label="联系电话" width="120">
          <template #default="{ row }">{{ row.contactPhone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PASS' ? 'success' : row.status === 'REJECT' ? 'danger' : 'warning'" size="small">
              {{ { PENDING: '待审核', PASS: '已上架', REJECT: '已驳回' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="审核意见" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitIdle, myIdle, uploadFile } from '../api'

const formRef = ref()
const form = reactive({
  name: '', category: '家具', quantity: 1, contactName: '', contactPhone: '',
  location: '', description: '', image: ''
})
const rows = ref([])

// 必填校验规则: 名称/联系人/联系电话/图片
const rules = {
  name: [{ required: true, message: '请输入物资名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请填写正确的11位手机号', trigger: 'blur' }
  ],
  image: [{ required: true, message: '请上传物资照片', trigger: 'change' }]
}

// 提交上架申请
const submit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    await submitIdle(form)
    ElMessage.success('提交成功, 请等待仓库老师审核')
    // 重置表单
    Object.assign(form, { name: '', category: '家具', quantity: 1, contactName: '', contactPhone: '', location: '', description: '', image: '' })
    load()
  })
}

// 上传照片
const doUpload = async (options) => {
  const fd = new FormData()
  fd.append('file', options.file)
  const res = await uploadFile(fd)
  form.image = res.data.url
}

// 加载我的提交
const load = async () => {
  const res = await myIdle({ pageNum: 1, pageSize: 50 })
  rows.value = res.data.records
}

onMounted(load)
</script>
