package vrp;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class JadeLauncher {

    /**
     * Launch the JADE platform and all agents using the current UI settings.
     * (You actually don't need to call this if you're using MasUI + JadePlatformManager,
     * but if you DO call it, it must behave the same way.)
     */
    public static void launch(MasUI ui) throws Exception {
        // 1) Start JADE runtime
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();

        // Show JADE management GUI and auto-select MTP port
        p.setParameter(Profile.GUI, "true");
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
        AgentController mra = main.createNewAgent(
                "MRA",
                "vrp.ManagerAgent",
                new Object[]{"opt=" + opt}
        );
        mra.start();

        // 4) Start each Delivery Agent with numeric args {cap, dv}
        for (int i = 1; i <= numDAs; i++) {
            AgentController da = main.createNewAgent(
                    "DA" + i,
                    "vrp.DeliveryAgent",
                    new Object[]{cap, dv}   // <-- FIXED here too
            );
            da.start();
        }

        // 5) Start GUI agent with SAME MasUI instance
        AgentController gui = main.createNewAgent(
                "GUI",
                "vrp.GuiAgent",
                new Object[]{ ui }
        );
        gui.start();

        System.out.println("✅ JADE launched with " + numDAs + " DAs using " + opt + ".");
    }

    /**
     * Standalone entry point: opens the Swing UI.
     * IMPORTANT:
     * After this shows the UI, you should click "Launch MAS (JADE)" from that window.
     * Avoid ALSO calling JadeLauncher.launch(ui) manually at the same time,
     * or you'll create a second platform and a second MasUI and things won't update.
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            MasUI ui = new MasUI();
            ui.setVisible(true);
            // We do NOT auto-launch JADE here.
            // You launch JADE by pressing the button in the UI.
        });
    }
}
