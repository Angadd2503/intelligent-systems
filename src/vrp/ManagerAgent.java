// src/vrp/ManagerAgent.java
package vrp;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.List;

/** Negotiates with delivery agents: CFP -> collect PROPOSE -> ACCEPT_PROPOSAL -> wait AGREE */
public class ManagerAgent extends Agent {
    private List<Item> deliveryItems;
    private AID[] deliveryAgents;

    @Override
    protected void setup() {
        System.out.println("Manager-agent " + getAID().getName() + " is ready.");

        try {
            // Prefer classpath resource; falls back to file path
            deliveryItems = ItemsParser.load("data/Items.txt");
            System.out.println("Manager loaded " + deliveryItems.size() + " items for delivery.");
        } catch (IOException e) {
            System.err.println("Manager failed to load items: " + e.getMessage());
            doDelete();
            return;
        }

        // ---- DF polling: wait up to ~5 seconds until delivery agents register
        deliveryAgents = waitForDeliveryAgents(5000, 500);
        if (deliveryAgents == null || deliveryAgents.length == 0) {
            System.out.println("CRITICAL ERROR: No delivery agents found in DF. Manager terminating.");
            doDelete();
            return;
        }
        System.out.println("Found " + deliveryAgents.length + " delivery agents. Starting requests.");

        addBehaviour(new RequestPerformer());
    }

    private AID[] waitForDeliveryAgents(long timeoutMs, long pollMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        sd.setName("JADE-delivery");
        template.addServices(sd);

        while (System.currentTimeMillis() < end) {
            try {
                DFAgentDescription[] result = DFService.search(this, template);
                if (result != null && result.length > 0) {
                    AID[] agents = new AID[result.length];
                    for (int i = 0; i < result.length; i++) agents[i] = result[i].getName();
                    return agents;
                }
            } catch (FIPAException ignored) {}
            try { Thread.sleep(pollMs); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    private class RequestPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        private Item currentItem;
        private double bestProposal = Double.MAX_VALUE;
        private AID bestProposer;

        @Override
        public void action() {
            switch (step) {
                case 0: // send CFP for next item
                    if (deliveryItems.isEmpty()) {
                        System.out.println("All items have been assigned. Manager terminating.");
                        myAgent.doDelete();
                        step = 4;
                        return;
                    }
                    currentItem = deliveryItems.remove(0);
                    System.out.println("-------------------------------------------");
                    System.out.println("Manager requesting delivery for item: " + currentItem.getId());

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID agent : deliveryAgents) cfp.addReceiver(agent);

                    // Distance from depot(0,0) to item (x,y)
                    double distance = Math.hypot(currentItem.getX(), currentItem.getY());
                    cfp.setContent(String.valueOf(distance));
                    cfp.setConversationId("vrp-delivery");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("vrp-delivery"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                    );
                    step = 1;
                    repliesCnt = 0;
                    bestProposal = Double.MAX_VALUE;
                    bestProposer = null;
                    break;

                case 1: // collect proposals
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply == null) { block(); return; }

                    repliesCnt++;
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        try {
                            double eta = Double.parseDouble(reply.getContent());
                            if (eta < bestProposal) {
                                bestProposal = eta;
                                bestProposer = reply.getSender();
                            }
                        } catch (NumberFormatException ignored) {}
                    } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                        // ignored, just counting
                    }

                    if (repliesCnt >= deliveryAgents.length) step = 2;
                    break;

                case 2: // send order to best proposer or retry
                    if (bestProposer != null) {
                        System.out.println("Best proposal is " + String.format("%.2f", bestProposal)
                                + " mins from " + bestProposer.getName());
                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(bestProposer);
                        order.setContent(String.valueOf(currentItem.getId()));
                        order.setConversationId("vrp-delivery");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        myAgent.send(order);
                        mt = MessageTemplate.and(
                                MessageTemplate.MatchConversationId("vrp-delivery"),
                                MessageTemplate.MatchInReplyTo(order.getReplyWith())
                        );
                        step = 3;
                    } else {
                        System.out.println("No proposals received for item " + currentItem.getId() + ". Retrying later.");
                        deliveryItems.add(currentItem);
                        step = 0;
                    }
                    break;

                case 3: // wait for AGREE
                    ACLMessage conf = myAgent.receive(mt);
                    if (conf == null) { block(); return; }

                    if (conf.getPerformative() == ACLMessage.AGREE) {
                        System.out.println("SUCCESS: Item " + currentItem.getId()
                                + " assigned to " + conf.getSender().getName());
                    } else {
                        System.out.println("FAILURE: Agent " + conf.getSender().getName()
                                + " failed to take the job. Re-queueing item " + currentItem.getId());
                        deliveryItems.add(currentItem);
                    }
                    step = 0; // next item
                    break;
            }
        }

        @Override public boolean done() { return step == 4; }
    }
}
