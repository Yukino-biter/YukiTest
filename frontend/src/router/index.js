import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user.js'

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

let validated = false

router.beforeEach(async (to) => {
  if (to.meta.public) return

  const userStore = useUserStore()

  if (!userStore.token) {
    return { name: 'login' }
  }

  if (!validated) {
    const ok = await userStore.validateToken()
    validated = true
    if (!ok) return { name: 'login' }
  }
})

export default router
