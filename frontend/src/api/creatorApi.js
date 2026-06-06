import axios from 'axios'

const http = axios.create({
  headers: {
    'Content-Type': 'application/json',
  },
})

async function request(config) {
  try {
    const response = await http(config)
    return response.data
  } catch (error) {
    if (error.response) {
      throw new Error(`HTTP ${error.response.status}`, { cause: error })
    }
    throw new Error(error.message || '网络请求失败', { cause: error })
  }
}

export function getCreatorStatus() {
  return request({ method: 'GET', url: '/api/creator/status' })
}

export function listChapters() {
  return request({ method: 'GET', url: '/api/creator/chapters' })
}

export function validateChapter(chapter) {
  return request({ method: 'POST', url: '/api/creator/validate', data: chapter })
}

export function saveChapter(chapter) {
  return request({ method: 'POST', url: '/api/creator/chapters', data: chapter })
}

export function playChapter(chapterId) {
  return request({ method: 'POST', url: `/api/creator/chapters/${encodeURIComponent(chapterId)}/play` })
}
