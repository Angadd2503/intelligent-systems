package vrp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ItemsParser {
    public static List<Item> parseItems(String filePath) throws Exception {
        List<Item> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Ignorar líneas vacías o comentarios
                }
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String id = parts[0].trim();
                    double x = Double.parseDouble(parts[1].trim());
                    double y = Double.parseDouble(parts[2].trim());
                    double weight = Double.parseDouble(parts[3].trim());
                    items.add(new Item(id, x, y, weight));
                }
            }
        }
        return items;
    }
}
