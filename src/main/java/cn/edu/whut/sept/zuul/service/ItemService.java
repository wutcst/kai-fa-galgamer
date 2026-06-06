package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.item.CraftResult;
import cn.edu.whut.sept.zuul.item.Item;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ItemService {

    public static final String SOUL_BELL = "soul_bell";

    private static final Set<String> ANTI_CYCLE_KEY_ITEMS = Set.of(
            "blank_dice",
            "savebreaker_key",
            "nameless_badge",
            "pure_seed",
            "throne_fragment"
    );

    private static final List<String> SOUL_BELL_RECIPE = List.of(
            "broken_bell",
            "soul_flower",
            "silver_thread"
    );

    private final Map<String, Item> items = createItems();

    public Item get(String itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("未知物品：" + itemId);
        }
        return item;
    }

    public boolean exists(String itemId) {
        return items.containsKey(itemId);
    }

    public boolean isKeyItem(String itemId) {
        return ANTI_CYCLE_KEY_ITEMS.contains(itemId);
    }

    public CraftResult craftSoulBell(List<String> inventoryItems) {
        return craft(SOUL_BELL, inventoryItems);
    }

    public CraftResult craft(String resultItemId, List<String> inventoryItems) {
        if (!SOUL_BELL.equals(resultItemId)) {
            return CraftResult.failure(resultItemId, "未知合成配方：" + resultItemId, List.of());
        }
        if (inventoryItems.contains(SOUL_BELL)) {
            return CraftResult.success(SOUL_BELL, "灵魂之铃已在背包中。");
        }

        List<String> missing = SOUL_BELL_RECIPE.stream()
                .filter(itemId -> !inventoryItems.contains(itemId))
                .toList();
        if (!missing.isEmpty()) {
            return CraftResult.failure(SOUL_BELL, "合成灵魂之铃失败，缺少材料：" + displayNames(missing), missing);
        }
        return CraftResult.success(SOUL_BELL, "灵魂之铃合成完成。");
    }

    private String displayNames(List<String> itemIds) {
        return String.join("、", itemIds.stream().map(itemId -> get(itemId).name()).toList());
    }

    private static Map<String, Item> createItems() {
        Map<String, Item> map = new LinkedHashMap<>();
        register(map, "blank_dice", "空白骰子");
        register(map, "savebreaker_key", "断档之钥");
        register(map, "nameless_badge", "无名徽章");
        register(map, "pure_seed", "纯净种子");
        register(map, "throne_fragment", "王座碎片");
        register(map, SOUL_BELL, "灵魂之铃");
        register(map, "broken_bell", "破损铃铛");
        register(map, "soul_flower", "灵魂花");
        register(map, "silver_thread", "银线");
        register(map, "library_note", "禁书残页");
        register(map, "rune_clue", "符文线索");
        register(map, "boss_truth", "祖尔真相");
        register(map, "mirror_shard", "镜面碎片");
        register(map, "purified_ash", "净化灰烬");
        return Map.copyOf(map);
    }

    private static void register(Map<String, Item> map, String id, String name) {
        if (map.containsKey(id)) {
            throw new IllegalArgumentException("重复物品 ID：" + id);
        }
        map.put(id, new Item(id, name, ANTI_CYCLE_KEY_ITEMS.contains(id)));
    }

    public List<Item> allItems() {
        return new ArrayList<>(items.values());
    }
}
