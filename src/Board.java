package src;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Represents the state of a Numberlink board.
 * Contains the grid of locations and methods for manipulating the board state.
 * 
 * There are a large number of special cases and rules that need to be handled when applying moves and updating the board state, done in order to make the largest possible number of inferences from a given move and prune the search space as much as possible.
 * 
 * It has a bunch of different constructors because I was indecisive about how I wanted to represent them initially. This will be revised at some point.
 * 
 * Potential improvements:
 * - Reduce the size of the Board class by having the width and height be provided by the grid array dimensions instead of storing them separately
 * - Reduce the size of the Board class by not keeping the updatesScheduled queue beyond the point in which it is needed
 * - Similarly, the moves queue
 * - Don't automatically duplicate the Location objects in the copy constructor; instead, create new ones on an as-needed basis
 * - Improve applyMoves to be more efficient when applying multiple moves at once
 * - Reduce the number of separate constructors and unify all the different representations of a board into a single one
 */

public class Board {
    // Dimensions of the board
    private final int width;
    private final int height;
    private final Location[][] grid;
    private final PriorityQueue<Move> moves = new PriorityQueue<>((a, b) -> {
        // Higher score goes first
        return Integer.compare(b.getScore(), a.getScore());
    });

    // Scheduled updates and whether each location has been edited
    public PriorityQueue<Location> updatesScheduled;

    /**
     * @param grid The grid of locations making up this board
     */
    public Board(Location[][] grid) {
        this.grid = grid;
        this.width = grid[0].length;
        this.height = grid.length;

        this.updatesScheduled = new PriorityQueue<>(width * height, (a, b) -> {
            // For now, just say that all locations are equal; the update order doesn't affect the correctness of the result
            return 0;
        });
    }

