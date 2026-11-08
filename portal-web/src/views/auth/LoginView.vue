<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="brand">
        <span class="brand-mark">云</span>
        <span>云枢智学</span>
      </router-link>

      <h1>欢迎回来</h1>
      <p class="subtitle">登录后继续你的学习与面试训练</p>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="密码登录" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
            <el-form-item label="手机号 / 邮箱" prop="account">
              <el-input v-model="passwordForm.account" placeholder="请输入手机号或邮箱" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                @keyup.enter="handlePasswordLogin"
              />
            </el-form-item>
            <div class="form-extra">
              <router-link to="/forgot-password">忘记密码？</router-link>
            </div>
            <el-button type="primary" class="submit" :loading="loading" @click="handlePasswordLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="验证码登录" name="sms">
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" label-position="top">
            <el-form-item label="手机号" prop="mobile">
              <el-input v-model="smsForm.mobile" placeholder="请输入手机号" clearable />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <div class="code-row">
                <el-input v-model="smsForm.code" placeholder="6 位验证码" maxlength="6" />
                <el-button :disabled="countdown > 0" @click="handleSendCode">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="handleSmsLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { loginByPassword, loginBySms, sendSmsCode } from '@/api/auth'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'password' | 'sms'>('password')
const loading = ref(false)
const countdown = ref(0)
let timer: number | undefined

const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({ account: '', password: '' })
const passwordRules: FormRules = {
  account: [{ required: true, message: '请输入手机号或邮箱', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }]
}

const smsFormRef = ref<FormInstance>()
const smsForm = reactive({ mobile: '', code: '' })
const smsRules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, min: 6, max: 6, message: '请输入 6 位验证码', trigger: 'blur' }]
}

async function validate(formRef: FormInstance | undefined) {
  if (!formRef) return false
  try {
    await formRef.validate()
    return true
  } catch {
    return false
  }
}

function goNext() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  router.replace(redirect)
}

async function handlePasswordLogin() {
  if (!(await validate(passwordFormRef.value))) return
  loading.value = true
  try {
    const result = await loginByPassword({ ...passwordForm })
    userStore.setTokens(result.accessToken, result.refreshToken)
    ElMessage.success('登录成功')
    goNext()
  } finally {
    loading.value = false
  }
}

async function handleSmsLogin() {
  if (!(await validate(smsFormRef.value))) return
  loading.value = true
  try {
    const result = await loginBySms({ ...smsForm })
    userStore.setTokens(result.accessToken, result.refreshToken)
    ElMessage.success('登录成功')
    goNext()
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.mobile)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  if (countdown.value > 0) return
  await sendSmsCode({ mobile: smsForm.mobile, type: 'login' })
  ElMessage.success('验证码已发送')
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) {
      window.clearInterval(timer)
      timer = undefined
    }
  }, 1000)
}

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 40px 20px;
  background:
    radial-gradient(circle at 15% 20%, rgba(37, 99, 235, 0.12), transparent 40%),
    radial-gradient(circle at 85% 80%, rgba(124, 58, 237, 0.12), transparent 40%),
    $yszx-bg;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 36px;
  background: $yszx-surface;
  border: 1px solid $yszx-border;
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.1);

  h1 {
    margin: 24px 0 6px;
    font-size: 26px;
  }
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #fff;
  background: linear-gradient(135deg, $yszx-primary, #7c3aed);
  border-radius: 10px;
}

.subtitle {
  margin: 0 0 20px;
  color: $yszx-text-secondary;
  font-size: 14px;
}

.form-extra {
  display: flex;
  justify-content: flex-end;
  margin: -6px 0 14px;
  font-size: 13px;
  color: $yszx-primary;
}

.code-row {
  display: flex;
  gap: 8px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.submit {
  width: 100%;
  height: 42px;
}

.auth-footer {
  margin-top: 22px;
  color: $yszx-text-secondary;
  font-size: 14px;
  text-align: center;

  a {
    color: $yszx-primary;
    font-weight: 600;
  }
}
</style>
