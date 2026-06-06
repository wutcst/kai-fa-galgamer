import DirectionButtons from './DirectionButtons'

function GameScreen({ snapshot, assets, loading, onAction }) {
  const sceneUrl = assets[snapshot.roomAssetKey] ?? assets.fallback
  const utilityActions = snapshot.availableActions.filter((action) => action.actionType !== 'MOVE')

  return (
    <main className="game-screen">
      <div className="scene-image" style={{ backgroundImage: `url("${sceneUrl}")` }} />
      <div className="scene-vignette" />

      <header className="top-hud">
        <div className="brand-mark">
          <strong>World of Zuul</strong>
          <span>Uncertain Fate</span>
        </div>
        <div className="hp-panel">
          <span>HP</span>
          <strong>{snapshot.playerHp}</strong>
        </div>
        <div className="phase-panel">{snapshot.gamePhase}</div>
      </header>

      <section className="narrative-layout">
        <article className="story-panel">
          <p className="room-id">{snapshot.currentRoomId}</p>
          <h1>{snapshot.roomTitle}</h1>
          <p>{snapshot.roomDescription}</p>
          {snapshot.systemMessage ? <strong className="system-message">{snapshot.systemMessage}</strong> : null}
          {snapshot.errorMessage ? <strong className="error-message">{snapshot.errorMessage}</strong> : null}
        </article>

        <aside className="control-panel">
          <DirectionButtons actions={snapshot.availableActions} onAction={onAction} disabled={loading} />

          <div className="action-row">
            {utilityActions.map((action) => (
              <button
                key={`${action.actionType}-${action.target}`}
                className="action-button"
                type="button"
                disabled={loading}
                onClick={() =>
                  onAction({
                    actionType: action.actionType,
                    target: action.target,
                    value: null,
                  })
                }
              >
                {action.label}
              </button>
            ))}
          </div>
        </aside>
      </section>

      <aside className="log-panel">
        <h2>探索日志</h2>
        <ul>
          {snapshot.logs?.slice(-5).map((log, index) => (
            <li key={`${index}-${log}`}>{log}</li>
          ))}
        </ul>
      </aside>
    </main>
  )
}

export default GameScreen
