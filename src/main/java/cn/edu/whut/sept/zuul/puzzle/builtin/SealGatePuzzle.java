package cn.edu.whut.sept.zuul.puzzle.builtin;

import cn.edu.whut.sept.zuul.puzzle.Puzzle;
import cn.edu.whut.sept.zuul.puzzle.PuzzleContext;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResult;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResultType;

import java.util.Map;

public class SealGatePuzzle implements Puzzle {

    @Override
    public String id() {
        return "triple_seal_gate";
    }

    @Override
    public String description() {
        return "三重封印要求你证明记忆、秩序或灵魂已经站在你这边。";
    }

    @Override
    public PuzzleResult attempt(String input, PuzzleContext context) {
        if (!"open".equalsIgnoreCase(input == null ? "" : input.trim())) {
            return new PuzzleResult(PuzzleResultType.NEED_MORE_CLUE, "封印只回应 open 指令。", Map.of());
        }
        int shards = 0;
        if (context.worldState().getBoolean("memory_shard")) {
            shards++;
        }
        if (context.worldState().getBoolean("order_shard")) {
            shards++;
        }
        if (context.worldState().getBoolean("soul_shard")) {
            shards++;
        }
        boolean hasSoulBell = context.playerService().inventoryItems().contains("soul_bell");
        boolean brokenFate = context.worldState().getBoolean("altar_corrupted");
        if (shards >= 2 || hasSoulBell || brokenFate) {
            context.worldState().setBoolean("triple_seal_open", true);
            context.playerService().gainItem("nameless_badge");
            return new PuzzleResult(PuzzleResultType.SOLVED, "三重封印开启，无名徽章落入你的掌心。", Map.of());
        }
        return new PuzzleResult(PuzzleResultType.LOCKED, "封印仍然闭合。你需要至少两枚碎片、灵魂之铃或破碎命运路线。", Map.of("shards", shards));
    }

    @Override
    public boolean isSolved(PuzzleContext context) {
        return context.worldState().getBoolean("triple_seal_open");
    }
}
