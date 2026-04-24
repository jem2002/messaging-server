package DocumentService;

import CryptoService.CryptoManager;
import FileStorageService.StorageManager;
import FileSystemStorage.LocalFileManager;
import LogService.LogManager;
import MySqlRepository.MySqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentManager {
    private static final Logger logger = LoggerFactory.getLogger(DocumentManager.class);

    private final LocalFileManager fileManager;
    private final CryptoManager cryptoManager;
    private final MySqlDao dao;
    private final LogManager logManager;

    public DocumentManager(LocalFileManager fileManager, CryptoManager cryptoManager, MySqlDao dao, LogManager logManager) {
        this.fileManager = fileManager;
        this.cryptoManager = cryptoManager;
        this.dao = dao;
        this.logManager = logManager;
    }
    // 1. Método puente para buscar los detalles en la BD (Para el Router)
    public Map<String, String> obtenerDetallesDescarga(long documentId) throws Exception {
        return dao.obtenerDetallesDescarga(documentId);
    }

    // 2. Método puente para desencriptar y enviar (Para el Handler - DESCARGA NORMAL)
    public void enviarDocumentoAlCliente(String encryptedPath, java.io.OutputStream out) throws Exception {
        cryptoManager.desencriptarYEnviarAlSocket(encryptedPath, out);
    }

    public void enviarDocumentoOriginal(long documentId, OutputStream out) throws Exception {
        String path = dao.obtenerRutaOriginal(documentId);
        Files.copy(Paths.get(path), out);
        out.flush();
    }

    public void enviarDocumentoEncriptado(long documentId, OutputStream out) throws Exception {
        Map<String, String> detalles = dao.obtenerDetallesDescarga(documentId);
        String path = detalles.get("ruta_cifrada");
        Files.copy(Paths.get(path), out);
        out.flush();
    }

    public void enviarDocumentoHash(long documentId, OutputStream out) throws Exception {
        String hash = dao.obtenerHashValue(documentId);
        out.write(hash.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
    public List<Map<String, String>> obtenerDocumentosDisponibles() {
        try {
            return dao.listarDocumentosDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de documentos de la BD", e);
            return new ArrayList<>();
        }
    }

    public List<Map<String, String>> obtenerArchivosDisponibles() {
        try {
            return dao.listarArchivosDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de archivos de la BD", e);
            return new ArrayList<>();
        }
    }

    public List<Map<String, String>> obtenerMensajesDisponibles() {
        try {
            return dao.listarMensajesDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de mensajes de la BD", e);
            return new ArrayList<>();
        }
    }

    public boolean procesarRecepcionDocumento(InputStream redStream, String nombre, long sizeBytes,
                                              String extension, String mimeType, long ownerUserId, String ownerIp) {
        Long docId = null;
        try {
            // 1. Delegar I/O a tu LocalFileManager (Streaming puro al disco)
            logger.info("1. Guardando original en disco...");
            String originalPath = fileManager.guardarOriginal(redStream, extension, sizeBytes);

            // 2. Generar Hash y Cifrar en un solo pase (Single-Pass Streaming)
            logger.info("2. Generando Hash y cifrando...");
            String encryptedDir = "./storage/encrypted"; // Directorio destino
            CryptoResult cryptoResult = cryptoManager.procesarArchivo(originalPath, encryptedDir);

            // 3. Persistencia Transaccional en MySQL
            logger.info("3. Guardando metadatos en MySQL...");

            // a) Insertar en tabla documents (Retorna el ID autogenerado)
            docId = dao.registrarDocumento(nombre, sizeBytes, extension, mimeType, "FILE", originalPath, ownerUserId, ownerIp);

            // b) Insertar en tabla document_hashes (Asumimos SHA256 según tu config)
            dao.registrarHashDocumento(docId, "SHA256", cryptoResult.getHashResult());

            // c) Insertar en tabla encrypted_documents (Asumimos AES256)
            dao.registrarCifradoDocumento(docId, "AES256", cryptoResult.getFinalEncryptedPath(), "SERVER_STATIC_KEY");

            // 4. Registrar en la bitácora de auditoría (logs)
            logManager.registrarAccion(docId, ownerUserId, "RECEIVE", "SUCCESS", "Archivo guardado, hasheado y cifrado.");

            logger.info("¡Documento procesado al 100%! ID asignado: {}", docId);
            return true;

        } catch (Exception e) {
            logger.error("Error crítico procesando el documento.", e);
            if (ownerUserId > 0) {
                logManager.registrarAccion(docId, ownerUserId, "RECEIVE", "FAILED", "Error: " + e.getMessage());
            }
            return false;
        }
    }
}