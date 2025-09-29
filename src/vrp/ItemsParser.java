package vrp;

import java.io.BufferedReader;
import java.io.FileReader;

public class ItemsParser {

    public static java.util.List<Item> parseItems(String path) throws Exception {
        java.util.List<Item> items = new java.util.ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line; int ln = 0; int autoId = 1;
            while ((line = br.readLine()) != null) {
                ln++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                String[] p = line.split("\\s*,\\s*");
                if (p.length == 4) {
                    // id,x,y,weight  -> ignoramos weight porque Item no lo usa
                    items.add(new Item(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2])));
                } else if (p.length == 3) {
                    // id,x,y
                    items.add(new Item(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2])));
                } else if (p.length == 2) {
                    // x,y  -> id auto iN
                    items.add(new Item("i"+(autoId++), Double.parseDouble(p[0]), Double.parseDouble(p[1])));
                } else {
                    throw new IllegalArgumentException("Línea "+ln+" inválida: "+line);
                }
            }
        }
        return items;
    }
}
