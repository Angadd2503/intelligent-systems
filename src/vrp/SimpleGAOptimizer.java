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
                        double dv,
                        int generations,
                        int popSize) {

        // población inicial: permutaciones
        java.util.List<java.util.List<Item>> population = new java.util.ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            java.util.List<Item> copy = new java.util.ArrayList<>(items);
            Collections.shuffle(copy, rand);
            population.add(copy);
        }

        java.util.List<Item> best = null;
        double bestFit = Double.NEGATIVE_INFINITY;

        for (int g = 0; g < generations; g++) {
            // selección por torneo simple
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
                double fit = fitness(chrom, numVehicles, capacity, dv);
                if (fit > bestFit) {
                    bestFit = fit;
                    best = chrom;
                }
            }
        }
        return decode(best, numVehicles, capacity, dv);
    }

    // Order Crossover (OX) simplificado
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
        if (chrom.size() < 2) return;
        int i = rand.nextInt(chrom.size());
        int j = rand.nextInt(chrom.size());
        java.util.Collections.swap(chrom, i, j);
    }

    private double fitness(java.util.List<Item> chrom, int numVehicles, int capacity, double dv) {
        Result r = decode(chrom, numVehicles, capacity, dv);
        return r.itemsDelivered * 1000.0 - r.totalDistance;
    }

    private Result decode(java.util.List<Item> chrom, int numVehicles, int capacity, double dv) {
        Result res = new Result();
        for (int v = 0; v < numVehicles; v++) res.routes.add(new java.util.ArrayList<>());

        int v = 0;
        double vx = 0.0, vy = 0.0; // posición actual del vehículo
        double vdist = 0.0;        // distancia acumulada SIN incluir la vuelta
        int vcount = 0;            // items cargados en el vehículo actual

        for (Item it : chrom) {
            if (v >= numVehicles) break;

            // si se llenó la capacidad, cerramos y pasamos al siguiente vehículo
            if (vcount >= capacity) {
                // reiniciar estado para el siguiente vehículo
                v++;
                if (v >= numVehicles) break;
                vx = 0.0; vy = 0.0; vdist = 0.0; vcount = 0;
            }

            double leg = Math.hypot(it.getX() - vx, it.getY() - vy);
            double projected = vdist + leg + Math.hypot(it.getX(), it.getY()); // ida + vuelta desde el nuevo punto
            if (projected <= dv) {
                res.routes.get(v).add(it);
                vdist += leg;
                vx = it.getX(); vy = it.getY();
                vcount++;
            }
            // si no cabe por dv, probamos siguiente ítem (estrategia simple)
        }

        // calcular la distancia total (una sola vez por ruta)
        res.totalDistance = 0.0;
        for (java.util.List<Item> route : res.routes) {
            if (route.isEmpty()) continue;
            double x = 0.0, y = 0.0;
            for (Item it : route) {
                res.totalDistance += Math.hypot(it.getX() - x, it.getY() - y);
                x = it.getX(); y = it.getY();
            }
            res.totalDistance += Math.hypot(x, y); // regresar al depósito
        }

        res.itemsDelivered = res.routes.stream().mapToInt(java.util.List::size).sum();
        return res;
    }

    // distancia dentro de la ruta (sin la vuelta)
    private double intraDistance(java.util.List<Item> route) {
        double sum = 0.0;
        double x = 0.0, y = 0.0;
        for (Item it : route) {
            sum += Math.hypot(it.getX() - x, it.getY() - y);
            x = it.getX(); y = it.getY();
        }
        return sum;
    }

    // distancia completa de la ruta SIN incluir el cierre (lo sumo aparte arriba para consistencia)
    private double pathDistance(java.util.List<Item> route) {
        if (route.isEmpty()) return 0.0;
        double sum = 0.0;
        double x = 0.0, y = 0.0;
        for (Item it : route) {
            sum += Math.hypot(it.getX() - x, it.getY() - y);
            x = it.getX(); y = it.getY();
        }
        return sum;
    }
}
