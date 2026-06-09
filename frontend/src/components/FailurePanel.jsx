function FailurePanel({ snapshot, assets, loading, onAction }) {
  const failure = snapshot.failure
  const bg = assets[failure?.assetKey] ?? assets['ending.boss_failure'] ?? assets.fallback
  const actions = failure?.actions ?? snapshot.availableActions ?? []

  return (
    <main className="failure-screen" style={{ backgroundImage: `url("${bg}")` }}>
      <section className="failure-panel">
        <p>Game Over</p>
        <h1>{failure?.title ?? '循环崩塌'}</h1>
        <article>{failure?.description ?? snapshot.systemMessage}</article>
        <div className="main-menu-actions">
          {actions.map((action) => (
            <button
              key={`${action.actionType}-${action.target}`}
              type="button"
              disabled={loading}
              onClick={() => onAction(action)}
            >
              {action.label}
            </button>
          ))}
        </div>
      </section>
    </main>
  )
}

export default FailurePanel
