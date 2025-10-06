package src;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Container for all the data associated with a single grid square on a given board
 * 
 * Potential improvements:
 * - Reduce the size of the Location class by packing all the booleans together into a single value
 * - Reduce the size of the Location class by storing colors as bytes instead of Integers
 * - Reduce duplication of coordinate data by having the Board class provide coordinates based on array indices, so those don't have to be stored in each Location
 */

public class Location {

    // Duplicate tracking of the coordinate at this location to make it easier to access its neighbors
    private final Coordinate coordinate;

    // Whether there is a connection in each direction; Up, Down, Left, Right. Duplicated in this location's neighbors
    private final boolean[] connections = new boolean[4];

    // This is an index because the actual colors are sometimes not resolved until later. Uses Integer for nullability
    private Integer colorIndex;

    // Whether this Location is a start or end point; start and end points can only have one connection as opposed to two
    private final boolean isStart;

    // Whether this location has been edited since its connections were last checked. If so, it needs to propagate connection checks to its neighbors.
    private boolean edited = false;

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

    /**
     * Copy constructor
     * @param other Location to copy
     */
    public Location(Location other) {
        this.coordinate = other.coordinate;
        this.colorIndex = other.colorIndex;
        this.isStart = other.isStart;
        this.connections[0] = other.connections[0];
        this.connections[1] = other.connections[1];
        this.connections[2] = other.connections[2];
        this.connections[3] = other.connections[3];
    }

    // TODO broader documentation on the connection logic

    /**
     * Connect this location to another location in the given direction; mark the "edited" flag as appropriate and resolve the color propagation
     * @param direction Direction to connect in
     * @param other Other location to connect to; must be adjacent in the given direction
     * @param board Board containing both locations
     * @throws InvalidMoveException if the connection would cause a conflict (e.g. color mismatch)
     */
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

    /**
     * Propagate color information through connected locations; if this location is uncolored, it will take the color of a connected location
     * @param board Board containing this location
     * @throws InvalidMoveException if a color conflict is detected (e.g. two connected locations have different colors)
     */
    public void updateColor(Board board) throws InvalidMoveException {

        // Check each connected direction
        for (int i = 0; i < connections.length; i++) {
            if (!connections[i]) {
                continue; 
            }

            Coordinate direction = Coordinate.DIRECTIONS[i];
            Location other = board.getLocation(coordinate.add(direction));
            
            if (colorIndex == null && other.getColorIndex() != null) {
                // Take the color of the connected location
                setColorIndex(other.getColorIndex());

                // Check again in case there's another location that can propagate the color further but that was already checked
                updateColor(board); 
                
                // There's an edge case where this forces a new connection, if there's only one possible candidate to connect to with a matching color
                if (getRemainingConnections() > 0) {
                    edited = true;
                    registerUpdate(board);
                }
            } else if (colorIndex != null && other.getColorIndex() == null) {
                // Propagate this location's color to the connected location
                other.setColorIndex(colorIndex);

                // Propagate the color updates through the neighbor to its neighbors, etc
                other.updateColor(board);

                // Similarly, this might force a new connection as well
                if (other.getRemainingConnections() > 0) {
                    other.edited = true;
                    other.registerUpdate(board);
                }
            } else if (colorIndex != null && other.getColorIndex() != null && !colorIndex.equals(other.getColorIndex())) {
                // It's an error if two connected locations have different colors 
                throw new InvalidMoveException("Color conflict", this);
            }
        }
    }

    /**
     * Check the connections for this location, making any that are known to be correct, and flagging connection checks as necessary
     * @param board The board containing this location
     * @throws InvalidMoveException If an invalid move is detected
     */
    public void checkConnections(Board board) throws InvalidMoveException {

        if (getRemainingConnections() == 0) {
            // Can't connect to anything else; exit early
            return;
        }

        int remainingConnections = getRemainingConnections();

        // Get a list of directions that cannot be connected to
        ArrayList<Coordinate> blockedDirections = new ArrayList<>();
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (isBlockingConnection(dir, board)) {
                blockedDirections.add(dir);
            }
        }

        // Flag for whether another location was edited as a result of this check
        boolean editedOther = false;

