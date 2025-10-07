package vrp;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javax.swing.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class GuiAgent extends jade.core.Agent {
    private transient MasUI ui;

    public void setUi(MasUI uiInstance) {
        this.ui = uiInstance;
    }

    protected void setup() {
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchConversationId("optimization-result")
                );
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String jsonContent = msg.getContent();
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, List<Item>>>(){}.getType();
                    Map<String, List<Item>> routes = gson.fromJson(jsonContent, type);
                    String status = msg.getUserDefinedParameter("status");

                    SwingUtilities.invokeLater(() -> ui.updateRoutes(routes, status));
                } else {
                    block();
                }
            }
        });
    }

    public void requestOptimization(List<Item> itemsToOptimize) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID("MRA", AID.ISLOCALNAME));
        request.setConversationId("optimize-request");
        request.setContent(new Gson().toJson(itemsToOptimize));
        send(request);
    }
}
