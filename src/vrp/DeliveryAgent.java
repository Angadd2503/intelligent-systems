package vrp;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

// Esta es la versión correcta y compatible
public class DeliveryAgent extends Agent {
    private int capacity;
    private double maxDistance;

    @Override
    protected void setup() {
        // 1. Leer los argumentos que le pasa la UI al crearlo
        Object[] args = getArguments();
        if (args != null && args.length == 2) {
            this.capacity = (int) args[0];
            this.maxDistance = (double) args[1];
        } else {
            // Valores por defecto si no se pasan argumentos
            this.capacity = 10;
            this.maxDistance = 300;
        }

        // 2. Registrarse con el MRA (se ejecuta una sola vez)
        ACLMessage registrationMsg = new ACLMessage(ACLMessage.INFORM);
        registrationMsg.addReceiver(new jade.core.AID("MRA", jade.core.AID.ISLOCALNAME));
        registrationMsg.setConversationId("capacity"); // <-- Usa ConversationId, no Ontología
        registrationMsg.setContent("cap=" + capacity + ",dv=" + maxDistance); // <-- Envía el contenido esperado
        send(registrationMsg);
        System.out.println(getLocalName() + ": Registration message sent to MRA.");

        // 3. Comportamiento para esperar la ruta final
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Escucha por un mensaje con el ConversationId "route"
                MessageTemplate mt = MessageTemplate.MatchConversationId("route");
                ACLMessage msg = myAgent.receive(mt);

                if (msg != null) {
                    System.out.println(getLocalName() + ": My route has arrived! -> " + msg.getContent());
                } else {
                    block();
                }
            }
        });
    }
}

