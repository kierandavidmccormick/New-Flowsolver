package src;
/**
 * Represents a coordinate in a 2D grid.
 */
public class Coordinate {
    private final int row;
    private final int col;

    public static final Coordinate UP = new Coordinate(-1, 0);
    public static final Coordinate DOWN = new Coordinate(1, 0);
    public static final Coordinate LEFT = new Coordinate(0, -1);
    public static final Coordinate RIGHT = new Coordinate(0, 1);
    public static final Coordinate[] DIRECTIONS = {UP, DOWN, LEFT, RIGHT};
    
    public static Coordinate getOpposite(Coordinate c) {
        if (c.equals(UP)) return DOWN;
        if (c.equals(DOWN)) return UP;
        if (c.equals(LEFT)) return RIGHT;
        if (c.equals(RIGHT)) return LEFT;
        System.err.println("Invalid coordinate: " + c);
        return null; // or throw an exception
    }

    public static Coordinate leftTurn(Coordinate c) {
        if (c.equals(UP)) return LEFT;
        if (c.equals(DOWN)) return RIGHT;
        if (c.equals(LEFT)) return DOWN;
        if (c.equals(RIGHT)) return UP;
        System.err.println("Invalid coordinate: " + c);
        return null; // or throw an exception
    }
    
    public static Coordinate rightTurn(Coordinate c) {
        if (c.equals(UP)) return RIGHT;
        if (c.equals(DOWN)) return LEFT;
        if (c.equals(LEFT)) return UP;
        if (c.equals(RIGHT)) return DOWN;
        System.err.println("Invalid coordinate: " + c);
        return null; // or throw an exception
    }

    public static int getOppositeIndex(int index) {
        if (index == 0) return 1;
        if (index == 1) return 0;
        if (index == 2) return 3;
        if (index == 3) return 2;
        System.err.println("Invalid index: " + index);
        return -1; // or throw an exception
    }

    public static int toIndex(Coordinate c) {
        if (c.equals(UP)) return 0;
        if (c.equals(DOWN)) return 1;
        if (c.equals(LEFT)) return 2;
        if (c.equals(RIGHT)) return 3;
        System.err.println("Invalid coordinate: " + c);
        return -1; // or throw an exception
    }

    public Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Coordinate add(Coordinate other) {
        return new Coordinate(this.row + other.row, this.col + other.col);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate other = (Coordinate) obj;
        return row == other.row && col == other.col;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }

    public boolean isInBounds() {
        return row >= 0 && col >= 0 && row < GUI.boardSize && col < GUI.boardSize;
    }
}