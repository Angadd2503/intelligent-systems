// src/vrp/ManagerAgent.java
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
import java.io.IOException;
import java.util.List;

public class ManagerAgent extends Agent {
    private List<Item> deliveryItems;
    private AID[] deliveryAgents;

    @Override
    protected void setup() {
        System.out.println("Manager-agent " + getAID().getName() + " is ready.");


        try {
            deliveryItems = ItemsParser.load("data/Items.txt");
            System.out.println("Manager loaded " + deliveryItems.size() + " items for delivery.");
        } catch (IOException e) {
            System.err.println("Manager failed to load items: " + e.getMessage());
            doDelete();
            return;
        }

        System.out.println("Manager is searching for delivery services IMMEDIATELY.");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        sd.setName("JADE-delivery");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println("Found " + result.length + " delivery agents in setup().");

            if (result.length > 0) {
                deliveryAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    deliveryAgents[i] = result[i].getName();
                }
                addBehaviour(new RequestPerformer());
            } else {
                System.out.println("CRITICAL ERROR: No delivery agents found in DF. Manager terminating.");
                doDelete();
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    private class RequestPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        private Item currentItem;
        private double bestProposal = Double.MAX_VALUE;
        private AID bestProposer;

        public void action() {
            switch (step) {
                case 0:
                    if (deliveryItems.isEmpty()) {
                        System.out.println("All items have been assigned. Manager terminating.");
                        myAgent.doDelete();
                        step = 4;
                        break;
                    }
                    currentItem = deliveryItems.remove(0);
                    System.out.println("-------------------------------------------");
                    System.out.println("Manager requesting delivery for item: " + currentItem.getId());
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID agent : deliveryAgents) {
                        cfp.addReceiver(agent);
                    }
                    double distance = Math.sqrt(currentItem.getX() * currentItem.getX() + currentItem.getY() * currentItem.getY());
                    cfp.setContent(String.valueOf(distance));
                    cfp.setConversationId("vrp-delivery");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("vrp-delivery"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    repliesCnt = 0;
                    bestProposal = Double.MAX_VALUE;
                    bestProposer = null;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            double eta = Double.parseDouble(reply.getContent());
                            if (eta < bestProposal) {
                                bestProposal = eta;
                                bestProposer = reply.getSender();
                            }
                        }
                        if (repliesCnt >= deliveryAgents.length) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    if (bestProposer != null) {
                        System.out.println("Best proposal is " + String.format("%.2f", bestProposal) + " mins from " + bestProposer.getName());
                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(bestProposer);
                        order.setContent(String.valueOf(currentItem.getId()));
                        order.setConversationId("vrp-delivery");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("vrp-delivery"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        step = 3;
                    } else {
                        System.out.println("No proposals received for item " + currentItem.getId() + ". Retrying later.");
                        deliveryItems.add(currentItem);
                        step = 0;
                    }
                    break;
                case 3:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("SUCCESS: Item " + currentItem.getId() + " assigned to " + reply.getSender().getName());
                        } else {
                            System.out.println("FAILURE: Agent " + reply.getSender().getName() + " failed to take the job.");
                            deliveryItems.add(currentItem);
                        }
                        step = 0;
                    } else {
                        block();
                    }
                    break;
            }
        }
        public boolean done() {
            return step == 4;
        }
    }
}