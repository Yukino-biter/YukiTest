<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/index.js'

const route = useRoute()
const router = useRouter()
const paperId = Number(route.params.paperId)

const paper = ref(null)
const mains = ref([])
const answers = ref({})
const currentMainIdx = ref(0)
const currentSubIdx = ref(0)
const loading = ref(true)
const submitting = ref(false)
const saving = ref(false)
const timeLeft = ref(0)
const timerRunning = ref(false)
let timer = null
let autoSaveTimer = null
const saveKey = `exam_progress_${paperId}`

// Flatten all items for navigation
const allItems = computed(() => {
  const list = []
  mains.value.forEach((main, mi) => {
    main.items.forEach((item, si) => {
      list.push({ mainIdx: mi, subIdx: si, item, main })
    })
  })
  return list
})

const currentItem = computed(() => {
  const main = mains.value[currentMainIdx.value]
  if (!main) return null
  return main.items[currentSubIdx.value] || null
})

const currentMain = computed(() => mains.value[currentMainIdx.value] || null)
const hasMaterial = computed(() => currentMain.value?.material && currentMain.value.material.trim())
const totalItems = computed(() => allItems.value.length)
const answeredCount = computed(() => Object.keys(answers.value).length)
const progress = computed(() => totalItems.value ? Math.round(answeredCount.value / totalItems.value * 100) : 0)

// Check if current section is reading/listening
const isSplitView = computed(() => {
  const s = currentMain.value?.section
  return s === 'reading' || s === 'listening'
})

onMounted(async () => {
  try {
    const data = await api.getPaper(paperId)
    paper.value = data.paper
    mains.value = data.mains
    timeLeft.value = (data.paper.examMinutes || 120) * 60
    await loadDraft()
    autoSaveTimer = setInterval(saveToLocal, 30000)
    // Focus on specific question if ?focus=xxx is present
    const focusId = Number(route.query.focus)
    if (focusId) focusToQuestion(focusId)
  } catch (e) {
    alert(e.message)
    router.push('/')
  } finally {
    loading.value = false
  }
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (autoSaveTimer) clearInterval(autoSaveTimer)
  document.removeEventListener('keydown', handleKeydown)
})

function startTimer() {
  timerRunning.value = true
  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) {
      clearInterval(timer)
      timerRunning.value = false
      submitExam()
    }
  }, 1000)
}

function pauseTimer() {
  clearInterval(timer)
  timer = null
  timerRunning.value = false
}

function toggleTimer() {
  if (timerRunning.value) {
    pauseTimer()
  } else {
    startTimer()
  }
}

