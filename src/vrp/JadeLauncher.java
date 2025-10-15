package vrp;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class JadeLauncher {

    /**
     * Launch the JADE platform and all agents using the current UI settings.
     */
    public static void launch(MasUI ui) throws Exception {
        // 1) Start JADE platform
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();

        // Show the yellow JADE GUI (Agent Management)
        p.setParameter(Profile.GUI, "true");

        // Let HTTP-MTP choose a free port automatically (avoids "7778 in use")
        p.setParameter("mtp", "jade.mtp.http.MessageTransportProtocol(-port 0)");

        ContainerController main = rt.createMainContainer(p);

        // 2) Read current UI selections
        String opt = ui.getSelectedOpt();        // "GREEDY" or "GA"
        int numDAs = ui.getNumDAs();
        int cap    = ui.getCapacityPerDA();
        double dv  = ui.getMaxDistance();

        System.out.println("MRA started with technique " + opt);
        System.out.println("Starting " + numDAs + " DAs with args: cap=" + cap + ",dv=" + dv);

        // 3) Start Manager (MRA)
        AgentController mra = main.createNewAgent("MRA", "vrp.ManagerAgent",
                new Object[]{"opt=" + opt});
        mra.start();

        // 4) Start Delivery Agents
        for (int i = 1; i <= numDAs; i++) {
            AgentController da = main.createNewAgent(
                    "DA" + i,
                    "vrp.DeliveryAgent",
                    new Object[]{cap, dv}   // DeliveryAgent reads these directly
            );
            da.start();
        }

        // 5) Start GUI Agent (pass Swing UI instance so it can paint and show logs)
        AgentController gui = main.createNewAgent("GUI", "vrp.GuiAgent", new Object[]{ ui });
        gui.start();

        System.out.println("✅ JADE launched with " + numDAs + " DAs using " + opt + ".");
    }

    /**
     * Standalone entry point: opens the Swing UI; you can then click buttons to launch/restart JADE.
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            MasUI ui = new MasUI();
            ui.setVisible(true);
        });
    }
}
