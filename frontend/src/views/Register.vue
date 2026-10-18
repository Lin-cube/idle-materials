<!-- 注册页 -->
<template>
  <div class="login-page">
    <el-card class="reg-card">
      <h1 class="title">注册账号</h1>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="学号/工号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="部门/社团">
          <el-input v-model="form.dept" placeholder="如: 机器人社团" />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="onRegister">注 册</el-button>
        <div class="links"><router-link to="/login">已有账号? 去登录</router-link></div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '', realName: '', phone: '', dept: '' })

// 表单校验规则
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

// 提交注册
const onRegister = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await register(form)
      ElMessage.success('注册成功, 请登录')
      router.push('/login')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.reg-card { width: 440px; padding: 12px 8px; }
.title { text-align: center; font-size: 18px; margin-bottom: 20px; }
.links { text-align: center; margin-top: 12px; }
.links a { color: #409eff; text-decoration: none; font-size: 13px; }
</style>
