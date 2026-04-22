package ui;

import ui.componentes.ComponenteClientes;
import ui.componentes.ComponenteLogs;
import ui.componentes.ComponenteTablaArchivos;
import ui.componentes.ComponenteTablaMensajes;

import javax.swing.*;
import java.awt.*;

public class Dashboard extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel pnlCartas;
    private ComponenteClientes panelClientes;
    private ComponenteLogs panelLogs;
    private ComponenteTablaArchivos tablaArchivos;
    private ComponenteTablaMensajes tablaMensajes;
    private JRadioButton rbArchivos, rbMensajes;

    public Dashboard(String ip, String puerto) {
        setTitle("Dashboard - Conectado a " + ip);
        setSize(1250, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        panelClientes = new ComponenteClientes();
        panelLogs = new ComponenteLogs();

        pnlCartas = new JPanel(cardLayout);
        tablaArchivos = new ComponenteTablaArchivos();
        tablaMensajes = new ComponenteTablaMensajes();

        pnlCartas.add(tablaArchivos, "TABLA_ARCHIVOS");
        pnlCartas.add(tablaMensajes, "TABLA_MENSAJES");

        JPanel pnlHerramientas = crearPanelHerramientas();

        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlFooter.setBackground(new Color(230, 230, 230));
        JLabel lblStatus = new JLabel(" CONECTADO A IP DEL SERVIDOR: " + ip + " EN PUERTO: " + puerto);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlFooter.add(lblStatus);

        JPanel pnlCentroContenedor = new JPanel(new BorderLayout());
        pnlCentroContenedor.add(pnlHerramientas, BorderLayout.NORTH);
        pnlCentroContenedor.add(pnlCartas, BorderLayout.CENTER);

        add(panelClientes, BorderLayout.WEST);
        add(pnlCentroContenedor, BorderLayout.CENTER);
        add(panelLogs, BorderLayout.EAST);
        add(pnlFooter, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel crearPanelHerramientas() {
        // Usamos un BorderLayout interno para poder mandar el botón de desconectar a la derecha
        JPanel pnlPrincipal = new JPanel(new BorderLayout());

        // Panel izquierdo para las opciones normales
        JPanel pnlIzquierdo = new JPanel(new FlowLayout(FlowLayout.LEFT));

        rbArchivos = new JRadioButton("Archivos", true);
        rbMensajes = new JRadioButton("Mensajes");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbArchivos);
        grupo.add(rbMensajes);

        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnEnviarArch = new JButton("Enviar Archivo");
        JButton btnEnviarMsg = new JButton("Enviar Mensaje");

        // --- BOTÓN DESCONECTAR ---
        JButton btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setForeground(new Color(150, 0, 0)); // Color rojo oscuro para advertir
        btnDesconectar.setFont(new Font("SansSerif", Font.BOLD, 12));

        // Lógica de intercambio de tablas
        btnFiltrar.addActionListener(e -> {
            if (rbArchivos.isSelected()) {
                cardLayout.show(pnlCartas, "TABLA_ARCHIVOS");
            } else {
                cardLayout.show(pnlCartas, "TABLA_MENSAJES");
            }
        });

        // Lógica de desconexión
        btnDesconectar.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que deseas cerrar la conexión?",
                    "Confirmar desconexión", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Aquí cerrarías los sockets en una app real
                new VentanaConexion().setVisible(true);
                this.dispose();
            }
        });

        btnEnviarArch.addActionListener(e -> abrirVentanaEnvioArchivo());
        btnEnviarMsg.addActionListener(e -> abrirVentanaEnvioMensaje());

        // Ensamblar panel izquierdo
        pnlIzquierdo.add(rbArchivos);
        pnlIzquierdo.add(rbMensajes);
        pnlIzquierdo.add(btnFiltrar);
        pnlIzquierdo.add(new JSeparator(SwingConstants.VERTICAL));
        pnlIzquierdo.add(btnEnviarArch);
        pnlIzquierdo.add(btnEnviarMsg);

        // Panel derecho para Desconectar
        JPanel pnlDerecho = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlDerecho.add(btnDesconectar);

        pnlPrincipal.add(pnlIzquierdo, BorderLayout.WEST);
        pnlPrincipal.add(pnlDerecho, BorderLayout.EAST);

        return pnlPrincipal;
    }

    private void abrirVentanaEnvioArchivo() {
        JDialog ventana = new JDialog(this, "Seleccionar archivo(s)", true);
        ventana.setLayout(new FlowLayout());
        JButton btnSeleccionar = new JButton("Seleccionar archivo(s)");
        JButton btnEnviar = new JButton("Enviar");

        btnEnviar.addActionListener(e -> {
            btnEnviar.setEnabled(false);
            btnEnviar.setText("Cargando...");
        });

        ventana.add(btnSeleccionar);
        ventana.add(btnEnviar);
        ventana.setSize(400, 150);
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);
    }

    private void abrirVentanaEnvioMensaje() {
        JDialog ventana = new JDialog(this, "Enviar Mensaje", true);
        ventana.setLayout(new BorderLayout());
        JTextArea txtMsg = new JTextArea(5, 30);
        JButton btnEnviar = new JButton("Enviar");

        ventana.add(new JScrollPane(txtMsg), BorderLayout.CENTER);
        ventana.add(btnEnviar, BorderLayout.SOUTH);
        ventana.pack();
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);
    }
}