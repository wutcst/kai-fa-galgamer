function EndingPanel({ snapshot, assets, loading, onAction }) {
  const ending = snapshot.ending
  const bg = assets[ending?.assetKey] ?? assets['ending.inherited_fate'] ?? assets.fallback

  return (
    <main className="ending-screen" style={{ backgroundImage: `url("${bg}")` }}>
      <section className="ending-panel">
        {bg ? <img src={bg} alt="" /> : null}
        <article>
          <p>{ending?.id ?? 'ending'}</p>
          <h1>{ending?.title ?? '结局'}</h1>
          <p style={{ marginTop: '16px', color: '#d9d2c5', lineHeight: '1.75' }}>
            {ending?.description ?? snapshot.systemMessage}
          </p>
          <div className="main-menu-actions" style={{ marginTop: '20px' }}>
            {(ending?.actions ?? snapshot.availableActions ?? []).map((action) => (
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
        </article>
      </section>
    </main>
  )
}

export default EndingPanel
