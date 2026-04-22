package ui.componentes;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;


class EditorGenerico extends AbstractCellEditor implements TableCellEditor {
    private PanelBotonesGenerico panel;
    private JTable tabla;
    public EditorGenerico(JTable tabla, boolean mostrarO) {
        this.tabla = tabla;
        this.panel = new PanelBotonesGenerico(mostrarO);
        panel.btnOriginal.addActionListener(e -> { accion("Original"); fireEditingStopped(); });
        panel.btnHash.addActionListener(e -> { accion("Hash"); fireEditingStopped(); });
        panel.btnEnc.addActionListener(e -> { accion("Encriptado"); fireEditingStopped(); });
    }
    private void accion(String tipo) {
        String item = (String) tabla.getValueAt(tabla.getEditingRow(), 0);
        JOptionPane.showMessageDialog(null, tipo + " en: " + item);
    }
    @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
        panel.setBackground(t.getSelectionBackground());
        return panel;
    }
    @Override public Object getCellEditorValue() { return ""; }
}