package cn.edu.whut.sept.zuul.player;

import cn.edu.whut.sept.zuul.item.CraftResult;
import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerServiceTest {

    @Test
    void duplicateKeyItemsAreNotAddedTwice() {
        PlayerService playerService = new PlayerService(new ItemService());

        playerService.gainItem("blank_dice");
        playerService.gainItem("blank_dice");

        assertEquals(1, playerService.inventoryItems().size());
        assertEquals("blank_dice", playerService.inventoryItems().get(0));
    }

    @Test
    void craftsSoulBellWhenAllMaterialsAreHeld() {
        PlayerService playerService = new PlayerService(new ItemService());
        playerService.gainItem("broken_bell");
        playerService.gainItem("soul_flower");
        playerService.gainItem("silver_thread");

        CraftResult result = playerService.craft("soul_bell");

        assertTrue(result.success());
        assertTrue(playerService.inventoryItems().contains("soul_bell"));
    }

    @Test
    void doesNotCraftSoulBellWithoutMaterials() {
        PlayerService playerService = new PlayerService(new ItemService());

        CraftResult result = playerService.craft("soul_bell");

        assertFalse(result.success());
        assertFalse(playerService.inventoryItems().contains("soul_bell"));
    }
}
