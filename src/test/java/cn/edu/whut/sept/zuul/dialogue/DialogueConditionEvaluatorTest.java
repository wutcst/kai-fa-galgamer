package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueConditionEvaluatorTest {

    @Test
    void evaluatesInventoryHpAndWorldFlagWithConfidence() {
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        worldState.setBoolean("mirror_broken", true);
        playerService.gainItem("nameless_badge");
        playerService.damage(40);

        DialogueConditionEvaluator.Evaluation evaluation = new DialogueConditionEvaluator().evaluate(List.of(
                new DialogueCondition("HAS_ITEM", "nameless_badge", null, null, null),
                new DialogueCondition("HP_GREATER_THAN", null, null, null, 80),
                new DialogueCondition("WORLD_FLAG_EQUALS", null, "mirror_broken", true, null)
        ), playerService, worldState);

        assertFalse(evaluation.available());
        assertEquals(2.0 / 3.0, evaluation.confidence());
        assertTrue(evaluation.lockedReason().contains("生命值"));
    }

    @Test
    void emptyConditionsAreAlwaysAvailable() {
        DialogueConditionEvaluator.Evaluation evaluation = new DialogueConditionEvaluator().evaluate(
                List.of(),
                new PlayerService(new ItemService()),
                new WorldState()
        );

        assertTrue(evaluation.available());
        assertEquals(1.0, evaluation.confidence());
    }
}
