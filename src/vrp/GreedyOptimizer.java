package vrp;

import java.util.LinkedHashMap;

public class GreedyOptimizer {

    public static class Result {
        public final java.util.Map<String, java.util.List<Item>> routes = new LinkedHashMap<>();
        public int delivered = 0;
        public double totalDistance = 0.0;
    }

    public static Result solve(java.util.List<Item> items,
                               int numDAs,
                               int capacityPerDA,
                               double dvPerDA) {

        Result res = new Result();
        java.util.List<Item> remaining = new java.util.ArrayList<>(items);

        for (int a = 1; a <= numDAs; a++) {
            String name = "DA" + a;
            java.util.List<Item> route = new java.util.ArrayList<>();
            res.routes.put(name, route);

            double traveled = 0.0;
            double curX = 0.0;
            double curY = 0.0; // depot (0,0)
            double curTime = 0.0; // time also starts at 0
            int capLeft = capacityPerDA;

            while (capLeft > 0 && !remaining.isEmpty()) {
                Item best = null;
                double bestGainDist = Double.POSITIVE_INFINITY;

                for (Item it : remaining) {
                    int dmd = it.getDemand();
                    if (dmd > capLeft) continue; // capacity check

                    // distance to go from current pos -> this item
                    double distToItem = dist(curX, curY, it.getX(), it.getY());

                    // newTime = arrive time (curTime + travel)
                    double arrivalTime = curTime + distToItem;

                    // you can wait until startTime if early
                    double serviceStart = Math.max(arrivalTime, it.getStartTime());

                    // but you cannot serve if you would miss the endTime
                    if (serviceStart > it.getEndTime()) {
                        // can't meet this item's time window
                        continue;
                    }

                    // projected distance including going back to depot
                    double backHome = dist(it.getX(), it.getY(), 0, 0);
                    double projectedTotalDist = traveled + distToItem + backHome;

                    // vehicle distance limit check
                    if (projectedTotalDist > dvPerDA) {
                        continue;
                    }

                    // choose the closest feasible item
                    if (distToItem < bestGainDist) {
                        bestGainDist = distToItem;
                        best = it;
                    }
                }

                if (best == null) break; // no more feasible stops

                // update distance/time/pos with chosen item
                double leg = dist(curX, curY, best.getX(), best.getY());
                double arrival = curTime + leg;
                double startService = Math.max(arrival, best.getStartTime());
                // assume instant service/drop-off for simplicity
                double leaveTime = startService;

                traveled += leg;
                curX = best.getX();
                curY = best.getY();
                curTime = leaveTime;

                route.add(best);
                remaining.remove(best);
                capLeft -= best.getDemand();
            }

            // close route: return to depot
            double lastLeg = dist(curX, curY, 0, 0);
            traveled += lastLeg;

            // if returning home violates dvPerDA, trim last stops
            while (traveled > dvPerDA && !route.isEmpty()) {
                Item last = route.remove(route.size() - 1);
                remaining.add(last);

                // recompute route distance/time after removing last
                RecalcResult rr = recomputeRoute(route);
                traveled = rr.totalDistWithReturn;
            }

            res.delivered += route.size();
            res.totalDistance += traveled;
        }

        return res;
    }

    // helper to recompute full route stats after popping last item
    private static RecalcResult recomputeRoute(java.util.List<Item> route) {
        double x = 0.0, y = 0.0;
        double distAccum = 0.0;

        for (Item it : route) {
            distAccum += dist(x, y, it.getX(), it.getY());
            x = it.getX();
            y = it.getY();
        }

        // distance + return to depot
        double totalWithReturn = distAccum + dist(x, y, 0, 0);

        RecalcResult r = new RecalcResult();
        r.totalDistWithReturn = totalWithReturn;
        return r;
    }

    private static class RecalcResult {
        double totalDistWithReturn;
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}
