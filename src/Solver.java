package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// A note: If all the moves from a given square are deadly, that square is deadly, as is the whole branch of the tree that generated from it
// I could do something like: globally generate all first moves, then only work locally to try to prove subtrees as fatal
// More generally, if any square has only deadly subtrees, then that square is deadly, for as far up the tree as that applies

// Even better, we can do short-depth BFS to find a move that's forced, then apply that repeatedly

public class Solver {
    public static int boardsCreated = 0;

    public static ArrayList<Board> solveBoard(Board board) {
        boardsCreated = 0;
        ArrayList<Board> solution = new ArrayList<>();
        solution.add(board);

        while (true) {
            // Iterative Deepening Search (IDS) with a depth limit of 4
            Move[] forcedMoves = null;
            int depthLimitAt = 0;
            for (int depthLimit = 0; depthLimit <= 4; depthLimit++) {

                if (depthLimit >= 0) {
                    System.out.println("Searching for forced moves at depth " + depthLimit + " - Created " + boardsCreated + " boards so far");
                }

                // Try to find a forced move
                forcedMoves = findForcedMove(solution.get(solution.size() - 1), depthLimit);
                if (forcedMoves != null || board.isSolved()) {
                    depthLimitAt = depthLimit;
                    break;
                }
            }
            if (forcedMoves == null) {
                // No forced move found; return the current solution
                return solution;
            }

            try {
                Board newBoard = new Board(solution.get(solution.size() - 1));
                for (Move forcedMove : forcedMoves) {
                    newBoard.applyMove(forcedMove);
                }
                solution.add(newBoard);
                boardsCreated++;
                if (depthLimitAt > 0) {
                    System.out.println("Found forced move: at d=" + depthLimitAt + " " + forcedMoves[0] + (forcedMoves.length > 1 ? " and " + forcedMoves[1] : "") + " - Created " + boardsCreated + " boards so far");
                    System.out.println(board.simpleReadout());
                } else if (depthLimitAt == 0) {
                    System.out.println("Found forced move: at d=" + depthLimitAt + " " + forcedMoves[0] + (forcedMoves.length > 1 ? " and " + forcedMoves[1] : "") + " - Created " + boardsCreated + " boards so far");
                    System.out.println(board.simpleReadout());
                }
            } catch (InvalidMoveException e) {
                // This shouldn't happen, but if it does, just return the current solution
                System.err.println("Invalid move: " + e.getMessage());
                return solution;
            }
        }
    }

    // Returns an array because it's possible for one location to have multiple forced moves (e.g. a corner)
    public static Move[] findForcedMove(Board board, int depthLimit) {

        ArrayList<Location> openLocations = board.getOpenLocations();
        HashMap<Location, ArrayList<Move>> candidates = new HashMap<>();
        for (Location loc : openLocations) {
            ArrayList<Move> moves = loc.getValidMoves(board);
            if (moves.size() == loc.getRemainingConnections()) {
                // Only one move available; this is a forced move
                // Unlikely but possible. Usually the local move forcing rules preempt this, but it's possible I've missed an edge case somewhere
                return moves.toArray(new Move[0]);
            } else if (moves.size() == 0) {
                // Something has gone wrong; this location should not be open if there are no valid moves
                System.err.println("No valid moves for location " + loc.getCoordinate());
                return null;
            }

            candidates.put(loc, moves);
        }

        // Check locations with fewer connection possibilities and more open connections first
        // From lowest to highest on countMoveCombinations
        sortLocationsByConnections(openLocations, board);

        // Like the above, but operating on move combinations rather than individual moves
        for (Location loc : openLocations) {
            ArrayList<Move[]> combos = loc.getValidMoveCombinations(board);
            ArrayList<Move[]> validCombos = new ArrayList<>();
            for (Move[] combo : combos) {
                Board testBoard = new Board(board);
                boardsCreated++;
                try {
                    for (Move move : combo) {
                        testBoard.applyMove(move);
                    }
                } catch (InvalidMoveException e) {
                    // Skip this move combo, don't check the resulting board
                    continue;
                }

                if (isDeadly(testBoard, depthLimit, null)) {
                    // This move combination leads to a deadly board; skip it
                    continue;
                }

                validCombos.add(combo);
            }

            if (validCombos.size() == 0) {
                // If there are no valid combinations left, we shouldn't be here
                System.err.println("Location " + loc.getCoordinate() + " has no valid move combinations");
                return null;
            } else if (validCombos.size() == 1) {
                // If there's only one combination left, return it
                return validCombos.get(0);
            }

        }
        return null;
    }

    // A board is deadly if there are no valid moves from an open location, or if all valid moves from an open location lead to a deadly board
    public static boolean isDeadly(Board board, int depthLimit, Coordinate target) {
        if (depthLimit == 0) {
            // Too deep; give up
            return false;
        }

        // Get all open locations on the board and sort to put the most promising ones first
        ArrayList<Location> openLocations = board.getOpenLocations();
        if (target == null) {
            // Sort by the number of connections available to find a promising location, wherever it is
            sortLocationsByConnections(openLocations, board);
        } else {
            // Sort by the distance to the target to quickly evaluate a move at a particular location
            sortLocationsByDistance(openLocations, board, target);
        }

        // Get the moves for each location
        HashMap<Location, ArrayList<Move>> validMoves = new HashMap<>();
        for (Location loc : openLocations) {
            validMoves.put(loc, loc.getValidMoves(board));
        }
        
        // Check all open locations for forced moves
        for (Location loc : openLocations) {
            ArrayList<Move> moves = validMoves.get(loc);
            if (moves == null || moves.size() < loc.getRemainingConnections()) {
                return true; // No or insufficient valid moves from this location, so this move is deadly
            }

            // Check to see if there is at least one valid, non-deadly move
            Board newBoard = new Board(board);
            boardsCreated++;
            for (Move move : moves) {
                try {
                    newBoard.applyMove(move);
                } catch (InvalidMoveException e) {
                    // Invalid move; skip this one
                    continue;
                }
            }
            if (isDeadly(newBoard, depthLimit - 1, moves.get(0).getStart())) {
                return true;
            }
        }
        return false;
    }

    // It's important for performance to pick good locations first - we choose the ones with the most restricted connections first
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

    public static void sortLocationsByDistance(List<Location> locations, Board board, Coordinate target) {
        locations.sort((a, b) -> {
            // Sort by the distance to the target coordinate
            int aDistance = a.getCoordinate().manhattanDistance(target);
            int bDistance = b.getCoordinate().manhattanDistance(target);

            // If the distances are not equal, sort by that
            if (aDistance != bDistance) {
                return Integer.compare(aDistance, bDistance);
            }

            // If the distances are equal, sort by the xcoordinate, then by the ycoordinate
            if (a.getCoordinate().getRow() != b.getCoordinate().getRow()) {
                return Integer.compare(a.getCoordinate().getRow(), b.getCoordinate().getRow());
            } else {
                return Integer.compare(a.getCoordinate().getCol(), b.getCoordinate().getCol());
            }
        });
    }
}