package cn.edu.whut.sept.zuul.event;

import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventEngineTest {

    @Test
    void duplicateEventIdsAreRejected() {
        EventEngine engine = new EventEngine();

        assertThrows(IllegalArgumentException.class,
                () -> engine.register(new WorldFlagEvent("library_clue_event", "duplicate", List.of(), List.of())));
    }

    @Test
    void eventRewardsAndFlagsAreAppliedOnce() {
        EventEngine engine = new EventEngine();
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        EventResult first = engine.trigger("broken_shelf_event", playerService, worldState);
        EventResult second = engine.trigger("broken_shelf_event", playerService, worldState);

        assertEquals(EventResultType.MESSAGE, first.type());
        assertEquals(EventResultType.MESSAGE, second.type());
        assertTrue(worldState.getBoolean("memory_shard"));
        assertTrue(worldState.getBoolean("event_completed.broken_shelf_event"));
        assertEquals(3, playerService.inventoryItems().size());
        assertTrue(playerService.inventoryItems().contains("blank_dice"));
        assertTrue(playerService.inventoryItems().contains("nameless_badge"));
        assertTrue(playerService.inventoryItems().contains("boss_truth"));
    }

    @Test
    void unknownEventDoesNotPolluteCompletedFlags() {
        WorldState worldState = new WorldState();

        EventResult result = new EventEngine().trigger("missing_event", new PlayerService(new ItemService()), worldState);

        assertEquals(EventResultType.NONE, result.type());
        assertFalse(worldState.getBoolean("event_completed.missing_event"));
    }
}
