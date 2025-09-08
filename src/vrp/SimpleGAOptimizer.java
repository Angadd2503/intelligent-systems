package vrp;

import java.util.*;

public class SimpleGAOptimizer {
    public static class Result {
        public List<List<Item>> routes = new ArrayList<>();
        public double totalDistance;
        public int itemsDelivered;
    }

    private Random rand = new Random();

    public Result solve(List<Item> items, int numVehicles, int capacity,
                        int generations, int popSize) {
        // init population
        List<List<Item>> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            List<Item> copy = new ArrayList<>(items);
            Collections.shuffle(copy);
            population.add(copy);
        }

        List<Item> best = null;
        double bestFit = -1;

        // evolve
        for (int g = 0; g < generations; g++) {
            List<List<Item>> newPop = new ArrayList<>();
            for (int i = 0; i < popSize; i++) {
                List<Item> p1 = population.get(rand.nextInt(popSize));
                List<Item> p2 = population.get(rand.nextInt(popSize));
                List<Item> child = crossover(p1, p2);
                if (rand.nextDouble() < 0.2) mutate(child);
                newPop.add(child);
            }
            population = newPop;

            for (List<Item> chrom : population) {
                double fit = fitness(chrom, numVehicles, capacity);
                if (fit > bestFit) {
                    bestFit = fit;
                    best = chrom;
                }
            }
        }

        return decode(best, numVehicles, capacity);
    }

    private List<Item> crossover(List<Item> p1, List<Item> p2) {
        int n = p1.size();
        int a = rand.nextInt(n), b = rand.nextInt(n);
        if (a > b) { int t = a; a = b; b = t; }
        List<Item> child = new ArrayList<>(Collections.nCopies(n, null));
        for (int i = a; i <= b; i++) child.set(i, p1.get(i));
        int idx = (b + 1) % n;
        for (Item it : p2) {
            if (!child.contains(it)) {
                child.set(idx, it);
                idx = (idx + 1) % n;
                if (idx >= n) idx = 0;
            }
        }
        return child;
    }

    private void mutate(List<Item> chrom) {
        int i = rand.nextInt(chrom.size());
        int j = rand.nextInt(chrom.size());
        Collections.swap(chrom, i, j);
    }

    private double fitness(List<Item> chrom, int numVehicles, int capacity) {
        Result r = decode(chrom, numVehicles, capacity);
        return r.itemsDelivered * 1000 - r.totalDistance;
    }

    private Result decode(List<Item> chrom, int numVehicles, int capacity) {
        Result res = new Result();
        for (int v = 0; v < numVehicles; v++) res.routes.add(new ArrayList<>());

        int vehicle = 0;
        for (Item it : chrom) {
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

    private double computeDistance(List<List<Item>> routes) {
        double sum = 0;
        for (List<Item> route : routes) {
            double x = 0, y = 0;
            for (Item it : route) {
                sum += Math.hypot(it.getX() - x, it.getY() - y);
                x = it.getX(); y = it.getY();
            }
            sum += Math.hypot(x, y); // back to depot
        }
        return sum;
    }

    // Quick test
    public static void main(String[] args) throws Exception {
        List<Item> items = ItemsParser.load("data/Items.txt");
        SimpleGAOptimizer ga = new SimpleGAOptimizer();
        Result r = ga.solve(items, 2, 3, 100, 20);
        System.out.println("GA: items=" + r.itemsDelivered + " dist=" + r.totalDistance);
    }
}

