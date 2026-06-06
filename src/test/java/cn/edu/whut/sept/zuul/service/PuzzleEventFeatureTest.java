package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.event.EventEngine;
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
    void mirrorPasswordRequiresLibraryClueThenRewardsByEvent() {
        RoomService service = service();
        service.initGame();
        act(service, "MOVE", "north", null);
        act(service, "INSPECT", "memory_library", null);
        act(service, "MOVE", "south", null);
        GameSnapshot mirror = act(service, "MOVE", "east", null);

        assertNotNull(mirror.puzzle());
        assertEquals("mirror_number_door", mirror.puzzle().id());

        GameSnapshot solved = act(service, "ANSWER", "mirror_number_door", "21");

        assertNull(solved.puzzle());
        assertTrue(solved.flags().get("mirror_door_open"));
        assertTrue(solved.flags().get("event_completed.mirror_room_event"));
        assertTrue(solved.inventoryItems().contains("mirror_shard"));
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
    void sealGateOpensAfterTwoShardEventsAndRewardsBadge() {
        RoomService service = service();
        service.initGame();
        act(service, "MOVE", "north", null);
        act(service, "MOVE", "west", null);
        act(service, "INSPECT", "broken_shelf", null);
        act(service, "MOVE", "east", null);
        act(service, "MOVE", "south", null);
        act(service, "MOVE", "south", null);
        act(service, "ANSWER", "rune_direction_sequence", "south north east west");
        act(service, "MOVE", "south", null);
        act(service, "INSPECT", "order_altar", null);
        act(service, "MOVE", "north", null);
        act(service, "MOVE", "north", null);
        act(service, "MOVE", "west", null);
        GameSnapshot gate = act(service, "MOVE", "south", null);

        assertEquals("triple_seal_gate", gate.currentRoomId());
        assertNotNull(gate.puzzle());

        GameSnapshot opened = act(service, "ANSWER", "triple_seal_gate", "open");

        assertTrue(opened.flags().get("triple_seal_open"));
        assertTrue(opened.inventoryItems().contains("nameless_badge"));
        assertNull(opened.puzzle());
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
        return new RoomService(playerService, worldState, new PuzzleEngine(new PuzzleConfig().puzzleRegistry()), new EventEngine());
    }

    private GameSnapshot act(RoomService service, String actionType, String target, String value) {
        return service.perform(new GameActionRequest(actionType, target, value));
    }
}
