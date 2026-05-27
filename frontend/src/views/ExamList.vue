<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'

const router = useRouter()
const papers = ref([])
const loading = ref(true)
const currentLevel = ref('ALL')

const LEVEL_TABS = [
  { key: 'ALL',        label: '全部' },
  { key: 'N1',         label: 'N1' },
  { key: 'N2',         label: 'N2' },
  { key: 'N3',         label: 'N3' },
  { key: 'N4_OR_BELOW', label: 'N4/N5' },
]

const filteredPapers = computed(() => {
  if (currentLevel.value === 'ALL') return papers.value
  if (currentLevel.value === 'N4_OR_BELOW') {
    return papers.value.filter(p => p.level === 'N4' || p.level === 'N5')
  }
  return papers.value.filter(p => p.level === currentLevel.value)
})

const gridClass = computed(() =>
  filteredPapers.value.length <= 1 ? 'paper-grid paper-grid--single' : 'paper-grid'
)

onMounted(async () => {
  try {
    papers.value = await api.getPapers()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function startExam(paperId) {
  router.push(`/exam/${paperId}`)
}

function levelClass(level) {
  return `badge badge--${level}`
}
</script>

<template>
  <div class="main">
    <h1 class="page-title">试卷列表</h1>

    <!-- Level filter tabs -->
    <div v-if="!loading && papers.length > 0" class="level-tabs">
      <button
        v-for="tab in LEVEL_TABS"
        :key="tab.key"
        class="level-tab"
        :class="[
          `level-tab--${tab.key.toLowerCase()}`,
          { 'level-tab--active': currentLevel === tab.key }
        ]"
        @click="currentLevel = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="papers.length === 0" class="empty">
      <div class="empty__icon">📝</div>
      <p>暂无已发布的试卷</p>
      <p class="empty__hint">请通过管理接口导入题库后发布试卷</p>
    </div>

    <div v-else-if="filteredPapers.length === 0" class="empty">
      <div class="empty__icon">📭</div>
      <p>该等级暂无试卷</p>
    </div>

    <div v-else :class="gridClass">
      <div v-for="p in filteredPapers" :key="p.id" class="card paper-card" @click="startExam(p.id)">
        <div class="paper-card__header">
          <span :class="levelClass(p.level)">{{ p.level }}</span>
          <span class="paper-card__time">{{ p.examMinutes }} 分钟</span>
        </div>
        <h3 class="paper-card__name">{{ p.paperName }}</h3>
        <button class="btn btn--primary btn--sm">开始考试</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  margin-bottom: 20px;
}

/* === Level filter tabs === */
.level-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.level-tab {
  padding: 8px 24px;
  border-radius: 999px;
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 600;
  border: 1.5px solid var(--paper-dark);
  background: #fff;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s var(--ease);
  letter-spacing: 0.02em;
}

.level-tab:hover {
  border-color: var(--ink-faint);
  color: var(--ink);
}

/* Active states per level */
.level-tab--active.level-tab--all {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.level-tab--active.level-tab--n1 {
  background: #d32f2f;
  border-color: #d32f2f;
  color: #fff;
}

.level-tab--active.level-tab--n2 {
  background: #f57c00;
  border-color: #f57c00;
  color: #fff;
}

.level-tab--active.level-tab--n3 {
  background: var(--n3);
  border-color: var(--n3);
  color: var(--ink);
}

.level-tab--active.level-tab--n4_or_below {
  background: var(--n4);
  border-color: var(--n4);
  color: #fff;
}

/* === Paper grid === */
.paper-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.paper-grid--single {
  grid-template-columns: 1fr;
}

.paper-grid--single .paper-card {
  max-width: 340px;
}

.paper-card { cursor: pointer; }

.paper-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.paper-card__time {
  font-size: .8rem;
  color: var(--ink-faint);
}

.paper-card__name {
  font-size: 1rem;
  font-weight: 500;
  margin-bottom: 16px;
  line-height: 1.4;
}

.empty {
  text-align: center;
  padding: 80px 20px;
  color: var(--ink-faint);
}
.empty__icon { font-size: 3rem; margin-bottom: 16px; }
.empty__hint { font-size: .85rem; margin-top: 8px; }

/* === Responsive === */
@media (max-width: 1024px) {
  .paper-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .paper-grid {
    grid-template-columns: 1fr;
  }
  .level-tabs {
    gap: 8px;
  }
  .level-tab {
    padding: 6px 18px;
    font-size: .8rem;
  }
}
</style>
