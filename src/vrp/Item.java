package vrp;

public class Item {
    private final String id;
    private final double x;
    private final double y;
    // Si luego quieres capacidad por peso, añade: private final double weight;

    public Item(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
    // Constructor opcional con peso:
    // public Item(String id, double x, double y, double weight) { ... }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    // public double getWeight() { return weight; }
}

