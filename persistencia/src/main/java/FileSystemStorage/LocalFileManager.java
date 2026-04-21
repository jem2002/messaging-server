package FileSystemStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Clase encargada de persistir los binarios en el disco físico.
 */
public class LocalFileManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileManager.class);

    private final Path originalDir = Paths.get("./storage/original");
    private final Path encryptedDir = Paths.get("./storage/encrypted");

    public LocalFileManager() {
        crearDirectoriosSiNoExisten();
    }

    private void crearDirectoriosSiNoExisten() {
        try {
            Files.createDirectories(originalDir);
            Files.createDirectories(encryptedDir);
        } catch (IOException e) {
            logger.error("Error crítico al inicializar directorios de almacenamiento", e);
            throw new RuntimeException("No se pueden inicializar los directorios de disco", e);
        }
    }

    public String guardarOriginal(InputStream inputStream, String extension) throws IOException {
        String fileName = generarNombreUnico(extension);
        Path targetLocation = originalDir.resolve(fileName);

        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        logger.debug("Archivo original guardado en: {}", targetLocation.toAbsolutePath());

        return targetLocation.toAbsolutePath().toString();
    }

    public String guardarCifrado(InputStream inputStream, String extension) throws IOException {
        String fileName = generarNombreUnico(extension) + ".enc";
        Path targetLocation = encryptedDir.resolve(fileName);

        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        logger.debug("Archivo cifrado guardado en: {}", targetLocation.toAbsolutePath());

        return targetLocation.toAbsolutePath().toString();
    }

    public InputStream leerArchivo(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("El archivo no existe en la ruta: " + filePath);
        }
        return new BufferedInputStream(new FileInputStream(file));
    }

    private String generarNombreUnico(String extension) {
        String ext = (extension != null && !extension.isEmpty()) ? extension : "";
        if (!ext.isEmpty() && !ext.startsWith(".")) {
            ext = "." + ext;
        }
        return UUID.randomUUID().toString() + ext;
    }
}