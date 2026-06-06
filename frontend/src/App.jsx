import { useEffect, useState } from 'react'
import { initGame, loadAssetManifest, performAction, saveGame } from './api/gameApi'
import GameScreen from './components/GameScreen'
import './App.css'

const fallbackSnapshot = {
  currentRoomId: 'loading',
  roomTitle: '命运正在展开',
  roomDescription: '正在连接后端游戏会话。',
  playerHp: 100,
  inventoryItems: [],
  gamePhase: 'EXPLORING',
  roomAssetKey: 'fallback',
  availableActions: [],
  logs: [],
  systemMessage: '',
  errorMessage: null,
}

function App() {
  const [snapshot, setSnapshot] = useState(fallbackSnapshot)
  const [assets, setAssets] = useState({ fallback: '/assets/scenes/scene-fallback.png' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    async function boot() {
      setLoading(true)
      try {
        const [manifest, initialSnapshot] = await Promise.all([loadAssetManifest(), initGame()])
        if (!active) {
          return
        }
        setAssets(manifest)
        setSnapshot(initialSnapshot)
        setError('')
      } catch (err) {
        if (active) {
          setError(`启动失败：${err.message}`)
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    boot()
    return () => {
      active = false
    }
  }, [])

  const handleAction = async (action) => {
    setLoading(true)
    try {
      const nextSnapshot =
        action.actionType === 'SAVE' ? await saveGame(action.target ?? 'slot_1') : await performAction(action)
      setSnapshot(nextSnapshot)
      setError('')
    } catch (err) {
      setError(`动作失败：${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <GameScreen snapshot={snapshot} assets={assets} loading={loading} onAction={handleAction} />
      {loading ? <div className="loading-mask">命运骰正在转动...</div> : null}
      {error ? <div className="global-error">{error}</div> : null}
    </>
  )
}

export default App
