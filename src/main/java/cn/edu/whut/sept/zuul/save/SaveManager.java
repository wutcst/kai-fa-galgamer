package cn.edu.whut.sept.zuul.save;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SaveManager {

    private final SaveService saveService;

    public SaveManager(SaveService saveService) {
        this.saveService = saveService;
    }

    public String save(String saveId, SaveStateAccess access) {
        if (access.saveBlocked()) {
            throw new IllegalStateException("请先完成并确认小游戏结果，再保存。");
        }
        GameSaveData data = new GameSaveData();
        data.setSaveId(saveId);
        data.setCurrentRoomId(access.currentRoomId());
        data.setPlayerHp(access.playerHp());
        data.setInventoryItems(access.inventoryItems());
        data.setFlags(access.flags());
        data.setCounters(access.counters());
        data.setGamePhase(access.phaseForSave());
        data.setBossState(access.bossForSave());
        data.setEndingState(access.endingForSave());
        return saveService.save(data);
    }

    public Optional<GameSaveData> load(String saveId, SaveStateAccess access) {
        Optional<GameSaveData> loaded = saveService.load(saveId);
        loaded.ifPresent(data -> restoreInOrder(data, access));
        return loaded;
    }

    private void restoreInOrder(GameSaveData data, SaveStateAccess access) {
        access.restoreWorldState(data.getFlags(), data.getCounters());
        access.restoreSpatialContext(data.getCurrentRoomId());
        access.restorePlayer(data.getPlayerHp(), data.getInventoryItems());
        access.restoreTransientState(data.getBossState(), data.getEndingState());
        access.restorePhase(data.getGamePhase());
    }

    public SaveService saveService() {
        return saveService;
    }
}
