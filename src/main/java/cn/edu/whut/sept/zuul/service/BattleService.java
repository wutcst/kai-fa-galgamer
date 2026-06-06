package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class BattleService {

    private static final String FINAL_BOSS_ID = "zuul_overlord";
    private static final String FINAL_BOSS_NAME = "Zuul Overlord";
    private static final int BOSS_MAX_HP = 120;
    private static final int PHASE_TWO_THRESHOLD = BOSS_MAX_HP / 2;

    private final Random random = new Random();
    private BattleState activeBattle;

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
            activeBattle = null;
            worldState.setBoolean("final_boss_lost", true);
            return "你倒在王座前。循环尚未终结。";
        }

        String normalized = action == null ? "attack" : action.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "attack" -> attack(playerService, worldState);
            case "defend" -> defend(playerService);
            case "roll" -> roll(playerService, worldState);
            case "use_soul_bell" -> useSoulBell(playerService, worldState);
            case "flee" -> flee();
            default -> "Boss 战动作无效：" + action;
        };
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
                "character.zuul_overlord"
        );
    }

    private String attack(PlayerService playerService, WorldState worldState) {
        int damage = 22 + activeBattle.rolledBonus;
        if (playerService.inventoryItems().contains(ItemService.SOUL_BELL)) {
            damage += 4;
        }
        activeBattle.rolledBonus = 0;
        activeBattle.lastAction = "attack";
        return resolveExchange(damage, playerService, worldState, "你斩向祖尔霸主，命运核心随剑痕震颤。");
    }

    private String defend(PlayerService playerService) {
        activeBattle.playerGuard = activeBattle.phase == 1 ? 10 : 14;
        activeBattle.lastAction = "defend";
        int incoming = incomingDamage(playerService);
        playerService.damage(incoming);
        activeBattle.turn++;
        activeBattle.message = "你举起防御姿态，抵消了大部分反击，承受 " + incoming + " 点伤害。";
        return activeBattle.message;
    }

    private String roll(PlayerService playerService, WorldState worldState) {
        int roll = random.nextInt(1, 7);
        activeBattle.rolledBonus = roll + (activeBattle.phase == 2 ? 3 : 0);
        activeBattle.lastAction = "roll";
        int chipDamage = 8 + activeBattle.rolledBonus;
        return resolveExchange(chipDamage, playerService, worldState,
                "空白骰在掌心停下，下一击获得 +" + activeBattle.rolledBonus + " 命运加值。");
    }

    private String useSoulBell(PlayerService playerService, WorldState worldState) {
        activeBattle.lastAction = "use_soul_bell";
        if (!playerService.inventoryItems().contains(ItemService.SOUL_BELL)) {
            activeBattle.message = "你还没有灵魂之铃，无法撼动祖尔的第二层护影。";
            return activeBattle.message;
        }
        activeBattle.playerGuard = 8;
        return resolveExchange(30, playerService, worldState, "灵魂之铃响起，王座阴影被迫显露裂隙。");
    }

    private String flee() {
        activeBattle = null;
        return "你退回王座大厅边缘，祖尔霸主没有追击，只等待你再次选择。";
    }

    private String resolveExchange(int damage, PlayerService playerService, WorldState worldState, String lead) {
        activeBattle.enemyHp = Math.max(0, activeBattle.enemyHp - damage);
        if (activeBattle.enemyHp <= 0) {
            worldState.setBoolean("final_boss_defeated", true);
            worldState.setBoolean("event_completed.final_boss", true);
            playerService.gainItem("throne_fragment");
            activeBattle.message = lead + " Zuul Overlord 崩解，王座碎片落入你的背包。";
            activeBattle = null;
            return lead + " 你击败了 Zuul Overlord，并获得 throne_fragment。";
        }

        boolean phaseChanged = enterPhaseTwoIfNeeded(worldState);
        int incoming = incomingDamage(playerService);
        playerService.damage(incoming);
        activeBattle.turn++;
        String phaseText = phaseChanged ? " Boss 进入 Phase 2，未定倒影覆盖了王座。" : "";
        activeBattle.message = lead + " 造成 " + damage + " 点伤害。" + phaseText
                + " 祖尔反击，你承受 " + incoming + " 点伤害。";
        if (playerService.hp() <= 0) {
            worldState.setBoolean("final_boss_lost", true);
            activeBattle.message += " 你倒在王座前，循环开始回卷。";
        }
        return activeBattle.message;
    }

    private boolean enterPhaseTwoIfNeeded(WorldState worldState) {
        if (activeBattle.phase == 2 || activeBattle.enemyHp > PHASE_TWO_THRESHOLD) {
            return false;
        }
        activeBattle.phase = 2;
        activeBattle.traits.add("phase_2_uncertain_self");
        activeBattle.traits.add("fate_core_overdrive");
        worldState.setBoolean("boss_phase_2", true);
        return true;
    }

    private int incomingDamage(PlayerService playerService) {
        int base = activeBattle.phase == 1 ? 10 : 14;
        if (playerService.inventoryItems().contains(ItemService.SOUL_BELL)) {
            base -= 2;
        }
        int reduced = Math.max(1, base - activeBattle.playerGuard);
        activeBattle.playerGuard = 0;
        return reduced;
    }

    private static final class BattleState {
        private int enemyHp = BOSS_MAX_HP;
        private int phase = 1;
        private int turn = 1;
        private int playerGuard = 0;
        private int rolledBonus = 0;
        private String lastAction = "";
        private String message = "祖尔霸主正在观察你的破绽。";
        private final List<String> traits = new ArrayList<>(List.of(
                "black_armor",
                "mirror_counter",
                "dice_distortion"
        ));
    }
}
