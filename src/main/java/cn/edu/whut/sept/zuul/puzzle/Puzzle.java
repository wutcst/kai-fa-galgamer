package cn.edu.whut.sept.zuul.puzzle;

public interface Puzzle {

    String id();

    String description();

    PuzzleResult attempt(String input, PuzzleContext context);

    boolean isSolved(PuzzleContext context);
}
