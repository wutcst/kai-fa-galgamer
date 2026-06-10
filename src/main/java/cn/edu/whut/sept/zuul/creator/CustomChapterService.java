package cn.edu.whut.sept.zuul.creator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cn.edu.whut.sept.zuul.dialogue.DialogueEngine;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.save.SaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class CustomChapterService {

    private static final Set<String> EVENT_TYPES = Set.of("story", "reward", "minigame", "battle", "puzzle", "setFlag", "ending");
    private static final Set<String> DIALOGUE_NODE_TYPES = Set.of("SPEECH", "CHOICE", "EVENT_TRIGGER");
    private static final Set<String> CONDITION_TYPES = Set.of("HAS_ITEM", "HP_GREATER_THAN", "WORLD_FLAG_EQUALS");
    private static final Set<String> EFFECT_TYPES = Set.of("GAIN_HP", "LOSE_HP", "GAIN_ITEM", "LOSE_ITEM", "SET_FLAG", "BYPASS_PUZZLE");

    private final Path chaptersDirectory;
    private final ObjectMapper objectMapper;
    private final SaveService saveService;
    private final ItemService itemService = new ItemService();

    @Autowired
    public CustomChapterService(SaveService saveService) {
        this(Paths.get("custom-chapters"), saveService);
    }

    CustomChapterService(Path chaptersDirectory, SaveService saveService) {
        this.chaptersDirectory = chaptersDirectory.toAbsolutePath().normalize();
        this.saveService = saveService;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public boolean unlocked() {
        return saveService.loadProfile().isCreatorModeUnlocked();
    }

    public List<GameSnapshot.CreatorChapterSummary> listChapters() {
        if (!Files.isDirectory(chaptersDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(chaptersDirectory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::summaryOrNull)
                    .filter(summary -> summary != null)
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    public List<String> validate(JsonNode chapter) {
        List<String> errors = new ArrayList<>();
        if (chapter == null || !chapter.isObject()) {
            return List.of("chapter 必须是 JSON 对象。");
        }
        String chapterId = text(chapter, "chapterId");
        String title = text(chapter, "title");
        String startRoomId = text(chapter, "startRoomId");
        JsonNode rooms = chapter.get("rooms");
        if (chapterId.isBlank()) {
            errors.add("chapterId 必填。");
        }
        if (title.isBlank()) {
            errors.add("title 必填。");
        }
        if (startRoomId.isBlank()) {
            errors.add("startRoomId 必填。");
        }
        if (rooms == null || !rooms.isArray() || rooms.isEmpty()) {
            errors.add("rooms 至少需要一个房间。");
            return errors;
        }

        Set<String> roomIds = new HashSet<>();
        for (JsonNode room : rooms) {
            String roomId = text(room, "roomId");
            if (roomId.isBlank()) {
                errors.add("rooms[].roomId 必填。");
                continue;
            }
            if (!roomIds.add(roomId)) {
                errors.add("重复房间 ID：" + roomId);
            }
        }
        if (!roomIds.contains(startRoomId)) {
            errors.add("startRoomId 指向不存在房间：" + startRoomId);
        }
        for (JsonNode room : rooms) {
            String roomId = text(room, "roomId");
            JsonNode exits = room.get("exits");
            if (exits != null && exits.isObject()) {
                exits.fields().forEachRemaining(entry -> {
                    String target = entry.getValue().asText("");
                    if (!roomIds.contains(target)) {
                        errors.add("房间 " + roomId + " 的出口 " + entry.getKey() + " 指向不存在房间：" + target);
                    }
                });
            }
            validateEvent(roomId, room.get("event"), errors);
            validateDialogue(roomId, room.get("dialogue"), errors);
        }
        return errors;
    }

    public GameSnapshot.CreatorChapterSummary save(JsonNode chapter) {
        List<String> errors = validate(chapter);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
        if (!unlocked()) {
            throw new IllegalStateException("Creator Mode 尚未解锁。");
        }
        String chapterId = text(chapter, "chapterId");
        try {
            Files.createDirectories(chaptersDirectory);
            objectMapper.writeValue(resolveChapterPath(chapterId).toFile(), chapter);
            return summary(chapter);
        } catch (IOException ex) {
            throw new IllegalStateException("保存自定义章节失败：" + ex.getMessage(), ex);
        }
    }

    public Optional<JsonNode> load(String chapterId) {
        Path path = resolveChapterPath(chapterId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readTree(path.toFile()));
        } catch (IOException ex) {
            throw new IllegalStateException("读取自定义章节失败：" + ex.getMessage(), ex);
        }
    }

    public boolean delete(String chapterId) {
        if (!unlocked()) {
            throw new IllegalStateException("Creator Mode 尚未解锁。");
        }
        try {
            return Files.deleteIfExists(resolveChapterPath(chapterId));
        } catch (IOException ex) {
            throw new IllegalStateException("删除自定义章节失败：" + ex.getMessage(), ex);
        }
    }

    public Path resolveChapterPath(String chapterId) {
        String normalized = normalizeId(chapterId);
        Path targetPath = chaptersDirectory.resolve(normalized + ".json").normalize();
        if (!targetPath.startsWith(chaptersDirectory)) {
            throw new SecurityException("非法路径穿越尝试。");
        }
        return targetPath;
    }

    private void validateEvent(String roomId, JsonNode event, List<String> errors) {
        if (event == null || event.isNull()) {
            return;
        }
        String type = text(event, "type");
        if (!EVENT_TYPES.contains(type)) {
            errors.add("房间 " + roomId + " 使用了非法事件类型：" + type);
            return;
        }
        if ("puzzle".equals(type)) {
            if (text(event, "prompt").isBlank()) {
                errors.add("房间 " + roomId + " 的 puzzle.prompt 必填。");
            }
            if (text(event, "answer").isBlank()) {
                errors.add("房间 " + roomId + " 的 puzzle.answer 必填。");
            }
        }
        if ("battle".equals(type) && event.get("enemy") == null) {
            errors.add("房间 " + roomId + " 的 battle.enemy 必填。");
        }
    }

    private void validateDialogue(String roomId, JsonNode dialogue, List<String> errors) {
        if (dialogue == null || dialogue.isNull()) {
            return;
        }
        if (!dialogue.isObject()) {
            errors.add("房间 " + roomId + " 的 dialogue 必须是对象。");
            return;
        }
        String groupId = text(dialogue, "dialogueGroupId");
        String startNodeId = text(dialogue, "startNodeId");
        JsonNode nodes = dialogue.get("nodes");
        if (groupId.isBlank()) {
            errors.add("房间 " + roomId + " 的 dialogue.dialogueGroupId 必填。");
        }
        if (startNodeId.isBlank()) {
            errors.add("房间 " + roomId + " 的 dialogue.startNodeId 必填。");
        }
        if (nodes == null || !nodes.isObject() || nodes.isEmpty()) {
            errors.add("房间 " + roomId + " 的 dialogue.nodes 至少需要一个节点。");
            return;
        }
        if (!nodes.has(startNodeId)) {
            errors.add("房间 " + roomId + " 的 dialogue.startNodeId 指向不存在节点：" + startNodeId);
        }

        Set<String> nodeIds = new HashSet<>();
        nodes.fieldNames().forEachRemaining(nodeIds::add);
        Set<String> reachable = new HashSet<>();
        validateDialogueNode(roomId, startNodeId, nodes, nodeIds, reachable, errors);
        for (String nodeId : nodeIds) {
            if (!reachable.contains(nodeId)) {
                errors.add("房间 " + roomId + " 的 dialogue 存在孤岛节点：" + nodeId);
            }
        }
    }

    private void validateDialogueNode(String roomId, String nodeId, JsonNode nodes, Set<String> nodeIds,
                                      Set<String> reachable, List<String> errors) {
        if (nodeId == null || nodeId.isBlank() || DialogueEngine.EXIT.equals(nodeId) || !reachable.add(nodeId)) {
            return;
        }
        JsonNode node = nodes.get(nodeId);
        if (node == null || !node.isObject()) {
            errors.add("房间 " + roomId + " 的 dialogue 节点不存在：" + nodeId);
            return;
        }
        String type = text(node, "type").toUpperCase();
        if (!DIALOGUE_NODE_TYPES.contains(type)) {
            errors.add("房间 " + roomId + " 的 dialogue 节点 " + nodeId + " 使用非法类型：" + text(node, "type"));
            return;
        }
        if ("CHOICE".equals(type)) {
            validateChoiceNode(roomId, nodeId, node, nodes, nodeIds, reachable, errors);
            return;
        }
        String nextNodeId = text(node, "nextNodeId");
        if (nextNodeId.isBlank()) {
            errors.add("房间 " + roomId + " 的 dialogue 节点 " + nodeId + " 缺少 nextNodeId。");
            return;
        }
        if (!DialogueEngine.EXIT.equals(nextNodeId) && !nodeIds.contains(nextNodeId)) {
            errors.add("房间 " + roomId + " 的 dialogue 节点 " + nodeId + " 指向不存在节点：" + nextNodeId);
            return;
        }
        validateDialogueNode(roomId, nextNodeId, nodes, nodeIds, reachable, errors);
    }

    private void validateChoiceNode(String roomId, String nodeId, JsonNode node, JsonNode nodes, Set<String> nodeIds,
                                    Set<String> reachable, List<String> errors) {
        JsonNode choices = node.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            errors.add("房间 " + roomId + " 的 CHOICE 节点 " + nodeId + " 至少需要一个 choices[]。");
            return;
        }
        int index = 0;
        for (JsonNode choice : choices) {
            String choicePath = nodeId + ".choices[" + index + "]";
            String nextNodeId = text(choice, "nextNodeId");
            if (nextNodeId.isBlank()) {
                errors.add("房间 " + roomId + " 的 " + choicePath + " 缺少 nextNodeId。");
            } else if (!DialogueEngine.EXIT.equals(nextNodeId) && !nodeIds.contains(nextNodeId)) {
                errors.add("房间 " + roomId + " 的 " + choicePath + " 指向不存在节点：" + nextNodeId);
            }
            validateConditions(roomId, choicePath, choice.get("conditions"), errors);
            validateEffects(roomId, choicePath, choice.get("effects"), errors);
            validateDialogueNode(roomId, nextNodeId, nodes, nodeIds, reachable, errors);
            index++;
        }
    }

    private void validateConditions(String roomId, String path, JsonNode conditions, List<String> errors) {
        if (conditions == null || conditions.isNull()) {
            return;
        }
        if (!conditions.isArray()) {
            errors.add("房间 " + roomId + " 的 " + path + ".conditions 必须是数组。");
            return;
        }
        for (JsonNode condition : conditions) {
            String type = text(condition, "type").toUpperCase();
            if (!CONDITION_TYPES.contains(type)) {
                errors.add("房间 " + roomId + " 的 " + path + " 使用非法条件类型：" + text(condition, "type"));
            }
            if ("HAS_ITEM".equals(type) && !itemService.exists(text(condition, "itemKey"))) {
                errors.add("房间 " + roomId + " 的 " + path + " 使用未知物品条件：" + text(condition, "itemKey"));
            }
        }
    }

    private void validateEffects(String roomId, String path, JsonNode effects, List<String> errors) {
        if (effects == null || effects.isNull()) {
            return;
        }
        if (!effects.isArray()) {
            errors.add("房间 " + roomId + " 的 " + path + ".effects 必须是数组。");
            return;
        }
        for (JsonNode effect : effects) {
            String type = text(effect, "type").toUpperCase();
            if (!EFFECT_TYPES.contains(type)) {
                errors.add("房间 " + roomId + " 的 " + path + " 使用非法副作用类型：" + text(effect, "type"));
            }
            if (("GAIN_ITEM".equals(type) || "LOSE_ITEM".equals(type)) && !itemService.exists(text(effect, "itemKey"))) {
                errors.add("房间 " + roomId + " 的 " + path + " 使用未知物品副作用：" + text(effect, "itemKey"));
            }
        }
    }

    private GameSnapshot.CreatorChapterSummary summaryOrNull(Path path) {
        try {
            return summary(objectMapper.readTree(path.toFile()));
        } catch (IOException ex) {
            return null;
        }
    }

    private GameSnapshot.CreatorChapterSummary summary(JsonNode chapter) {
        JsonNode rooms = chapter.get("rooms");
        return new GameSnapshot.CreatorChapterSummary(
                text(chapter, "chapterId"),
                text(chapter, "title"),
                text(chapter, "author").isBlank() ? "Player" : text(chapter, "author"),
                rooms != null && rooms.isArray() ? rooms.size() : 0
        );
    }

    private String normalizeId(String rawId) {
        String candidate = rawId == null || rawId.isBlank() ? "chapter" : rawId;
        String normalized = candidate.replaceAll("[^A-Za-z0-9_-]", "_");
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }
}
