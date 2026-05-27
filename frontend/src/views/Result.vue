<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/index.js'

const route = useRoute()
const router = useRouter()
const attemptId = Number(route.params.attemptId)
const result = ref(null)
const loading = ref(true)
const error = ref('')

const sectionOrder = ['vocabulary', 'grammar', 'reading', 'listening']
const sectionIcons = { vocabulary: '📖', grammar: '📝', reading: '📰', listening: '🎧' }
const sectionLabel = { vocabulary: '文字·词汇', grammar: '语法', reading: '阅读', listening: '听力' }

onMounted(async () => {
  try {
    result.value = await api.getExamResult(attemptId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

function sectionRate(stat) {
  if (!stat.total) return 0
  return Math.round(stat.correct / stat.total * 100)
}

function rateColor(rate) {
  if (rate >= 80) return 'var(--correct)'
  if (rate >= 60) return '#f59e0b'
  return 'var(--wrong)'
}

function goToAnalysis(questionItemId, section, userAnswer) {
  router.push({ path: `/ai/${questionItemId}`, query: { section, userAnswer: userAnswer || '' } })
}
</script>

<template>
  <div class="main">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="result-error">{{ error }}</div>

    <template v-else-if="result">
      <!-- Overview header -->
      <div class="score-header card">
        <div class="score-stats">
          <div class="stat"><span class="stat__num">{{ result.total }}</span><span class="stat__label">总题数</span></div>
          <div class="stat"><span class="stat__num stat__num--correct">{{ result.correct }}</span><span class="stat__label">正确</span></div>
          <div class="stat"><span class="stat__num stat__num--wrong">{{ result.wrong }}</span><span class="stat__label">错误/未答</span></div>
        </div>
      </div>

      <!-- Section cards -->
      <div class="section-grid">
        <div v-for="key in sectionOrder" :key="key" class="card section-card">
          <div class="section-card__header">
            <span class="section-card__icon">{{ sectionIcons[key] }}</span>
            <span class="section-card__label">{{ sectionLabel[key] }}</span>
          </div>
          <div class="section-card__rate" :style="{ color: rateColor(sectionRate(result.sections[key])) }">
            {{ sectionRate(result.sections[key]) }}%
          </div>
          <div class="section-card__bar">
            <div class="section-card__bar-fill" :style="{ width: sectionRate(result.sections[key]) + '%', background: rateColor(sectionRate(result.sections[key])) }"></div>
          </div>
          <div class="section-card__detail">
            错 <strong>{{ result.sections[key]?.wrong || 0 }}</strong> 题 / 共 {{ result.sections[key]?.total || 0 }} 题
          </div>
        </div>
      </div>

      <!-- Question grid -->
      <div class="card question-grid-card">
        <h2 class="grid-title">答题详情</h2>
        <div class="question-grid">
          <button
            v-for="(q, idx) in result.questions"
            :key="q.questionItemId"
            class="q-btn q-btn--clickable"
            :class="{
              'q-btn--correct': q.isCorrect,
              'q-btn--wrong': !q.isCorrect && q.userAnswer,
              'q-btn--unanswered': !q.userAnswer && !q.isCorrect
            }"
            :title="q.isCorrect ? '正确 — 点击查看解析' : (q.section === 'listening' ? '听力题 — 点击查看' : '点击查看AI解析')"
            @click="goToAnalysis(q.questionItemId, q.section, q.userAnswer)"
          >
            <span class="q-btn__num">{{ idx + 1 }}</span>
            <span v-if="!q.isCorrect" class="q-btn__mark">{{ q.section === 'listening' ? '🎧' : (q.userAnswer ? '✗' : '—') }}</span>
          </button>
        </div>
        <div class="grid-legend">
          <span class="legend-item"><span class="legend-dot legend-dot--correct"></span> 正确</span>
          <span class="legend-item"><span class="legend-dot legend-dot--wrong"></span> 错误</span>
          <span class="legend-item"><span class="legend-dot legend-dot--unanswered"></span> 未答</span>
          <span class="legend-item">点击任意题号查看解析</span>
        </div>
      </div>

      <!-- Actions -->
      <div class="result-actions">
        <button class="btn btn--primary" @click="router.push('/')">返回试卷列表</button>
        <button class="btn btn--secondary" @click="router.push('/wrong-book')">查看错题本</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.main {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px;
}

.result-error { color: var(--wrong); text-align: center; padding: 40px; }
.loading { text-align: center; padding: 60px; color: var(--ink-faint); }

/* === Header === */
.score-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  padding: 32px;
  margin-bottom: 20px;
}

.score-stats {
  display: flex;
  gap: 36px;
}
.stat { text-align: center; }
.stat__num {
  display: block;
  font-family: var(--font-mono);
  font-size: 1.8rem;
  font-weight: 600;
}
.stat__num--correct { color: var(--correct); }
.stat__num--wrong { color: var(--wrong); }
.stat__label {
  font-size: .8rem;
  color: var(--ink-faint);
}

/* === Section cards === */
.section-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.section-card { padding: 20px; }
.section-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.section-card__icon { font-size: 1.2rem; }
.section-card__label {
  font-size: .85rem;
  font-weight: 500;
  color: var(--ink-light);
}
.section-card__rate {
  font-family: var(--font-mono);
  font-size: 1.6rem;
  font-weight: 700;
  margin-bottom: 8px;
}
.section-card__bar {
  height: 6px;
  background: var(--paper-dark);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 10px;
}
.section-card__bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width .6s var(--ease);
}
.section-card__detail {
  font-size: .8rem;
  color: var(--ink-faint);
}
.section-card__detail strong {
  color: var(--wrong);
  font-family: var(--font-mono);
}

/* === Question grid === */
.question-grid-card { padding: 24px; margin-bottom: 20px; }
.grid-title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  margin-bottom: 16px;
}
.question-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}
.q-btn {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  border: 1.5px solid var(--paper-dark);
  background: #fff;
  font-family: var(--font-mono);
  font-size: .8rem;
  cursor: default;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.q-btn--correct {
  background: var(--correct-bg);
  border-color: var(--correct);
  color: var(--correct);
}
.q-btn--wrong {
  background: #fff0f0;
  border-color: var(--wrong);
  color: var(--wrong);
}
.q-btn--unanswered {
  background: #f5f5f5;
  border-color: #bbb;
  color: #999;
}
.q-btn--clickable { cursor: pointer; transition: transform .12s var(--ease), box-shadow .12s; }
.q-btn--clickable:hover { transform: scale(1.1); box-shadow: 0 2px 8px rgba(0,0,0,.12); }
.q-btn__mark {
  position: absolute;
  top: 2px;
  right: 3px;
  font-size: .65rem;
  line-height: 1;
}

.grid-legend {
  display: flex;
  gap: 16px;
  font-size: .8rem;
  color: var(--ink-faint);
}
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}
.legend-dot--correct { background: var(--correct-bg); border: 1px solid var(--correct); }
.legend-dot--wrong { background: #fff0f0; border: 1px solid var(--wrong); }
.legend-dot--unanswered { background: #f5f5f5; border: 1px solid #bbb; }
.legend-dot--listening { background: #f0f0ff; border: 1px solid #999; }

/* === Actions === */
.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
