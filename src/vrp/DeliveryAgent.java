package vrp;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Locale;

public class DeliveryAgent extends Agent {
    private double baseSpeedKmh = 35.0;
    private int capacity = 3;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null) {
            if (args.length > 0) {
                try { baseSpeedKmh = Double.parseDouble(args[0].toString()); } catch (Exception ignored) {}
            }
            if (args.length > 1) {
                try { capacity = Integer.parseInt(args[1].toString()); } catch (Exception ignored) {}
            }
        }

        System.out.println("Delivery-agent " + getAID().getName()
                + " up. speed=" + baseSpeedKmh + "km/h, capacity=" + capacity);

        // Register service in DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        sd.setName("JADE-delivery");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Agent " + getAID().getName()
                    + " successfully registered as a delivery service.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());
        addBehaviour(new AcceptOrdersServer());
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
        System.out.println("Delivery-agent " + getAID().getName() + " terminating.");
    }

    public void updateSettings(final double newSpeedKmh, final int newCapacity) {
        addBehaviour(new OneShotBehaviour() {
            @Override public void action() {
                baseSpeedKmh = newSpeedKmh;
                capacity = newCapacity;
                System.out.println("Settings updated. speed=" + baseSpeedKmh
                        + "km/h, capacity=" + capacity);
            }
        });
    }

    /** Accepts either "123.4" or "distance=123.4" */
    private double parseDistanceKm(String content) {
        if (content == null || content.trim().isEmpty()) return -1.0;
        String s = content.trim();
        try {
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            String[] parts = s.split("=");
            if (parts.length == 2 && parts[0].trim().equalsIgnoreCase("distance")) {
                try { return Double.parseDouble(parts[1].trim()); } catch (Exception ignored2) {}
            }
        }
        return -1.0;
    }

    /** Handles CFP and replies with PROPOSE(eta) or REFUSE */
    private class OfferRequestsServer extends CyclicBehaviour {
        @Override public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            double distanceKm = parseDistanceKm(msg.getContent());
            ACLMessage reply = msg.createReply();

            if (capacity <= 0) {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("overloaded");
            } else if (distanceKm > 0.0 && baseSpeedKmh > 0.0) {
                double etaMin = distanceKm / baseSpeedKmh * 60.0;
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.format(Locale.US, "%.2f", etaMin));
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("invalid-request");
            }
            myAgent.send(reply);
        }
    }

    /** Handles ACCEPT_PROPOSAL and replies with AGREE or REFUSE */
    private class AcceptOrdersServer extends CyclicBehaviour {
        @Override public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            ACLMessage reply = msg.createReply();
            if (capacity > 0) {
                capacity--;
                reply.setPerformative(ACLMessage.AGREE);
                System.out.println("Job accepted from agent " + msg.getSender().getName()
                        + ". Remaining capacity=" + capacity);
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("overloaded");
            }
            myAgent.send(reply);
        }
    }
}
