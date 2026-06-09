package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "统一游戏状态快照。后端所有核心游戏接口均返回该对象，前端仅基于快照渲染界面。")
public record GameSnapshot(
        @Schema(description = "当前房间 ID。", example = "fate_hall")
        String currentRoomId,

        @Schema(description = "当前房间标题。", example = "命运大厅")
        String roomTitle,

        @Schema(description = "当前房间描述。", example = "破碎石阶延伸向四条岔路。")
        String roomDescription,

        @Schema(description = "玩家当前生命值。", example = "100", minimum = "0")
        int playerHp,

        @ArraySchema(schema = @Schema(description = "背包物品 ID。", example = "blank_dice"))
        List<String> inventoryItems,

        @Schema(description = "当前游戏阶段。", example = "EXPLORING")
        GamePhase gamePhase,

        @Schema(description = "当前房间场景资源 key。", example = "scene.fate_hall")
        String roomAssetKey,

        @ArraySchema(schema = @Schema(implementation = GameActionOption.class))
        List<GameActionOption> availableActions,

        @Schema(description = "当前房间未解决谜题。没有谜题或谜题已解决时为 null。", nullable = true)
        PuzzleView puzzle,

        @Schema(description = "当前世界 Flag。")
        Map<String, Boolean> flags,

        @Schema(description = "当前进行中的小游戏。没有小游戏时为 null。", nullable = true)
        MiniGameView miniGame,

        @Schema(description = "待确认的小游戏结果。没有结果时为 null。", nullable = true)
        MiniGameOutcome miniGameOutcome,

        @Schema(description = "主菜单视图。非主菜单阶段为 null。", nullable = true)
        MenuView menu,

        @Schema(description = "存档视图。", nullable = true)
        SaveView save,

        @Schema(description = "当前 Boss 战状态。没有战斗时为 null。", nullable = true)
        BattleView battle,

        @ArraySchema(schema = @Schema(implementation = ChoiceView.class))
        List<ChoiceView> choices,

        @Schema(description = "已选择的结局。没有结局时为 null。", nullable = true)
        EndingView ending,

        @Schema(description = "创作者模式视图。非创作者模式为 null。", nullable = true)
        CreatorView creator,

        @Schema(description = "探索地图视图。")
        WorldMapView map,

        @ArraySchema(schema = @Schema(description = "最近探索日志。", example = "你向北移动，抵达：记忆图书馆"))
        List<String> logs,

        @Schema(description = "普通系统消息，用于展示操作结果或剧情提示。")
        String systemMessage,

        @Schema(description = "错误提示。没有错误时为 null。", nullable = true)
        String errorMessage
) {
    public GameSnapshot withMessage(String nextSystemMessage, String nextErrorMessage) {
        return new GameSnapshot(
                currentRoomId,
                roomTitle,
                roomDescription,
                playerHp,
                inventoryItems,
                gamePhase,
                roomAssetKey,
                availableActions,
                puzzle,
                flags,
                miniGame,
                miniGameOutcome,
                menu,
                save,
                battle,
                choices,
                ending,
                creator,
                map,
                logs,
                nextSystemMessage,
                nextErrorMessage
        );
    }

    public record PuzzleView(
            String id,
            String prompt,
            String kind,
            List<String> options,
            boolean freeText,
            String submitAction
    ) {
    }

    public record MiniGameView(
            String sessionId,
            String gameId,
            String eventId,
            String phase,
            List<String> actions,
            Map<String, Object> state
    ) {
    }

    public record MiniGameOutcome(
            String sessionId,
            String gameId,
            String eventId,
            String resultType,
            int score,
            String message,
            List<String> rewardItems,
            Map<String, Boolean> flags,
            Map<String, Object> details
    ) {
    }

    public record MenuView(
            boolean hasAnySave,
            boolean creatorModeUnlocked,
            List<GameActionOption> actions
    ) {
    }

    public record SaveView(
            List<SaveSlotView> slots
    ) {
    }

    public record SaveSlotView(
            String saveId,
            boolean exists,
            String currentRoomId,
            String roomTitle,
            String gamePhase,
            long savedAt
    ) {
    }

    public record BattleView(
            String enemyId,
            String enemyName,
            int enemyHp,
            int enemyMaxHp,
            int phase,
            int turn,
            int playerGuard,
            int rolledBonus,
            List<String> traits,
            String lastAction,
            String message,
            String assetKey
    ) {
    }

    public record ChoiceView(
            int number,
            String id,
            String label,
            String description,
            boolean unlocked
    ) {
    }

    public record EndingView(
            String id,
            String title,
            String description,
            String assetKey
    ) {
    }

    public record CreatorView(
            boolean unlocked,
            List<CreatorChapterSummary> chapters,
            List<String> validationErrors,
            List<GameActionOption> actions
    ) {
    }

    public record CreatorChapterSummary(
            String chapterId,
            String title,
            String author,
            int rooms
    ) {
    }

    public record WorldMapView(
            List<MapRoomView> rooms,
            List<MapExitView> exits
    ) {
    }

    public record MapRoomView(
            String id,
            String title,
            int x,
            int y,
            boolean explored,
            boolean current,
            boolean adjacentToCurrent
    ) {
    }

    public record MapExitView(
            String from,
            String to,
            String direction,
            boolean locked
    ) {
    }
}
