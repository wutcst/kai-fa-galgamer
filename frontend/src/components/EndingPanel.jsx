function EndingPanel({ snapshot, assets, loading, onAction }) {
  const ending = snapshot.ending
  const bg = assets[ending?.assetKey] ?? assets['ending.inherited_fate'] ?? assets.fallback

  return (
    <main className="ending-screen" style={{ backgroundImage: `url("${bg}")` }}>
      <section className="ending-panel">
        <p>{ending?.endingId ?? 'ending'}</p>
        <h1>{ending?.title ?? '结局'}</h1>
        <article>{ending?.text ?? snapshot.systemMessage}</article>
        <div className="main-menu-actions">
          {(ending?.actions ?? snapshot.availableActions ?? []).map((action) => (
            <button key={`${action.actionType}-${action.target}`} disabled={loading} onClick={() => onAction(action)}>
              {action.label}
            </button>
          ))}
        </div>
      </section>
    </main>
  )
}

export default EndingPanel
