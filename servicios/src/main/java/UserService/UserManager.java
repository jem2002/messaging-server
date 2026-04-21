package UserService;

import MySqlRepository.MySqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public void desconectarPorCaidaDeRed(String ipAddress, int port) {
        try {
            dao.cerrarSesionPorIpYPuerto(ipAddress, port);
            logger.info("Estado actualizado: Sesión cerrada en BD para {}:{}", ipAddress, port);
        } catch (Exception e) {
            logger.error("Error al cerrar sesión por caída de red", e);
        }
    }
}