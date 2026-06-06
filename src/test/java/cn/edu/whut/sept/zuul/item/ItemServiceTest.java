package cn.edu.whut.sept.zuul.item;

import cn.edu.whut.sept.zuul.service.ItemService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest {

    @Test
    void defaultItemsContainAntiCycleKeysAndSoulBell() {
        ItemService itemService = new ItemService();

        assertTrue(itemService.exists("blank_dice"));
        assertTrue(itemService.exists("savebreaker_key"));
        assertTrue(itemService.exists("nameless_badge"));
        assertTrue(itemService.exists("pure_seed"));
        assertTrue(itemService.exists("throne_fragment"));
        assertTrue(itemService.exists("soul_bell"));
        assertTrue(itemService.isKeyItem("blank_dice"));
        assertFalse(itemService.isKeyItem("soul_bell"));
    }

    @Test
    void soulBellCraftingReportsMissingMaterials() {
        ItemService itemService = new ItemService();

        CraftResult result = itemService.craftSoulBell(List.of("broken_bell"));

        assertFalse(result.success());
        assertTrue(result.missingItemIds().contains("soul_flower"));
        assertTrue(result.missingItemIds().contains("silver_thread"));
    }
}
