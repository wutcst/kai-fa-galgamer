package cn.edu.whut.sept.zuul.puzzle;

import java.util.Map;

public record PuzzleResult(PuzzleResultType type, String message, Map<String, Object> data) {

    public PuzzleResult {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
