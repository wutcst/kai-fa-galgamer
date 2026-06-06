package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.item.CraftResult;
import cn.edu.whut.sept.zuul.item.Item;
import cn.edu.whut.sept.zuul.item.ItemGainResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerService {

    private static final int INITIAL_HP = 100;

    private final ItemService itemService;
    private final List<String> inventoryItems = new ArrayList<>();
    private int hp = INITIAL_HP;

    public PlayerService(ItemService itemService) {
        this.itemService = itemService;
    }

    public synchronized void reset() {
        hp = INITIAL_HP;
        inventoryItems.clear();
    }

    public synchronized void restore(int restoredHp, List<String> restoredInventoryItems) {
        hp = Math.max(0, restoredHp);
        inventoryItems.clear();
        if (restoredInventoryItems != null) {
            restoredInventoryItems.stream()
                    .filter(itemService::exists)
                    .distinct()
                    .forEach(inventoryItems::add);
        }
    }

    public synchronized int hp() {
        return hp;
    }

    public synchronized void damage(int amount) {
        hp = Math.max(0, hp - Math.max(0, amount));
    }

    public synchronized List<String> inventoryItems() {
        return List.copyOf(inventoryItems);
    }

    public synchronized ItemGainResult gainItem(String itemId) {
        Item item = itemService.get(itemId);
        boolean added = false;
        if (!inventoryItems.contains(item.id())) {
            inventoryItems.add(item.id());
            added = true;
        }
        return new ItemGainResult(item.id(), item.name(), added, item.keyItem());
    }

    public synchronized List<ItemGainResult> gainItems(List<String> itemIds) {
        return itemIds.stream().map(this::gainItem).toList();
    }

    public synchronized CraftResult craft(String itemId) {
        CraftResult result = itemService.craft(itemId, inventoryItems());
        if (result.success() && !inventoryItems.contains(result.resultItemId())) {
            gainItem(result.resultItemId());
        }
        return result;
    }
}
