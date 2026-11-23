import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'admin-login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '后台登录', guestOnly: true }
  },
  {
    path: '/',
    component: AdminLayout,
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '数据看板', permission: 'dashboard:view' }
      },
      {
        path: 'content/subjects',
        name: 'subjects',
        component: () => import('@/views/content/SubjectView.vue'),
        meta: { title: '学科管理', permission: 'subject:view' }
      },
      {
        path: 'content/courses',
        name: 'courses',
        component: () => import('@/views/content/CourseView.vue'),
        meta: { title: 'B站课程库', permission: 'course:view' }
      },
      {
        path: 'content/questions',
        name: 'questions',
        component: () => import('@/views/content/QuestionView.vue'),
        meta: { title: '题库管理', permission: 'question:view' }
      },
      {
        path: 'content/interview-bank',
        name: 'interview-bank',
        component: () => import('@/views/content/InterviewBankView.vue'),
        meta: { title: '面试题库', permission: 'interviewbank:view' }
      },
      {
        path: 'content/routes',
        name: 'route-manage',
        component: () => import('@/views/content/RouteView.vue'),
        meta: { title: '学习路线', permission: 'route:view' }
      },
      {
        path: 'content/routes/:id',
        name: 'route-manage-detail',
        component: () => import('@/views/content/RouteDetailView.vue'),
        meta: { title: '路线课程管理', permission: 'route:view' }
      },
      {
        path: 'students',
        name: 'students',
        component: () => import('@/views/student/StudentListView.vue'),
        meta: { title: '学员管理', permission: 'student:view' }
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '用户管理', permission: 'user:view' }
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleView.vue'),
        meta: { title: '角色权限', superAdminOnly: true }
      },
      {
        path: 'system/logs',
        name: 'system-logs',
        component: () => import('@/views/system/OperationLogView.vue'),
        meta: { title: '操作日志', permission: 'system:log_view' }
      },
      {
        path: 'system/settings',
        name: 'system-settings',
        component: () => import('@/views/system/SettingView.vue'),
        meta: { title: '系统设置', permission: 'system:setting' }
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/error/ForbiddenView.vue'),
        meta: { title: '无权限' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'admin-not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const userStore = useAdminUserStore()
  const permissionStore = usePermissionStore()

  if (to.meta.guestOnly && userStore.isLoggedIn) {
    return { path: '/dashboard' }
  }

  if (to.path !== '/login' && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.superAdminOnly && !permissionStore.isSuperAdmin) {
    return '/403'
  }

  if (to.meta.permission && !permissionStore.has(to.meta.permission)) {
    return '/403'
  }

  document.title = to.meta.title ? `${to.meta.title} - 云枢智学管理后台` : '云枢智学管理后台'
  return true
})

export default router
