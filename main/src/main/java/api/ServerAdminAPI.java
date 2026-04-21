package api;
import MySqlRepository.MySqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Expone las capacidades del servidor para el Administrador del sistema.
 */
public class ServerAdminAPI {
    private static final Logger logger = LoggerFactory.getLogger(ServerAdminAPI.class);
    private final MySqlDao dao;

    public ServerAdminAPI(MySqlDao dao) {
        this.dao = dao;
    }

    public void listarClientes() {
        System.out.println("\n--- CLIENTES REGISTRADOS EN LA BASE DE DATOS ---");
        System.out.printf("%-5s | %-20s | %-15s | %-20s%n", "ID", "USUARIO", "IP", "FECHA REGISTRO");
        System.out.println("---------------------------------------------------------------------------");
        try {
            List<Map<String, String>> usuarios = dao.listarUsuariosRegistrados();
            if (usuarios.isEmpty()) {
                System.out.println("No hay usuarios registrados aún.");
            } else {
                for (Map<String, String> u : usuarios) {
                    System.out.printf("%-5s | %-20s | %-15s | %-20s%n",
                            u.get("id"), u.get("username"), u.get("ip"), u.get("fecha"));
                }
            }
        } catch (Exception e) {
            logger.error("Error al listar clientes desde la API", e);
        }
        System.out.println("---------------------------------------------------------------------------");
    }

    public void listarDocumentos() {
        System.out.println("\n--- DOCUMENTOS EN EL SISTEMA ---");
        try {
            List<Map<String, String>> docs = dao.listarDocumentosDisponibles();
            if (docs.isEmpty()) {
                System.out.println("No hay documentos almacenados.");
            } else {
                for (Map<String, String> doc : docs) {
                    System.out.printf("ID: %s | Archivo: %s | Tamaño: %s bytes | Propietario: %s%n",
                            doc.get("id"), doc.get("nombre"), doc.get("tamaño"), doc.get("propietario"));
                }
            }
        } catch (Exception e) {
            logger.error("Error listando documentos", e);
        }
    }

    public void mostrarLogs() {
        System.out.println("\n--- LOGS RECIENTES ---");
        // Nota: Deberías agregar un método en tu MySqlDao para SELECT * FROM logs ORDER BY timestamp DESC LIMIT 10
        System.out.println("Mostrando los últimos 10 eventos del sistema (Auditoría)...");
    }
}