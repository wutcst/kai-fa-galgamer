package cn.edu.whut.sept.zuul.minigame;

import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class MiniGameService {

    private static final int LINK_BOARD_ROWS = 4;
    private static final int LINK_BOARD_COLS = 4;
    private static final int LINK_TARGET_SUCCESS_PAIRS = 5;
    private static final int LINK_MAX_MOVES = 18;
    private static final int LINK_REFRESH_ATTEMPTS = 100;
    private static final List<String> LINK_SYMBOL_POOL = List.of("a", "b", "c", "d");
    private static final int MIRROR_RESCUE_HP_COST = 15;
    private static final String MIRROR_RESCUE_ID = "mirror_break_hp";
    private static final String GARDEN_RESCUE_SILVER_ID = "garden_material_silver_thread";
    private static final String GARDEN_RESCUE_FLOWER_ID = "garden_material_soul_flower";

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

    public synchronized String rescuePendingOutcome(String sessionId, String rescueId, PlayerService playerService) {
        if (pendingOutcome == null || activeSession == null || !activeSession.sessionId().equals(sessionId)) {
            return "没有可救赎的小游戏结果。";
        }
        if (pendingOutcome.type() != MiniGameResultType.FAILURE) {
            return "当前结果不需要救赎。";
        }

        String normalized = rescueId == null ? "" : rescueId.trim();
        Map<String, Object> details = new HashMap<>(pendingOutcome.details());
        if (MIRROR_RESCUE_ID.equals(normalized) && "mirror_room_event".equals(activeSession.eventId())) {
            if (playerService.hp() <= MIRROR_RESCUE_HP_COST) {
                return "生命值不足，无法承受碎镜反噬。";
            }
            playerService.damage(MIRROR_RESCUE_HP_COST);
            details.put("rescuedBy", MIRROR_RESCUE_ID);
            details.put("hpCost", MIRROR_RESCUE_HP_COST);
            pendingOutcome = rewardResolver.preview(
                    activeSession.eventId(),
                    activeSession.gameId(),
                    MiniGameResultType.RESCUED_BY_ITEM,
                    pendingOutcome.score(),
                    details
            );
            activeSession.finish(pendingOutcome);
            return "你承受 15 点灵魂反噬，强行砸碎命运之镜。";
        }

        if (GARDEN_RESCUE_SILVER_ID.equals(normalized) && "garden_event".equals(activeSession.eventId())) {
            return rescueGardenWithMaterial("silver_thread", normalized, playerService, details);
        }
        if (GARDEN_RESCUE_FLOWER_ID.equals(normalized) && "garden_event".equals(activeSession.eventId())) {
            return rescueGardenWithMaterial("soul_flower", normalized, playerService, details);
        }
        return "当前小游戏没有这个救赎选项。";
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

    public synchronized GameSnapshot.MiniGameOutcome outcomeView(PlayerService playerService) {
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
                pendingOutcome.details(),
                rescueOptions(playerService)
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
        String[][] board = createPlayableLinkBoard();
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
            MiniGameResultType type = total == 21 ? MiniGameResultType.GREAT_SUCCESS
                    : evaluatePoint21Relaxed(total, 16) ? MiniGameResultType.SUCCESS
                    : MiniGameResultType.FAILURE;
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
        if (isCleared(board)) {
            return board;
        }
        String[][] next = reshuffleRemainingTiles(board);
        if (pathFinder.hasAvailableMatch(next)) {
            activeSession.state().put("message", "棋盘无解，后端已重新排列。");
            return next;
        }
        activeSession.state().put("rawBoard", next);
        activeSession.state().put("board", boardView(next));
        settleLink(matchedPairs() >= LINK_TARGET_SUCCESS_PAIRS);
        return next;
    }

    private void finish(MiniGameResultType type, int score, Map<String, Object> details) {
        MiniGameResult result = rewardResolver.preview(activeSession.eventId(), activeSession.gameId(), type, score, details);
        activeSession.finish(result);
        pendingOutcome = result;
    }

    public boolean evaluatePoint21Relaxed(int playerPoints, int bankerPoints) {
        if (playerPoints > 21) {
            return false;
        }
        return playerPoints >= 17 || playerPoints > bankerPoints;
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
        if ("link_match".equals(session.gameId())) {
            state.put("maxMoves", LINK_MAX_MOVES);
            state.put("targetPairs", LINK_TARGET_SUCCESS_PAIRS);
            state.put("remainingPairs", remainingPairs(rawBoard()));
        }
        return Map.copyOf(state);
    }

    private String rescueGardenWithMaterial(String itemId, String rescueId, PlayerService playerService, Map<String, Object> details) {
        if (!playerService.consumeItem(itemId)) {
            return "缺少救赎材料：" + itemId;
        }
        details.put("rescuedBy", rescueId);
        details.put("consumedItem", itemId);
        pendingOutcome = rewardResolver.preview(
                activeSession.eventId(),
                activeSession.gameId(),
                MiniGameResultType.RESCUED_BY_ITEM,
                pendingOutcome.score(),
                details
        );
        activeSession.finish(pendingOutcome);
        return "你消耗 " + itemId + " 灌溉枯土，强行唤醒纯净种子。";
    }

    private List<GameSnapshot.RescueOption> rescueOptions(PlayerService playerService) {
        if (pendingOutcome == null || pendingOutcome.type() != MiniGameResultType.FAILURE || activeSession == null) {
            return List.of();
        }
        List<GameSnapshot.RescueOption> options = new ArrayList<>();
        if ("mirror_room_event".equals(activeSession.eventId()) && playerService.hp() > MIRROR_RESCUE_HP_COST) {
            options.add(new GameSnapshot.RescueOption(
                    MIRROR_RESCUE_ID,
                    "碎镜钥匙救赎",
                    "遭受 15 点灵魂反噬，强行砸碎命运之镜以取出钥匙。",
                    "HP -15",
                    "minigame.rescue.mirror"
            ));
        }
        if ("garden_event".equals(activeSession.eventId())) {
            if (playerService.inventoryItems().contains("silver_thread")) {
                options.add(new GameSnapshot.RescueOption(
                        GARDEN_RESCUE_SILVER_ID,
                        "纯净之种救赎",
                        "消耗银线重织花园根系，跳过连线失败惩罚。",
                        "silver_thread x1",
                        "minigame.rescue.garden"
                ));
            }
            if (playerService.inventoryItems().contains("soul_flower")) {
                options.add(new GameSnapshot.RescueOption(
                        GARDEN_RESCUE_FLOWER_ID,
                        "纯净之种救赎",
                        "消耗灵魂之花进行土壤灌溉，跳过连线失败惩罚。",
                        "soul_flower x1",
                        "minigame.rescue.garden"
                ));
            }
        }
        return List.copyOf(options);
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

    private String[][] createPlayableLinkBoard() {
        for (int attempt = 0; attempt < LINK_REFRESH_ATTEMPTS; attempt++) {
            String[][] board = fillLinkBoard(shuffledLinkSymbols(LINK_BOARD_ROWS * LINK_BOARD_COLS));
            if (pathFinder.hasAvailableMatch(board)) {
                return board;
            }
        }
        return fillLinkBoard(pairedLinkSymbols(LINK_BOARD_ROWS * LINK_BOARD_COLS));
    }

    private List<String> shuffledLinkSymbols(int tileCount) {
        List<String> symbols = pairedLinkSymbols(tileCount);
        shuffleSymbols(symbols);
        return symbols;
    }

    private List<String> pairedLinkSymbols(int tileCount) {
        if (tileCount % 2 != 0) {
            throw new IllegalStateException("连连看棋盘格子数必须为偶数。");
        }
        List<String> symbols = new ArrayList<>(tileCount);
        for (int pairIndex = 0; pairIndex < tileCount / 2; pairIndex++) {
            String symbol = LINK_SYMBOL_POOL.get(pairIndex % LINK_SYMBOL_POOL.size());
            symbols.add(symbol);
            symbols.add(symbol);
        }
        return symbols;
    }

    private String[][] fillLinkBoard(List<String> symbols) {
        String[][] board = new String[LINK_BOARD_ROWS][LINK_BOARD_COLS];
        int index = 0;
        for (int row = 0; row < LINK_BOARD_ROWS; row++) {
            for (int col = 0; col < LINK_BOARD_COLS; col++) {
                board[row][col] = symbols.get(index++);
            }
        }
        return board;
    }

    private String[][] reshuffleRemainingTiles(String[][] board) {
        for (int attempt = 0; attempt < LINK_REFRESH_ATTEMPTS; attempt++) {
            String[][] next = fillRemainingTiles(board, shuffledRemainingSymbols(board));
            if (pathFinder.hasAvailableMatch(next)) {
                return next;
            }
        }
        return forceAvailableRemainingBoard(board);
    }

    private List<String> shuffledRemainingSymbols(String[][] board) {
        List<String> symbols = remainingSymbols(board);
        shuffleSymbols(symbols);
        return symbols;
    }

    private List<String> remainingSymbols(String[][] board) {
        List<String> symbols = new ArrayList<>();
        for (String[] row : board) {
            for (String symbol : row) {
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        }
        return symbols;
    }

    private String[][] fillRemainingTiles(String[][] board, List<String> symbols) {
        String[][] next = new String[board.length][board[0].length];
        int index = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (board[row][col] != null) {
                    next[row][col] = symbols.get(index++);
                }
            }
        }
        return next;
    }

    private String[][] forceAvailableRemainingBoard(String[][] board) {
        List<Position> positions = filledPositions(board);
        List<String> symbols = remainingSymbols(board);
        for (String symbol : symbolsWithAtLeastPair(symbols)) {
            for (int first = 0; first < positions.size(); first++) {
                for (int second = first + 1; second < positions.size(); second++) {
                    Position firstPosition = positions.get(first);
                    Position secondPosition = positions.get(second);
                    String[][] next = placePairFirst(board, symbols, symbol, firstPosition, secondPosition);
                    if (pathFinder.findPath(next, firstPosition.row(), firstPosition.col(),
                            secondPosition.row(), secondPosition.col()).isPresent()) {
                        return next;
                    }
                }
            }
        }
        return fillRemainingTiles(board, symbols);
    }

    private List<Position> filledPositions(String[][] board) {
        List<Position> positions = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (board[row][col] != null) {
                    positions.add(new Position(row, col));
                }
            }
        }
        return positions;
    }

    private List<String> symbolsWithAtLeastPair(List<String> symbols) {
        List<String> pairedSymbols = new ArrayList<>();
        for (String symbol : symbols) {
            if (!pairedSymbols.contains(symbol) && Collections.frequency(symbols, symbol) >= 2) {
                pairedSymbols.add(symbol);
            }
        }
        return pairedSymbols;
    }

    private String[][] placePairFirst(String[][] board, List<String> symbols, String symbol, Position first, Position second) {
        List<String> rest = new ArrayList<>(symbols);
        rest.remove(symbol);
        rest.remove(symbol);
        shuffleSymbols(rest);

        String[][] next = new String[board.length][board[0].length];
        next[first.row()][first.col()] = symbol;
        next[second.row()][second.col()] = symbol;

        int index = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (board[row][col] != null && next[row][col] == null) {
                    next[row][col] = rest.get(index++);
                }
            }
        }
        return next;
    }

    private void shuffleSymbols(List<String> symbols) {
        for (int index = symbols.size() - 1; index > 0; index--) {
            Collections.swap(symbols, index, random.nextInt(index + 1));
        }
    }

    private int remainingPairs(String[][] board) {
        long remainingTiles = Arrays.stream(board)
                .flatMap(Arrays::stream)
                .filter(symbol -> symbol != null)
                .count();
        return (int) (remainingTiles / 2);
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

    private record Position(int row, int col) {
    }
}
