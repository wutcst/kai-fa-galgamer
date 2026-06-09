package cn.edu.whut.sept.zuul.minigame;

import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniGameServiceTest {

    @Test
    void diceRollFinishesSessionAndCreatesOutcome() {
        MiniGameService service = service(5, 5);

        service.start("dice_check", "order_altar_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "roll", Map.of());

        assertFalse(service.hasActiveMiniGame());
        PlayerService playerService = new PlayerService(new ItemService());
        assertNotNull(service.outcomeView(playerService));
        assertEquals("GREAT_SUCCESS", service.outcomeView(playerService).resultType());
    }

    @Test
    void pointGameBustsImmediatelyOnDraw() {
        MiniGameService service = service(9, 9, 9);

        service.start("point_game", "mirror_room_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());

        PlayerService playerService = new PlayerService(new ItemService());
        assertNotNull(service.outcomeView(playerService));
        assertEquals("FAILURE", service.outcomeView(playerService).resultType());
        assertEquals(1, service.outcomeView(playerService).rescueOptions().size());
    }

    @Test
    void linkMatchStartsWithPairedPlayableBoard() {
        MiniGameService service = service(3, 1, 0, 2, 7, 5);

        service.start("link_match", "garden_event");

        String[][] board = boardFromView(service);
        Map<String, Integer> counts = new HashMap<>();
        int tileCount = 0;
        for (String[] row : board) {
            for (String symbol : row) {
                if (symbol != null) {
                    counts.merge(symbol, 1, Integer::sum);
                    tileCount++;
                }
            }
        }

        assertEquals(16, tileCount);
        assertTrue(counts.values().stream().allMatch(count -> count % 2 == 0));
        assertTrue(new LinkMatchPathFinder().hasAvailableMatch(board));
    }

    @Test
    void linkMatchClearsMatchingTilesThroughBackendPathCheck() {
        MiniGameService service = service();

        service.start("link_match", "garden_event");
        String sessionId = service.view().sessionId();
        int[] match = availableMatch(boardFromView(service));
        String response = service.handleInput(sessionId, "match", Map.of(
                "rowA", match[0],
                "colA", match[1],
                "rowB", match[2],
                "colB", match[3]
        ));

        assertEquals("连线成功。", response);
        assertEquals(1, service.view().state().get("matchedPairs"));
    }

    @Test
    void acknowledgeAppliesRewardsAndClearsState() {
        MiniGameService service = service(5, 5);
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        service.start("dice_check", "order_altar_event");
        service.handleInput(service.view().sessionId(), "roll", Map.of());
        service.acknowledge(playerService, worldState);

        assertFalse(service.hasActiveMiniGame());
        assertFalse(service.hasPendingOutcome());
        assertTrue(playerService.inventoryItems().contains("blank_dice"));
        assertTrue(worldState.getBoolean("event_completed.order_altar_event"));
    }

    @Test
    void relaxedPoint21AcceptsSeventeenOrBetter() {
        MiniGameService service = service(8, 9);

        service.start("point_game", "mirror_room_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "stand", Map.of());

        PlayerService playerService = new PlayerService(new ItemService());
        assertEquals("SUCCESS", service.outcomeView(playerService).resultType());
    }

    @Test
    void mirrorFailureCanBeRescuedByHpCost() {
        MiniGameService service = service(9, 9, 9);
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();

        service.start("point_game", "mirror_room_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());
        service.handleInput(sessionId, "draw", Map.of());
        String message = service.rescuePendingOutcome(sessionId, "mirror_break_hp", playerService);

        assertTrue(message.contains("15"));
        assertEquals(85, playerService.hp());
        assertEquals("RESCUED_BY_ITEM", service.outcomeView(playerService).resultType());

        service.acknowledge(playerService, worldState);

        assertTrue(playerService.inventoryItems().contains("savebreaker_key"));
        assertTrue(worldState.getBoolean("event_completed.mirror_room_event"));
    }

    @Test
    void gardenFailureCanBeRescuedByMaterial() {
        MiniGameService service = service();
        PlayerService playerService = new PlayerService(new ItemService());
        WorldState worldState = new WorldState();
        playerService.gainItem("silver_thread");

        service.start("link_match", "garden_event");
        String sessionId = service.view().sessionId();
        service.handleInput(sessionId, "cancel", Map.of());
        service.rescuePendingOutcome(sessionId, "garden_material_silver_thread", playerService);

        assertEquals("RESCUED_BY_ITEM", service.outcomeView(playerService).resultType());
        assertFalse(playerService.inventoryItems().contains("silver_thread"));

        service.acknowledge(playerService, worldState);

        assertTrue(playerService.inventoryItems().contains("pure_seed"));
        assertTrue(worldState.getBoolean("garden_restored"));
    }

    private MiniGameService service(int... values) {
        return new MiniGameService(new MiniGameRewardResolver(), new SequenceRandom(values));
    }

    @SuppressWarnings("unchecked")
    private String[][] boardFromView(MiniGameService service) {
        List<Map<String, Object>> tiles = (List<Map<String, Object>>) service.view().state().get("board");
        int rows = tiles.stream()
                .mapToInt(tile -> ((Number) tile.get("row")).intValue())
                .max()
                .orElse(0) + 1;
        int cols = tiles.stream()
                .mapToInt(tile -> ((Number) tile.get("col")).intValue())
                .max()
                .orElse(0) + 1;
        String[][] board = new String[rows][cols];
        for (Map<String, Object> tile : tiles) {
            if (!Boolean.TRUE.equals(tile.get("empty"))) {
                int row = ((Number) tile.get("row")).intValue();
                int col = ((Number) tile.get("col")).intValue();
                board[row][col] = String.valueOf(tile.get("symbol"));
            }
        }
        return board;
    }

    private int[] availableMatch(String[][] board) {
        LinkMatchPathFinder finder = new LinkMatchPathFinder();
        for (int r1 = 0; r1 < board.length; r1++) {
            for (int c1 = 0; c1 < board[r1].length; c1++) {
                if (board[r1][c1] == null) {
                    continue;
                }
                for (int r2 = r1; r2 < board.length; r2++) {
                    for (int c2 = 0; c2 < board[r2].length; c2++) {
                        if (r1 == r2 && c2 <= c1) {
                            continue;
                        }
                        if (board[r1][c1].equals(board[r2][c2])
                                && finder.findPath(board, r1, c1, r2, c2).isPresent()) {
                            return new int[]{r1, c1, r2, c2};
                        }
                    }
                }
            }
        }
        throw new AssertionError("随机连连看棋盘没有可消除元素。");
    }

    private static class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        SequenceRandom(int... values) {
            this.values = values.length == 0 ? new int[]{0} : values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index++ % values.length];
            return Math.floorMod(value, bound);
        }

        @Override
        public int nextInt(int origin, int bound) {
            int value = values[index++ % values.length];
            return origin + Math.floorMod(value, bound - origin);
        }
    }
}
