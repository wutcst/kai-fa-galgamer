package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "玩家动作请求。前端点击按钮或提交输入时发送该对象。")
public record GameActionRequest(
        @Schema(description = "动作类型，例如 MOVE、LOOK、ANSWER、MINI_GAME_INPUT。", example = "MOVE", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionType,

        @Schema(description = "动作目标，例如方向、谜题 ID、小游戏 Session ID。", example = "north")
        String target,

        @Schema(description = "玩家输入值，例如谜题答案、小游戏动作。", example = "roll")
        String value,

        @Schema(description = "结构化动作数据，例如连连看坐标。")
        Map<String, Object> payload
) {
    public GameActionRequest(String actionType, String target, String value) {
        this(actionType, target, value, Map.of());
    }

    public GameActionRequest {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
