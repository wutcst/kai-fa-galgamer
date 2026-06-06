package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "游戏阶段，用于前端决定当前应渲染的主要交互面板。")
public enum GamePhase {
    @Schema(description = "主菜单或初始状态")
    MAIN_MENU,

    @Schema(description = "房间探索阶段")
    EXPLORING,

    @Schema(description = "谜题输入阶段")
    PUZZLE,

    @Schema(description = "小游戏阶段")
    MINI_GAME,

    @Schema(description = "战斗阶段")
    BATTLE,

    @Schema(description = "结局选择阶段")
    ENDING_CHOICE,

    @Schema(description = "游戏结束")
    GAME_OVER
}
