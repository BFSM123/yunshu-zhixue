<template>
  <div class="login-page">
    <section class="visual">
      <span class="visual__mark" aria-hidden="true">云</span>
      <span class="visual__grid" aria-hidden="true"></span>
      <span class="visual__glow visual__glow--a" aria-hidden="true"></span>
      <span class="visual__glow visual__glow--b" aria-hidden="true"></span>
      <span class="visual__ring visual__ring--outer" aria-hidden="true"></span>
      <span class="visual__ring visual__ring--inner" aria-hidden="true"></span>

      <header class="brand">
        <span class="brand-mark">云</span>
        <div class="brand-copy">
          <span class="brand-name">云枢智学</span>
          <span class="brand-sub">YUNSHU ZHIXUE</span>
        </div>
      </header>

      <div class="visual__content">
        <p class="eyebrow">AI 智能面试训练平台</p>
        <h1>让每一次<br /><em>训练</em>都有回响。</h1>
        <p class="lead">
          从题库、课程到 AI 模拟面试，在一个控制台里管理内容、学员与数据。
        </p>

        <ul class="features">
          <li>
            <span class="feature-index">01</span>
            <div>
              <strong>内容中枢</strong>
              <p>学科、题库、课程与学习路线统一编排</p>
            </div>
          </li>
          <li>
            <span class="feature-index">02</span>
            <div>
              <strong>学员洞察</strong>
              <p>学习记录与面试表现一屏掌握</p>
            </div>
          </li>
          <li>
            <span class="feature-index">03</span>
            <div>
              <strong>安全可控</strong>
              <p>RBAC 权限体系与全程操作审计</p>
            </div>
          </li>
        </ul>
      </div>

      <footer class="visual__footer">
        <span>管理端 · 内部系统</span>
        <span>© 2026 云枢智学</span>
      </footer>
    </section>

    <section class="stage">
      <div class="paper">
        <div class="form-card">
          <div class="form-card__head">
            <span class="form-card__badge">ADMIN CONSOLE</span>
            <h2>登录后台</h2>
            <p>使用管理员账号继续</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
            <el-form-item label="账号" prop="account">
              <el-input
                v-model="form.account"
                size="large"
                placeholder="请输入用户名或手机号"
                autocomplete="username"
                clearable
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
                show-password
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-button type="primary" class="submit" :loading="loading" @click="handleLogin">
              <span>进入管理后台</span>
              <el-icon><Right /></el-icon>
            </el-button>
          </el-form>

          <div class="form-card__foot">
            <span class="status-dot"></span>
            后台账号与学员账号相互独立
          </div>
        </div>

        <p class="stage-note">仅限获得授权的内部人员访问</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, Right, User } from '@element-plus/icons-vue'
import { adminLogin } from '@/api/auth'
import { useAdminUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'

const route = useRoute()
const router = useRouter()
const userStore = useAdminUserStore()
const permissionStore = usePermissionStore()

const formRef = ref<FormInstance>()
const form = reactive({ account: '', password: '' })
const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }]
}

const loading = ref(false)

async function handleLogin() {
  if (loading.value || !formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await adminLogin({ account: form.account.trim(), password: form.password })
    userStore.setTokens(result.accessToken, result.refreshToken)
    const permissions = result.permissions ?? []
    const roleCode = result.roleCode ?? ''
    userStore.setProfile({
      id: 0,
      username: result.username || form.account,
      nickname: result.nickname,
      roleCode,
      permissions
    })
    permissionStore.setPermissions(permissions, roleCode)
    ElMessage.success('登录成功')
    await router.replace(safeRedirect(route.query.redirect))
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function safeRedirect(value: unknown): string {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
    return '/dashboard'
  }
  return value
}
</script>

<style scoped lang="scss">
.login-page {
  --ink: #070d1a;
  --ink-soft: #0c1628;
  --paper: #f3f1ec;
  --cobalt: #2563eb;
  --cyan: #22d3ee;
  --amber: #f5b942;

  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(420px, 0.88fr);
  min-height: 100%;
  background: var(--paper);
  overflow: hidden;
}

/* ---------- 左侧品牌区 ---------- */
.visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px 48px 30px;
  color: #f8fafc;
  background:
    radial-gradient(120% 80% at 12% 8%, rgba(37, 99, 235, 0.3), transparent 58%),
    radial-gradient(90% 70% at 82% 88%, rgba(34, 211, 238, 0.16), transparent 60%),
    linear-gradient(150deg, #0a1326 0%, var(--ink) 55%, #0b1729 100%);
  overflow: hidden;
  isolation: isolate;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)' opacity='0.45'/%3E%3C/svg%3E");
    opacity: 0.16;
    mix-blend-mode: soft-light;
    pointer-events: none;
  }
}

