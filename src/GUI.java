package src;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;

// TODO: Add a "play" button to step through the solution automatically

public class GUI {

    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 1000;
    private static final int OUTER_BORDER_SIZE = 0; // Size of the border around the grid

    public static final Colors colors = new Colors("data/colors.json");

    private static Runnable updateButtonStates = null; // Placeholder for button state update runnable
    private static Runnable updateLabels = null; // Placeholder for label update runnable
    public static void main(String[] args) {

        final ArrayList<ArrayList<Board>> solveHistories = new ArrayList<>();
        final ArrayList<ArrayList<boolean[][]>> diffs = new ArrayList<>(); // Store diffs for each board's solve history
        final ArrayList<ArrayList<Move[]>> moveHistories = new ArrayList<>(); // Store move histories for each board

        int boardIndex = 0;                                         // Change this to view different boards
        final int[] boardIndexHolder = {boardIndex};                // Use an array to allow mutation in lambdas
        int solution_index = 0;                                     // Change this to view different moves in a solution
        final int[] solutionIndexHolder = {solution_index};         // Use an array to allow mutation in lambdas

        SwingUtilities.invokeLater(() -> renderStuff(solveHistories, diffs, moveHistories, boardIndexHolder, solutionIndexHolder));

        // Start a separate thread to load solutions in the background
        new Thread(() -> {
            for (int i = 0; i < 270; i++) {
                if (i == 155 || i == 176) continue; // Skip boards 155 and 176 as they have non-unique solutions

                ArrayList<Move[]> moveHistory = new ArrayList<>();
                ArrayList<Board> solveHistory = getSolveHistory(i, moveHistory);
                if (solveHistory == null || solveHistory.isEmpty()) {
                    System.err.println("No solution found for board " + i);
                    break;
                }
                solveHistories.add(solveHistory);
                diffs.add(getDiffs(solveHistory));
                moveHistories.add(moveHistory);

                // Update button states and labels on the Swing thread
                SwingUtilities.invokeLater(() -> {
                    if (updateButtonStates != null) updateButtonStates.run();
                    if (updateLabels != null) updateLabels.run();
                });
            }
        }).start();
    }

    // TODO Really, these should live in Solver.java
    private static ArrayList<Board> getSolveHistory(int i, ArrayList<Move[]> moveHistory) {
        Board startBoard = new Board("boards/imported.txt", i);
        try {
            startBoard.updateAll();
        } catch (InvalidMoveException e) {
            // Shouldn't happen unless the board is invalid
            System.err.println("Invalid move: " + e.getMessage());
        }

        System.out.println("Starting board " + i + ":\n" + startBoard.simpleReadout() + "\n");

        return Solver.solveBoard(startBoard, moveHistory);
    }

    private static ArrayList<boolean[][]> getDiffs(ArrayList<Board> solveHistory) {
        ArrayList<boolean[][]> diffs = new ArrayList<>();
        diffs.add(solveHistory.get(0).getDiff(solveHistory.get(0))); // First board has no diff
        for (int i = 1; i < solveHistory.size(); i++) {
            boolean[][] diff = solveHistory.get(i).getDiff(solveHistory.get(i - 1));
            diffs.add(diff);
        }
        return diffs;
    }

