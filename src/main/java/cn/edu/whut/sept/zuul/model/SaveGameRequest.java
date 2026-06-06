package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "保存游戏请求。")
public record SaveGameRequest(
        @Schema(description = "存档位 ID，仅允许前后端约定的安全字符串。", example = "slot_1", requiredMode = Schema.RequiredMode.REQUIRED)
        String saveId
) {
}
