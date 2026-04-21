package FileStorageService;

import FileSystemStorage.LocalFileManager;

import java.io.InputStream;

public class StorageManager {
    private final LocalFileManager fileManager;

    public StorageManager() {
        this.fileManager = new LocalFileManager();
    }

    public String guardarStreamOriginal(InputStream is, String extension) throws Exception {
        return fileManager.guardarOriginal(is, extension);
    }

    public String obtenerRutaDirectorioCifrados() {
        return "./storage/encrypted";
    }
}