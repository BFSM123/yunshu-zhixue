import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', guestOnly: true }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { title: '找回密码', guestOnly: true }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'questions',
        name: 'question-bank',
        component: () => import('@/views/questions/QuestionBankView.vue'),
        meta: { title: '题库中心' }
      },
      {
        path: 'practice',
        name: 'practice',
        component: () => import('@/views/practice/PracticeView.vue'),
        meta: { title: '刷题练习', requiresAuth: true }
      },
      {
        path: 'wrong-book',
        name: 'wrong-book',
        component: () => import('@/views/wrong-book/WrongBookView.vue'),
        meta: { title: '错题本', requiresAuth: true }
      },
      {
        path: 'routes',
        name: 'route-list',
        component: () => import('@/views/routes/RouteListView.vue'),
        meta: { title: '学习路线', requiresAuth: true }
      },
      {
        path: 'routes/:id',
        name: 'route-detail',
        component: () => import('@/views/routes/RouteDetailView.vue'),
        meta: { title: '路线详情', requiresAuth: true }
      },
      {
        path: 'interview',
        name: 'interview-config',
        component: () => import('@/views/interview/InterviewConfigView.vue'),
        meta: { title: 'AI 面试', requiresAuth: true }
      },
      {
        path: 'interview/session',
        name: 'interview-session',
        component: () => import('@/views/interview/InterviewSessionView.vue'),
        meta: { title: '面试进行中', requiresAuth: true }
      },
      {
        path: 'interview/records',
        name: 'interview-records',
        component: () => import('@/views/interview/InterviewRecordsView.vue'),
        meta: { title: '面试记录', requiresAuth: true }
      },
      {
        path: 'interview/reports/:id',
        name: 'interview-report',
        component: () => import('@/views/interview/InterviewReportView.vue'),
        meta: { title: '面试报告', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && userStore.isLoggedIn) {
    return { path: '/' }
  }

  document.title = to.meta.title ? `${to.meta.title} - 云枢智学` : '云枢智学'
  return true
})

export default router
