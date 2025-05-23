package src;
import java.awt.Color;
import java.util.ArrayList;

/* Container for all the data associated with a single grid square */
public class Location {

    private final Coordinate coordinate;
    private final Boolean[] connections = new Boolean[4];   // Up, Down, Left, Right
    private Color color;
    private final boolean isStart;
    private boolean edited = false; // Whether this location has been edited since the last check;

    /**
     * @param c Coordinate of this location
     * @param color Color of this location; null if color unresolved
     * @param isStart Whether this is a starting location
     */
    public Location(Coordinate c, Color color, boolean isStart) {
        this.coordinate = c;
        this.color = color;
        this.isStart = isStart;
        for (int i = 0; i < connections.length; i++) {
            connections[i] = false;
        }
    }

    // Updates self and other connections as necessary
    // Handles color updates, but not connection updates; those belong to the calling function
    // Assumes that location is valid to connect to
    public void connectTo(Coordinate direction, Location other, Board board) {

        int index = Coordinate.toIndex(direction);
        if (index == -1) {
            System.err.println("Invalid connection direction: " + direction);
            return;
        }

        connections[index] = true;
        other.connections[Coordinate.getOppositeIndex(index)] = true;
        other.edited = true;
        if (color != other.getColor()) {
            other.updateColor(board);
        }
    }

    // Updates the color of this location and all connected uncolored locations
    public void updateColor(Board board) {

        for (int i = 0; i < connections.length; i++) {
            if (!connections[i]) {
                continue; 
            }

            Coordinate direction = Coordinate.DIRECTIONS[i];
            Location other = board.getLocation(coordinate.add(direction));
            
            if (color == null && other.getColor() != null) {
                // Check again in case we already checked a direction with no color
                setColor(other.getColor());
                updateColor(board); 
                
                // There's an edge case where this forces a new connection
                if (getMaxConnections() - countConnections() > 0) {
                    edited = true;
                    registerUpdate(board);
                }
            } else if (color != null && other.getColor() == null) {
                // Propagate the color updates through the neighbor
                other.setColor(color);
                other.updateColor(board); 
                
                if (other.getMaxConnections() - other.countConnections() > 0) {
                    other.edited = true;
                    other.registerUpdate(board);
                }
            } else if (color != null && other.getColor() != null && !color.equals(other.getColor())) {
                // This shouldn't ever happen
                System.err.println("Color conflict between " + this + " and " + other);
            }
        }
    }

    // Makes any connections that can be guaranteed to be valid
    public void checkConnections(Board board) {

        // List of directions that cannot be connected to
        ArrayList<Coordinate> blockedDirections = new ArrayList<>();
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (isBlockingConnection(dir, board)) {
                blockedDirections.add(dir);
            }
        }

        boolean editedOther = false;

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
                    connectTo(dir, other, board);
                    editedOther = true;
                }
            }
        } else {
            // Fewer options left than connections that need to be made; this is a bug
            System.err.println("Overconstrained location at " + coordinate);
            System.err.println("Blocked directions: " + blockedDirections);
            System.err.println("Connections: " + connections[0] + " " + connections[1] + " " + connections[2] + " " + connections[3]);
        }

        // Propagate connection checks to neighbors if this location changed its connections
        if (edited || editedOther) {
            // We have to check all connections; there are more possibilities than with colors
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                Coordinate newCoordinate = coordinate.add(dir);
                if (newCoordinate.isInBounds()) {
                    board.getLocation(newCoordinate).registerUpdate(board);
                }
            }
        }

        edited = false; // Reset the edited flag
    }

    // Determines whether it's valid to make a connection in the given direction
    // This has a bunch of finicky cases that really need 100% test coverage
    public boolean isBlockingConnection(Coordinate direction, Board board) {

        if (connections[Coordinate.toIndex(direction)]) {
            // Blocked because already connected
            return true;
        }

        Coordinate newCoordinate = coordinate.add(direction);
        if (!newCoordinate.isInBounds()) {
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
     
        if (isUTurn(direction, other, board)) {
            // Blocked because this would make a U-turn
            return true;
        }

        return false; 
    }

    // Checks a special and particularly complicated case that's forbidden, when a given path bends back on itself
    // Assumes location is correct and in bounds
    public boolean isUTurn(Coordinate direction, Location other, Board board) {
        Coordinate leftOffset = Coordinate.leftTurn(direction);
        Coordinate rightOffset = Coordinate.rightTurn(direction);
        int dirIndex = Coordinate.toIndex(direction);
        int leftIndex = Coordinate.toIndex(leftOffset);
        int rightIndex = Coordinate.toIndex(rightOffset);

        // Check if this makes a U-turn to the left
        if (coordinate.add(leftOffset).isInBounds() && other.getCoordinate().add(leftOffset).isInBounds()) {

            Location leftNeighbor = board.getLocation(coordinate.add(leftOffset));

            if (connections[leftIndex] && (leftNeighbor.getConnections()[dirIndex] || other.getConnections()[leftIndex])) {
                return true;
            } else if (other.getConnections()[leftIndex] && leftNeighbor.getConnections()[dirIndex]) {
                return true;
            }
        }

        // Check if this makes a U-turn to the right
        if (coordinate.add(rightOffset).isInBounds() && other.getCoordinate().add(rightOffset).isInBounds()) {

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

    private void registerUpdate(Board board) {
        if (!board.updatesScheduled.contains(this)) {
            board.updatesScheduled.add(this);
        }
    }
}