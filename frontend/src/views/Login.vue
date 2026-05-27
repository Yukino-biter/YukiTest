<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { useUserStore } from '../stores/user.js'

const router = useRouter()
const userStore = useUserStore()
const isRegister = ref(false)
const form = ref({ username: '', password: '', nickname: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const data = isRegister.value
      ? await api.register(form.value)
      : await api.login({ username: form.value.username, password: form.value.password })
    userStore.setAuth(data)
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">Yuki<span>Test</span></h1>
      <p class="login-sub">JLPT 备考助手</p>

      <form @submit.prevent="submit" class="login-form">
        <div class="field">
          <label>用户名</label>
          <input class="input" v-model="form.username" placeholder="请输入用户名" required />
        </div>
        <div v-if="isRegister" class="field">
          <label>昵称</label>
          <input class="input" v-model="form.nickname" placeholder="可选" />
        </div>
        <div class="field">
          <label>密码</label>
          <input class="input" type="password" v-model="form.password" placeholder="请输入密码" required />
        </div>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button class="btn btn--primary btn--lg btn--block" :disabled="loading">
          {{ loading ? '请稍候...' : (isRegister ? '注册' : '登录') }}
        </button>
      </form>

      <p class="login-switch">
        <a href="#" @click.prevent="isRegister = !isRegister">
          {{ isRegister ? '已有账号？立即登录' : '还没有账号？立即注册' }}
        </a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--indigo) 0%, #283593 50%, var(--paper) 100%);
}

.login-card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 48px 40px;
  width: 380px;
  max-width: 90vw;
}

.login-title {
  font-family: var(--font-display);
  font-size: 2rem;
  text-align: center;
  margin-bottom: 4px;
}
.login-title span { color: var(--vermillion); }

.login-sub {
  text-align: center;
  color: var(--ink-faint);
  font-size: .875rem;
  margin-bottom: 32px;
}

.login-form { display: flex; flex-direction: column; gap: 18px; }

.field label {
  display: block;
  font-size: .8rem;
  font-weight: 500;
  color: var(--ink-light);
  margin-bottom: 6px;
}

.login-error {
  color: var(--wrong);
  font-size: .85rem;
  text-align: center;
}

.login-switch {
  text-align: center;
  margin-top: 20px;
  font-size: .85rem;
}
</style>
