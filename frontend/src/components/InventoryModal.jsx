import { useEffect, useMemo, useState } from 'react'

const ITEM_DETAILS = {
  blank_dice: {
    name: '空白骰子',
    description: '一枚没有点数的骰子，能让既定命运短暂停顿。',
    assetKey: 'item.blank_dice',
    keyItem: true,
  },
  savebreaker_key: {
    name: '断档之钥',
    description: '钥齿像碎裂存档的断面，能撬开循环留下的锁。',
    assetKey: 'item.savebreaker_key',
    keyItem: true,
  },
  nameless_badge: {
    name: '无名徽章',
    description: '徽面没有姓名，只留下拒绝继承王座的誓痕。',
    assetKey: 'item.nameless_badge',
    keyItem: true,
  },
  pure_seed: {
    name: '纯净种子',
    description: '仍有青白魂光在种壳里呼吸，未被轮回污染。',
    assetKey: 'item.pure_seed',
    keyItem: true,
  },
  throne_fragment: {
    name: '王座碎片',
    description: '黑金王座剥落的一角，残留着支配命运的余温。',
    assetKey: 'item.throne_fragment',
    keyItem: true,
  },
  soul_bell: {
    name: '灵魂之铃',
    description: '由形体、灵魂与束缚重铸的铃，声响能指向循环裂缝。',
    assetKey: 'item.soul_bell',
    keyItem: false,
  },
  broken_bell: {
    name: '破损铃铛',
    description: '灵魂之铃遗失的形体，边缘还带着旧裂纹。',
    assetKey: 'item.broken_bell',
    keyItem: false,
  },
  soul_flower: {
    name: '灵魂花',
    description: '花瓣中漂浮着低语，是修复灵魂之铃的魂光。',
    assetKey: 'item.soul_flower',
    keyItem: false,
  },
  silver_thread: {
    name: '银线',
    description: '能束住魂光的炼金丝线，细得像一道月痕。',
    assetKey: 'item.silver_thread',
    keyItem: false,
  },
}

function InventoryModal({ inventoryItems = [], assets, loading, onAction, onClose }) {
  const items = useMemo(
    () =>
      inventoryItems.map((itemId) => ({
        id: itemId,
        ...(ITEM_DETAILS[itemId] ?? {
          name: itemId.replaceAll('_', ' '),
          description: '未记录说明的物品。',
          assetKey: `item.${itemId}`,
          keyItem: false,
        }),
      })),
    [inventoryItems],
  )
  const [selectedId, setSelectedId] = useState(items[0]?.id ?? '')
  const selected = items.find((item) => item.id === selectedId) ?? items[0] ?? null

  useEffect(() => {
    if (!selected || !items.some((item) => item.id === selected.id)) {
      setSelectedId(items[0]?.id ?? '')
    }
  }, [items, selected])

  const handleCraft = () => {
    onAction({
      actionType: 'CRAFT',
      target: 'soul_bell',
      value: null,
    })
  }

  return (
    <div className="inventory-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="inventory-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="inventory-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="inventory-header">
          <div>
            <p>Inventory</p>
            <h2 id="inventory-title">背包</h2>
          </div>
          <button className="inventory-close" type="button" aria-label="关闭背包" onClick={onClose}>
            ×
          </button>
        </header>

        <div className="inventory-body">
          <div className="inventory-grid" aria-label="当前物品列表">
            {items.length === 0 ? (
              <div className="inventory-empty">背包仍是空的。</div>
            ) : (
              items.map((item) => (
                <button
                  key={item.id}
                  className={selected?.id === item.id ? 'inventory-slot selected' : 'inventory-slot'}
                  type="button"
                  onClick={() => setSelectedId(item.id)}
                >
                  <img src={assets[item.assetKey] ?? assets.fallback} alt="" />
                  <span>{item.name}</span>
                </button>
              ))
            )}
          </div>

          <article className="inventory-detail">
            {selected ? (
              <>
                <img src={assets[selected.assetKey] ?? assets.fallback} alt="" />
                <p className="inventory-code">{selected.id}</p>
                <h3>{selected.name}</h3>
                <p>{selected.description}</p>
                <span className={selected.keyItem ? 'item-badge key' : 'item-badge'}>{selected.keyItem ? '反循环关键道具' : '普通物品'}</span>
              </>
            ) : (
              <p className="inventory-empty-detail">调查房间后，获得的物品会出现在这里。</p>
            )}
          </article>
        </div>

        <footer className="inventory-footer">
          <p>Alchemy</p>
          <button className="craft-button" type="button" disabled={loading} onClick={handleCraft}>
            合成灵魂之铃
          </button>
        </footer>
      </section>
    </div>
  )
}

export default InventoryModal
