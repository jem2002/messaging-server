package ui.componentes;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class ComponenteTablaArchivos extends JPanel {
    public ComponenteTablaArchivos() {
        setLayout(new BorderLayout());
        String[] columnas = {"Nombre", "Tamaño", "Extensión", "Propietario", "Descargar"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(45);
        tabla.setRowSelectionAllowed(false); // Desactiva la selección de filas
        tabla.setCellSelectionEnabled(false); // Desactiva la selección de celdas
        tabla.setFocusable(false);

        modelo.addRow(new Object[]{"informe", "1.4 MB", ".pdf", "192.168.1.6", ""});
        modelo.addRow(new Object[]{"shrek", "1200 MB", ".mp4", "192.168.1.7", ""});

        TableColumn col = tabla.getColumnModel().getColumn(4);
        col.setPreferredWidth(320);
        col.setCellRenderer(new RendererGenerico(true));
        col.setCellEditor(new EditorGenerico(tabla, true));

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }
}