.visual__mark {
  position: absolute;
  right: -24px;
  bottom: 8px;
  z-index: 1;
  color: rgba(255, 255, 255, 0.028);
  font-family: 'Source Han Serif SC', 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: 300px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
  user-select: none;
}

.visual__grid {
  position: absolute;
  inset: 0;
  z-index: 1;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(circle at 28% 38%, #000 0%, transparent 74%);
  -webkit-mask-image: radial-gradient(circle at 28% 38%, #000 0%, transparent 74%);
}

.visual__glow {
  position: absolute;
  z-index: 1;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
}

.visual__glow--a {
  top: -110px;
  left: -90px;
  width: 360px;
  height: 360px;
  background: rgba(37, 99, 235, 0.55);
}

.visual__glow--b {
  right: 40px;
  bottom: -140px;
  width: 320px;
  height: 320px;
  background: rgba(14, 165, 233, 0.32);
}

.visual__ring {
  position: absolute;
  z-index: 1;
  border-radius: 50%;
  pointer-events: none;
}

.visual__ring--outer {
  top: 50%;
  right: -210px;
  width: 580px;
  height: 580px;
  border: 1px dashed rgba(125, 211, 252, 0.24);
  animation: spin-slow 90s linear infinite;

  &::before {
    content: '';
    position: absolute;
    top: 42px;
    left: 78px;
    width: 7px;
    height: 7px;
    background: var(--amber);
    border-radius: 50%;
    box-shadow: 0 0 18px rgba(245, 185, 66, 0.9);
  }
}

.visual__ring--inner {
  top: 50%;
  right: -90px;
  width: 350px;
  height: 350px;
  border: 1px solid rgba(245, 185, 66, 0.2);
  animation: spin-slow-reverse 60s linear infinite;
}

.brand {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  animation: fade-up 0.7s ease both;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  font-size: 17px;
  font-weight: 700;
  background: linear-gradient(140deg, #3b82f6, var(--cyan));
  border-radius: 11px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.brand-sub {
  color: #7dd3fc;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 9px;
  letter-spacing: 0.32em;
}

.visual__content {
  position: relative;
  z-index: 2;
  max-width: 560px;
  margin: 28px 0;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  color: #7dd3fc;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.24em;
  animation: fade-up 0.7s ease 0.1s both;

  &::before {
    content: '';
    width: 30px;
    height: 1px;
    background: #7dd3fc;
  }
}

h1 {
  margin: 20px 0 18px;
  font-family: 'Source Han Serif SC', 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: clamp(40px, 4.6vw, 62px);
  font-weight: 600;
  line-height: 1.18;
  letter-spacing: 0.02em;
  animation: fade-up 0.8s ease 0.18s both;

  em {
    position: relative;
    color: var(--amber);
    font-style: normal;

    &::after {
      content: '';
      position: absolute;
      right: 0;
      bottom: 2px;
      left: 0;
      z-index: -1;
      height: 9px;
      background: rgba(245, 185, 66, 0.12);
    }
  }
}

.lead {
  max-width: 450px;
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.9;
  animation: fade-up 0.8s ease 0.26s both;
}

.features {
  margin: 32px 0 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  animation: fade-up 0.8s ease 0.34s both;

  li {
    display: flex;
    gap: 18px;
    padding: 14px 0;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  }

  strong {
    display: block;
    font-size: 13.5px;
    font-weight: 600;
    letter-spacing: 0.06em;
  }

  p {
    margin: 4px 0 0;
    color: #8494ab;
    font-size: 12.5px;
    line-height: 1.7;
  }
}

.feature-index {
  padding-top: 3px;
  color: var(--amber);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.1em;
}

.visual__footer {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #5c6b80;
  font-size: 11px;
  letter-spacing: 0.08em;
  animation: fade-up 0.8s ease 0.42s both;
}

/* ---------- 右侧表单区 ---------- */
.stage {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background:
    radial-gradient(80% 60% at 100% 0%, rgba(37, 99, 235, 0.07), transparent 60%),
    var(--paper);
}

.paper {
  width: 100%;
  max-width: 420px;
  margin-left: -180px;
}

.form-card {
  position: relative;
  padding: 40px 36px 28px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 20px;
  box-shadow:
    0 30px 60px -24px rgba(15, 23, 42, 0.25),
    inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  animation: rise 0.85s cubic-bezier(0.2, 0.7, 0.2, 1) 0.15s both;

  &::before {
    content: '';
    position: absolute;
    top: 26px;
    left: 0;
    width: 3px;
    height: 42px;
    background: linear-gradient(var(--cobalt), var(--cyan));
    border-radius: 0 3px 3px 0;
  }
}

.form-card__badge {
  display: inline-flex;
  padding: 4px 10px;
  color: var(--cobalt);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 9px;
  letter-spacing: 0.2em;
  background: #eef4ff;
  border-radius: 999px;
}

.form-card__head {
  h2 {
    margin: 16px 0 6px;
    color: #0f172a;
    font-size: 26px;
    font-weight: 700;
    letter-spacing: 0.01em;
  }

  p {
    margin: 0 0 26px;
    color: #64748b;
    font-size: 13px;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 6px;
    color: #334155;
    font-size: 13px;
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    padding: 2px 14px;
    background: #f8fafc;
    border-radius: 10px;
    box-shadow: inset 0 0 0 1px #e2e8f0;
    transition: background 0.2s ease, box-shadow 0.2s ease;
  }

  :deep(.el-input__wrapper:hover) {
    box-shadow: inset 0 0 0 1px #cbd5e1;
  }

  :deep(.el-input__wrapper.is-focus) {
    background: #fff;
    box-shadow:
      inset 0 0 0 1px var(--cobalt),
      0 0 0 4px rgba(37, 99, 235, 0.1);
  }

  :deep(.el-input__inner) {
    height: 42px;
    font-size: 14px;
  }

  :deep(.el-input__prefix-inner .el-icon) {
    color: #94a3b8;
    font-size: 16px;
  }
}

.submit {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  font-size: 14px;
  letter-spacing: 0.1em;
  background: linear-gradient(135deg, #2563eb, #1d4ed8 55%, #0ea5e9);
  border: none;
  border-radius: 10px;
  box-shadow: 0 12px 24px -12px rgba(37, 99, 235, 0.75);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;

  :deep(span) {
    display: inline-flex;
    align-items: center;
  }

  .el-icon {
    margin-left: 8px;
    transition: transform 0.2s ease;
  }

  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.04);
    box-shadow: 0 16px 30px -12px rgba(37, 99, 235, 0.8);

    .el-icon {
      transform: translateX(3px);
    }
  }

  &:active {
    transform: translateY(0);
  }
}

.form-card__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  padding-top: 18px;
  color: #94a3b8;
  font-size: 12px;
  border-top: 1px dashed #e2e8f0;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
}

.stage-note {
  margin: 18px 0 0;
  color: #94a3b8;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-align: center;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes spin-slow {
  from {
    transform: translateY(-50%) rotate(0deg);
  }
  to {
    transform: translateY(-50%) rotate(360deg);
  }
}

@keyframes spin-slow-reverse {
  from {
    transform: translateY(-50%) rotate(0deg);
  }
  to {
    transform: translateY(-50%) rotate(-360deg);
  }
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .visual {
    min-height: 320px;
    padding: 30px 32px;
  }

  .visual__content {
    margin: 22px 0 8px;
  }

  h1 {
    font-size: 40px;
  }

  .features,
  .visual__mark {
    display: none;
  }

  .visual__footer {
    margin-top: 22px;
  }

  .stage {
    padding: 40px 24px 56px;
  }

  .paper {
    max-width: 440px;
    margin-left: 0;
  }
}

@media (max-width: 640px) {
  .visual {
    min-height: 248px;
    padding: 24px;
  }

  h1 {
    margin: 14px 0 12px;
    font-size: 32px;
  }

  .lead {
    font-size: 13px;
  }

  .visual__footer span:last-child {
    display: none;
  }

  .stage {
    padding: 24px 16px 40px;
  }

  .form-card {
    padding: 28px 22px 22px;
    border-radius: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-page *,
  .login-page *::before,
  .login-page *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
