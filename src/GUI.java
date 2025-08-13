package src;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;

// TODO: Better visualization for per-move changes, to see the diff from the previous move
// TODO: Add a "play" button to step through the solution automatically

public class GUI {

    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 1000;
    private static final int OUTER_BORDER_SIZE = 20; // Size of the border around the grid

    public static final Colors colors = new Colors("data/colors.json");

    private static Runnable updateButtonStates = null; // Placeholder for button state update runnable
    private static Runnable updateLabels = null; // Placeholder for label update runnable
    public static void main(String[] args) {

        final ArrayList<ArrayList<Board>> solveHistories = new ArrayList<>();

        int boardIndex = 0;                                         // Change this to view different boards
        final int[] boardIndexHolder = {boardIndex};                // Use an array to allow mutation in lambdas
        int solution_index = 0;                                     // Change this to view different moves in a solution
        final int[] solutionIndexHolder = {solution_index};         // Use an array to allow mutation in lambdas

        SwingUtilities.invokeLater(() -> renderStuff(solveHistories, boardIndexHolder, solutionIndexHolder));

        // Start a separate thread to load solutions in the background
        new Thread(() -> {
            for (int i = solveHistories.size(); i < 270; i++) {
                if (i == 155 || i == 176) continue; // Skip boards 155 and 176 as they have non-unique solutions

                ArrayList<Board> solveHistory = getSolveHistory(i);
                if (solveHistory == null || solveHistory.isEmpty()) {
                    System.err.println("No solution found for board " + i);
                    break;
                }
                solveHistories.add(solveHistory);

                // Update button states and labels on the Swing thread
                SwingUtilities.invokeLater(() -> {
                    if (updateButtonStates != null) updateButtonStates.run();
                    if (updateLabels != null) updateLabels.run();
                });
            }
        }).start();
    }

    private static ArrayList<Board> getSolveHistory(int i) {
        Board startBoard = getBoardFromStringArchive("boards/imported.txt", i);
        try {
            startBoard.updateAll();
        } catch (InvalidMoveException e) {
            // Shouldn't happen unless the board is invalid
            System.err.println("Invalid move: " + e.getMessage());
        }

        System.out.println("Starting board " + i + ":\n" + startBoard.simpleReadout() + "\n");
        
        return Solver.solveBoard(startBoard);
    }

    private static void renderStuff(ArrayList<ArrayList<Board>> solveHistories, int[] boardIndexHolder, int[] solutionIndexHolder) {
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
                    JPanel square = getGridSquare(solveHistories, boardIndexHolder, solutionIndexHolder, row, col);
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
        moveArrowPanel.setLayout(new GridLayout(1, 3, 10, 0)); // 3 columns: left arrow, label, right arrow
        moveArrowPanel.setBackground(Color.WHITE);

        javax.swing.JButton leftArrow = new javax.swing.JButton("<");
        javax.swing.JButton rightArrow = new javax.swing.JButton(">");

        // --- Board arrow panel with board label in the same row ---
        JPanel boardArrowPanel = new JPanel();
        boardArrowPanel.setLayout(new GridLayout(1, 3, 10, 0)); // 3 columns: left arrow, label, right arrow
        boardArrowPanel.setBackground(Color.WHITE);

        javax.swing.JButton boardLeftArrow = new javax.swing.JButton("<");
        javax.swing.JButton boardRightArrow = new javax.swing.JButton(">");

        // Helper to update button enabled states
        Runnable updateButtonStates = () -> {
            leftArrow.setEnabled(solutionIndexHolder[0] > 0);
            rightArrow.setEnabled(solutionIndexHolder[0] < solveHistories.get(boardIndexHolder[0]).size() - 1);
            boardLeftArrow.setEnabled(boardIndexHolder[0] > 0);
            boardRightArrow.setEnabled(boardIndexHolder[0] < solveHistories.size() - 1);
        };
        GUI.updateButtonStates = updateButtonStates; // So other threads can call this

        // Helper to update label text
        Runnable updateLabels = () -> {
            moveLabel.setText("Move " + (solutionIndexHolder[0] + 1) + "/" + solveHistories.get(boardIndexHolder[0]).size());
            boardLabel.setText("Board " + (boardIndexHolder[0] + 1) + "/" + solveHistories.size());
        };
        GUI.updateLabels = updateLabels; // So other threads can call this

        leftArrow.addActionListener(e -> {
            if (solutionIndexHolder[0] > 0) {
                solutionIndexHolder[0]--;
                updateLabels.run();
                updateButtonStates.run();
                for (JPanel square : squares) {
                    square.repaint();
                }
            }
        });

        rightArrow.addActionListener(e -> {
            if (solutionIndexHolder[0] < solveHistories.get(boardIndexHolder[0]).size() - 1) {
                solutionIndexHolder[0]++;
                updateLabels.run();
                updateButtonStates.run();
                for (JPanel square : squares) {
                    square.repaint();
                }
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

        // Initial button states and labels
        updateLabels.run();
        updateButtonStates.run();

        moveArrowPanel.add(leftArrow);
        moveArrowPanel.add(moveLabel);
        moveArrowPanel.add(rightArrow);

        boardArrowPanel.add(boardLeftArrow);
        boardArrowPanel.add(boardLabel);
        boardArrowPanel.add(boardRightArrow);

        // --- Main panel layout ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(boardArrowPanel, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        mainPanel.add(moveArrowPanel, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public static Board getBoardFromStringArchive(String filename, int index) {
        // Open a file
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
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
            int width = Integer.parseInt(parts[0]);
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

            // Create a Board object from the contents
            return new Board(contents, width, height);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JPanel getGridSquare(ArrayList<ArrayList<Board>> solveHistories, int[] boardIndexHolder, int[] solutionIndexHolder, int row, int col) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Board board = solveHistories.get(boardIndexHolder[0]).get(solutionIndexHolder[0]);         // Workaround for lambda variable capture

                // Work out the color of the circle
                Color circleColor = Color.GRAY;
                if (board.getLocation(row, col).getColorIndex() != null) {
                    circleColor = colors.getColorByIndex(board.getLocation(row, col).getColorIndex());
                }

                // Draw the circle
                g.setColor(circleColor);

                int outerCircleDiameter = (int) (Math.min(getWidth(), getHeight()) * 0.85);
                int lineWidth = (int) (Math.min(getWidth(), getHeight()) * 0.4);
                int innerCircleDiameter = (int) (Math.min(getWidth(), getHeight()) * 0.5);
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                g.fillOval(centerX - outerCircleDiameter / 2, centerY - outerCircleDiameter / 2, outerCircleDiameter, outerCircleDiameter);

                // Draw the connections to each connected grid square
                for (int i = 0; i < Coordinate.DIRECTIONS.length; i++) {
                    Coordinate dir = Coordinate.DIRECTIONS[i];
                    if (board.getLocation(row, col).getConnections()[i]) {
                        int neighborCenterX = centerX + dir.getCol() * getWidth();
                        int neighborCenterY = centerY + dir.getRow() * getHeight();
                        g.setColor(circleColor);
                        g.fillRect(
                            Math.min(centerX, neighborCenterX) - lineWidth / 2,
                            Math.min(centerY, neighborCenterY) - lineWidth / 2,
                            Math.abs(neighborCenterX - centerX) + lineWidth,
                            Math.abs(neighborCenterY - centerY) + lineWidth
                        );
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