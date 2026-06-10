package cn.edu.whut.sept.zuul.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.edu.whut.sept.zuul.dialogue.DialogueConditionEvaluator;
import cn.edu.whut.sept.zuul.dialogue.DialogueEffectExecutor;
import cn.edu.whut.sept.zuul.dialogue.DialogueEngine;
import cn.edu.whut.sept.zuul.dialogue.DialogueGraph;
import cn.edu.whut.sept.zuul.dialogue.DialogueNode;
import cn.edu.whut.sept.zuul.dialogue.DialogueCharacter;
import cn.edu.whut.sept.zuul.dialogue.DialogueChoice;
import cn.edu.whut.sept.zuul.dialogue.DialogueCondition;
import cn.edu.whut.sept.zuul.dialogue.DialogueEffect;
import cn.edu.whut.sept.zuul.event.EventEngine;
import cn.edu.whut.sept.zuul.event.EventResult;
import cn.edu.whut.sept.zuul.item.CraftResult;
import cn.edu.whut.sept.zuul.minigame.MiniGameService;
import cn.edu.whut.sept.zuul.model.Direction;
import cn.edu.whut.sept.zuul.model.GameActionOption;
import cn.edu.whut.sept.zuul.model.GameActionRequest;
import cn.edu.whut.sept.zuul.model.GamePhase;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.model.Room;
import cn.edu.whut.sept.zuul.creator.CustomChapterService;
import cn.edu.whut.sept.zuul.puzzle.PuzzleEngine;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResult;
import cn.edu.whut.sept.zuul.save.BossSaveData;
import cn.edu.whut.sept.zuul.save.EndingSaveData;
import cn.edu.whut.sept.zuul.save.ProfileState;
import cn.edu.whut.sept.zuul.save.SaveManager;
import cn.edu.whut.sept.zuul.save.SaveStateAccess;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RoomService {

    private static final String START_ROOM_ID = "fate_hall";
    private static final String FATE_HALL_DIALOGUE = "dial_fate_hall_meeting";
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
    private static final Map<String, RoomMapPoint> ROOM_MAP_POINTS = Map.ofEntries(
            Map.entry("fate_hall", new RoomMapPoint(50, 35)),
            Map.entry("memory_library", new RoomMapPoint(50, 15)),
            Map.entry("broken_shelf", new RoomMapPoint(30, 15)),
            Map.entry("mirror_corridor", new RoomMapPoint(75, 35)),
            Map.entry("broken_mirror_room", new RoomMapPoint(75, 15)),
            Map.entry("rune_floor", new RoomMapPoint(50, 55)),
            Map.entry("order_altar", new RoomMapPoint(50, 70)),
            Map.entry("soul_garden", new RoomMapPoint(50, 85)),
            Map.entry("material_storage", new RoomMapPoint(30, 85)),
            Map.entry("whisper_pool", new RoomMapPoint(70, 85)),
            Map.entry("alchemy_workshop", new RoomMapPoint(25, 35)),
            Map.entry("triple_seal_gate", new RoomMapPoint(25, 55)),
            Map.entry("zuul_throne", new RoomMapPoint(25, 75))
    );

    private final PlayerService playerService;
    private final WorldState worldState;
    private final PuzzleEngine puzzleEngine;
    private final EventEngine eventEngine;
    private final MiniGameService miniGameService;
    private final BattleService battleService;
    private final EndingService endingService;
    private final DialogueEngine dialogueEngine;
    private final SaveManager saveManager;
    private final CustomChapterService customChapterService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Room> rooms = createRooms();
    private final Map<String, String> roomDialogues = new LinkedHashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final LinkedHashSet<String> visitedRoomIds = new LinkedHashSet<>();
    private String currentRoomId = START_ROOM_ID;
    private GamePhase phase = GamePhase.MAIN_MENU;
    private List<String> creatorValidationErrors = List.of();

    @Autowired
    public RoomService(PlayerService playerService, WorldState worldState, PuzzleEngine puzzleEngine,
                       EventEngine eventEngine, MiniGameService miniGameService, BattleService battleService,
                       EndingService endingService, DialogueEngine dialogueEngine, SaveManager saveManager,
                       CustomChapterService customChapterService) {
        this.playerService = playerService;
        this.worldState = worldState;
        this.puzzleEngine = puzzleEngine;
        this.eventEngine = eventEngine;
        this.miniGameService = miniGameService;
        this.battleService = battleService;
        this.endingService = endingService;
        this.dialogueEngine = dialogueEngine;
        this.saveManager = saveManager;
        this.customChapterService = customChapterService;
    }

    RoomService(PlayerService playerService, WorldState worldState, PuzzleEngine puzzleEngine,
                EventEngine eventEngine, MiniGameService miniGameService) {
        this(playerService, worldState, puzzleEngine, eventEngine, miniGameService,
                new BattleService(),
                new EndingService(),
                defaultDialogueEngine(),
                new SaveManager(new cn.edu.whut.sept.zuul.save.SaveService()),
                new CustomChapterService(new cn.edu.whut.sept.zuul.save.SaveService()));
    }

    RoomService(PlayerService playerService, WorldState worldState, PuzzleEngine puzzleEngine,
                EventEngine eventEngine, MiniGameService miniGameService, BattleService battleService,
                EndingService endingService) {
        this(playerService, worldState, puzzleEngine, eventEngine, miniGameService,
                battleService,
                endingService,
                defaultDialogueEngine(),
                new SaveManager(new cn.edu.whut.sept.zuul.save.SaveService()),
                new CustomChapterService(new cn.edu.whut.sept.zuul.save.SaveService()));
    }

    public synchronized GameSnapshot initGame() {
        rooms.clear();
        rooms.putAll(createRooms());
        roomDialogues.clear();
        currentRoomId = START_ROOM_ID;
        phase = GamePhase.EXPLORING;
        logs.clear();
        visitedRoomIds.clear();
        visitedRoomIds.add(START_ROOM_ID);
        playerService.reset();
        worldState.reset();
        miniGameService.reset();
        battleService.reset();
        endingService.reset();
        dialogueEngine.reset();
        creatorValidationErrors = List.of();
        return snapshot("新游戏已初始化。你在命运大厅醒来。", null);
    }

    public synchronized GameSnapshot menu() {
        rooms.clear();
        rooms.putAll(createRooms());
        roomDialogues.clear();
        phase = GamePhase.MAIN_MENU;
        return snapshot("欢迎来到 World of Zuul: Uncertain Fate。", null);
    }

    public synchronized GameSnapshot state() {
        return snapshot(null, null);
    }

    public synchronized GameSnapshot perform(GameActionRequest request) {
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            return snapshot("动作未执行。", "缺少动作类型。");
        }
        String actionType = request.actionType().trim().toUpperCase(Locale.ROOT);
        if ("NEW_GAME".equals(actionType)) {
            return initGame();
        }
        if ("CONTINUE".equals(actionType)) {
            return continueLatest();
        }
        if ("SAVE".equals(actionType)) {
            return save(request.target());
        }
        if ("LOAD".equals(actionType)) {
            return load(request.target());
        }
        if (dialogueEngine.hasActiveSession()) {
            if ("ADV_NEXT".equals(actionType)) {
                phase = dialogueEngine.advance(playerService, worldState);
                return snapshot("对话继续。", null);
            }
            if ("ADV_CHOOSE".equals(actionType)) {
                phase = dialogueEngine.choose(request.target(), playerService, worldState);
                return snapshot("抉择已记录。", null);
            }
            return snapshot("对话尚未结束。", "请先完成当前 ADV 对话。");
        }
        if ("CREATOR_LIST".equals(actionType)) {
            phase = GamePhase.CREATOR;
            return snapshot("Creator Mode 已打开。", null);
        }
        if ("CREATOR_VALIDATE".equals(actionType)) {
            return validateCreatorPayload(request.payload());
        }
        if ("CREATOR_PLAY".equals(actionType)) {
            return playCreatorChapter(request.target());
        }
        if ("ACK_GAME_OVER".equals(actionType)) {
            return acknowledgeGameOver();
        }
        if (phase == GamePhase.MAIN_MENU) {
            return snapshot("动作未执行。", "请先从主菜单开始或继续游戏。");
        }
        if (phase == GamePhase.CREATOR) {
            return snapshot("动作未执行。", "Creator Mode 中请使用 Creator 操作或返回新游戏。");
        }

        if ("ACK_MINI_GAME_RESULT".equals(actionType)) {
            return snapshot(miniGameService.acknowledge(playerService, worldState), null);
        }
        if ("RESCUE_MINI_GAME_RESULT".equals(actionType)) {
            String rescueId = request.value();
            if ((rescueId == null || rescueId.isBlank()) && request.payload().get("rescueId") instanceof String payloadRescueId) {
                rescueId = payloadRescueId;
            }
            return snapshot(miniGameService.rescuePendingOutcome(request.target(), rescueId, playerService), null);
        }
        if ("MINI_GAME_INPUT".equals(actionType)) {
            return snapshot(miniGameService.handleInput(request.target(), request.value(), request.payload()), null);
        }
        if (miniGameService.hasActiveMiniGame() || miniGameService.hasPendingOutcome()) {
            return snapshot("小游戏尚未结束。", "请先完成或确认小游戏结果。");
        }
        if ("BATTLE_ACTION".equals(actionType) || "BOSS_ACTION".equals(actionType)) {
            String battleAction = request.value();
            if (battleAction == null || battleAction.isBlank()) {
                battleAction = request.target();
            }
            if ((battleAction == null || battleAction.isBlank()) && request.payload().get("action") instanceof String payloadAction) {
                battleAction = payloadAction;
            }
            String message = battleService.perform(battleAction, playerService, worldState);
            if (playerService.hp() <= 0 || worldState.getBoolean("final_boss_lost")) {
                battleService.reset();
                phase = GamePhase.GAME_OVER;
                return snapshot(message, null);
            }
            return snapshot(message, null);
        }
        if (battleService.hasActiveBattle()) {
            return snapshot("Boss 战尚未结束。", "请先完成当前 Boss 战。");
        }
        if ("CHOOSE_ENDING".equals(actionType) || "FINAL_CHOICE".equals(actionType)) {
            String choiceId = request.target();
            if ((choiceId == null || choiceId.isBlank()) && request.payload().get("choiceId") instanceof String payloadChoice) {
                choiceId = payloadChoice;
            }
            String message = endingService.choose(choiceId, playerService, worldState);
            persistCreatorUnlockIfNeeded();
            return snapshot(message, null);
        }

        return switch (actionType) {
            case "MOVE" -> move(request.target());
            case "LOOK" -> look();
            case "INSPECT" -> inspect();
            case "ANSWER" -> answer(request.target(), request.value());
            case "CRAFT" -> craft(request.target());
            case "START_BATTLE" -> startBattle();
            default -> snapshot("动作未执行。", "当前阶段暂不支持动作：" + request.actionType());
        };
    }

    public synchronized GameSnapshot save(String saveId) {
        try {
            String normalizedSaveId = saveManager.save(saveId, saveAccess());
            return snapshot("存档已保存：" + normalizedSaveId, null);
        } catch (RuntimeException ex) {
            return snapshot("存档未完成。", ex.getMessage());
        }
    }

    public synchronized GameSnapshot load(String saveId) {
        try {
            rooms.clear();
            rooms.putAll(createRooms());
            roomDialogues.clear();
            return saveManager.load(saveId, saveAccess())
                    .map(data -> snapshot("存档已读取：" + data.getSaveId(), null))
                    .orElseGet(() -> snapshot("读档未完成。", "存档不存在：" + saveManager.saveService().normalizeId(saveId)));
        } catch (RuntimeException ex) {
            return snapshot("读档未完成。", ex.getMessage());
        }
    }

    private GameSnapshot continueLatest() {
        return saveManager.saveService().latestSaveId()
                .map(this::load)
                .orElseGet(() -> snapshot("继续游戏未完成。", "没有可读取的存档。"));
    }

    private GameSnapshot validateCreatorPayload(Map<String, Object> payload) {
        JsonNode chapter = objectMapper.valueToTree(payload == null ? Map.of() : payload.getOrDefault("chapter", payload));
        creatorValidationErrors = customChapterService.validate(chapter);
        phase = GamePhase.CREATOR;
        return snapshot(creatorValidationErrors.isEmpty() ? "自定义章节校验通过。" : "自定义章节校验失败。", null);
    }

    private GameSnapshot playCreatorChapter(String chapterId) {
        phase = GamePhase.CREATOR;
        if (!creatorModeUnlocked()) {
            return snapshot("Creator Mode 尚未解锁。", "完成真结局后才能试玩自定义章节。");
        }
        return customChapterService.load(chapterId)
                .map(chapter -> {
                    creatorValidationErrors = customChapterService.validate(chapter);
                    if (!creatorValidationErrors.isEmpty()) {
                        return snapshot("自定义章节无法试玩。", String.join("；", creatorValidationErrors));
                    }
                    
                    rooms.clear();
                    roomDialogues.clear();
                    playerService.reset();
                    worldState.reset();
                    miniGameService.reset();
                    battleService.reset();
                    endingService.reset();
                    dialogueEngine.reset();
                    logs.clear();
                    visitedRoomIds.clear();
                    
                    String startRoomId = chapter.path("startRoomId").asText();
                    JsonNode roomsNode = chapter.get("rooms");
                    if (roomsNode != null && roomsNode.isArray()) {
                        for (JsonNode roomNode : roomsNode) {
                            String rId = roomNode.path("roomId").asText();
                            String rTitle = roomNode.path("name").asText(roomNode.path("title").asText("未命名房间"));
                            String rDesc = roomNode.path("description").asText("这间房间没有描述。");
                            String rInspect = roomNode.path("inspectText").asText(roomNode.path("inspect").asText(rDesc));
                            String rAsset = roomNode.path("assetKey").asText("scene.fallback");
                            
                            Room room = new Room(rId, rTitle, rDesc, rInspect, rAsset);
                            
                            JsonNode exitsNode = roomNode.get("exits");
                            if (exitsNode != null && exitsNode.isObject()) {
                                exitsNode.fields().forEachRemaining(entry -> {
                                    Direction.fromText(entry.getKey()).ifPresent(dir -> room.connect(dir, entry.getValue().asText()));
                                });
                            }
                            rooms.put(rId, room);
                            
                            JsonNode dialNode = roomNode.get("dialogue");
                            if (dialNode != null && dialNode.isObject()) {
                                String groupId = dialNode.path("dialogueGroupId").asText();
                                if (!groupId.isBlank()) {
                                    roomDialogues.put(rId, groupId);
                                    registerCustomDialogue(dialNode);
                                }
                            }
                        }
                    }
                    
                    currentRoomId = rooms.containsKey(startRoomId) ? startRoomId : START_ROOM_ID;
                    visitedRoomIds.add(currentRoomId);
                    phase = GamePhase.EXPLORING;
                    checkAndStartRoomDialogue();
                    
                    return snapshot("自定义章节试玩已启动：" + chapter.path("title").asText(chapterId), null);
                })
                .orElseGet(() -> snapshot("自定义章节不存在。", "未找到章节：" + chapterId));
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
        visitedRoomIds.add(targetRoomId);
        Room next = currentRoom();
        phase = GamePhase.EXPLORING;
        checkAndStartRoomDialogue();
        return snapshot("你向" + direction.label() + "移动，抵达：" + next.title(), null);
    }

    private void checkAndStartRoomDialogue() {
        String dialGroupId = roomDialogues.get(currentRoomId);
        if (dialGroupId != null && dialogueEngine.canStart(dialGroupId, worldState)) {
            dialogueEngine.start(dialGroupId, currentPhase());
        }
    }

    private void registerCustomDialogue(JsonNode dialNode) {
        if (dialNode == null || !dialNode.isObject()) return;
        String groupId = dialNode.path("dialogueGroupId").asText();
        String startNodeId = dialNode.path("startNodeId").asText();
        if (groupId.isBlank() || startNodeId.isBlank()) return;

        JsonNode nodesObj = dialNode.get("nodes");
        if (nodesObj == null || !nodesObj.isObject()) return;

        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        nodesObj.fields().forEachRemaining(entry -> {
            String nodeId = entry.getKey();
            JsonNode node = entry.getValue();
            
            String type = node.path("type").asText("SPEECH");
            String speakerSide = node.has("speakerSide") ? node.path("speakerSide").asText() : null;
            String speakerName = node.has("speakerName") ? node.path("speakerName").asText() : null;
            String expressionKey = node.has("expressionKey") ? node.path("expressionKey").asText() : null;
            String text = node.has("text") ? node.path("text").asText() : null;
            String audioSfx = node.has("audioSfx") ? node.path("audioSfx").asText() : null;
            
            DialogueCharacter left = parseCharacter(node.get("leftCharacter"));
            DialogueCharacter right = parseCharacter(node.get("rightCharacter"));
            
            String eventType = node.has("eventType") ? node.path("eventType").asText() : null;
            String eventPayload = node.has("eventPayload") ? node.path("eventPayload").asText() : null;
            String dialogueLog = node.has("dialogueLog") ? node.path("dialogueLog").asText() : null;
            String nextNodeId = node.has("nextNodeId") ? node.path("nextNodeId").asText() : null;
            
            List<DialogueChoice> choices = new ArrayList<>();
            JsonNode choicesNode = node.get("choices");
            if (choicesNode != null && choicesNode.isArray()) {
                for (JsonNode choiceNode : choicesNode) {
                    choices.add(parseChoice(choiceNode));
                }
            }
            
            nodes.put(nodeId, new DialogueNode(
                nodeId, type, speakerSide, speakerName, expressionKey, text, audioSfx,
                left, right, eventType, eventPayload, dialogueLog, nextNodeId, choices
            ));
        });

        dialogueEngine.registerGraph(groupId, new DialogueGraph(groupId, startNodeId, nodes));
    }

    private DialogueCharacter parseCharacter(JsonNode charNode) {
        if (charNode == null || !charNode.isObject()) return null;
        String id = charNode.path("id").asText();
        String expression = charNode.path("expression").asText("default");
        return new DialogueCharacter(id, expression);
    }

    private DialogueChoice parseChoice(JsonNode choiceNode) {
        if (choiceNode == null || !choiceNode.isObject()) return null;
        String choiceId = choiceNode.path("choiceId").asText();
        String text = choiceNode.path("text").asText();
        String nextNodeId = choiceNode.path("nextNodeId").asText();
        
        List<DialogueCondition> conditions = new ArrayList<>();
        JsonNode condNode = choiceNode.get("conditions");
        if (condNode != null && condNode.isArray()) {
            for (JsonNode cond : condNode) {
                String type = cond.path("type").asText();
                String itemKey = cond.has("itemKey") ? cond.path("itemKey").asText() : null;
                String flagKey = cond.has("flagKey") ? cond.path("flagKey").asText() : null;
                Boolean expected = cond.has("expected") ? cond.path("expected").asBoolean() : null;
                Integer value = cond.has("value") ? cond.path("value").asInt() : null;
                conditions.add(new DialogueCondition(type, itemKey, flagKey, expected, value));
            }
        }
        
        List<DialogueEffect> effects = new ArrayList<>();
        JsonNode effNode = choiceNode.get("effects");
        if (effNode != null && effNode.isArray()) {
            for (JsonNode eff : effNode) {
                String type = eff.path("type").asText();
                String itemKey = eff.has("itemKey") ? eff.path("itemKey").asText() : null;
                String flagKey = eff.has("flagKey") ? eff.path("flagKey").asText() : null;
                Boolean booleanValue = eff.has("booleanValue") ? eff.path("booleanValue").asBoolean() : null;
                Integer value = eff.has("value") ? eff.path("value").asInt() : null;
                String eventPayload = eff.has("eventPayload") ? eff.path("eventPayload").asText() : null;
                effects.add(new DialogueEffect(type, itemKey, flagKey, booleanValue, value, eventPayload));
            }
        }
        
        return new DialogueChoice(choiceId, text, conditions, effects, nextNodeId);
    }

    public Map<String, Room> rooms() {
        return Map.copyOf(rooms);
    }

    private GameSnapshot look() {
        Room room = currentRoom();
        return snapshot("你环顾四周：" + room.description(), null);
    }

    private GameSnapshot inspect() {
        Room room = currentRoom();
        if ("fate_hall".equals(room.id()) && dialogueEngine.canStart(FATE_HALL_DIALOGUE, worldState)) {
            dialogueEngine.start(FATE_HALL_DIALOGUE, currentPhase());
            return snapshot("命运大厅的阴影中，有人拦住了你的去路。", null);
        }
        String dialGroupId = roomDialogues.get(room.id());
        if (dialGroupId != null && dialogueEngine.canStart(dialGroupId, worldState)) {
            dialogueEngine.start(dialGroupId, currentPhase());
            return snapshot("调查线索，触发了场景对话。", null);
        }
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
        String error = result.type().name().equals("FAILED") || result.type().name().equals("LOCKED") ? result.message() : null;
        phase = error == null ? GamePhase.EXPLORING : GamePhase.PUZZLE;
        return snapshot(result.message(), error);
    }

    private GameSnapshot craft(String target) {
        String itemId = target == null || target.isBlank() ? ItemService.SOUL_BELL : target;
        CraftResult result = playerService.craft(itemId);
        if (result.success()) {
            phase = GamePhase.EXPLORING;
            return snapshot(result.message(), null);
        }
        phase = GamePhase.CRAFTING;
        return snapshot("合成未完成。", result.message());
    }

    private GameSnapshot startBattle() {
        if (!"zuul_throne".equals(currentRoomId)) {
            return snapshot("战斗尚未开始。", "只有抵达祖尔王座才能挑战 Zuul Overlord。");
        }
        phase = GamePhase.BATTLE;
        return snapshot(battleService.startFinalBattle(worldState), null);
    }

    private GameSnapshot acknowledgeGameOver() {
        battleService.reset();
        miniGameService.reset();
        endingService.reset();
        phase = GamePhase.MAIN_MENU;
        return snapshot("循环回到起点。", null);
    }

    private GameSnapshot snapshot(String systemMessage, String errorMessage) {
        if (systemMessage != null && !systemMessage.isBlank()) {
            appendLog(systemMessage);
        }

        Room room = currentRoom();
        GamePhase snapshotPhase = currentPhase();
        return new GameSnapshot(
                room.id(),
                room.title(),
                room.description(),
                playerService.hp(),
                playerService.inventoryItems(),
                snapshotPhase,
                room.assetKey(),
                dialogueEngine.view(playerService, worldState),
                availableActions(room),
                puzzleView(room),
                worldState.flags(),
                miniGameService.view(),
                miniGameService.outcomeView(playerService),
                menuView(snapshotPhase),
                saveView(),
                battleService.view(),
                endingService.availableChoices(playerService, worldState),
                endingService.endingView(),
                creatorView(snapshotPhase),
                failureView(snapshotPhase),
                mapView(),
                List.copyOf(logs),
                systemMessage,
                errorMessage
        );
    }

    private GamePhase currentPhase() {
        if (phase == GamePhase.MAIN_MENU || phase == GamePhase.CREATOR || phase == GamePhase.GAME_OVER) {
            return phase;
        }
        if (endingService.endingView() != null || !endingService.availableChoices(playerService, worldState).isEmpty()) {
            return GamePhase.ENDING;
        }
        if (battleService.hasActiveBattle()) {
            return GamePhase.BATTLE;
        }
        if (miniGameService.hasActiveMiniGame() || miniGameService.hasPendingOutcome()) {
            return GamePhase.MINIGAME;
        }
        if (phase == GamePhase.PUZZLE || phase == GamePhase.CRAFTING) {
            return phase;
        }
        return GamePhase.EXPLORING;
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

    private GameSnapshot.MenuView menuView(GamePhase snapshotPhase) {
        if (snapshotPhase != GamePhase.MAIN_MENU) {
            return null;
        }
        return new GameSnapshot.MenuView(
                saveManager.saveService().hasAnySave(),
                creatorModeUnlocked(),
                List.of(
                        new GameActionOption("NEW_GAME", "新游戏", "", false),
                        new GameActionOption("CONTINUE", "继续游戏", "", false),
                        new GameActionOption("CREATOR_LIST", creatorModeUnlocked() ? "Creator Mode" : "Creator Mode（未解锁）", "", false)
                )
        );
    }

    private GameSnapshot.SaveView saveView() {
        List<GameSnapshot.SaveSlotView> slots = saveManager.saveService().listSaves().stream()
                .map(data -> new GameSnapshot.SaveSlotView(
                        data.getSaveId(),
                        true,
                        data.getCurrentRoomId(),
                        rooms.getOrDefault(data.getCurrentRoomId(), currentRoom()).title(),
                        data.getGamePhase().name(),
                        data.getSavedAt()
                ))
                .toList();
        return new GameSnapshot.SaveView(slots);
    }

    private GameSnapshot.CreatorView creatorView(GamePhase snapshotPhase) {
        if (snapshotPhase != GamePhase.CREATOR) {
            return null;
        }
        return new GameSnapshot.CreatorView(
                creatorModeUnlocked(),
                customChapterService.listChapters(),
                creatorValidationErrors,
                List.of(
                        new GameActionOption("CREATOR_VALIDATE", "校验章节 JSON", "", true),
                        new GameActionOption("CREATOR_PLAY", "试玩章节", "", true),
                        new GameActionOption("NEW_GAME", "返回新游戏", "", false)
                )
        );
    }

    private GameSnapshot.FailureView failureView(GamePhase snapshotPhase) {
        if (snapshotPhase != GamePhase.GAME_OVER) {
            return null;
        }
        return new GameSnapshot.FailureView(
                "循环崩塌",
                "你倒在祖尔王座前，命运核心把失败的回声重新卷回起点。",
                "ending.boss_failure",
                List.of(new GameActionOption("ACK_GAME_OVER", "确认失败", "", false))
        );
    }

    private GameSnapshot.WorldMapView mapView() {
        Room current = currentRoom();
        List<String> adjacentRoomIds = List.copyOf(current.exits().values());
        List<GameSnapshot.MapRoomView> roomViews = rooms.values().stream()
                .map(room -> {
                    RoomMapPoint point = ROOM_MAP_POINTS.getOrDefault(room.id(), new RoomMapPoint(50, 50));
                    boolean explored = visitedRoomIds.contains(room.id());
                    return new GameSnapshot.MapRoomView(
                            room.id(),
                            explored ? room.title() : null,
                            point.x(),
                            point.y(),
                            explored,
                            room.id().equals(currentRoomId),
                            adjacentRoomIds.contains(room.id())
                    );
                })
                .toList();
        List<GameSnapshot.MapExitView> exitViews = rooms.values().stream()
                .flatMap(room -> room.exits().entrySet().stream()
                        .map(entry -> new GameSnapshot.MapExitView(
                                room.id(),
                                entry.getValue(),
                                entry.getKey().code(),
                                !canMove(room.id(), entry.getKey())
                        )))
                .toList();
        return new GameSnapshot.WorldMapView(roomViews, exitViews);
    }

    private List<GameActionOption> availableActions(Room room) {
        List<GameActionOption> actions = new ArrayList<>();
        GamePhase snapshotPhase = currentPhase();
        if (snapshotPhase == GamePhase.MAIN_MENU) {
            actions.add(new GameActionOption("NEW_GAME", "新游戏", "", false));
            actions.add(new GameActionOption("CONTINUE", "继续游戏", "", false));
            actions.add(new GameActionOption("CREATOR_LIST", "Creator Mode", "", false));
            return actions;
        }
        if (snapshotPhase == GamePhase.CREATOR) {
            actions.add(new GameActionOption("CREATOR_VALIDATE", "校验章节 JSON", "", true));
            actions.add(new GameActionOption("CREATOR_PLAY", "试玩章节", "", true));
            actions.add(new GameActionOption("NEW_GAME", "返回新游戏", "", false));
            return actions;
        }
        if (snapshotPhase == GamePhase.GAME_OVER) {
            actions.add(new GameActionOption("ACK_GAME_OVER", "确认失败", "", false));
            return actions;
        }
        if (dialogueEngine.hasActiveSession()) {
            GameSnapshot.DialogueView dialogue = dialogueEngine.view(playerService, worldState);
            if (dialogue != null && "CHOICE".equalsIgnoreCase(dialogue.nodeType())) {
                actions.add(new GameActionOption("ADV_CHOOSE", "选择回应", "", true));
            } else {
                actions.add(new GameActionOption("ADV_NEXT", "继续对话", "", false));
            }
            return actions;
        }
        if (endingService.endingView() != null) {
            actions.add(new GameActionOption("NEW_GAME", "返回新游戏", "", false));
            if (creatorModeUnlocked()) {
                actions.add(new GameActionOption("CREATOR_LIST", "进入 Creator Mode", "", false));
            }
            return actions;
        }
        if (battleService.hasActiveBattle()) {
            actions.add(new GameActionOption("BATTLE_ACTION", "攻击", "attack", false));
            actions.add(new GameActionOption("BATTLE_ACTION", "防御", "defend", false));
            actions.add(new GameActionOption("BATTLE_ACTION", "掷命运骰", "roll", false));
            actions.add(new GameActionOption("BATTLE_ACTION", "敲响灵魂之铃", "use_soul_bell", false));
            actions.add(new GameActionOption("SAVE", "保存游戏", "slot_1", false));
            return actions;
        }
        if (!endingService.availableChoices(playerService, worldState).isEmpty()) {
            actions.add(new GameActionOption("CHOOSE_ENDING", "选择结局", "", true));
            return actions;
        }
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
        if ("zuul_throne".equals(room.id()) && !worldState.getBoolean("final_boss_defeated")) {
            actions.add(new GameActionOption("START_BATTLE", "挑战 Zuul Overlord", "zuul_overlord", false));
        }
        if (miniGameService.hasActiveMiniGame()) {
            actions.add(new GameActionOption("MINI_GAME_INPUT", "操作小游戏", "", false));
        }
        if (miniGameService.hasPendingOutcome()) {
            actions.add(new GameActionOption("ACK_MINI_GAME_RESULT", "确认小游戏结果", "", false));
        }
        actions.add(new GameActionOption("SAVE", "保存游戏", "slot_1", false));
        actions.add(new GameActionOption("LOAD", "读取游戏", "slot_1", false));
        return actions;
    }

    private boolean creatorModeUnlocked() {
        return worldState.getBoolean("creator_mode_unlocked") || saveManager.saveService().loadProfile().isCreatorModeUnlocked();
    }

    private void persistCreatorUnlockIfNeeded() {
        if (!worldState.getBoolean("creator_mode_unlocked")) {
            return;
        }
        ProfileState profile = saveManager.saveService().loadProfile();
        profile.setCreatorModeUnlocked(true);
        profile.setCycleBroken(worldState.getBoolean("cycle_broken"));
        profile.getCompletedEndings().add("write_own_chapter");
        saveManager.saveService().saveProfile(profile);
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

    private SaveStateAccess saveAccess() {
        return new SaveStateAccess() {
            @Override
            public String currentRoomId() {
                return RoomService.this.currentRoomId;
            }

            @Override
            public int playerHp() {
                return playerService.hp();
            }

            @Override
            public List<String> inventoryItems() {
                return playerService.inventoryItems();
            }

            @Override
            public List<String> visitedRoomIds() {
                return List.copyOf(visitedRoomIds);
            }

            @Override
            public Map<String, Boolean> flags() {
                return worldState.flags();
            }

            @Override
            public Map<String, Integer> counters() {
                return worldState.counters();
            }

            @Override
            public GamePhase phaseForSave() {
                return dialogueEngine.hasActiveSession() ? dialogueEngine.suspendedPhase() : currentPhase();
            }

            @Override
            public BossSaveData bossForSave() {
                return battleService.saveData();
            }

            @Override
            public EndingSaveData endingForSave() {
                return null;
            }

            @Override
            public boolean saveBlocked() {
                return miniGameService.hasActiveMiniGame() || miniGameService.hasPendingOutcome() || dialogueEngine.hasActiveSession();
            }

            @Override
            public void restoreWorldState(Map<String, Boolean> flags, Map<String, Integer> counters) {
                worldState.replaceFlags(flags);
                worldState.replaceCounters(counters);
            }

            @Override
            public void restoreSpatialContext(String restoredRoomId) {
                currentRoomId = rooms.containsKey(restoredRoomId) ? restoredRoomId : START_ROOM_ID;
            }

            @Override
            public void restoreVisitedRooms(List<String> restoredVisitedRoomIds) {
                visitedRoomIds.clear();
                if (restoredVisitedRoomIds != null) {
                    restoredVisitedRoomIds.stream()
                            .filter(rooms::containsKey)
                            .forEach(visitedRoomIds::add);
                }
                if (visitedRoomIds.isEmpty()) {
                    visitedRoomIds.add(START_ROOM_ID);
                }
                visitedRoomIds.add(currentRoomId);
            }

            @Override
            public void restorePlayer(int hp, List<String> inventoryItems) {
                playerService.restore(hp, inventoryItems);
            }

            @Override
            public void restoreTransientState(BossSaveData bossState, EndingSaveData endingState) {
                miniGameService.reset();
                battleService.restore(bossState);
                endingService.reset();
                dialogueEngine.reset();
                creatorValidationErrors = List.of();
            }

            @Override
            public void restorePhase(GamePhase restoredPhase) {
                if (restoredPhase == GamePhase.MAIN_MENU || restoredPhase == GamePhase.CREATOR) {
                    phase = restoredPhase;
                    return;
                }
                if (restoredPhase == GamePhase.BATTLE && battleService.hasActiveBattle()) {
                    phase = GamePhase.BATTLE;
                    return;
                }
                phase = restoredPhase == null || restoredPhase == GamePhase.MINIGAME || restoredPhase == GamePhase.BATTLE
                        ? GamePhase.EXPLORING
                        : restoredPhase;
            }
        };
    }

    private static DialogueEngine defaultDialogueEngine() {
        return new DialogueEngine(new DialogueConditionEvaluator(), new DialogueEffectExecutor());
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

    private record RoomMapPoint(int x, int y) {
    }
}
