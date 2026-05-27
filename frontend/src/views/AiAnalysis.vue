<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { api } from '../api/index.js'

const route = useRoute()
const router = useRouter()
const questionItemId = Number(route.params.questionItemId)
const content = ref('')
const loading = ref(true)
const error = ref('')
const outputRef = ref(null)
const isListening = ref(route.query.section === 'listening')
const noKey = ref(false)
const correctAnswer = ref('')
const userAnswer = ref(route.query.userAnswer || '')

onMounted(async () => {
  if (isListening.value) { loading.value = false; return }
  await startStream()
})

async function startStream() {
  content.value = ''
  loading.value = true
  error.value = ''
  noKey.value = false
  correctAnswer.value = ''

  try {
    const body = await api.analyzeStream(questionItemId)
    const reader = body.getReader()
    const decoder = new TextDecoder()
    let currentEvent = 'message'

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })

      for (const line of chunk.split('\n')) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
          continue
        }
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (data === '[DONE]') continue

        if (currentEvent === 'no_key') {
          noKey.value = true
        } else if (currentEvent === 'answer') {
          correctAnswer.value = data
        } else if (currentEvent === 'error') {
          error.value = data
        } else if (currentEvent === 'message') {
          // Try JSON parse for OpenAI-style chunks
          try {
            const parsed = JSON.parse(data)
            if (parsed.choices?.[0]?.delta?.content) {
              content.value += parsed.choices[0].delta.content
            }
          } catch {
            // Plain text chunk
            content.value += data
          }
        }
      }
      await nextTick()
      if (outputRef.value) outputRef.value.scrollTop = outputRef.value.scrollHeight
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function goSettings() {
  router.push('/settings')
}
</script>

<template>
  <div class="main">
    <div class="ai-card card">
      <h1 class="ai-title">AI 解析</h1>

      <!-- Listening: static hint -->
      <div v-if="isListening" class="ai-hint-block">
        <p class="ai-hint-block__icon">🎧</p>
        <p class="ai-hint-block__text">
          提示：听力部分暂不提供 AI 智能解析，请对照听力视频源原文与翻译自行进行复习。
        </p>
        <button class="btn btn--secondary" @click="router.back()">返回</button>
      </div>

      <!-- No API key: show answer + hint -->
      <div v-else-if="noKey" class="ai-hint-block">
        <div v-if="correctAnswer" class="ai-answer-card">
          <span class="ai-answer-card__label">正确答案</span>
          <span class="ai-answer-card__value">{{ correctAnswer }}</span>
        </div>
        <div v-if="userAnswer" class="ai-answer-card ai-answer-card--user">
          <span class="ai-answer-card__label">你的答案</span>
          <span class="ai-answer-card__value">{{ userAnswer }}</span>
        </div>
        <p class="ai-hint-block__text">
          检测到您当前未配置 API Key，已为您展示正确选项。如需享受 AI 流式深度多角度分析，请前往
          <a href="#" @click.prevent="goSettings" class="ai-link">『设置』</a>
          页面配置您的专属 Key。
        </p>
        <button class="btn btn--secondary" @click="router.back()">返回</button>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="ai-error">
        <p>{{ error }}</p>
        <button class="btn btn--secondary btn--sm" @click="router.back()">返回</button>
      </div>

      <!-- Normal AI stream -->
      <template v-else>
        <div ref="outputRef" class="ai-output" v-html="marked(content || '')"></div>
        <div v-if="loading && !content" class="ai-loading">
          <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
        </div>
        <div v-if="!loading && content" class="ai-actions">
          <button class="btn btn--secondary btn--sm" @click="startStream">重新解析</button>
          <button class="btn btn--ghost btn--sm" @click="router.back()">返回</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.ai-card {
  max-width: 720px;
  margin: 24px auto;
  padding: 32px;
  min-height: 400px;
}
.ai-title {
  font-family: var(--font-display);
  font-size: 1.3rem;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--paper-dark);
}
.ai-output {
  font-size: .95rem;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-output :deep(h2) {
  font-family: var(--font-display);
  font-size: 1.1rem;
  margin: 20px 0 8px;
  color: var(--indigo);
}
.ai-output :deep(p) { margin-bottom: 8px; }
.ai-error { color: var(--wrong); font-size: .9rem; padding: 20px 0; }
.ai-loading {
  text-align: center;
  font-size: 2rem;
  color: var(--ink-faint);
}
.dot { animation: blink 1.4s infinite both; }
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%,80%,100% { opacity: 0; } 40% { opacity: 1; } }

.ai-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--paper-dark);
}

/* Hint block (no-key / listening) */
.ai-hint-block {
  text-align: center;
  padding: 40px 20px;
}
.ai-hint-block__icon { font-size: 3rem; margin-bottom: 16px; }
.ai-hint-block__text {
  color: var(--ink-light);
  font-size: .95rem;
  line-height: 1.7;
  margin-bottom: 24px;
}
.ai-link { color: var(--indigo); text-decoration: underline; }

/* Answer card */
.ai-answer-card {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  border-radius: var(--radius);
  background: var(--correct-bg);
  border: 1.5px solid var(--correct);
  margin-bottom: 16px;
}
.ai-answer-card--user {
  background: #fff0f0;
  border-color: var(--wrong);
}
.ai-answer-card__label {
  font-size: .8rem;
  color: var(--ink-faint);
}
.ai-answer-card__value {
  font-family: var(--font-mono);
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--ink);
}
</style>
