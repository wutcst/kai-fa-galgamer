import { useMemo, useState } from 'react'

const antiCycleItems = ['blank_dice', 'savebreaker_key', 'nameless_badge', 'pure_seed', 'throne_fragment']

function ChoicePanel({ snapshot, assets, loading, onAction }) {
  const choices = snapshot.choices ?? []
  const ending = snapshot.ending
  const selectableChoices = choices.filter((choice) => choice.unlocked !== false)
  const lastChoiceId = selectableChoices.length ? selectableChoices[selectableChoices.length - 1].id : ''
  const [selected, setSelected] = useState(lastChoiceId)
  const activeChoiceId = selectableChoices.some((choice) => choice.id === selected) ? selected : lastChoiceId
  const keyCount = useMemo(
    () => antiCycleItems.filter((itemId) => snapshot.inventoryItems?.includes(itemId)).length,
    [snapshot.inventoryItems],
  )

  if (ending) {
    const endingArt = assets[ending.assetKey] ?? assets['ending.write_own_chapter'] ?? assets.fallback
    return (
      <section className="choice-panel ending-panel">
        <img src={endingArt} alt="" />
        <article>
          <span>Ending</span>
          <h2>{ending.title}</h2>
          <p>{ending.description}</p>
        </article>
      </section>
    )
  }

  if (!choices.length) {
    return null
  }

  return (
    <section className="choice-panel">
      <header className="choice-header">
        <span>Fate Core</span>
        <h2>选择循环的终点</h2>
        <p>反循环关键道具 {keyCount} / {antiCycleItems.length}</p>
      </header>

      <div className="choice-list">
        {choices.map((choice) => (
          <button
            key={choice.id}
            type="button"
            className={activeChoiceId === choice.id ? 'is-selected' : ''}
            disabled={loading || choice.unlocked === false}
            onClick={() => setSelected(choice.id)}
          >
            <strong>{choice.number}. {choice.label}</strong>
            <span>{choice.description}</span>
          </button>
        ))}
      </div>

      <button
        className="choice-confirm"
        type="button"
        disabled={loading || !activeChoiceId}
        onClick={() =>
          onAction({
            actionType: 'CHOOSE_ENDING',
            target: activeChoiceId,
            value: activeChoiceId,
          })
        }
      >
        确认结局
      </button>
    </section>
  )
}

export default ChoicePanel
