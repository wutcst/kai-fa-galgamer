package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EndingService {

    private static final List<String> KEY_ITEMS = List.of(
            "blank_dice",
            "savebreaker_key",
            "nameless_badge",
            "pure_seed",
            "throne_fragment"
    );

    private static final List<EndingChoice> CHOICES = List.of(
            new EndingChoice("sit_on_throne", "坐上王座", "继承旧循环，成为下一位王座阴影。"),
            new EndingChoice("question_core", "质问命运核心", "用第一件反循环关键道具向核心提出质疑。"),
            new EndingChoice("refuse_throne", "拒绝王座", "拒绝祖尔赋予挑战者的名字与位置。"),
            new EndingChoice("break_core", "击碎核心外壳", "让循环第一次留下不可逆裂痕。"),
            new EndingChoice("call_uncertain_self", "呼唤未定的自己", "面对循环深处披着失败记忆的倒影。"),
            new EndingChoice("write_own_chapter", "书写自己的篇章", "集齐五件反循环关键道具，亲手终结未定命运。")
    );

    private GameSnapshot.EndingView selectedEnding;

    public synchronized void reset() {
        selectedEnding = null;
    }

    public synchronized GameSnapshot.EndingView endingView() {
        return selectedEnding;
    }

    public synchronized List<GameSnapshot.ChoiceView> availableChoices(PlayerService playerService, WorldState worldState) {
        if (!worldState.getBoolean("final_boss_defeated") || selectedEnding != null) {
            return List.of();
        }

        int unlockedCount = Math.min(KEY_ITEMS.size(), countKeyItems(playerService));
        List<GameSnapshot.ChoiceView> views = new ArrayList<>();
        for (int i = 0; i <= unlockedCount && i < CHOICES.size(); i++) {
            EndingChoice choice = CHOICES.get(i);
            views.add(new GameSnapshot.ChoiceView(i + 1, choice.id(), choice.label(), choice.description(), true));
        }
        return views;
    }

    public synchronized String choose(String choiceId, PlayerService playerService, WorldState worldState) {
        if (!worldState.getBoolean("final_boss_defeated")) {
            return "命运核心尚未显露。请先击败 Zuul Overlord。";
        }
        if (selectedEnding != null) {
            return "结局已经确定：" + selectedEnding.title();
        }

        List<GameSnapshot.ChoiceView> available = availableChoices(playerService, worldState);
        String normalized = choiceId == null || choiceId.isBlank()
                ? available.get(available.size() - 1).id()
                : choiceId.trim().toLowerCase(Locale.ROOT);

        List<String> unlockedIds = available.stream()
                .map(GameSnapshot.ChoiceView::id)
                .toList();
        if (!unlockedIds.contains(normalized)) {
            return "该结局选项尚未解锁。当前反循环关键道具数量：" + countKeyItems(playerService) + " / " + KEY_ITEMS.size();
        }

        selectedEnding = resolve(normalized, worldState);
        return selectedEnding.title() + " 已达成。";
    }

    public int countKeyItems(PlayerService playerService) {
        int count = 0;
        List<String> inventory = playerService.inventoryItems();
        for (String itemId : KEY_ITEMS) {
            if (inventory.contains(itemId)) {
                count++;
            }
        }
        return count;
    }

    private GameSnapshot.EndingView resolve(String choiceId, WorldState worldState) {
        return switch (choiceId) {
            case "sit_on_throne" -> {
                worldState.setBoolean("cycle_inherited", true);
                yield ending(choiceId, "王座之命", "你坐上王座。下一位挑战者的脚步已经在命运大厅响起，而黑雾将你的双手锁在扶手上。循环仍在继续。", "ending.sit_on_throne");
            }
            case "question_core" -> {
                worldState.setBoolean("uncertain_self_defeated", true);
                yield ending(choiceId, "抗命之章", "你质问命运核心，第一次怀疑成为砸向旧规则的楔子。循环尚未终结，但它已经受伤。", "ending.question_core");
            }
            case "refuse_throne" -> {
                worldState.setBoolean("uncertain_self_defeated", true);
                yield ending(choiceId, "无名之命", "你拒绝王座，也拒绝它赋予你的名字。命运核心失去了下一任统治者。", "ending.refuse_throne");
            }
            case "break_core" -> {
                worldState.setBoolean("uncertain_self_defeated", true);
                worldState.setBoolean("anti_cycle_authority", true);
                yield ending(choiceId, "破碎之命", "你击碎核心外壳。旧规则再也无法完整愈合，命运第一次留下了不可逆的裂痕。", "ending.break_core");
            }
            case "call_uncertain_self" -> {
                worldState.setBoolean("uncertain_self_defeated", true);
                worldState.setBoolean("anti_cycle_authority", true);
                yield ending(choiceId, "真实之命", "你唤来循环深处另一个自己，并拒绝再一次成为它。倒影散去，核心终于沉默。", "ending.call_uncertain_self");
            }
            case "write_own_chapter" -> {
                worldState.setBoolean("uncertain_self_defeated", true);
                worldState.setBoolean("anti_cycle_authority", true);
                worldState.setBoolean("cycle_broken", true);
                worldState.setBoolean("creator_mode_unlocked", true);
                yield ending(choiceId, "书写自己的篇章", "骰子停止转动，镜子停止模仿，无名书在图书馆中逐一合拢。祖尔不需要新的王，它需要一个愿意终结循环的人。", "ending.write_own_chapter");
            }
            default -> throw new IllegalArgumentException("未知结局选项：" + choiceId);
        };
    }

    private GameSnapshot.EndingView ending(String id, String title, String description, String assetKey) {
        return new GameSnapshot.EndingView(id, title, description, assetKey);
    }

    private record EndingChoice(String id, String label, String description) {
    }
}
