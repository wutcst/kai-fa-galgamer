package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "游戏阶段。")
public enum GamePhase {
    EXPLORING,
    MINIGAME,
    BATTLE,
    ENDING
}
