import java.io.FileNotFoundException;
import java.io.FileReader;

import org.json.JSONObject;
import org.json.JSONTokener;

/* Container class for a game of numberlink */
public class Board {
    private final Location[][] grid;
    private final int size;

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
            size = 0;
            // This is fatal; exit the program
            System.exit(1);
            return;
            // This return is unreachable, but it's here to satisfy the compiler
        }
        JSONObject obj = new JSONObject(new JSONTokener(reader));
        
        int size = obj.getInt("size");
        this.size = size;
        this.grid = new Location[size][size];

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
            JSONObject colorObj = flows.getJSONObject(key);
            int startCol = colorObj.getInt("Sx");
            int startRow = colorObj.getInt("Sy");
            int endCol = colorObj.getInt("Ex");
            int endRow = colorObj.getInt("Ey");

            // Create Location objects for the start and end points
            grid[startRow][startCol] = new Location(new Coordinate(startRow, startCol), this, GUI.COLOR_MAP.get(key), true);
            grid[endRow][endCol] = new Location(new Coordinate(endRow, endCol), this, GUI.COLOR_MAP.get(key), true);
        }

        // Initialize everything else to be blank
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != null) continue; // Already did these
                grid[i][j] = new Location(new Coordinate(i, j), this, null, false);
            }
        }
    }

    public Location getLocation(int row, int col) {
        return grid[row][col];
    }

    public Location getLocation(Coordinate coordinate) {
        return grid[coordinate.getRow()][coordinate.getCol()];
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    public boolean isInBounds(Coordinate coordinate) {
        return isInBounds(coordinate.getRow(), coordinate.getCol());
    }

    public int getSize() {
        return size;
    }
}