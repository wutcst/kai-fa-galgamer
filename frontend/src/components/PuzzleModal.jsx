import { useMemo, useState } from 'react'

function PuzzleModal({ puzzle, loading, onAction }) {
  const [answer, setAnswer] = useState('')
  const freeText = Boolean(puzzle.freeText)
  const options = Array.isArray(puzzle.options) ? puzzle.options : []
  const placeholder = useMemo(() => {
    switch (puzzle.kind) {
      case 'DIRECTION_SEQUENCE':
        return '输入方向序列，例如 south north east west'
      case 'ITEM_COMBINATION':
        return '输入要合成的道具 ID'
      case 'SEAL_GATE':
        return '输入 open 检验封印'
      default:
        return '输入答案'
    }
  }, [puzzle.kind])

  const submitValue = (value) => {
    const normalized = String(value ?? '').trim()
    if (!normalized || loading) {
      return
    }
    if (String(puzzle.submitAction).toUpperCase() === 'CRAFT') {
      onAction({ actionType: 'CRAFT', target: normalized, value: null })
    } else {
      onAction({ actionType: 'ANSWER', target: puzzle.id, value: normalized })
    }
    setAnswer('')
  }

  return (
    <section className="puzzle-modal" aria-label="谜题面板">
      <div className="puzzle-header">
        <span>{puzzle.kind}</span>
        <h2>{puzzle.prompt}</h2>
      </div>

      {freeText ? (
        <form
          className="puzzle-input-row"
          onSubmit={(event) => {
            event.preventDefault()
            submitValue(answer)
          }}
        >
          <input
            value={answer}
            disabled={loading}
            placeholder={placeholder}
            onChange={(event) => setAnswer(event.target.value)}
          />
          <button type="submit" disabled={loading || !answer.trim()}>
            提交
          </button>
        </form>
      ) : null}

      {options.length ? (
        <div className="puzzle-options">
          {options.map((option) => (
            <button key={option} type="button" disabled={loading} onClick={() => submitValue(option)}>
              {option.replaceAll('_', ' ')}
            </button>
          ))}
        </div>
      ) : null}
    </section>
  )
}

export default PuzzleModal
