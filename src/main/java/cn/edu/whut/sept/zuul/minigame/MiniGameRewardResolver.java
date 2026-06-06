package cn.edu.whut.sept.zuul.minigame;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MiniGameRewardResolver {

    public MiniGameResult preview(String eventId, String gameId, MiniGameResultType type, int score, Map<String, Object> details) {
        Reward reward = rewardFor(eventId, type);
        return new MiniGameResult(type, score, reward.message(), reward.items(), reward.flags(), details);
    }

    public void apply(String eventId, MiniGameResult result, PlayerService playerService, WorldState worldState) {
        result.rewardItems().forEach(playerService::gainItem);
        result.flags().forEach(worldState::setBoolean);
        worldState.setBoolean("event_completed." + eventId, true);
    }

    private Reward rewardFor(String eventId, MiniGameResultType type) {
        if (type == MiniGameResultType.CANCELLED) {
            return new Reward("小游戏已取消，事件仍未完成。", List.of(), Map.of());
        }
        if (type == MiniGameResultType.FAILURE) {
            return new Reward("小游戏失败，命运暂时没有给出奖励。", List.of(), Map.of());
        }

        List<String> items = new ArrayList<>();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        String message;
        switch (eventId) {
            case "mirror_room_event" -> {
                flags.put("mirror_success", true);
                if (type == MiniGameResultType.GREAT_SUCCESS) {
                    items.add("savebreaker_key");
                    message = "点数完美抵达二十一，断档之钥从镜面裂隙中落下。";
                } else {
                    items.add("mirror_shard");
                    message = "镜面承认了你的点数，镜面碎片凝结成形。";
                }
            }
            case "order_altar_event" -> {
                flags.put("order_shard", true);
                flags.put("altar_purified", true);
                if (type == MiniGameResultType.GREAT_SUCCESS) {
                    items.add("blank_dice");
                    message = "双骰掷出命运的高位，空白骰子回应了祭坛。";
                } else {
                    items.add("purified_ash");
                    message = "祭坛恢复秩序，净化灰烬留在石盘中央。";
                }
            }
            case "garden_event" -> {
                flags.put("soul_shard", true);
                flags.put("garden_restored", true);
                if (type == MiniGameResultType.GREAT_SUCCESS) {
                    items.add("pure_seed");
                    message = "所有符文被连通，纯净种子在灵魂花园中苏醒。";
                } else {
                    items.add("soul_flower");
                    message = "灵魂花园短暂复苏，灵魂花回应了你的连线。";
                }
            }
            default -> message = "小游戏完成，世界记录了这次结果。";
        }
        return new Reward(message, items, flags);
    }

    private record Reward(String message, List<String> items, Map<String, Boolean> flags) {
    }
}
