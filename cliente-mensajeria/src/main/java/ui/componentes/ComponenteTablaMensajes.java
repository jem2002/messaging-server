package ui.componentes;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class ComponenteTablaMensajes extends JPanel {
    private JTable tabla;
    private DefaultTableModel modelo;

    public ComponenteTablaMensajes() {
        setLayout(new BorderLayout());

        String[] col = {"Emisor", "Contenido", "Descargar"};
        modelo = new DefaultTableModel(col, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1 || c == 2; }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(90); // Altura generosa para varias líneas de texto
        tabla.setRowSelectionAllowed(false);
        tabla.setFillsViewportHeight(true);

        // --- MANEJO DE ANCHOS ---
        TableColumnModel tcm = tabla.getColumnModel();

        // Emisor: Ancho fijo y pequeño
        tcm.getColumn(0).setPreferredWidth(120);
        tcm.getColumn(0).setMaxWidth(150);

        // Contenido: Flexible y amplio
        tcm.getColumn(1).setPreferredWidth(500);
        tcm.getColumn(1).setCellRenderer(new MensajeCopiableRenderer());
        tcm.getColumn(1).setCellEditor(new MensajeCopiableEditor());

        // Descargar: Ancho fijo para los dos botones
        tcm.getColumn(2).setPreferredWidth(220);
        tcm.getColumn(2).setMinWidth(220);
        tcm.getColumn(2).setCellRenderer(new RendererGenerico(false));
        tcm.getColumn(2).setCellEditor(new EditorGenerico(tabla, false));

        // Ejemplo de mensaje largo para probar el crecimiento vertical
// Ejemplo 1: Mensaje técnico con instrucciones
        agregarMensaje("192.168.1.10", "Se ha detectado una nueva versión del protocolo en el nodo central. Por favor, asegúrense de actualizar sus clientes antes del reinicio programado para las 00:00 UTC.");

// Ejemplo 2: Mensaje corto (Verifica alineación)
        agregarMensaje("Servidor", "Conexión establecida con éxito.");

// Ejemplo 3: Diálogo con datos sensibles (Ideal para probar el botón de Encriptado)
        agregarMensaje("192.168.1.15", "La clave de acceso temporal para el repositorio compartido es: AC-7789-XQ2. Recuerden que este mensaje debe ser borrado tras su uso por motivos de seguridad.");

// Ejemplo 4: Mensaje muy largo para probar el crecimiento vertical
        agregarMensaje("192.168.1.4", "Atención equipo: He subido los archivos del proyecto final. El paquete incluye los diagramas de arquitectura, el esquema de la base de datos y la documentación de la API. Si encuentran algún error en el Hash al momento de descargar, por favor notifíquenlo de inmediato en este chat para resubir las partes corruptas.");

// Ejemplo 5: Alerta de seguridad
        agregarMensaje("Firewall", "ADVERTENCIA: Se han detectado múltiples intentos fallidos de inicio de sesión desde la IP 10.0.0.5. El acceso ha sido restringido temporalmente por seguridad.");

// Ejemplo 6: Mensaje con caracteres especiales o rutas
        agregarMensaje("192.168.1.6", "He movido los logs de la carpeta /var/log/app/ a la carpeta compartida /mnt/backup/logs/. El proceso tomó aproximadamente 15 minutos y no hubo pérdida de paquetes.");

        agregarMensaje("192.168.1.8", "Pendientes para hoy:\n1. Revisar logs del servidor.\n2. Validar firmas digitales de los paquetes entrantes.\n3. Actualizar la base de datos de clientes conocidos.");

// Ejemplo 9: Error crítico de red
        agregarMensaje("Sistema", "ERROR CRÍTICO: Se ha perdido la sincronización con el nodo secundario 192.168.1.250. Se recomienda verificar la integridad del cableado o el estado del switch principal.");

// Ejemplo 10: Mensaje con una URL larga (prueba el ajuste de línea)
        agregarMensaje("Admin", "Para más detalles sobre la encriptación AES-256 utilizada en este software, consulten la documentación oficial en: https://csrc.nist.gov/publications/detail/fips/197/final");

// Ejemplo 11: Mensaje de broma o social (para simular un chat real)
        agregarMensaje("192.168.1.22", "¿Alguien sabe si el café de la sala de servidores sigue caliente? XD. Por cierto, ya terminé de subir los archivos de la auditoría.");

// Ejemplo 12: Mensaje corto de sistema
        agregarMensaje("192.168.1.5", "Archivo recibido: 'backup_db_2026.sql'.");

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public void agregarMensaje(String emisor, String texto) {
        modelo.addRow(new Object[]{emisor, texto, ""});
    }
}