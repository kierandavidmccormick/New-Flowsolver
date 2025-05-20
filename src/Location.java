package src;
import java.awt.Color;
import java.util.ArrayList;

// TODO redo the update logic; color changes and connection changes can trigger each other
// TODO that *needs* a test suite first, though

/* Container for all the data associated with a single grid square */
public class Location {

    private final Coordinate coordinate;
    private final Boolean[] connections = new Boolean[4];   // Up, Down, Left, Right
    private final Board board;
    private Color color;
    private final boolean isStart;

    /**
     * @param c Coordinate of this location
     * @param board Board this location belongs to
     * @param color Color of this location; null if color unresolved
     * @param isStart Whether this is a starting location
     */
    public Location(Coordinate c, Board board, Color color, boolean isStart) {
        this.coordinate = c;
        this.board = board;
        this.color = color;
        this.isStart = isStart;
        for (int i = 0; i < connections.length; i++) {
            connections[i] = false;
        }
    }

    // Updates self and other connections as necessary
    // Handles color updates, but not connection updates; those belong to the calling function
    // Assumes that location is valid to connect to
    // TODO revise
    public void connectTo(Coordinate direction, Location other) {

        int index = Coordinate.toIndex(direction);
        if (index == -1) {
            System.err.println("Invalid connection direction: " + direction);
            return;
        }

        connections[index] = true;
        other.connections[Coordinate.getOppositeIndex(index)] = true;
        if (color != other.getColor()) {
            other.updateColor();
        }
    }

    // Updates the color of this location and all connected uncolored locations
    // TODO revise
    public void updateColor() {

        for (int i = 0; i < connections.length; i++) {
            if (connections[i]) {

                Coordinate direction = Coordinate.DIRECTIONS[i];
                Location other = board.getLocation(coordinate.add(direction));
                
                if (color == null && other.getColor() != null) {
                    // Check again in case we already checked a direction with no color
                    setColor(other.getColor());
                    updateColor(); 
                    
                    // There's an edge case where this forces a new connection
                    if (getMaxConnections() - countConnections() > 0) {
                        checkConnections(true);
                    }
                } else if (color != null && other.getColor() == null) {
                    // Propagate the color updates through the neighbor
                    other.setColor(color);
                    other.updateColor(); 
                    
                    if (other.getMaxConnections() - other.countConnections() > 0) {
                        other.checkConnections(true);
                    }
                } else if (color != null && other.getColor() != null && !color.equals(other.getColor())) {
                    // This shouldn't ever happen
                    System.err.println("Color conflict between " + this + " and " + other);
                }
            }
        }
    }

    // Makes any connections that can be guaranteed to be valid
    // TODO add tests
    // TODO revise this
    /** 
     * @param edited Whether this location was edited before its connection check was called; if so, it needs to propagate a check to all its neighbors
     */
    public void checkConnections(boolean edited) {

        // List of directions that cannot be connected to
        ArrayList<Coordinate> blockedDirections = new ArrayList<>();
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (isBlockingConnection(dir)) {
                blockedDirections.add(dir);
            }
        }

        ArrayList<Coordinate> directionsEdited = new ArrayList<>();

        if (countConnections() >= getMaxConnections()) {
            // Already have the maximum number of allowed connections
        } else if (4 - blockedDirections.size() > getMaxConnections() - countConnections()) {
            // Can't prove any individual connection is correct
        } else if (4 - blockedDirections.size() == getMaxConnections() - countConnections()) {
            // Exactly as many options left as connections that need to be made; make them
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                if (!blockedDirections.contains(dir)) {
                    // Connect to this direction
                    Location other = board.getLocation(coordinate.add(dir));
                    connectTo(dir, other);
                    directionsEdited.add(dir);
                }
            }
        } else {
            // Fewer options left than connections that need to be made; this is a bug
            System.err.println("Overconstrained location at " + coordinate);
            System.err.println("Blocked directions: " + blockedDirections);
            System.err.println("Connections: " + connections[0] + " " + connections[1] + " " + connections[2] + " " + connections[3]);
        }

        // Propagate connection checks to neighbors if this location changed its connections
        if (directionsEdited.size() > 0 || edited) {
            // We have to check all connections; there are more possibilities than with colors
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                Coordinate newCoordinate = coordinate.add(dir);
                if (board.isInBounds(newCoordinate)) {
                    board.getLocation(newCoordinate).checkConnections(directionsEdited.contains(dir));
                }
            }
        }
        
    }

    // Determines whether it's valid to make a connection in the given direction
    // This has a bunch of finicky cases that really need 100% test coverage
    public boolean isBlockingConnection(Coordinate direction) {

        if (connections[Coordinate.toIndex(direction)]) {
            // Blocked because already connected
            return true;
        }

        Coordinate newCoordinate = coordinate.add(direction);
        if (!board.isInBounds(newCoordinate)) {
            // Blocked because out of bounds
            return true;
        }

        Location other = board.getLocation(newCoordinate);
        if (color != null && other.getColor() != null && color != other.getColor()) {
            // Blocked because different colors
            return true;
        }

        if (other.countConnections() >= other.getMaxConnections()) {
            // Blocked because other location is already full
            return true;
        }
     
        if (isUTurn(direction, other)) {
            // Blocked because this would make a U-turn
            return true;
        }

        return false; 
    }

    // Checks a special and particularly complicated case that's forbidden, when a given path bends back on itself
    // Assumes location is correct and in bounds
    public boolean isUTurn(Coordinate direction, Location other) {
        Coordinate leftOffset = Coordinate.leftTurn(direction);
        Coordinate rightOffset = Coordinate.rightTurn(direction);
        int dirIndex = Coordinate.toIndex(direction);
        int leftIndex = Coordinate.toIndex(leftOffset);
        int rightIndex = Coordinate.toIndex(rightOffset);

        // Check if this makes a U-turn to the left
        if (board.isInBounds(coordinate.add(leftOffset)) && board.isInBounds(other.getCoordinate().add(leftOffset))) {
            
            Location leftNeighbor = board.getLocation(coordinate.add(leftOffset));

            if (connections[leftIndex] && (leftNeighbor.getConnections()[dirIndex] || other.getConnections()[leftIndex])) {
                return true;
            } else if (other.getConnections()[leftIndex] && leftNeighbor.getConnections()[dirIndex]) {
                return true;
            }
        }

        // Check if this makes a U-turn to the right
        if (board.isInBounds(coordinate.add(rightOffset)) && board.isInBounds(other.getCoordinate().add(rightOffset))) {

            Location rightNeighbor = board.getLocation(coordinate.add(rightOffset));

            if (connections[rightIndex] && (rightNeighbor.getConnections()[dirIndex] || other.getConnections()[rightIndex])) {
                return true;
            } else if (other.getConnections()[rightIndex] && rightNeighbor.getConnections()[dirIndex]) {
                return true;

            }
        }
        
        return false;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public Board getBoard() {
        return board;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Boolean[] getConnections() {
        return connections;
    }

    // Number of connections made so far
    public int countConnections() {
        int count = 0;
        for (Boolean connection : connections) {
            if (connection) {
                count++;
            }
        }
        return count;
    }

    public boolean isStart() {
        return isStart;
    }

    // 1 for start and end locations, 2 for everytwhere else
    private int getMaxConnections() {
        if (isStart) {
            return 1;
        } else {
            return 2;
        }
    }
}