package vrp;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class DeliveryAgent extends Agent {
    private int cap = 10;
    private double dv = 300;

    @Override
    protected void setup() {
        // args estilo "cap=10,dv=300"
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String s = (String) args[0];
            for (String kv : s.split(",")) {
                String[] p = kv.split("=");
                if (p.length == 2) {
                    if (p[0].equals("cap")) cap = Integer.parseInt(p[1]);
                    if (p[0].equals("dv"))  dv  = Double.parseDouble(p[1]);
                }
            }
        }
        // Notifica al MRA sus capacidades
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setConversationId("capacity");
        msg.setContent("cap="+cap+",dv="+dv);
        msg.addReceiver(new AID("MRA", AID.ISLOCALNAME));
        send(msg);

        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = receive();
                if (m == null) { block(); return; }
                if ("route".equals(m.getConversationId())) {
                    System.out.println(getLocalName()+" recibió ruta: "+m.getContent());
                    // aquí podrías confirmar o "simular ejecución"
                }
            }
        });
    }
}
