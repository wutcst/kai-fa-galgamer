package cn.edu.whut.sept.zuul.save;

import java.util.ArrayList;
import java.util.List;

public class BossSaveData {

    private String bossId = "zuul_overlord";
    private String name = "Zuul Overlord";
    private int hp;
    private int maxHp;
    private String phase = "PHASE_1";
    private int turn;
    private String currentIntent = "HEAVY_STRIKE";
    private int playerGuard;
    private int rolledBonus;
    private int d100Result;
    private int d100Step;
    private int consecutiveFailures;
    private boolean bonusDieActive;
    private boolean penaltyDieActive;
    private boolean guaranteedCriticalNextTurn;
    private int soulBellCooldown;
    private boolean soulBellUsed;
    private boolean pendingCoreBlast;
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

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public String getCurrentIntent() {
        return currentIntent;
    }

    public void setCurrentIntent(String currentIntent) {
        this.currentIntent = currentIntent;
    }

    public int getPlayerGuard() {
        return playerGuard;
    }

    public void setPlayerGuard(int playerGuard) {
        this.playerGuard = playerGuard;
    }

    public int getRolledBonus() {
        return rolledBonus;
    }

    public void setRolledBonus(int rolledBonus) {
        this.rolledBonus = rolledBonus;
    }

    public int getD100Result() {
        return d100Result;
    }

    public void setD100Result(int d100Result) {
        this.d100Result = d100Result;
    }

    public int getD100Step() {
        return d100Step;
    }

    public void setD100Step(int d100Step) {
        this.d100Step = d100Step;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public boolean isBonusDieActive() {
        return bonusDieActive;
    }

    public void setBonusDieActive(boolean bonusDieActive) {
        this.bonusDieActive = bonusDieActive;
    }

    public boolean isPenaltyDieActive() {
        return penaltyDieActive;
    }

    public void setPenaltyDieActive(boolean penaltyDieActive) {
        this.penaltyDieActive = penaltyDieActive;
    }

    public boolean isGuaranteedCriticalNextTurn() {
        return guaranteedCriticalNextTurn;
    }

    public void setGuaranteedCriticalNextTurn(boolean guaranteedCriticalNextTurn) {
        this.guaranteedCriticalNextTurn = guaranteedCriticalNextTurn;
    }

    public int getSoulBellCooldown() {
        return soulBellCooldown;
    }

    public void setSoulBellCooldown(int soulBellCooldown) {
        this.soulBellCooldown = soulBellCooldown;
    }

    public boolean isSoulBellUsed() {
        return soulBellUsed;
    }

    public void setSoulBellUsed(boolean soulBellUsed) {
        this.soulBellUsed = soulBellUsed;
    }

    public boolean isPendingCoreBlast() {
        return pendingCoreBlast;
    }

    public void setPendingCoreBlast(boolean pendingCoreBlast) {
        this.pendingCoreBlast = pendingCoreBlast;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
    }
}
