import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api/index.js'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('yuki_token') || '')
  const nickname = ref(localStorage.getItem('yuki_nickname') || '')
  const isLoggedIn = ref(!!token.value)

  function setAuth(data) {
    token.value = data.token
    nickname.value = data.nickname || ''
    isLoggedIn.value = true
    localStorage.setItem('yuki_token', data.token)
    localStorage.setItem('yuki_nickname', data.nickname || '')
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    isLoggedIn.value = false
    localStorage.removeItem('yuki_token')
    localStorage.removeItem('yuki_nickname')
  }

  async function validateToken() {
    if (!token.value) return false
    try {
      const data = await api.getMe()
      nickname.value = data.nickname || ''
      localStorage.setItem('yuki_nickname', data.nickname || '')
      isLoggedIn.value = true
      return true
    } catch {
      logout()
      return false
    }
  }

  return { token, nickname, isLoggedIn, setAuth, logout, validateToken }
})
