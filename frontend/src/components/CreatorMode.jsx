import { useEffect, useState } from 'react'
import { listChapters, playChapter, saveChapter, validateChapter } from '../api/creatorApi'
import ChapterEditor from './ChapterEditor'
import { sampleChapterText } from './sampleChapter'

function CreatorMode({ snapshot, loading, onSnapshot, onAction }) {
  const creator = snapshot.creator ?? {}
  const [chapters, setChapters] = useState(creator.chapters ?? [])
  const [text, setText] = useState(sampleChapterText())
  const [errors, setErrors] = useState(creator.validationErrors ?? [])
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    listChapters()
      .then((items) => {
        if (active) setChapters(items)
      })
      .catch((error) => {
        if (active) setMessage(error.message)
      })
    return () => {
      active = false
    }
  }, [])

  const parse = () => JSON.parse(text)

  const validate = async () => {
    try {
      const result = await validateChapter(parse())
      setErrors(result.errors ?? [])
      setMessage(result.valid ? '校验通过。' : '校验失败。')
    } catch (error) {
      setErrors([error.message])
    }
  }

  const save = async () => {
    try {
      const summary = await saveChapter(parse())
      setChapters((items) => [...items.filter((item) => item.chapterId !== summary.chapterId), summary])
      setMessage('章节已保存。')
      setErrors([])
    } catch (error) {
      setErrors([error.message])
    }
  }

  const play = async (chapterId) => {
    try {
      const next = await playChapter(chapterId)
      onSnapshot(next)
    } catch (error) {
      setErrors([error.message])
    }
  }

  return (
    <main className="creator-screen">
      <section className="creator-sidebar">
        <p>Creator Mode</p>
        <h1>{creator.unlocked ? '创作者模式' : '创作者模式未解锁'}</h1>
        <button disabled={loading} onClick={() => onAction({ actionType: 'NEW_GAME' })}>返回新游戏</button>
        <div className="chapter-list">
          {chapters.map((chapter) => (
            <article key={chapter.chapterId}>
              <h3>{chapter.title}</h3>
              <p>{chapter.chapterId} · {chapter.rooms} rooms</p>
              <button disabled={loading || !creator.unlocked} onClick={() => play(chapter.chapterId)}>试玩</button>
            </article>
          ))}
        </div>
      </section>
      <section className="creator-editor">
        <ChapterEditor value={text} onChange={setText} />
        <div className="creator-actions">
          <button disabled={loading} onClick={validate}>校验</button>
          <button disabled={loading || !creator.unlocked} onClick={save}>保存</button>
        </div>
        {message ? <strong>{message}</strong> : null}
        {errors.length ? <pre className="creator-errors">{errors.join('\n')}</pre> : null}
      </section>
    </main>
  )
}

export default CreatorMode
