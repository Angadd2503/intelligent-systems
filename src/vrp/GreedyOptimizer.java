package vrp;

import java.util.*;

public class GreedyOptimizer {

    // Clase interna para devolver un resultado estructurado
    public static class Result {
        public final Map<String, List<Item>> routes;
        public final int delivered;
        public final double totalDistance;

        public Result(Map<String, List<Item>> routes, int delivered, double totalDistance) {
            this.routes = routes;
            this.delivered = delivered;
            this.totalDistance = totalDistance;
        }
    }

    public static Result solve(List<Item> allItems, int numVehicles, int capacity, double maxDistance) {
        Map<String, List<Item>> routes = new LinkedHashMap<>();
        Set<Item> unassignedItems = new HashSet<>(allItems);
        double totalDistance = 0;
        int deliveredCount = 0;

        Item depot = new Item("DEPOT", 0, 0, 0);

        for (int i = 1; i <= numVehicles; i++) {
            String vehicleId = "Route " + i;
            List<Item> currentRoute = new ArrayList<>();
            double currentDistance = 0;
            Item lastLocation = depot;

            while (currentRoute.size() < capacity) {
                Item closestItem = null;
                double minDistance = Double.MAX_VALUE;

                // Encontrar el item no asignado más cercano
                for (Item item : unassignedItems) {
                    double dist = lastLocation.distance(item);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestItem = item;
                    }
                }

                if (closestItem == null) {
                    break; // No hay más items para asignar
                }

                double distanceToNext = lastLocation.distance(closestItem);
                double returnToDepotDist = closestItem.distance(depot);

                if (currentDistance + distanceToNext + returnToDepotDist <= maxDistance) {
                    currentRoute.add(closestItem);
                    unassignedItems.remove(closestItem);
                    currentDistance += distanceToNext;
                    lastLocation = closestItem;
                } else {
                    // No se puede añadir este item sin exceder la distancia máxima, probar con otro.
                    // Para un greedy simple, simplemente paramos aquí para este vehículo.
                    break;
                }
            }

            currentDistance += lastLocation.distance(depot); // Añadir distancia de vuelta al depósito
            routes.put(vehicleId, currentRoute);
            totalDistance += currentDistance;
            deliveredCount += currentRoute.size();
        }

        return new Result(routes, deliveredCount, totalDistance);
    }
}
