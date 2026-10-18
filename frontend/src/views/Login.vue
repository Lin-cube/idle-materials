<!-- 登录页 -->
<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1 class="title">🏫 校园闲置物资管理平台</h1>
      <p class="subtitle">让闲置物资流动起来</p>
      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="学号/工号" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
        <div class="links">
          <router-link to="/register">没有账号? 去注册</router-link>
        </div>
      </el-form>
      <!-- 演示账号提示 -->
      <el-alert type="info" :closable="false" style="margin-top: 16px">
        <p>演示账号(密码均为 123456):</p>
        <p>student学生 / teacher教师 / warehouse仓库 / asset资产中心 / leader领导</p>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

// 登录
const onLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    store.setLogin(res.data.token, res.data.user)
    ElMessage.success('欢迎回来, ' + res.data.user.realName)
    router.push('/')
  } finally {
    loading.value = false
  }
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
.login-card { width: 400px; padding: 12px 8px; }
.title { text-align: center; font-size: 20px; margin-bottom: 4px; }
.subtitle { text-align: center; color: #999; margin-bottom: 24px; font-size: 13px; }
.links { text-align: center; margin-top: 12px; }
.links a { color: #409eff; text-decoration: none; font-size: 13px; }
</style>
