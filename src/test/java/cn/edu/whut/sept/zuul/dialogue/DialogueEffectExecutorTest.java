package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueEffectExecutorTest {

    @Test
    void appliesHpItemFlagAndPuzzleEffects() {
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        DialogueEffectExecutor executor = new DialogueEffectExecutor();

        executor.execute(List.of(
                new DialogueEffect("LOSE_HP", null, null, null, 15, null),
                new DialogueEffect("GAIN_ITEM", "nameless_badge", null, null, null, null),
                new DialogueEffect("SET_FLAG", null, "lied_to_scholar", true, null, null)
        ), playerService, worldState);

        assertEquals(85, playerService.hp());
        assertTrue(playerService.inventoryItems().contains("nameless_badge"));
        assertTrue(worldState.getBoolean("lied_to_scholar"));

        executor.execute(List.of(
                new DialogueEffect("LOSE_ITEM", "nameless_badge", null, null, null, null),
                new DialogueEffect("BYPASS_PUZZLE", null, null, null, null, "triple-seal-gate-unlock")
        ), playerService, worldState);

        assertFalse(playerService.inventoryItems().contains("nameless_badge"));
        assertTrue(worldState.getBoolean("triple_seal_open"));
    }
}
