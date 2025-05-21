package Test;
import org.junit.Test;

import src.Board;
import src.Location;
import src.Coordinate;
import src.GUI;

import static org.junit.Assert.assertTrue;

import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
public class ConnectionTest {
    @Test
    public void connectionTest() {
        Board board = new Board("boards/test1.json");

        Location topLeft = board.getLocation(0, 0);
        Location topRight = board.getLocation(0, 1);
        Location bottomLeft = board.getLocation(1, 0);

        topLeft.connectTo(Coordinate.RIGHT, topRight);
        assertEquals(topLeft.countConnections(), 1);
        assertEquals(topRight.countConnections(), 1);
        assertTrue(topLeft.getConnections()[3]);
        assertTrue(topRight.getConnections()[2]);

        bottomLeft.connectTo(Coordinate.UP, topLeft);
        assertEquals(bottomLeft.countConnections(), 1);
        assertEquals(topLeft.countConnections(), 2);
        assertTrue(bottomLeft.getConnections()[0]);
        assertTrue(topLeft.getConnections()[1]);
    }

    @Test
    public void colorConnectionTest() {
        Board board = new Board("boards/test1.json");

        Location topLeft = board.getLocation(0, 0);
        Location topRight = board.getLocation(0, 1);
        Location bottomLeft = board.getLocation(1, 0);
        Location bottomRight = board.getLocation(1, 1);
        topLeft.setColor(Color.RED);
        bottomRight.setColor(Color.GREEN);

        topLeft.connectTo(Coordinate.RIGHT, topRight);
        assertEquals(topLeft.getColor(), topRight.getColor());
        assertEquals(topRight.getColor(), Color.RED);

        bottomLeft.connectTo(Coordinate.RIGHT, bottomRight);
        assertEquals(bottomLeft.getColor(), bottomRight.getColor());
        assertEquals(bottomLeft.getColor(), Color.GREEN);
    }

    @Test
    public void colorPropagationTest1() {
        Board board = new Board("boards/test1.json");

        Location topLeft = board.getLocation(0, 0);
        Location topRight = board.getLocation(0, 1);
        Location bottomRight = board.getLocation(1, 1);

        bottomRight.setColor(Color.RED);
        topLeft.connectTo(Coordinate.RIGHT, topRight);
        topRight.connectTo(Coordinate.DOWN, bottomRight);

        assertEquals(topLeft.getColor(), Color.RED);
    }

    @Test
    public void colorPropagationTest2() {
        Board board = new Board("boards/test1.json");

        Location topLeft = board.getLocation(0, 0);
        Location topRight = board.getLocation(0, 1);
        Location bottomRight = board.getLocation(1, 1);

        bottomRight.setColor(Color.RED);
        topLeft.connectTo(Coordinate.RIGHT, topRight);
        bottomRight.connectTo(Coordinate.UP, topRight);

        assertEquals(topLeft.getColor(), Color.RED);
    }

    @Test
    public void blockingConnectionsTest() {
        GUI.loadColorsFromJson("data/colors.json");     // Not ideal, but fine as a workaround for now
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
        assertFalse(r0c0.isBlockingConnection(Coordinate.DOWN));
        assertFalse(r0c0.isBlockingConnection(Coordinate.RIGHT));

        // Middle left can connect up and down
        assertFalse(r1c0.isBlockingConnection(Coordinate.UP));
        assertFalse(r1c0.isBlockingConnection(Coordinate.DOWN));

        // Bottom left can connect up
        assertFalse(r2c0.isBlockingConnection(Coordinate.UP));

        // Center can connect up, down, left, and right
        assertFalse(r1c1.isBlockingConnection(Coordinate.UP));
        assertFalse(r1c1.isBlockingConnection(Coordinate.DOWN));
        assertFalse(r1c1.isBlockingConnection(Coordinate.LEFT));
        assertFalse(r1c1.isBlockingConnection(Coordinate.RIGHT));

        // Can't connect if already connected
        r0c0.connectTo(Coordinate.DOWN, r1c0);
        assertTrue(r0c0.isBlockingConnection(Coordinate.DOWN));
        assertTrue(r1c0.isBlockingConnection(Coordinate.UP));

        // Should still be able to connect...
        assertFalse(r1c1.isBlockingConnection(Coordinate.LEFT));
        assertFalse(r1c0.isBlockingConnection(Coordinate.RIGHT));
        // ... until it would make a u-turn
        r0c0.connectTo(Coordinate.RIGHT, r0c1);
        assertTrue(r1c1.isBlockingConnection(Coordinate.LEFT));
        assertTrue(r1c0.isBlockingConnection(Coordinate.RIGHT));
        assertTrue(r1c1.isBlockingConnection(Coordinate.UP));
        assertTrue(r0c1.isBlockingConnection(Coordinate.DOWN));
        
        // Can't connect off the edge of the board
        assertTrue(r0c2.isBlockingConnection(Coordinate.RIGHT));
        assertTrue(r0c2.isBlockingConnection(Coordinate.UP));

        // Can't connect if connections are already full
        assertTrue(r1c1.isBlockingConnection(Coordinate.UP));

        // Can't connect different colors
        assertTrue(r2c0.isBlockingConnection(Coordinate.RIGHT));
        assertTrue(r2c1.isBlockingConnection(Coordinate.LEFT));

    }

