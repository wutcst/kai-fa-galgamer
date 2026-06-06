package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "当前快照下前端可以展示给玩家的动作按钮或输入入口。")
public record GameActionOption(
        @Schema(description = "动作类型，前后端共同约定。", example = "MOVE")
        String actionType,

        @Schema(description = "前端展示文本。", example = "前往记忆图书馆")
        String label,

        @Schema(description = "动作目标，例如方向、房间 ID、谜题 ID 或存档位。", example = "memory_library")
        String target,

        @Schema(description = "是否需要玩家额外输入。", example = "false")
        boolean requiresInput
) {
}
