package vrp;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class MasUI extends JFrame {

    // ---- Controls -----------------------------------------------------------
    private final JTextField tfNumItems = new JTextField("45", 6);
    private final JTextField tfNumDAs   = new JTextField("3", 6);
    private final JTextField tfCapacity = new JTextField("7", 6);
    private final JTextField tfMaxDist  = new JTextField("250", 6);
    private final JTextField tfSeed     = new JTextField("42", 6);

    private final JComboBox<String> cbOpt = new JComboBox<>(new String[]{"GREEDY", "GA"});

    private final JRadioButton rbAuto = new JRadioButton("Generate automatically", true);
    private final JRadioButton rbFile = new JRadioButton("Load from file");
    private final JTextField tfFile   = new JTextField("", 18);
    private final JButton btnBrowse   = new JButton("Browse…");

    private final JButton btnLoadGen      = new JButton("Load / Generate");
    private final JButton btnOptimize     = new JButton("Optimize (Local)");
    private final JButton btnOptimizeJade = new JButton("Optimize (JADE)");

    private final JButton btnLaunchJade   = new JButton("Launch MAS (JADE)");
    private final JButton btnRestartJade  = new JButton("Restart JADE");
    private final JButton btnShutdownJade = new JButton("Shutdown JADE");

    private final DefaultListModel<String> messages = new DefaultListModel<>();
    private final JList<String> messagesList = new JList<>(messages);

    private final JLabel statusBar = new JLabel("Ready");

    // ---- Data ---------------------------------------------------------------
    private List<Item> items = new ArrayList<>();
    private final Map<String, List<Item>> routes = new LinkedHashMap<>();

    // Callbacks that the GuiAgent can set
    private Consumer<List<Item>> onOptimizeToJade;
    private Consumer<String> onTechniqueChange;

    // ---- Canvas -------------------------------------------------------------
    private final JPanel canvas = new JPanel() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            final int MARGIN = 40;
            final int DOT_R = 3;

            // background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, W, H);

            // determine scaling to fill canvas
            double maxX = items.stream().mapToDouble(Item::getX).max().orElse(1);
            double maxY = items.stream().mapToDouble(Item::getY).max().orElse(1);
            double scaleX = (W - 2.0 * MARGIN) / Math.max(1, maxX);
            double scaleY = (H - 2.0 * MARGIN) / Math.max(1, maxY);

            int depotX = MARGIN, depotY = H - MARGIN;

            // depot
            g2.setColor(Color.GRAY);
            g2.fillOval(depotX - 4, depotY - 4, 8, 8);
            g2.drawString("DEPOT(0,0)", depotX + 10, depotY - 6);

            // pixel positions for items (with Y inverted so “up” is up)
            Map<Item, Point> pix = new LinkedHashMap<>();
            for (Item it : items) {
                int px = (int) (MARGIN + it.getX() * scaleX);
                int py = (int) (H - (MARGIN + it.getY() * scaleY));
                pix.put(it, new Point(px, py));
            }

            // draw routes first (behind labels)
            Random rnd = new Random(123);
            for (Map.Entry<String, List<Item>> e : routes.entrySet()) {
                g2.setColor(new Color(60 + rnd.nextInt(180), 60 + rnd.nextInt(180), 60 + rnd.nextInt(180)));
                List<Item> seq = e.getValue();
                int lastX = depotX, lastY = depotY;
                for (Item it : seq) {
                    Point p = pix.get(it);
                    if (p == null) continue;
                    g2.drawLine(lastX, lastY, p.x, p.y);
                    lastX = p.x; lastY = p.y;
                }
                g2.drawLine(lastX, lastY, depotX, depotY);
                g2.drawString(e.getKey(), lastX + 6, lastY - 6);
            }

            // draw points + smart labels (spiral placement to avoid overlaps)
            g2.setColor(Color.BLACK);
            List<Rectangle> occupied = new ArrayList<>();
            FontMetrics fm = g2.getFontMetrics();

            for (Item it : items) {
                Point p = pix.get(it);
                g2.fillOval(p.x - DOT_R, p.y - DOT_R, DOT_R * 2, DOT_R * 2);

                String text = it.getId();
                Rectangle chosen = null;

                for (int radius = 8; radius <= 32 && chosen == null; radius += 8) {
                    for (int angle = 0; angle < 360; angle += 45) {
                        int dx = (int) (radius * Math.cos(Math.toRadians(angle)));
                        int dy = (int) (radius * Math.sin(Math.toRadians(angle)));
                        int tx = p.x + dx;
                        int ty = p.y + dy;
                        Rectangle r = new Rectangle(tx, ty - fm.getAscent(),
                                fm.stringWidth(text), fm.getHeight());
                        boolean overlaps = false;
                        for (Rectangle o : occupied) {
                            if (o.intersects(r)) { overlaps = true; break; }
                        }
                        if (!overlaps) { chosen = r; break; }
                    }
                }
                if (chosen == null) {
                    chosen = new Rectangle(p.x + 5, p.y - fm.getAscent(),
                            fm.stringWidth(text), fm.getHeight());
                }
                g2.drawString(text, chosen.x, chosen.y + fm.getAscent() - 2);
                occupied.add(chosen);
            }
        }
    };

    // ---- UI ----------------------------------------------------------------
    public MasUI() {
        super("VRP MAS Config UI (Swing)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // left column
        JPanel params = new JPanel(new GridLayout(0, 2, 6, 6));
        params.setBorder(new TitledBorder("Parameters"));
        params.add(new JLabel("# Items:"));          params.add(tfNumItems);
        params.add(new JLabel("# DAs:"));            params.add(tfNumDAs);
        params.add(new JLabel("Capacity per DA:"));  params.add(tfCapacity);
        params.add(new JLabel("Max distance dv:"));  params.add(tfMaxDist);
        params.add(new JLabel("Seed:"));             params.add(tfSeed);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbAuto); bg.add(rbFile);

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fileRow.add(tfFile); fileRow.add(btnBrowse);

        JPanel inputBox = new JPanel(new BorderLayout(6, 6));
        inputBox.setBorder(new TitledBorder("Items input"));
        inputBox.add(rbAuto, BorderLayout.NORTH);
        inputBox.add(rbFile, BorderLayout.CENTER);
        inputBox.add(fileRow, BorderLayout.SOUTH);

        JPanel optBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        optBox.setBorder(new TitledBorder("Optimization"));
        optBox.add(new JLabel("Technique:"));
        optBox.add(cbOpt);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        actions.add(btnLoadGen);
        actions.add(btnOptimize);
        actions.add(btnOptimizeJade);

        JPanel jade = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        jade.setBorder(new TitledBorder("JADE Control"));
        jade.add(btnLaunchJade);
        jade.add(btnRestartJade);
        jade.add(btnShutdownJade);

        JPanel msgPanel = new JPanel(new BorderLayout());
        msgPanel.setBorder(new TitledBorder("Messages"));
        messagesList.setVisibleRowCount(10);
        msgPanel.add(new JScrollPane(messagesList), BorderLayout.CENTER);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        left.add(params);
        left.add(Box.createVerticalStrut(6));
        left.add(inputBox);
        left.add(Box.createVerticalStrut(6));
        left.add(optBox);
        left.add(Box.createVerticalStrut(6));
        left.add(actions);
        left.add(Box.createVerticalStrut(6));
        left.add(jade);
        left.add(Box.createVerticalStrut(6));
        left.add(msgPanel);

        // right
        canvas.setPreferredSize(new Dimension(820, 560));
        canvas.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, new JScrollPane(canvas));
        split.setDividerLocation(380);

        JPanel root = new JPanel(new BorderLayout());
        root.add(split, BorderLayout.CENTER);

        JPanel sb = new JPanel(new BorderLayout());
        sb.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        sb.add(statusBar, BorderLayout.WEST);
        root.add(sb, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(null);

        // events
        btnBrowse.addActionListener(e -> onBrowse());
        btnLoadGen.addActionListener(e -> onLoadOrGenerate());
        btnOptimize.addActionListener(e -> onOptimizeLocal());
        btnOptimizeJade.addActionListener(e -> sendToJade());

        btnLaunchJade.addActionListener(e -> onLaunchJade());
        btnRestartJade.addActionListener(e -> onRestartJade());
        btnShutdownJade.addActionListener(e -> onShutdownJade());

        refreshJadeButtons();
    }

    // ---- Getters used elsewhere --------------------------------------------
    public String getSelectedOpt() { return String.valueOf(cbOpt.getSelectedItem()); }
    public int getNumDAs()         { return Integer.parseInt(tfNumDAs.getText().trim()); }
    public int getCapacityPerDA()  { return Integer.parseInt(tfCapacity.getText().trim()); }
    public double getMaxDistance() { return Double.parseDouble(tfMaxDist.getText().trim()); }
    public long getSeed()          { return Long.parseLong(tfSeed.getText().trim()); }

    // Hooks for GuiAgent
    public void setOnOptimizeToJade(Consumer<List<Item>> handler) { this.onOptimizeToJade = handler; }
    public void setOnTechniqueChange(Consumer<String> handler)    { this.onTechniqueChange = handler; }

    // ---- Message area helpers ----------------------------------------------
    public void appendMessage(String line) {
        SwingUtilities.invokeLater(() -> {
            messages.addElement(line);
            int last = messages.size() - 1;
            if (last >= 0) messagesList.ensureIndexIsVisible(last);
        });
    }
    // alias used by some code
    public void uiLog(String line) { appendMessage(line); }

    // ---- Events -------------------------------------------------------------
    private void onBrowse() {
        JFileChooser fc = new JFileChooser(new File("data"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            tfFile.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void onLoadOrGenerate() {
        try {
            if (rbAuto.isSelected()) {
                items = randomItems(Integer.parseInt(tfNumItems.getText().trim()), getSeed());
            } else {
                String path = tfFile.getText().trim();
                if (path.isEmpty()) throw new IllegalArgumentException("Select an items file.");
                items = ItemsParser.parseItems(path);
            }
            routes.clear();
            canvas.repaint();
            status("GUI: items loaded (" + items.size() + ")");
            appendMessage("GUI: items loaded (" + items.size() + ")");
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void onOptimizeLocal() {
        try {
            if (items.isEmpty()) onLoadOrGenerate();
            int numDAs = getNumDAs();
            int cap    = getCapacityPerDA();
            double dv  = getMaxDistance();
            String opt = getSelectedOpt();

            long t0 = System.currentTimeMillis();

            if ("GA".equalsIgnoreCase(opt)) {
                SimpleGAOptimizer ga = new SimpleGAOptimizer();
                SimpleGAOptimizer.Result r = ga.solve(items, numDAs, cap, dv, 200, 40);
                routes.clear();
                for (int i = 0; i < r.routes.size(); i++) routes.put("DA" + (i + 1), r.routes.get(i));
                long ms = System.currentTimeMillis() - t0;
                status(String.format("Optimized with GA • Delivered: %d • Total distance: %.1f • Time: %dms",
                        r.itemsDelivered, r.totalDistance, ms));
                appendMessage(String.format("GUI(Local): Optimized with GA • Delivered: %d • Total distance: %.1f • Time: %dms",
                        r.itemsDelivered, r.totalDistance, ms));
            } else {
                GreedyOptimizer.Result r = GreedyOptimizer.solve(items, numDAs, cap, dv);
                routes.clear();
                routes.putAll(r.routes);
                long ms = System.currentTimeMillis() - t0;
                status(String.format("Optimized with Greedy • Delivered: %d • Total distance: %.1f • Time: %dms",
                        r.delivered, r.totalDistance, ms));
                appendMessage(String.format("GUI(Local): Optimized with Greedy • Delivered: %d • Total distance: %.1f • Time: %dms",
                        r.delivered, r.totalDistance, ms));
            }
            canvas.repaint();
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void sendToJade() {
        try {
            if (items.isEmpty()) onLoadOrGenerate();
            if (!JadePlatformManager.isRunning())
                throw new IllegalStateException("Launch JADE first (GUI agent not connected).");
            if (onOptimizeToJade == null)
                throw new IllegalStateException("GUI agent wiring not ready.");
            onOptimizeToJade.accept(new ArrayList<>(items));
            appendMessage("GUI → MRA: optimize-request (items=" + items.size() + ")");
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void onLaunchJade() {
        try {
            JadePlatformManager.start(this, getSelectedOpt(), getNumDAs(), getCapacityPerDA(), getMaxDistance());
            appendMessage("GUI: JADE launched");
            status("JADE launched");
        } catch (Exception ex) {
            showErr(ex);
        }
        refreshJadeButtons();
    }

    private void onRestartJade() {
        try {
            JadePlatformManager.restart(this);
            appendMessage("GUI: JADE restarted");
            status("JADE restarted");
        } catch (Exception ex) {
            showErr(ex);
        }
        refreshJadeButtons();
    }

    private void onShutdownJade() {
        JadePlatformManager.stop();
        appendMessage("GUI: JADE stopped");
        status("JADE stopped");
        refreshJadeButtons();
    }

    private void refreshJadeButtons() {
        boolean running = JadePlatformManager.isRunning();
        btnLaunchJade.setEnabled(!running);
        btnRestartJade.setEnabled(running);
        btnShutdownJade.setEnabled(running);
        btnOptimizeJade.setEnabled(running);
    }

    // ---- Called by GuiAgent to paint routes + set status --------------------
    public void updateRoutes(Map<String, List<Item>> newRoutes, String statusText) {
        routes.clear();
        if (newRoutes != null) routes.putAll(newRoutes);
        if (statusText != null && !statusText.isEmpty()) status(statusText);
        canvas.repaint();
    }

    // ---- Small utilities ----------------------------------------------------
    private void status(String s) { statusBar.setText(s); }

    private static List<Item> randomItems(int n, long seed) {
        Random rnd = new Random(seed);
        List<Item> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            double x = rnd.nextDouble() * 300.0;
            double y = rnd.nextDouble() * 200.0;
            list.add(new Item("i" + i, x, y));
        }
        return list;
    }

    private static void showErr(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Optional: keep old API name some code may call
    public void setJadeLaunchEnabled(boolean enabled) {
        btnLaunchJade.setEnabled(enabled);
    }

    // Standalone UI launcher (without JADE)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MasUI ui = new MasUI();
            ui.setVisible(true);
            ui.appendMessage("GUI: items loaded (" + ui.items.size() + ")");
        });
    }
}



