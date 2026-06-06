import { useMemo, useState } from 'react'

function MiniGamePanel({ miniGame, assets, loading, onAction }) {
  if (miniGame.gameId === 'dice_check') {
    return <DicePanel miniGame={miniGame} assets={assets} loading={loading} onAction={onAction} />
  }
  if (miniGame.gameId === 'point_game') {
    return <PointPanel miniGame={miniGame} assets={assets} loading={loading} onAction={onAction} />
  }
  if (miniGame.gameId === 'link_match') {
    return <LinkMatchPanel miniGame={miniGame} assets={assets} loading={loading} onAction={onAction} />
  }
  return null
}

function DicePanel({ miniGame, assets, loading, onAction }) {
  const state = miniGame.state ?? {}
  const dice = Array.isArray(state.dice) ? state.dice : [0, 0]
  const bg = assets['minigame.dice.bg'] ?? assets.fallback
  return (
    <section className="minigame-panel dice-panel" style={{ backgroundImage: `url("${bg}")` }}>
      <MiniGameHeader miniGame={miniGame} title="双骰判定" />
      <div className="dice-row">
        {dice.map((value, index) => <div className="die-face" key={index}>{value || '?'}</div>)}
      </div>
      <p className="minigame-score">Total: {state.total ?? '--'}</p>
      <button className="minigame-primary" disabled={loading} onClick={() => input(onAction, miniGame, 'roll')}>
        掷骰
      </button>
    </section>
  )
}

function PointPanel({ miniGame, assets, loading, onAction }) {
  const state = miniGame.state ?? {}
  const cards = Array.isArray(state.cards) ? state.cards : []
  const bg = assets['minigame.point.bg'] ?? assets.fallback
  return (
    <section className="minigame-panel point-panel" style={{ backgroundImage: `url("${bg}")` }}>
      <MiniGameHeader miniGame={miniGame} title="镜面点数" />
      <div className="point-total">{state.total ?? 0}</div>
      <div className="card-row">
        {cards.length ? cards.map((card, index) => <span className="point-card" key={`${index}-${card}`}>{card}</span>) : <span className="point-card is-empty">?</span>}
      </div>
      <div className="minigame-actions">
        <button className="minigame-primary" disabled={loading} onClick={() => input(onAction, miniGame, 'draw')}>抽取</button>
        <button className="minigame-secondary" disabled={loading} onClick={() => input(onAction, miniGame, 'stand')}>停手</button>
      </div>
    </section>
  )
}

function LinkMatchPanel({ miniGame, assets, loading, onAction }) {
  const [selected, setSelected] = useState([])
  const state = miniGame.state ?? {}
  const tiles = Array.isArray(state.board) ? state.board : []
  const gridSize = useMemo(() => Math.sqrt(tiles.length || 16), [tiles.length])
  const bg = assets['minigame.link.bg'] ?? assets.fallback

  const choose = (tile) => {
    if (loading || tile.empty) return
    const next = selected.some((item) => item.row === tile.row && item.col === tile.col) ? [] : [...selected, tile].slice(-2)
    setSelected(next)
    if (next.length === 2) {
      onAction({
        actionType: 'MINI_GAME_INPUT',
        target: miniGame.sessionId,
        value: 'match',
        payload: { rowA: next[0].row, colA: next[0].col, rowB: next[1].row, colB: next[1].col },
      })
      setSelected([])
    }
  }

  return (
    <section className="minigame-panel link-panel" style={{ backgroundImage: `url("${bg}")` }}>
      <MiniGameHeader miniGame={miniGame} title="符文连线" />
      <div className="link-board" style={{ gridTemplateColumns: `repeat(${gridSize}, minmax(0, 1fr))` }}>
        {tiles.map((tile) => (
          <button
            className={`link-tile ${tile.empty ? 'is-empty' : ''} ${isSelected(selected, tile) ? 'is-selected' : ''}`}
            key={`${tile.row}-${tile.col}`}
            disabled={loading || tile.empty}
            onClick={() => choose(tile)}
          >
            {!tile.empty ? <img src={tileAsset(assets, tile.symbol)} alt={`tile ${tile.symbol}`} /> : null}
          </button>
        ))}
      </div>
      <p className="minigame-message">{state.message}</p>
      <div className="minigame-actions">
        <button className="minigame-secondary" disabled={loading} onClick={() => input(onAction, miniGame, 'cancel')}>结束</button>
      </div>
    </section>
  )
}

function MiniGameHeader({ miniGame, title }) {
  return (
    <header className="minigame-header">
      <span>{miniGame.gameId}</span>
      <h2>{title}</h2>
    </header>
  )
}

function input(onAction, miniGame, value) {
  onAction({ actionType: 'MINI_GAME_INPUT', target: miniGame.sessionId, value, payload: {} })
}

function isSelected(selected, tile) {
  return selected.some((item) => item.row === tile.row && item.col === tile.col)
}

function tileAsset(assets, symbol) {
  return assets[`minigame.link.tile.${symbol}`] ?? assets.fallback
}

export default MiniGamePanel
