package ui;

import javax.swing.*;
import java.awt.*;

public class VentanaConexion extends JFrame {
    public VentanaConexion() {
        setTitle("Conexión al Servidor");
        setSize(400, 280); // Aumentamos un poco el alto para que respiren los componentes
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Usamos 5 filas para dar espacio al nuevo componente
        setLayout(new GridLayout(5, 2, 10, 10));

        JTextField txtIp = new JTextField("192.168.1.12");
        JTextField txtPort = new JTextField("8090");

        // --- CONFIGURACIÓN DE RADIO BUTTONS ---
        JRadioButton rbTcp = new JRadioButton("TCP", true); // Seleccionado por defecto
        JRadioButton rbUdp = new JRadioButton("UDP");

        // El ButtonGroup hace que la selección sea exclusiva
        ButtonGroup grupoProtocolo = new ButtonGroup();
        grupoProtocolo.add(rbTcp);
        grupoProtocolo.add(rbUdp);

        // Panel para agrupar los radios en una sola celda del GridLayout
        JPanel pnlRadio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlRadio.add(rbTcp);
        pnlRadio.add(rbUdp);

        JButton btnConectar = new JButton("Conectar");

        // --- AGREGAR COMPONENTES ---
        add(new JLabel("  Dirección IP:"));
        add(txtIp);

        add(new JLabel("  Puerto:"));
        add(txtPort);

        add(new JLabel("  Protocolo:"));
        add(pnlRadio);

        // Espacio vacío para alinear el botón a la derecha
        add(new JLabel(""));
        add(btnConectar);

        btnConectar.addActionListener(e -> {
            // Lógica para saber cuál está seleccionado
            String protocolo = rbTcp.isSelected() ? "TCP" : "UDP";
            System.out.println("Conectando via " + protocolo + "...");

            new Dashboard(txtIp.getText(), txtPort.getText()).setVisible(true);
            this.dispose();
        });

        // Margen interno para que no pegue a los bordes de la ventana
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLocationRelativeTo(null);
    }
}