    private static void renderStuff(ArrayList<ArrayList<Board>> solveHistories, ArrayList<ArrayList<boolean[][]>> diffs, ArrayList<ArrayList<Move[]>> moveHistories, int[] boardIndexHolder, int[] solutionIndexHolder) {
        JFrame frame = new JFrame("Grid");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setLocationRelativeTo(null); // center on screen

        // --- Move counter label ---
        javax.swing.JLabel moveLabel = new javax.swing.JLabel("Move " + (solutionIndexHolder[0] + 1) + "/" + solveHistories.get(boardIndexHolder[0]).size(), javax.swing.SwingConstants.CENTER);
        moveLabel.setFont(moveLabel.getFont().deriveFont(24f));

        // --- Board index label ---
        javax.swing.JLabel boardLabel = new javax.swing.JLabel("Board " + (boardIndexHolder[0] + 1) + "/" + solveHistories.size(), javax.swing.SwingConstants.CENTER);
        boardLabel.setFont(boardLabel.getFont().deriveFont(24f));

        final JPanel gridPanel = new JPanel();
        final ArrayList<JPanel> squares = new ArrayList<>();
        gridPanel.setBorder(BorderFactory.createEmptyBorder(
            OUTER_BORDER_SIZE, OUTER_BORDER_SIZE, OUTER_BORDER_SIZE, OUTER_BORDER_SIZE));

        Runnable updateGrid = () -> {
            int boardHeight = solveHistories.get(boardIndexHolder[0]).get(0).getHeight();
            int boardWidth = solveHistories.get(boardIndexHolder[0]).get(0).getWidth();
            gridPanel.removeAll(); // Clear existing squares
            gridPanel.setLayout(new GridLayout(boardHeight, boardWidth, 1, 1)); // Reset layout

            squares.clear();
            for (int row = 0; row < boardHeight; row++) {
                for (int col = 0; col < boardWidth; col++) {
                    JPanel square = getGridSquare(solveHistories, diffs, moveHistories, boardIndexHolder, solutionIndexHolder, row, col);
                    square.setBackground(Color.BLACK);
                    square.setPreferredSize(new Dimension(100, 100));
                    squares.add(square);
                    gridPanel.add(square);
                }
            }
            gridPanel.revalidate(); // Refresh the panel
            gridPanel.repaint(); // Repaint the panel
        };
        updateGrid.run(); // Initial grid setup

        // --- Arrow panel with move label in the same row ---
        JPanel moveArrowPanel = new JPanel();
        moveArrowPanel.setLayout(new GridLayout(1, 5, 10, 0)); // 3 columns: left arrow, label, right arrow
        moveArrowPanel.setBackground(Color.WHITE);

        javax.swing.JButton solutionSkipLeftArrow = new javax.swing.JButton("|<<");
        javax.swing.JButton solutionLeftArrow = new javax.swing.JButton("<");
        javax.swing.JButton solutionRightArrow = new javax.swing.JButton(">");
        javax.swing.JButton solutionSkipRightArrow = new javax.swing.JButton(">>|");

        // --- Board arrow panel with board label in the same row ---
        JPanel boardArrowPanel = new JPanel();
        boardArrowPanel.setLayout(new GridLayout(1, 3, 10, 0)); // 3 columns: left arrow, label, right arrow
        boardArrowPanel.setBackground(Color.WHITE);

        javax.swing.JButton boardSkipLeftArrow = new javax.swing.JButton("|<<");
        javax.swing.JButton boardLeftArrow = new javax.swing.JButton("<");
        javax.swing.JButton boardRightArrow = new javax.swing.JButton(">");
        javax.swing.JButton boardSkipRightArrow = new javax.swing.JButton(">>|");

        // Helper to update button enabled states
        Runnable updateButtonStates = () -> {
            solutionSkipLeftArrow.setEnabled(solutionIndexHolder[0] > 0);
            solutionLeftArrow.setEnabled(solutionIndexHolder[0] > 0);
            solutionRightArrow.setEnabled(solutionIndexHolder[0] < solveHistories.get(boardIndexHolder[0]).size() - 1 || boardIndexHolder[0] < solveHistories.size() - 1);
            solutionSkipRightArrow.setEnabled(solutionIndexHolder[0] < solveHistories.get(boardIndexHolder[0]).size() - 1);

            boardSkipLeftArrow.setEnabled(boardIndexHolder[0] > 0);
            boardLeftArrow.setEnabled(boardIndexHolder[0] > 0);
            boardRightArrow.setEnabled(boardIndexHolder[0] < solveHistories.size() - 1);
            boardSkipRightArrow.setEnabled(boardIndexHolder[0] < solveHistories.size() - 1);
        };
        GUI.updateButtonStates = updateButtonStates; // So other threads can call this

        // Helper to update label text
        Runnable updateLabels = () -> {
            moveLabel.setText("Move " + (solutionIndexHolder[0] + 1) + "/" + solveHistories.get(boardIndexHolder[0]).size());
            boardLabel.setText("Board " + (boardIndexHolder[0] + 1) + "/" + solveHistories.size());
        };
        GUI.updateLabels = updateLabels; // So other threads can call this

        solutionSkipLeftArrow.addActionListener(e -> {
            if (solutionIndexHolder[0] > 0) {
                solutionIndexHolder[0] = 0; // Skip to the first move
                updateLabels.run();
                updateButtonStates.run();
                for (JPanel square : squares) {
                    square.repaint();
                }
            }
        });

        solutionLeftArrow.addActionListener(e -> {
            if (solutionIndexHolder[0] > 0) {
                solutionIndexHolder[0]--;
                updateLabels.run();
                updateButtonStates.run();
                for (JPanel square : squares) {
                    square.repaint();
                }
            }
        });

        solutionRightArrow.addActionListener(e -> {
            if (solutionIndexHolder[0] < solveHistories.get(boardIndexHolder[0]).size() - 1) {
                solutionIndexHolder[0]++;
                updateLabels.run();
                updateButtonStates.run();
                for (JPanel square : squares) {
                    square.repaint();
                }
            } else if (boardIndexHolder[0] < solveHistories.size() - 1) {
                // If at the end of the current board, move to the next board
                boardIndexHolder[0]++;
                solutionIndexHolder[0] = 0; // Reset to first step of the new board's solution
                updateLabels.run();
                updateButtonStates.run();
                updateGrid.run(); // Update the grid for the new board
            }
        });

        solutionSkipRightArrow.addActionListener(e -> {
            if (solutionIndexHolder[0] < solveHistories.get(boardIndexHolder[0]).size() - 1) {
                solutionIndexHolder[0] = solveHistories.get(boardIndexHolder[0]).size() - 1; // Skip to the last move
                updateLabels.run();
                updateButtonStates.run();
                for (JPanel square : squares) {
                    square.repaint();
                }
            }
        });

        boardSkipLeftArrow.addActionListener(e -> {
            if (boardIndexHolder[0] > 0) {
                boardIndexHolder[0] = 0; // Skip to the first board
                solutionIndexHolder[0] = 0;
                updateLabels.run();
                updateButtonStates.run();
                updateGrid.run(); // Update the grid for the new board
            }
        });

        boardLeftArrow.addActionListener(e -> {
            if (boardIndexHolder[0] > 0) {
                boardIndexHolder[0]--;
                solutionIndexHolder[0] = 0;
                updateLabels.run();
                updateButtonStates.run();
                updateGrid.run(); // Update the grid for the new board
            }
        });

        boardRightArrow.addActionListener(e -> {
            if (boardIndexHolder[0] < solveHistories.size() - 1) {
                boardIndexHolder[0]++;
                solutionIndexHolder[0] = 0;
                updateLabels.run();
                updateButtonStates.run();
                updateGrid.run(); // Update the grid for the new board
            }
        });

        boardSkipRightArrow.addActionListener(e -> {
            if (boardIndexHolder[0] < solveHistories.size() - 1) {
                boardIndexHolder[0] = solveHistories.size() - 1; // Skip to the last board
                solutionIndexHolder[0] = 0;
                updateLabels.run();
                updateButtonStates.run();
                updateGrid.run(); // Update the grid for the new board
            }
        });

        // Initial button states and labels
        updateLabels.run();
        updateButtonStates.run();

        moveArrowPanel.add(solutionSkipLeftArrow);
        moveArrowPanel.add(solutionLeftArrow);
        moveArrowPanel.add(moveLabel);
        moveArrowPanel.add(solutionRightArrow);
        moveArrowPanel.add(solutionSkipRightArrow);

        boardArrowPanel.add(boardSkipLeftArrow);
        boardArrowPanel.add(boardLeftArrow);
        boardArrowPanel.add(boardLabel);
        boardArrowPanel.add(boardRightArrow);
        boardArrowPanel.add(boardSkipRightArrow);

        // --- Main panel layout ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(boardArrowPanel, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        mainPanel.add(moveArrowPanel, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static JPanel getGridSquare(ArrayList<ArrayList<Board>> solveHistories, ArrayList<ArrayList<boolean[][]>> diffs, ArrayList<ArrayList<Move[]>> moveHistories, int[] boardIndexHolder, int[] solutionIndexHolder, int row, int col) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Fetch the data that's to be rendered
                Board board = solveHistories.get(boardIndexHolder[0]).get(solutionIndexHolder[0]);         // Workaround for lambda variable capture
                boolean[][] diff = diffs.get(boardIndexHolder[0]).get(solutionIndexHolder[0]);
                ArrayList<Coordinate> moveCoordinates = new ArrayList<>();
                for (Move move : moveHistories.get(boardIndexHolder[0]).get(solutionIndexHolder[0])) {
                    moveCoordinates.add(move.getStart());
                    moveCoordinates.add(move.getStart().add(move.getDirection()));
                }

                Location loc = board.getLocation(row, col);
                Coordinate coord = loc.getCoordinate();

                // Set some dimensions based on the panel size
                int minDimension = Math.min(getWidth(), getHeight());
                int outerCircleDiameter = (int) (minDimension * (loc.isStart() ? 0.85 : 0.65));
                int lineWidth = (int) (minDimension * 0.4);
                int innerCircleDiameter = (int) (minDimension * 0.4);
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int borderWidth = (int) (minDimension / 16);

                // Work out the color of the circle
                Color circleColor = Color.GRAY;
                if (loc.getColorIndex() != null) {
                    circleColor = colors.getColorByIndex(loc.getColorIndex());
                }

                // Draw the circle
                if ((loc.getRemainingConnections() > 0 && loc.countConnections() > 0) || loc.isStart()) {
                    g.setColor(circleColor);
                    g.fillOval(centerX - outerCircleDiameter / 2, centerY - outerCircleDiameter / 2, outerCircleDiameter, outerCircleDiameter);
                }

                // Draw the connections to each connected grid square
                for (int i = 0; i < Coordinate.DIRECTIONS.length; i++) {
                    Coordinate dir = Coordinate.DIRECTIONS[i];

                    if (loc.getConnections()[i]) {
                        g.setColor(circleColor);
                        if (dir == Coordinate.UP) {
                            g.fillRect(centerX - lineWidth / 2, 0, lineWidth, centerY);
                        } else if (dir == Coordinate.DOWN) {
                            g.fillRect(centerX - lineWidth / 2, centerY, lineWidth, getHeight() - centerY);
                        } else if (dir == Coordinate.LEFT) {
                            g.fillRect(0, centerY - lineWidth / 2, centerX, lineWidth);
                        } else if (dir == Coordinate.RIGHT) {
                            g.fillRect(centerX, centerY - lineWidth / 2, getWidth() - centerX, lineWidth);
                        }
                    }

                    int neighborRow = row + dir.getRow();
                    int neighborCol = col + dir.getCol();
                    Coordinate neighborCoord = new Coordinate(neighborRow, neighborCol);
                    if (diff[row][col]) {

                        boolean drawBorder = false;
                        boolean drawCorner = false;
                        Coordinate rightTurnNeighbor = coord.add(Coordinate.rightTurn(dir));
                        Coordinate rightCornerNeighbor = rightTurnNeighbor.add(dir);

                        // Decide what kind of border to draw, if any
                        if (moveCoordinates.contains(loc.getCoordinate()) && !moveCoordinates.contains(neighborCoord)) {
                            g.setColor(Color.RED);
                            drawBorder = true;
                        } else if (!neighborCoord.isInBounds(board) || !diff[neighborRow][neighborCol] || (!moveCoordinates.contains(coord) && moveCoordinates.contains(neighborCoord))) {
                            g.setColor(Color.YELLOW);
                            drawBorder = true;
                        } else if (neighborCoord.isInBounds(board) && rightTurnNeighbor.isInBounds(board) && rightCornerNeighbor.isInBounds(board)) {
                            // A bunch of elaborate logic to draw interior corners for the diff borders
                            if (!moveCoordinates.contains(coord) &&
                                            (!moveCoordinates.contains(neighborCoord) && diff[neighborRow][neighborCol]) &&
                                            (!moveCoordinates.contains(rightTurnNeighbor) && diff[rightTurnNeighbor.getRow()][rightTurnNeighbor.getCol()]) && 
                                            (moveCoordinates.contains(rightCornerNeighbor) || !diff[rightCornerNeighbor.getRow()][rightCornerNeighbor.getCol()])) {
                                g.setColor(Color.YELLOW);
                                drawCorner = true;
                            } else if (moveCoordinates.contains(coord) &&
                                            (moveCoordinates.contains(neighborCoord) || !diff[neighborRow][neighborCol]) &&
                                            (moveCoordinates.contains(rightTurnNeighbor) || !diff[rightTurnNeighbor.getRow()][rightTurnNeighbor.getCol()]) && 
                                            (!moveCoordinates.contains(rightCornerNeighbor) && diff[rightTurnNeighbor.getRow()][rightTurnNeighbor.getCol()])) {
                                g.setColor(Color.RED);
                                drawCorner = true;
                                // It's extremely unlikely a red interior corner will ever happen, but I've included it for completeness's sake
                            }
                        }

                        if (drawBorder) {
                            if (dir == Coordinate.UP) {
                                g.fillRect(0, 0, getWidth(), borderWidth);
                            } else if (dir == Coordinate.DOWN) {
                                g.fillRect(0, getHeight() - borderWidth, getWidth(), borderWidth);
                            } else if (dir == Coordinate.LEFT) {
                                g.fillRect(0, 0, borderWidth, getHeight());
                            } else if (dir == Coordinate.RIGHT) {
                                g.fillRect(getWidth() - borderWidth, 0, borderWidth, getHeight());
                            }
                        } else if (drawCorner) {
                            if (dir == Coordinate.UP) {
                                g.fillRect(getWidth() - borderWidth, 0, borderWidth, borderWidth);
                            } else if (dir == Coordinate.DOWN) {
                                g.fillRect(0, getHeight() - borderWidth, borderWidth, borderWidth);
                            } else if (dir == Coordinate.LEFT) {
                                g.fillRect(0, 0, borderWidth, borderWidth);
                            } else if (dir == Coordinate.RIGHT) {
                                g.fillRect(getWidth() - borderWidth, getHeight() - borderWidth, borderWidth, borderWidth);
                            }
                        }
                    }
                }

                // Draw an additional circle of the same color if the location is a corner
                if (loc.countConnections() == 2) {
                    if (loc.getConnections()[Coordinate.toIndex(Coordinate.UP)] && loc.getConnections()[Coordinate.toIndex(Coordinate.DOWN)]) {
                        // Do nothing
                    } else if (loc.getConnections()[Coordinate.toIndex(Coordinate.LEFT)] && loc.getConnections()[Coordinate.toIndex(Coordinate.RIGHT)]) {
                        // Do nothing
                    } else {
                        g.setColor(circleColor);
                        g.fillOval(centerX - lineWidth / 2, centerY - lineWidth / 2, lineWidth, lineWidth);
                    }
                }

                // Mark the start/end locations
                if (board.getLocation(row, col).isStart()) {
                    g.setColor(Color.BLACK);
                    g.fillOval(centerX - innerCircleDiameter / 2, centerY - innerCircleDiameter / 2, innerCircleDiameter, innerCircleDiameter);
                }
            }
        };
    }
}