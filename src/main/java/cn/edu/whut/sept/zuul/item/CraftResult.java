package cn.edu.whut.sept.zuul.item;

import java.util.List;

public record CraftResult(
        boolean success,
        String resultItemId,
        String message,
        List<String> missingItemIds
) {
    public static CraftResult success(String resultItemId, String message) {
        return new CraftResult(true, resultItemId, message, List.of());
    }

    public static CraftResult failure(String resultItemId, String message, List<String> missingItemIds) {
        return new CraftResult(false, resultItemId, message, List.copyOf(missingItemIds));
    }
}
