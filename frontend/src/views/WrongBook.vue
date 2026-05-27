<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'

const router = useRouter()
const items = ref([])
const loading = ref(true)
const filterResolved = ref(null)
const openPapers = ref(new Set())

const sectionOrder = { vocabulary: 0, grammar: 1, reading: 2, listening: 3 }
const sectionLabel = { vocabulary: '文字·词汇', grammar: '语法', reading: '阅读', listening: '听力' }

async function load() {
  loading.value = true
  try {
    const params = { page: 1, size: 1000 }
    if (filterResolved.value !== null) params.resolved = filterResolved.value
    const data = await api.getWrongBook(params)
    items.value = data.records || []
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

// Group by paper, sorted by section order within each group
const grouped = computed(() => {
  const map = new Map()
  for (const item of items.value) {
    const key = item.paperId || 0
    if (!map.has(key)) {
      map.set(key, {
        paperId: item.paperId,
        paperName: item.paperName || '未知试卷',
        level: item.level || 'N?',
        items: []
      })
    }
    map.get(key).items.push(item)
  }
  // Sort items within each group by section order
  for (const group of map.values()) {
    group.items.sort((a, b) => (sectionOrder[a.section] ?? 9) - (sectionOrder[b.section] ?? 9))
  }
  // Sort groups: most recent paper first (by paperId desc)
  return [...map.values()].sort((a, b) => (b.paperId || 0) - (a.paperId || 0))
})

function togglePaper(paperId) {
  if (openPapers.value.has(paperId)) {
    openPapers.value.delete(paperId)
  } else {
    openPapers.value.add(paperId)
  }
}

function isOpen(paperId) {
  return openPapers.value.has(paperId)
}

// Get items by section within a group
function sectionsInGroup(group) {
  const map = new Map()
  for (const item of group.items) {
    const sec = item.section || 'vocabulary'
    if (!map.has(sec)) map.set(sec, [])
    map.get(sec).push(item)
  }
  return [...map.entries()].sort((a, b) => (sectionOrder[a[0]] ?? 9) - (sectionOrder[b[0]] ?? 9))
}

async function markResolved(id) {
  try {
    await api.markResolved(id)
    const item = items.value.find(i => i.id === id)
    if (item) item.isResolved = 1
  } catch (e) {
    alert(e.message)
  }
}

function goToExam(paperId, questionItemId) {
  router.push({ path: `/exam/${paperId}`, query: { focus: questionItemId } })
}

onMounted(load)
</script>

<template>
  <div class="main">
    <h1 class="page-title">错题本</h1>

    <div class="wrong-filters">
      <button class="btn btn--sm" :class="filterResolved === null ? 'btn--primary' : 'btn--ghost'" @click="filterResolved = null; load()">全部</button>
      <button class="btn btn--sm" :class="filterResolved === 0 ? 'btn--primary' : 'btn--ghost'" @click="filterResolved = 0; load()">未掌握</button>
      <button class="btn btn--sm" :class="filterResolved === 1 ? 'btn--primary' : 'btn--ghost'" @click="filterResolved = 1; load()">已掌握</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="grouped.length === 0" class="empty">
      <div class="empty__icon">✅</div>
      <p>暂无错题记录</p>
    </div>

    <div v-else class="accordion">
      <div v-for="group in grouped" :key="group.paperId" class="accordion__item card">
        <!-- Header: clickable -->
        <div class="accordion__header" @click="togglePaper(group.paperId)">
          <div class="accordion__header-left">
            <span class="badge" :class="`badge--${group.level}`">{{ group.level }}</span>
            <span class="accordion__paper-name">{{ group.paperName }}</span>
          </div>
          <div class="accordion__header-right">
            <span class="accordion__count">{{ group.items.length }} 题</span>
            <span class="accordion__arrow" :class="{ 'accordion__arrow--open': isOpen(group.paperId) }">▸</span>
          </div>
        </div>

        <!-- Body: collapsible -->
        <transition name="slide">
          <div v-if="isOpen(group.paperId)" class="accordion__body">
            <template v-for="[sec, secItems] in sectionsInGroup(group)" :key="sec">
              <div class="section-divider">
                <span class="section-divider__label">{{ sectionLabel[sec] || sec }}</span>
                <span class="section-divider__count">{{ secItems.length }} 题</span>
              </div>
              <div v-for="item in secItems" :key="item.id" class="wrong-row" :class="{ 'wrong-row--resolved': item.isResolved }">
                <div class="wrong-row__main">
                  <p class="wrong-row__content">{{ item.content }}</p>
                  <div class="wrong-row__answers">
                    <span class="wrong-row__user">你的: <strong>{{ item.userAnswer }}</strong></span>
                    <span class="wrong-row__correct">正确: <strong>{{ item.correctAnswer }}</strong></span>
                    <span class="wrong-row__wc">错 {{ item.wrongCount }} 次</span>
                  </div>
                </div>
                <div class="wrong-row__actions">
                  <span v-if="item.isResolved" class="wrong-row__resolved-tag">已掌握</span>
                  <template v-else>
                    <button class="btn btn--ghost btn--sm" @click.stop="markResolved(item.id)">标记已掌握</button>
                    <button
                      v-if="sec !== 'listening'"
                      class="btn btn--secondary btn--sm"
                      @click.stop="router.push({ path: `/ai/${item.questionItemId}`, query: { section: sec } })"
                    >AI 解析</button>
                    <button
                      class="btn btn--primary btn--sm"
                      @click.stop="goToExam(item.paperId, item.questionItemId)"
                    >回到试题</button>
                  </template>
                </div>
              </div>
            </template>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-family: var(--font-display); font-size: 1.5rem; margin-bottom: 20px; }
.wrong-filters { display: flex; gap: 8px; margin-bottom: 20px; }

/* === Accordion === */
.accordion { display: flex; flex-direction: column; gap: 10px; }

.accordion__item { padding: 0; overflow: hidden; }

.accordion__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  transition: background .15s;
}
.accordion__header:hover { background: var(--paper); }

