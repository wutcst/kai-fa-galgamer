package cn.edu.whut.sept.zuul.ending;

import cn.edu.whut.sept.zuul.model.GameActionOption;
import cn.edu.whut.sept.zuul.save.EndingSaveData;
import cn.edu.whut.sept.zuul.save.ProfileState;
import cn.edu.whut.sept.zuul.save.SaveService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class EndingService {

    private static final Set<String> KEY_ITEMS = Set.of(
            "blank_dice",
            "savebreaker_key",
            "nameless_badge",
            "pure_seed",
            "throne_fragment"
    );

    private final SaveService saveService;
    private EndingState activeEnding;

    public EndingService(SaveService saveService) {
        this.saveService = saveService;
    }

    public synchronized EndingState prepareFinalChoice(PlayerService playerService) {
        int count = keyItemCount(playerService);
        activeEnding = new EndingState(
                "final_choice",
                "最终选择",
                "命运核心裂开了 " + count + " 道光。你要继承循环，封存循环，还是谱写新章？",
                "ending.inherited_fate",
                saveService.loadProfile().isCreatorModeUnlocked(),
                choices(count)
        );
        return activeEnding;
    }

    public synchronized EndingState resolve(String choice, PlayerService playerService, WorldState worldState) {
        int count = keyItemCount(playerService);
        String normalized = choice == null || choice.isBlank() ? "seal_loop" : choice.trim().toLowerCase();
        boolean trueEnding = "write_own_chapter".equals(normalized) && count >= KEY_ITEMS.size();
        if (trueEnding) {
            worldState.setBoolean("cycle_broken", true);
            worldState.setBoolean("creator_mode_unlocked", true);
            ProfileState profile = saveService.loadProfile();
            profile.setCycleBroken(true);
            profile.setCreatorModeUnlocked(true);
            profile.getCompletedEndings().add("true_fate");
            saveService.saveProfile(profile);
            activeEnding = new EndingState(
                    "true_fate",
                    "谱写新章",
                    "你没有登上王座，而是终止循环。空白章节在命运核心后展开，Creator Mode 已解锁。",
                    "ending.write_own_chapter",
                    true,
                    List.of(new GameActionOption("CREATOR_LIST", "进入 Creator Mode", "", false),
                            new GameActionOption("NEW_GAME", "返回主菜单", "", false))
            );
            return activeEnding;
        }
        String endingId = count >= 3 ? "sealed_fate" : "inherited_fate";
        activeEnding = new EndingState(
                endingId,
                count >= 3 ? "封存循环" : "继承余烬",
                count >= 3
                        ? "你以残缺关键物品封存了王座，世界暂时获得安宁，但空白章节仍未出现。"
                        : "关键物品不足，命运核心将你推回既定循环。",
                "ending.inherited_fate",
                saveService.loadProfile().isCreatorModeUnlocked(),
                List.of(new GameActionOption("NEW_GAME", "返回主菜单", "", false))
        );
        ProfileState profile = saveService.loadProfile();
        profile.getCompletedEndings().add(endingId);
        saveService.saveProfile(profile);
        return activeEnding;
    }

    public synchronized EndingState activeEnding() {
        return activeEnding;
    }

    public synchronized EndingSaveData saveData() {
        if (activeEnding == null) {
            return null;
        }
        EndingSaveData data = new EndingSaveData();
        data.setEndingId(activeEnding.endingId());
        data.setTitle(activeEnding.title());
        data.setText(activeEnding.text());
        data.setAssetKey(activeEnding.assetKey());
        data.setCreatorModeUnlocked(activeEnding.creatorModeUnlocked());
        return data;
    }

    public synchronized void restore(EndingSaveData data) {
        if (data == null || data.getEndingId() == null) {
            activeEnding = null;
            return;
        }
        activeEnding = new EndingState(
                data.getEndingId(),
                data.getTitle(),
                data.getText(),
                data.getAssetKey(),
                data.isCreatorModeUnlocked(),
                data.isCreatorModeUnlocked()
                        ? List.of(new GameActionOption("CREATOR_LIST", "进入 Creator Mode", "", false),
                        new GameActionOption("NEW_GAME", "返回主菜单", "", false))
                        : List.of(new GameActionOption("NEW_GAME", "返回主菜单", "", false))
        );
    }

    public synchronized void clear() {
        activeEnding = null;
    }

    public boolean creatorModeUnlocked() {
        return saveService.loadProfile().isCreatorModeUnlocked();
    }

    private int keyItemCount(PlayerService playerService) {
        return (int) playerService.inventoryItems().stream().filter(KEY_ITEMS::contains).count();
    }

    private List<GameActionOption> choices(int count) {
        if (count >= KEY_ITEMS.size()) {
            return List.of(
                    new GameActionOption("FINAL_CHOICE", "谱写新章", "write_own_chapter", false),
                    new GameActionOption("FINAL_CHOICE", "封存循环", "seal_loop", false),
                    new GameActionOption("FINAL_CHOICE", "继承王座", "inherit_throne", false)
            );
        }
        return List.of(
                new GameActionOption("FINAL_CHOICE", count >= 3 ? "封存循环" : "继承王座", count >= 3 ? "seal_loop" : "inherit_throne", false)
        );
    }

    public record EndingState(
            String endingId,
            String title,
            String text,
            String assetKey,
            boolean creatorModeUnlocked,
            List<GameActionOption> actions
    ) {
    }
}
