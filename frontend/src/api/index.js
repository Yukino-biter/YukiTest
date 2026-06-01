const BASE = '/api/yuki'

function authHeaders() {
  const token = localStorage.getItem('yuki_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request(method, url, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json', ...authHeaders() } }
  if (body) opts.body = JSON.stringify(body)
  const res = await fetch(BASE + url, opts)
  const data = await res.json()
  if (res.status === 401) {
    localStorage.removeItem('yuki_token')
    window.location.href = '/login'
    throw new Error('未认证')
  }
  if (data.code !== 0) throw new Error(data.message || '请求失败')
  return data.data
}

export const api = {
  // Auth
  register: (body) => request('POST', '/user/register', body),
  login:    (body) => request('POST', '/user/login', body),

  // Papers
  getPapers:  ()      => request('GET',  '/papers'),
  getPaper:   async (id) => {
    const data = await request('GET', `/papers/${id}`)
    // Parse options JSON string → object
    if (data.mains) {
      data.mains.forEach(main => {
        if (main.items) {
          main.items.forEach(item => {
            if (typeof item.options === 'string') {
              try { item.options = JSON.parse(item.options) } catch {}
            }
          })
        }
      })
    }
    return data
  },
  getTime:    (level) => request('GET',  `/time/${level}`),

  // Exam
  submitExam: (body) => request('POST', '/exam/submit', body),
  getExamResult: (attemptId) => request('GET', `/exam/result/${attemptId}`),
  saveProgress: (body) => request('POST', '/exam/save-progress', body),
  getDraft: (paperId) => request('GET', `/exam/draft/${paperId}`),
  clearDraft: (paperId) => request('POST', `/exam/clear-draft/${paperId}`),

  // AI
  analyzeStream: async (questionItemId) => {
    const token = localStorage.getItem('yuki_token')
    const res = await fetch(BASE + '/ai/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ questionItemId })
    })
    return res.body
  },

  // Wrong book
  getWrongBook: (params = {}) => {
    const qs = new URLSearchParams(params).toString()
    return request('GET', `/wrong-book?${qs}`)
  },
  markResolved: (id) => request('PUT', `/wrong-book/${id}/resolve`),

  // Stats
  getStats: () => request('GET', '/stats'),

  // User
  getMe:         ()      => request('GET',  '/user/me'),
  updateAiConfig: (body) => request('POST', '/user/update-ai-config', body),
}
