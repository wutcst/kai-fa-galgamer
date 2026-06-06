package cn.edu.whut.sept.zuul.puzzle;

import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PuzzleEngine {

    private final PuzzleRegistry registry;

    public PuzzleEngine(PuzzleRegistry registry) {
        this.registry = registry;
    }

    public PuzzleResult attempt(String puzzleId, String input, PlayerService playerService, WorldState worldState) {
        if (puzzleId == null || puzzleId.isBlank()) {
            return new PuzzleResult(PuzzleResultType.LOCKED, "这里没有可解的谜题。", Map.of());
        }
        Puzzle puzzle = registry.get(puzzleId);
        if (puzzle == null) {
            return new PuzzleResult(PuzzleResultType.LOCKED, "谜题未注册：" + puzzleId, Map.of("puzzleId", puzzleId));
        }

        PuzzleContext context = new PuzzleContext(playerService, worldState);
        if (puzzle.isSolved(context) || worldState.getBoolean("puzzle_solved." + puzzleId)) {
            return new PuzzleResult(PuzzleResultType.ALREADY_SOLVED, "该谜题已经被解开。", Map.of("puzzleId", puzzleId));
        }

        PuzzleResult result = puzzle.attempt(input, context);
        if (result.type() == PuzzleResultType.SOLVED) {
            worldState.setBoolean("puzzle_solved." + puzzleId, true);
        }
        return result;
    }
}
