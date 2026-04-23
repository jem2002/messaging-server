package LogService;

import MySqlRepository.MySqlDao;

public class LogManager {
    private final MySqlDao dao;

    public LogManager(MySqlDao dao) {
        this.dao = dao;
    }

    public void registrarAccion(Long docId, long senderId, String action, String status, String details) {
        dao.registrarLog(docId, senderId, null, action, "TCP", status, details);
    }

    public java.util.List<java.util.Map<String, String>> listarLogs() {
        try {
            return dao.listarLogs();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }
}