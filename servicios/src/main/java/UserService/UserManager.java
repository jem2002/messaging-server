package UserService;

import MySqlRepository.MySqlDao;

public class UserManager {
    private final MySqlDao dao;

    public UserManager(MySqlDao dao) {
        this.dao = dao;
    }

    public long conectarUsuario(String username, String ipAddress) throws Exception {
        return dao.obtenerORegistrarUsuario(username, ipAddress);
    }
}