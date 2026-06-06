package cn.edu.whut.sept.zuul.puzzle;

import java.util.HashMap;
import java.util.Map;

public class PuzzleRegistry {

    private final Map<String, Puzzle> puzzles = new HashMap<>();

    public void register(Puzzle puzzle) {
        if (puzzles.containsKey(puzzle.id())) {
            throw new IllegalArgumentException("重复谜题 ID：" + puzzle.id());
        }
        puzzles.put(puzzle.id(), puzzle);
    }

    public Puzzle get(String puzzleId) {
        return puzzles.get(puzzleId);
    }
}
