package src;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;
import java.util.Map;
import java.util.HashMap;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GUI {

    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 1000;
    private static final int OUTER_BORDER_SIZE = 20; // Size of the border around the grid

    // Map of color indexes to Color objects
    public static final Map<Integer, Color> COLOR_INDEX_MAP = new HashMap<>();
    // Map of color indexes to color names
    public static final Map<String, Color> COLOR_NAME_MAP = new HashMap<>();
    // Map of color names to indexes
    public static final Map<String, Integer> COLOR_REVERSE_INDEX_MAP = new HashMap<>();

    // Reads colors from colors.json and populates COLOR_INDEX_MAP and COLOR_NAME_MAP
    public static void loadColorsFromJson(String filename) {
        try (FileReader reader = new FileReader(filename)) {
            JSONObject obj = new JSONObject(new JSONTokener(reader));

            int i = 0;
            for (String name : obj.keySet()) {
                // Get the RGB array
                org.json.JSONArray arr = obj.getJSONArray(name);
                Color color = new Color(arr.getInt(0), arr.getInt(1), arr.getInt(2));
                COLOR_INDEX_MAP.put(i, color);
                COLOR_REVERSE_INDEX_MAP.put(name, i);
                COLOR_NAME_MAP.put(name, color);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // Populate the list of colors
        loadColorsFromJson("data/colors.json");

        Board startBoard = Solver.getRootBoard("boards/board5.json");
        try {
            startBoard.updateAll();
        } catch (InvalidMoveException e) {
            // Shouldn't happen unless the board is invalid
            System.err.println("Invalid move: " + e.getMessage());
        }

        System.out.println("Starting board:" + startBoard.simpleReadout());

        // final Board board = startBoard;
        // final Board board = Solver.solveBoard(startBoard, 0);
        final Board board = Solver.solveBoardBetter(startBoard);
        if (board == null) {
            System.err.println("No solution found");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Grid");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
            frame.setLocationRelativeTo(null); // center on screen

            JPanel gridPanel = new JPanel(new GridLayout(Solver.BOARD_SIZE, Solver.BOARD_SIZE, 5, 5));
            gridPanel.setBackground(Color.WHITE);

            for (int i = 0; i < Solver.BOARD_SIZE * Solver.BOARD_SIZE; i++) {
                final int idx = i;

                JPanel square = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        int col = idx % Solver.BOARD_SIZE;
                        int row = idx / Solver.BOARD_SIZE;

                        // Work out the color of the circle
                        Color circleColor = Color.GRAY;
                        if (board.getLocation(row, col).getColorIndex() != null) {
                            circleColor = GUI.COLOR_INDEX_MAP.get(board.getLocation(row, col).getColorIndex());
                        }

                        // Draw the circle
                        g.setColor(circleColor);
                        int diameter = Math.min(getWidth(), getHeight()) - 30;
                        int centerX = getWidth() / 2;
                        int centerY = getHeight() / 2;
                        g.fillOval(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter);

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
                                    Math.min(centerX, neighborCenterX) - 10,
                                    Math.min(centerY, neighborCenterY) - 10,
                                    Math.abs(neighborCenterX - centerX) + 20,
                                    Math.abs(neighborCenterY - centerY) + 20
                                );
                            }
                        }

                        // Mark the start/end locations
                        if (board.getLocation(row, col).isStart()) {
                            g.setColor(Color.BLACK);
                            g.fillOval(centerX - 15, centerY - 15, 30, 30);
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
}