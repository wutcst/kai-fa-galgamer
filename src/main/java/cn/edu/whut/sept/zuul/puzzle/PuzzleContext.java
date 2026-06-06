package cn.edu.whut.sept.zuul.puzzle;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;

public record PuzzleContext(PlayerService playerService, WorldState worldState) {
}
