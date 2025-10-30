package vrp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SimpleGAOptimizer {

    public static class Result {
        public final List<List<Item>> routes = new ArrayList<>();
        public double totalDistance;
        public int itemsDelivered;
    }

    private final Random rand = new Random();

    public Result solve(
            List<Item> items,
            int numVehicles,
            int capacity,
            double dv,
            int generations,
            int popSize
    ) {

        // Initial population: random permutations of all items
        List<List<Item>> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            List<Item> copy = new ArrayList<>(items);
            Collections.shuffle(copy, rand);
            population.add(copy);
        }

        List<Item> best = null;
        double bestFit = Double.NEGATIVE_INFINITY;

        for (int g = 0; g < generations; g++) {
            // breed / mutate
            List<List<Item>> newPop = new ArrayList<>(popSize);
            for (int i = 0; i < popSize; i++) {
                List<Item> p1 = population.get(rand.nextInt(popSize));
                List<Item> p2 = population.get(rand.nextInt(popSize));
                List<Item> child = crossover(p1, p2);
                if (rand.nextDouble() < 0.2) {
                    mutate(child);
                }
                newPop.add(child);
            }
            population = newPop;

            for (List<Item> chrom : population) {
                double fit = fitnessBalanced(chrom, numVehicles, capacity, dv);
                if (fit > bestFit) {
                    bestFit = fit;
                    best = chrom;
                }
            }
        }

        return decodeBalanced(best, numVehicles, capacity, dv);
    }

    // ----- GA operators -----
    private List<Item> crossover(List<Item> p1, List<Item> p2) {
        int n = p1.size();
        if (n == 0) return new ArrayList<>();
        int a = rand.nextInt(n), b = rand.nextInt(n);
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }

        List<Item> child = new ArrayList<>(Collections.nCopies(n, (Item) null));
        for (int i = a; i <= b; i++) {
            child.set(i, p1.get(i));
        }

        int idx = (b + 1) % n;
        for (Item it : p2) {
            if (!child.contains(it)) {
                child.set(idx, it);
                idx = (idx + 1) % n;
            }
        }
        return child;
    }

    private void mutate(List<Item> chrom) {
        if (chrom.size() < 2) return;
        int i = rand.nextInt(chrom.size());
        int j = rand.nextInt(chrom.size());
        Collections.swap(chrom, i, j);
    }

    // ----- Fitness using the balanced decoder -----
    private double fitnessBalanced(List<Item> chrom,
                                   int numVehicles,
                                   int capacity,
                                   double dv) {
        Result r = decodeBalanced(chrom, numVehicles, capacity, dv);
        // Reward delivered items heavily; penalize distance
        return r.itemsDelivered * 1000.0 - r.totalDistance;
    }

    // ===== Balanced decoder with time windows =====
    // We assign each item from the chromosome to the "best" vehicle that:
    // - still has capacity
    // - can reach within the time window
    // - does not break the dv (max distance) limit
    private Result decodeBalanced(List<Item> chrom,
                                  int numVehicles,
                                  int capacity,
                                  double dv) {

        Result res = new Result();
        for (int v = 0; v < numVehicles; v++) {
            res.routes.add(new ArrayList<>());
        }

        double[] curX = new double[numVehicles];
        double[] curY = new double[numVehicles];
        double[] curDist = new double[numVehicles];   // distance traveled so far (not incl. final return yet)
        double[] curTime = new double[numVehicles];   // time so far
        int[] curCount = new int[numVehicles];

        for (int i = 0; i < numVehicles; i++) {
            curX[i] = 0.0;
            curY[i] = 0.0;
            curDist[i] = 0.0;
            curTime[i] = 0.0;
            curCount[i] = 0;
        }

        for (Item it : chrom) {
            int bestV = -1;
            double bestScore = Double.POSITIVE_INFINITY; // lower is better

            for (int v = 0; v < numVehicles; v++) {
                if (curCount[v] >= capacity) continue; // capacity full

                // distance from vehicle v current pos -> this item
                double travelLeg = hypot(it.getX() - curX[v], it.getY() - curY[v]);

                // arrival time if we go now
                double arrival = curTime[v] + travelLeg;

                // we can wait if early
                double serviceStart = Math.max(arrival, it.getStartTime());

                // if still past endTime, can't serve on this vehicle
                if (serviceStart > it.getEndTime()) {
                    continue;
                }

                // predict new distance so far if we add this point
                double newDistSoFar = curDist[v] + travelLeg;

                // plus distance to go back to depot after visiting this item
                double backHome = hypot(it.getX(), it.getY());
                double projectedTotalDistForThisVehicle = newDistSoFar + backHome;

                // enforce dv limit (per-vehicle max distance)
                if (projectedTotalDistForThisVehicle > dv) {
                    continue;
                }

                // score: greedy choose the vehicle with smallest added distance
                // we can bias by 'v' a little to break ties
                double score = projectedTotalDistForThisVehicle + 1e-6 * v;

                if (score < bestScore) {
                    bestScore = score;
                    bestV = v;
                }
            }

            if (bestV >= 0) {
                // Assign item to that vehicle
                double travelLeg = hypot(it.getX() - curX[bestV], it.getY() - curY[bestV]);
                double arrival = curTime[bestV] + travelLeg;
                double serviceStart = Math.max(arrival, it.getStartTime());

                // update route state
                res.routes.get(bestV).add(it);
                curDist[bestV] += travelLeg;
                curX[bestV] = it.getX();
                curY[bestV] = it.getY();
                curTime[bestV] = serviceStart; // assume instant service
                curCount[bestV]++;
            }
        }

        // Local improvement step (2-opt per route) to shorten path distance
        for (List<Item> route : res.routes) {
            twoOpt(route);
        }

        // Compute totals for all routes:
        res.totalDistance = 0.0;
        int delivered = 0;

        for (int v = 0; v < numVehicles; v++) {
            List<Item> route = res.routes.get(v);
            if (route.isEmpty()) continue;

            double x = 0.0;
            double y = 0.0;
            double d = 0.0;

            for (Item it : route) {
                d += hypot(it.getX() - x, it.getY() - y);
                x = it.getX();
                y = it.getY();
            }

            // return to depot
            d += hypot(x, y);

            res.totalDistance += d;
            delivered += route.size();
        }

        res.itemsDelivered = delivered;
        return res;
    }

    // 2-opt to reduce distance for each vehicle route
    private static void twoOpt(List<Item> route) {
        int n = route.size();
        if (n < 4) return;
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < n - 3; i++) {
                for (int k = i + 2; k < n - 1; k++) {
                    double a1 = segLen(route, i, i + 1);
                    double a2 = segLen(route, k, k + 1);
                    double b1 = segLen(route, i, k);
                    double b2 = segLen(route, i + 1, k + 1);
                    if (b1 + b2 + 1e-9 < a1 + a2) {
                        // reverse (i+1..k)
                        for (int l = i + 1, r = k; l < r; l++, r--) {
                            Collections.swap(route, l, r);
                        }
                        improved = true;
                    }
                }
            }
        }
    }

    private static double segLen(List<Item> r, int a, int b) {
        double xa = (a >= 0 && a < r.size()) ? r.get(a).getX() : 0.0;
        double ya = (a >= 0 && a < r.size()) ? r.get(a).getY() : 0.0;
        double xb = (b >= 0 && b < r.size()) ? r.get(b).getX() : 0.0;
        double yb = (b >= 0 && b < r.size()) ? r.get(b).getY() : 0.0;
        return hypot(xb - xa, yb - ya);
    }

    // Utilities
    private static double hypot(double dx, double dy) {
        return Math.hypot(dx, dy);
    }
}
