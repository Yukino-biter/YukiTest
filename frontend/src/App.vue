<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './stores/user.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const nickname = computed(() => userStore.nickname || '用户')

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <nav v-if="isLoggedIn && !route.meta.hideNavbar && route.name !== 'exam'" class="nav">
    <div class="nav__logo">Yuki<span>Test</span></div>
    <div class="nav__links">
      <router-link to="/">试卷</router-link>
      <router-link to="/wrong-book">错题本</router-link>
      <router-link to="/settings">设置</router-link>
    </div>
    <div class="nav__user">
      <span>{{ nickname }}</span>
      <button class="btn btn--sm btn--ghost" style="color:#fff;border-color:rgba(255,255,255,.3)" @click="logout">退出</button>
    </div>
  </nav>

  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>
