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

export function initGame() {
  return request({
    method: 'GET',
    url: '/api/game/init',
  })
}

export function performAction(action) {
  return request({
    method: 'POST',
    url: '/api/game/action',
    data: action,
  })
}

export function saveGame(saveId = 'slot_1') {
  return request({
    method: 'POST',
    url: '/api/game/save',
    data: { saveId },
  })
}

export function loadAssetManifest() {
  return request({
    method: 'GET',
    url: '/assets/asset-manifest.json',
  })
}
