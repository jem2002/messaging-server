package MySqlRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Objeto de Acceso a Datos (DAO) unificado para operaciones en MySQL.
 */
public class MySqlDao {
    private static final Logger logger = LoggerFactory.getLogger(MySqlDao.class);
    private final DatabaseConnectionManager dbManager;

    public MySqlDao() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    public long obtenerORegistrarUsuario(String username, String ipAddress) throws SQLException {
        String selectSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, ip_address) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, username);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, ipAddress);
                insertStmt.executeUpdate();

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Fallo al obtener el ID del usuario insertado.");
                    }
                }
            }
        }
    }

    // 1. Guardar el Documento principal
    public long registrarDocumento(String name, long sizeBytes, String extension, String mimeType,
                                   String docType, String originalPath, long ownerUserId, String ownerIp) throws Exception {
        String sql = "INSERT INTO documents (name, size_bytes, extension, mime_type, doc_type, original_path, owner_user_id, owner_ip) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setLong(2, sizeBytes);
            stmt.setString(3, extension);
            stmt.setString(4, mimeType);
            stmt.setString(5, docType);
            stmt.setString(6, originalPath);
            stmt.setLong(7, ownerUserId);
            stmt.setString(8, ownerIp);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new Exception("No se pudo obtener el ID del documento generado.");
            }
        }
    }

    // 2. Guardar el Hash
    public void registrarHashDocumento(long documentId, String algorithm, String hashValue) throws Exception {
        String sql = "INSERT INTO document_hashes (document_id, algorithm, hash_value) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            stmt.setString(2, algorithm);
            stmt.setString(3, hashValue);
            stmt.executeUpdate();
        }
    }
    // 3. Guardar el Cifrado
    public void registrarCifradoDocumento(long documentId, String algorithm, String encryptedPath, String keyReference) throws Exception {
        String sql = "INSERT INTO encrypted_documents (document_id, algorithm, encrypted_path, key_reference) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            stmt.setString(2, algorithm);
            stmt.setString(3, encryptedPath);
            stmt.setString(4, keyReference);
            stmt.executeUpdate();
        }
    }

    public void registrarLog(Long documentId, long senderId, Long receiverId, String action, String protocol, String status, String details) {
        String sql = "INSERT INTO logs (document_id, sender_user_id, receiver_user_id, action, protocol, status, details, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (documentId != null) stmt.setLong(1, documentId); else stmt.setNull(1, Types.BIGINT);
            stmt.setLong(2, senderId);
            if (receiverId != null) stmt.setLong(3, receiverId); else stmt.setNull(3, Types.BIGINT);
            stmt.setString(4, action);
            stmt.setString(5, protocol);
            stmt.setString(6, status);
            stmt.setString(7, details);

            // Tiempo de Colombia (UTC-5)
            java.time.ZonedDateTime colombiaTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Bogota"));
            stmt.setTimestamp(8, java.sql.Timestamp.valueOf(colombiaTime.toLocalDateTime()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al registrar log en auditoría: {} - Detalles: {}", action, details, e);
        }
    }

    public List<Map<String, String>> listarArchivosDisponibles() throws Exception {
        return listarDocumentosFiltrados(false);
    }

    public List<Map<String, String>> listarMensajesDisponibles() throws Exception {
        return listarDocumentosFiltrados(true);
    }

    private List<Map<String, String>> listarDocumentosFiltrados(boolean soloTexto) throws Exception {
        List<Map<String, String>> documentos = new ArrayList<>();
        String sql = "SELECT d.id, d.name, d.size_bytes, d.extension, d.original_path, u.username, u.ip_address " +
                "FROM documents d " +
                "JOIN users u ON d.owner_user_id = u.id " +
                (soloTexto ? "WHERE d.extension = '.txt' " : "WHERE d.extension != '.txt' ") +
                "ORDER BY d.id DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> doc = new HashMap<>();
                doc.put("id", String.valueOf(rs.getLong("id")));
                doc.put("nombre", rs.getString("name"));
                doc.put("tamano_bytes", String.valueOf(rs.getLong("size_bytes")));
                doc.put("extension", rs.getString("extension"));
                doc.put("ruta_original", rs.getString("original_path"));
                String propietario = rs.getString("username") + " (" + rs.getString("ip_address") + ")";
                doc.put("propietario", propietario);
                documentos.add(doc);
            }
        }
        return documentos;
    }

    public List<Map<String, String>> listarDocumentosDisponibles() throws Exception {
        return listarDocumentosFiltradosGeneral();
    }

    private List<Map<String, String>> listarDocumentosFiltradosGeneral() throws Exception {
        List<Map<String, String>> documentos = new ArrayList<>();
        String sql = "SELECT d.id, d.name, d.size_bytes, d.extension, u.username, u.ip_address " +
                "FROM documents d " +
                "JOIN users u ON d.owner_user_id = u.id " +
                "ORDER BY d.id DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> doc = new HashMap<>();
                doc.put("id", String.valueOf(rs.getLong("id")));
                doc.put("nombre", rs.getString("name"));
                doc.put("tamano_bytes", String.valueOf(rs.getLong("size_bytes")));
                doc.put("extension", rs.getString("extension"));
                String propietario = rs.getString("username") + " (" + rs.getString("ip_address") + ")";
                doc.put("propietario", propietario);
                documentos.add(doc);
            }
        }
        return documentos;
    }

    public List<Map<String, String>> listarUsuariosRegistrados() throws SQLException {
        List<Map<String, String>> usuarios = new ArrayList<>();
        String sql = "SELECT id, username, ip_address, created_at FROM users ORDER BY id ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("id", String.valueOf(rs.getLong("id")));
                user.put("username", rs.getString("username"));
                user.put("ip", rs.getString("ip_address"));
                user.put("fecha", rs.getString("created_at"));
                usuarios.add(user);
            }
        }
        return usuarios;
    }

    public long registrarSesionActiva(long userId, String ipAddress, int port, String protocol) throws SQLException {
        // Agregamos 'port' a la consulta
        String sql = "INSERT INTO client_connections (user_id, ip_address, port, protocol, is_active, connected_at) VALUES (?, ?, ?, ?, TRUE, NOW())";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, userId);
            stmt.setString(2, ipAddress);
            stmt.setInt(3, port);         // Insertamos el puerto
            stmt.setString(4, protocol);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getLong(1) : -1;
        }
    }
    public void cerrarSesionActiva(long sessionId) throws SQLException {
        String sql = "UPDATE client_connections SET is_active = FALSE, disconnected_at = NOW() WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            stmt.executeUpdate();
        }
    }
    public long cerrarSesionPorIpYPuerto(String ipAddress, int port) throws Exception {
        String selectSql = "SELECT user_id FROM client_connections WHERE ip_address = ? AND port = ? AND is_active = TRUE";
        String updateSql = "UPDATE client_connections SET is_active = FALSE, disconnected_at = NOW() WHERE ip_address = ? AND port = ? AND is_active = TRUE";

        long userId = 0;
        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, ipAddress);
                selectStmt.setInt(2, port);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong("user_id");
                    }
                }
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, ipAddress);
                updateStmt.setInt(2, port);
                updateStmt.executeUpdate();
            }
        }
        return userId;
    }
    public List<Map<String, String>> listarClientesActivos() throws Exception {
        List<Map<String, String>> activos = new ArrayList<>();
        // JOIN entre usuarios y sus conexiones activas
        String sql = "SELECT u.username, c.ip_address, c.connected_at " +
                "FROM users u " +
                "JOIN client_connections c ON u.id = c.user_id " +
                "WHERE c.is_active = TRUE";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> client = new HashMap<>();
                client.put("username", rs.getString("username"));
                client.put("ip", rs.getString("ip_address"));
                client.put("fecha_inicio", rs.getString("connected_at"));
                activos.add(client);
            }
        }
        return activos;
    }
    public void limpiarConexionesMuertas() {
        String sql = "UPDATE client_connections SET is_active = FALSE, disconnected_at = NOW() WHERE is_active = TRUE";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int limpiados = stmt.executeUpdate();
            if (limpiados > 0) {
                logger.info("Limpieza de inicio: {} conexiones 'muertas' fueron cerradas.", limpiados);
            }
        } catch (SQLException e) {
            logger.error("Error limpiando conexiones muertas", e);
        }
    }

    public long obtenerIdUsuarioPorUsername(String username) throws Exception {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new Exception("El usuario " + username + " no existe en la base de datos.");
                }
            }
        }
    }

    public String obtenerRutaOriginal(long documentId) throws Exception {
        String sql = "SELECT original_path FROM documents WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("original_path");
                else throw new Exception("Documento no encontrado.");
            }
        }
    }

    public String obtenerHashValue(long documentId) throws Exception {
        String sql = "SELECT hash_value FROM document_hashes WHERE document_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("hash_value");
                else throw new Exception("Hash no encontrado.");
            }
        }
    }

    public Map<String, String> obtenerDetallesDescarga(long documentId) throws Exception {
        String sql = "SELECT d.name, d.size_bytes, e.encrypted_path " +
                "FROM documents d " +
                "JOIN encrypted_documents e ON d.id = e.document_id " +
                "WHERE d.id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> detalles = new HashMap<>();
                    detalles.put("nombre", rs.getString("name"));
                    detalles.put("tamano", String.valueOf(rs.getLong("size_bytes")));
                    detalles.put("ruta_cifrada", rs.getString("encrypted_path"));
                    return detalles;
                } else {
                    throw new Exception("Documento no encontrado o no tiene archivo físico.");
                }
            }
        }
    }

    public List<Map<String, String>> listarLogs() throws Exception {
        List<Map<String, String>> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.document_id, u1.username as sender, " +
                "l.action, l.protocol, l.status, l.details, l.timestamp " +
                "FROM logs l " +
                "LEFT JOIN users u1 ON l.sender_user_id = u1.id " +
                "ORDER BY l.id DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> log = new HashMap<>();
                log.put("id", String.valueOf(rs.getLong("id")));
                log.put("document_id", rs.getString("document_id") != null ? rs.getString("document_id") : "");
                log.put("sender", rs.getString("sender") != null ? rs.getString("sender") : "");
                log.put("action", rs.getString("action"));
                log.put("protocol", rs.getString("protocol"));
                log.put("status", rs.getString("status"));
                log.put("details", rs.getString("details"));
                log.put("timestamp", rs.getString("timestamp"));
                logs.add(log);
            }
        }
        return logs;
    }
}