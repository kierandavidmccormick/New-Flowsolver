package src;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

// A note: If all the moves from a given square are deadly, that square is deadly, as is the whole branch of the tree that generated from it
// I could do something like: globally generate all first moves, then only work locally to try to prove subtrees as fatal
// More generally, if any square has only deadly subtrees, then that square is deadly, for as far up the tree as that applies

// Even better, we can do short-depth BFS to find a move that's forced, then apply that repeatedly

public class Solver {

    public static int BOARD_SIZE;
    public static int boardsCreated = 0;

    public static Board getRootBoard(String filename) {

        // Pull the board data from the specified file
        FileReader reader;
        try {
            reader = new FileReader(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            BOARD_SIZE = 0;
            // This is fatal; exit the program
            System.exit(1);
            return null;
            // This return is unreachable, but it's here to satisfy the compiler
        }
        JSONObject obj = new JSONObject(new JSONTokener(reader));
        
        BOARD_SIZE = obj.getInt("size");
        Location[][] grid = new Location[BOARD_SIZE][BOARD_SIZE];

        // Initialize the grid with blank locations
        // It's easiest to do it this way because of how we read the board file
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
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

            if (!GUI.COLOR_REVERSE_INDEX_MAP.containsKey(key)) {
                System.err.println("Unknown color: " + key);
                continue;
            }

            // Create Location objects for the start and end points
            grid[startRow][startCol] = new Location(new Coordinate(startRow, startCol), GUI.COLOR_REVERSE_INDEX_MAP.get(key), true);
            grid[endRow][endCol] = new Location(new Coordinate(endRow, endCol), GUI.COLOR_REVERSE_INDEX_MAP.get(key), true);
        }

        Board rootBoard = new Board(grid);

        // Initialize everything else to be blank
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (grid[i][j] != null) {
                    // Already did this one
                } else {
                    grid[i][j] = new Location(new Coordinate(i, j), null, false);
                }

                // Schedule an update for this location
                rootBoard.updatesScheduled.add(grid[i][j]);
            }
        }

        return rootBoard;
    }

    // Recursive DFS
    // Very dumb; only really works for easy problems
    public static Board solveBoard(Board board, int depth) {
        if (depth > BOARD_SIZE * BOARD_SIZE) {
            System.err.println("Too deep; giving up");
            // In theory, this shouldn't happen, but this is still useful just in case
            return null;
        }

        while (!board.getMovesQueue().isEmpty()) {
            Board newBoard = new Board(board);
            Move move = board.getMovesQueue().poll();
            try {
                newBoard.applyMove(move);
            } catch (InvalidMoveException e) {
                // Invalid move; skip this one
                continue;
            }

            boardsCreated++;
            // System.out.println("Created board " + boardsCreated + " depth " + depth + ": " + newBoard.simpleReadout());

            if (newBoard.isSolved()) {
                // System.out.println("Final move: " + move.toString());
                return newBoard;
            }

            Board deeperChild = solveBoard(newBoard, depth + 1);
            if (deeperChild != null) {
                // System.out.println("Move: " + move.toString());
                return deeperChild;
            }
        }
        return null;
    }

}