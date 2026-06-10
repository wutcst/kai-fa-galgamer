import { useEffect, useMemo, useState } from 'react'

const TYPE_INTERVAL_MS = 30

function DialogueBox({ dialogue, loading, onAdvance }) {
  const fullText = useMemo(() => dialogue.text ?? '', [dialogue.text])
  const nodeKey = `${dialogue.currentNodeId}:${fullText}`
  const [typing, setTyping] = useState({ key: nodeKey, count: 0 })

  if (typing.key !== nodeKey) {
    setTyping({ key: nodeKey, count: 0 })
  }

  useEffect(() => {
    if (typing.key !== nodeKey || typing.count >= fullText.length) {
      return undefined
    }

    const timer = window.setTimeout(() => {
      setTyping((current) => (
        current.key === nodeKey
          ? { ...current, count: Math.min(current.count + 1, fullText.length) }
          : current
      ))
    }, TYPE_INTERVAL_MS)

    return () => window.clearTimeout(timer)
  }, [fullText.length, nodeKey, typing.count, typing.key])

  const visibleCount = typing.key === nodeKey ? typing.count : 0
  const visibleText = fullText.slice(0, visibleCount)
  const complete = visibleText.length >= fullText.length

  const handleClick = (event) => {
    event.stopPropagation()
    if (loading) {
      return
    }
    if (!complete) {
      setTyping((current) => ({ ...current, count: fullText.length }))
      return
    }
    onAdvance()
  }

  return (
    <article className="adv-dialogue-box" onMouseDown={handleClick}>
      <header>
        <span>{dialogue.speakerSide === 'NARRATOR' ? '旁白' : dialogue.speakerName}</span>
        <small>{complete ? 'NEXT' : 'TYPE'}</small>
      </header>
      <p>{visibleText}</p>
    </article>
  )
}

export default DialogueBox
