package cn.edu.whut.sept.zuul.minigame;

import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniGameServiceTest {

    @Test
    void diceRollFinishesSessionAndCreatesOutcome() {
        MiniGameService service = service(5, 5);

        service.start("dice_check", "order_altar_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "roll", Map.of());

        assertFalse(service.hasActiveMiniGame());
        assertNotNull(service.outcomeView());
        assertEquals("GREAT_SUCCESS", service.outcomeView().resultType());
    }

    @Test
    void pointGameBustsImmediatelyOnDraw() {
        MiniGameService service = service(9, 9, 9);

        service.start("point_game", "mirror_room_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());

        assertNotNull(service.outcomeView());
        assertEquals("FAILURE", service.outcomeView().resultType());
    }

    @Test
    void linkMatchClearsMatchingTilesThroughBackendPathCheck() {
        MiniGameService service = service();

        service.start("link_match", "garden_event");
        String sessionId = service.view().sessionId();
        String response = service.handleInput(sessionId, "match", Map.of("rowA", 0, "colA", 0, "rowB", 0, "colB", 1));

        assertEquals("连线成功。", response);
        assertEquals(1, service.view().state().get("matchedPairs"));
    }

    @Test
    void acknowledgeAppliesRewardsAndClearsState() {
        MiniGameService service = service(5, 5);
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        service.start("dice_check", "order_altar_event");
        service.handleInput(service.view().sessionId(), "roll", Map.of());
        service.acknowledge(playerService, worldState);

        assertFalse(service.hasActiveMiniGame());
        assertFalse(service.hasPendingOutcome());
        assertTrue(playerService.inventoryItems().contains("blank_dice"));
        assertTrue(worldState.getBoolean("event_completed.order_altar_event"));
    }

    private MiniGameService service(int... values) {
        return new MiniGameService(new MiniGameRewardResolver(), new SequenceRandom(values));
    }

    private static class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        SequenceRandom(int... values) {
            this.values = values.length == 0 ? new int[]{0} : values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index++ % values.length];
            return Math.floorMod(value, bound);
        }

        @Override
        public int nextInt(int origin, int bound) {
            int value = values[index++ % values.length];
            return origin + Math.floorMod(value, bound - origin);
        }
    }
}
