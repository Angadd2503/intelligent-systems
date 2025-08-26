package vrp;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.*;

public class MRA extends Agent {
    private HashMap<AID, int> deliveryAgentsMap = new HashMap<>();

    protected void setup() {
        System.out.println("MRA " + getAID().getName() + " is ready.");

        // Comportement qui démarre immédiatement
        addBehaviour(new OneShotBehaviour(this) {
            public void action() {
                System.out.println("\n--- MRA: Starting communication test ---");
                myAgent.addBehaviour(new CapacityCollectorBehaviour());
            }
        });
    }

    private class CapacityCollectorBehaviour extends Behaviour {
        private AID[] deliveryAgentsAIDs;
        private int repliesCnt = 0; // The counter of replies
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Search Delivery Agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("delivery");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found " + result.length + " delivery agent(s)");
                        if (result.length == 0) {
                            System.out.println("No agents found.");
                            return;
                        }
                        deliveryAgentsAIDs = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            deliveryAgentsAIDs[i] = result[i].getName();
                            System.out.println(" - " + deliveryAgentsAIDs[i].getName());
                        }
                        step = 1;
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                        step = 4;
                    }
                    break;

                case 1:
                    //Requests
                    ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
                    for (AID agent : deliveryAgentsAIDs) {
                        requestMsg.addReceiver(agent);
                    }
                    requestMsg.setContent("What is your capacity?");
                    requestMsg.setConversationId("capacity-query");
                    requestMsg.setReplyWith("req-" + System.currentTimeMillis());
                    myAgent.send(requestMsg);
                    System.out.println("Capacity request sent to all agents");
                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("capacity-query"),
                        MessageTemplate.MatchInReplyTo(requestMsg.getReplyWith())
                    );
                    step = 2;
                    break;

                case 2:
                    //Responses
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            try {
                                int capacity = Integer.parseInt(reply.getContent());
                                deliveryAgentsMap.put(reply.getSender(), capacity);
                                System.out.println("MRA: " + reply.getSender().getLocalName() + " -> capacity: " + capacity);
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid response from " + reply.getSender().getLocalName());
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= deliveryAgentsAIDs.length) {
                            step = 3;
                        }
                    } else {
                        block();
                    }
                    break;

                case 3:
                    // Show result
                    System.out.println("All capacities received:");
                    for (Map.Entry<AID, Integer> entry : deliveryAgentsMap.entrySet()) {
                        System.out.println(" - " + entry.getKey().getLocalName() + " : " + entry.getValue() + " packages");
                    }
                    step = 4;
                    break;
            }
        }

        public boolean done() {
            return (step == 4);
        }
    }

    protected void takeDown() {
        System.out.println("MRA-agent " + getAID().getName() + " terminating.");
    }
}
