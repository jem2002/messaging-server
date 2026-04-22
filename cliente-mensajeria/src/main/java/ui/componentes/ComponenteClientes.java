package ui.componentes;

import javax.swing.*;
import java.awt.*;

public class ComponenteClientes extends JPanel {
    public ComponenteClientes() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Clientes Conectados (4/10)"));
        setPreferredSize(new Dimension(220, 0));

        DefaultListModel<String> modeloLista = new DefaultListModel<>();
        // Llenado de ejemplo
        for(int i=1; i<=30; i++) {
            modeloLista.addElement("<html>IP: 192.168.1." + i + "<br/><font color='gray'>Conexión: 10:00:0" + i + "</font></html>");
        }

        JList<String> lista = new JList<>(modeloLista);

        // AGREGAR SCROLL BAR
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scroll, BorderLayout.CENTER);
    }
}