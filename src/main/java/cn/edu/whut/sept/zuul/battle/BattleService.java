package cn.edu.whut.sept.zuul.battle;

import cn.edu.whut.sept.zuul.save.BossSaveData;
import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BattleService {

    private static final String BOSS_ID = "zuul_overlord";
    private static final String BOSS_NAME = "祖尔王座主宰";
    private BossState activeBoss;

    public synchronized BossState start(PlayerService playerService, WorldState worldState) {
        if (activeBoss != null && !activeBoss.defeated()) {
            return activeBoss;
        }
        int maxHp = playerService.inventoryItems().contains(ItemService.SOUL_BELL) ? 72 : 96;
        activeBoss = new BossState(BOSS_ID, BOSS_NAME, maxHp, maxHp, "PHASE_1", new ArrayList<>());
        activeBoss.logs().add("王座阴影抬起头，祖尔主宰向你宣战。");
        if (playerService.inventoryItems().contains(ItemService.SOUL_BELL)) {
            activeBoss.logs().add("灵魂之铃削弱了王座回响，Boss 最大生命下降。");
            worldState.setBoolean("boss.weakened_by_soul_bell", true);
        }
        return activeBoss;
    }

    public synchronized BattleResult act(String action, PlayerService playerService, WorldState worldState) {
        if (activeBoss == null || activeBoss.defeated()) {
            return new BattleResult(false, false, "当前没有进行中的 Boss 战。");
        }
        String normalized = action == null || action.isBlank() ? "attack" : action.trim().toLowerCase();
        int playerDamage;
        int bossDamage;
        switch (normalized) {
            case "defend" -> {
                playerDamage = 8;
                bossDamage = 4;
                activeBoss.logs().add("你架起防御，抓住间隙反击。");
            }
            case "use_soul_bell" -> {
                if (playerService.inventoryItems().contains(ItemService.SOUL_BELL)) {
                    playerDamage = 24;
                    bossDamage = 6;
                    worldState.setBoolean("boss.soul_bell_used", true);
                    activeBoss.logs().add("灵魂之铃震碎一层王座护甲。");
                } else {
                    playerDamage = 4;
                    bossDamage = 12;
                    activeBoss.logs().add("你没有灵魂之铃，动作露出破绽。");
                }
            }
            default -> {
                playerDamage = worldState.getBoolean("boss.soul_bell_used") ? 18 : 14;
                bossDamage = worldState.getBoolean("boss.weakened_by_soul_bell") ? 8 : 12;
                activeBoss.logs().add("你向王座主宰发起攻击。");
            }
        }
        activeBoss.damage(playerDamage);
        if (activeBoss.hp() <= activeBoss.maxHp() / 2 && "PHASE_1".equals(activeBoss.phase())) {
            activeBoss.phase("PHASE_2");
            activeBoss.logs().add("祖尔主宰进入第二阶段，命运骰开始崩裂。");
        }
        if (activeBoss.defeated()) {
            playerService.gainItem("throne_fragment");
            worldState.setBoolean("boss.zuul_defeated", true);
            activeBoss.logs().add("王座主宰倒下，王座碎片落入你的掌心。");
            return new BattleResult(true, false, "Boss 已被击败。");
        }
        playerService.damage(bossDamage);
        activeBoss.logs().add("祖尔反击造成 " + bossDamage + " 点伤害。");
        if (playerService.hp() <= 0) {
            worldState.setBoolean("ending.fallen_to_zuul", true);
            return new BattleResult(false, true, "你倒在王座前，循环再次收束。");
        }
        return new BattleResult(false, false, "Boss 回合结算完成。");
    }

    public synchronized BossState activeBoss() {
        return activeBoss;
    }

    public synchronized void clear() {
        activeBoss = null;
    }

    public synchronized BossSaveData saveData() {
        if (activeBoss == null || activeBoss.defeated()) {
            return null;
        }
        BossSaveData data = new BossSaveData();
        data.setBossId(activeBoss.bossId());
        data.setName(activeBoss.name());
        data.setHp(activeBoss.hp());
        data.setMaxHp(activeBoss.maxHp());
        data.setPhase(activeBoss.phase());
        data.setLogs(activeBoss.logs());
        return data;
    }

    public synchronized void restore(BossSaveData data) {
        if (data == null || data.getHp() <= 0 || data.getMaxHp() <= 0) {
            activeBoss = null;
            return;
        }
        activeBoss = new BossState(
                data.getBossId(),
                data.getName(),
                data.getHp(),
                data.getMaxHp(),
                data.getPhase(),
                new ArrayList<>(data.getLogs())
        );
    }

    public static class BossState {
        private final String bossId;
        private final String name;
        private int hp;
        private final int maxHp;
        private String phase;
        private final List<String> logs;

        public BossState(String bossId, String name, int hp, int maxHp, String phase, List<String> logs) {
            this.bossId = bossId;
            this.name = name;
            this.hp = hp;
            this.maxHp = maxHp;
            this.phase = phase;
            this.logs = logs;
        }

        public String bossId() {
            return bossId;
        }

        public String name() {
            return name;
        }

        public int hp() {
            return hp;
        }

        public int maxHp() {
            return maxHp;
        }

        public String phase() {
            return phase;
        }

        public List<String> logs() {
            return logs;
        }

        void damage(int amount) {
            int damage = Math.max(0, amount);
            hp = Math.max(0, hp - damage);
            logs.add("Boss 受到 " + damage + " 点伤害。");
        }

        public boolean defeated() {
            return hp <= 0;
        }

        private void phase(String nextPhase) {
            phase = nextPhase;
            logs.add("阶段切换：" + nextPhase);
        }
    }

    public record BattleResult(boolean victory, boolean defeat, String message) {
    }
}
