package cn.edu.whut.sept.zuul.creator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
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

    private final Path chaptersDirectory;
    private final ObjectMapper objectMapper;
    private final SaveService saveService;

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
