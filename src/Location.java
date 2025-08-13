package src;
import java.util.ArrayList;

/* Container for all the data associated with a single grid square */
public class Location {

    private final Coordinate coordinate;
    private final Boolean[] connections = new Boolean[4];   // Up, Down, Left, Right
    private Integer colorIndex; // Use Integer for nullability
    private final boolean isStart;
    private boolean edited = false; // Whether this location has been edited since the last check;

    /**
     * @param c Coordinate of this location
     * @param colorIndex Index of this location's color; null if color unresolved
     * @param isStart Whether this is a starting location
     */
    public Location(Coordinate c, Integer colorIndex, boolean isStart) {
        this.coordinate = c;
        this.colorIndex = colorIndex;
        this.isStart = isStart;
        for (int i = 0; i < connections.length; i++) {
            connections[i] = false;
        }
    }

    // Copy constructor
    public Location(Location other) {
        this.coordinate = other.coordinate;
        this.colorIndex = other.colorIndex;
        this.isStart = other.isStart;
        this.connections[0] = other.connections[0];
        this.connections[1] = other.connections[1];
        this.connections[2] = other.connections[2];
        this.connections[3] = other.connections[3];
    }

    // Updates self and other connections as necessary
    // Handles color updates, but not connection updates; those belong to the calling function
    // Assumes that location is valid to connect to
    public void connectTo(Coordinate direction, Location other, Board board) throws InvalidMoveException {

        int index = Coordinate.toIndex(direction);
        if (index == -1) {
            System.err.println("Invalid connection direction: " + direction);
            return;
        }

        connections[index] = true;
        other.connections[Coordinate.getOppositeIndex(index)] = true;
        other.edited = true;
        if (colorIndex != other.getColorIndex()) {
            other.updateColor(board);
        }
    }

    // Updates the color of this location and all connected uncolored locations
    public void updateColor(Board board) throws InvalidMoveException {

        for (int i = 0; i < connections.length; i++) {
            if (!connections[i]) {
                continue; 
            }

            Coordinate direction = Coordinate.DIRECTIONS[i];
            Location other = board.getLocation(coordinate.add(direction));
            
            if (colorIndex == null && other.getColorIndex() != null) {
                // Check again in case we already checked a direction with no color
                setColorIndex(other.getColorIndex());
                updateColor(board); 
                
                // There's an edge case where this forces a new connection
                if (getRemainingConnections() > 0) {
                    edited = true;
                    registerUpdate(board);
                }
            } else if (colorIndex != null && other.getColorIndex() == null) {
                // Propagate the color updates through the neighbor
                other.setColorIndex(colorIndex);
                other.updateColor(board);

                if (other.getRemainingConnections() > 0) {
                    other.edited = true;
                    other.registerUpdate(board);
                }
            } else if (colorIndex != null && other.getColorIndex() != null && !colorIndex.equals(other.getColorIndex())) {
                // This also implies an error, either an invalid move or an improperly-formatted board
                throw new InvalidMoveException("Color conflict", this);
            }
        }
    }

    // Makes any connections that can be guaranteed to be valid
    public void checkConnections(Board board) throws InvalidMoveException {

        // List of directions that cannot be connected to
        ArrayList<Coordinate> blockedDirections = new ArrayList<>();
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (isBlockingConnection(dir, board)) {
                blockedDirections.add(dir);
            }
        }

        boolean editedOther = false;

