// ConsoleRedirector.java
package vrp;

import javax.swing.JTextArea;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class ConsoleRedirector extends OutputStream {
    private final JTextArea textArea;

    public ConsoleRedirector(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        // Redirige los datos a la JTextArea
        textArea.append(String.valueOf((char) b));
        // Mueve el cursor al final para que siempre se vea lo último
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    public static void redirectSystemStreams(JTextArea logArea) {
        OutputStream out = new ConsoleRedirector(logArea);
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
}