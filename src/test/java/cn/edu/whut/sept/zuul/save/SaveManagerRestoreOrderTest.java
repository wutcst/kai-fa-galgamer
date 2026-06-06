package cn.edu.whut.sept.zuul.save;

import cn.edu.whut.sept.zuul.model.GamePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(List.of("world", "spatial", "player", "transient", "phase"), target.calls);
    }

    private static class RecordingAccess implements SaveStateAccess {
        private final List<String> calls = new ArrayList<>();

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
        public Map<String, Boolean> flags() {
            return Map.of("test.flag", true);
        }

        @Override
        public Map<String, Integer> counters() {
            return Map.of("counter", 1);
        }

        @Override
        public GamePhase phaseForSave() {
            return GamePhase.BOSS;
        }

        @Override
        public BossSaveData bossForSave() {
            return null;
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
        public void restorePlayer(int hp, List<String> inventoryItems) {
            calls.add("player");
        }

        @Override
        public void restoreTransientState(BossSaveData bossState, EndingSaveData endingState) {
            calls.add("transient");
        }

        @Override
        public void restorePhase(GamePhase phase) {
            calls.add("phase");
        }
    }
}
