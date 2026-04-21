package DocumentService;

public class CryptoResult {
    public final String hashValue;
    public final String encryptedPath;
    public final String algorithmHash = "SHA256";
    public final String algorithmCipher = "AES256";

    public CryptoResult(String hashValue, String encryptedPath) {
        this.hashValue = hashValue;
        this.encryptedPath = encryptedPath;
    }
}