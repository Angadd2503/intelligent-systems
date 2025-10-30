package vrp;

import java.io.Serializable;

public class Item implements Serializable {
    private final String id;
    private final double x;
    private final double y;
    private final int demand;
    private final int startTime;   // Time window start
    private final int endTime;     // Time window end

    // --- Constructors ---
    // Basic constructor (no time window)
    public Item(String id, double x, double y) {
        this(id, x, y, 1, 0, Integer.MAX_VALUE);
    }

    // With demand
    public Item(String id, double x, double y, int demand) {
        this(id, x, y, demand, 0, Integer.MAX_VALUE);
    }

    // With demand and time window (main constructor)
    public Item(String id, double x, double y, int demand, int startTime, int endTime) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // --- Getters ---
    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getDemand() { return demand; }
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }

    // --- Distance function ---
    public double distanceTo(Item other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("%s(%.1f, %.1f, d=%d, TW=[%d,%d])",
                id, x, y, demand, startTime, endTime);
    }
}
