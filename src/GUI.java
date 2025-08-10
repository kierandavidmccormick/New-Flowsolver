package src;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;

public class GUI {

    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 1000;
    private static final int OUTER_BORDER_SIZE = 20; // Size of the border around the grid

    public static final Colors colors = new Colors("data/colors.json");

    // // Map of color indexes to Color objects
    // public static final Map<Integer, Color> COLOR_INDEX_MAP = new HashMap<>();
    // // Map of color indexes to color names
    // public static final Map<String, Color> COLOR_NAME_MAP = new HashMap<>();
    // // Map of color names to indexes
    // public static final Map<String, Integer> COLOR_REVERSE_INDEX_MAP = new HashMap<>();

    // // Reads colors from colors.json and populates COLOR_INDEX_MAP and COLOR_NAME_MAP
    // public static void loadColorsFromJson(String filename) {
    //     try (FileReader reader = new FileReader(filename)) {
    //         JSONObject obj = new JSONObject(new JSONTokener(reader));

    //         int i = 0;
    //         for (String name : obj.keySet()) {
    //             // Get the RGB array
    //             org.json.JSONArray arr = obj.getJSONArray(name);
    //             Color color = new Color(arr.getInt(0), arr.getInt(1), arr.getInt(2));
    //             COLOR_INDEX_MAP.put(i, color);
    //             COLOR_REVERSE_INDEX_MAP.put(name, i);
    //             COLOR_NAME_MAP.put(name, color);
    //             i++;
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    public static void main(String[] args) {

        Board lastBoard = null;
        for (int i = 0; i < 270; i++) {
            if (i == 155 || i == 176) {
                continue; // Skip boards 155 and 176 as they have non-unique solutions
            }
            Board startBoard = getBoardFromStringArchive("boards/imported.txt", i);
            try {
                startBoard.updateAll();
            } catch (InvalidMoveException e) {
                // Shouldn't happen unless the board is invalid
                System.err.println("Invalid move: " + e.getMessage());
                break;
            }

            System.out.println("Starting board " + i + ":\n" + startBoard.simpleReadout() + "\n");

            lastBoard = Solver.solveBoard(startBoard);
            if (lastBoard == null) {
                System.err.println("No solution found");
                break;
            }
        }

        final Board board = lastBoard;
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Grid");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
            frame.setLocationRelativeTo(null); // center on screen

            JPanel gridPanel = new JPanel(new GridLayout(board.getHeight(), board.getWidth(), 5, 5));
            gridPanel.setBackground(Color.WHITE);

            for (int i = 0; i < board.getHeight() * board.getWidth(); i++) {
                final int idx = i;

                JPanel square = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        int col = idx % board.getWidth();
                        int row = idx / board.getWidth();

                        // Work out the color of the circle
                        Color circleColor = Color.GRAY;
                        if (board.getLocation(row, col).getColorIndex() != null) {
                            circleColor = colors.getColorByIndex(board.getLocation(row, col).getColorIndex());
                        }

                        // Draw the circle
                        g.setColor(circleColor);
                        
                        // These don't need to be precise, close enough is good enough
                        int outerCircleDiameter = (int) (Math.min(getWidth(), getHeight()) * 0.85);
                        int lineWidth = (int) (Math.min(getWidth(), getHeight()) * 0.4);
                        int innerCircleDiameter = (int) (Math.min(getWidth(), getHeight()) * 0.5);
                        int centerX = getWidth() / 2;
                        int centerY = getHeight() / 2;
                        g.fillOval(centerX - outerCircleDiameter / 2, centerY - outerCircleDiameter / 2, outerCircleDiameter, outerCircleDiameter);

                        // Draw the connections to each connected grid square
                        for (int i = 0; i < Coordinate.DIRECTIONS.length; i++) {
                            // Check if there's a connection
                            Coordinate dir = Coordinate.DIRECTIONS[i];
                            if (board.getLocation(row, col).getConnections()[i]) 
                            {
                                // Draw the line
                                int neighborCenterX = centerX + dir.getCol() * getWidth();
                                int neighborCenterY = centerY + dir.getRow() * getHeight();
                                g.setColor(circleColor);
                                ((Graphics) g).fillRect(
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
                square.setBackground(Color.BLACK);
                square.setPreferredSize(new Dimension(100, 100));
                gridPanel.add(square);
            }

            // Add an empty border around the grid panel
            gridPanel.setBorder(BorderFactory.createEmptyBorder(
                OUTER_BORDER_SIZE, OUTER_BORDER_SIZE, OUTER_BORDER_SIZE, OUTER_BORDER_SIZE));

            frame.add(gridPanel, BorderLayout.CENTER);
            frame.setVisible(true);
        });
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
}