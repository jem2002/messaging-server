package tcpsocketserver;

import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import executor.ThreadPoolManager;
import handler.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
import pool.PooledClientConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPSocketServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketServer.class);

    private final int port;
    private final IConnectionPool pool;
    private final ThreadPoolManager threadPool;
    private final MainRouter router;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private final BroadcastManager broadcastManager;

    // 1. NUEVOS ATRIBUTOS
    private final TransferManager transferManager;
    private final DocumentManager documentManager;

    // 2. PEDIRLOS EN EL CONSTRUCTOR
    public TCPSocketServer(int port, IConnectionPool pool, ThreadPoolManager threadPool, MainRouter router,
                           BroadcastManager broadcastManager, TransferManager transferManager, DocumentManager documentManager) {
        this.port = port;
        this.pool = pool;
        this.threadPool = threadPool;
        this.router = router;
        this.running = true;
        this.broadcastManager = broadcastManager;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
    }

    public void stopServer() {
        this.running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error cerrando tcpsocketserver.TCPSocketServer", e);
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("tcpsocketserver.TCPSocketServer escuchando en el puerto TCP: {}", port);

            while (running) {
                // 1. Aceptar conexión (Bloqueante)
                Socket clientSocket = serverSocket.accept();
                logger.debug("Nueva conexión TCP entrante...");

                // 2. Adquirir conexión reciclable del pool
                PooledClientConnection pooledConnection = pool.acquire();

                if (pooledConnection == null) {
                    logger.warn("Rechazando conexión: Pool agotado.");
                    clientSocket.close();
                    continue;
                }

                // 3. Configurar el socket y delegar al Handler en el ThreadPool
                pooledConnection.setSocket(clientSocket);

                // 4. PASAR LAS NUEVAS DEPENDENCIAS AL HANDLER
                ClientHandler handler = new ClientHandler(pooledConnection, pool, router, this.broadcastManager, this.transferManager, this.documentManager);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error en el bucle principal de tcpsocketserver.TCPSocketServer", e);
            } else {
                logger.info("tcpsocketserver.TCPSocketServer detenido correctamente.");
            }
        } catch (Exception e) {
            logger.error("Error configurando la conexión del cliente.", e);
        }
    }
}