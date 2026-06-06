package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Schema(description = "游戏房间定义，包含描述、场景资源与方向出口。")
public class Room {

    private final String id;
    private final String title;
    private final String description;
    private final String inspectText;
    private final String assetKey;
    private final Map<Direction, String> exits = new EnumMap<>(Direction.class);

    public Room(String id, String title, String description, String inspectText, String assetKey) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.inspectText = inspectText;
        this.assetKey = assetKey;
    }

    public Room connect(Direction direction, String targetRoomId) {
        exits.put(direction, targetRoomId);
        return this;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String inspectText() {
        return inspectText;
    }

    public String assetKey() {
        return assetKey;
    }

    public Map<Direction, String> exits() {
        return Collections.unmodifiableMap(exits);
    }
}
