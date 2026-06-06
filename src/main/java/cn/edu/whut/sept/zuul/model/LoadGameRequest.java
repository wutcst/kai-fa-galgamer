package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "读取游戏请求。")
public record LoadGameRequest(
        @Schema(description = "存档位 ID。", example = "slot_1", requiredMode = Schema.RequiredMode.REQUIRED)
        String saveId
) {
}
