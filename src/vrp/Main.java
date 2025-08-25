package vrp;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Main {

    public void startJade(int numDeliveryAgents, double speed, int capacity) {
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.PLATFORM_ID, "VRP-Platform");

        ContainerController mainContainer = rt.createMainContainer(profile);

        try {
            System.out.println("Starting Delivery Agents...");
            for (int i = 1; i <= numDeliveryAgents; i++) {
                Object[] args = new Object[]{speed, capacity};
                AgentController deliveryAgent = mainContainer.createNewAgent(
                        "delivery-agent-" + i,
                        "vrp.DeliveryAgent",
                        args
                );
                deliveryAgent.start();
            }

            try {
                Thread.sleep(2000); // Pausa de 2 segundos
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Starting Manager Agent...");
            AgentController managerAgent = mainContainer.createNewAgent(
                    "manager",
                    "vrp.ManagerAgent",
                    null
            );
            managerAgent.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MasUI.main(args);
    }
}