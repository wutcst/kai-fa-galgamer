import { useEffect, useMemo, useState } from 'react'

const battleActions = [
  { id: 'attack', label: '攻击', mark: 'ATK' },
  { id: 'defend', label: '防御', mark: 'DEF' },
  { id: 'roll', label: '掷命运骰', mark: 'D100' },
  { id: 'use_soul_bell', label: '灵魂之铃', mark: 'BELL' },
]

const intentAdvice = {
  HEAVY_STRIKE: {
    best: ['defend'],
    danger: ['attack'],
  },
  FATE_DISTORTION: {
    best: ['roll', 'use_soul_bell'],
    danger: ['attack'],
  },
  CORE_CHARGE: {
    best: ['attack', 'use_soul_bell'],
    danger: ['defend'],
  },
}

function BattlePanel({ battle, assets, loading, playerHp, onAction }) {
  const enemyHp = battle.enemyHp ?? battle.enemyHealth ?? 0
  const enemyMaxHp = battle.enemyMaxHp ?? battle.enemyMaxHealth ?? 1
  const hpRatio = Math.max(0, Math.min(1, enemyHp / enemyMaxHp))
  const playerRatio = Math.max(0, Math.min(1, (playerHp ?? 0) / 100))
  const phase = battle.phase ?? 1
  const bossArt = assets[battle.assetKey ?? 'character.zuul_overlord'] ?? assets.fallback
  const intentIcon = assets[battle.intentAssetKey] ?? assets['battle.intent.heavy'] ?? assets.fallback
  const diceArt = assets['battle.d100.die'] ?? assets.fallback
  const impactVfx = assets['ui.battle_impact_vfx']
  const effect = battle.lastAction === 'use_soul_bell' ? 'bell' : battle.lastAction
  const [rolling, setRolling] = useState(false)
  const [displayRoll, setDisplayRoll] = useState(battle.d100Result || 0)
  const hints = Array.isArray(battle.battleHints) ? battle.battleHints : []
  const soulBellSpent = hints.some((hint) => String(hint).includes('灵魂之铃已在本战使用'))

  useEffect(() => {
    if (!battle.d100Result) {
      return undefined
    }
    let interval
    let timeout
    const start = window.setTimeout(() => {
      setRolling(true)
      interval = window.setInterval(() => {
        setDisplayRoll(Math.floor(Math.random() * 100) + 1)
      }, 38)
      timeout = window.setTimeout(() => {
        window.clearInterval(interval)
        setDisplayRoll(battle.d100Result)
        setRolling(false)
      }, 500)
    }, 0)
    return () => {
      window.clearTimeout(start)
      if (interval) {
        window.clearInterval(interval)
      }
      if (timeout) {
        window.clearTimeout(timeout)
      }
    }
  }, [battle.turn, battle.d100Result, battle.lastAction])

  const advice = useMemo(() => intentAdvice[battle.currentIntent] ?? intentAdvice.HEAVY_STRIKE, [battle.currentIntent])

  return (
    <section
      key={`${battle.turn}-${battle.enemyHp}-${battle.phase}-${battle.lastAction}`}
      className={`battle-panel phase-${phase} ${effect ? `effect-${effect}` : ''}`}
      style={impactVfx ? { '--battle-impact-vfx': `url("${impactVfx}")` } : undefined}
    >
      <div className="player-health-strip" aria-label="Player health">
        <span>你的生命</span>
        <div><b style={{ width: `${playerRatio * 100}%` }} /></div>
        <strong>{playerHp ?? 0} / 100</strong>
      </div>

      <div className={`boss-intent-banner intent-${String(battle.currentIntent || '').toLowerCase()}`}>
        <img src={intentIcon} alt="" />
        <div>
          <span>Boss 意图</span>
          <strong>{battle.intentLabel ?? '蓄势重击'}</strong>
          <p>{battle.intentDescription ?? '祖尔霸主正在积蓄下一轮行动。'}</p>
        </div>
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

        <div className={`d100-readout ${rolling ? 'is-rolling' : ''}`}>
          <img src={diceArt} alt="" />
          <div>
            <span>D100</span>
            <strong>{displayRoll || '--'}</strong>
            <small>{stepLabel(battle.d100Step)}{battle.bonusDieApplied ? ' · 奖励骰' : ''}</small>
          </div>
        </div>

        <p className="battle-message">{battle.message ?? '祖尔霸主正在等待你的行动。'}</p>

        {hints.length ? (
          <ul className="battle-hints">
            {hints.map((hint) => <li key={hint}>{hint}</li>)}
          </ul>
        ) : null}

        <nav className="battle-actions" aria-label="Boss 战行动">
          {battleActions.map((action) => {
            const tone = advice.best.includes(action.id)
              ? 'is-recommended'
              : advice.danger.includes(action.id)
                ? 'is-danger'
                : ''
            const disabled = loading || (action.id === 'use_soul_bell' && (soulBellSpent || battle.soulBellCooldown > 0))
            return (
              <button
                key={action.id}
                type="button"
                className={tone}
                disabled={disabled}
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
            )
          })}
        </nav>
      </div>
    </section>
  )
}

function stepLabel(step) {
  switch (step) {
    case 1:
      return '大成功'
    case 2:
      return '极难成功'
    case 3:
      return '困难成功'
    case 4:
      return '常规成功'
    case 5:
      return '失败'
    case 6:
      return '大失败'
    default:
      return '等待掷骰'
  }
}

export default BattlePanel
