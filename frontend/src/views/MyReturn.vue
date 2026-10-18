<!-- 归还管理: 借用中的记录 -> 发起归还; 归还历史查看 -->
<template>
  <div>
    <!-- 借用中的记录 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header><b>借用中(可发起归还)</b></template>
      <el-table :data="borrowing" stripe>
        <el-table-column prop="requestNo" label="单号" width="190" />
        <el-table-column label="物资" min-width="140">
          <template #default="{ row }">{{ itemNames[row.itemId] || ('#' + row.itemId) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="60" />
        <el-table-column prop="dueDate" label="应还日期" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OVERDUE' ? 'danger' : 'primary'" size="small">
              {{ row.status === 'OVERDUE' ? '已逾期' : '借出中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" size="small" @click="openReturn(row)">发起归还</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 归还历史 -->
    <el-card shadow="never">
      <template #header><b>归还记录</b></template>
      <el-table :data="returns" stripe>
        <el-table-column prop="id" label="核对单号" width="90" />
        <el-table-column prop="returnQty" label="归还数量" width="90" />
        <el-table-column label="损坏情况" width="100">
          <template #default="{ row }">
            <el-tag :type="damageType(row.damage)" size="small">{{ damageLabel(row.damage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="赔偿金额" width="100">
          <template #default="{ row }">¥{{ row.compensate }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'CONFIRMED' ? (row.compensated === 1 ? '已赔偿完成' : '已核对') : '待仓库核对' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" min-width="150" />
      </el-table>
    </el-card>

    <!-- 发起归还弹窗 -->
    <el-dialog v-model="returnVisible" title="发起归还" width="520px">
      <el-form label-width="100px">
        <el-form-item label="归还数量">
          <el-input-number v-model="returnForm.returnQty" :min="1" :max="currentRow?.quantity || 1" />
          <span style="margin-left: 8px; color: #999; font-size: 12px">须与借出数量一致</span>
        </el-form-item>
        <el-form-item label="实物拍照">
          <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*">
            <el-button size="small" type="primary" plain>上传实物照片</el-button>
          </el-upload>
          <div v-if="returnForm.photoUrls" style="width: 100%; margin-top: 8px">
            <el-image :src="returnForm.photoUrls" style="width: 80px; height: 80px" fit="cover" />
          </div>
        </el-form-item>
        <el-form-item label="损坏情况">
          <el-radio-group v-model="returnForm.damage">
            <el-radio value="NONE">完好</el-radio>
            <el-radio value="DAMAGED">损坏</el-radio>
            <el-radio value="LOST">丢失</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="returnForm.damage !== 'NONE'" label="情况说明">
          <el-input v-model="returnForm.damageDesc" type="textarea" :rows="2"
                    placeholder="描述损坏/丢失情况" />
        </el-form-item>
        <el-alert type="info" :closable="false"
                  title="请把物资送到仓库, 仓库老师核对实物后完成归还; 损坏/丢失将按学校规定赔偿" />
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReturn">提交归还</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { myRecords, myReturns, applyReturn, uploadFile, getItems } from '../api'

const borrowing = ref([])
const returns = ref([])
const itemNames = ref({})
const returnVisible = ref(false)
const currentRow = ref(null)
const returnForm = reactive({ returnQty: 1, photoUrls: '', damage: 'NONE', damageDesc: '' })

const damageLabel = (d) => ({ NONE: '完好', DAMAGED: '损坏', LOST: '丢失' }[d] || d)
const damageType = (d) => ({ NONE: 'success', DAMAGED: 'warning', LOST: 'danger' }[d] || 'info')

// 加载借用中记录和归还历史
// 注意: 这里是"借用记录"(record), 不是"申请单"(request), 两者状态体系不同
const load = async () => {
  const [recordRes, returnRes] = await Promise.all([
    myRecords({ pageNum: 1, pageSize: 50 }),
    myReturns({ pageNum: 1, pageSize: 50 })
  ])
  borrowing.value = recordRes.data.records.filter(r => ['BORROWED', 'OVERDUE'].includes(r.status))
  returns.value = returnRes.data.records
  // 物资名称映射
  const itemsRes = await getItems({ pageNum: 1, pageSize: 100 })
  itemsRes.data.records.forEach(i => { itemNames.value[i.id] = i.name })
}

// 打开归还弹窗
const openReturn = (row) => {
  currentRow.value = row
  returnForm.returnQty = row.quantity
  returnForm.photoUrls = ''
  returnForm.damage = 'NONE'
  returnForm.damageDesc = ''
  returnVisible.value = true
}

// 自定义上传: 调后端上传接口拿URL
const doUpload = async (options) => {
  const fd = new FormData()
  fd.append('file', options.file)
  const res = await uploadFile(fd)
  returnForm.photoUrls = res.data.url
  ElMessage.success('照片上传成功')
}

// 提交归还
const submitReturn = async () => {
  await applyReturn({ recordId: currentRow.value.id, ...returnForm })
  ElMessage.success('归还申请已提交')
  returnVisible.value = false
  load()
}

onMounted(load)
</script>
