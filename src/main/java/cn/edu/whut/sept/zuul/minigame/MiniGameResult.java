package cn.edu.whut.sept.zuul.minigame;

import java.util.List;
import java.util.Map;

public record MiniGameResult(
        MiniGameResultType type,
        int score,
        String message,
        List<String> rewardItems,
        Map<String, Boolean> flags,
        Map<String, Object> details
) {
    public MiniGameResult {
        rewardItems = rewardItems == null ? List.of() : List.copyOf(rewardItems);
        flags = flags == null ? Map.of() : Map.copyOf(flags);
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
