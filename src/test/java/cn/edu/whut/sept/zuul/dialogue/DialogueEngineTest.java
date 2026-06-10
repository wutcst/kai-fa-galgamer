package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.model.GamePhase;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueEngineTest {

    @Test
    void speechChoiceAndExitFlowRestoresSuspendedPhase() {
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        DialogueEngine engine = engine();

        engine.start("dial_fate_hall_meeting", GamePhase.EXPLORING);

        GameSnapshot.DialogueView first = engine.view(playerService, worldState);
        assertNotNull(first);
        assertEquals("SPEECH", first.nodeType());
        assertEquals("RIGHT", first.speakerSide());

        assertEquals(GamePhase.EXPLORING, engine.advance(playerService, worldState));
        GameSnapshot.DialogueView choice = engine.view(playerService, worldState);
        assertEquals("CHOICE", choice.nodeType());
        assertFalse(choice.choices().get(2).available());

        engine.choose("threat", playerService, worldState);
        assertEquals(90, playerService.hp());
        assertEquals("node_choice_b_speech", engine.view(playerService, worldState).currentNodeId());

        assertEquals(GamePhase.EXPLORING, engine.advance(playerService, worldState));
        assertNull(engine.view(playerService, worldState));
        assertTrue(worldState.getBoolean("dialogue_completed.dial_fate_hall_meeting"));
    }

    @Test
    void perfectChoiceExecutesEventTriggerWhenConditionsPass() {
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        DialogueEngine engine = engine();
        playerService.gainItem("nameless_badge");

        engine.start("dial_fate_hall_meeting", GamePhase.EXPLORING);
        engine.advance(playerService, worldState);
        engine.choose("badge_truth", playerService, worldState);

        assertEquals("EVENT_TRIGGER", engine.view(playerService, worldState).nodeType());
        engine.advance(playerService, worldState);

        assertTrue(worldState.getBoolean("triple_seal_open"));
        assertTrue(worldState.getBoolean("dialogue_completed.dial_fate_hall_meeting"));
    }

    private DialogueEngine engine() {
        return new DialogueEngine(new DialogueConditionEvaluator(), new DialogueEffectExecutor());
    }
}
