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
    private final Map<String, VehicleInfo> vehicles = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private String opt = "GREEDY"; // optional, set if you pass opt=... when creating MRA

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

    @Override
    protected void setup() {
        // read optional opt=... param
        Object[] args = getArguments();
        if (args != null) {
            for (Object a : args) {
                String s = String.valueOf(a);
                if (s.toLowerCase().startsWith("opt=")) opt = s.substring(4).trim().toUpperCase();
            }
        }

        sendLog("MRA: started with technique " + opt);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // 1) DA registration
                MessageTemplate mtCap = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchConversationId("capacity")
                );
                ACLMessage capMsg = myAgent.receive(mtCap);
                if (capMsg != null) {
                    String da = capMsg.getSender().getLocalName();
                    int cap = 10; double dv = 300;
                    try {
                        for (String kv : capMsg.getContent().split(",")) {
                            String[] p = kv.split("=");
                            if (p.length == 2) {
                                if (p[0].trim().equals("cap")) cap = Integer.parseInt(p[1].trim());
                                if (p[0].trim().equals("dv"))  dv  = Double.parseDouble(p[1].trim());
                            }
                        }
                    } catch (Exception ignored) {}

                    vehicles.put(da, new VehicleInfo(cap, dv));
                    sendLog("MRA: registered " + da + "  cap=" + cap + "  dv=" + dv);
                    return;
                }

                // 2) Optimization request from GUI
                MessageTemplate mtOpt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("optimize-request")
                );
                ACLMessage optMsg = myAgent.receive(mtOpt);
                if (optMsg != null) {
                    if (vehicles.isEmpty()) {
                        ACLMessage fail = optMsg.createReply();
                        fail.setPerformative(ACLMessage.FAILURE);
                        fail.setContent("No DAs registered.");
                        myAgent.send(fail);
                        sendLog("MRA: optimization aborted — no DAs registered.");
                        return;
                    }

                    Type listType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(optMsg.getContent(), listType);

                    int numDAs = vehicles.size();
                    int cap = vehicles.values().iterator().next().cap;
                    double dv = vehicles.values().iterator().next().dv;

                    sendLog(String.format(
                            "GUI → MRA: optimize-request (items=%d, DAs=%d, cap=%d, dv=%.0f, opt=%s)",
                            items.size(), numDAs, cap, dv, opt));

                    // Greedy (you can branch here if you also wire GA)
                    GreedyOptimizer.Result res = GreedyOptimizer.solve(items, numDAs, cap, dv);
                    sendLog("MRA: ✅ Optimization done (Greedy). Delivered=" + res.delivered +
                            "  TotalDist=" + String.format("%.1f", res.totalDistance));

                    // send each DA its route + log per-DA
                    List<String> daNames = new ArrayList<>(vehicles.keySet());
                    int idx = 0;
                    for (Map.Entry<String, List<Item>> e : res.routes.entrySet()) {
                        String daName = (idx < daNames.size()) ? daNames.get(idx++) : e.getKey();
                        List<Item> route = e.getValue();
                        double rd = computeRouteDistance(route);

                        // send route to DA
                        RouteInfo payload = new RouteInfo(route, rd);
                        ACLMessage daMsg = new ACLMessage(ACLMessage.INFORM);
                        daMsg.setConversationId("route");
                        daMsg.addReceiver(new AID(daName, AID.ISLOCALNAME));
                        daMsg.setContent(gson.toJson(payload));
                        myAgent.send(daMsg);

                        sendLog("MRA → " + daName + ": route of " + route.size() +
                                " items, dist=" + String.format("%.1f", rd));
                    }

                    // reply to GUI with the routes map (for drawing)
                    ACLMessage guiReply = optMsg.createReply();
                    guiReply.setPerformative(ACLMessage.INFORM);
                    guiReply.setConversationId("optimization-result");
                    guiReply.setContent(gson.toJson(res.routes));
                    guiReply.addUserDefinedParameter(
                            "status",
                            "Delivered: " + res.delivered +
                                    " • Total distance: " + String.format("%.1f", res.totalDistance));
                    myAgent.send(guiReply);

                    sendLog("MRA: results sent to GUI.");
                    return;
                }

                block();
            }
        });
    }

    private double computeRouteDistance(List<Item> route) {
        if (route == null || route.isEmpty()) return 0.0;
        double total = 0.0;
        double x = 0.0, y = 0.0; // depot
        for (Item it : route) {
            total += Math.hypot(it.getX() - x, it.getY() - y);
            x = it.getX(); y = it.getY();
        }
        total += Math.hypot(x, y); // back to depot
        return total;
    }

    private void sendLog(String line) {
        ACLMessage log = new ACLMessage(ACLMessage.INFORM);
        log.setConversationId("log");
        log.setContent(line);
        log.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        send(log);
        System.out.println(line); // keep console too
    }
}
