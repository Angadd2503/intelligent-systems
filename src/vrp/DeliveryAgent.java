// DeliveryAgent.java
package vrp;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.google.gson.Gson;

public class DeliveryAgent extends Agent {
    private int capacity = 10;
    private double maxDistance = 300;
    private final Gson gson = new Gson();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            try {
                // Handle Integer or int
                if (args[0] instanceof Number) {
                    capacity = ((Number) args[0]).intValue();
                }
                // Handle Double or double
                if (args[1] instanceof Number) {
                    maxDistance = ((Number) args[1]).doubleValue();
                }
            } catch (Exception e) {
                System.err.println("DA " + getLocalName() + ": Error parsing args, using defaults. " + e.getMessage());
            }
        }

        System.out.println("DA: " + getLocalName() + " initialized with cap=" + capacity + " dv=" + maxDistance);

        // Register to MRA
        ACLMessage reg = new ACLMessage(ACLMessage.INFORM);
        reg.addReceiver(new AID("MRA", AID.ISLOCALNAME));
        reg.setConversationId("capacity");
        reg.setContent("cap=" + capacity + ",dv=" + maxDistance);
        send(reg);

        log("DA: " + getLocalName() + " registered cap=" + capacity + " dv=" + maxDistance);

        // Wait for routes
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchConversationId("route");
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    log("MRA → " + getLocalName() + ": route payload received");
                    log("payload: " + trim(msg.getContent(), 240));

                    // Parse and log distance if it matches RouteInfo
                    try {
                        ManagerAgent.RouteInfo info =
                                gson.fromJson(msg.getContent(), ManagerAgent.RouteInfo.class);
                        if (info != null) {
                            log(getLocalName() + ": route size=" +
                                    (info.route == null ? 0 : info.route.size()) +
                                    " distance=" + String.format("%.1f", info.distance));
                        }
                    } catch (Exception e) {
                        log(getLocalName() + ": Error parsing route: " + e.getMessage());
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void log(String line) {
        ACLMessage log = new ACLMessage(ACLMessage.INFORM);
        log.setConversationId("log");
        log.setContent(line);
        log.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        send(log);
        System.out.println(line);
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + " …";
    }
}