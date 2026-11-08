<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="brand">
        <span class="brand-mark">云</span>
        <span>云枢智学</span>
      </router-link>

      <h1>找回密码</h1>
      <p class="subtitle">
        {{ step === 1 ? '验证手机号后设置新密码' : '验证通过，请设置新密码' }}
      </p>

      <el-steps :active="step" finish-status="success" simple class="steps">
        <el-step title="验证手机号" />
        <el-step title="设置新密码" />
      </el-steps>

      <el-form v-if="step === 1" ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-position="top">
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="verifyForm.mobile" placeholder="请输入注册手机号" clearable />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="verifyForm.code" placeholder="6 位验证码" maxlength="6" />
            <el-button :disabled="countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="handleVerify">
          下一步
        </el-button>
      </el-form>

      <el-form v-else ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="6~20 位新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            @keyup.enter="handleReset"
          />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="handleReset">
          重置密码
        </el-button>
      </el-form>

      <div class="auth-footer">
        想起密码了？
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { resetPassword, sendSmsCode, verifyResetCode } from '@/api/auth'

const router = useRouter()

const step = ref(1)
const loading = ref(false)
const countdown = ref(0)
const resetToken = ref('')
let timer: number | undefined

const verifyFormRef = ref<FormInstance>()
const verifyForm = reactive({ mobile: '', code: '' })
const verifyRules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, min: 6, max: 6, message: '请输入 6 位验证码', trigger: 'blur' }]
}

const resetFormRef = ref<FormInstance>()
const resetForm = reactive({ newPassword: '', confirmPassword: '' })
const resetRules: FormRules = {
  newPassword: [{ required: true, min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        if (!value) callback(new Error('请再次输入新密码'))
        else if (value !== resetForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function handleSendCode() {
  if (!/^1[3-9]\d{9}$/.test(verifyForm.mobile)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  if (countdown.value > 0) return
  await sendSmsCode({ mobile: verifyForm.mobile, type: 'reset' })
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

async function handleVerify() {
  if (!verifyFormRef.value) return
  const valid = await verifyFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await verifyResetCode({ ...verifyForm })
    resetToken.value = result.resetToken
    step.value = 2
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  if (!resetFormRef.value) return
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPassword({
      resetToken: resetToken.value,
      newPassword: resetForm.newPassword
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    router.replace('/login')
  } finally {
    loading.value = false
  }
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

.steps {
  margin-bottom: 24px;
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
