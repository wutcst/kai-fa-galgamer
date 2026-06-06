package cn.edu.whut.sept.zuul.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SaveService {

    private static final String PROFILE_ID = "profile";
    private final Path saveDirectory;
    private final ObjectMapper objectMapper;

    public SaveService() {
        this(Paths.get("data", "saves"));
    }

    public SaveService(Path saveDirectory) {
        this.saveDirectory = saveDirectory.toAbsolutePath().normalize();
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String save(GameSaveData data) {
        String normalized = normalizeId(data.getSaveId());
        data.setSaveId(normalized);
        data.setSavedAt(System.currentTimeMillis());
        try {
            Files.createDirectories(saveDirectory);
            objectMapper.writeValue(resolveSavePath(normalized).toFile(), data);
            return normalized;
        } catch (IOException ex) {
            throw new IllegalStateException("存档失败：" + ex.getMessage(), ex);
        }
    }

    public Optional<GameSaveData> load(String saveId) {
        String normalized = normalizeId(saveId);
        Path path = resolveSavePath(normalized);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(path.toFile(), GameSaveData.class));
        } catch (IOException ex) {
            throw new IllegalStateException("读档失败：" + ex.getMessage(), ex);
        }
    }

    public List<GameSaveData> listSaves() {
        if (!Files.isDirectory(saveDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(saveDirectory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals(PROFILE_ID + ".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::readSaveOrNull)
                    .filter(data -> data != null)
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    public Optional<String> latestSaveId() {
        if (!Files.isDirectory(saveDirectory)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(saveDirectory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals(PROFILE_ID + ".json"))
                    .max(Comparator.comparing(path -> path.toFile().lastModified()))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.json$", ""));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public boolean delete(String saveId) {
        String normalized = normalizeId(saveId);
        if (PROFILE_ID.equals(normalized)) {
            throw new IllegalArgumentException("profile.json 不能通过普通存档接口删除。");
        }
        try {
            return Files.deleteIfExists(resolveSavePath(normalized));
        } catch (IOException ex) {
            throw new IllegalStateException("删除存档失败：" + ex.getMessage(), ex);
        }
    }

    public ProfileState loadProfile() {
        Path path = resolveSavePath(PROFILE_ID);
        if (!Files.exists(path)) {
            return new ProfileState();
        }
        try {
            return objectMapper.readValue(path.toFile(), ProfileState.class);
        } catch (IOException ex) {
            return new ProfileState();
        }
    }

    public void saveProfile(ProfileState profile) {
        try {
            Files.createDirectories(saveDirectory);
            objectMapper.writeValue(resolveSavePath(PROFILE_ID).toFile(), profile);
        } catch (IOException ex) {
            throw new IllegalStateException("保存玩家档案失败：" + ex.getMessage(), ex);
        }
    }

    public boolean hasAnySave() {
        return !listSaves().isEmpty();
    }

    public String normalizeId(String rawId) {
        String candidate = rawId == null || rawId.isBlank() ? "slot_1" : rawId;
        String normalized = candidate.replaceAll("[^A-Za-z0-9_-]", "_");
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    public Path resolveSavePath(String saveId) {
        String normalized = normalizeId(saveId);
        Path targetPath = saveDirectory.resolve(normalized + ".json").normalize();
        if (!targetPath.startsWith(saveDirectory)) {
            throw new SecurityException("非法路径穿越尝试。");
        }
        return targetPath;
    }

    private GameSaveData readSaveOrNull(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), GameSaveData.class);
        } catch (IOException ex) {
            return null;
        }
    }
}
