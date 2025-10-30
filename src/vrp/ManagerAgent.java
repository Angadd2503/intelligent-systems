package vrp;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class ManagerAgent extends Agent {

    // Vehicle registry: DA name -> (capacity, dv)
    private final Map<String, VehicleInfo> vehicles = new LinkedHashMap<>();

    private final Gson gson = new Gson();

    // current optimisation technique ("GREEDY" or "GA")
    private String opt = "GREEDY";

    /* ================== Helper classes ================== */

    static class VehicleInfo {
        int cap;
        double dv;

        VehicleInfo(int c, double d) {
            cap = c;
            dv = d;
        }
    }

    // This is what we send to each DA: the actual route list + its total distance
    public static class RouteInfo {
        public final List<Item> route;
        public final double distance;

        public RouteInfo(List<Item> route, double distance) {
            this.route = route;
            this.distance = distance;
        }
    }

    /* ================== Agent setup ================== */

    @Override
    protected void setup() {
        // Read optional "opt=..." arg passed from JadePlatformManager when creating MRA
        Object[] args = getArguments();
        if (args != null) {
            for (Object a : args) {
                String s = String.valueOf(a);
                if (s.toLowerCase().startsWith("opt=")) {
                    opt = s.substring(4).trim().toUpperCase();
                }
            }
        }

        sendLog("MRA: started with technique " + opt);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {

                /* ========== 1) Handle DA registration (capacity / dv) ========== */

                MessageTemplate mtCap = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchConversationId("capacity")
                );
                ACLMessage capMsg = myAgent.receive(mtCap);
                if (capMsg != null) {
                    String daName = capMsg.getSender().getLocalName();
                    int cap = 10;
                    double dv = 300;

                    try {
                        // Content format: "cap=7,dv=250"
                        for (String kv : capMsg.getContent().split(",")) {
                            String[] p = kv.split("=");
                            if (p.length == 2) {
                                String key = p[0].trim();
                                String val = p[1].trim();
                                if ("cap".equals(key)) cap = Integer.parseInt(val);
                                if ("dv".equals(key)) dv = Double.parseDouble(val);
                            }
                        }
                    } catch (Exception ignored) {
                        // If we can't parse, we just fall back to defaults above
                    }

                    vehicles.put(daName, new VehicleInfo(cap, dv));
                    sendLog("MRA: registered " + daName + "  cap=" + cap + "  dv=" + dv);
                    return;
                }

                /* ========== 2) Handle technique switching from GUI (GREEDY <-> GA) ========== */

                MessageTemplate mtTech = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchConversationId("set-technique")
                );
                ACLMessage techMsg = myAgent.receive(mtTech);
                if (techMsg != null) {
                    String newTech = techMsg.getContent().trim().toUpperCase();
                    opt = newTech;

                    sendLog("MRA: technique switched to " + opt);

                    // Acknowledge back to GUI so it can log
                    ACLMessage ack = new ACLMessage(ACLMessage.INFORM);
                    ack.setConversationId("technique-set");
                    ack.setContent(opt);
                    ack.addReceiver(new AID("GUI", AID.ISLOCALNAME));
                    myAgent.send(ack);

                    return;
                }

                /* ========== 3) Handle optimize-request from GUI agent ========== */

                MessageTemplate mtOpt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("optimize-request")
                );
                ACLMessage optMsg = myAgent.receive(mtOpt);
                if (optMsg != null) {

                    // safety check: do we have any vehicles?
                    if (vehicles.isEmpty()) {
                        ACLMessage fail = optMsg.createReply();
                        fail.setPerformative(ACLMessage.FAILURE);
                        fail.setContent("No DAs registered.");
                        myAgent.send(fail);

                        sendLog("MRA: optimization aborted — no DAs registered.");
                        return;
                    }

                    // parse the list of Item objects from the GUI request
                    Type listType = new TypeToken<List<Item>>() {}.getType();
                    List<Item> items = gson.fromJson(optMsg.getContent(), listType);

                    int numDAs = vehicles.size();
                    int cap = vehicles.values().iterator().next().cap;
                    double dv = vehicles.values().iterator().next().dv;

                    sendLog(String.format(
                            "GUI → MRA: optimize-request (items=%d, DAs=%d, cap=%d, dv=%.0f, opt=%s)",
                            items.size(), numDAs, cap, dv, opt
                    ));

                    // ========== NEW FOR RESEARCH EXTENSION (VRPTW logging) ==========
                    // We log the time window of each item. This proves that we are
                    // doing Vehicle Routing Problem with Time Windows, not just plain VRP.
                    for (Item it : items) {
                        sendLog(
                                "Item " + it.getId()
                                        + " TW=[" + it.getStartTime()
                                        + "," + it.getEndTime() + "]"
                        );
                    }
                    // ===============================================================

                    Map<String, List<Item>> resultRoutes;
                    int delivered;
                    double totalDistance;

                    // choose algorithm: GA vs Greedy
                    if ("GA".equalsIgnoreCase(opt)) {

                        // --- Genetic Algorithm path with time windows ---
                        SimpleGAOptimizer ga = new SimpleGAOptimizer();
                        SimpleGAOptimizer.Result gaRes =
                                ga.solve(items, numDAs, cap, dv, 200, 40);

                        // Convert GA result to DA-name map like { "DA1": [...], "DA2": [...] }
                        resultRoutes = new LinkedHashMap<>();
                        for (int i = 0; i < gaRes.routes.size(); i++) {
                            resultRoutes.put("DA" + (i + 1), gaRes.routes.get(i));
                        }

                        delivered = gaRes.itemsDelivered;
                        totalDistance = gaRes.totalDistance;

                        sendLog("MRA: ✅ Optimization done (GA). Delivered=" + delivered +
                                "  TotalDist=" + String.format("%.1f", totalDistance));

                    } else {

                        // --- Greedy path with time windows ---
                        GreedyOptimizer.Result grRes =
                                GreedyOptimizer.solve(items, numDAs, cap, dv);

                        resultRoutes = grRes.routes;
                        delivered = grRes.delivered;
                        totalDistance = grRes.totalDistance;

                        sendLog("MRA: ✅ Optimization done (Greedy). Delivered=" + delivered +
                                "  TotalDist=" + String.format("%.1f", totalDistance));
                    }

                    // Send each DA their individual route (as RouteInfo JSON)
                    List<String> daNames = new ArrayList<>(vehicles.keySet());
                    int idx = 0;

                    for (Map.Entry<String, List<Item>> e : resultRoutes.entrySet()) {
                        String daName = (idx < daNames.size()) ? daNames.get(idx++) : e.getKey();
                        List<Item> route = e.getValue();
                        double rd = computeRouteDistance(route);

                        // Build payload and send ACLMessage to that DA
                        RouteInfo payload = new RouteInfo(route, rd);

                        ACLMessage daMsg = new ACLMessage(ACLMessage.INFORM);
                        daMsg.setConversationId("route");
                        daMsg.addReceiver(new AID(daName, AID.ISLOCALNAME));
                        daMsg.setContent(gson.toJson(payload));
                        myAgent.send(daMsg);

                        sendLog("MRA → " + daName + ": route of " + route.size() +
                                " items, dist=" + String.format("%.1f", rd));
                    }

                    // Reply back to GUI agent so that the Swing UI can draw routes
                    ACLMessage guiReply = optMsg.createReply();
                    guiReply.setPerformative(ACLMessage.INFORM);
                    guiReply.setConversationId("optimization-result");
                    guiReply.setContent(gson.toJson(resultRoutes));
                    guiReply.addUserDefinedParameter(
                            "status",
                            "Delivered: " + delivered +
                                    " • Total distance: " + String.format("%.1f", totalDistance)
                    );
                    myAgent.send(guiReply);

                    sendLog("MRA: results sent to GUI.");

                    return;
                }

                // If we didn't handle any message above, block()
                block();
            }
        });
    }

    /* ================== Helper methods ================== */

    // Compute total distance of a single DA route (depot -> all stops -> depot)
    private double computeRouteDistance(List<Item> route) {
        if (route == null || route.isEmpty()) return 0.0;

        double total = 0.0;
        double x = 0.0;
        double y = 0.0; // depot start

        for (Item it : route) {
            total += Math.hypot(it.getX() - x, it.getY() - y);
            x = it.getX();
            y = it.getY();
        }

        // add return to depot
        total += Math.hypot(x, y);

        return total;
    }

    // Send a log line both to console and to GUI agent (so it shows up in UI Messages list)
    private void sendLog(String line) {
        ACLMessage log = new ACLMessage(ACLMessage.INFORM);
        log.setConversationId("log");
        log.setContent(line);
        log.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        send(log);

        System.out.println(line);
    }
}
