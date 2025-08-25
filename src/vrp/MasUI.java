
package vrp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class MasUI extends JFrame {

    private JTextField numAgentsField;
    private JTextField speedField;
    private JTextField capacityField;
    private JButton startButton;
    private JTextArea logArea;
    private Main jadeStarter; // Referencia a la clase que inicia JADE

    public MasUI() {
        super("MAS VRP Configuration");
        jadeStarter = new Main();

        // --- Panel de Configuración ---
        JPanel configPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        configPanel.add(new JLabel("Number of Delivery Agents:"));
        numAgentsField = new JTextField("3");
        configPanel.add(numAgentsField);

        configPanel.add(new JLabel("Speed (km/h):"));
        speedField = new JTextField("35.0");
        configPanel.add(speedField);

        configPanel.add(new JLabel("Capacity per Agent:"));
        capacityField = new JTextField("3");
        configPanel.add(capacityField);

        startButton = new JButton("Start MAS");
        configPanel.add(new JLabel()); // Placeholder
        configPanel.add(startButton);

        // --- Panel de Log ---
        logArea = new JTextArea(20, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("System Log"));

        // Redirigir la salida de la consola al JTextArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(logArea));
        System.setOut(printStream);
        System.setErr(printStream);

        // --- Layout Principal ---
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.add(configPanel, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // --- ActionListener para el botón ---
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int numAgents = Integer.parseInt(numAgentsField.getText());
                    double speed = Double.parseDouble(speedField.getText());
                    int capacity = Integer.parseInt(capacityField.getText());

                    if (numAgents <= 0 || speed <= 0 || capacity <= 0) {
                        JOptionPane.showMessageDialog(MasUI.this,
                                "Please enter positive values.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Iniciar el sistema JADE en un nuevo hilo para no bloquear la UI
                    new Thread(() -> {
                        jadeStarter.startJade(numAgents, speed, capacity);
                    }).start();

                    startButton.setEnabled(false); // Desactivar el botón después de iniciar
                    startButton.setText("MAS Running...");

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(MasUI.this,
                            "Invalid input. Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- Configuración de la Ventana ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null); // Centrar en la pantalla
        setVisible(true);
    }

    // Clase para redirigir el output de la consola
    public static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;
        public CustomOutputStream(JTextArea textArea) { this.textArea = textArea; }
        @Override
        public void write(int b) throws IOException {
            textArea.append(String.valueOf((char)b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        // Ejecutar la UI
        SwingUtilities.invokeLater(() -> new MasUI());
    }
}