package vrp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ItemsParser {

    // tiny holder for one row: name, x, y
    public static class Item {
        public final String name;
        public final int x;
        public final int y;
        public Item(String name, int x, int y) {
            this.name = name; this.x = x; this.y = y;
        }
        @Override public String toString() { return "(" + name + ", " + x + ", " + y + ")"; }
    }

    /** Reads lines like:  name x y   (e.g., A 10 5). Returns a list of Items. */
    public static List<Item> readItems(String filename) {
        List<Item> items = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length == 3) {
                    String name = parts[0];
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        items.add(new Item(name, x, y));
                    } catch (NumberFormatException ignored) {
                        // skip bad line
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Parser] Could not read file: " + e.getMessage());
        }
        return items;
    }

    /** Small fallback list if you don’t pass a file yet. */
    public static List<Item> defaults() {
        List<Item> d = new ArrayList<>();
        d.add(new Item("A", 10, 5));
        d.add(new Item("B", -3, 8));
        d.add(new Item("C", 12, -4));
        return d;
    }
}