        if (4 - blockedDirections.size() == remainingConnections) {
            // If this location has to make exactly as many connections as there are open options, we know it has to connect to all of them
            
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                if (!blockedDirections.contains(dir)) {
                    Location other = board.getLocation(coordinate.add(dir));
                    connectTo(dir, other, board);
                    editedOther = true;
                }
            }
        } else if (4 - blockedDirections.size() > remainingConnections) {
            // Even if this location has more options than connections to make, it can still be proved that it has to connect to another of the same color, which can only be the "loose end" of a path coming from the other start location of that color

            for (Coordinate dir : Coordinate.DIRECTIONS) {
                if (!blockedDirections.contains(dir) && !connections[Coordinate.toIndex(dir)]) {
                    Location other = board.getLocation(coordinate.add(dir));
                    if (this.colorIndex == other.getColorIndex() && this.colorIndex != null) {
                        connectTo(dir, other, board);
                        editedOther = true;
                        break; // There can only ever be one of these
                    }
                }
            }
        } else {
            // Fewer options left than connections that need to be made; implies an invalid move or an improperly-formatted board
            throw new InvalidMoveException("Overconstrained location", this);
        }

        // Propagate updates to neighbors if any new connections were made
        if (edited || editedOther) {
            // We have to check all the directions because a new connection here might make it invalid for a neighbor to connect here, which might force that neighbor to connect somewhere else
            for (Coordinate dir : Coordinate.DIRECTIONS) {
                Coordinate newCoordinate = coordinate.add(dir);
                if (newCoordinate.isInBounds(board)) {
                    board.getLocation(newCoordinate).registerUpdate(board);
                }
            }
        }

        edited = false; // Reset the edited flag
    }

    /**
     * Count the number of valid move combinations from this location
     * @param board The board containing this location
     * @return If this location has X valid directions to connect to and Y remaining connections, returns X choose Y
     */
    public int countMoveCombinations(Board board) {
        int validDirs = 0;
        int remainingConnections = getRemainingConnections();
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (!isBlockingConnection(dir, board)) {
                validDirs++;
            }
        }
        
        // Calculate the combinations up to 2; that's all that's ever possible under the current setup
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

    /**
     * Get a list of Move objects corresponding to each valid direction this location can connect to
     * @param board The board containing this location
     * @return A list of valid moves from this location
     */
    public ArrayList<Move> getValidMoves(Board board) {
        ArrayList<Move> validMoves = new ArrayList<>(4);
        for (Coordinate dir : Coordinate.DIRECTIONS) {
            if (!isBlockingConnection(dir, board)) {
                validMoves.add(new Move(coordinate, dir, board));
            }
        }
        return validMoves;
    }

    /**
     * Get a list of all valid move combinations from this location, where a combination is an array of Move objects
     * @param board The board containing this location
     * @return An arraylist of arrays of moves, each array representing a valid combination of moves from this location
     */
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

    /**
     * Check if a connection in the given direction would be blocked for any reason; out of bounds, already connected, color mismatch, other location full, or would create a U-turn
     * @param direction The direction to check
     * @param board The board containing this location
     * @return True if the connection is blocked, false otherwise
     */
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

    /**
     * Check if connecting to another location in the given direction would create a U-turn, either by forming a U-shaped connection or a 2x2 square of the same color. This is a case that's forbidden in only some numberlink formulations, but its inclusion does not affect the overall complexity of the problem.
     * @param direction The direction to check
     * @param other The other location to check against
     * @param board The board containing the locations
     * @return True if the connection would create a U-turn, false otherwise
     */
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

    /**
     * Get the coordinate of this location
     * @return The coordinate of this location
     */
    public Coordinate getCoordinate() {
        return coordinate;
    }

    /**
     * Get the color index of this location; null if unresolved
     * @return The color index of this location
     */
    public Integer getColorIndex() {
        return colorIndex;
    }

    /**
     * Set the color index of this location; used for color propagation
     * @param colorIndex The new color index of this location
     */
    public void setColorIndex(Integer colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * Get the connections array, where each index corresponds to a direction (0=Up, 1=Down, 2=Left, 3=Right)
     * @return The connections array
     */
    public boolean[] getConnections() {
        return connections;
    }

    /**
     * Set the edited flag for this location; used to indicate that connection checks need to be propagated to this location's neighbors
     * @param edited
     */
    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    /**
     * Count the current number of connections this location has
     * @return The number of connections
     */
    public int countConnections() {
        int count = 0;
        for (Boolean connection : connections) {
            if (connection) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get the number of remaining connections this location must make
     * @return The number of remaining connections
     */
    public int getRemainingConnections() {
        return getMaxConnections() - countConnections();
    }

    /**
     * Check if this location is a start or end point
     * @return True if this location is a start or end point, false otherwise
     */
    public boolean isStart() {
        return isStart;
    }

    /**
     * Get the maximum number of connections this location can have; 1 if it's a start/end point, 2 otherwise
     * @return
     */
    private int getMaxConnections() {
        if (isStart) {
            return 1;
        } else {
            return 2;
        }
    }

    /**
     * Add this location to the board's update queue
     * @param board The board containing this location
     */
    private void registerUpdate(Board board) {
        if (!board.updatesScheduled.contains(this)) {
            board.updatesScheduled.add(this);
        }
    }

    /**
     * String representation of this location for debugging purposes
     */
    @Override
    public String toString() {
        return "Loc:{" + coordinate.toString() + ", colorIndex=" + colorIndex + "}";
    }

    /**
     * Check if this location has connections or color different from another location; used for rendering changes between subsequent boards
     * @param other The other location to compare to
     * @return True if the locations differ, false otherwise
     */
    public boolean getDiff(Location other) {

        if (Arrays.equals(connections, other.connections) && colorIndex == other.colorIndex) {
            return false;
        }
        return true;
    }
}