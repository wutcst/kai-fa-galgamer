package cn.edu.whut.sept.zuul.save;

import cn.edu.whut.sept.zuul.model.GamePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SaveManagerRestoreOrderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadShouldRestoreInStrictLifecycleOrder() {
        SaveService saveService = new SaveService(tempDir);
        SaveManager saveManager = new SaveManager(saveService);
        RecordingAccess source = new RecordingAccess();
        saveManager.save("slot_1", source);

        RecordingAccess target = new RecordingAccess();
        saveManager.load("slot_1", target);

        assertEquals(List.of("world", "spatial", "visited", "player", "transient", "phase"), target.calls);
        assertNotNull(target.restoredBossState);
        assertEquals("CORE_CHARGE", target.restoredBossState.getCurrentIntent());
    }

    private static class RecordingAccess implements SaveStateAccess {
        private final List<String> calls = new ArrayList<>();
        private BossSaveData restoredBossState;

        @Override
        public String currentRoomId() {
            return "fate_hall";
        }

        @Override
        public int playerHp() {
            return 88;
        }

        @Override
        public List<String> inventoryItems() {
            return List.of("blank_dice");
        }

        @Override
        public List<String> visitedRoomIds() {
            return List.of("fate_hall");
        }

        @Override
        public Map<String, Boolean> flags() {
            return Map.of("test.flag", true);
        }

        @Override
        public Map<String, Integer> counters() {
            return Map.of("counter", 1);
        }

        @Override
        public GamePhase phaseForSave() {
            return GamePhase.BATTLE;
        }

        @Override
        public BossSaveData bossForSave() {
            BossSaveData data = new BossSaveData();
            data.setHp(77);
            data.setMaxHp(120);
            data.setCurrentIntent("CORE_CHARGE");
            data.setTurn(4);
            data.setSoulBellCooldown(1);
            return data;
        }

        @Override
        public EndingSaveData endingForSave() {
            return null;
        }

        @Override
        public boolean saveBlocked() {
            return false;
        }

        @Override
        public void restoreWorldState(Map<String, Boolean> flags, Map<String, Integer> counters) {
            calls.add("world");
        }

        @Override
        public void restoreSpatialContext(String currentRoomId) {
            calls.add("spatial");
        }

        @Override
        public void restoreVisitedRooms(List<String> visitedRoomIds) {
            calls.add("visited");
        }

        @Override
        public void restorePlayer(int hp, List<String> inventoryItems) {
            calls.add("player");
        }

        @Override
        public void restoreTransientState(BossSaveData bossState, EndingSaveData endingState) {
            calls.add("transient");
            restoredBossState = bossState;
        }

        @Override
        public void restorePhase(GamePhase phase) {
            calls.add("phase");
        }
    }
}
