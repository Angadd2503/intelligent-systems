package vrp;

import java.util.LinkedHashMap;

public class GreedyOptimizer {

    public static class Result {
        public final java.util.Map<String, java.util.List<Item>> routes = new LinkedHashMap<>();
        public int delivered = 0;
        public double totalDistance = 0.0;
    }

    public static Result solve(java.util.List<Item> items, int numDAs, int capacityPerDA, double dvPerDA) {
        Result res = new Result();
        java.util.List<Item> remaining = new java.util.ArrayList<>(items);

        for (int a = 1; a <= numDAs; a++) {
            String name = "DA" + a;
            java.util.List<Item> route = new java.util.ArrayList<>();
            res.routes.put(name, route);

            double traveled = 0.0;
            int capLeft = capacityPerDA;
            double curX = 0.0, curY = 0.0; // depósito (0,0)

            while (capLeft > 0 && !remaining.isEmpty()) {
                Item best = null;
                double bestGain = Double.POSITIVE_INFINITY;

                for (Item it : remaining) {
                    double toItem = dist(curX, curY, it.getX(), it.getY());
                    double backHome = dist(it.getX(), it.getY(), 0, 0);
                    double projected = traveled + toItem + backHome;
                    if (projected <= dvPerDA && toItem < bestGain) {
                        bestGain = toItem; best = it;
                    }
                }
                if (best == null) break;

                traveled += dist(curX, curY, best.getX(), best.getY());
                curX = best.getX(); curY = best.getY();
                route.add(best);
                remaining.remove(best);
                capLeft--;
            }
            traveled += dist(curX, curY, 0, 0);

            // Si excede dv, retrocede últimos items hasta cumplir
            while (traveled > dvPerDA && !route.isEmpty()) {
                Item last = route.remove(route.size() - 1);
                remaining.add(last);
                traveled = recomputeDistance(route);
            }

            res.delivered += route.size();
            res.totalDistance += traveled;
        }
        return res;
    }

    private static double recomputeDistance(java.util.List<Item> route) {
        double d = 0.0, x = 0.0, y = 0.0;
        for (Item it : route) {
            d += dist(x, y, it.getX(), it.getY());
            x = it.getX(); y = it.getY();
        }
        d += dist(x, y, 0, 0);
        return d;
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.hypot(dx, dy);
        // Math.sqrt(dx*dx + dy*dy) también sirve
    }
}


