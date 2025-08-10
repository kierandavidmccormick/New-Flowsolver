package src;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.Color;

public class Colors {
    // Map of color indexes to Color objects
    private final Map<Integer, Color> COLOR_INDEX_MAP = new HashMap<>();
    // Map of color indexes to color names
    private final Map<String, Color> COLOR_NAME_MAP = new HashMap<>();
    // Map of color names to indexes
    private final Map<String, Integer> COLOR_REVERSE_INDEX_MAP = new HashMap<>();

    // Reads colors from colors.json and populates COLOR_INDEX_MAP and COLOR_NAME_MAP
    private void loadColorsFromJson(String filename) {
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

    public Colors(String filename) {
        loadColorsFromJson(filename);
    }

    public Color getColorByIndex(int index) {
        if (COLOR_INDEX_MAP.containsKey(index)) {
            return COLOR_INDEX_MAP.get(index);
        } else {
            // Generate a new random color
            Random rand = new Random();
            Color color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            // Generate a unique name for the color
            String name;
            do {
                name = "random_" + index + "_" + rand.nextInt(1000000);
            } while (COLOR_NAME_MAP.containsKey(name));
            // Add to all maps
            COLOR_INDEX_MAP.put(index, color);
            COLOR_NAME_MAP.put(name, color);
            COLOR_REVERSE_INDEX_MAP.put(name, index);
            return color;
        }
    }

    public Color getColorByName(String name) {
        if (COLOR_NAME_MAP.containsKey(name)) {
            return COLOR_NAME_MAP.get(name);
        } else {
            return null;
        }
    }

    public Integer getColorIndexByName(String name) {
        if (COLOR_REVERSE_INDEX_MAP.containsKey(name)) {
            return COLOR_REVERSE_INDEX_MAP.get(name);
        } else {
            return null;
        }
    }
}
