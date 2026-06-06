package cn.edu.whut.sept.zuul.puzzle.builtin;

import cn.edu.whut.sept.zuul.puzzle.Puzzle;
import cn.edu.whut.sept.zuul.puzzle.PuzzleContext;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResult;
import cn.edu.whut.sept.zuul.puzzle.PuzzleResultType;

import java.util.Map;

public class PasswordPuzzle implements Puzzle {

    @Override
    public String id() {
        return "mirror_number_door";
    }

    @Override
    public String description() {
        return "镜面要求你给出恰好的数字。";
    }

    @Override
    public PuzzleResult attempt(String input, PuzzleContext context) {
        if (!context.playerService().inventoryItems().contains("library_note")) {
            return new PuzzleResult(PuzzleResultType.NEED_MORE_CLUE, "镜面仍然浑浊。先去记忆图书馆寻找数字线索。", Map.of());
        }
        if ("21".equals(input == null ? "" : input.trim())) {
            context.worldState().setBoolean("mirror_door_open", true);
            return new PuzzleResult(PuzzleResultType.SOLVED, "镜面接受了 21，隐藏裂隙吐出镜面碎片。", Map.of("startEventId", "mirror_room_event"));
        }
        context.worldState().setBoolean("mirror_failed_once", true);
        return new PuzzleResult(PuzzleResultType.FAILED, "镜面拒绝了这个数字，但失败的倒影记住了你的尝试。", Map.of());
    }

    @Override
    public boolean isSolved(PuzzleContext context) {
        return context.worldState().getBoolean("mirror_door_open");
    }
}
