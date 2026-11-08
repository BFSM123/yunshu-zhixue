import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { PERMISSIONS_KEY, ROLE_KEY } from '@/utils/token'

export interface MenuItem {
  title: string
  path: string
  permission?: string
  icon?: string
  children?: MenuItem[]
}

/**
 * 菜单与权限码。菜单按权限码动态渲染；角色管理仅超级管理员可见（无独立权限码，D4-5）。
 */
const MENU_TREE: MenuItem[] = [
  { title: '数据看板', path: '/dashboard', permission: 'dashboard:view', icon: 'DataLine' },
  {
    title: '内容管理',
    path: '/content',
    icon: 'Files',
    children: [
      { title: '学科管理', path: '/content/subjects', permission: 'subject:view' },
      { title: 'B站课程库', path: '/content/courses', permission: 'course:view' },
      { title: '题库管理', path: '/content/questions', permission: 'question:view' },
      { title: '面试题库', path: '/content/interview-bank', permission: 'interviewbank:view' },
      { title: '学习路线', path: '/content/routes', permission: 'route:view' }
    ]
  },
  { title: '学员管理', path: '/students', permission: 'student:view', icon: 'User' },
  {
    title: '系统管理',
    path: '/system',
    icon: 'Setting',
    children: [
      { title: '用户管理', path: '/system/users', permission: 'user:view' },
      { title: '角色权限', path: '/system/roles', permission: 'role:manage' },
      { title: '操作日志', path: '/system/logs', permission: 'system:log_view' },
      { title: '系统设置', path: '/system/settings', permission: 'system:setting' }
    ]
  }
]

function readPermissions(): string[] {
  const raw = localStorage.getItem(PERMISSIONS_KEY)
  if (!raw) return []
  try {
    const value = JSON.parse(raw) as unknown
    return Array.isArray(value) ? (value as string[]) : []
  } catch {
    return []
  }
}

export const usePermissionStore = defineStore('admin-permission', () => {
  const permissions = ref<string[]>(readPermissions())
  const roleCode = ref(localStorage.getItem(ROLE_KEY) || '')
  const showAllMenus = import.meta.env.VITE_SHOW_ALL_MENUS === 'true'

  const isSuperAdmin = computed(() => roleCode.value === 'super_admin')

  function has(code?: string): boolean {
    if (!code) return true
    if (showAllMenus || isSuperAdmin.value) return true
    return permissions.value.includes(code)
  }

  function filterMenus(items: MenuItem[]): MenuItem[] {
    return items
      .filter((item) => has(item.permission))
      .map((item) => ({
        ...item,
        children: item.children ? filterMenus(item.children) : undefined
      }))
      .filter((item) => !item.children || item.children.length > 0)
  }

  const menus = computed(() => filterMenus(MENU_TREE))

  function setPermissions(list: string[], role: string) {
    permissions.value = list
    roleCode.value = role
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(list))
    localStorage.setItem(ROLE_KEY, role)
  }

  function clear() {
    permissions.value = []
    roleCode.value = ''
    localStorage.removeItem(PERMISSIONS_KEY)
    localStorage.removeItem(ROLE_KEY)
  }

  return { permissions, roleCode, isSuperAdmin, has, menus, setPermissions, clear }
})
