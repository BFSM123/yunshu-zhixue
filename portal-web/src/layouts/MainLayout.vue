<template>
  <div class="portal-layout">
    <header class="portal-header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark">云</span>
          <span class="brand-name">云枢智学</span>
        </router-link>

        <nav class="nav">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
            {{ item.label }}
          </router-link>
        </nav>

        <div class="actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <span class="user-entry">
                <el-avatar :size="30" :src="userStore.profile?.avatar || undefined">
                  {{ userInitial }}
                </el-avatar>
                <span class="user-name">{{ userStore.profile?.nickname || '我的账号' }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>

    <footer class="portal-footer">
      云枢智学 · AI 赋能的智能面试训练平台
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { logout } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { label: '首页', path: '/' },
  { label: '题库', path: '/questions' },
  { label: '刷题', path: '/practice' },
  { label: '错题本', path: '/wrong-book' },
  { label: '学习路线', path: '/routes' },
  { label: 'AI 面试', path: '/interview' }
]

const userInitial = computed(() => (userStore.profile?.nickname || '我').slice(0, 1))

async function handleLogout() {
  try {
    if (userStore.refreshToken) {
      await logout(userStore.refreshToken)
    }
  } catch {
    // 登出接口失败不阻塞本地清理
  } finally {
    userStore.clear()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.portal-layout {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid $yszx-border;
  backdrop-filter: blur(10px);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  max-width: 1200px;
  height: $yszx-header-height;
  margin: 0 auto;
  padding: 0 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
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

.nav {
  display: flex;
  flex: 1;
  gap: 6px;
}

.nav-item {
  padding: 8px 14px;
  color: $yszx-text-secondary;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    color: $yszx-primary;
    background: #eff6ff;
  }

  &.router-link-active {
    color: $yszx-primary;
    font-weight: 600;
    background: #eff6ff;
  }
}

.actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portal-main {
  flex: 1;
}

.portal-footer {
  padding: 24px;
  color: $yszx-text-secondary;
  font-size: 13px;
  text-align: center;
  border-top: 1px solid $yszx-border;
}

@media (max-width: 900px) {
  .nav {
    display: none;
  }

  .header-inner {
    justify-content: space-between;
  }
}
</style>
