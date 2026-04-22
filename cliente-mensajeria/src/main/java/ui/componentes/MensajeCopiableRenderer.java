package ui.componentes;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MensajeCopiableRenderer extends JPanel implements TableCellRenderer {
    private JTextArea textArea = new JTextArea();
    private JButton btnCopiar = new JButton("📋");

    public MensajeCopiableRenderer() {
        setLayout(new BorderLayout(5, 5));
        setBackground(Color.WHITE);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

        btnCopiar.setPreferredSize(new Dimension(30, 30));

        add(textArea, BorderLayout.CENTER);

        // Panel para que el botón no se estire verticalmente
        JPanel pnlBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlBoton.setOpaque(false);
        pnlBoton.add(btnCopiar);
        add(pnlBoton, BorderLayout.EAST);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
        textArea.setText(v != null ? v.toString() : "");

        // Ajuste dinámico: obligamos al JTextArea a saber cuánto ancho tiene la columna
        textArea.setSize(t.getColumnModel().getColumn(c).getWidth() - 40, 1);

        return this;
    }
}