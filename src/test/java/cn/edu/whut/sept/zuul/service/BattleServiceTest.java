package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleServiceTest {

    @Test
    void attackCanPushBossIntoPhaseTwo() {
        BattleService battleService = new BattleService();
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        battleService.startFinalBattle(worldState);
        battleService.perform("attack", playerService, worldState);
        battleService.perform("attack", playerService, worldState);
        battleService.perform("attack", playerService, worldState);

        GameSnapshot.BattleView battleView = battleService.view();
        assertNotNull(battleView);
        assertTrue(battleService.hasActiveBattle());
        assertTrue(worldState.getBoolean("boss_phase_2"));
        assertEquals(2, battleView.phase());
        assertTrue(battleView.traits().contains("phase_2_uncertain_self"));
    }

    @Test
    void defeatingBossGrantsThroneFragmentAndEndsBattle() {
        BattleService battleService = new BattleService();
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        battleService.startFinalBattle(worldState);
        for (int i = 0; i < 6; i++) {
            battleService.perform("attack", playerService, worldState);
        }

        assertFalse(battleService.hasActiveBattle());
        assertNull(battleService.view());
        assertTrue(worldState.getBoolean("final_boss_defeated"));
        assertTrue(worldState.getBoolean("event_completed.final_boss"));
        assertTrue(playerService.inventoryItems().contains("throne_fragment"));
    }
}
