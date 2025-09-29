package vrp;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.LinkedHashMap;
import java.util.Map;

public class ManagerAgent extends Agent {
    private String opt = "GREEDY";
    private final Map<String, VehicleInfo> vehicles = new LinkedHashMap<>();
    static class VehicleInfo { int cap; double dv; VehicleInfo(int c,double d){cap=c;dv=d;} }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String s = (String) args[0];
            for (String kv : s.split(",")) {
                String[] p = kv.split("=");
                if (p.length==2 && p[0].equals("opt")) opt = p[1];
            }
        }
        System.out.println("MRA iniciado con técnica " + opt);

        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = receive();
                if (m == null) { block(); return; }
                if ("capacity".equals(m.getConversationId())) {
                    int cap=10; double dv=300;
                    for (String kv : m.getContent().split(",")) {
                        String[] p = kv.split("=");
                        if (p.length==2) {
                            if (p[0].equals("cap")) cap = Integer.parseInt(p[1]);
                            if (p[0].equals("dv"))  dv  = Double.parseDouble(p[1]);
                        }
                    }
                    vehicles.put(m.getSender().getLocalName(), new VehicleInfo(cap,dv));
                    System.out.println("MRA registró "+m.getSender().getLocalName()+" cap="+cap+" dv="+dv);
                }
            }
        });
    }

    public void sendRoute(String daLocalName, String routeJson) {
        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
        reply.setConversationId("route");
        reply.setContent(routeJson);
        reply.addReceiver(new AID(daLocalName, AID.ISLOCALNAME));
        send(reply);
    }
}

