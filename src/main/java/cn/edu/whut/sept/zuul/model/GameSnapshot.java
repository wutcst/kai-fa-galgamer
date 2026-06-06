package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "统一游戏状态快照。后端所有核心游戏接口均返回该对象，前端仅基于快照渲染界面。")
public record GameSnapshot(
        @Schema(description = "当前房间 ID。", example = "start_room")
        String currentRoomId,

        @Schema(description = "当前房间标题。", example = "命运裂隙")
        String roomTitle,

        @Schema(description = "当前房间描述。", example = "你在一道微光裂隙前醒来，远处传来王座钟声。")
        String roomDescription,

        @Schema(description = "玩家当前生命值。", example = "100", minimum = "0")
        int playerHp,

        @ArraySchema(schema = @Schema(description = "背包物品 ID。", example = "blank_dice"))
        List<String> inventoryItems,

        @Schema(description = "当前游戏阶段。", example = "EXPLORING")
        GamePhase gamePhase,

        @Schema(description = "当前房间场景资源 key，前端通过 asset-manifest.json 映射到图片路径。", example = "scene.fate_hall")
        String roomAssetKey,

        @ArraySchema(schema = @Schema(implementation = GameActionOption.class))
        List<GameActionOption> availableActions,

        @ArraySchema(schema = @Schema(description = "最近探索日志。", example = "你向北移动，抵达：记忆图书馆"))
        List<String> logs,

        @Schema(description = "普通系统消息，用于展示操作结果或剧情提示。", example = "新游戏已初始化。")
        String systemMessage,

        @Schema(description = "错误提示。没有错误时为 null。", example = "当前阶段不能执行该动作。", nullable = true)
        String errorMessage
) {
}
