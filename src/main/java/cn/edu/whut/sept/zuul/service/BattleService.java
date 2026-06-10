package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.save.BossSaveData;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class BattleService {

    public enum BossIntent {
        HEAVY_STRIKE,
        FATE_DISTORTION,
        CORE_CHARGE
    }

    private static final String FINAL_BOSS_ID = "zuul_overlord";
    private static final String FINAL_BOSS_NAME = "Zuul Overlord";
    private static final int BOSS_MAX_HP = 120;
    private static final int PHASE_TWO_THRESHOLD = BOSS_MAX_HP / 2;
    private static final int BASE_SKILL = 60;
    private static final int NAMELESS_BADGE_BONUS = 15;
    private static final int MAX_SKILL = 95;

    private final Random random;
    private BattleState activeBattle;

    public BattleService() {
        this(new Random());
    }

    BattleService(Random random) {
        this.random = random;
    }

    public synchronized void reset() {
        activeBattle = null;
    }

    public synchronized boolean hasActiveBattle() {
        return activeBattle != null;
    }

    public synchronized String startFinalBattle(WorldState worldState) {
        if (worldState.getBoolean("final_boss_defeated")) {
            return "祖尔霸主已经倒下，命运核心正在等待最后选择。";
        }
        if (activeBattle != null) {
            return "祖尔霸主仍在王座前等待你的下一次行动。";
        }
        activeBattle = new BattleState();
        worldState.setBoolean("final_boss_started", true);
        return "Zuul Overlord 从王座阴影中起身。最终 Boss 战开始。";
    }

    public synchronized String perform(String action, PlayerService playerService, WorldState worldState) {
        if (activeBattle == null) {
            return "当前没有正在进行的 Boss 战。";
        }
        if (playerService.hp() <= 0) {
            worldState.setBoolean("final_boss_lost", true);
            return "你倒在王座前。循环尚未终结。";
        }

        String prelude = releaseCoreBlastIfNeeded(playerService, worldState);
        if (playerService.hp() <= 0) {
            activeBattle.lastAction = "core_blast";
            activeBattle.message = prelude + " 你倒在王座前，循环开始回卷。";
            return activeBattle.message;
        }

        String normalized = action == null ? "attack" : action.trim().toLowerCase(Locale.ROOT);
        String result = switch (normalized) {
            case "attack" -> attack(playerService, worldState);
            case "defend" -> defend(playerService, worldState);
            case "roll" -> roll(playerService, worldState);
            case "use_soul_bell" -> useSoulBell(playerService, worldState);
            case "flee" -> flee();
            default -> "Boss 战动作无效：" + action;
        };
        if (!prelude.isBlank() && activeBattle != null) {
            activeBattle.message = prelude + " " + activeBattle.message;
            return activeBattle.message;
        }
        return result;
    }

    public synchronized GameSnapshot.BattleView view() {
        if (activeBattle == null) {
            return null;
        }
        return new GameSnapshot.BattleView(
                FINAL_BOSS_ID,
                FINAL_BOSS_NAME,
                activeBattle.enemyHp,
                BOSS_MAX_HP,
                activeBattle.phase,
                activeBattle.turn,
                activeBattle.playerGuard,
                activeBattle.rolledBonus,
                List.copyOf(activeBattle.traits),
                activeBattle.lastAction,
                activeBattle.message,
                "character.zuul_overlord",
                activeBattle.currentIntent.name(),
                intentLabel(activeBattle.currentIntent),
                intentDescription(activeBattle.currentIntent),
                intentAssetKey(activeBattle.currentIntent),
                activeBattle.d100Result,
                activeBattle.d100Step,
                activeBattle.bonusDieApplied,
                activeBattle.soulBellCooldown,
                hints()
        );
    }

    public synchronized BossSaveData saveData() {
        if (activeBattle == null) {
            return null;
        }
        BossSaveData data = new BossSaveData();
        data.setBossId(FINAL_BOSS_ID);
        data.setName(FINAL_BOSS_NAME);
        data.setHp(activeBattle.enemyHp);
        data.setMaxHp(BOSS_MAX_HP);
        data.setPhase("PHASE_" + activeBattle.phase);
        data.setTurn(activeBattle.turn);
        data.setCurrentIntent(activeBattle.currentIntent.name());
        data.setPlayerGuard(activeBattle.playerGuard);
        data.setRolledBonus(activeBattle.rolledBonus);
        data.setD100Result(activeBattle.d100Result);
        data.setD100Step(activeBattle.d100Step);
        data.setConsecutiveFailures(activeBattle.consecutiveFailures);
        data.setBonusDieActive(activeBattle.bonusDieApplied);
        data.setPenaltyDieActive(activeBattle.penaltyDieActive);
        data.setGuaranteedCriticalNextTurn(activeBattle.guaranteedCriticalNextTurn);
        data.setSoulBellCooldown(activeBattle.soulBellCooldown);
        data.setSoulBellUsed(activeBattle.soulBellUsed);
        data.setPendingCoreBlast(activeBattle.pendingCoreBlast);
        data.setLogs(List.of(activeBattle.message));
        return data;
    }

    public synchronized void restore(BossSaveData data) {
        if (data == null || data.getHp() <= 0) {
            activeBattle = null;
            return;
        }
        BattleState restored = new BattleState();
        restored.enemyHp = Math.min(BOSS_MAX_HP, data.getHp());
        restored.phase = parsePhase(data.getPhase());
        restored.turn = Math.max(1, data.getTurn());
        restored.currentIntent = parseIntent(data.getCurrentIntent());
        restored.playerGuard = Math.max(0, data.getPlayerGuard());
        restored.rolledBonus = Math.max(0, data.getRolledBonus());
        restored.d100Result = Math.max(0, data.getD100Result());
        restored.d100Step = Math.max(0, data.getD100Step());
        restored.consecutiveFailures = Math.max(0, data.getConsecutiveFailures());
        restored.bonusDieApplied = data.isBonusDieActive();
        restored.penaltyDieActive = data.isPenaltyDieActive();
        restored.guaranteedCriticalNextTurn = data.isGuaranteedCriticalNextTurn();
        restored.soulBellCooldown = Math.max(0, data.getSoulBellCooldown());
        restored.soulBellUsed = data.isSoulBellUsed();
        restored.pendingCoreBlast = data.isPendingCoreBlast();
        restored.message = data.getLogs().isEmpty() ? "祖尔霸主正在观察你的破绽。" : data.getLogs().get(data.getLogs().size() - 1);
        if (restored.phase == 2) {
            restored.traits.add("phase_2_uncertain_self");
            restored.traits.add("fate_core_overdrive");
        }
        activeBattle = restored;
    }

    int getD100Step(int d100, int skillValue) {
        if (d100 <= 5) {
            return 1;
        }
        if (d100 <= skillValue * 0.4) {
            return 2;
        }
        if (d100 <= skillValue * 0.7) {
            return 3;
        }
        if (d100 <= skillValue) {
            return 4;
        }
        if (d100 == 100) {
            return 6;
        }
        return 5;
    }

    private String attack(PlayerService playerService, WorldState worldState) {
        activeBattle.lastAction = "attack";
        BattleRoll roll = rollD100(playerService);
        return switch (activeBattle.currentIntent) {
            case HEAVY_STRIKE -> attackIntoHeavyStrike(roll, playerService, worldState);
            case FATE_DISTORTION -> attackIntoFateDistortion(roll, playerService, worldState);
            case CORE_CHARGE -> attackIntoCoreCharge(roll, playerService, worldState);
        };
    }

    private String defend(PlayerService playerService, WorldState worldState) {
        activeBattle.lastAction = "defend";
        return switch (activeBattle.currentIntent) {
            case HEAVY_STRIKE -> defendHeavyStrike(rollD100(playerService), playerService, worldState);
            case FATE_DISTORTION -> {
                DamageReport damage = damagePlayer(playerService, worldState, 8);
                yield completeTurn("你试图架起防线，但因果扭曲绕过了护势，灵魂承受 " + damage.text() + "。", worldState, true);
            }
            case CORE_CHARGE -> {
                activeBattle.pendingCoreBlast = true;
                yield completeTurn("你选择防御，但命运核心的毁灭蓄力无法被盾势削弱。", worldState, true);
            }
        };
    }

    private String roll(PlayerService playerService, WorldState worldState) {
        activeBattle.lastAction = "roll";
        return switch (activeBattle.currentIntent) {
            case HEAVY_STRIKE -> {
                BattleRoll roll = rollD100(playerService);
                if (roll.success()) {
                    activeBattle.rolledBonus = 8;
                    DamageReport damage = damagePlayer(playerService, worldState, 12);
                    yield completeTurn("你拨动命运骰避开锋线，下一次判定获得 +8，仍承受 " + damage.text() + "。", worldState, true);
                }
                DamageReport damage = damagePlayer(playerService, worldState, 18);
                yield completeTurn("骰面被巨剑威压震散，你未能找到破绽并承受 " + damage.text() + "。", worldState, true);
            }
            case FATE_DISTORTION -> rollAgainstFateDistortion(playerService, worldState);
            case CORE_CHARGE -> {
                BattleRoll roll = rollD100(playerService);
                if (roll.success()) {
                    activeBattle.rolledBonus = 10;
                }
                activeBattle.pendingCoreBlast = true;
                yield completeTurn("你掷出命运骰试图积累优势，但核心充能没有被打断。", worldState, true);
            }
        };
    }

    private String useSoulBell(PlayerService playerService, WorldState worldState) {
        activeBattle.lastAction = "use_soul_bell";
        if (!playerService.inventoryItems().contains(ItemService.SOUL_BELL)) {
            activeBattle.message = "你还没有灵魂之铃，无法撼动祖尔的意图。";
            return activeBattle.message;
        }
        if (activeBattle.soulBellUsed || activeBattle.soulBellCooldown > 0) {
            activeBattle.message = "灵魂之铃的余音尚未散去，本场战斗暂时不能再次敲响。";
            return activeBattle.message;
        }
        activeBattle.soulBellUsed = true;
        activeBattle.soulBellCooldown = 2;
        activeBattle.pendingCoreBlast = false;
        dealBossDamage(12);
        String defeated = finishIfBossDefeated(playerService, worldState, "灵魂之铃震碎当前意图，祖尔霸主的王座阴影被迫后退。");
        if (defeated != null) {
            return defeated;
        }
        return completeTurn("灵魂之铃震碎当前意图，并造成 12 点精神裂伤。", worldState, true);
    }

    private String flee() {
        activeBattle = null;
        return "你退回王座大厅边缘，祖尔霸主没有追击，只等待你再次选择。";
    }

    private String attackIntoHeavyStrike(BattleRoll roll, PlayerService playerService, WorldState worldState) {
        if (roll.critical()) {
            dealBossDamage(36);
            String defeated = finishIfBossDefeated(playerService, worldState, "你以大成功震开巨剑，祖尔霸主的重击被彻底打断。");
            if (defeated != null) {
                return defeated;
            }
            return completeTurn("你以大成功震开巨剑，造成 36 点真实伤害。", worldState, true);
        }
        DamageReport damage = damagePlayer(playerService, worldState, roll.catastrophe() ? 34 : 30);
        return completeTurn("你选择对攻，但蓄势重击拥有霸体，攻击被碾碎并承受 " + damage.text() + "。", worldState, true);
    }

    private String attackIntoFateDistortion(BattleRoll roll, PlayerService playerService, WorldState worldState) {
        int recoil = roll.critical() ? 36 : roll.success() ? 24 : 16;
        DamageReport damage = damagePlayer(playerService, worldState, recoil);
        return completeTurn("命运扭曲反转了剑锋，你的攻击没有伤到祖尔，反而回弹 " + damage.text() + "。", worldState, true);
    }

    private String attackIntoCoreCharge(BattleRoll roll, PlayerService playerService, WorldState worldState) {
        if (roll.hardSuccess()) {
            int damage = roll.critical() ? 52 : 38;
            dealBossDamage(damage);
            activeBattle.penaltyDieActive = true;
            String defeated = finishIfBossDefeated(playerService, worldState, "你击碎命运核心的蓄力环，祖尔霸主的毁灭技被打断。");
            if (defeated != null) {
                return defeated;
            }
            return completeTurn("你击碎命运核心的蓄力环，造成 " + damage + " 点伤害，Boss 下回合失衡。", worldState, true);
        }
        if (roll.success()) {
            dealBossDamage(18);
            activeBattle.pendingCoreBlast = true;
            String defeated = finishIfBossDefeated(playerService, worldState, "你的攻击擦过命运核心，但没有完全中断蓄力。");
            if (defeated != null) {
                return defeated;
            }
            return completeTurn("你造成 18 点伤害，但未能打断核心充能。", worldState, true);
        }
        activeBattle.pendingCoreBlast = true;
        return completeTurn("攻击落空，毁灭性的能量仍在核心内聚集。", worldState, true);
    }

    private String defendHeavyStrike(BattleRoll roll, PlayerService playerService, WorldState worldState) {
        if (roll.hardSuccess()) {
            dealBossDamage(10);
            activeBattle.penaltyDieActive = true;
            String defeated = finishIfBossDefeated(playerService, worldState, "你完成完美盾反，祖尔霸主失衡。");
            if (defeated != null) {
                return defeated;
            }
            return completeTurn("你完成完美盾反，无伤化解重击并造成 10 点反制伤害。", worldState, true);
        }
        if (roll.success()) {
            DamageReport damage = damagePlayer(playerService, worldState, 6);
            return completeTurn("你完成标准格挡，只承受 " + damage.text() + "。", worldState, true);
        }
        DamageReport damage = damagePlayer(playerService, worldState, roll.catastrophe() ? 30 : 21);
        return completeTurn("防线崩溃，巨剑震碎护势，你承受 " + damage.text() + "。", worldState, true);
    }

    private String rollAgainstFateDistortion(PlayerService playerService, WorldState worldState) {
        BattleRoll roll;
        if (playerService.inventoryItems().contains("blank_dice")) {
            int skill = skillValue(playerService);
            int forcedD100 = Math.max(6, (int) Math.floor(skill * 0.7));
            roll = forcedRoll(forcedD100, 3);
            activeBattle.rolledBonus = 15;
            dealBossDamage(16);
            String defeated = finishIfBossDefeated(playerService, worldState, "空白骰子覆写因果，命运扭曲被强制校正。");
            if (defeated != null) {
                return defeated;
            }
            return completeTurn("空白骰子自动触发，判定固定为困难成功，扭曲被驱散并积累 +15 命运加值。", worldState, true);
        }

        roll = rollD100(playerService);
        if (roll.success()) {
            activeBattle.rolledBonus = roll.hardSuccess() ? 14 : 10;
            dealBossDamage(12);
            String defeated = finishIfBossDefeated(playerService, worldState, "你拨正因果，未定倒影在王座边缘碎裂。");
            if (defeated != null) {
                return defeated;
            }
            return completeTurn("你拨正因果，扭曲状态被驱散，下一次判定获得命运加值。", worldState, true);
        }
        return completeTurn("你未能拨动命运的丝线，祖尔的因果扭曲仍在拖拽你的行动。", worldState, true);
    }

    private BattleRoll rollD100(PlayerService playerService) {
        int skill = skillValue(playerService);
        boolean bonus = activeBattle.consecutiveFailures >= 2 || activeBattle.penaltyDieActive;
        if (activeBattle.guaranteedCriticalNextTurn) {
            activeBattle.guaranteedCriticalNextTurn = false;
            activeBattle.rolledBonus = 0;
            activeBattle.d100Result = 1;
            activeBattle.d100Step = 1;
            activeBattle.bonusDieApplied = true;
            activeBattle.consecutiveFailures = 0;
            activeBattle.penaltyDieActive = false;
            return new BattleRoll(1, 1, skill, true);
        }

        skill = Math.min(MAX_SKILL, skill + activeBattle.rolledBonus);
        activeBattle.rolledBonus = 0;
        int d100 = random.nextInt(1, 101);
        if (bonus) {
            d100 = Math.min(d100, random.nextInt(1, 101));
        }
        int step = getD100Step(d100, skill);
        activeBattle.d100Result = d100;
        activeBattle.d100Step = step;
        activeBattle.bonusDieApplied = bonus;
        activeBattle.penaltyDieActive = false;
        updateFailureCount(step);
        return new BattleRoll(d100, step, skill, bonus);
    }

    private BattleRoll forcedRoll(int d100, int step) {
        activeBattle.d100Result = d100;
        activeBattle.d100Step = step;
        activeBattle.bonusDieApplied = false;
        activeBattle.consecutiveFailures = 0;
        activeBattle.penaltyDieActive = false;
        return new BattleRoll(d100, step, skillValueForView(), false);
    }

    private int skillValue(PlayerService playerService) {
        int value = BASE_SKILL;
        if (playerService.inventoryItems().contains("nameless_badge")) {
            value += NAMELESS_BADGE_BONUS;
        }
        return Math.min(MAX_SKILL, value);
    }

    private int skillValueForView() {
        return BASE_SKILL;
    }

    private void updateFailureCount(int step) {
        if (step <= 4) {
            activeBattle.consecutiveFailures = 0;
        } else {
            activeBattle.consecutiveFailures++;
        }
    }

    private String releaseCoreBlastIfNeeded(PlayerService playerService, WorldState worldState) {
        if (!activeBattle.pendingCoreBlast) {
            return "";
        }
        activeBattle.pendingCoreBlast = false;
        DamageReport damage = damagePlayer(playerService, worldState, 45);
        return "命运核心释放全屏毁灭冲击，你承受 " + damage.text() + "。";
    }

    private DamageReport damagePlayer(PlayerService playerService, WorldState worldState, int amount) {
        if (amount <= 0) {
            return new DamageReport(0, false);
        }
        if (playerService.hp() <= amount && playerService.inventoryItems().contains("savebreaker_key")
                && playerService.consumeItem("savebreaker_key")) {
            int actual = Math.max(0, playerService.hp() - 1);
            playerService.damage(actual);
            activeBattle.guaranteedCriticalNextTurn = true;
            return new DamageReport(actual, true);
        }
        playerService.damage(amount);
        if (playerService.hp() <= 0) {
            worldState.setBoolean("final_boss_lost", true);
        }
        return new DamageReport(amount, false);
    }

    private void dealBossDamage(int damage) {
        activeBattle.enemyHp = Math.max(0, activeBattle.enemyHp - Math.max(0, damage));
    }

    private String finishIfBossDefeated(PlayerService playerService, WorldState worldState, String lead) {
        if (activeBattle.enemyHp > 0) {
            return null;
        }
        worldState.setBoolean("final_boss_defeated", true);
        worldState.setBoolean("event_completed.final_boss", true);
        playerService.gainItem("throne_fragment");
        String message = lead + " Zuul Overlord 崩解，王座碎片落入你的背包。";
        activeBattle = null;
        return message;
    }

    private String completeTurn(String message, WorldState worldState, boolean advanceIntent) {
        boolean phaseChanged = enterPhaseTwoIfNeeded(worldState);
        if (activeBattle.soulBellCooldown > 0) {
            activeBattle.soulBellCooldown--;
        }
        if (advanceIntent) {
            advanceIntent();
        }
        activeBattle.turn++;
        activeBattle.message = message + (phaseChanged ? " Boss 进入 Phase 2，未定倒影覆盖了王座。" : "");
        return activeBattle.message;
    }

    private boolean enterPhaseTwoIfNeeded(WorldState worldState) {
        if (activeBattle.phase == 2 || activeBattle.enemyHp > PHASE_TWO_THRESHOLD) {
            return false;
        }
        activeBattle.phase = 2;
        if (!activeBattle.traits.contains("phase_2_uncertain_self")) {
            activeBattle.traits.add("phase_2_uncertain_self");
        }
        if (!activeBattle.traits.contains("fate_core_overdrive")) {
            activeBattle.traits.add("fate_core_overdrive");
        }
        worldState.setBoolean("boss_phase_2", true);
        return true;
    }

    private void advanceIntent() {
        activeBattle.currentIntent = switch (activeBattle.currentIntent) {
            case HEAVY_STRIKE -> BossIntent.FATE_DISTORTION;
            case FATE_DISTORTION -> BossIntent.CORE_CHARGE;
            case CORE_CHARGE -> BossIntent.HEAVY_STRIKE;
        };
    }

    private List<String> hints() {
        if (activeBattle == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        result.add("D100: " + stepLabel(activeBattle.d100Step));
        if (activeBattle.bonusDieApplied) {
            result.add("奖励骰已触发");
        }
        if (activeBattle.pendingCoreBlast) {
            result.add("下回合开始将释放毁灭冲击");
        }
        if (activeBattle.guaranteedCriticalNextTurn) {
            result.add("下一次判定必定大成功");
        }
        if (activeBattle.soulBellUsed) {
            result.add("灵魂之铃已在本战使用");
        }
        return result;
    }

    private String stepLabel(int step) {
        return switch (step) {
            case 1 -> "大成功";
            case 2 -> "极难成功";
            case 3 -> "困难成功";
            case 4 -> "常规成功";
            case 5 -> "失败";
            case 6 -> "大失败";
            default -> "尚未掷骰";
        };
    }

    private String intentLabel(BossIntent intent) {
        return switch (intent) {
            case HEAVY_STRIKE -> "蓄势重击";
            case FATE_DISTORTION -> "命运扭曲";
            case CORE_CHARGE -> "核心充能";
        };
    }

    private String intentDescription(BossIntent intent) {
        return switch (intent) {
            case HEAVY_STRIKE -> "巨剑举起，最优反制是防御或盾反。";
            case FATE_DISTORTION -> "因果正在反转，直接攻击会反弹伤害。";
            case CORE_CHARGE -> "毁灭性能量汇聚，必须用困难成功以上的攻击打断。";
        };
    }

    private String intentAssetKey(BossIntent intent) {
        return switch (intent) {
            case HEAVY_STRIKE -> "battle.intent.heavy";
            case FATE_DISTORTION -> "battle.intent.fate";
            case CORE_CHARGE -> "battle.intent.core";
        };
    }

    private int parsePhase(String phase) {
        if ("PHASE_2".equalsIgnoreCase(phase) || "2".equals(phase)) {
            return 2;
        }
        return 1;
    }

    private BossIntent parseIntent(String intent) {
        try {
            return BossIntent.valueOf(intent == null ? "" : intent);
        } catch (IllegalArgumentException ex) {
            return BossIntent.HEAVY_STRIKE;
        }
    }

    private record BattleRoll(int d100, int step, int skill, boolean bonusDie) {
        boolean critical() {
            return step == 1;
        }

        boolean hardSuccess() {
            return step <= 3;
        }

        boolean success() {
            return step <= 4;
        }

        boolean catastrophe() {
            return step == 6;
        }
    }

    private record DamageReport(int amount, boolean savedByKey) {
        String text() {
            return savedByKey ? amount + " 点伤害，断档之钥破碎并将你锁在 1 点生命" : amount + " 点伤害";
        }
    }

    private static final class BattleState {
        private int enemyHp = BOSS_MAX_HP;
        private int phase = 1;
        private int turn = 1;
        private int playerGuard = 0;
        private int rolledBonus = 0;
        private int d100Result = 0;
        private int d100Step = 0;
        private int consecutiveFailures = 0;
        private boolean bonusDieApplied = false;
        private boolean penaltyDieActive = false;
        private boolean guaranteedCriticalNextTurn = false;
        private int soulBellCooldown = 0;
        private boolean soulBellUsed = false;
        private boolean pendingCoreBlast = false;
        private BossIntent currentIntent = BossIntent.HEAVY_STRIKE;
        private String lastAction = "";
        private String message = "祖尔霸主正在观察你的破绽。";
        private final List<String> traits = new ArrayList<>(List.of(
                "black_armor",
                "mirror_counter",
                "dice_distortion"
        ));
    }
}
