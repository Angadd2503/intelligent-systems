package vrp;

import jade.core.Agent;

import java.util.List;

public class DeliveryAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("🚚 " + getLocalName() + " started.");

        // 1) read optional args (e.g., (4,150)) — not required yet
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            System.out.print("Args: ");
            for (Object a : args) System.out.print(a + " ");
            System.out.println();
        }

        // 2) use your parser to load items
        String path = "data/items.txt";           // keep it simple
        List<ItemsParser.Item> items = ItemsParser.readItems(path);

        System.out.println("✅ Loaded " + items.size() + " items:");
        for (ItemsParser.Item it : items) {
            System.out.println(" - " + it.name + " (" + it.x + ", " + it.y + ")");
        }

        // keep the agent alive so you can see it in the JADE GUI
    }
}
