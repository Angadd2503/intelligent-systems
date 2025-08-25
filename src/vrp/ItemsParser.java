package vrp;

import java.io.*;
import java.util.*;

public class ItemsParser {

    // A static method to load items from a text file
    public static List<Item> load(String filename) throws IOException {
        List<Item> items = new ArrayList<>();
        int autoIdCounter = 1000;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }// skip blanks or comments

                // Expecting format: id x y
                String[] parts = line.split(",");
                try {
                    if (parts.length >= 3) {
                        int id = Integer.parseInt(parts[0].trim());
                        double x = Double.parseDouble(parts[1].trim());
                        double y = Double.parseDouble(parts[2].trim());
                        items.add(new Item(id, x, y));

                    } else if (parts.length == 2) {
                        double x = Double.parseDouble(parts[0].trim());
                        double y = Double.parseDouble(parts[1].trim());
                        items.add(new Item(autoIdCounter++, x, y));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid line (bad number format): " + line);
                }
            }
        }
        return items;
    }

    // Optional demo runner (so you can run this file directly)
    public static void main(String[] args) {
        try {
            List<Item> items = load("data/Items.txt");
            System.out.println("✅ Loaded " + items.size() + " items");
            for (Item it : items) {
                System.out.println("  " + it);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
