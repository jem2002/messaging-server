package ui.componentes;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

class RendererGenerico implements TableCellRenderer {
    private boolean mostrarO;
    public RendererGenerico(boolean mostrarO) { this.mostrarO = mostrarO; }
    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
        PanelBotonesGenerico p = new PanelBotonesGenerico(mostrarO);
        p.setBackground(s ? t.getSelectionBackground() : t.getBackground());
        return p;
    }
}