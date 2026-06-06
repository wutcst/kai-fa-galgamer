package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "玩家动作请求。前端点击按钮或提交输入时发送该对象。")
public record GameActionRequest(
        @Schema(description = "动作类型，例如 MOVE、LOOK、ANSWER、ATTACK、CHOOSE。", example = "MOVE", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionType,

        @Schema(description = "动作目标，例如方向、房间 ID、谜题 ID、结局选项 ID。", example = "north")
        String target,

        @Schema(description = "玩家输入值，例如谜题答案或小游戏输入。", example = "open the gate")
        String value
) {
}
