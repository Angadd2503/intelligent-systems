package vrp;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class DeliveryAgent extends Agent {
    private double baseSpeedKmh = 35.0;
    private int capacity = 3;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null) {
            if (args.length > 0) {
                try { baseSpeedKmh = Double.parseDouble(args[0].toString()); } catch (Exception ignore) {}
            }
            if (args.length > 1) {
                try { capacity = Integer.parseInt(args[1].toString()); } catch (Exception ignore) {}
            }
        }
        System.out.println("Delivery-agent " + getAID().getName() +
                " up. speed=" + baseSpeedKmh + "km/h, capacity=" + capacity);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        sd.setName("JADE-delivery");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Agent " + getAID().getName() + " successfully registered as a delivery service.");
        } catch (FIPAException fe){
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());

        addBehaviour(new AcceptOrdersServer());
    }

    protected void takeDown() {
        try{
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Delivery-agent " + getAID().getName() + " temrinating.");
    }

    public void updateSettings(final double newSpeedKmh, final int newCapacity) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                baseSpeedKmh = newSpeedKmh;
                capacity = newCapacity;
                System.out.println("Settings updated. speed=" + baseSpeedKmh + "km/h, capacity=" + capacity);
            }
        });
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP received
                double distanceKm = parseDistanceKm(msg.getContent());
                ACLMessage reply = msg.createReply();

                if (capacity <= 0) {
                    // No remaining capacity
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("overloaded");
                } else if (distanceKm <= 0 || baseSpeedKmh <= 0) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("invalid-request");
                } else {
                    // Compute ETA in minutes (distance / speed * 60)
                    double etaMin = (distanceKm / baseSpeedKmh) * 60.0;
                    reply.setPerformative(ACLMessage.PROPOSE);
                    // To mimic BookSeller style, reply with a simple numeric string.
                    // (Alternatively: reply.setContent("eta=" + String.format(Locale.US,"%.2f", etaMin));)
                    reply.setContent(String.format(java.util.Locale.US, "%.2f", etaMin));
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    } // End of OfferRequestsServer

    private class AcceptOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if (capacity > 0) {
                    capacity -= 1;
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("Job accepted from agent " + msg.getSender().getName()
                            + ". Remaining capacity=" + capacity);
                } else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("overloaded");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    } // End of AcceptOrdersServer

    private double parseDistanceKm(String content) {
        if (content == null || content.trim().isEmpty()) return -1.0;
        String s = content.trim();
        try {
            // raw numeric?
            return Double.parseDouble(s);
        } catch (Exception ignore) {
            // key=value?
            String[] parts = s.split("=");
            if (parts.length == 2 && parts[0].trim().equalsIgnoreCase("distance")) {
                try { return Double.parseDouble(parts[1].trim()); } catch (Exception e) { return -1.0; }
            }
            return -1.0;
        }
    }
}