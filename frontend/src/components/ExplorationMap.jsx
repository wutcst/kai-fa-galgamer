import { useMemo, useState } from 'react'

const EMPTY_ROOMS = []
const EMPTY_EXITS = []

function ExplorationMap({ map }) {
  const [expanded, setExpanded] = useState(false)
  const rooms = map?.rooms ?? EMPTY_ROOMS
  const exits = map?.exits ?? EMPTY_EXITS

  const roomLookup = useMemo(
    () => new Map(rooms.map((room) => [room.id, room])),
    [rooms],
  )

  const miniRoomIds = useMemo(() => {
    const ids = new Set()
    rooms
      .filter((room) => room.explored || room.current || room.adjacentToCurrent)
      .forEach((room) => ids.add(room.id))
    exits.forEach((exit) => {
      const from = roomLookup.get(exit.from)
      const to = roomLookup.get(exit.to)
      if (from?.current || to?.current) {
        ids.add(exit.from)
        ids.add(exit.to)
      }
    })
    return ids
  }, [exits, roomLookup, rooms])

  if (!rooms.length) {
    return null
  }

  return (
    <>
      <button className="exploration-map-toggle" type="button" onClick={() => setExpanded(true)}>
        <span className="map-chip">探索地图</span>
        <MapCanvas rooms={rooms} exits={exits} roomLookup={roomLookup} visibleRoomIds={miniRoomIds} compact />
      </button>

      {expanded ? (
        <div className="map-modal-backdrop" role="presentation" onClick={() => setExpanded(false)}>
          <section className="map-modal" role="dialog" aria-modal="true" aria-label="完整探索地图" onClick={(event) => event.stopPropagation()}>
            <header className="map-modal-header">
              <div>
                <span>World Map</span>
                <h2>探索地图</h2>
              </div>
              <button type="button" onClick={() => setExpanded(false)} aria-label="关闭地图">
                X
              </button>
            </header>
            <MapCanvas rooms={rooms} exits={exits} roomLookup={roomLookup} />
          </section>
        </div>
      ) : null}
    </>
  )
}

function MapCanvas({ rooms, exits, roomLookup, visibleRoomIds, compact = false }) {
  const visibleRooms = visibleRoomIds ? rooms.filter((room) => visibleRoomIds.has(room.id)) : rooms
  const visibleIds = new Set(visibleRooms.map((room) => room.id))
  const visibleExits = uniqueExits(
    exits.filter((exit) => {
      if (!visibleIds.has(exit.from) || !visibleIds.has(exit.to)) {
        return false
      }
      if (!compact) {
        return true
      }
      const from = roomLookup.get(exit.from)
      const to = roomLookup.get(exit.to)
      return from?.current || to?.current || (from?.explored && to?.explored)
    }),
  )

  return (
    <div className={compact ? 'map-canvas map-canvas-compact' : 'map-canvas'}>
      <svg className="map-lines" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
        {visibleExits.map((exit) => {
          const from = roomLookup.get(exit.from)
          const to = roomLookup.get(exit.to)
          if (!from || !to) {
            return null
          }
          return (
            <line
              key={`${exit.from}-${exit.to}`}
              className={exit.locked ? 'map-line map-line-locked' : 'map-line'}
              x1={from.x}
              y1={from.y}
              x2={to.x}
              y2={to.y}
            />
          )
        })}
      </svg>

      {visibleRooms.map((room) => (
        <div
          key={room.id}
          className={[
            'map-node',
            room.explored ? 'is-explored' : 'is-hidden',
            room.current ? 'is-current' : '',
            room.adjacentToCurrent && !room.current ? 'is-adjacent' : '',
          ].filter(Boolean).join(' ')}
          style={{ left: `${room.x}%`, top: `${room.y}%` }}
          title={room.title ?? '未探索'}
        >
          <span className="map-node-dot" />
          {room.title ? <span className="map-node-title">{room.title}</span> : <span className="map-node-fog" />}
        </div>
      ))}
    </div>
  )
}

function uniqueExits(exits) {
  const edges = new Map()
  exits.forEach((exit) => {
    const key = [exit.from, exit.to].sort().join('--')
    const current = edges.get(key)
    edges.set(key, current ? { ...current, locked: current.locked || exit.locked } : exit)
  })
  return Array.from(edges.values())
}

export default ExplorationMap
