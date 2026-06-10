import { useEffect, useState } from 'react'
import { getMenu, initGame, loadAssetManifest, loadGame, performAction, saveGame } from './api/gameApi'
import CreatorMode from './components/CreatorMode'
import EndingPanel from './components/EndingPanel'
import FailurePanel from './components/FailurePanel'
import GameScreen from './components/GameScreen'
import MainMenu from './components/MainMenu'
import SaveModal from './components/SaveModal'
import './App.css'

const fallbackSnapshot = {
  currentRoomId: 'loading',
  roomTitle: '命运正在展开',
  roomDescription: '正在连接后端游戏会话。',
  playerHp: 100,
  inventoryItems: [],
  gamePhase: 'MAIN_MENU',
  roomAssetKey: 'fallback',
  dialogue: null,
  availableActions: [],
  puzzle: null,
  miniGame: null,
  miniGameOutcome: null,
  menu: null,
  save: { slots: [] },
  battle: null,
  choices: [],
  ending: null,
  failure: null,
  creator: null,
  flags: {},
  map: { rooms: [], exits: [] },
  logs: [],
  systemMessage: '',
  errorMessage: null,
}

function App() {
  const [snapshot, setSnapshot] = useState(fallbackSnapshot)
  const [assets, setAssets] = useState({ fallback: '/assets/scenes/scene-fallback.png' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saveOpen, setSaveOpen] = useState(false)

  useEffect(() => {
    let active = true

    async function boot() {
      setLoading(true)
      try {
        const [manifest, initialSnapshot] = await Promise.all([loadAssetManifest(), getMenu()])
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
      let nextSnapshot
      if (action.actionType === 'SAVE') {
        nextSnapshot = await saveGame(action.target ?? 'slot_1')
      } else if (action.actionType === 'LOAD') {
        nextSnapshot = await loadGame(action.target ?? 'slot_1')
      } else if (action.actionType === 'NEW_GAME') {
        nextSnapshot = await initGame()
      } else {
        nextSnapshot = await performAction(action)
      }
      setSnapshot(nextSnapshot)
      setError('')
      if (['SAVE', 'LOAD'].includes(action.actionType)) {
        setSaveOpen(false)
      }
    } catch (err) {
      setError(`动作失败：${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {snapshot.gamePhase === 'MAIN_MENU' ? (
        <MainMenu snapshot={snapshot} assets={assets} loading={loading} onAction={handleAction} />
      ) : snapshot.gamePhase === 'CREATOR' ? (
        <CreatorMode snapshot={snapshot} loading={loading} onSnapshot={setSnapshot} onAction={handleAction} />
      ) : snapshot.gamePhase === 'ENDING' && snapshot.ending && !snapshot.choices?.length ? (
        <EndingPanel snapshot={snapshot} assets={assets} loading={loading} onAction={handleAction} />
      ) : snapshot.gamePhase === 'GAME_OVER' ? (
        <FailurePanel snapshot={snapshot} assets={assets} loading={loading} onAction={handleAction} />
      ) : (
        <GameScreen
          snapshot={snapshot}
          assets={assets}
          loading={loading}
          onAction={handleAction}
          onOpenSave={() => setSaveOpen(true)}
        />
      )}
      {saveOpen ? <SaveModal snapshot={snapshot} loading={loading} onAction={handleAction} onClose={() => setSaveOpen(false)} /> : null}
      {loading ? (
        <div className="loading-mask">
          {assets['ui.loading_spinner'] ? <img src={assets['ui.loading_spinner']} alt="" /> : null}
          <span>命运骰正在转动...</span>
        </div>
      ) : null}
      {error ? <div className="global-error">{error}</div> : null}
    </>
  )
}

export default App