function formatTime(s) {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function selectAnswer(itemId, option) {
  answers.value[itemId] = option
  saveToLocal()
}

function saveToLocal() {
  try {
    localStorage.setItem(saveKey, JSON.stringify({
      answers: answers.value,
      currentMainIdx: currentMainIdx.value,
      currentSubIdx: currentSubIdx.value,
      timeLeft: timeLeft.value
    }))
  } catch {}
}

function loadLocal() {
  try {
    const raw = localStorage.getItem(saveKey)
    if (!raw) return false
    const saved = JSON.parse(raw)
    if (saved.answers) answers.value = saved.answers
    if (typeof saved.currentMainIdx === 'number') currentMainIdx.value = saved.currentMainIdx
    if (typeof saved.currentSubIdx === 'number') currentSubIdx.value = saved.currentSubIdx
    if (typeof saved.timeLeft === 'number' && saved.timeLeft > 0) timeLeft.value = saved.timeLeft
    return true
  } catch { return false }
}

async function loadDraft() {
  // Try backend draft first
  try {
    const draft = await api.getDraft(paperId)
    if (draft && draft.answers) {
      const parsed = typeof draft.answers === 'string' ? JSON.parse(draft.answers) : draft.answers
      if (parsed && Object.keys(parsed).length > 0) {
        if (confirm('检测到上次未完成的考试进度，是否恢复？')) {
          answers.value = parsed
          if (typeof draft.timeLeft === 'number' && draft.timeLeft > 0) {
            timeLeft.value = draft.timeLeft
          }
          return
        }
      }
    }
  } catch {}
  // Fallback to local
  loadLocal()
}

async function saveAndExit() {
  if (saving.value) return
  if (!confirm('已为你保存当前进度，下次进入可继续作答。确认退出吗？')) return

  saving.value = true
  try {
    await api.saveProgress({ paperId, answers: answers.value, timeLeft: timeLeft.value })
    localStorage.removeItem(saveKey)
    router.push('/')
  } catch (e) {
    alert(e.message)
  } finally {
    saving.value = false
  }
}

// Keyboard shortcuts
function handleKeydown(e) {
  // Ignore when typing in input/textarea
  const tag = e.target.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

  const key = e.key

  // Navigation: Arrow keys only
  if (key === 'ArrowLeft') {
    e.preventDefault()
    prevSub()
  } else if (key === 'ArrowRight') {
    e.preventDefault()
    nextSub()
  }

  // Option selection: 1-4 directly maps to option keys "1","2","3","4"
  if (!currentItem.value || !currentItem.value.options) return
  const optionKeys = Object.keys(currentItem.value.options)
  if (optionKeys.includes(key)) {
    e.preventDefault()
    selectAnswer(currentItem.value.id, key)
  }
}

function goToItem(idx) {
  const target = allItems.value[idx]
  if (target) {
    currentMainIdx.value = target.mainIdx
    currentSubIdx.value = target.subIdx
  }
}

function focusToQuestion(questionItemId) {
  const idx = allItems.value.findIndex(e => e.item.id === questionItemId)
  if (idx >= 0) goToItem(idx)
}

function prevSub() {
  if (currentSubIdx.value > 0) {
    currentSubIdx.value--
  } else if (currentMainIdx.value > 0) {
    // 跳到上一大题最后一小题
    currentMainIdx.value--
    currentSubIdx.value = currentMain.value.items.length - 1
  }
}

function nextSub() {
  const main = currentMain.value
  if (!main) return
  if (currentSubIdx.value < main.items.length - 1) {
    currentSubIdx.value++
  } else if (currentMainIdx.value < mains.value.length - 1) {
    // 跳到下一大题第1小题
    currentMainIdx.value++
    currentSubIdx.value = 0
  }
}

function navIndex(mainIdx, subIdx) {
  return allItems.value.findIndex(i => i.mainIdx === mainIdx && i.subIdx === subIdx)
}

async function submitExam() {
  if (submitting.value) return
  if (answeredCount.value === 0) {
    alert('⚠️ 无法提交白卷！如果您想退出并保留当前进度，请点击右上角的「保存并退出」按钮。')
    return
  }
  const unanswered = totalItems.value - answeredCount.value
  if (unanswered > 0 && !confirm(`还有 ${unanswered} 题未作答，确认交卷？`)) return

  submitting.value = true
  try {
    const answerList = Object.entries(answers.value).map(([qid, ans]) => ({
      questionItemId: Number(qid),
      userAnswer: ans
    }))
    const result = await api.submitExam({ paperId, answers: answerList })
    localStorage.removeItem(saveKey)
    api.clearDraft(paperId).catch(() => {})
    router.push(`/result/${result.attemptId}`)
  } catch (e) {
    alert(e.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="exam-page" v-if="!loading && paper">
    <!-- Top bar -->
    <div class="exam-bar">
      <div class="exam-bar__left">
        <span class="exam-bar__title">{{ paper.paperName }}</span>
      </div>
      <div class="exam-bar__center">
        <button class="btn btn--ghost btn--sm timer-toggle" @click="toggleTimer">
          {{ timerRunning ? '暂停计时' : '开始计时' }}
        </button>
        <div class="timer" :class="{ 'timer--urgent': timeLeft < 600, 'timer--paused': !timerRunning }">
          {{ formatTime(timeLeft) }}
        </div>
      </div>
      <div class="exam-bar__right">
        <div class="progress-bar">
          <div class="progress-bar__fill" :style="{ width: progress + '%' }"></div>
        </div>
        <span class="progress-text">{{ answeredCount }}/{{ totalItems }}</span>
        <button class="btn btn--ghost btn--sm save-exit-btn" @click="saveAndExit" :disabled="saving">
          {{ saving ? '保存中...' : '保存并退出' }}
        </button>
        <button class="btn btn--primary btn--sm" @click="submitExam" :disabled="submitting">
          {{ submitting ? '提交中...' : '交卷' }}
        </button>
      </div>
    </div>

    <div class="exam-body">
      <!-- Left: question nav -->
      <div class="exam-nav">
        <div class="exam-nav__grid">
          <button
            v-for="(entry, idx) in allItems"
            :key="entry.item.id"
            class="exam-nav__btn"
            :class="{
              'exam-nav__btn--current': entry.mainIdx === currentMainIdx && entry.subIdx === currentSubIdx,
              'exam-nav__btn--answered': answers[entry.item.id]
            }"
            @click="goToItem(idx)"
          >{{ idx + 1 }}</button>
        </div>
      </div>

      <!-- Center: exam content -->
      <div class="exam-content" :class="{ 'exam-content--split': isSplitView && hasMaterial }">
        <!-- Split view: reading/listening with material -->
        <template v-if="isSplitView && hasMaterial">
          <div class="exam-material">
            <div class="exam-material__header">
              <span class="badge" :class="`badge--${currentMain.level}`">{{ currentMain.level }}</span>
              <span class="exam-section-tag">{{ currentMain.section }}</span>
              <span v-if="currentMain.title" class="exam-material__title">{{ currentMain.title }}</span>
            </div>
            <div class="exam-material__body">
              {{ currentMain.material }}
            </div>
          </div>

          <div class="exam-question-panel">
            <div class="sub-nav">
              <button class="btn btn--ghost btn--sm" :disabled="currentMainIdx === 0 && currentSubIdx === 0" @click="prevSub">← 上一小题</button>
              <span class="sub-nav__info">第 {{ currentSubIdx + 1 }}/{{ currentMain.items.length }} 小题</span>
              <button class="btn btn--ghost btn--sm" :disabled="currentMainIdx >= mains.length - 1 && currentSubIdx >= currentMain.items.length - 1" @click="nextSub">下一小题 →</button>
            </div>

            <div v-if="currentItem" class="exam-question">
              <p class="exam-question__stem">{{ currentItem.content }}</p>
              <div class="exam-options">
                <div
                  v-for="(text, key) in currentItem.options"
                  :key="key"
                  class="option"
                  :class="{ 'option--selected': answers[currentItem.id] === key }"
                  @click="selectAnswer(currentItem.id, key)"
                >
                  <span class="option__label">{{ key }}</span>
                  <span class="option__text">{{ text }}</span>
                </div>
              </div>
            </div>

            <div class="exam-sub-actions">
              <button class="btn btn--secondary btn--sm" @click="prevSub" :disabled="currentMainIdx === 0 && currentSubIdx === 0">← 上一题</button>
              <span class="exam-nav__info">{{ answeredCount }}/{{ totalItems }} 题</span>
              <button class="btn btn--secondary btn--sm" @click="nextSub" :disabled="currentMainIdx >= mains.length - 1 && currentSubIdx >= currentMain.items.length - 1">下一题 →</button>
            </div>
          </div>
        </template>

        <!-- Single view: vocabulary/grammar -->
        <template v-else>
          <div class="exam-single">
            <div class="exam-single__header">
              <span class="badge" :class="`badge--${currentMain?.level}`">{{ currentMain?.level }}</span>
              <span class="exam-section-tag">{{ currentMain?.section }}</span>
              <span v-if="currentMain?.title" class="exam-single__title">{{ currentMain?.title }}</span>
            </div>

            <div class="sub-nav">
              <button class="btn btn--ghost btn--sm" :disabled="currentMainIdx === 0 && currentSubIdx === 0" @click="prevSub">← 上一题</button>
              <span class="sub-nav__info">第 {{ currentSubIdx + 1 }}/{{ currentMain.items.length }} 小题</span>
              <button class="btn btn--ghost btn--sm" :disabled="currentMainIdx >= mains.length - 1 && currentSubIdx >= currentMain.items.length - 1" @click="nextSub">下一题 →</button>
            </div>

            <div v-if="currentItem" class="exam-question">
              <p class="exam-question__stem">{{ currentItem.content }}</p>
              <div class="exam-options">
                <div
                  v-for="(text, key) in currentItem.options"
                  :key="key"
                  class="option"
                  :class="{ 'option--selected': answers[currentItem.id] === key }"
                  @click="selectAnswer(currentItem.id, key)"
                >
                  <span class="option__label">{{ key }}</span>
                  <span class="option__text">{{ text }}</span>
                </div>
              </div>
            </div>

            <div class="exam-actions">
              <button class="btn btn--secondary" @click="prevSub" :disabled="currentMainIdx === 0 && currentSubIdx === 0">← 上一题</button>
              <span class="exam-nav__info">{{ answeredCount }}/{{ totalItems }} 题</span>
              <button class="btn btn--secondary" @click="nextSub" :disabled="currentMainIdx >= mains.length - 1 && currentSubIdx >= currentMain.items.length - 1">下一题 →</button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>

  <div v-else class="main"><p>加载中...</p></div>
</template>

<style scoped>
.exam-page {
  position: fixed;
  inset: 0;
  background: var(--paper);
  display: flex;
  flex-direction: column;
}

/* === Top bar === */
.exam-bar {
  height: 52px;
  background: var(--indigo);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 16px;
  flex-shrink: 0;
}

.exam-bar__title { font-size: .9rem; font-weight: 500; }
.exam-bar__center { flex: 1; text-align: center; }
.exam-bar__right { display: flex; align-items: center; gap: 12px; }

.timer {
  font-family: var(--font-mono);
  font-size: 1.25rem;
  font-weight: 500;
  letter-spacing: .06em;
  display: inline;
}
.timer--urgent { color: var(--vermillion-light); animation: blink 1s infinite; }
.timer--paused { opacity: .5; }

.timer-toggle {
  color: rgba(255,255,255,.85);
  border-color: rgba(255,255,255,.3);
  margin-right: 8px;
  font-size: .75rem;
}
.timer-toggle:hover { border-color: rgba(255,255,255,.6); color: #fff; }
@keyframes blink { 50% { opacity: .5; } }

.progress-bar {
  width: 80px;
  height: 6px;
  background: rgba(255,255,255,.2);
  border-radius: 3px;
  overflow: hidden;
}
.progress-bar__fill {
  height: 100%;
  background: var(--vermillion-light);
  border-radius: 3px;
  transition: width .3s var(--ease);
}
.progress-text { font-size: .8rem; color: rgba(255,255,255,.7); font-family: var(--font-mono); }

.save-exit-btn {
  color: rgba(255,255,255,.85);
  border-color: rgba(255,255,255,.35);
  font-size: .8rem;
}
.save-exit-btn:hover { border-color: rgba(255,255,255,.6); color: #fff; }

/* === Body === */
.exam-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* === Nav panel === */
.exam-nav {
  width: 72px;
  background: #fff;
  border-right: 1px solid var(--paper-dark);
  padding: 12px 8px;
  overflow-y: auto;
  flex-shrink: 0;
}

.exam-nav__grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.exam-nav__btn {
  width: 40px;
  height: 40px;
  border: 1.5px solid var(--paper-dark);
  border-radius: 6px;
  background: #fff;
  font-family: var(--font-mono);
  font-size: .8rem;
  color: var(--ink-light);
  cursor: pointer;
  transition: all .15s var(--ease);
  margin: 0 auto;
}

.exam-nav__btn:hover { border-color: var(--ink-faint); }
.exam-nav__btn--current { border-color: var(--indigo); background: var(--indigo-bg); color: var(--indigo); font-weight: 700; }
.exam-nav__btn--answered { background: var(--correct-bg); border-color: var(--correct); color: var(--correct); }

/* === Content area === */
.exam-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.exam-content--split {
  display: flex;
  gap: 0;
  padding: 0;
}

/* === Split view: material (left) === */
.exam-material {
  width: 50%;
  border-right: 2px solid var(--paper-dark);
  display: flex;
  flex-direction: column;
  background: #fff;
}

.exam-material__header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--paper-dark);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.exam-section-tag {
  font-size: .75rem;
  color: var(--ink-faint);
  text-transform: uppercase;
  letter-spacing: .06em;
}

.exam-material__title {
  font-size: .9rem;
  font-weight: 500;
  color: var(--ink-light);
}

.exam-material__body {
  flex: 1;
  padding: 24px;
  font-size: .95rem;
  line-height: 1.9;
  overflow-y: auto;
  color: var(--ink);
  white-space: pre-wrap;
  font-family: var(--font-body);
  user-select: text;
}

/* === Split view: question panel (right) === */
.exam-question-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sub-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 24px;
  border-bottom: 1px solid var(--paper-dark);
  background: #fff;
  flex-shrink: 0;
}

.sub-nav__info {
  font-size: .85rem;
  color: var(--ink-faint);
  font-family: var(--font-mono);
}

.exam-sub-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--paper-dark);
  background: #fff;
  flex-shrink: 0;
}

.exam-nav__info {
  font-size: .85rem;
  color: var(--ink-faint);
  font-family: var(--font-mono);
  margin: 0 auto;
}

/* === Single view === */
.exam-single {
  max-width: 720px;
  margin: 0 auto;
}

.exam-single__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.exam-single__title {
  font-size: .95rem;
  color: var(--ink-light);
}

.exam-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

/* === Question === */
.exam-question {
  padding: 24px;
}

.exam-question__stem {
  font-size: 1rem;
  line-height: 1.7;
  margin-bottom: 20px;
  color: var(--ink);
}

.exam-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
