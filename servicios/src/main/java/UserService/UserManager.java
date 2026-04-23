package UserService;

import MySqlRepository.MySqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserManager {
    // 1. EL LOGGER QUE FALTABA
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private final MySqlDao dao;

    public UserManager(MySqlDao dao) {
        this.dao = dao;
    }

    public long conectarUsuario(String username, String ipAddress, int port) throws Exception {
        long userId = dao.obtenerORegistrarUsuario(username, ipAddress);
        dao.registrarSesionActiva(userId, ipAddress, port, "TCP");
        return userId;
    }

    // 2. NUEVO MÉTODO PARA CERRAR LA SESIÓN
    public long desconectarPorCaidaDeRed(String ipAddress, int port) {
        try {
            long userId = dao.cerrarSesionPorIpYPuerto(ipAddress, port);
            logger.info("Estado actualizado: Sesión cerrada en BD para {}:{} (User ID: {})", ipAddress, port, userId);
            return userId;
        } catch (Exception e) {
            logger.error("Error al cerrar sesión por caída de red", e);
            return 0;
        }
    }
    public List<Map<String, String>> obtenerClientesActivos() {
        try {
            return dao.listarClientesActivos();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de clientes activos", e);
            return new ArrayList<>(); // Retorna lista vacía si hay error
        }
    }
    public long obtenerIdUsuario(String username) throws Exception {
        return dao.obtenerIdUsuarioPorUsername(username);
    }
}