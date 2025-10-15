package vrp;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.List;

public final class JadePlatformManager {

    private static Runtime runtime;
    private static ContainerController main;
    private static AgentController mra;
    private static AgentController gui;
    private static final List<AgentController> das = new ArrayList<>();
    private static boolean running = false;

    private JadePlatformManager() {}

    public static synchronized boolean isRunning() { return running; }

    public static synchronized void start(MasUI ui, String opt, int numDAs, int cap, double dv) throws Exception {
        if (running) stop();

        runtime = Runtime.instance();
        Profile p = new ProfileImpl();

        p.setParameter(Profile.GUI, "true");
        // IMPORTANT: auto-port MTP avoids 7778 reuse & deadMTP errors on quick restarts
        p.setParameter("mtp", "jade.mtp.http.MessageTransportProtocol(-port 0)");

        main = runtime.createMainContainer(p);

        mra = main.createNewAgent("MRA", "vrp.ManagerAgent", new Object[]{"opt=" + opt});
        mra.start();

        System.out.println("Starting " + numDAs + " DAs with args: cap=" + cap + ",dv=" + dv);
        for (int i = 1; i <= numDAs; i++) {
            AgentController da = main.createNewAgent("DA" + i, "vrp.DeliveryAgent",
                    new Object[]{"cap=" + cap, "dv=" + dv});
            da.start();
            das.add(da);
        }

        gui = main.createNewAgent("GUI", "vrp.GuiAgent", new Object[]{ ui });
        gui.start();

        running = true;
        System.out.println("✅ JADE launched with " + numDAs + " DAs using " + opt + ".");
    }

    public static synchronized void stop() {
        // Kill agents first; suppress any errors so shutdown proceeds
        try { if (gui != null) gui.kill(); } catch (Exception ignored) {}
        try { if (mra != null) mra.kill(); } catch (Exception ignored) {}
        for (AgentController da : das) {
            try { if (da != null) da.kill(); } catch (Exception ignored) {}
        }
        das.clear();

        // Kill container then shutdown runtime
        try { if (main != null) main.kill(); } catch (Exception ignored) {}
        try {
            if (runtime != null) {
                runtime.shutDown();
                // Give the MTP a breath to release sockets, avoids 1099/7778 races
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            }
        } catch (Exception ignored) {}

        gui = null;
        mra = null;
        main = null;
        runtime = null;
        running = false;
        System.out.println("🛑 JADE platform stopped.");
    }

    public static synchronized void restart(MasUI ui) throws Exception {
        start(ui, ui.getSelectedOpt(), ui.getNumDAs(), ui.getCapacityPerDA(), ui.getMaxDistance());
    }
}



