package cn.edu.whut.sept.zuul.dialogue;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DialogueConditionEvaluator {

    public Evaluation evaluate(List<DialogueCondition> conditions, PlayerService playerService, WorldState worldState) {
        if (conditions == null || conditions.isEmpty()) {
            return new Evaluation(true, 1.0, "");
        }
        int matched = 0;
        String firstLockedReason = "";
        for (DialogueCondition condition : conditions) {
            boolean passed = matches(condition, playerService, worldState);
            if (passed) {
                matched++;
            } else if (firstLockedReason.isBlank()) {
                firstLockedReason = lockedReason(condition);
            }
        }
        double confidence = (double) matched / conditions.size();
        return new Evaluation(matched == conditions.size(), confidence, firstLockedReason);
    }

    private boolean matches(DialogueCondition condition, PlayerService playerService, WorldState worldState) {
        String type = normalize(condition.type());
        return switch (type) {
            case "HAS_ITEM" -> playerService.inventoryItems().contains(condition.itemKey());
            case "HP_GREATER_THAN" -> playerService.hp() > safeInt(condition.value());
            case "WORLD_FLAG_EQUALS" -> worldState.getBoolean(condition.flagKey()) == Boolean.TRUE.equals(condition.expected());
            default -> false;
        };
    }

    private String lockedReason(DialogueCondition condition) {
        String type = normalize(condition.type());
        return switch (type) {
            case "HAS_ITEM" -> "需要持有 " + condition.itemKey();
            case "HP_GREATER_THAN" -> "需要生命值高于 " + safeInt(condition.value());
            case "WORLD_FLAG_EQUALS" -> "需要世界标记 " + condition.flagKey() + " = " + Boolean.TRUE.equals(condition.expected());
            default -> "未知条件：" + condition.type();
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }

    public record Evaluation(
            boolean available,
            double confidence,
            String lockedReason
    ) {
    }
}
