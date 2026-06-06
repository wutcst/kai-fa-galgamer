import { useEffect, useState } from 'react'

const battleActions = [
  { id: 'attack', label: '攻击', mark: '⚔' },
  { id: 'defend', label: '防御', mark: '◆' },
  { id: 'roll', label: '掷命运骰', mark: '◇' },
  { id: 'use_soul_bell', label: '灵魂之铃', mark: '◎' },
]

function BattlePanel({ battle, assets, loading, playerHp, onAction }) {
  const [effect, setEffect] = useState('')
  const enemyHp = battle.enemyHp ?? battle.enemyHealth ?? 0
  const enemyMaxHp = battle.enemyMaxHp ?? battle.enemyMaxHealth ?? 1
  const hpRatio = Math.max(0, Math.min(1, enemyHp / enemyMaxHp))
  const playerRatio = Math.max(0, Math.min(1, (playerHp ?? 0) / 100))
  const phase = battle.phase ?? 1
  const bossArt = assets[battle.assetKey ?? 'character.zuul_overlord'] ?? assets.fallback

  useEffect(() => {
    if (!battle.lastAction) {
      return undefined
    }
    const nextEffect = battle.lastAction === 'use_soul_bell' ? 'bell' : battle.lastAction
    setEffect(nextEffect)
    const timer = window.setTimeout(() => setEffect(''), 520)
    return () => window.clearTimeout(timer)
  }, [battle.enemyHp, battle.lastAction, battle.phase, battle.turn])

  return (
    <section className={`battle-panel phase-${phase} ${effect ? `effect-${effect}` : ''}`}>
      <div className="player-health-strip" aria-label="Player health">
        <span>你的生命</span>
        <div><b style={{ width: `${playerRatio * 100}%` }} /></div>
        <strong>{playerHp ?? 0} / 100</strong>
      </div>
      <img className="boss-art" src={bossArt} alt={`${battle.enemyName} portrait`} />
      <div className="battle-copy">
        <header className="battle-header">
          <span>Boss Phase {phase}</span>
          <h2>{battle.enemyName ?? 'Zuul Overlord'}</h2>
        </header>

        <div className="boss-health" aria-label="Boss health">
          <b style={{ width: `${hpRatio * 100}%` }} />
        </div>

        <p className="battle-meta">
          HP {enemyHp} / {enemyMaxHp}
          {battle.traits?.length ? ` · ${battle.traits.join(' · ')}` : ''}
        </p>

        <p className="battle-message">{battle.message ?? '祖尔霸主正在等待你的行动。'}</p>

        <nav className="battle-actions" aria-label="Boss 战行动">
          {battleActions.map((action) => (
            <button
              key={action.id}
              type="button"
              disabled={loading}
              onClick={() =>
                onAction({
                  actionType: 'BATTLE_ACTION',
                  target: action.id,
                  value: action.id,
                })
              }
            >
              <span>{action.mark}</span>
              {action.label}
            </button>
          ))}
        </nav>
      </div>
    </section>
  )
}

export default BattlePanel
