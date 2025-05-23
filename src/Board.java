package src;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.PriorityQueue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/* Container class for a game of numberlink */
public class Board {
    private final Location[][] grid;

    // Scheduled updates and whether each location has been edited
    public final PriorityQueue<Location> updatesScheduled;

    /**
     * @param filename Path to the JSON file containing the board data
     */
    public Board(String filename) {

        // Pull the board data from the specified file
        FileReader reader;
        try {
            reader = new FileReader(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            grid = null;
            updatesScheduled = null;
            // This is fatal; exit the program
            System.exit(1);
            return;
            // This return is unreachable, but it's here to satisfy the compiler
        }
        JSONObject obj = new JSONObject(new JSONTokener(reader));
        
        int size = obj.getInt("size");
        this.grid = new Location[size][size];
        this.updatesScheduled = new PriorityQueue<>(size * size, (a, b) -> {
            // For now, just say that all locations are equal; the update order doesn't affect the correctness of the result
            return 0;
        });

        // This is improper, but very temporary
        // TODO fixme when the Solver class exists
        GUI.boardSize = size;

        // Initialize the grid with blank locations
        // It's easiest to do it this way because of how we read the board file
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
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

            if (!GUI.COLOR_MAP.containsKey(key)) {
                System.err.println("Unknown color: " + key);
                continue;
            }

            // Create Location objects for the start and end points
            grid[startRow][startCol] = new Location(new Coordinate(startRow, startCol), GUI.COLOR_MAP.get(key), true);
            grid[endRow][endCol] = new Location(new Coordinate(endRow, endCol), GUI.COLOR_MAP.get(key), true);
        }

        // Initialize everything else to be blank
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != null) {
                    // Already did this one
                } else {
                    grid[i][j] = new Location(new Coordinate(i, j), null, false);
                }

                // Schedule an update for this location
                updatesScheduled.add(grid[i][j]);
            }
        }
    }

    public void updateAll() {
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
}