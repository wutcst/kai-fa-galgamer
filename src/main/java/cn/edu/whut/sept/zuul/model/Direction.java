package cn.edu.whut.sept.zuul.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;
import java.util.Optional;

@Schema(description = "房间移动方向。")
public enum Direction {
    NORTH("north", "北"),
    SOUTH("south", "南"),
    EAST("east", "东"),
    WEST("west", "西"),
    UP("up", "上"),
    DOWN("down", "下");

    private final String code;
    private final String label;

    Direction(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static Optional<Direction> fromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "north", "n", "北" -> Optional.of(NORTH);
            case "south", "s", "南" -> Optional.of(SOUTH);
            case "east", "e", "东" -> Optional.of(EAST);
            case "west", "w", "西" -> Optional.of(WEST);
            case "up", "u", "上" -> Optional.of(UP);
            case "down", "d", "下" -> Optional.of(DOWN);
            default -> Optional.empty();
        };
    }
}
