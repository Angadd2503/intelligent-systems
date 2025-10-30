package vrp;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class GuiAgent extends Agent {
    private MasUI ui;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    @Override
    protected void setup() {
        System.out.println("[GUI AGENT] Setup starting...");

        // Try to receive the MasUI instance as the first argument
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MasUI) {
            ui = (MasUI) args[0];
            System.out.println("[GUI AGENT] MasUI instance received successfully");

            // Hook UI → JADE actions
            ui.setOnOptimizeToJade(this::requestOptimizeFromMRA);
            ui.setOnTechniqueChange(this::requestTechniqueChange);
            System.out.println("[GUI AGENT] Callbacks registered");
        } else {
            System.out.println("[GUI AGENT] ERROR: No MasUI passed to GuiAgent!");
        }

        logToUi("GUI agent ready.");

        // Listen for messages from the platform
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) {
                    block();
                    return;
                }

                String cid = msg.getConversationId();
                System.out.println("[GUI AGENT] Received message with conversationId: " + cid);

                // 1) Plain log lines (sent by ManagerAgent/DeliveryAgents)
                if ("log".equals(cid)) {
                    logToUi(msg.getContent());
                }
                // 2) Optimization result → update routes on the UI
                else if ("optimization-result".equals(cid)) {
                    System.out.println("[GUI AGENT] Processing optimization-result");
                    try {
                        String json = msg.getContent();
                        System.out.println("[GUI AGENT] JSON content length: " + json.length());

                        java.lang.reflect.Type t =
                                new com.google.gson.reflect.TypeToken<Map<String, List<Item>>>() {}.getType();
                        Map<String, List<Item>> routes = gson.fromJson(json, t);
                        String status = msg.getUserDefinedParameter("status");

                        System.out.println("[GUI AGENT] Parsed routes: " + routes.size() + " vehicles");
                        System.out.println("[GUI AGENT] Status: " + status);

                        if (ui != null) {
                            SwingUtilities.invokeLater(() -> {
                                // update map + canvas + status bar (already existed)
                                ui.updateRoutes(routes, status);
                                logToUi("GUI: optimization-result → " + status);

                                // 🔥 NEW: show each DA route in the Messages panel with time windows
                                for (Map.Entry<String, List<Item>> entry : routes.entrySet()) {
                                    String daName = entry.getKey();
                                    List<Item> daRoute = entry.getValue();

                                    StringBuilder sb = new StringBuilder();
                                    sb.append(daName).append(" route: ");

                                    if (daRoute == null || daRoute.isEmpty()) {
                                        sb.append("(no stops)");
                                    } else {
                                        for (Item it : daRoute) {
                                            sb.append(it.getId());

                                            // show [start→end] if we have a real window
                                            int st = it.getStartTime();
                                            int en = it.getEndTime();

                                            // If start/end are default (0 / very large), we can either
                                            // hide them or still show them. We'll show them anyway,
                                            // because it's still valid in viva: "no restriction".
                                            sb.append(" [")
                                                    .append(st)
                                                    .append("→")
                                                    .append(en)
                                                    .append("] ");
                                        }
                                    }

                                    ui.appendMessage(sb.toString());
                                }
                            });
                        } else {
                            System.out.println("[GUI AGENT] ERROR: ui is null!");
                        }
                    } catch (Exception ex) {
                        System.err.println("[GUI AGENT] Error parsing optimization-result:");
                        ex.printStackTrace();
                        logToUi("GUI: error parsing optimization-result: " + ex.getMessage());
                    }
                }
                // 3) Route message mirrored
                else if ("route".equals(cid)) {
                    logToUi("GUI: route delivered → " + msg.getSender().getLocalName());
                }
                // 4) Technique change acknowledgement
                else if ("technique-set".equals(cid)) {
                    logToUi("MRA: technique switched to " + msg.getContent());
                }
                else {
                    System.out.println("[GUI AGENT] Unknown conversationId: " + cid);
                }
            }
        });

        System.out.println("[GUI AGENT] Setup complete");
    }

    /* ====================== UI → JADE helpers ====================== */

    /** Called by the UI when the user clicks "Optimize (JADE)". */
    private void requestOptimizeFromMRA(List<Item> items) {
        System.out.println("[GUI AGENT] requestOptimizeFromMRA called with " + items.size() + " items");
        try {
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setConversationId("optimize-request");
            req.addReceiver(new AID("MRA", AID.ISLOCALNAME));
            req.setContent(gson.toJson(items));
            send(req);
            System.out.println("[GUI AGENT] Sent optimize-request to MRA");
            logToUi("GUI→MRA: optimize-request (" + items.size() + " items)");
        } catch (Exception ex) {
            System.err.println("[GUI AGENT] Failed to send optimize-request:");
            ex.printStackTrace();
            logToUi("GUI: failed to send optimize-request: " + ex.getMessage());
        }
    }

    /** Called by the UI when the technique dropdown changes (GREEDY ↔ GA). */
    private void requestTechniqueChange(String technique) {
        System.out.println("[GUI AGENT] requestTechniqueChange called: " + technique);
        try {
            ACLMessage inf = new ACLMessage(ACLMessage.INFORM);
            inf.setConversationId("set-technique");
            inf.addReceiver(new AID("MRA", AID.ISLOCALNAME));
            inf.setContent(technique);
            send(inf);
            System.out.println("[GUI AGENT] Sent set-technique to MRA");
            logToUi("GUI→MRA: set-technique = " + technique);
        } catch (Exception ex) {
            System.err.println("[GUI AGENT] Failed to send set-technique:");
            ex.printStackTrace();
            logToUi("GUI: failed to send set-technique: " + ex.getMessage());
        }
    }

    /* ====================== Small utility ====================== */

    private void logToUi(String line) {
        if (ui != null) {
            SwingUtilities.invokeLater(() -> ui.appendMessage(line));
        } else {
            System.out.println("[GUI AGENT - NO UI] " + line);
        }
    }
}
