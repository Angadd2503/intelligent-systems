package vrp;

public class Item {
    public final int id;     // 1..N
    public final double x;   // X coordinate
    public final double y;   // Y coordinate

    public Item(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Item{id=" + id + ", x=" + x + ", y=" + y + "}";
    }
}

