package cn.edu.whut.sept.zuul.event;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;

public interface GameEvent {

    String id();

    EventResult trigger(PlayerService playerService, WorldState worldState);
}
