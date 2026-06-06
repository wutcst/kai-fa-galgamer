function MainMenu({ snapshot, assets, loading, onAction }) {
  const menu = snapshot.menu ?? {}
  const actions = menu.actions ?? snapshot.availableActions ?? []
  const bg = assets['cover.key_visual'] ?? assets['cover.title_art'] ?? assets.fallback

  return (
    <main className="main-menu" style={{ backgroundImage: `url("${bg}")` }}>
      <section className="main-menu-panel">
        <p>World of Zuul</p>
        <h1>Uncertain Fate</h1>
        <div className="main-menu-actions">
          {actions.map((action) => (
            <button
              key={`${action.actionType}-${action.target}`}
              type="button"
              disabled={loading || (action.actionType === 'CREATOR_LIST' && !menu.creatorModeUnlocked)}
              onClick={() => onAction(action)}
            >
              {action.label}
            </button>
          ))}
        </div>
        <footer>
          <span>{menu.hasAnySave ? '检测到可继续存档' : '暂无存档'}</span>
          <span>{menu.creatorModeUnlocked ? 'Creator Mode 已解锁' : 'Creator Mode 需真结局解锁'}</span>
        </footer>
      </section>
    </main>
  )
}

export default MainMenu
