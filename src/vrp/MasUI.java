package vrp;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MasUI extends JFrame {
    // --- Referencia al Agente ---
    private GuiAgent guiAgent;

    // --- Controles de la UI ---
    private final JTextField tfNumItems = new JTextField("30", 5);
    private final JTextField tfNumDAs   = new JTextField("3", 5);
    private final JTextField tfCapacity = new JTextField("10", 5);
    private final JTextField tfMaxDist  = new JTextField("300", 5);
    private final JTextField tfSeed     = new JTextField("42", 5);
    private final JComboBox<String> cbOpt = new JComboBox<>(new String[]{"GREEDY"});
    private final JRadioButton rbAuto = new JRadioButton("Generate automatically", true);
    private final JRadioButton rbFile = new JRadioButton("Load from file");
    private final JTextField tfFile   = new JTextField("data/items.txt", 18);
    private final JButton btnBrowse   = new JButton("Browse…");
    private final JButton btnLoadGen  = new JButton("Load / Gen");
    private final JButton btnOptimize = new JButton("Optimize");
    private final JButton btnLaunch   = new JButton("Launch MAS (JADE)");
    private final JLabel status = new JLabel("Ready. Launch the JADE platform to begin.");
    private final JTextArea logArea = new JTextArea(10, 30);

    // --- Datos ---
    private java.util.List<Item> items = new ArrayList<>();
    private final Map<String, java.util.List<Item>> routes = new LinkedHashMap<>();

    // --- Canvas para Visualización ---
    private final JPanel canvas = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.GRAY);
            g2.drawOval(38, 38, 10, 10);
            g2.drawString("DEPOT", 55, 35);
            g2.setColor(Color.BLACK);
            for (Item it : items) {
                int x = 40 + (int) Math.round(it.getX());
                int y = 40 + (int) Math.round(it.getY());
                g2.fillOval(x - 3, y - 3, 6, 6);
            }
            Random r = new Random(123);
            for (Map.Entry<String, java.util.List<Item>> e : routes.entrySet()) {
                g2.setColor(new Color(80 + r.nextInt(150), 80 + r.nextInt(150), 80 + r.nextInt(150)));
                java.util.List<Item> seq = e.getValue();
                int lastX = 40, lastY = 40;
                for (Item it : seq) {
                    int x = 40 + (int) Math.round(it.getX());
                    int y = 40 + (int) Math.round(it.getY());
                    g2.drawLine(lastX, lastY, x, y);
                    lastX = x;
                    lastY = y;
                }
                g2.drawLine(lastX, lastY, 40, 40);
                g2.drawString(e.getKey(), Math.max(45, lastX), Math.max(45, lastY));
            }
        }
    };

    public MasUI() {
        super("VRP MAS Config UI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setupUIComponents(); // Método auxiliar para mantener el constructor limpio
        setupActionListeners();
    }

    // En MasUI.java
    private void onLaunchJade() {
        System.out.println("Attempting to launch JADE platform...");
        status.setText("Launching JADE platform, please wait...");

        try {
            // --- INICIO DE JADE ---
            jade.core.Runtime rt = jade.core.Runtime.instance();
            rt.setCloseVM(true);
            jade.core.Profile profile = new jade.core.ProfileImpl();
            profile.setParameter(jade.core.Profile.GUI, "true");
            profile.setParameter(jade.core.Profile.CONTAINER_NAME, "Main-Container");
            profile.setParameter(jade.core.Profile.MAIN_HOST, "localhost");
            jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(profile);
            System.out.println("JADE Main Container created.");

            // --- LECTURA DE PARÁMETROS DE LA UI ---
            int numDAs = Integer.parseInt(tfNumDAs.getText().trim());
            int cap = Integer.parseInt(tfCapacity.getText().trim());
            double dv = Double.parseDouble(tfMaxDist.getText().trim());
            System.out.println("Parameters read: DAs=" + numDAs + ", cap=" + cap + ", dv=" + dv);

            // --- CREACIÓN DE AGENTES ---
            mainContainer.createNewAgent("MRA", "vrp.ManagerAgent", null).start();
            for (int i = 1; i <= numDAs; i++) {
                mainContainer.createNewAgent("DA" + i, "vrp.DeliveryAgent", new Object[]{cap, dv}).start();
            }
            guiAgent = new GuiAgent();
            guiAgent.setUi(this);
            mainContainer.acceptNewAgent("GuiAgent", guiAgent).start();
            System.out.println("All agents created successfully.");

            // --- ACTUALIZACIÓN SEGURA DE LA UI ---
            // Usamos invokeLater para garantizar que la UI se actualiza en el hilo correcto.
            SwingUtilities.invokeLater(() -> {
                status.setText("JADE launched. DAs will register shortly.");
                btnLaunch.setEnabled(false);
                btnOptimize.setEnabled(true); // <--- Habilitar el botón
                System.out.println("Optimize button has been enabled.");
            });

        } catch (Exception ex) {
            // Si algo falla, lo veremos claramente.
            showErr(ex); // Muestra el popup de error
            SwingUtilities.invokeLater(() -> {
                status.setText("ERROR launching JADE! Check logs.");
            });
            System.err.println("--- JADE LAUNCH FAILED ---");
            ex.printStackTrace(); // Imprime el error detallado en la consola
        }
    }

    private void onOptimize() {
        if (guiAgent == null) {
            showErr(new Exception("Launch the JADE platform first!"));
            return;
        }
        if (items.isEmpty()) {
            showErr(new Exception("Load or generate items first!"));
            return;
        }
        status.setText("Requesting optimization from MRA...");
        guiAgent.requestOptimization(items);
    }

    // --- Métodos de la UI (sin cambios importantes) ---
    public void updateRoutes(Map<String, java.util.List<Item>> newRoutes, String statusText) {
        routes.clear();
        routes.putAll(newRoutes);
        status.setText(statusText);
        canvas.repaint();
    }

    private void onLoadOrGenerate() {
        try {
            if (rbAuto.isSelected()) {
                int n = Integer.parseInt(tfNumItems.getText().trim());
                long seed = Long.parseLong(tfSeed.getText().trim());
                items = randomItems(n, seed);
            } else {
                items = ItemsParser.parseItems(tfFile.getText().trim());
            }
            routes.clear();
            status.setText("Loaded items: " + items.size());
            canvas.repaint();
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser(new File("data"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            tfFile.setText(fc.getSelectedFile().getPath());
        }
    }

    private void setupActionListeners() {
        btnBrowse.addActionListener(e -> onBrowse());
        btnLoadGen.addActionListener(e -> onLoadOrGenerate());
        btnOptimize.addActionListener(e -> onOptimize());
        btnLaunch.addActionListener(e -> onLaunchJade());
        btnOptimize.setEnabled(false);
    }

    private static java.util.List<Item> randomItems(int n, long seed) {
        Random rnd = new Random(seed);
        java.util.List<Item> list = new ArrayList<>();
        for (int i = 1; i <= n; i++)
            list.add(new Item("i" + i, rnd.nextDouble() * 400, rnd.nextDouble() * 300, rnd.nextDouble() * 5 + 1));
        return list;
    }

    private static void showErr(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    // En MasUI.java
    private void setupUIComponents() {
        // --- 1. PANEL DE PARÁMETROS (Izquierda) ---
        // (Este panel ahora solo contiene la configuración, sin botones ni log)
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
        grid.setBorder(new TitledBorder("Parameters"));
        grid.add(new JLabel("# Items:")); grid.add(tfNumItems);
        grid.add(new JLabel("# DAs:")); grid.add(tfNumDAs);
        grid.add(new JLabel("Capacity/DA (items):")); grid.add(tfCapacity);
        grid.add(new JLabel("Max distance/DA:")); grid.add(tfMaxDist);
        grid.add(new JLabel("Seed:")); grid.add(tfSeed);

        ButtonGroup bg = new ButtonGroup(); bg.add(rbAuto); bg.add(rbFile);
        JPanel rbBox = new JPanel(new GridLayout(0, 1));
        rbBox.add(rbAuto); rbBox.add(rbFile);

        JPanel fileBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fileBox.add(tfFile); fileBox.add(btnBrowse);

        JPanel inputBox = new JPanel(new BorderLayout(6, 6));
        inputBox.setBorder(new TitledBorder("Items input"));
        inputBox.add(rbBox, BorderLayout.NORTH);
        inputBox.add(fileBox, BorderLayout.CENTER);

        JPanel optBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        optBox.setBorder(new TitledBorder("Optimization"));
        optBox.add(new JLabel("Technique:"));
        optBox.add(cbOpt);

        left.add(grid);
        left.add(Box.createVerticalStrut(8));
        left.add(inputBox);
        left.add(Box.createVerticalStrut(8));
        left.add(optBox);

        // --- 2. ZONA CENTRAL (Split Pane con el canvas) ---
        canvas.setPreferredSize(new Dimension(800, 500));
        canvas.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        // IMPORTANTE: Ahora el panel izquierdo va dentro de un JScrollPane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(left), new JScrollPane(canvas));
        split.setDividerLocation(380);

        // --- 3. BARRA DE ACCIONES (Arriba) ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actionPanel.setBorder(BorderFactory.createEtchedBorder());
        actionPanel.add(btnLoadGen);
        actionPanel.add(btnOptimize);
        actionPanel.add(btnLaunch);

        // --- 4. PANEL INFERIOR (Log y Barra de Estado) ---
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(300, 120)); // Damos un tamaño preferido

        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.add(status, BorderLayout.CENTER);

        JSplitPane southSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusBar, logScrollPane);
        southSplit.setBorder(BorderFactory.createEmptyBorder());
        southSplit.setResizeWeight(0); // El divisor no se mueve al cambiar tamaño

        // --- 5. PANEL RAÍZ (Uniendo todo) ---
        JPanel root = new JPanel(new BorderLayout());
        root.add(actionPanel, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(southSplit, BorderLayout.SOUTH);

        setContentPane(root);
        pack(); // pack() ahora funcionará mucho mejor
        setMinimumSize(new Dimension(800, 600)); // Establecemos un tamaño mínimo
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MasUI ui = new MasUI();
            ui.setVisible(true);
            ConsoleRedirector.redirectSystemStreams(ui.logArea);
        });
    }
}
