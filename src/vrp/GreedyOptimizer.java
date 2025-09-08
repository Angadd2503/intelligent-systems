package vrp;

import java.util.*;

public class GreedyOptimizer {
    public static class Result {
        public List<List<Item>> routes = new ArrayList<>();
        public double totalDistance;
        public int itemsDelivered;
    }

    public static Result solve(List<Item> items, int numVehicles, int capacity) {
        Result res = new Result();
        for (int v = 0; v < numVehicles; v++) res.routes.add(new ArrayList<>());

        // Just assign items in order, fill each vehicle to capacity
        int vehicle = 0;
        for (Item it : items) {
            if (res.routes.get(vehicle).size() >= capacity) {
                vehicle++;
                if (vehicle >= numVehicles) break;
            }
            res.routes.get(vehicle).add(it);
        }

        res.itemsDelivered = res.routes.stream().mapToInt(List::size).sum();
        res.totalDistance = computeDistance(res.routes);
        return res;
    }

    private static double computeDistance(List<List<Item>> routes) {
        double sum = 0;
        for (List<Item> route : routes) {
            double x = 0, y = 0;
            for (Item it : route) {
                sum += Math.hypot(it.getX() - x, it.getY() - y);
                x = it.getX(); y = it.getY();
            }
            sum += Math.hypot(x, y); // return to depot
        }
        return sum;
    }

    public static void main(String[] args) throws Exception {
        List<Item> items = ItemsParser.load("data/Items.txt");
        Result r = GreedyOptimizer.solve(items, 2, 3);
        System.out.println("Greedy: items=" + r.itemsDelivered + " dist=" + r.totalDistance);
    }
}

