package ui.componentes;


import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class MensajeCopiableEditor extends AbstractCellEditor implements TableCellEditor {
    private JPanel panel = new JPanel(new BorderLayout(5, 5));
    private JTextArea textArea = new JTextArea();
    private JButton btnCopiar = new JButton("📋");

    public MensajeCopiableEditor() {
        panel.setBackground(Color.WHITE);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

        btnCopiar.addActionListener(e -> {
            StringSelection sel = new StringSelection(textArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            btnCopiar.setText("✅");
            Timer timer = new Timer(1000, ev -> {
                btnCopiar.setText("📋");
                fireEditingStopped();
            });
            timer.setRepeats(false);
            timer.start();
        });

        panel.add(textArea, BorderLayout.CENTER);

        JPanel pnlBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlBoton.setOpaque(false);
        pnlBoton.add(btnCopiar);
        panel.add(pnlBoton, BorderLayout.EAST);

        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    @Override
    public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
        textArea.setText(v != null ? v.toString() : "");
        textArea.setSize(t.getColumnModel().getColumn(c).getWidth() - 40, 1);
        return panel;
    }

    @Override
    public Object getCellEditorValue() { return textArea.getText(); }
}