        if (4 - blockedDirections.size() == getRemainingConnections()) {
            // Exactly as many options left as connections that need to be made; make them
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                if (!blockedDirections.contains(dir)) {
                    // Connect to this direction
                    Location other = board.getLocation(coordinate.add(dir));
                    connectTo(dir, other, board);
                    editedOther = true;
                }
            }
        } else if (getRemainingConnections() == 0) {
            // Already have the maximum number of allowed connections
        } else if (4 - blockedDirections.size() > getRemainingConnections()) {
            // Check to see if there are any loose ends that can be connected
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                if (!blockedDirections.contains(dir) && !connections[Coordinate.toIndex(dir)]) {
                    Location other = board.getLocation(coordinate.add(dir));
                    if (this.colorIndex == other.getColorIndex() && this.colorIndex != null) {
                        connectTo(dir, other, board);
                        editedOther = true;
                    }
                }
            }
            
            // Can't prove any individual connection is correct
        } else {
            // Fewer options left than connections that need to be made; implies an invalid move or an improperly-formatted board
            throw new InvalidMoveException("Overconstrained location", this);
        }

        // Propagate connection checks to neighbors if this location changed its connections
        if (edited || editedOther) {
            // We have to check all connections; there are more possibilities than with colors
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                Coordinate newCoordinate = coordinate.add(dir);
                if (newCoordinate.isInBounds(board)) {
                    board.getLocation(newCoordinate).registerUpdate(board);
                }
            }
        }

        edited = false; // Reset the edited flag
    }

    // Number of possible connection configurations for this location
    // Guaranteed to be more than getRemainingConnections() by the move forcing logic
    // Seems like a reasonable heuristic for the best location to explore first
    public int countMoveCombinations(Board board) {
        int validDirs = 0;
        int remainingConnections = getRemainingConnections();
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (!isBlockingConnection(dir, board)) {
                validDirs++;
            }
        }
        
        if (remainingConnections == 1) {
            return validDirs;
        } else if (remainingConnections == 2) {
            // Combinations of 2 from validDirs
            return validDirs * (validDirs - 1) / 2;
        } else {
            // Something has gone wrong
            System.err.println("Unexpected number of remaining connections");
            return 99999;
        }
    }

    public ArrayList<Move> getValidMoves(Board board) {
        ArrayList<Move> validMoves = new ArrayList<>(4);
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (!isBlockingConnection(dir, board)) {
                validMoves.add(new Move(coordinate, dir, board));
            }
        }
        return validMoves;
    }

    public ArrayList<Move[]> getValidMoveCombinations(Board board) {
        ArrayList<Move[]> combinations = new ArrayList<>();
        ArrayList<Move> validMoves = getValidMoves(board);
        int comboSize = getRemainingConnections();

        if (comboSize == 1) {
            // Just return the valid moves
            combinations.addAll(validMoves.stream().map(move -> new Move[]{move}).toList());
        } else if (comboSize == 2) {
            // Combinations of 2 from validMoves
            for (int i = 0; i < validMoves.size(); i++) {
                for (int j = i + 1; j < validMoves.size(); j++) {
                    if (i == j) continue; // Skip same index
                    combinations.add(new Move[]{validMoves.get(i), validMoves.get(j)});
                }
            }
        } else {
            // Something has gone wrong
            System.err.println("Unexpected number of remaining connections");
        }
        
        return combinations;
    }

    // Determines whether it's valid to make a connection in the given direction
    // This has a bunch of finicky cases that really need 100% test coverage
    public boolean isBlockingConnection(Coordinate direction, Board board) {

        if (connections[Coordinate.toIndex(direction)]) {
            // Blocked because already connected
            return true;
        }

        Coordinate newCoordinate = coordinate.add(direction);
        if (!newCoordinate.isInBounds(board)) {
            // Blocked because out of bounds
            return true;
        }

        Location other = board.getLocation(newCoordinate);
        if (colorIndex != null && other.getColorIndex() != null && !colorIndex.equals(other.getColorIndex())) {
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
    // TODO make a test case for if color propagation makes a 2x2 block of one color
    public boolean isUTurn(Coordinate direction, Location other, Board board) {
        
        Coordinate leftOffset = Coordinate.leftTurn(direction);
        Coordinate leftCoordinate = coordinate.add(leftOffset);
        Coordinate leftCorner = leftCoordinate.add(direction);
        int dirIndex = Coordinate.toIndex(direction);
        int leftIndex = Coordinate.toIndex(leftOffset);

        // Check if this makes a U-turn to the left
        if (leftCoordinate.isInBounds(board) && leftCorner.isInBounds(board)) {

            // Check for a U-shaped connection of any color
            Location leftNeighbor = board.getLocation(leftCoordinate);
            if (connections[leftIndex] && (leftNeighbor.getConnections()[dirIndex] || other.getConnections()[leftIndex])) {
                return true;
            } else if (other.getConnections()[leftIndex] && leftNeighbor.getConnections()[dirIndex]) {
                return true;
            }

            // Check for a 2x2 square of the same color, regardless of connections
            Location leftCornerNeighbor = board.getLocation(other.getCoordinate().add(leftOffset));
            if (this.colorIndex != null && this.colorIndex.equals(other.getColorIndex())
                && this.colorIndex.equals(leftNeighbor.getColorIndex())
                && this.colorIndex.equals(leftCornerNeighbor.getColorIndex())) {
                return true;
            }
        }

        Coordinate rightOffset = Coordinate.rightTurn(direction);
        Coordinate rightCoordinate = coordinate.add(rightOffset);
        Coordinate rightCorner = rightCoordinate.add(direction);
        int rightIndex = Coordinate.toIndex(rightOffset);

        // Check if this makes a U-turn to the right
        if (rightCoordinate.isInBounds(board) && rightCorner.isInBounds(board)) {

            Location rightNeighbor = board.getLocation(rightCoordinate);
            if (connections[rightIndex] && (rightNeighbor.getConnections()[dirIndex] || other.getConnections()[rightIndex])) {
                return true;
            } else if (other.getConnections()[rightIndex] && rightNeighbor.getConnections()[dirIndex]) {
                return true;
            }

            Location rightCornerNeighbor = board.getLocation(other.getCoordinate().add(rightOffset));
            if (this.colorIndex != null && this.colorIndex.equals(other.getColorIndex())
                && this.colorIndex.equals(rightNeighbor.getColorIndex())
                && this.colorIndex.equals(rightCornerNeighbor.getColorIndex())) {
                return true;
            }
        }
        
        return false;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public Integer getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(Integer colorIndex) {
        this.colorIndex = colorIndex;
    }

    public Boolean[] getConnections() {
        return connections;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
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

    public int getRemainingConnections() {
        return getMaxConnections() - countConnections();
    }

    public boolean isStart() {
        return isStart;
    }

    // 1 for start and end locations, 2 for everywhere else
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

    @Override
    public String toString() {
        return "Loc:{" + coordinate.toString() + ", colorIndex=" + colorIndex + "}";
    }
}