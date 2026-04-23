package RequestRouter;

public class TransferTicket {
    public final String token;
    public final String filename;
    public final long sizeBytes;
    public final String extension;
    public final String mimeType;
    public final long ownerUserId;
    public final String ownerIp;

    public TransferTicket(String token, String filename, long sizeBytes, String extension, String mimeType, long ownerUserId, String ownerIp) {
        this.token = token;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.extension = extension;
        this.mimeType = mimeType;
        this.ownerUserId = ownerUserId;
        this.ownerIp = ownerIp;
    }
}