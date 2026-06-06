package cn.edu.whut.sept.zuul.puzzle.builtin;

import cn.edu.whut.sept.zuul.puzzle.Puzzle;
import cn.edu.whut.sept.zuul.puzzle.PuzzleContext;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResult;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResultType;

import java.util.Map;

public class ItemCombinationPuzzle implements Puzzle {

    @Override
    public String id() {
        return "soul_bell_formula";
    }

    @Override
    public String description() {
        return "配方写着：铃需要形体、灵魂与束缚。";
    }

    @Override
    public PuzzleResult attempt(String input, PuzzleContext context) {
        return new PuzzleResult(PuzzleResultType.PARTIAL, "请在炼金工坊合成 soul_bell。", Map.of());
    }

    @Override
    public boolean isSolved(PuzzleContext context) {
        return context.playerService().inventoryItems().contains("soul_bell");
    }
}
