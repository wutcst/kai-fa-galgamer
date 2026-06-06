function BattlePanel({ boss, loading, onAction }) {
  if (!boss) return null
  const hpPercent = boss.maxHp > 0 ? Math.max(0, Math.round((boss.hp / boss.maxHp) * 100)) : 0

  return (
    <section className="battle-panel">
      <header>
        <span>{boss.phase}</span>
        <h2>{boss.name}</h2>
      </header>
      <div className="boss-hp">
        <span style={{ width: `${hpPercent}%` }} />
      </div>
      <p>{boss.hp} / {boss.maxHp}</p>
      <div className="battle-actions">
        {(boss.actions ?? []).map((action) => (
          <button key={action.target} disabled={loading} onClick={() => onAction(action)}>
            {action.label}
          </button>
        ))}
      </div>
      <ul>
        {(boss.battleLogs ?? []).map((log, index) => <li key={`${index}-${log}`}>{log}</li>)}
      </ul>
    </section>
  )
}

export default BattlePanel
