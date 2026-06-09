package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleServiceTest {

    @Test
    void d100StepUsesSmoothThresholds() {
        BattleService battleService = new BattleService(new SequenceRandom(1));

        assertEquals(1, battleService.getD100Step(5, 60));
        assertEquals(2, battleService.getD100Step(24, 60));
        assertEquals(3, battleService.getD100Step(42, 60));
        assertEquals(4, battleService.getD100Step(60, 60));
        assertEquals(5, battleService.getD100Step(61, 60));
        assertEquals(6, battleService.getD100Step(100, 60));
    }

    @Test
    void intentCounterPlayCanPushBossIntoPhaseTwo() {
        BattleService battleService = new BattleService(new SequenceRandom(1));
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        battleService.startFinalBattle(worldState);
        battleService.perform("defend", playerService, worldState);
        battleService.perform("roll", playerService, worldState);
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
        BattleService battleService = new BattleService(new SequenceRandom(1));
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        battleService.startFinalBattle(worldState);
        for (int i = 0; i < 2; i++) {
            battleService.perform("defend", playerService, worldState);
            battleService.perform("roll", playerService, worldState);
            battleService.perform("attack", playerService, worldState);
        }

        assertFalse(battleService.hasActiveBattle());
        assertNull(battleService.view());
        assertTrue(worldState.getBoolean("final_boss_defeated"));
        assertTrue(worldState.getBoolean("event_completed.final_boss"));
        assertTrue(playerService.inventoryItems().contains("throne_fragment"));
    }

    @Test
    void blankDiceForcesFateDistortionRollToHardSuccess() {
        BattleService battleService = new BattleService(new SequenceRandom(90, 90));
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        playerService.gainItem("blank_dice");

        battleService.startFinalBattle(worldState);
        battleService.perform("defend", playerService, worldState);
        battleService.perform("roll", playerService, worldState);

        GameSnapshot.BattleView battleView = battleService.view();
        assertNotNull(battleView);
        assertEquals(3, battleView.d100Step());
        assertEquals(15, battleView.rolledBonus());
    }

    @Test
    void savebreakerKeyPreventsLethalDamageAndGuaranteesNextCritical() {
        BattleService battleService = new BattleService(new SequenceRandom(90, 90));
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        playerService.gainItem("savebreaker_key");
        playerService.damage(90);

        battleService.startFinalBattle(worldState);
        battleService.perform("attack", playerService, worldState);

        assertEquals(1, playerService.hp());
        assertFalse(playerService.inventoryItems().contains("savebreaker_key"));
        assertTrue(battleService.view().battleHints().contains("下一次判定必定大成功"));
    }

    @Test
    void activeBattleStateCanBeSavedAndRestored() {
        BattleService battleService = new BattleService(new SequenceRandom(12, 12));
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        battleService.startFinalBattle(worldState);
        battleService.perform("defend", playerService, worldState);

        BattleService restored = new BattleService(new SequenceRandom(12, 12));
        restored.restore(battleService.saveData());

        GameSnapshot.BattleView battleView = restored.view();
        assertNotNull(battleView);
        assertEquals("FATE_DISTORTION", battleView.currentIntent());
        assertEquals(2, battleView.turn());
        assertEquals(12, battleView.d100Result());
    }

    private static class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        SequenceRandom(int... values) {
            this.values = values.length == 0 ? new int[]{1} : values;
        }

        @Override
        public int nextInt(int origin, int bound) {
            int value = values[index++ % values.length];
            return Math.max(origin, Math.min(bound - 1, value));
        }
    }
}
