package vrp;

public class Item {
    private final String id;
    private final double x;
    private final double y;
    private final double weight;

    public Item(String id, double x, double y, double weight) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.weight = weight;
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWeight() { return weight; }

    public double distance(Item other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }

    @Override
    public String toString() {
        return id;
    }
}

