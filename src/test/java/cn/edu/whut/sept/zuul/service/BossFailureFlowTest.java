package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.event.EventEngine;
import cn.edu.whut.sept.zuul.minigame.MiniGameRewardResolver;
import cn.edu.whut.sept.zuul.minigame.MiniGameService;
import cn.edu.whut.sept.zuul.model.GameActionRequest;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.puzzle.PuzzleConfig;
import cn.edu.whut.sept.zuul.puzzle.PuzzleEngine;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BossFailureFlowTest {

    @Test
    void bossDefeatReturnsGameOverAndAcknowledgementReturnsMenu() {
        ItemService itemService = new ItemService();
        PlayerService playerService = new PlayerService(itemService);
        RoomService service = new RoomService(
                playerService,
                new WorldState(),
                new PuzzleEngine(new PuzzleConfig().puzzleRegistry()),
                new EventEngine(),
                new MiniGameService(new MiniGameRewardResolver()),
                new BattleService(new SequenceRandom(90)),
                new EndingService()
        );

        service.initGame();
        playerService.gainItem(ItemService.SOUL_BELL);
        act(service, "MOVE", "west", null);
        act(service, "MOVE", "south", null);
        act(service, "ANSWER", "triple_seal_gate", "open");
        act(service, "MOVE", "south", null);
        act(service, "START_BATTLE", "zuul_overlord", null);
        playerService.damage(99);

        GameSnapshot failed = act(service, "BATTLE_ACTION", "attack", "attack");

        assertEquals("GAME_OVER", failed.gamePhase().name());
        assertNull(failed.battle());
        assertNotNull(failed.failure());
        assertEquals("ending.boss_failure", failed.failure().assetKey());

        GameSnapshot menu = act(service, "ACK_GAME_OVER", "", null);

        assertEquals("MAIN_MENU", menu.gamePhase().name());
    }

    private GameSnapshot act(RoomService service, String actionType, String target, String value) {
        return service.perform(new GameActionRequest(actionType, target, value));
    }

    private static class SequenceRandom extends Random {
        private final int value;

        SequenceRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int origin, int bound) {
            return Math.max(origin, Math.min(bound - 1, value));
        }
    }
}
