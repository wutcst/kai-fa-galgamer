function DialogueChoicePanel({ choices, loading, onChoose }) {
  return (
    <div className="adv-choice-panel" onMouseDown={(event) => event.stopPropagation()}>
      {choices.map((choice) => (
        <button
          key={choice.choiceId}
          className={`adv-choice-button${choice.available ? '' : ' is-locked'}`}
          type="button"
          disabled={loading || !choice.available}
          title={choice.available ? '' : `锁定：${choice.lockedReason || '条件未满足'}`}
          onClick={() => onChoose(choice.choiceId)}
        >
          <span>{choice.available ? 'BRANCH' : 'LOCKED'}</span>
          <strong>{choice.text}</strong>
          {!choice.available ? <small>{choice.lockedReason || '条件未满足'}</small> : null}
        </button>
      ))}
    </div>
  )
}

export default DialogueChoicePanel
