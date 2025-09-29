package vrp;

import java.util.Collections;
import java.util.Random;

public class SimpleGAOptimizer {

    public static class Result {
        public final java.util.List<java.util.List<Item>> routes = new java.util.ArrayList<>();
        public double totalDistance;
        public int itemsDelivered;
    }

    private final Random rand = new Random();

    public Result solve(java.util.List<Item> items,
                        int numVehicles,
                        int capacity,
                        int generations,
                        int popSize) {

        // población inicial: permutaciones de los items
        java.util.List<java.util.List<Item>> population = new java.util.ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            java.util.List<Item> copy = new java.util.ArrayList<>(items);
            Collections.shuffle(copy, rand);
            population.add(copy);
        }

        java.util.List<Item> best = null;
        double bestFit = Double.NEGATIVE_INFINITY;

        // evolución
        for (int g = 0; g < generations; g++) {
            java.util.List<java.util.List<Item>> newPop = new java.util.ArrayList<>();
            for (int i = 0; i < popSize; i++) {
                java.util.List<Item> p1 = population.get(rand.nextInt(popSize));
                java.util.List<Item> p2 = population.get(rand.nextInt(popSize));
                java.util.List<Item> child = crossover(p1, p2);
                if (rand.nextDouble() < 0.2) mutate(child);
                newPop.add(child);
            }
            population = newPop;

            for (java.util.List<Item> chrom : population) {
                double fit = fitness(chrom, numVehicles, capacity);
                if (fit > bestFit) {
                    bestFit = fit;
                    best = chrom;
                }
            }
        }

        return decode(best, numVehicles, capacity);
    }

    private java.util.List<Item> crossover(java.util.List<Item> p1, java.util.List<Item> p2) {
        int n = p1.size();
        if (n == 0) return new java.util.ArrayList<>();
        int a = rand.nextInt(n), b = rand.nextInt(n);
        if (a > b) { int t = a; a = b; b = t; }

        java.util.List<Item> child = new java.util.ArrayList<>(java.util.Collections.nCopies(n, (Item) null));
        for (int i = a; i <= b; i++) child.set(i, p1.get(i));

        int idx = (b + 1) % n;
        for (Item it : p2) {
            if (!child.contains(it)) {
                child.set(idx, it);
                idx = (idx + 1) % n;
            }
        }
        return child;
    }

    private void mutate(java.util.List<Item> chrom) {
        if (chrom.isEmpty()) return;
        int i = rand.nextInt(chrom.size());
        int j = rand.nextInt(chrom.size());
        java.util.Collections.swap(chrom, i, j);
    }

    private double fitness(java.util.List<Item> chrom, int numVehicles, int capacity) {
        Result r = decode(chrom, numVehicles, capacity);
        // Prioriza # items entregados; penaliza distancia
        return r.itemsDelivered * 1000.0 - r.totalDistance;
    }

    private Result decode(java.util.List<Item> chrom, int numVehicles, int capacity) {
        Result res = new Result();
        for (int v = 0; v < numVehicles; v++) res.routes.add(new java.util.ArrayList<>());

        int vehicle = 0;
        for (Item it : chrom) {
            if (res.routes.get(vehicle).size() >= capacity) {
                vehicle++;
                if (vehicle >= numVehicles) break;
            }
            res.routes.get(vehicle).add(it);
        }

        res.itemsDelivered = res.routes.stream().mapToInt(java.util.List::size).sum();
        res.totalDistance = computeDistance(res.routes);
        return res;
    }

    private double computeDistance(java.util.List<java.util.List<Item>> routes) {
        double sum = 0.0;
        for (java.util.List<Item> route : routes) {
            double x = 0.0, y = 0.0; // depot (0,0)
            for (Item it : route) {
                sum += Math.hypot(it.getX() - x, it.getY() - y);
                x = it.getX(); y = it.getY();
            }
            sum += Math.hypot(x, y); // regreso al depósito
        }
        return sum;
    }

    // Pequeña prueba local
    public static void main(String[] args) throws Exception {
        java.util.List<Item> items = ItemsParser.parseItems("data/items.txt");
        SimpleGAOptimizer ga = new SimpleGAOptimizer();
        Result r = ga.solve(items, 2, 3, 100, 20);
        System.out.println("GA: items=" + r.itemsDelivered + " dist=" + r.totalDistance);
    }
}
