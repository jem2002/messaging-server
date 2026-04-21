package UserService;

import MySqlRepository.MySqlDao;

public class UserManager {
    private final MySqlDao dao;

    public UserManager(MySqlDao dao) {
        this.dao = dao;
    }

    public long conectarUsuario(String username, String ipAddress, int port) throws Exception {
        // 1. Obtener o crear el usuario
        long userId = dao.obtenerORegistrarUsuario(username, ipAddress);

        // 2. Registrar la sesión pasando el puerto
        dao.registrarSesionActiva(userId, ipAddress, port, "TCP");

        return userId;
    }
}