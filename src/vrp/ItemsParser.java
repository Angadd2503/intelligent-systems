package vrp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads Items from either classpath resource /data/Items.txt or a file path. */
public class ItemsParser {

    public static List<Item> load(String filename) throws IOException {
        InputStream is = tryOpenClasspath(filename);
        if (is == null) {
            // Fallback to filesystem relative path
            File f = new File(filename);
            if (!f.exists()) throw new FileNotFoundException("Items file not found: " + filename);
            is = new FileInputStream(f);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            List<Item> items = new ArrayList<>();
            int autoId = 1000;
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
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
                        items.add(new Item(autoId++, x, y));
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println("Skipping invalid line (bad number format): " + line);
                }
            }
            return items;
        }
    }

    private static InputStream tryOpenClasspath(String filename) {
        // Allow both "data/Items.txt" and "/data/Items.txt"
        ClassLoader cl = ItemsParser.class.getClassLoader();
        InputStream is = cl.getResourceAsStream(filename);
        if (is != null) return is;
        if (filename.startsWith("/")) filename = filename.substring(1);
        return cl.getResourceAsStream(filename);
    }

    // Quick manual test
    public static void main(String[] args) {
        try {
            List<Item> items = load("data/Items.txt");
            System.out.println("✅ Loaded " + items.size() + " items");
            for (Item it : items) System.out.println("  " + it);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

