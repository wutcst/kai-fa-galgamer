function MiniGameOutcomePanel({ outcome, assets, loading, onAction }) {
  const rescueOptions = Array.isArray(outcome.rescueOptions) ? outcome.rescueOptions : []
  const outcomeArt = assets[outcomeAssetKey(outcome.resultType)] ?? assets.fallback

  return (
    <section className={`minigame-panel outcome-panel result-${outcome.resultType?.toLowerCase()}`}>
      <header className="minigame-header">
        <span>{outcome.gameId}</span>
        <h2>{resultTitle(outcome.resultType)}</h2>
      </header>
      <img className="outcome-art" src={outcomeArt} alt="" />
      <p className="minigame-message">{outcome.message}</p>
      <dl className="outcome-stats">
        <div>
          <dt>Score</dt>
          <dd>{outcome.score}</dd>
        </div>
        <div>
          <dt>Rewards</dt>
          <dd>{outcome.rewardItems?.length ? outcome.rewardItems.join(', ') : 'none'}</dd>
        </div>
      </dl>

      {rescueOptions.length ? (
        <div className="rescue-options">
          {rescueOptions.map((option) => (
            <article key={option.id} className="rescue-card">
              <img src={assets[option.assetKey] ?? assets.fallback} alt="" />
              <div>
                <strong>{option.label}</strong>
                <p>{option.description}</p>
                <span>{option.cost}</span>
              </div>
              <button
                type="button"
                disabled={loading}
                onClick={() => onAction({
                  actionType: 'RESCUE_MINI_GAME_RESULT',
                  target: outcome.sessionId,
                  value: option.id,
                  payload: { rescueId: option.id },
                })}
              >
                执行救赎
              </button>
            </article>
          ))}
        </div>
      ) : null}

      <button
        className="minigame-primary"
        type="button"
        disabled={loading}
        onClick={() => onAction({ actionType: 'ACK_MINI_GAME_RESULT', target: outcome.sessionId, value: null })}
      >
        确认结果
      </button>
    </section>
  )
}

function outcomeAssetKey(type) {
  switch (type) {
    case 'GREAT_SUCCESS':
    case 'SUCCESS':
      return 'minigame.outcome.success'
    case 'RESCUED_BY_ITEM':
      return 'minigame.outcome.rescue'
    default:
      return 'minigame.outcome.failure'
  }
}

function resultTitle(type) {
  switch (type) {
    case 'GREAT_SUCCESS':
      return '大成功'
    case 'SUCCESS':
      return '成功'
    case 'RESCUED_BY_ITEM':
      return '救赎成功'
    case 'CANCELLED':
      return '已取消'
    default:
      return '失败'
  }
}

export default MiniGameOutcomePanel
