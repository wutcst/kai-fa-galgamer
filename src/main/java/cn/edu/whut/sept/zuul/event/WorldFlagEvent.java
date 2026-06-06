package cn.edu.whut.sept.zuul.event;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;

import java.util.List;

public class WorldFlagEvent implements GameEvent {

    private final String id;
    private final String message;
    private final List<String> itemIds;
    private final List<String> flagIds;

    public WorldFlagEvent(String id, String message, List<String> itemIds, List<String> flagIds) {
        this.id = id;
        this.message = message;
        this.itemIds = List.copyOf(itemIds);
        this.flagIds = List.copyOf(flagIds);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public EventResult trigger(PlayerService playerService, WorldState worldState) {
        itemIds.forEach(playerService::gainItem);
        flagIds.forEach(flagId -> worldState.setBoolean(flagId, true));
        return EventResult.message(message);
    }
}
