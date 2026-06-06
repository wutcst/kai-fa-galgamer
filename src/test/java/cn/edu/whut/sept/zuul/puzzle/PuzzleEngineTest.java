package cn.edu.whut.sept.zuul.puzzle;

import cn.edu.whut.sept.zuul.event.EventEngine;
import cn.edu.whut.sept.zuul.service.ItemService;
import cn.edu.whut.sept.zuul.service.PlayerService;
import cn.edu.whut.sept.zuul.state.WorldState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleEngineTest {

    @Test
    void directionPuzzleSetsSolvedFlagAndRejectsRepeatedAttempt() {
        WorldState worldState = new WorldState();
        PlayerService playerService = new PlayerService(new ItemService());
        PuzzleEngine engine = engine();

        PuzzleResult solved = engine.attempt("rune_direction_sequence", "south north east west", playerService, worldState);
        PuzzleResult repeated = engine.attempt("rune_direction_sequence", "north east south west", playerService, worldState);

        assertEquals(PuzzleResultType.SOLVED, solved.type());
        assertTrue(worldState.getBoolean("altar_gate_open"));
        assertTrue(worldState.getBoolean("puzzle_solved.rune_direction_sequence"));
        assertEquals(PuzzleResultType.ALREADY_SOLVED, repeated.type());
    }

    @Test
    void unknownPuzzleIsLockedWithoutPollutingFlags() {
        WorldState worldState = new WorldState();

        PuzzleResult result = engine().attempt("missing", "open", new PlayerService(new ItemService()), worldState);

        assertEquals(PuzzleResultType.LOCKED, result.type());
    }

    private PuzzleEngine engine() {
        return new PuzzleEngine(new PuzzleConfig().puzzleRegistry());
    }
}
