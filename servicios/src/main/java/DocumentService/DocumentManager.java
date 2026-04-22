package DocumentService;

import CryptoService.CryptoManager;
import FileStorageService.StorageManager;
import LogService.LogManager;
import MySqlRepository.MySqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentManager {
    private static final Logger logger = LoggerFactory.getLogger(DocumentManager.class);

    private final StorageManager storageManager;
    private final CryptoManager cryptoManager;
    private final MySqlDao dao;
    private final LogManager logManager;

    public DocumentManager(StorageManager storageManager, CryptoManager cryptoManager, MySqlDao dao, LogManager logManager) {
        this.storageManager = storageManager;
        this.cryptoManager = cryptoManager;
        this.dao = dao;
        this.logManager = logManager;
    }
// Asegúrate de tener el import de List, Map y ArrayList arriba

    public List<Map<String, String>> obtenerDocumentosDisponibles() {
        try {
            return dao.listarDocumentosDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de documentos de la BD", e);
            return new ArrayList<>(); // Retorna lista vacía en vez de romper el servidor
        }
    }
    public boolean procesarRecepcionDocumento(InputStream redStream, String nombre, long sizeBytes,
                                              String extension, String mimeType, long ownerUserId, String ownerIp) {
        Long docId = null;
        try {
            // 1. Delegar I/O a filestorageservice
            logger.info("1. Guardando original en disco...");
            String originalPath = storageManager.guardarStreamOriginal(redStream, extension);

            // 2. Delegar a cryptoservice
            logger.info("2. Generando Hash y cifrando...");
            String encryptedDir = storageManager.obtenerRutaDirectorioCifrados();
            CryptoResult cryptoResult = cryptoManager.procesarArchivo(originalPath, encryptedDir);

            // 3. Persistencia en MySQL (Módulo inferior)
            logger.info("3. Guardando metadatos en MySQL...");
            docId = dao.registrarDocumento(nombre, sizeBytes, extension, mimeType, "FILE", originalPath, ownerUserId, ownerIp);
            dao.registrarHashDocumento(docId, cryptoResult.algorithmHash, cryptoResult.hashValue);
            dao.registrarCifradoDocumento(docId, cryptoResult.algorithmCipher, cryptoResult.encryptedPath, "KEY_REF_01");

            // 4. Delegar a logservice
            logManager.registrarAccion(docId, ownerUserId, "RECEIVE", "SUCCESS", "Archivo procesado y cifrado.");

            return true;

        } catch (Exception e) {
            logger.error("Error crítico procesando el documento.", e);
            if (ownerUserId > 0) {
                logManager.registrarAccion(docId, ownerUserId, "RECEIVE", "FAILED", e.getMessage());
            }
            return false;
        }
    }
}