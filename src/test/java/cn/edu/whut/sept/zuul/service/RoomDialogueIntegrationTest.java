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

class RoomDialogueIntegrationTest {

    @Test
    void activeDialogueSuspendsMovementUntilExit() {
        RoomService service = service();
        service.initGame();

        GameSnapshot started = act(service, "INSPECT", "fate_hall", null);

        assertNotNull(started.dialogue());
        assertEquals("SPEECH", started.dialogue().nodeType());

        GameSnapshot blocked = act(service, "MOVE", "north", null);
        assertEquals("请先完成当前 ADV 对话。", blocked.errorMessage());
        assertEquals("fate_hall", blocked.currentRoomId());

        GameSnapshot choice = act(service, "ADV_NEXT", started.dialogue().currentNodeId(), null);
        assertEquals("CHOICE", choice.dialogue().nodeType());

        GameSnapshot closingSpeech = act(service, "ADV_CHOOSE", "traveler", null);
        assertEquals("node_choice_a_speech", closingSpeech.dialogue().currentNodeId());

        GameSnapshot restored = act(service, "ADV_NEXT", closingSpeech.dialogue().currentNodeId(), null);
        assertNull(restored.dialogue());
        assertEquals("EXPLORING", restored.gamePhase().name());
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
