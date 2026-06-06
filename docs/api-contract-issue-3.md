# Issue 3 API 数据契约

本文件锁定阶段 2 的前后端 JSON 契约。Swagger UI 启动后可访问：

- `/swagger-ui/index.html`
- `/v3/api-docs`

## GameSnapshot

所有核心游戏接口统一返回 `GameSnapshot`。

```json
{
  "currentRoomId": "start_room",
  "roomTitle": "命运裂隙",
  "roomDescription": "你在一道微光裂隙前醒来，远处传来王座钟声。",
  "playerHp": 100,
  "inventoryItems": [],
  "gamePhase": "EXPLORING",
  "availableActions": [
    {
      "actionType": "MOVE",
      "label": "前往记忆图书馆",
      "target": "memory_library",
      "requiresInput": false
    },
    {
      "actionType": "LOOK",
      "label": "查看四周",
      "target": "start_room",
      "requiresInput": false
    }
  ],
  "systemMessage": "新游戏已初始化。",
  "errorMessage": null
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `currentRoomId` | string | 当前房间 ID，使用小写蛇形命名 |
| `roomTitle` | string | 当前房间标题 |
| `roomDescription` | string | 当前房间描述 |
| `playerHp` | number | 玩家生命值 |
| `inventoryItems` | string[] | 背包物品 ID 列表 |
| `gamePhase` | string | 当前阶段，如 `EXPLORING`、`PUZZLE`、`MINI_GAME`、`BATTLE` |
| `availableActions` | object[] | 前端可渲染的动作列表 |
| `systemMessage` | string | 普通系统消息 |
| `errorMessage` | string/null | 错误提示，无错误时为 `null` |

## GET /api/game/init

初始化新游戏，返回初始快照。

## POST /api/game/action

请求示例：

```json
{
  "actionType": "MOVE",
  "target": "north",
  "value": null
}
```

响应：`GameSnapshot`。

## POST /api/game/save

请求示例：

```json
{
  "saveId": "slot_1"
}
```

响应：`GameSnapshot`。

## POST /api/game/load

请求示例：

```json
{
  "saveId": "slot_1"
}
```

响应：`GameSnapshot`。
