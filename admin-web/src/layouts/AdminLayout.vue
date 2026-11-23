<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ 'is-collapsed': isCollapsed }">
      <div class="logo">
        <span class="logo-mark">云</span>
        <div v-if="!isCollapsed" class="logo-copy">
          <span class="logo-text">云枢智学</span>
          <span class="logo-sub">管理后台</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        class="menu"
        background-color="transparent"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <template v-for="item in permissionStore.menus" :key="item.path">
          <el-sub-menu v-if="item.children?.length" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <el-button class="collapse-btn" text circle @click="toggleCollapse">
            <el-icon>
              <component :is="isCollapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item">
              <span :class="{ 'crumb-current': index === breadcrumbs.length - 1 }">{{ item }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <el-dropdown>
          <span class="user-entry">
            <el-avatar :size="30">{{ userInitial }}</el-avatar>
            <span class="user-name">{{ userStore.profile?.nickname || '管理员' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/system/settings')">系统设置</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main ref="contentRef" class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import { useAdminUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { adminLogout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useAdminUserStore()
const permissionStore = usePermissionStore()

const isCollapsed = ref(false)
const contentRef = ref<HTMLElement | null>(null)

const userInitial = computed(() => (userStore.profile?.nickname || '管').slice(0, 1))

// 详情等子路径仍高亮所属菜单
const activeMenu = computed(() => {
  const paths = permissionStore.menus.flatMap((group) => [
    group.path,
    ...(group.children?.map((child) => child.path) ?? [])
  ])
  const matched = paths
    .filter((path) => path !== '/' && (route.path === path || route.path.startsWith(`${path}/`)))
    .sort((left, right) => right.length - left.length)[0]
  return matched || route.path
})

const breadcrumbs = computed(() => {
  for (const group of permissionStore.menus) {
    if (group.path === route.path) {
      return [group.title]
    }
    const child = group.children?.find(
      (item) => route.path === item.path || route.path.startsWith(`${item.path}/`)
    )
    if (child) {
      return [group.title, child.title]
    }
  }
  return [String(route.meta.title || '管理后台')]
})

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

watch(
  () => route.path,
  () => {
    contentRef.value?.scrollTo({ top: 0 })
  }
)

async function handleLogout() {
  try {
    if (userStore.refreshToken) {
      await adminLogout(userStore.refreshToken)
    }
  } catch {
    // 登出失败不阻塞本地状态清理
  } finally {
    userStore.clear()
    permissionStore.clear()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.admin-layout {
  display: flex;
  height: 100%;
}

.sidebar {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: $yszx-sidebar-width;
  background: $yszx-sidebar-bg;
  transition: width 0.2s ease;

  &.is-collapsed {
    width: 64px;

    .logo {
      justify-content: center;
      padding: 0;
    }

    .menu {
      padding: 12px 4px;
    }
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: $yszx-header-height;
  padding: 0 18px;
  color: #fff;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #7c3aed);
  border-radius: 9px;
}

.logo-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
}

.logo-sub {
  color: #94a3b8;
  font-size: 11px;
}

.menu {
  flex: 1;
  padding: 12px 10px;
  border-right: none;

  &.el-menu--collapse {
    width: 100%;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 42px;
    margin-bottom: 4px;
    border-radius: 8px;
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.9), rgba(99, 102, 241, 0.75));
  }
}

.main-area {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $yszx-header-height;
  padding: 0 24px 0 12px;
  background: $yszx-surface;
  border-bottom: 1px solid $yszx-border;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.collapse-btn {
  color: $yszx-text-secondary;
}

.crumb-current {
  color: $yszx-text;
  font-weight: 600;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
}

.content {
  flex: 1;
  overflow: auto;
}
</style>
