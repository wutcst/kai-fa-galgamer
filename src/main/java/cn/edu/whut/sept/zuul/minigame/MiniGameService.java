package cn.edu.whut.sept.zuul.minigame;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class MiniGameService {

    private static final int LINK_TARGET_SUCCESS_PAIRS = 5;
    private static final int LINK_MAX_MOVES = 18;

    private final MiniGameRewardResolver rewardResolver;
    private final LinkMatchPathFinder pathFinder = new LinkMatchPathFinder();
    private final Random random;
    private MiniGameSession activeSession;
    private MiniGameResult pendingOutcome;

    @Autowired
    public MiniGameService(MiniGameRewardResolver rewardResolver) {
        this.rewardResolver = rewardResolver;
        this.random = new Random();
    }

    MiniGameService(MiniGameRewardResolver rewardResolver, Random random) {
        this.rewardResolver = rewardResolver;
        this.random = random;
    }

    public synchronized void reset() {
        activeSession = null;
        pendingOutcome = null;
    }

    public synchronized boolean hasActiveMiniGame() {
        return activeSession != null && !activeSession.finished();
    }

    public synchronized boolean hasPendingOutcome() {
        return pendingOutcome != null;
    }

    public synchronized String start(String gameId, String eventId) {
        if (hasActiveMiniGame() || hasPendingOutcome()) {
            return "小游戏正在进行，无法重复启动。";
        }
        activeSession = new MiniGameSession(UUID.randomUUID().toString(), gameId, eventId);
        switch (gameId) {
            case "dice_check" -> initDice(activeSession);
            case "point_game" -> initPoint(activeSession);
            case "link_match" -> initLink(activeSession);
            default -> throw new IllegalArgumentException("未知小游戏：" + gameId);
        }
        return "小游戏开始：" + gameId;
    }

    public synchronized String handleInput(String sessionId, String input, Map<String, Object> payload) {
        if (!hasActiveMiniGame() || !activeSession.sessionId().equals(sessionId)) {
            return "小游戏请求无效或已结束。";
        }
        String action = input == null ? "" : input.trim().toLowerCase();
        return switch (activeSession.gameId()) {
            case "dice_check" -> handleDice(action);
            case "point_game" -> handlePoint(action);
            case "link_match" -> handleLink(action, payload == null ? Map.of() : payload);
            default -> "未知小游戏。";
        };
    }

    public synchronized String acknowledge(PlayerService playerService, WorldState worldState) {
        if (pendingOutcome == null || activeSession == null) {
            return "没有待确认的小游戏结果。";
        }
        rewardResolver.apply(activeSession.eventId(), pendingOutcome, playerService, worldState);
        String message = pendingOutcome.message();
        activeSession = null;
        pendingOutcome = null;
        return "小游戏结果已确认。" + message;
    }

    public synchronized GameSnapshot.MiniGameView view() {
        if (!hasActiveMiniGame()) {
            return null;
        }
        return new GameSnapshot.MiniGameView(
                activeSession.sessionId(),
                activeSession.gameId(),
                activeSession.eventId(),
                activeSession.phase(),
                actionsFor(activeSession.gameId()),
                publicState(activeSession)
        );
    }

    public synchronized GameSnapshot.MiniGameOutcome outcomeView() {
        if (pendingOutcome == null || activeSession == null) {
            return null;
        }
        return new GameSnapshot.MiniGameOutcome(
                activeSession.sessionId(),
                activeSession.gameId(),
                activeSession.eventId(),
                pendingOutcome.type().name(),
                pendingOutcome.score(),
                pendingOutcome.message(),
                pendingOutcome.rewardItems(),
                pendingOutcome.flags(),
                pendingOutcome.details()
        );
    }

    private void initDice(MiniGameSession session) {
        session.state().put("dice", List.of(0, 0));
        session.state().put("total", 0);
    }

    private void initPoint(MiniGameSession session) {
        session.state().put("cards", new ArrayList<Integer>());
        session.state().put("total", 0);
    }

    private void initLink(MiniGameSession session) {
        String[][] board = {
                {"a", "a", "b", "b"},
                {"c", "c", "d", "d"},
                {"a", "a", "b", "b"},
                {"c", "c", "d", "d"}
        };
        session.state().put("board", boardView(board));
        session.state().put("rawBoard", board);
        session.state().put("matchedPairs", 0);
        session.state().put("moves", 0);
        session.state().put("lastPath", List.of());
        session.state().put("message", "点击两个相同符文，后端会判断两折以内是否可连通。");
    }

    private String handleDice(String action) {
        if (!"roll".equals(action)) {
            return "请选择掷骰。";
        }
        int first = random.nextInt(1, 7);
        int second = random.nextInt(1, 7);
        int total = first + second;
        activeSession.state().put("dice", List.of(first, second));
        activeSession.state().put("total", total);
        MiniGameResultType type = total >= 11 ? MiniGameResultType.GREAT_SUCCESS
                : total >= 7 ? MiniGameResultType.SUCCESS
                : MiniGameResultType.FAILURE;
        finish(type, total, Map.of("dice", List.of(first, second), "total", total));
        return "掷骰结果：" + total;
    }

    @SuppressWarnings("unchecked")
    private String handlePoint(String action) {
        List<Integer> cards = (List<Integer>) activeSession.state().get("cards");
        int total = (int) activeSession.state().get("total");
        if ("draw".equals(action)) {
            int card = random.nextInt(1, 11);
            cards.add(card);
            total += card;
            activeSession.state().put("total", total);
            if (total > 21) {
                finish(MiniGameResultType.FAILURE, total, Map.of("cards", List.copyOf(cards), "total", total));
                return "抽取 " + card + "，总点数 " + total + "，爆点失败。";
            }
            if (total == 21) {
                finish(MiniGameResultType.GREAT_SUCCESS, total, Map.of("cards", List.copyOf(cards), "total", total));
                return "抽取 " + card + "，恰好二十一点。";
            }
            return "抽取 " + card + "，当前点数 " + total + "。";
        }
        if ("stand".equals(action)) {
            MiniGameResultType type = total >= 17 && total <= 20 ? MiniGameResultType.SUCCESS : MiniGameResultType.FAILURE;
            finish(type, total, Map.of("cards", List.copyOf(cards), "total", total));
            return "停手，总点数 " + total + "。";
        }
        return "请选择抽取或停手。";
    }

    private String handleLink(String action, Map<String, Object> payload) {
        if ("cancel".equals(action)) {
            finish(MiniGameResultType.FAILURE, matchedPairs(), Map.of("matchedPairs", matchedPairs()));
            return "连线小游戏已结束。";
        }
        if (!"match".equals(action)) {
            return "请选择两个符文进行连线。";
        }
        String[][] board = rawBoard();
        int rowA;
        int colA;
        int rowB;
        int colB;
        try {
            rowA = intPayload(payload, "rowA");
            colA = intPayload(payload, "colA");
            rowB = intPayload(payload, "rowB");
            colB = intPayload(payload, "colB");
        } catch (RuntimeException ex) {
            activeSession.state().put("message", "坐标数据无效。");
            return "坐标数据无效。";
        }
        var path = pathFinder.findPath(board, rowA, colA, rowB, colB);
        int moves = ((Number) activeSession.state().get("moves")).intValue() + 1;
        activeSession.state().put("moves", moves);
        if (path.isEmpty()) {
            activeSession.state().put("message", "这两个符文无法在两折以内连通。");
            if (moves >= LINK_MAX_MOVES) {
                settleLink(false);
                return "步数耗尽，连线失败。";
            }
            return "无法连通。";
        }

        board[rowA][colA] = null;
        board[rowB][colB] = null;
        int matchedPairs = matchedPairs() + 1;
        activeSession.state().put("matchedPairs", matchedPairs);
        activeSession.state().put("board", boardView(board));
        activeSession.state().put("lastPath", path.get());
        activeSession.state().put("message", "连线成功。");

        if (isCleared(board)) {
            settleLink(true);
            return "棋盘清空，连连看大成功。";
        }
        if (!pathFinder.hasAvailableMatch(board)) {
            board = reshuffle(board);
            activeSession.state().put("rawBoard", board);
            activeSession.state().put("board", boardView(board));
        }
        if (moves >= LINK_MAX_MOVES) {
            settleLink(false);
            return "达到步数限制，连线结束。";
        }
        return "连线成功。";
    }

    private void settleLink(boolean cleared) {
        int matchedPairs = matchedPairs();
        MiniGameResultType type = cleared ? MiniGameResultType.GREAT_SUCCESS
                : matchedPairs >= LINK_TARGET_SUCCESS_PAIRS ? MiniGameResultType.SUCCESS
                : MiniGameResultType.FAILURE;
        finish(type, matchedPairs, Map.of(
                "matchedPairs", matchedPairs,
                "moves", activeSession.state().get("moves"),
                "board", activeSession.state().get("board"),
                "lastPath", activeSession.state().get("lastPath")
        ));
    }

    private String[][] reshuffle(String[][] board) {
        String[][] next = board;
        for (int attempt = 0; attempt < 3; attempt++) {
            next = pathFinder.deterministicReshuffle(next);
            if (pathFinder.hasAvailableMatch(next)) {
                activeSession.state().put("message", "棋盘无解，后端已重新排列。");
                return next;
            }
        }
        settleLink(matchedPairs() >= LINK_TARGET_SUCCESS_PAIRS);
        return next;
    }

    private void finish(MiniGameResultType type, int score, Map<String, Object> details) {
        MiniGameResult result = rewardResolver.preview(activeSession.eventId(), activeSession.gameId(), type, score, details);
        activeSession.finish(result);
        pendingOutcome = result;
    }

    private List<String> actionsFor(String gameId) {
        return switch (gameId) {
            case "dice_check" -> List.of("roll");
            case "point_game" -> List.of("draw", "stand");
            case "link_match" -> List.of("match", "cancel");
            default -> List.of();
        };
    }

    private Map<String, Object> publicState(MiniGameSession session) {
        Map<String, Object> state = new HashMap<>(session.state());
        state.remove("rawBoard");
        return Map.copyOf(state);
    }

    private String[][] rawBoard() {
        return (String[][]) activeSession.state().get("rawBoard");
    }

    private int matchedPairs() {
        return ((Number) activeSession.state().get("matchedPairs")).intValue();
    }

    private boolean isCleared(String[][] board) {
        return Arrays.stream(board).flatMap(Arrays::stream).allMatch(symbol -> symbol == null);
    }

    private int intPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<Map<String, Object>> boardView(String[][] board) {
        List<Map<String, Object>> tiles = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                String symbol = board[row][col];
                Map<String, Object> tile = new HashMap<>();
                tile.put("row", row);
                tile.put("col", col);
                tile.put("symbol", symbol);
                tile.put("empty", symbol == null);
                tiles.add(tile);
            }
        }
        return tiles;
    }
}
