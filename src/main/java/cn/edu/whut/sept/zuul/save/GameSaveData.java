package cn.edu.whut.sept.zuul.save;

import cn.edu.whut.sept.zuul.model.GamePhase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSaveData {

    private int schemaVersion = 2;
    private String saveId = "slot_1";
    private String currentRoomId = "fate_hall";
    private int playerHp = 100;
    private List<String> inventoryItems = new ArrayList<>();
    private List<String> visitedRoomIds = new ArrayList<>();
    private Map<String, Boolean> flags = new HashMap<>();
    private Map<String, Integer> counters = new HashMap<>();
    private GamePhase gamePhase = GamePhase.EXPLORING;
    private BossSaveData bossState;
    private EndingSaveData endingState;
    private long savedAt;

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getSaveId() {
        return saveId;
    }

    public void setSaveId(String saveId) {
        this.saveId = saveId;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public int getPlayerHp() {
        return playerHp;
    }

    public void setPlayerHp(int playerHp) {
        this.playerHp = playerHp;
    }

    public List<String> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<String> inventoryItems) {
        this.inventoryItems = inventoryItems == null ? new ArrayList<>() : new ArrayList<>(inventoryItems);
    }

    public List<String> getVisitedRoomIds() {
        return visitedRoomIds;
    }

    public void setVisitedRoomIds(List<String> visitedRoomIds) {
        this.visitedRoomIds = visitedRoomIds == null ? new ArrayList<>() : new ArrayList<>(visitedRoomIds);
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public void setFlags(Map<String, Boolean> flags) {
        this.flags = flags == null ? new HashMap<>() : new HashMap<>(flags);
    }

    public Map<String, Integer> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, Integer> counters) {
        this.counters = counters == null ? new HashMap<>() : new HashMap<>(counters);
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase == null ? GamePhase.EXPLORING : gamePhase;
    }

    public BossSaveData getBossState() {
        return bossState;
    }

    public void setBossState(BossSaveData bossState) {
        this.bossState = bossState;
    }

    public EndingSaveData getEndingState() {
        return endingState;
    }

    public void setEndingState(EndingSaveData endingState) {
        this.endingState = endingState;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(long savedAt) {
        this.savedAt = savedAt;
    }
}
