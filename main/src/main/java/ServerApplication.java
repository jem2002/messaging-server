import CryptoService.CryptoManager;
import DocumentService.DocumentManager;
import FileStorageService.StorageManager;
import LogService.LogManager;
import MySqlRepository.DatabaseInitializer;
import MySqlRepository.MySqlDao;
import RequestRouter.MainRouter;
import UserService.UserManager;
import api.ServerAdminAPI;
import config.ServerConfig;
import console.InteractiveConsole;
import executor.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.ConnectionPoolManager;
import protocolSelector.ProtocolSelector;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        logger.info("Arrancando Messaging Server...");

        try {
            // 1. Configuración
            ServerConfig config = new ServerConfig();

            // 2. Módulo Persistencia
          //  DatabaseInitializer.initializeSchema();
            MySqlDao dao = new MySqlDao();
            dao.limpiarConexionesMuertas();

            // 3. Módulo Servicios
            UserManager userManager = new UserManager(dao);
            StorageManager storageManager = new StorageManager();
            CryptoManager cryptoManager = new CryptoManager();
            LogManager logManager = new LogManager(dao);
            DocumentManager documentManager = new DocumentManager(storageManager, cryptoManager, dao, logManager);

            // 4. Módulo Protocolo
            MainRouter router = new MainRouter(userManager, documentManager, logManager);

            // 5. Módulo Gestión de Conexiones
            int maxConnections = config.getMaxConnections();
            ConnectionPoolManager pool = new ConnectionPoolManager(maxConnections);
            ThreadPoolManager threadPool = new ThreadPoolManager(maxConnections);

            // 6. Módulo Red
            ProtocolSelector networkServer = new ProtocolSelector();
            networkServer.iniciarServidor(
                    config.getProtocol(),
                    config.getPort(),
                    pool,
                    threadPool,
                    router
            );

            // 7. Interfaces Expuestas y Consola Administrativa
            ServerAdminAPI adminAPI = new ServerAdminAPI(dao);
            InteractiveConsole console = new InteractiveConsole(adminAPI, networkServer);

            // Arrancamos la consola en el hilo principal
            console.run();

        } catch (Exception e) {
            logger.error("Error crítico durante el arranque del servidor.", e);
            System.exit(1);
        }
    }
}