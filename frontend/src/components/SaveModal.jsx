function SaveModal({ snapshot, loading, onAction, onClose }) {
  const slots = snapshot.save?.slots ?? []

  return (
    <div className="inventory-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="save-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <header className="inventory-header">
          <div>
            <p>Archive</p>
            <h2>存档</h2>
          </div>
          <button className="inventory-close" type="button" aria-label="关闭存档" onClick={onClose}>×</button>
        </header>
        <div className="save-slot-grid">
          {['slot_1', 'slot_2', 'slot_3'].map((slotId) => {
            const slot = slots.find((item) => item.saveId === slotId)
            return (
              <article className="save-slot" key={slotId}>
                <h3>{slotId}</h3>
                <p>{slot ? `${slot.currentRoomId} · ${slot.gamePhase}` : '空存档位'}</p>
                <div>
                  <button disabled={loading} onClick={() => onAction({ actionType: 'SAVE', target: slotId })}>保存</button>
                  <button disabled={loading || !slot} onClick={() => onAction({ actionType: 'LOAD', target: slotId })}>读取</button>
                </div>
              </article>
            )
          })}
        </div>
      </section>
    </div>
  )
}

export default SaveModal
