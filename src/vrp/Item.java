package vrp;

public class Item {
    private final String id;
    private final double x;
    private final double y;
    private final int demand; // NEW

    // Default demand = 1 for existing code paths
    public Item(String id, double x, double y) {
        this(id, x, y, 1);
    }

    public Item(String id, double x, double y, int demand) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand <= 0 ? 1 : demand;
    }

    public String getId() { return id; }
    public double getX()  { return x;  }
    public double getY()  { return y;  }
    public int getDemand(){ return demand; }

    @Override
    public String toString() {
        return "Item{" + id + ",x=" + x + ",y=" + y + ",d=" + demand + "}";
    }
}



