package cn.edu.whut.sept.zuul.minigame;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkMatchPathTest {

    private final LinkMatchPathFinder finder = new LinkMatchPathFinder();

    @Test
    void supportsZeroOneAndTwoTurnPaths() {
        String[][] zeroTurn = {
                {"a", "a", null},
                {"b", null, "b"}
        };
        assertTrue(finder.findPath(zeroTurn, 0, 0, 0, 1).isPresent());

        String[][] oneTurn = {
                {"a", null, null},
                {null, null, "a"}
        };
        assertTrue(finder.findPath(oneTurn, 0, 0, 1, 2).isPresent());

        String[][] twoTurn = {
                {"a", "b", "a"},
                {null, "b", null},
                {null, null, null}
        };
        assertTrue(finder.findPath(twoTurn, 0, 0, 0, 2).isPresent());
    }

    @Test
    void rejectsDifferentSymbols() {
        String[][] different = {
                {"a", "b"}
        };
        assertFalse(finder.findPath(different, 0, 0, 0, 1).isPresent());
    }

    @Test
    void canUseOutsideBoundaryButCannotCrossTiles() {
        String[][] board = {
                {"a", "b", "a"},
                {"b", "b", "b"}
        };
        assertTrue(finder.findPath(board, 0, 0, 0, 2).isPresent());

        String[][] surrounded = {
                {"a", "b", null},
                {"b", null, "b"},
                {null, "b", "a"}
        };
        assertFalse(finder.findPath(surrounded, 0, 0, 2, 2).isPresent());
    }
}
