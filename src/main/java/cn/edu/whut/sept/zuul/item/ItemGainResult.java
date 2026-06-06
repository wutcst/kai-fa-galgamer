package cn.edu.whut.sept.zuul.item;

public record ItemGainResult(
        String itemId,
        String itemName,
        boolean added,
        boolean keyItem
) {
    public String message() {
        if (added) {
            return "获得物品：" + itemName;
        }
        if (keyItem) {
            return "关键道具已持有：" + itemName;
        }
        return "物品已持有：" + itemName;
    }
}
