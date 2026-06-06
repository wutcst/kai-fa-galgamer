package cn.edu.whut.sept.zuul.puzzle.builtin;

import cn.edu.whut.sept.zuul.puzzle.Puzzle;
import cn.edu.whut.sept.zuul.puzzle.PuzzleContext;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResult;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResultType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DirectionSequencePuzzle implements Puzzle {

    private static final List<String> CORRECT_SEQUENCE = List.of("south", "north", "east", "west");

    @Override
    public String id() {
        return "rune_direction_sequence";
    }

    @Override
    public String description() {
        return "符文按南、北、东、西闪烁。";
    }

    @Override
    public PuzzleResult attempt(String input, PuzzleContext context) {
        List<String> sequence = Arrays.stream((input == null ? "" : input).trim().toLowerCase().split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
        if (sequence.equals(CORRECT_SEQUENCE)) {
            context.worldState().setBoolean("altar_gate_open", true);
            return new PuzzleResult(PuzzleResultType.SOLVED, "符文地板稳定下来，通往秩序祭坛的道路打开了。", Map.of());
        }

        int errors = context.worldState().incrementInt("rune_error_count");
        if (errors >= 3) {
            context.playerService().damage(8);
            context.worldState().setBoolean("rune_disordered", true);
            return new PuzzleResult(PuzzleResultType.FAILED, "符文第三次错乱，命运回火灼伤了你。", Map.of("errors", errors));
        }
        return new PuzzleResult(PuzzleResultType.FAILED, "符文顺序错误，地板重新归位。错误次数：" + errors + "/3。", Map.of("errors", errors));
    }

    @Override
    public boolean isSolved(PuzzleContext context) {
        return context.worldState().getBoolean("altar_gate_open");
    }
}
