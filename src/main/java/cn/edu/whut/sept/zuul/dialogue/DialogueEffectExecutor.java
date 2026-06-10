package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DialogueEffectExecutor {

    public void execute(List<DialogueEffect> effects, PlayerService playerService, WorldState worldState) {
        if (effects == null) {
            return;
        }
        effects.forEach(effect -> execute(effect, playerService, worldState));
    }

    public void executeEvent(DialogueNode node, PlayerService playerService, WorldState worldState) {
        if (node == null || node.eventType() == null || node.eventType().isBlank()) {
            return;
        }
        execute(new DialogueEffect(node.eventType(), null, null, null, null, node.eventPayload()), playerService, worldState);
    }

    private void execute(DialogueEffect effect, PlayerService playerService, WorldState worldState) {
        String type = effect.type() == null ? "" : effect.type().trim().toUpperCase(Locale.ROOT);
        switch (type) {
            case "GAIN_HP" -> playerService.heal(safeInt(effect.value()));
            case "LOSE_HP" -> playerService.damage(safeInt(effect.value()));
            case "GAIN_ITEM" -> playerService.gainItem(effect.itemKey());
            case "LOSE_ITEM" -> playerService.consumeItem(effect.itemKey());
            case "SET_FLAG" -> worldState.setBoolean(effect.flagKey(), Boolean.TRUE.equals(effect.booleanValue()));
            case "BYPASS_PUZZLE" -> bypassPuzzle(effect.eventPayload(), worldState);
            default -> {
            }
        }
    }

    private void bypassPuzzle(String payload, WorldState worldState) {
        if ("triple-seal-gate-unlock".equals(payload)) {
            worldState.setBoolean("triple_seal_open", true);
            worldState.setBoolean("puzzle_solved.triple_seal_gate", true);
        }
        worldState.setBoolean("dialogue_event." + payload, true);
    }

    private int safeInt(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }
}
