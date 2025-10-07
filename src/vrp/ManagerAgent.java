package vrp;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

public class ManagerAgent extends Agent {
    private final Map<String, VehicleInfo> vehicles = new LinkedHashMap<>();
    private final Gson gson = new Gson();

    static class VehicleInfo {
        int cap; double dv;
        VehicleInfo(int c, double d) { cap = c; dv = d; }
    }

    public static class RouteInfo {
        public final List<Item> route;
        public final double distance;

        public RouteInfo(List<Item> route, double distance) {
            this.route = route;
            this.distance = distance;
        }
    }
    private double calculateRouteDistance(List<Item> route) {
        if (route == null || route.isEmpty()) {
            return 0.0;
        }
        double totalDist = 0;
        Item depot = new Item("DEPOT", 0, 0, 0);
        Item lastStop = depot;
        for (Item item : route) {
            totalDist += lastStop.distance(item);
            lastStop = item;
        }
        totalDist += lastStop.distance(depot); // Volver al depósito
        return totalDist;
    }

    @Override
    protected void setup() {
        System.out.println("ManagerAgent " + getAID().getLocalName() + " is ready.");
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Template 1: Listen for DA registrations
                MessageTemplate mtCapacity = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchConversationId("capacity")
                );
                ACLMessage capacityMsg = myAgent.receive(mtCapacity);
                if (capacityMsg != null) {
                    String senderName = capacityMsg.getSender().getLocalName();
                    String content = capacityMsg.getContent();
                    int cap = 10; double dv = 300;
                    try {
                        for (String kv : content.split(",")) {
                            String[] p = kv.split("=");
                            if (p.length == 2) {
                                if (p[0].trim().equals("cap")) cap = Integer.parseInt(p[1].trim());
                                if (p[0].trim().equals("dv"))  dv  = Double.parseDouble(p[1].trim());
                            }
                        }
                    } catch (Exception e) { System.err.println("Error parsing capacity from " + senderName); }
                    vehicles.put(senderName, new VehicleInfo(cap, dv));
                    System.out.println("MRA: Registered " + senderName + " [cap=" + cap + ", dv=" + dv + "]");
                    return;
                }

                // Template 2: Listen for optimization requests from the GUI
                MessageTemplate mtOptimize = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("optimize-request")
                );
                ACLMessage optimizeMsg = myAgent.receive(mtOptimize);
                if (optimizeMsg != null) {
                    if (vehicles.isEmpty()) {
                        ACLMessage failureReply = optimizeMsg.createReply();
                        failureReply.setPerformative(ACLMessage.FAILURE);
                        failureReply.setContent("No DAs registered.");
                        myAgent.send(failureReply);
                        return;
                    }
                    String jsonContent = optimizeMsg.getContent();
                    Type listType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(jsonContent, listType);
                    int cap = vehicles.values().stream().findFirst().get().cap;
                    double dv = vehicles.values().stream().findFirst().get().dv;

                    GreedyOptimizer.Result res = GreedyOptimizer.solve(items, vehicles.size(), cap, dv);

                    ACLMessage guiReply = optimizeMsg.createReply();
                    guiReply.setPerformative(ACLMessage.INFORM);
                    guiReply.setConversationId("optimization-result");

                    Map<String, List<Item>> finalRoutes = new HashMap<>();
                    List<String> daNames = new ArrayList<>(vehicles.keySet());
                    int daIndex = 0;
                    for(Map.Entry<String, List<Item>> routeEntry : res.routes.entrySet()) {
                        if (daIndex < daNames.size()) {
                            String daName = daNames.get(daIndex++);
                            List<Item> route = routeEntry.getValue();
                            finalRoutes.put(daName, route);

                            double routeDistance = calculateRouteDistance(route);

                            // Crea el objeto RouteInfo
                            RouteInfo routeInfo = new RouteInfo(route, routeDistance);

                            // Envía el objeto RouteInfo como JSON
                            ACLMessage daRouteMsg = new ACLMessage(ACLMessage.INFORM);
                            daRouteMsg.setConversationId("route");
                            daRouteMsg.addReceiver(new jade.core.AID(daName, jade.core.AID.ISLOCALNAME));
                            daRouteMsg.setContent(gson.toJson(routeInfo)); // <-- Enviamos el nuevo objeto
                            myAgent.send(daRouteMsg);

                            System.out.println("MRA: Enviada ruta a " + daName + " con distancia: " + String.format("%.1f", routeDistance));
                        }
                    }
                    String status = "Delivered: " + res.delivered + " | Total distance: " + String.format("%.1f", res.totalDistance);
                    guiReply.setContent(gson.toJson(finalRoutes));
                    guiReply.addUserDefinedParameter("status", status);
                    myAgent.send(guiReply);
                    System.out.println("MRA: Optimization complete. Results sent to GUI and routes to DAs.");
                    return;
                }
                block();
            }
        });
    }
}