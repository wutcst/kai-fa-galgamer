function MiniGameOutcomePanel({ outcome, loading, onAction }) {
  return (
    <section className={`minigame-panel outcome-panel result-${outcome.resultType?.toLowerCase()}`}>
      <header className="minigame-header">
        <span>{outcome.gameId}</span>
        <h2>{resultTitle(outcome.resultType)}</h2>
      </header>
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

function resultTitle(type) {
  switch (type) {
    case 'GREAT_SUCCESS':
      return '大成功'
    case 'SUCCESS':
      return '成功'
    case 'CANCELLED':
      return '已取消'
    default:
      return '失败'
  }
}

export default MiniGameOutcomePanel