.accordion__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.accordion__paper-name {
  font-size: .95rem;
  font-weight: 500;
  color: var(--ink);
}

.accordion__header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.accordion__count {
  font-size: .8rem;
  color: var(--ink-faint);
  font-family: var(--font-mono);
}
.accordion__arrow {
  font-size: 1rem;
  color: var(--ink-faint);
  transition: transform .25s var(--ease);
}
.accordion__arrow--open { transform: rotate(90deg); }

/* === Slide transition === */
.slide-enter-active { animation: slideDown .25s var(--ease); }
.slide-leave-active { animation: slideDown .2s var(--ease) reverse; }
@keyframes slideDown {
  from { max-height: 0; opacity: 0; }
  to { max-height: 2000px; opacity: 1; }
}

.accordion__body {
  border-top: 1px solid var(--paper-dark);
  padding: 0 20px 16px;
}

/* === Section divider === */
.section-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 0 8px;
}
.section-divider__label {
  font-size: .8rem;
  font-weight: 600;
  color: var(--indigo);
  text-transform: uppercase;
  letter-spacing: .04em;
}
.section-divider__count {
  font-size: .75rem;
  color: var(--ink-faint);
  font-family: var(--font-mono);
}

/* === Wrong row === */
.wrong-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--paper-dark);
  border-radius: var(--radius);
  margin-bottom: 8px;
  background: #fff;
  transition: background .15s;
}
.wrong-row:hover { background: var(--paper); }
.wrong-row--resolved { opacity: .55; }

.wrong-row__main { flex: 1; min-width: 0; }
.wrong-row__content {
  font-size: .93rem;
  line-height: 1.6;
  margin-bottom: 8px;
  color: var(--ink);
}
.wrong-row__answers {
  display: flex;
  gap: 16px;
  font-size: .82rem;
  color: var(--ink-light);
  flex-wrap: wrap;
}
.wrong-row__user strong { color: var(--wrong); }
.wrong-row__correct strong { color: var(--correct); }
.wrong-row__wc { color: var(--vermillion); font-family: var(--font-mono); }

.wrong-row__actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  align-items: flex-start;
  flex-wrap: wrap;
}
.wrong-row__resolved-tag {
  font-size: .8rem;
  color: var(--correct);
  font-weight: 500;
  padding: 4px 0;
}

.empty { text-align: center; padding: 60px; color: var(--ink-faint); }
.empty__icon { font-size: 2.5rem; margin-bottom: 12px; }
</style>
