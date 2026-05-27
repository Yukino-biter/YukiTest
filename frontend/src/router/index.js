import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true, hideNavbar: true } },
  { path: '/', name: 'exams', component: () => import('../views/ExamList.vue') },
  { path: '/exam/:paperId', name: 'exam', component: () => import('../views/Exam.vue') },
  { path: '/result/:attemptId', name: 'result', component: () => import('../views/Result.vue') },
  { path: '/ai/:questionItemId', name: 'ai', component: () => import('../views/AiAnalysis.vue') },
  { path: '/wrong-book', name: 'wrong-book', component: () => import('../views/WrongBook.vue') },
  { path: '/settings', name: 'settings', component: () => import('../views/Settings.vue') },
  { path: '/stats', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('yuki_token')
  if (!to.meta.public && !token) {
    return { name: 'login' }
  }
})

export default router
