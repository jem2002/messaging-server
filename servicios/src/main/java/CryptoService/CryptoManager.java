package CryptoService;

import DocumentService.CryptoResult;
import EncryptionUtils.EncryptionUtils;
import EncryptionUtils.IEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;

public class CryptoManager {
    private static final Logger logger = LoggerFactory.getLogger(CryptoManager.class);

    private final IEncryptionUtils cryptoUtils;
    private static SecretKey secretKey;

    public CryptoManager() {
        this.cryptoUtils = new EncryptionUtils();
        try {
            if (secretKey == null) {
                secretKey = cryptoUtils.generateKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando motor AES desde Shared", e);
        }
    }

    public CryptoResult procesarArchivo(String originalPath, String targetEncryptedDir) throws Exception {
        logger.info("Iniciando Hash y Cifrado en un solo pase para: {}", originalPath);

        MessageDigest digest = cryptoUtils.getHashDigest();
        Cipher cipher = cryptoUtils.getEncryptionCipher(secretKey);

        // Usamos java.nio.file.Path para las rutas
        Path sourcePath = Paths.get(originalPath);
        Path finalEncryptedPath = Paths.get(targetEncryptedDir, UUID.randomUUID().toString() + ".enc");

        // Streaming con NIO (Más rápido y moderno que FileInputStream/FileOutputStream)
        try (InputStream is = Files.newInputStream(sourcePath);
             DigestInputStream dis = new DigestInputStream(is, digest);
             OutputStream os = Files.newOutputStream(finalEncryptedPath);
             CipherOutputStream cos = new CipherOutputStream(os, cipher)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        String hashResult = cryptoUtils.bytesToHex(digest.digest());

        logger.debug("Procesamiento exitoso. Hash: {}", hashResult);
        return new CryptoResult(hashResult, finalEncryptedPath.toAbsolutePath().toString());
    }
}