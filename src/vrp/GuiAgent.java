package vrp;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class GuiAgent extends Agent {
    private MasUI ui;                                // Provided by launcher
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    @Override
    protected void setup() {
        // Try to receive the MasUI instance as the first argument
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MasUI) {
            ui = (MasUI) args[0];

            // Hook UI → JADE actions
            ui.setOnOptimizeToJade(this::requestOptimizeFromMRA);     // send items to MRA
            ui.setOnTechniqueChange(this::requestTechniqueChange);     // runtime GREEDY ↔ GA
        } else {
            System.out.println("[GUI] No MasUI passed to GuiAgent.");
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

                // 1) Plain log lines (sent by ManagerAgent/DeliveryAgents)
                if ("log".equals(cid)) {
                    logToUi(msg.getContent());
                }
                // 2) Optimization result → update routes on the UI
                else if ("optimization-result".equals(cid)) {
                    try {
                        String json = msg.getContent();
                        java.lang.reflect.Type t =
                                new com.google.gson.reflect.TypeToken<Map<String, List<Item>>>() {}.getType();
                        Map<String, List<Item>> routes = gson.fromJson(json, t);
                        String status = msg.getUserDefinedParameter("status");

                        if (ui != null) {
                            SwingUtilities.invokeLater(() -> {
                                ui.updateRoutes(routes, status);
                                logToUi("GUI: optimization-result → " + status);
                            });
                        }
                    } catch (Exception ex) {
                        logToUi("GUI: error parsing optimization-result: " + ex.getMessage());
                    }
                }
                // 3) Route message mirrored (useful if you want to see per-DA payloads)
                else if ("route".equals(cid)) {
                    logToUi("GUI: route delivered → " + msg.getContent());
                }
                // 4) Technique change acknowledgement (optional)
                else if ("technique-set".equals(cid)) {
                    logToUi("MRA: technique switched to " + msg.getContent());
                }
            }
        });
    }

    /* ====================== UI → JADE helpers ====================== */

    /** Called by the UI when the user clicks "Optimize (JADE)". */
    private void requestOptimizeFromMRA(List<Item> items) {
        try {
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setConversationId("optimize-request");
            req.addReceiver(new AID("MRA", AID.ISLOCALNAME));
            req.setContent(gson.toJson(items));
            send(req);
            logToUi("GUI→MRA: optimize-request (" + items.size() + " items)");
        } catch (Exception ex) {
            logToUi("GUI: failed to send optimize-request: " + ex.getMessage());
        }
    }

    /** Called by the UI when the technique dropdown changes (GREEDY ↔ GA). */
    private void requestTechniqueChange(String technique) {
        try {
            ACLMessage inf = new ACLMessage(ACLMessage.INFORM);
            inf.setConversationId("set-technique");
            inf.addReceiver(new AID("MRA", AID.ISLOCALNAME));
            inf.setContent(technique);
            send(inf);
            logToUi("GUI→MRA: set-technique = " + technique);
        } catch (Exception ex) {
            logToUi("GUI: failed to send set-technique: " + ex.getMessage());
        }
    }

    /* ====================== Small utility ====================== */

    private void logToUi(String line) {
        if (ui != null) {
            SwingUtilities.invokeLater(() -> ui.appendMessage(line));
        } else {
            System.out.println("[GUI] " + line);
        }
    }

}
