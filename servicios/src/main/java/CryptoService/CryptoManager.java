package CryptoService;

import DocumentService.CryptoResult;
import EncryptionUtils.EncryptionUtils;
import EncryptionUtils.IEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;

public class CryptoManager {
    private static final Logger logger = LoggerFactory.getLogger(CryptoManager.class);

    private final IEncryptionUtils cryptoUtils;
    private static SecretKey secretKey;

    public CryptoManager() {
        // Inyectamos la utilería compartida
        this.cryptoUtils = new EncryptionUtils();

        try {
            if (secretKey == null) {
                secretKey = cryptoUtils.generateKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando motor AES desde Shared", e);
        }
    }

    public CryptoResult procesarArchivo(String originalPath, String targetEncryptedPath) throws Exception {
        logger.info("Iniciando Hash y Cifrado usando utilería Shared para: {}", originalPath);

        // Obtenemos los motores desde el módulo Shared
        MessageDigest digest = cryptoUtils.getHashDigest();
        Cipher cipher = cryptoUtils.getEncryptionCipher(secretKey);

        String finalEncryptedPath = targetEncryptedPath + File.separator + UUID.randomUUID().toString() + ".enc";

        try (FileInputStream fis = new FileInputStream(originalPath);
             DigestInputStream dis = new DigestInputStream(fis, digest);
             FileOutputStream fos = new FileOutputStream(finalEncryptedPath);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        // Usamos la función transversal para el Hex
        String hashResult = cryptoUtils.bytesToHex(digest.digest());

        return new CryptoResult(hashResult, finalEncryptedPath);
    }
}