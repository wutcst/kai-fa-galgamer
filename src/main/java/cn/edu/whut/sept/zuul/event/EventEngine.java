package cn.edu.whut.sept.zuul.event;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EventEngine {

    private final Map<String, GameEvent> events = new HashMap<>();

    public EventEngine() {
        registerDefaults();
    }

    public void register(GameEvent event) {
        if (events.containsKey(event.id())) {
            throw new IllegalArgumentException("重复事件 ID：" + event.id());
        }
        events.put(event.id(), event);
    }

    public EventResult trigger(String eventId, PlayerService playerService, WorldState worldState) {
        if (eventId == null || eventId.isBlank()) {
            return EventResult.none();
        }
        String completedFlag = "event_completed." + eventId;
        if (worldState.getBoolean(completedFlag)) {
            return EventResult.message("事件已完成：" + eventId);
        }
        GameEvent event = events.get(eventId);
        if (event == null) {
            return EventResult.none();
        }
        EventResult result = event.trigger(playerService, worldState);
        worldState.setBoolean(completedFlag, true);
        return result;
    }

    private void registerDefaults() {
        register(new WorldFlagEvent(
                "library_clue_event",
                "你记录禁书残页：命运以六开始，镜面只接受恰好的二十一。",
                java.util.List.of("library_note", "rune_clue"),
                java.util.List.of()
        ));
        register(new WorldFlagEvent(
                "broken_shelf_event",
                "破损书架吐出旧挑战者的真相：祖尔也许只是失败循环中的另一个自己。",
                java.util.List.of("blank_dice", "boss_truth"),
                java.util.List.of("memory_shard")
        ));
        register(new WorldFlagEvent(
                "mirror_room_event",
                "镜面裂隙凝成 mirror_shard，倒影的规则被削弱。",
                java.util.List.of("mirror_shard", "savebreaker_key"),
                java.util.List.of("mirror_success", "boss_mirror_weakened")
        ));
        register(new WorldFlagEvent(
                "order_altar_event",
                "秩序祭坛承认了正确序列，Order Shard 与 purified_ash 显现。",
                java.util.List.of("purified_ash"),
                java.util.List.of("order_shard", "altar_purified")
        ));
        register(new WorldFlagEvent(
                "garden_event",
                "灵魂花园恢复短暂呼吸，Soul Shard 与 pure_seed 回应了你。",
                java.util.List.of("pure_seed", "soul_flower"),
                java.util.List.of("soul_shard", "garden_restored")
        ));
        register(new WorldFlagEvent(
                "material_storage_event",
                "你从旧木箱中取出 broken_bell 与 silver_thread。",
                java.util.List.of("broken_bell", "silver_thread"),
                java.util.List.of()
        ));
        register(new WorldFlagEvent(
                "whisper_pool_event",
                "水池低语配方：形体、灵魂与束缚缺一不可。",
                java.util.List.of("soul_flower"),
                java.util.List.of("formula_known")
        ));
    }
}
