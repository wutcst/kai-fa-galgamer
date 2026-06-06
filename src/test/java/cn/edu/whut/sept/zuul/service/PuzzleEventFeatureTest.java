package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.event.EventEngine;
import cn.edu.whut.sept.zuul.minigame.MiniGameRewardResolver;
import cn.edu.whut.sept.zuul.minigame.MiniGameService;
import cn.edu.whut.sept.zuul.model.GameActionRequest;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.puzzle.PuzzleConfig;
import cn.edu.whut.sept.zuul.puzzle.PuzzleEngine;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleEventFeatureTest {

    @Test
    void mirrorPasswordStartsPointMiniGameInsteadOfImmediateReward() {
        RoomService service = service();
        service.initGame();
        act(service, "MOVE", "north", null);
        act(service, "INSPECT", "memory_library", null);
        act(service, "MOVE", "south", null);
        act(service, "MOVE", "east", null);

        GameSnapshot solved = act(service, "ANSWER", "mirror_number_door", "21");

        assertNull(solved.puzzle());
        assertNotNull(solved.miniGame());
        assertEquals("point_game", solved.miniGame().gameId());
        assertEquals("MINIGAME", solved.gamePhase().name());
    }

    @Test
    void runePuzzleControlsSouthExitAndBlocksRepeatDamage() {
        RoomService service = service();
        service.initGame();
        GameSnapshot rune = act(service, "MOVE", "south", null);
        assertNotNull(rune.puzzle());

        GameSnapshot blocked = act(service, "MOVE", "south", null);
        assertEquals("rune_floor", blocked.currentRoomId());
        assertEquals("符文地板尚未稳定，请先完成方向序列谜题。", blocked.errorMessage());

        GameSnapshot solved = act(service, "ANSWER", "rune_direction_sequence", "south north east west");
        assertTrue(solved.flags().get("altar_gate_open"));
        GameSnapshot repeated = act(service, "ANSWER", "rune_direction_sequence", "north east south west");
        assertEquals(100, repeated.playerHp());

        GameSnapshot altar = act(service, "MOVE", "south", null);
        assertEquals("order_altar", altar.currentRoomId());
    }

    @Test
    void altarInspectStartsDiceMiniGameAndBlocksMovementUntilOutcomeConfirmed() {
        RoomService service = service();
        service.initGame();
        act(service, "MOVE", "south", null);
        act(service, "ANSWER", "rune_direction_sequence", "south north east west");
        act(service, "MOVE", "south", null);

        GameSnapshot miniGame = act(service, "INSPECT", "order_altar", null);
        assertNotNull(miniGame.miniGame());
        assertEquals("dice_check", miniGame.miniGame().gameId());

        GameSnapshot blocked = act(service, "MOVE", "south", null);
        assertEquals("请先完成或确认小游戏结果。", blocked.errorMessage());
    }

    @Test
    void ackMiniGameResultReturnsToExploringAndCompletesEvent() {
        RoomService service = service();
        service.initGame();
        act(service, "MOVE", "south", null);
        act(service, "ANSWER", "rune_direction_sequence", "south north east west");
        act(service, "MOVE", "south", null);
        GameSnapshot miniGame = act(service, "INSPECT", "order_altar", null);

        GameSnapshot outcome = act(service, "MINI_GAME_INPUT", miniGame.miniGame().sessionId(), "roll");
        assertNotNull(outcome.miniGameOutcome());

        GameSnapshot acknowledged = act(service, "ACK_MINI_GAME_RESULT", outcome.miniGameOutcome().sessionId(), null);

        assertNull(acknowledged.miniGame());
        assertNull(acknowledged.miniGameOutcome());
        assertEquals("EXPLORING", acknowledged.gamePhase().name());
        assertTrue(acknowledged.flags().get("event_completed.order_altar_event"));
    }

    @Test
    void answerRequestInRoomWithoutPuzzleIsRejected() {
        RoomService service = service();
        service.initGame();

        GameSnapshot result = act(service, "ANSWER", "mirror_number_door", "21");

        assertEquals("当前房间没有这个谜题。", result.errorMessage());
    }

    private RoomService service() {
        ItemService itemService = new ItemService();
        PlayerService playerService = new PlayerService(itemService);
        WorldState worldState = new WorldState();
        return new RoomService(
                playerService,
                worldState,
                new PuzzleEngine(new PuzzleConfig().puzzleRegistry()),
                new EventEngine(),
                new MiniGameService(new MiniGameRewardResolver()),
                new BattleService(),
                new EndingService()
        );
    }

    private GameSnapshot act(RoomService service, String actionType, String target, String value) {
        return service.perform(new GameActionRequest(actionType, target, value));
    }
}
