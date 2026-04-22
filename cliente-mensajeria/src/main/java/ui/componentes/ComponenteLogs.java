package ui.componentes;

import javax.swing.*;
import java.awt.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ComponenteLogs extends JPanel {
    private JTextArea areaTexto;

    public ComponenteLogs() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("INFORME LOGS"));
        setPreferredSize(new Dimension(280, 0));

        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 11)); // Fuente tipo consola
        areaTexto.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(areaTexto);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scroll, BorderLayout.CENTER);

        // --- DATOS DE EJEMPLO PARA RELLENAR EL SCROLL ---
        insertarDatosIniciales();
    }

    private void insertarDatosIniciales() {
        registrarEvento("SYSTEM: Servidor iniciado en puerto 8090");
        registrarEvento("IP 192.168.1.6: CONNECTED");
        registrarEvento("IP 192.168.1.6: UPLOAD \"informe.pdf\" (1.4MB) - SUCCESS");
        registrarEvento("IP 192.168.1.6: SEND_MSG \"Hola a todos, acabo de subir...\"");
        registrarEvento("IP 192.168.1.4: CONNECTED");
        registrarEvento("IP 192.168.1.7: CONNECTED");
        registrarEvento("IP 192.168.1.6: DISCONNECTED (User logout)");
        registrarEvento("IP 192.168.1.7: UPLOAD \"gato.jpg\" (6.7MB) - SUCCESS");
        registrarEvento("IP 192.168.1.4: DOWNLOAD \"informe.pdf\" - SUCCESS");
        registrarEvento("IP 192.168.1.9: CONNECTED");
        registrarEvento("IP 192.168.1.7: UPLOAD \"shrek.mp4\" (1200MB) - SUCCESS");
        registrarEvento("IP 192.168.1.7: DOWNLOAD \"shrek.mp4\" (Encrypted) - SUCCESS");
        registrarEvento("SYSTEM: Backup automático de base de datos finalizado");
        registrarEvento("IP 192.168.1.4: ATTEMPT_DOWNLOAD \"shrek.mp4\" - ERROR: Key mismatch");
        registrarEvento("IP 192.168.1.12: SYSTEM_PING - Latency: 14ms");
        registrarEvento("IP 192.168.1.9: SEND_MSG \"¿Alguien tiene la llave para el video?\"");
        registrarEvento("IP 192.168.1.7: SEND_MSG \"Yo te la paso por privado\"");
        registrarEvento("IP 192.168.1.4: DISCONNECTED (Timeout)");
        registrarEvento("SYSTEM: Limpieza de archivos temporales ejecutada");
        registrarEvento("IP 192.168.1.15: CONNECTION_REJECTED (Invalid Protocol)");
    }

    public void registrarEvento(String mensaje) {
        // Obtenemos la hora actual para cada log
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        areaTexto.append("[" + hora + "] " + mensaje + "\n");

        // Asegura que el scroll baje automáticamente al final
        SwingUtilities.invokeLater(() -> {
            areaTexto.setCaretPosition(areaTexto.getDocument().getLength());
        });
    }
}