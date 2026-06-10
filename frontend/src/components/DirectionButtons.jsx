const directionLabels = {
  north: '北',
  south: '南',
  east: '东',
  west: '西',
  up: '上',
  down: '下',
}

const directionOrder = ['north', 'west', 'east', 'south', 'up', 'down']

function DirectionButtons({ actions, assets = {}, onAction, disabled }) {
  const moveActions = directionOrder
    .map((direction) => actions.find((action) => action.actionType === 'MOVE' && action.target === direction))
    .filter(Boolean)

  return (
    <nav className="direction-buttons" aria-label="方向移动">
      <div className="direction-grid">
        {moveActions.map((action) => (
          <button
            key={action.target}
            className={`direction-button direction-${action.target}`}
            type="button"
            disabled={disabled}
            onClick={() => onAction({ actionType: 'MOVE', target: action.target, value: null })}
          >
            {assets[`ui.direction.${action.target}`] ? <img src={assets[`ui.direction.${action.target}`]} alt="" /> : null}
            <span>{directionLabels[action.target] ?? action.label}</span>
            <small>{action.target}</small>
          </button>
        ))}
      </div>
    </nav>
  )
}

export default DirectionButtons
