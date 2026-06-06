package cn.edu.whut.sept.zuul.minigame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

public class LinkMatchPathFinder {

    private static final int[][] DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    public Optional<List<Map<String, Integer>>> findPath(String[][] board, int rowA, int colA, int rowB, int colB) {
        if (!validTile(board, rowA, colA) || !validTile(board, rowB, colB) || rowA == rowB && colA == colB) {
            return Optional.empty();
        }
        if (!board[rowA][colA].equals(board[rowB][colB])) {
            return Optional.empty();
        }

        int rows = board.length;
        int cols = board[0].length;
        int startRow = rowA + 1;
        int startCol = colA + 1;
        int endRow = rowB + 1;
        int endCol = colB + 1;
        Queue<Node> queue = new ArrayDeque<>();
        Map<String, Integer> bestTurns = new HashMap<>();

        for (int direction = 0; direction < DIRECTIONS.length; direction++) {
            queue.add(new Node(startRow, startCol, direction, 0, List.of(point(rowA, colA))));
        }

        while (!queue.isEmpty()) {
            Node node = queue.remove();
            int nextRow = node.row + DIRECTIONS[node.direction][0];
            int nextCol = node.col + DIRECTIONS[node.direction][1];
            while (insideExpanded(nextRow, nextCol, rows, cols) && passable(board, nextRow, nextCol, endRow, endCol)) {
                List<Map<String, Integer>> path = appendPath(node.path, nextRow - 1, nextCol - 1);
                if (nextRow == endRow && nextCol == endCol) {
                    return Optional.of(path);
                }
                for (int nextDirection = 0; nextDirection < DIRECTIONS.length; nextDirection++) {
                    int turns = node.turns + (nextDirection == node.direction ? 0 : 1);
                    if (turns <= 2) {
                        String key = nextRow + "," + nextCol + "," + nextDirection;
                        if (bestTurns.getOrDefault(key, 99) > turns) {
                            bestTurns.put(key, turns);
                            queue.add(new Node(nextRow, nextCol, nextDirection, turns, path));
                        }
                    }
                }
                nextRow += DIRECTIONS[node.direction][0];
                nextCol += DIRECTIONS[node.direction][1];
            }
        }
        return Optional.empty();
    }

    public boolean hasAvailableMatch(String[][] board) {
        for (int r1 = 0; r1 < board.length; r1++) {
            for (int c1 = 0; c1 < board[r1].length; c1++) {
                if (board[r1][c1] == null) {
                    continue;
                }
                for (int r2 = r1; r2 < board.length; r2++) {
                    for (int c2 = 0; c2 < board[r2].length; c2++) {
                        if (r1 == r2 && c2 <= c1) {
                            continue;
                        }
                        if (board[r1][c1].equals(board[r2][c2]) && findPath(board, r1, c1, r2, c2).isPresent()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public String[][] deterministicReshuffle(String[][] board) {
        List<String> symbols = new ArrayList<>();
        for (String[] row : board) {
            for (String symbol : row) {
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        }
        symbols.sort(Comparator.naturalOrder());
        String[][] next = new String[board.length][board[0].length];
        int index = 0;
        for (int row = 0; row < next.length; row++) {
            for (int col = 0; col < next[row].length; col++) {
                if (board[row][col] != null) {
                    next[row][col] = symbols.get(index++ % symbols.size());
                }
            }
        }
        return next;
    }

    private boolean validTile(String[][] board, int row, int col) {
        return row >= 0 && row < board.length && col >= 0 && col < board[row].length && board[row][col] != null;
    }

    private boolean insideExpanded(int row, int col, int rows, int cols) {
        return row >= 0 && row <= rows + 1 && col >= 0 && col <= cols + 1;
    }

    private boolean passable(String[][] board, int expandedRow, int expandedCol, int endRow, int endCol) {
        if (expandedRow == endRow && expandedCol == endCol) {
            return true;
        }
        int row = expandedRow - 1;
        int col = expandedCol - 1;
        return row < 0 || col < 0 || row >= board.length || col >= board[row].length || board[row][col] == null;
    }

    private List<Map<String, Integer>> appendPath(List<Map<String, Integer>> path, int row, int col) {
        List<Map<String, Integer>> next = new ArrayList<>(path);
        next.add(point(row, col));
        return next;
    }

    private static Map<String, Integer> point(int row, int col) {
        return Map.of("row", row, "col", col);
    }

    private record Node(int row, int col, int direction, int turns, List<Map<String, Integer>> path) {
    }
}
