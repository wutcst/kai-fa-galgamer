const JSON_HEADERS = {
  'Content-Type': 'application/json',
}

async function request(path, options) {
  const response = await fetch(path, options)
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  return response.json()
}

export function initGame() {
  return request('/api/game/init')
}

export function performAction(action) {
  return request('/api/game/action', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(action),
  })
}

export function saveGame(saveId = 'slot_1') {
  return request('/api/game/save', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ saveId }),
  })
}

export function loadAssetManifest() {
  return request('/assets/asset-manifest.json')
}
