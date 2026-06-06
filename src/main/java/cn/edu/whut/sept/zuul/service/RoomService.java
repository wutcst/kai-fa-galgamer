package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.event.EventEngine;
import cn.edu.whut.sept.zuul.event.EventResult;
import cn.edu.whut.sept.zuul.model.Direction;
import cn.edu.whut.sept.zuul.model.GameActionOption;
import cn.edu.whut.sept.zuul.model.GameActionRequest;
import cn.edu.whut.sept.zuul.model.GamePhase;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.model.Room;
import cn.edu.whut.sept.zuul.item.CraftResult;
import cn.edu.whut.sept.zuul.minigame.MiniGameService;
import cn.edu.whut.sept.zuul.puzzle.PuzzleEngine;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResult;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RoomService {

    private static final String START_ROOM_ID = "fate_hall";
    private static final int MAX_LOG_SIZE = 6;
    private static final Map<String, String> ROOM_EVENTS = Map.of(
            "memory_library", "library_clue_event",
            "broken_shelf", "broken_shelf_event",
            "order_altar", "order_altar_event",
            "soul_garden", "garden_event",
            "material_storage", "material_storage_event",
            "whisper_pool", "whisper_pool_event"
    );
    private static final Map<String, String> ROOM_PUZZLES = Map.of(
            "mirror_corridor", "mirror_number_door",
            "rune_floor", "rune_direction_sequence",
            "alchemy_workshop", "soul_bell_formula",
            "triple_seal_gate", "triple_seal_gate"
    );
    private static final Map<String, String> MINI_GAME_EVENTS = Map.of(
            "mirror_room_event", "point_game",
            "order_altar_event", "dice_check",
            "garden_event", "link_match"
    );

    private final PlayerService playerService;
    private final WorldState worldState;
    private final PuzzleEngine puzzleEngine;
    private final EventEngine eventEngine;
    private final MiniGameService miniGameService;
    private final Map<String, Room> rooms = createRooms();
    private final List<String> logs = new ArrayList<>();
    private String currentRoomId = START_ROOM_ID;

    public RoomService(PlayerService playerService, WorldState worldState, PuzzleEngine puzzleEngine,
                       EventEngine eventEngine, MiniGameService miniGameService) {
        this.playerService = playerService;
        this.worldState = worldState;
        this.puzzleEngine = puzzleEngine;
        this.eventEngine = eventEngine;
        this.miniGameService = miniGameService;
    }

    public synchronized GameSnapshot initGame() {
        currentRoomId = START_ROOM_ID;
        logs.clear();
        playerService.reset();
        worldState.reset();
        miniGameService.reset();
        return snapshot("新游戏已初始化。你在命运大厅醒来。", null);
    }

    public synchronized GameSnapshot perform(GameActionRequest request) {
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            return snapshot("动作未执行。", "缺少动作类型。");
        }

        String actionType = request.actionType().trim().toUpperCase(Locale.ROOT);
        if ("ACK_MINI_GAME_RESULT".equals(actionType)) {
            return snapshot(miniGameService.acknowledge(playerService, worldState), null);
        }
        if ("MINI_GAME_INPUT".equals(actionType)) {
            return snapshot(miniGameService.handleInput(request.target(), request.value(), request.payload()), null);
        }
        if (miniGameService.hasActiveMiniGame() || miniGameService.hasPendingOutcome()) {
            return snapshot("小游戏尚未结束。", "请先完成或确认小游戏结果。");
        }

        return switch (actionType) {
            case "MOVE" -> move(request.target());
            case "LOOK" -> look();
            case "INSPECT" -> inspect();
            case "ANSWER" -> answer(request.target(), request.value());
            case "CRAFT" -> craft(request.target());
            default -> snapshot("动作未执行。", "当前阶段暂不支持动作：" + request.actionType());
        };
    }

    public synchronized GameSnapshot save(String saveId) {
        String normalizedSaveId = saveId == null || saveId.isBlank() ? "slot_1" : saveId;
        return snapshot("Mock 存档已保存：" + normalizedSaveId, null);
    }

    public synchronized GameSnapshot load(String saveId) {
        String normalizedSaveId = saveId == null || saveId.isBlank() ? "slot_1" : saveId;
        return snapshot("Mock 存档已读取：" + normalizedSaveId, null);
    }

    public Map<String, Room> rooms() {
        return Map.copyOf(rooms);
    }

    private GameSnapshot move(String directionText) {
        Room current = currentRoom();
        Direction direction = Direction.fromText(directionText).orElse(null);
        if (direction == null) {
            return snapshot("你停在原地。", "未知方向：" + directionText);
        }

        String targetRoomId = current.exits().get(direction);
        if (targetRoomId == null) {
            return snapshot("你停在原地。", "当前房间不能向" + direction.label() + "移动。");
        }
        if (!canMove(current.id(), direction)) {
            return snapshot("你停在原地。", blockedMoveMessage(current.id()));
        }

        currentRoomId = targetRoomId;
        Room next = currentRoom();
        return snapshot("你向" + direction.label() + "移动，抵达：" + next.title(), null);
    }

    private GameSnapshot look() {
        Room room = currentRoom();
        return snapshot("你环顾四周：" + room.description(), null);
    }

    private GameSnapshot inspect() {
        Room room = currentRoom();
        String eventId = ROOM_EVENTS.get(room.id());
        if (eventId == null) {
            return snapshot("调查结果：" + room.inspectText(), null);
        }

        if (MINI_GAME_EVENTS.containsKey(eventId) && !worldState.getBoolean("event_completed." + eventId)) {
            return snapshot("调查结果：" + room.inspectText() + " " + miniGameService.start(MINI_GAME_EVENTS.get(eventId), eventId), null);
        }
        EventResult result = eventEngine.trigger(eventId, playerService, worldState);
        String eventText = result.message() == null || result.message().isBlank() ? "" : " " + result.message();
        return snapshot("调查结果：" + room.inspectText() + eventText, null);
    }

    private GameSnapshot answer(String target, String value) {
        String puzzleId = target == null || target.isBlank() ? ROOM_PUZZLES.get(currentRoomId) : target;
        if (puzzleId == null || !puzzleId.equals(ROOM_PUZZLES.get(currentRoomId))) {
            return snapshot("解谜请求被拦截。", "当前房间没有这个谜题。");
        }

        PuzzleResult result = puzzleEngine.attempt(puzzleId, value, playerService, worldState);
        Object startEventId = result.data().get("startEventId");
        if (startEventId instanceof String eventId) {
            if (MINI_GAME_EVENTS.containsKey(eventId) && !worldState.getBoolean("event_completed." + eventId)) {
                return snapshot(result.message() + " " + miniGameService.start(MINI_GAME_EVENTS.get(eventId), eventId), null);
            }
            EventResult eventResult = eventEngine.trigger(eventId, playerService, worldState);
            String suffix = eventResult.message() == null || eventResult.message().isBlank() ? "" : " " + eventResult.message();
            return snapshot(result.message() + suffix, null);
        }
        return snapshot(result.message(), result.type().name().equals("FAILED") || result.type().name().equals("LOCKED") ? result.message() : null);
    }

    private GameSnapshot craft(String target) {
        String itemId = target == null || target.isBlank() ? ItemService.SOUL_BELL : target;
        CraftResult result = playerService.craft(itemId);
        if (result.success()) {
            return snapshot(result.message(), null);
        }
        return snapshot("合成未完成。", result.message());
    }

    private GameSnapshot snapshot(String systemMessage, String errorMessage) {
        if (systemMessage != null && !systemMessage.isBlank()) {
            appendLog(systemMessage);
        }

        Room room = currentRoom();
        return new GameSnapshot(
                room.id(),
                room.title(),
                room.description(),
                playerService.hp(),
                playerService.inventoryItems(),
                miniGameService.hasActiveMiniGame() || miniGameService.hasPendingOutcome() ? GamePhase.MINIGAME : GamePhase.EXPLORING,
                room.assetKey(),
                availableActions(room),
                puzzleView(room),
                worldState.flags(),
                miniGameService.view(),
                miniGameService.outcomeView(),
                List.copyOf(logs),
                systemMessage,
                errorMessage
        );
    }

    private Room currentRoom() {
        return rooms.get(currentRoomId);
    }

    private void appendLog(String message) {
        logs.add(message);
        if (logs.size() > MAX_LOG_SIZE) {
            logs.subList(0, logs.size() - MAX_LOG_SIZE).clear();
        }
    }

    private List<GameActionOption> availableActions(Room room) {
        List<GameActionOption> actions = new ArrayList<>();
        room.exits().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().code()))
                .forEach(entry -> actions.add(new GameActionOption(
                        "MOVE",
                        "前往" + entry.getKey().label(),
                        entry.getKey().code(),
                        false
                )));
        actions.add(new GameActionOption("LOOK", "查看四周", room.id(), false));
        actions.add(new GameActionOption("INSPECT", "调查线索", room.id(), false));
        GameSnapshot.PuzzleView puzzle = puzzleView(room);
        if (puzzle != null && "ANSWER".equals(puzzle.submitAction())) {
            actions.add(new GameActionOption("ANSWER", "解开谜题", puzzle.id(), true));
        }
        if ("alchemy_workshop".equals(room.id())) {
            actions.add(new GameActionOption("CRAFT", "合成灵魂之铃", ItemService.SOUL_BELL, false));
        }
        if (miniGameService.hasActiveMiniGame()) {
            actions.add(new GameActionOption("MINI_GAME_INPUT", "操作小游戏", "", false));
        }
        if (miniGameService.hasPendingOutcome()) {
            actions.add(new GameActionOption("ACK_MINI_GAME_RESULT", "确认小游戏结果", "", false));
        }
        actions.add(new GameActionOption("SAVE", "保存游戏", "slot_1", false));
        return actions;
    }

    private boolean canMove(String roomId, Direction direction) {
        if ("rune_floor".equals(roomId) && direction == Direction.SOUTH) {
            return worldState.getBoolean("altar_gate_open");
        }
        if ("triple_seal_gate".equals(roomId) && direction == Direction.SOUTH) {
            return worldState.getBoolean("triple_seal_open");
        }
        return true;
    }

    private String blockedMoveMessage(String roomId) {
        if ("rune_floor".equals(roomId)) {
            return "符文地板尚未稳定，请先完成方向序列谜题。";
        }
        if ("triple_seal_gate".equals(roomId)) {
            return "三重封印仍然闭合，请先解开封印门谜题。";
        }
        return "道路暂时被封锁。";
    }

    private GameSnapshot.PuzzleView puzzleView(Room room) {
        String puzzleId = ROOM_PUZZLES.get(room.id());
        if (puzzleId == null || isPuzzleSolved(puzzleId)) {
            return null;
        }
        return switch (puzzleId) {
            case "mirror_number_door" -> new GameSnapshot.PuzzleView(
                    puzzleId,
                    "镜面要求你给出恰好的数字。",
                    "PASSWORD",
                    List.of("18", "20", "21", "24"),
                    true,
                    "ANSWER"
            );
            case "rune_direction_sequence" -> new GameSnapshot.PuzzleView(
                    puzzleId,
                    "选择正确的四枚符文顺序。",
                    "DIRECTION_SEQUENCE",
                    List.of("south north east west", "north east south west", "west east north south"),
                    true,
                    "ANSWER"
            );
            case "soul_bell_formula" -> new GameSnapshot.PuzzleView(
                    puzzleId,
                    "完成灵魂之铃配方。",
                    "ITEM_COMBINATION",
                    List.of("soul_bell"),
                    false,
                    "CRAFT"
            );
            case "triple_seal_gate" -> new GameSnapshot.PuzzleView(
                    puzzleId,
                    "检验三重封印。",
                    "SEAL_GATE",
                    List.of("open"),
                    false,
                    "ANSWER"
            );
            default -> null;
        };
    }

    private boolean isPuzzleSolved(String puzzleId) {
        return switch (puzzleId) {
            case "mirror_number_door" -> worldState.getBoolean("mirror_door_open");
            case "rune_direction_sequence" -> worldState.getBoolean("altar_gate_open");
            case "soul_bell_formula" -> playerService.inventoryItems().contains(ItemService.SOUL_BELL);
            case "triple_seal_gate" -> worldState.getBoolean("triple_seal_open");
            default -> worldState.getBoolean("puzzle_solved." + puzzleId);
        };
    }

    private static Map<String, Room> createRooms() {
        Map<String, Room> map = new LinkedHashMap<>();

        add(map, room("fate_hall", "命运大厅",
                "破碎石阶延伸向四条岔路，地面的六面骰纹路像仍在缓慢转动。",
                "脚印在大厅中央重复叠加，似乎不止一位挑战者从这里开始循环。",
                "scene.fate_hall")
                .connect(Direction.NORTH, "memory_library")
                .connect(Direction.EAST, "mirror_corridor")
                .connect(Direction.SOUTH, "rune_floor")
                .connect(Direction.WEST, "alchemy_workshop"));
        add(map, room("memory_library", "记忆图书馆",
                "高耸书架被暗金烛光切开，许多无名书在你靠近时轻轻翻页。",
                "一本禁书停在空白骰子的插图旁，页边写着：记忆会说谎，但缺页不会。",
                "scene.memory_library")
                .connect(Direction.SOUTH, "fate_hall")
                .connect(Direction.WEST, "broken_shelf"));
        add(map, room("broken_shelf", "破损书架",
                "倒塌的书架挡住半面墙，散落纸页间闪着微弱的青白光。",
                "你在木刺下发现被撕碎的挑战者记录，末尾只剩一句：不要登上王座。",
                "scene.broken_shelf")
                .connect(Direction.EAST, "memory_library"));
        add(map, room("mirror_corridor", "镜像回廊",
                "狭长回廊两侧都是破碎镜面，每个倒影都选择了不同命运。",
                "尽头的数字符文门没有完全关闭，镜中倒影正指向北方。",
                "scene.mirror_corridor")
                .connect(Direction.WEST, "fate_hall")
                .connect(Direction.NORTH, "broken_mirror_room"));
        add(map, room("broken_mirror_room", "碎镜室",
                "这里像是镜像回廊的伤口，地面铺满反射失败结局的碎片。",
                "一枚裂开的镜片倒映出你离开王座的样子。",
                "scene.broken_mirror_room")
                .connect(Direction.SOUTH, "mirror_corridor"));
        add(map, room("rune_floor", "符文地板",
                "东南西北四枚巨大符文嵌在石室地面，中央留着骰形凹槽。",
                "符文亮度按南、北、东、西短暂闪烁，像是在提醒一条序列。",
                "scene.rune_floor")
                .connect(Direction.NORTH, "fate_hall")
                .connect(Direction.SOUTH, "order_altar"));
        add(map, room("order_altar", "秩序祭坛",
                "两枚黑紫骰子悬浮在祭坛上方，断裂符文像失控的齿轮。",
                "祭坛边缘刻着一句旧誓言：规则可以束缚命运，也可以拆穿命运。",
                "scene.order_altar")
                .connect(Direction.NORTH, "rune_floor")
                .connect(Direction.SOUTH, "soul_garden"));
        add(map, room("soul_garden", "灵魂花园",
                "黑藤缠绕石柱，青白灵魂光点在枯萎花朵之间漂浮。",
                "花园深处传来铃声残响，像某个尚未完成的灵魂之铃。",
                "scene.soul_garden")
                .connect(Direction.NORTH, "order_altar")
                .connect(Direction.WEST, "material_storage")
                .connect(Direction.EAST, "whisper_pool"));
        add(map, room("material_storage", "材料仓库",
                "旧木箱与铁架堆满材料，银线和碎玻璃在微光中发亮。",
                "你辨认出银线、破铃与镜面碎片，它们似乎都能用于修复某件道具。",
                "scene.material_storage")
                .connect(Direction.EAST, "soul_garden"));
        add(map, room("whisper_pool", "低语水池",
                "水面安静得像镜子，池底却不断浮起属于失败者的低语。",
                "水声拼出一句话：铃需要形体、灵魂与束缚，缺一不可。",
                "scene.whisper_pool")
                .connect(Direction.WEST, "soul_garden"));
        add(map, room("alchemy_workshop", "炼金工坊",
                "木质工作台摆满药瓶、银线和半成品铃铛，炉火只剩暗红余温。",
                "配方纸被压在工具箱下，写着灵魂之铃的修复步骤。",
                "scene.alchemy_workshop")
                .connect(Direction.EAST, "fate_hall")
                .connect(Direction.SOUTH, "triple_seal_gate"));
        add(map, room("triple_seal_gate", "三重封印门",
                "巨大石门上有三个碎片槽位，暗金光从门缝中细细渗出。",
                "三个槽位分别回应记忆、秩序与灵魂，门后传来王座大厅的低鸣。",
                "scene.triple_seal_gate")
                .connect(Direction.NORTH, "alchemy_workshop")
                .connect(Direction.SOUTH, "zuul_throne"));
        add(map, room("zuul_throne", "祖尔王座",
                "黑金王座立于高台中央，命运核心悬浮其后，像一枚永不落地的骰子。",
                "王座阴影里没有欢迎，只有一个问题：你会结束循环，还是继承循环？",
                "scene.zuul_throne")
                .connect(Direction.NORTH, "triple_seal_gate"));

        return map;
    }

    private static Room room(String id, String title, String description, String inspectText, String assetKey) {
        return new Room(id, title, description, inspectText, assetKey);
    }

    private static void add(Map<String, Room> map, Room room) {
        map.put(room.id(), room);
    }
}
