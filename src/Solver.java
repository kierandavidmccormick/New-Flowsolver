package src;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    // public static Board solveBoard(Board board, int depth) {
    //     if (depth > BOARD_SIZE * BOARD_SIZE) {
    //         System.err.println("Too deep; giving up");
    //         // In theory, this shouldn't happen, but this is still useful just in case
    //         return null;
    //     }

    //     while (!board.getMovesQueue().isEmpty()) {
    //         Board newBoard = new Board(board);
    //         Move move = board.getMovesQueue().poll();
    //         try {
    //             newBoard.applyMove(move);
    //             newBoard.updateMoves();
    //         } catch (InvalidMoveException e) {
    //             // Invalid move; skip this one
    //             continue;
    //         }

    //         boardsCreated++;

    //         if (newBoard.isSolved()) {
    //             return newBoard;
    //         }

    //         Board deeperChild = solveBoard(newBoard, depth + 1);
    //         if (deeperChild != null) {
    //             return deeperChild;
    //         }
    //     }
    //     return null;
    // }

    public static Board solveBoardBetter(Board board) {
        while (true) {
            // Iterative Deepening Search (IDS) with a depth limit of 4
            Move forcedMove = null;
            for (int depthLimit = 1; depthLimit <= 4; depthLimit++) {
                // Try to find a forced move
                forcedMove = findForcedMove(board, depthLimit);
                if (forcedMove != null) {
                    break;
                }
            }
            if (forcedMove == null) {
                // No forced move found; return the current board
                return board;
            }

            try {
                board.applyMove(forcedMove);
            } catch (InvalidMoveException e) {
                // This shouldn't happen, but if it does, just return the current board
                System.err.println("Invalid move: " + e.getMessage());
                return board;
            }
        }
    }

    public static Move findForcedMove(Board board, int depthLimit) {

        ArrayList<Location> openLocations = board.getOpenLocations();
        HashMap<Location, ArrayList<Move>> candidates = new HashMap<>();
        for (Location loc : openLocations) {
            ArrayList<Move> moves = loc.getValidMoves(board);
            if (moves.size() == 1) {
                // Only one move available; this is a forced move
                // Unlikely, but possible; usually the local move forcing rules preempt this, but it's possible I've missed an edge case somewhere
                return moves.get(0);
            } else if (moves.size() == 0) {
                // Something has gone wrong; this location should not be open if there are no valid moves
                System.err.println("No valid moves for location " + loc.getCoordinate());
                return null;
            }

            candidates.put(loc, moves);
        }

        sortLocationsByConnections(openLocations, board);

        // Check all candidates and see if any of them trigger an InvalidMoveException; if so, remove it as a candidate
        for (Location loc : openLocations) {
            ArrayList<Move> moves = new ArrayList<>(candidates.get(loc));      // Make a copy to avoid concurrent modification issues
            for (Move move : moves) {
                Board testBoard = new Board(board);
                try {
                    testBoard.applyMove(move);
                } catch (InvalidMoveException e) {
                    // This move is invalid; remove it from the candidates
                    candidates.get(loc).remove(move);
                    continue;
                }

                if (isDeadly(testBoard, depthLimit)) {
                    // This move leads to a deadly board; remove it from the candidates
                    candidates.get(loc).remove(move);
                }
            }

            if (candidates.get(loc).size() == 1) {
                // If there's only one move left, return it
                return candidates.get(loc).get(0);
            } else if (candidates.get(loc).size() == 0) {
                // If there are no moves left, we shouldn't be here
                System.err.println("Location " + loc.getCoordinate() + " has no valid moves");
                return null;
            }
        }

        return null;
    }

    // A board is deadly if there are no valid moves from an open location, or if all valid moves from an open location lead to a deadly board
    public static boolean isDeadly(Board board, int depthLimit) {
        if (depthLimit == 0) {
            // Too deep; give up
            return false;
        }

        // Get all open locations on the board and sort to put the most promising ones first
        ArrayList<Location> openLocations = board.getOpenLocations();
        sortLocationsByConnections(openLocations, board);

        // Get the moves for each location
        HashMap<Location, ArrayList<Move>> validMoves = new HashMap<>();
        for (Location loc : openLocations) {
            validMoves.put(loc, loc.getValidMoves(board));
        }
        
        // Check all open locations for forced moves
        for (Location loc : openLocations) {
            ArrayList<Move> moves = validMoves.get(loc);
            if (moves == null || moves.size() == 0) {
                return true; // No valid moves from this location, so this move is deadly
            }

            boolean deadly = true;
            // Check to see if there is at least one valid, non-deadly move
            for (Move move : moves) {
                Board newBoard = new Board(board);
                try {
                    newBoard.applyMove(move);
                } catch (InvalidMoveException e) {
                    // Invalid move; skip this one
                    continue;
                }

                if (!isDeadly(newBoard, depthLimit - 1)) {
                    deadly = false;
                }
            }
            if (deadly) {
                // All moves from this location lead to a deadly board
                return true;
            }
        }
        return false;
    }

    // It's important for performance to pick good locations first
    // I make this a total ordering so the search is deterministic
    public static void sortLocationsByConnections(List<Location> locations, Board board) {
        locations.sort((a, b) -> {
            // Sort by the number of move combinations available at each location
            int aCount = a.countMoveCombinations(board);
            int bCount = b.countMoveCombinations(board);

            // If the counts are not equal, sort by that
            if (aCount != bCount) {
                return Integer.compare(aCount, bCount);
            }

            // If the counts are equal, sort by the xcoordinate, then by the ycoordinate
            if (a.getCoordinate().getRow() != b.getCoordinate().getRow()) {
                return Integer.compare(a.getCoordinate().getRow(), b.getCoordinate().getRow());
            } else {
                return Integer.compare(a.getCoordinate().getCol(), b.getCoordinate().getCol());
            }
        });
    }
}