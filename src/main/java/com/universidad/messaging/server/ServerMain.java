package com.universidad.messaging.server;

import com.universidad.messaging.server.network.TCPServer;
import com.universidad.messaging.server.network.UDPServer;
import com.universidad.messaging.server.pool.ConnectionPool;
import com.universidad.messaging.server.repository.pool.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    private static TCPServer tcpServer;
    private static UDPServer udpServer;

    public static void main(String[] args) {
        logger.info("Iniciando Messaging Server...");

        // 1. Leer configuración
        Properties config = loadConfiguration();
        int port = Integer.parseInt(config.getProperty("server.port", "8080"));
        String protocol = config.getProperty("server.protocol", "TCP").toUpperCase();

        // 2. Inicializar el Singleton de base de datos
        try {
            // Llama a getInstance para asegurar que el pool de base de datos se inicializa en el arranque
            DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();
            logger.info("Conexión a la base de datos inicializada correctamente.");
        } catch (Exception e) {
            logger.error("Error crítico al inicializar la base de datos. Abortando inicio.", e);
            System.exit(1);
        }

        // 3. Inicializar los directorios de storage en disco (original y cifrado)
        try {
            initStorageDirectories(config);
            logger.info("Directorios de almacenamiento verificados e inicializados.");
        } catch (Exception e) {
            logger.error("Error crítico al inicializar los directorios de disco. Abortando inicio.", e);
            System.exit(1);
        }

        // 4. Inicializar el ConnectionPool (Object Pool patrón para sockets de clientes)
        int maxPoolSize = Integer.parseInt(config.getProperty("server.pool.size", "100"));
        ConnectionPool connectionPool = new ConnectionPool(maxPoolSize);
        logger.info("Pool de conexiones inicializado con tamaño máximo: {}", maxPoolSize);

        // 5. Instanciar y arrancar el TCPServer o UDPServer
        if ("TCP".equals(protocol)) {
            tcpServer = new TCPServer(port, connectionPool);
            tcpServer.start();
        } else if ("UDP".equals(protocol)) {
            udpServer = new UDPServer(port, connectionPool);
            udpServer.start();
        } else {
            logger.error("Protocolo configurado no está soportado: {}. Use TCP o UDP.", protocol);
            System.exit(1);
        }

        // 6. Cierre Limpio (Graceful Shutdown)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("=========================================");
            logger.info("Iniciando secuencia de apagado seguro (Graceful Shutdown)...");
            
            // Detener servidores de red
            if (tcpServer != null) {
                tcpServer.stop();
                logger.info("TCPServer detenido.");
            }
            if (udpServer != null) {
                udpServer.stop();
                logger.info("UDPServer detenido.");
            }
            
            // Apagar procesadores de requests (pool de sockets)
            if (connectionPool != null) {
                connectionPool.shutdown();
                logger.info("ConnectionPool detenido.");
            }
            
            // Cerrar la conexión a base de datos de forma limpia
            try {
                DatabaseConnectionManager.getInstance().close();
                logger.info("Conexión a la base de datos cerrada de forma segura.");
            } catch (Exception e) {
                logger.error("Error al cerrar la base de datos durante el apagado", e);
            }
            
            logger.info("Servidor apagado de forma segura.");
            logger.info("=========================================");
        }, "Shutdown-Hook-Thread"));

        // Prevenir que la app termine prematuramente
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.warn("El hilo principal fue interrumpido.", e);
            Thread.currentThread().interrupt();
        }
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream is = ServerMain.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                properties.load(is);
            } else {
                logger.warn("Archivo 'config.properties' no encontrado en el classpath. Usando valores por defecto.");
            }
        } catch (IOException e) {
            logger.error("Error al leer 'config.properties'. Usando valores por defecto.", e);
        }
        return properties;
    }

    private static void initStorageDirectories(Properties config) throws IOException {
        String baseDir = config.getProperty("storage.dir", "./storage");
        File dirOriginal = new File(baseDir + File.separator + "original");
        File dirEncrypted = new File(baseDir + File.separator + "encrypted");

        if (!dirOriginal.exists() && !dirOriginal.mkdirs()) {
            throw new IOException("No se pudo crear el directorio de storage: " + dirOriginal.getAbsolutePath());
        }
        if (!dirEncrypted.exists() && !dirEncrypted.mkdirs()) {
            throw new IOException("No se pudo crear el directorio de storage cifrado: " + dirEncrypted.getAbsolutePath());
        }
    }
}
