package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.item.CraftResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryFeatureTest {

    @Test
    void duplicateKeyItemIsKeptOnce() {
        PlayerService playerService = new PlayerService(new ItemService());

        playerService.gainItem("blank_dice");
        playerService.gainItem("blank_dice");

        assertEquals(1, playerService.inventoryItems().size());
        assertEquals("blank_dice", playerService.inventoryItems().get(0));
    }

    @Test
    void soulBellIsCraftedByBackendWhenMaterialsAreReady() {
        PlayerService playerService = new PlayerService(new ItemService());
        playerService.gainItem("broken_bell");
        playerService.gainItem("soul_flower");
        playerService.gainItem("silver_thread");

        CraftResult result = playerService.craft("soul_bell");

        assertTrue(result.success());
        assertTrue(playerService.inventoryItems().contains("soul_bell"));
    }
}
