# Issue 4 地图、房间与探索动作系统

## 后端行为

- `RoomService` 管理当前探索会话、房间地图、移动、查看、调查和最近日志。
- 当前内置 13 个房间，覆盖记忆、镜像、秩序、灵魂、炼金、封印门和王座区域。
- `GET /api/game/init` 重置到 `fate_hall`。
- `POST /api/game/action` 当前支持：
  - `MOVE`：`target` 为 `north`、`south`、`east`、`west`、`up`、`down`。
  - `LOOK`：查看当前房间描述。
  - `INSPECT`：调查当前房间线索。
- 不存在的方向不会出现在 `availableActions`；如果手动调用非法方向，后端返回原房间快照并设置 `errorMessage`。

## 房间列表

| roomId | 说明 |
| --- | --- |
| `fate_hall` | 起点，连接四个核心区域 |
| `memory_library` | 记忆区域入口 |
| `broken_shelf` | 图书馆隐藏线索 |
| `mirror_corridor` | 镜像区域入口 |
| `broken_mirror_room` | 镜像支线房间 |
| `rune_floor` | 秩序方向谜题房 |
| `order_altar` | 秩序祭坛 |
| `soul_garden` | 灵魂区域入口 |
| `material_storage` | 材料仓库 |
| `whisper_pool` | 低语水池 |
| `alchemy_workshop` | 炼金工坊 |
| `triple_seal_gate` | Boss 前封印门 |
| `zuul_throne` | 祖尔王座 |

## 前端行为

- `frontend/src/api.js` 封装 `/api/game/init`、`/api/game/action`、`/api/game/save` 和资源 manifest。
- `GameScreen` 展示场景图、房间标题、描述、HP、系统消息、错误提示和探索日志。
- `DirectionButtons` 只根据后端下发的 `availableActions` 渲染可用方向按钮。
- `SAVE` 按钮调用 `/api/game/save`；`LOOK`、`INSPECT`、`MOVE` 调用 `/api/game/action`。

## 美术资源

资源位于 `frontend/public/assets`：

- `assets/scenes/*.png`：探索场景图。
- `assets/maps/world-overview.png`：地图预留资源。
- `assets/asset-manifest.json`：`roomAssetKey` 到图片路径的映射。

缺失的独立房间图先映射到同风格近似资源或 `fallback`，保证页面不空白。

## 手动验收

1. 启动后端和前端，打开网页。
2. 初始房间应显示命运大厅场景、标题、描述、HP 和可用方向。
3. 点击方向按钮后，房间标题、描述、场景图和可用方向应随后端快照变化。
4. 点击“查看四周”“调查线索”，探索日志应新增对应反馈。
5. 手动调用非法方向时，快照应保持原房间并返回 `errorMessage`。
6. 页面上不应显示当前房间没有的方向按钮。
