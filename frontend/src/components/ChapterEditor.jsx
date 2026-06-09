function ChapterEditor({ value, onChange }) {
  return (
    <textarea
      className="chapter-editor"
      value={value}
      spellCheck="false"
      onChange={(event) => onChange(event.target.value)}
    />
  )
}

export default ChapterEditor
