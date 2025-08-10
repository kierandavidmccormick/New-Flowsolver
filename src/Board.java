package src;
import java.util.ArrayList;
import java.util.PriorityQueue;

/* Container class for a game of numberlink */
public class Board {
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
     * @param filename Path to the JSON file containing the board data
     */
    public Board(Location[][] grid, int width, int height) {
        this.grid = grid;
        this.width = width;
        this.height = height;

        this.updatesScheduled = new PriorityQueue<>(width * height, (a, b) -> {
            // For now, just say that all locations are equal; the update order doesn't affect the correctness of the result
            return 0;
        });
    }

    // Copy constructor
    public Board(Board other) {
        this(new Location[other.grid.length][other.grid[0].length], other.width, other.height);

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                this.grid[i][j] = new Location(other.grid[i][j]);
            }
        }
    }

    public Board(String[] contents, int width, int height) {
        this(getGridFromString(contents, width, height), width, height);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                this.updatesScheduled.add(this.grid[row][col]);
            }
        }
    }

    public static Location[][] getGridFromString(String[] contents, int width, int height) {
        Location[][] grid = new Location[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
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

    public ArrayList<Location> getOpenLocations() {
        // Returns a list of locations that have at least one open connection
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

    public void updateMoves() {
        moves.clear();
        moves.addAll(getMoves());
    }

    public void updateAll() throws InvalidMoveException {
        Location loc;
        while ((loc = updatesScheduled.poll()) != null) {
            loc.checkConnections(this);
        }
    }

    public Location getLocation(int row, int col) {
        return grid[row][col];
    }

    public Location getLocation(Coordinate coordinate) {
        return grid[coordinate.getRow()][coordinate.getCol()];
    }

    public Location[][] getGrid() {
        return grid;
    }

    public PriorityQueue<Move> getMovesQueue() {
        return moves;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

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

    // Returns an ascii-art representation of the board for debug purposes
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