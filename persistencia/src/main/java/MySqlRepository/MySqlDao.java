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

    public long registrarDocumento(String nombre, long sizeBytes, String extension, String mimeType,
                                   String docType, String originalPath, long ownerUserId, String ownerIp) throws SQLException {
        String sql = "INSERT INTO documents (name, size_bytes, extension, mime_type, doc_type, original_path, owner_user_id, owner_ip) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nombre);
            stmt.setLong(2, sizeBytes);
            stmt.setString(3, extension);
            stmt.setString(4, mimeType);
            stmt.setString(5, docType);
            stmt.setString(6, originalPath);
            stmt.setLong(7, ownerUserId);
            stmt.setString(8, ownerIp);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Fallo al obtener el ID del documento insertado.");
                }
            }
        }
    }

    public void registrarHashDocumento(long documentId, String algorithm, String hashValue) throws SQLException {
        String sql = "INSERT INTO document_hashes (document_id, algorithm, hash_value) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            stmt.setString(2, algorithm);
            stmt.setString(3, hashValue);
            stmt.executeUpdate();
        }
    }

    public void registrarCifradoDocumento(long documentId, String algorithm, String encryptedPath, String keyReference) throws SQLException {
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
        String sql = "INSERT INTO logs (document_id, sender_user_id, receiver_user_id, action, protocol, status, details) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (documentId != null) stmt.setLong(1, documentId); else stmt.setNull(1, Types.BIGINT);
            stmt.setLong(2, senderId);
            if (receiverId != null) stmt.setLong(3, receiverId); else stmt.setNull(3, Types.BIGINT);
            stmt.setString(4, action);
            stmt.setString(5, protocol);
            stmt.setString(6, status);
            stmt.setString(7, details);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al registrar log en auditoría: {} - Detalles: {}", action, details, e);
        }
    }

    public List<Map<String, String>> listarDocumentosDisponibles() throws SQLException {
        List<Map<String, String>> documentos = new ArrayList<>();
        String sql = "SELECT d.id, d.name, d.size_bytes, d.extension, u.username as owner " +
                "FROM documents d INNER JOIN users u ON d.owner_user_id = u.id " +
                "ORDER BY d.created_at DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> doc = new HashMap<>();
                doc.put("id", String.valueOf(rs.getLong("id")));
                doc.put("nombre", rs.getString("name"));
                doc.put("tamaño", String.valueOf(rs.getLong("size_bytes")));
                doc.put("extension", rs.getString("extension"));
                doc.put("propietario", rs.getString("owner"));
                documentos.add(doc);
            }
        }
        return documentos;
    }
}