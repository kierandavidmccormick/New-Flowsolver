package Test;
import org.junit.Test;

import src.Board;
import src.Location;
import src.Coordinate;
import src.GUI;
import src.InvalidMoveException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ConnectionTest {
    @Test
    public void connectionTest() {
        try {
            Board board = new Board("boards/test1.json");

            Location topLeft = board.getLocation(0, 0);
            Location topRight = board.getLocation(0, 1);
            Location bottomLeft = board.getLocation(1, 0);

            topLeft.connectTo(Coordinate.RIGHT, topRight, board);
            assertEquals(topLeft.countConnections(), 1);
            assertEquals(topRight.countConnections(), 1);
            assertTrue(topLeft.getConnections()[3]);
            assertTrue(topRight.getConnections()[2]);

            bottomLeft.connectTo(Coordinate.UP, topLeft, board);
            assertEquals(bottomLeft.countConnections(), 1);
            assertEquals(topLeft.countConnections(), 2);
            assertTrue(bottomLeft.getConnections()[0]);
            assertTrue(topLeft.getConnections()[1]);
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    @Test
    public void colorConnectionTest() {
        try {
            Board board = new Board("boards/test1.json");

            Location topLeft = board.getLocation(0, 0);
            Location topRight = board.getLocation(0, 1);
            Location bottomLeft = board.getLocation(1, 0);
            Location bottomRight = board.getLocation(1, 1);
            topLeft.setColorIndex(1);
            bottomRight.setColorIndex(2);

            topLeft.connectTo(Coordinate.RIGHT, topRight, board);
            assertEquals(topLeft.getColorIndex(), topRight.getColorIndex());
            assertEquals(topRight.getColorIndex(), Integer.valueOf(1));

            bottomLeft.connectTo(Coordinate.RIGHT, bottomRight, board);
            assertEquals(bottomLeft.getColorIndex(), bottomRight.getColorIndex());
            assertEquals(bottomLeft.getColorIndex(), Integer.valueOf(2));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    @Test
    public void colorPropagationTest1() {
        try {
            Board board = new Board("boards/test1.json");

            Location topLeft = board.getLocation(0, 0);
            Location topRight = board.getLocation(0, 1);
            Location bottomRight = board.getLocation(1, 1);

            bottomRight.setColorIndex(1);
            topLeft.connectTo(Coordinate.RIGHT, topRight, board);
            topRight.connectTo(Coordinate.DOWN, bottomRight, board);

            assertEquals(topLeft.getColorIndex(), Integer.valueOf(1));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    @Test
    public void colorPropagationTest2() {
        try {
            Board board = new Board("boards/test1.json");

            Location topLeft = board.getLocation(0, 0);
            Location topRight = board.getLocation(0, 1);
            Location bottomRight = board.getLocation(1, 1);

            bottomRight.setColorIndex(1);
            topLeft.connectTo(Coordinate.RIGHT, topRight, board);
            bottomRight.connectTo(Coordinate.UP, topRight, board);

            assertEquals(topLeft.getColorIndex(), Integer.valueOf(1));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    @Test
    public void blockingConnectionsTest() {
        try {
            Board board = new Board("boards/test2.json");

            Location r0c0 = board.getLocation(0, 0);
            Location r0c1 = board.getLocation(0, 1);
            Location r0c2 = board.getLocation(0, 2);
            Location r1c0 = board.getLocation(1, 0);
            Location r1c1 = board.getLocation(1, 1);
            // Location r1c2 = board.getLocation(1, 2);
            Location r2c0 = board.getLocation(2, 0);
            Location r2c1 = board.getLocation(2, 1);
            // Location r2c2 = board.getLocation(2, 2);

            // Top left can connect to the right and down
            assertFalse(r0c0.isBlockingConnection(Coordinate.DOWN, board));
            assertFalse(r0c0.isBlockingConnection(Coordinate.RIGHT, board));

            // Middle left can connect up and down
            assertFalse(r1c0.isBlockingConnection(Coordinate.UP, board));
            assertFalse(r1c0.isBlockingConnection(Coordinate.DOWN, board));

            // Bottom left can connect up
            assertFalse(r2c0.isBlockingConnection(Coordinate.UP, board));

            // Center can connect up, down, left, and right
            assertFalse(r1c1.isBlockingConnection(Coordinate.UP, board));
            assertFalse(r1c1.isBlockingConnection(Coordinate.DOWN, board));
            assertFalse(r1c1.isBlockingConnection(Coordinate.LEFT, board));
            assertFalse(r1c1.isBlockingConnection(Coordinate.RIGHT, board));

            // Can't connect if already connected
            r0c0.connectTo(Coordinate.DOWN, r1c0, board);
            assertTrue(r0c0.isBlockingConnection(Coordinate.DOWN, board));
            assertTrue(r1c0.isBlockingConnection(Coordinate.UP, board));

            // Should still be able to connect...
            assertFalse(r1c1.isBlockingConnection(Coordinate.LEFT, board));
            assertFalse(r1c0.isBlockingConnection(Coordinate.RIGHT, board));
            // ... until it would make a u-turn
            r0c0.connectTo(Coordinate.RIGHT, r0c1, board);
            assertTrue(r1c1.isBlockingConnection(Coordinate.LEFT, board));
            assertTrue(r1c0.isBlockingConnection(Coordinate.RIGHT, board));
            assertTrue(r1c1.isBlockingConnection(Coordinate.UP, board));
            assertTrue(r0c1.isBlockingConnection(Coordinate.DOWN, board));

            // Can't connect off the edge of the board
            assertTrue(r0c2.isBlockingConnection(Coordinate.RIGHT, board));
            assertTrue(r0c2.isBlockingConnection(Coordinate.UP, board));

            // Can't connect if connections are already full
            assertTrue(r1c1.isBlockingConnection(Coordinate.UP, board));

            // Can't connect different colors
            assertTrue(r2c0.isBlockingConnection(Coordinate.RIGHT, board));
            assertTrue(r2c1.isBlockingConnection(Coordinate.LEFT, board));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    @Test
    public void uTurnTest1() {
        try {
            Board board = new Board("boards/test1.json");

            Location topLeft = board.getLocation(0, 0);
            Location topRight = board.getLocation(0, 1);
            Location bottomLeft = board.getLocation(1, 0);
            Location bottomRight = board.getLocation(1, 1);

            topLeft.connectTo(Coordinate.RIGHT, topRight, board);
            bottomLeft.connectTo(Coordinate.RIGHT, bottomRight, board);

            assertTrue(topLeft.isUTurn(Coordinate.DOWN, bottomLeft, board));
            assertTrue(bottomLeft.isUTurn(Coordinate.UP, topLeft, board));
            assertTrue(topRight.isUTurn(Coordinate.DOWN, bottomRight, board));
            assertTrue(bottomRight.isUTurn(Coordinate.UP, topRight, board));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    @Test
    public void uTurnTest2() {
        try {
            Board board = new Board("boards/test1.json");

            Location topLeft = board.getLocation(0, 0);
            Location topRight = board.getLocation(0, 1);
            Location bottomLeft = board.getLocation(1, 0);
            Location bottomRight = board.getLocation(1, 1);

            topLeft.connectTo(Coordinate.RIGHT, topRight, board);
            topLeft.connectTo(Coordinate.DOWN, bottomLeft, board);

            assertTrue(topRight.isUTurn(Coordinate.DOWN, bottomRight, board));
            assertTrue(bottomLeft.isUTurn(Coordinate.RIGHT, bottomRight, board));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    // This is an end-to-end test rather than a unit test because this behavior is still fluid
    @Test
    public void connectionPropagationTest() {
        try {
            Board board = new Board("boards/test2.json");

            Location r0c0 = board.getLocation(0, 0);
            Location r0c1 = board.getLocation(0, 1);
            Location r0c2 = board.getLocation(0, 2);
            Location r1c0 = board.getLocation(1, 0);
            Location r1c1 = board.getLocation(1, 1);
            Location r1c2 = board.getLocation(1, 2);
            Location r2c0 = board.getLocation(2, 0);
            Location r2c1 = board.getLocation(2, 1);
            Location r2c2 = board.getLocation(2, 2);

            board.updatesScheduled.clear(); // Clear the queue so we can test the propagation behavior
            board.updatesScheduled.add(r0c0);
            board.updateAll();

            assertEquals(r0c0.countConnections(), 2);
            assertEquals(r0c1.countConnections(), 2);
            assertEquals(r0c2.countConnections(), 2);
            assertEquals(r1c0.countConnections(), 2);
            assertEquals(r1c1.countConnections(), 1);
            assertEquals(r1c2.countConnections(), 2);
            assertEquals(r2c0.countConnections(), 1);
            assertEquals(r2c1.countConnections(), 1);
            assertEquals(r2c2.countConnections(), 1);

            // Need to reference the GUI.COLOR_MAP because those colors are slightly nonstandard
            assertEquals(r0c0.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
            assertEquals(r0c1.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
            assertEquals(r0c2.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
            assertEquals(r1c0.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
            assertEquals(r1c1.getColorIndex(), GUI.colors.getColorIndexByName("red"));
            assertEquals(r1c2.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
            assertEquals(r2c0.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
            assertEquals(r2c1.getColorIndex(), GUI.colors.getColorIndexByName("red"));
            assertEquals(r2c2.getColorIndex(), GUI.colors.getColorIndexByName("blue"));
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }

    // A larger but less carefully-checked test case
    @Test
    public void connectionPropagationTest2() {
        try {
            Board board = new Board("boards/board2.json");

            board.updatesScheduled.clear(); // Clear the queue so we can test the propagation behavior
            board.updatesScheduled.add(board.getLocation(0, 0));
            board.updatesScheduled.add(board.getLocation(0, 6));
            board.updatesScheduled.add(board.getLocation(6, 6));
            board.updateAll();

            // In theory, this should be sufficient to prove the board is connected correctly
            for (int row = 0; row < board.getHeight(); row++) {
                for (int col = 0; col < board.getWidth(); col++) {
                    Location loc = board.getLocation(row, col);
                    if (loc.isStart()) {
                        assertEquals(loc.countConnections(), 1);
                    } else {
                        assertEquals(loc.countConnections(), 2);
                    }
                    assertNotNull(loc.getColorIndex());
                }
            }
        } catch (InvalidMoveException e) {
            assertTrue(false);
        }
    }
}