    /**
     * Copy constructor; creates a deep copy of another board
     * @param other The board to copy
     */
    public Board(Board other) {
        this(new Location[other.grid.length][other.grid[0].length]);

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                this.grid[i][j] = new Location(other.grid[i][j]);
            }
        }
    }

    /**
     * Create a board from an array of strings, where each string represents a row of the board
     * @param contents The array of strings representing the board
     */
    public Board(String[] contents) {
        this(getGridFromString(contents));
        scheduleAll();
    }

    /**
     * Convert an array of strings into a grid of Location objects
     * @param contents The array of strings representing the board
     * @return A 2D array of Location objects
     */
    private static Location[][] getGridFromString(String[] contents) {
        Location[][] grid = new Location[contents.length][contents[0].length()];

        for (int row = 0; row < contents.length; row++) {
            for (int col = 0; col < contents[0].length(); col++) {
                if (contents[row].charAt(col) == '.') {
                    grid[row][col] = new Location(new Coordinate(row, col), null, false);
                } else {
                    // Convert hex character to integer; index is char value - 'A'
                    int colorIndex = contents[row].charAt(col) - 'A';
                    grid[row][col] = new Location(new Coordinate(row, col), colorIndex, true);
                }
            }
        }
        return grid;
    }

    /**
     * Create a board by reading it from an archive file
     * @param archivePath The path to the archive file
     * @param index The index of the board within the archive
     */
    public Board(String archivePath, int index) {
        this(getStringFromArchive(archivePath, index));
    }

    /**
     * Read a board from an archive file
     * @param archivePath The path to the archive file
     * @param index The index of the board within the archive
     * @return An array of strings representing the board, or null if there was an error
     */
    private static String[] getStringFromArchive(String archivePath, int index) {
        // Open a file
        try (BufferedReader reader = new BufferedReader(new FileReader(archivePath))) {
            // Read and discard lines until we the start of the right board
            String line;
            int currentIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                    if (currentIndex == index) {
                        break; // Found the start of the right board
                    }
                    currentIndex++;
                }
            }

            // Split line and take the first part as the board size
            String[] parts = line.split(" ");
            // int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            String[] contents = new String[height];

            // Read the next 'height' lines as the board contents
            for (int i = 0; i < height; i++) {
                if ((line = reader.readLine()) != null) {
                    contents[i] = line;
                } else {
                    System.err.println("Not enough lines in file for board height " + height);
                    return null;
                }
            }

            return contents;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a board by reading it from a JSON file
     * @param filePath The path to the JSON file
     */
    public Board(String filePath) {
        this(getGridFromFile(filePath));
        scheduleAll();
    }

    /**
     * Read a board from a JSON file
     * @param filePath The path to the JSON file
     * @return A 2D array of Location objects representing the board, or null if there was an error
     */
    private static Location[][] getGridFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            JSONObject obj = new JSONObject(new JSONTokener(reader));
            int board_size = obj.getInt("size");    // These are all square
            Location[][] grid = new Location[board_size][board_size];

            // Initialize the grid with blank locations
            // It's easiest to do it this way because of how we read the board file
            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    grid[i][j] = null;
                }
            }

            // Populate the grid with Location objects for each of the starting positions
            JSONObject flows = obj.getJSONObject("flows");
            for (String key : flows.keySet()) {
                // Fetch the data for a given color
                // A "color" consists of an array of four ints, representing the start and end coordinates
                JSONArray colorObj = flows.getJSONArray(key);
                int startCol = colorObj.getInt(0);
                int startRow = colorObj.getInt(1);
                int endCol = colorObj.getInt(2);
                int endRow = colorObj.getInt(3);

                if (GUI.colors.getColorIndexByName(key) == null) {
                    System.err.println("Unknown color: " + key);
                    continue;
                }

                // Create Location objects for the start and end points
                grid[startRow][startCol] = new Location(new Coordinate(startRow, startCol), GUI.colors.getColorIndexByName(key), true);
                grid[endRow][endCol] = new Location(new Coordinate(endRow, endCol), GUI.colors.getColorIndexByName(key), true);
            }

            // Initialize everything else to be blank
            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    if (grid[i][j] != null) {
                        // Already did this one
                    } else {
                        grid[i][j] = new Location(new Coordinate(i, j), null, false);
                    }
                }
            }

            return grid;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Schedule all locations for an update
     */
    public void scheduleAll() {
        for (Location[] row : grid) {
            for (Location loc : row) {
                if (loc != null) {
                    updatesScheduled.add(loc);
                }
            }
        }
    }

    /**
     * Get a list of all possible moves from the current board state
     * @return A list of all possible moves
     */
    public ArrayList<Move> getMoves() {
        ArrayList<Move> moves = new ArrayList<>();

        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                Location loc = grid[i][j];
                if (loc.getRemainingConnections() <= 0) {
                    continue;
                }

                for (Coordinate direction : Coordinate.DIRECTIONS) {
                    if (loc.isBlockingConnection(direction, this)) {
                        continue;
                    }

                    Move move = new Move(loc.getCoordinate(), direction, this);
                    moves.add(move);
                }
            }
        }

        return moves;
    }

    /**
     * Get a list of all open locations (locations with at least one remaining connection)
     * @return A list of all open locations
     */
    public ArrayList<Location> getOpenLocations() {
        ArrayList<Location> openLocations = new ArrayList<>();

        for (Location[] row : grid) {
            for (Location loc : row) {
                if (loc.getRemainingConnections() > 0) {
                    openLocations.add(loc);
                }
            }
        }
        return openLocations;
    }

    /**
     * Apply a move to the board, updating the relevant locations and scheduling updates as necessary
     * @param move The move to apply
     * @throws InvalidMoveException If the move produces an invalid board state
     */
    public void applyMove(Move move) throws InvalidMoveException {
        Location start = getLocation(move.getStart());
        Coordinate direction = move.getDirection();
        Location other = getLocation(move.getStart().add(direction));

        start.connectTo(direction, other, this);
        start.setEdited(true);      // Necessary because it hasn't been through checkConnections yet
        updatesScheduled.add(start);
        updatesScheduled.add(other);
        updateAll();
    }

    /**
     * Apply a series of moves to the board, updating the relevant locations and scheduling updates as necessary
     * @param moves The moves to apply
     * @throws InvalidMoveException If any of the moves produce an invalid board state
     */
    public void applyMoves(Move[] moves) throws InvalidMoveException {
        for (Move move : moves) {
            applyMove(move);
        }
    }

    /**
     * Update the moves queue with all possible moves from the current board state
     */
    public void updateMoves() {
        moves.clear();
        moves.addAll(getMoves());
    }

    /**
     * Update all locations that are scheduled for an update
     * @throws InvalidMoveException If any of the updates produce an invalid board state
     */
    public void updateAll() throws InvalidMoveException {
        Location loc;
        while ((loc = updatesScheduled.poll()) != null) {
            loc.checkConnections(this);
        }
    }

    /**
     * Get a location at a specific row and column
     * @param row The row of the location
     * @param col The column of the location
     * @return The location at the specified row and column
     */
    public Location getLocation(int row, int col) {
        return grid[row][col];
    }

    /**
     * Get a location at a specific coordinate
     * @param coordinate The coordinate of the location
     * @return The location at the specified coordinate
     */
    public Location getLocation(Coordinate coordinate) {
        return grid[coordinate.getRow()][coordinate.getCol()];
    }

    /**
     * Get the grid of locations making up this board
     * @return The grid of locations
     */
    public Location[][] getGrid() {
        return grid;
    }

    /**
     * Get the moves priority queue
     * @return The moves priority queue
     */
    public PriorityQueue<Move> getMovesQueue() {
        return moves;
    }

    /**
     * Get the width of the board
     * @return The width of the board
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the board
     * @return The height of the board
     */
    public int getHeight() {
        return height;
    }

    /**
     * Check if the board is solved - all locations have zero remaining connections and have a color assigned
     * @return True if the board is solved, false otherwise
     */
    public boolean isSolved() {
        for (Location[] row : grid) {
            for (Location loc : row) {
                if (loc.getRemainingConnections() != 0 || loc.getColorIndex() == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get a boolean grid representing the differences between this board and another board
     * @param other The board to compare to
     * @return A 2D boolean array where true indicates a difference in the corresponding position between the two boards
     */
    public boolean[][] getDiff(Board other) {
        boolean[][] diff = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                diff[row][col] = grid[row][col].getDiff(other.grid[row][col]);
            }
        }
        return diff;
    }

    /**
     * Get a simple string representation of the board for debugging purposes
     * @return A string representation of the board
     */
    public String simpleReadout() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n   ");
        for (int i = 0; i < this.width; i++) {
            sb.append(i%10 + " ");
        }
        sb.append("\n\n");
        for (Location[] row : grid) {
            boolean[] vertConnections = new boolean[this.width];
            sb.append(row[0].getCoordinate().getRow()%10 + "| ");
            for (Location loc : row) {
                if (loc.getColorIndex() == null) {
                    sb.append(".");
                } else {
                    sb.append((char) ('A' + loc.getColorIndex()));
                }

                if (loc.getConnections()[3]) {
                    sb.append("-");
                } else {
                    sb.append(" ");
                }

                if (loc.getConnections()[1]) {
                    vertConnections[loc.getCoordinate().getCol()] = true;
                } else {
                    vertConnections[loc.getCoordinate().getCol()] = false;
                }
            }
            sb.append("|" + row[0].getCoordinate().getRow());
            sb.append("\n   ");
            for (boolean connected : vertConnections) {
                sb.append(connected ? "| " : "  ");
            }
            sb.append("\n");
        }
        sb.append("   ");
        for (int i = 0; i < this.width; i++) {
            sb.append(i%10 + " ");
        }
        sb.append("\n");
        return sb.toString();
    }
}