package vrp;

import java.io.*;
import java.util.*;

public class ItemsParser {

    // A static method to load items from a text file
    public static List<Item> load(String filename) throws IOException {
        List<Item> items = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // skip blanks or comments

                // Expecting format: id x y
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        items.add(new Item(id, x, y));
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping invalid line: " + line);
                    }
                }
            }
        }
        return items;
    }

    // Optional demo runner (so you can run this file directly)
    public static void main(String[] args) {
        try {
            List<Item> items = load("items.txt");
            System.out.println("✅ Loaded " + items.size() + " items");
            for (Item it : items) {
                System.out.println("  " + it);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
