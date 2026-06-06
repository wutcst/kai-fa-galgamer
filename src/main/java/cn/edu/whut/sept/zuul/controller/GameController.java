package cn.edu.whut.sept.zuul.controller;

import cn.edu.whut.sept.zuul.model.GameActionRequest;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.model.LoadGameRequest;
import cn.edu.whut.sept.zuul.model.SaveGameRequest;
import cn.edu.whut.sept.zuul.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "游戏接口", description = "游戏状态快照、地图探索动作、存档与读档接口。")
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final RoomService roomService;

    public GameController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(
            summary = "初始化游戏",
            description = "创建新游戏或重置当前探索会话，并返回统一游戏状态快照 GameSnapshot。",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "初始化成功",
                    content = @Content(
                            schema = @Schema(implementation = GameSnapshot.class),
                            examples = @ExampleObject(name = "初始快照", value = """
                                    {
                                      "currentRoomId": "fate_hall",
                                      "roomTitle": "命运大厅",
                                      "roomDescription": "破碎石阶延伸向四条岔路，地面的六面骰纹路像仍在缓慢转动。",
                                      "playerHp": 100,
                                      "inventoryItems": [],
                                      "gamePhase": "EXPLORING",
                                      "roomAssetKey": "scene.fate_hall",
                                      "availableActions": [
                                        {
                                          "actionType": "MOVE",
                                          "label": "前往北",
                                          "target": "north",
                                          "requiresInput": false
                                        }
                                      ],
                                      "logs": ["新游戏已初始化。你在命运大厅醒来。"],
                                      "systemMessage": "新游戏已初始化。你在命运大厅醒来。",
                                      "errorMessage": null
                                    }
                                    """)
                    )
            )
    )
    @GetMapping("/init")
    public GameSnapshot initGame() {
        return roomService.initGame();
    }

    @Operation(
            summary = "执行玩家动作",
            description = "接收前端发送的动作请求。后续真实业务接入后，后端根据动作推进游戏并返回最新 GameSnapshot。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "玩家动作请求",
                    content = @Content(
                            schema = @Schema(implementation = GameActionRequest.class),
                            examples = @ExampleObject(name = "移动动作", value = """
                                    {
                                      "actionType": "MOVE",
                                      "target": "north",
                                      "value": null
                                    }
                                    """)
                    )
            ),
            responses = @ApiResponse(responseCode = "200", description = "动作处理成功", content = @Content(schema = @Schema(implementation = GameSnapshot.class)))
    )
    @PostMapping("/action")
    public GameSnapshot performAction(@RequestBody GameActionRequest request) {
        return roomService.perform(request);
    }

    @Operation(
            summary = "保存游戏",
            description = "保存当前游戏状态。当前阶段返回 Mock 快照，用于锁定前后端请求和响应格式。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SaveGameRequest.class),
                            examples = @ExampleObject(name = "保存到 slot_1", value = """
                                    {
                                      "saveId": "slot_1"
                                    }
                                    """)
                    )
            ),
            responses = @ApiResponse(responseCode = "200", description = "保存成功", content = @Content(schema = @Schema(implementation = GameSnapshot.class)))
    )
    @PostMapping("/save")
    public GameSnapshot saveGame(@RequestBody SaveGameRequest request) {
        String saveId = request == null ? "slot_1" : request.saveId();
        return roomService.save(saveId);
    }

    @Operation(
            summary = "读取游戏",
            description = "读取指定存档位并返回恢复后的 GameSnapshot。当前阶段返回 Mock 快照。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoadGameRequest.class),
                            examples = @ExampleObject(name = "读取 slot_1", value = """
                                    {
                                      "saveId": "slot_1"
                                    }
                                    """)
                    )
            ),
            responses = @ApiResponse(responseCode = "200", description = "读取成功", content = @Content(schema = @Schema(implementation = GameSnapshot.class)))
    )
    @PostMapping("/load")
    public GameSnapshot loadGame(@RequestBody LoadGameRequest request) {
        String saveId = request == null ? "slot_1" : request.saveId();
        return roomService.load(saveId);
    }
}
