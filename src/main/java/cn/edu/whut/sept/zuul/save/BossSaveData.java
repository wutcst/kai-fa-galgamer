package cn.edu.whut.sept.zuul.save;

import java.util.ArrayList;
import java.util.List;

public class BossSaveData {

    private String bossId = "zuul_overlord";
    private String name = "Zuul Overlord";
    private int hp;
    private int maxHp;
    private String phase = "PHASE_1";
    private List<String> logs = new ArrayList<>();

    public String getBossId() {
        return bossId;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
    }
}