    @Test
    public void uTurnTest1() {
        Board board = new Board("boards/test1.json");

        Location topLeft = board.getLocation(0, 0);
        Location topRight = board.getLocation(0, 1);
        Location bottomLeft = board.getLocation(1, 0);
        Location bottomRight = board.getLocation(1, 1);

        topLeft.connectTo(Coordinate.RIGHT, topRight);
        bottomLeft.connectTo(Coordinate.RIGHT, bottomRight);

        assertTrue(topLeft.isUTurn(Coordinate.DOWN, bottomLeft));
        assertTrue(bottomLeft.isUTurn(Coordinate.UP, topLeft));
        assertTrue(topRight.isUTurn(Coordinate.DOWN, bottomRight));
        assertTrue(bottomRight.isUTurn(Coordinate.UP, topRight));
    }

    @Test
    public void uTurnTest2() {
        Board board = new Board("boards/test1.json");

        Location topLeft = board.getLocation(0, 0);
        Location topRight = board.getLocation(0, 1);
        Location bottomLeft = board.getLocation(1, 0);
        Location bottomRight = board.getLocation(1, 1);

        topLeft.connectTo(Coordinate.RIGHT, topRight);
        topLeft.connectTo(Coordinate.DOWN, bottomLeft);

        assertTrue(topRight.isUTurn(Coordinate.DOWN, bottomRight));
        assertTrue(bottomLeft.isUTurn(Coordinate.RIGHT, bottomRight));
    }

    // This is an end-to-end test rather than a unit test because this behavior is still fluid
    @Test
    public void connectionPropagationTest() {
        GUI.loadColorsFromJson("data/colors.json");
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
        assertEquals(r0c0.getColor(), GUI.COLOR_MAP.get("blue"));
        assertEquals(r0c1.getColor(), GUI.COLOR_MAP.get("blue"));
        assertEquals(r0c2.getColor(), GUI.COLOR_MAP.get("blue"));
        assertEquals(r1c0.getColor(), GUI.COLOR_MAP.get("blue"));
        assertEquals(r1c1.getColor(), GUI.COLOR_MAP.get("red"));
        assertEquals(r1c2.getColor(), GUI.COLOR_MAP.get("blue"));
        assertEquals(r2c0.getColor(), GUI.COLOR_MAP.get("blue"));
        assertEquals(r2c1.getColor(), GUI.COLOR_MAP.get("red"));
        assertEquals(r2c2.getColor(), GUI.COLOR_MAP.get("blue"));
    }

    // A larger but less carefully-checked test case
    // For now, this should fail due to an unresolved edge case in the connection logic
    @Test
    public void connectionPropagationTest2() {
        GUI.loadColorsFromJson("data/colors.json");
        Board board = new Board("boards/board2.json");

        board.updatesScheduled.clear(); // Clear the queue so we can test the propagation behavior
        board.updatesScheduled.add(board.getLocation(0, 0));
        board.updatesScheduled.add(board.getLocation(0, 6));
        board.updatesScheduled.add(board.getLocation(6, 6));
        board.updateAll();

        // In theory, this should be sufficient to prove the board is connected correctly
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                Location loc = board.getLocation(row, col);
                if (loc.isStart()) {
                    assertEquals(loc.countConnections(), 1);
                } else {
                    assertEquals(loc.countConnections(), 2);
                }
                assertNotNull(loc.getColor());
            }
        }
    }
}


