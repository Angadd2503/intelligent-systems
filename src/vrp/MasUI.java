package vrp;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MasUI extends JFrame {

    // Controls
    private final JTextField tfNumItems = new JTextField("30", 5);
    private final JTextField tfNumDAs   = new JTextField("3", 5);
    private final JTextField tfCapacity = new JTextField("10", 5);
    private final JTextField tfMaxDist  = new JTextField("300", 5);
    private final JTextField tfSeed     = new JTextField("42", 5);
    private final JComboBox<String> cbOpt = new JComboBox<>(new String[]{"GREEDY"});

    private final JRadioButton rbAuto = new JRadioButton("Generate automatically", true);
    private final JRadioButton rbFile = new JRadioButton("Load from file");
    private final JTextField tfFile   = new JTextField("", 18);
    private final JButton btnBrowse   = new JButton("Browse…");
    private final JButton btnLoadGen  = new JButton("Load / Generate");
    private final JButton btnOptimize = new JButton("Optimize");
    private final JButton btnLaunch   = new JButton("Launch MAS (JADE)");

    private final JLabel status = new JLabel("Ready");

    // Data
    private java.util.List<Item> items = new java.util.ArrayList<>();
    private final Map<String, java.util.List<Item>> routes = new LinkedHashMap<>();

    // Canvas
    private final JPanel canvas = new JPanel() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // depot
            g2.setColor(Color.GRAY);
            g2.drawOval(38, 38, 10, 10);
            g2.drawString("DEPOT(0,0)", 55, 35);

            // items
            g2.setColor(Color.BLACK);
            for (Item it : items) {
                int x = 40 + (int) Math.round(it.getX());
                int y = 40 + (int) Math.round(it.getY());
                g2.fillOval(x - 3, y - 3, 6, 6);
                g2.drawString(it.getId(), x + 5, y - 5);
            }

            // routes
            Random r = new Random(123);
            for (Map.Entry<String, java.util.List<Item>> e : routes.entrySet()) {
                g2.setColor(new Color(80 + r.nextInt(150), 80 + r.nextInt(150), 80 + r.nextInt(150)));
                java.util.List<Item> seq = e.getValue();
                int lastX = 40, lastY = 40; // depot
                for (Item it : seq) {
                    int x = 40 + (int) Math.round(it.getX());
                    int y = 40 + (int) Math.round(it.getY());
                    g2.drawLine(lastX, lastY, x, y);
                    lastX = x; lastY = y;
                }
                g2.drawLine(lastX, lastY, 40, 40); // back to depot
                g2.drawString(e.getKey(), Math.max(45, lastX), Math.max(45, lastY));
            }
        }
    };

    public MasUI() {
        super("VRP MAS Config UI (Swing)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Left panel
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JPanel grid = new JPanel(new GridLayout(0,2,6,6));
        grid.setBorder(new TitledBorder("Parameters"));
        grid.add(new JLabel("# Items:")); grid.add(tfNumItems);
        grid.add(new JLabel("# DAs:"));   grid.add(tfNumDAs);
        grid.add(new JLabel("Capacity per DA:")); grid.add(tfCapacity);
        grid.add(new JLabel("Max distance dv:")); grid.add(tfMaxDist);
        grid.add(new JLabel("Seed:")); grid.add(tfSeed);

        ButtonGroup bg = new ButtonGroup(); bg.add(rbAuto); bg.add(rbFile);

        JPanel rbBox = new JPanel(new GridLayout(0,1));
        rbBox.add(rbAuto); rbBox.add(rbFile);

        JPanel fileBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fileBox.add(tfFile); fileBox.add(btnBrowse);

        JPanel inputBox = new JPanel(new BorderLayout(6,6));
        inputBox.setBorder(new TitledBorder("Items input"));
        inputBox.add(rbBox, BorderLayout.NORTH);
        inputBox.add(fileBox, BorderLayout.CENTER);

        JPanel optBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        optBox.setBorder(new TitledBorder("Optimization"));
        optBox.add(new JLabel("Technique:"));
        optBox.add(cbOpt);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        btns.add(btnLoadGen); btns.add(btnOptimize); btns.add(btnLaunch);

        left.add(grid);
        left.add(Box.createVerticalStrut(8));
        left.add(inputBox);
        left.add(Box.createVerticalStrut(8));
        left.add(optBox);
        left.add(Box.createVerticalStrut(8));
        left.add(btns);
        left.add(Box.createVerticalStrut(8));
        left.add(status);

        // Canvas
        canvas.setPreferredSize(new Dimension(820, 560));
        canvas.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, new JScrollPane(canvas));
        split.setDividerLocation(380);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.add(status, BorderLayout.WEST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(split, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(null);

        // events
        btnBrowse.addActionListener(e -> onBrowse());
        btnLoadGen.addActionListener(e -> onLoadOrGenerate());
        btnOptimize.addActionListener(e -> onOptimize());
        btnLaunch.addActionListener(e -> onLaunchJade());
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser(new File("data"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            tfFile.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void onLoadOrGenerate() {
        try {
            if (rbAuto.isSelected()) {
                int n = Integer.parseInt(tfNumItems.getText().trim());
                long seed = Long.parseLong(tfSeed.getText().trim());
                items = randomItems(n, seed);
            } else {
                String path = tfFile.getText().trim();
                if (path.isEmpty()) throw new IllegalArgumentException("Select an items file.");
                items = ItemsParser.parseItems(path);
            }
            routes.clear();
            status.setText("Loaded items: " + items.size());
            canvas.repaint();
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void onOptimize() {
        try {
            if (items.isEmpty()) onLoadOrGenerate();
            int numDAs = Integer.parseInt(tfNumDAs.getText().trim());
            int cap    = Integer.parseInt(tfCapacity.getText().trim());
            double dv  = Double.parseDouble(tfMaxDist.getText().trim());

            GreedyOptimizer.Result res = GreedyOptimizer.solve(items, numDAs, cap, dv);
            routes.clear();
            routes.putAll(res.routes);


            status.setText("Delivered: " + res.delivered + "  •  Total distance: " + String.format("%.1f", res.totalDistance));
            canvas.repaint();
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void onLaunchJade() {
        try {
            int numDAs = Integer.parseInt(tfNumDAs.getText().trim());
            int cap    = Integer.parseInt(tfCapacity.getText().trim());
            double dv  = Double.parseDouble(tfMaxDist.getText().trim());

            StringBuilder agents = new StringBuilder();
            agents.append("MRA:vrp.ManagerAgent(opt=GREEDY)");
            for (int i=1;i<=numDAs;i++) {
                agents.append(";DA").append(i).append(":vrp.DeliveryAgent(cap=").append(cap).append(",dv=").append(dv).append(")");
            }

            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(System.getProperty("java.home")+File.separator+"bin"+File.separator+"java");
            cmd.add("-cp"); cmd.add(System.getProperty("java.class.path"));
            cmd.add("jade.Boot");
            cmd.add("-gui");
            cmd.add("-agents"); cmd.add(agents.toString());

            new ProcessBuilder(cmd).inheritIO().start();
            status.setText("JADE launched with " + numDAs + " DAs.");
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    // IMPORTANT tweak: generate items within 0..300 x 0..200 so dv=300 works by default
    private static java.util.List<Item> randomItems(int n, long seed) {
        Random rnd = new Random(seed);
        java.util.List<Item> list = new java.util.ArrayList<>();
        for (int i=1;i<=n;i++) list.add(new Item("i"+i, rnd.nextDouble()*300, rnd.nextDouble()*200));
        return list;
    }

    private static void showErr(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
