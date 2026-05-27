<script setup>
import { ref } from 'vue'
import { api } from '../api/index.js'

const form = ref({ apiProvider: 'deepseek', apiKey: '', baseUrl: '', apiModel: '' })
const saving = ref(false)
const msg = ref('')

async function save() {
  saving.value = true
  msg.value = ''
  try {
    await api.updateAiConfig(form.value)
    msg.value = '保存成功'
  } catch (e) {
    msg.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="main">
    <h1 class="page-title">个人设置</h1>

    <div class="card settings-card">
      <h2 class="settings-section">AI 配置</h2>
      <p class="settings-hint">配置你的 AI 服务商信息。留空则使用系统默认（DeepSeek）。</p>

      <form @submit.prevent="save" class="settings-form">
        <div class="field">
          <label>API 提供商</label>
          <input class="input" v-model="form.apiProvider" placeholder="deepseek" />
        </div>
        <div class="field">
          <label>API Key</label>
          <input class="input" type="password" v-model="form.apiKey" placeholder="sk-..." />
        </div>
        <div class="field">
          <label>Base URL</label>
          <input class="input" v-model="form.baseUrl" placeholder="默认: https://api.deepseek.com/v1" />
        </div>
        <div class="field">
          <label>模型名称</label>
          <input class="input" v-model="form.apiModel" placeholder="默认: deepseek-chat" />
        </div>

        <p v-if="msg" class="settings-msg" :class="{ 'settings-msg--err': msg !== '保存成功' }">{{ msg }}</p>

        <button class="btn btn--primary" :disabled="saving">{{ saving ? '保存中...' : '保存配置' }}</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-family: var(--font-display); font-size: 1.5rem; margin-bottom: 24px; }
.settings-card { max-width: 560px; padding: 32px; }
.settings-section { font-family: var(--font-display); font-size: 1.15rem; margin-bottom: 6px; }
.settings-hint { font-size: .85rem; color: var(--ink-faint); margin-bottom: 24px; }
.settings-form { display: flex; flex-direction: column; gap: 16px; }
.field label { display: block; font-size: .8rem; font-weight: 500; color: var(--ink-light); margin-bottom: 6px; }
.settings-msg { font-size: .85rem; color: var(--correct); }
.settings-msg--err { color: var(--wrong); }
</style>
