import { defineStore } from 'pinia'
import { ref } from 'vue'

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

  return { token, nickname, isLoggedIn, setAuth, logout }
})
