package tcpsocketserver;

import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import executor.ThreadPoolManager;
import handler.ClientHandler;
import handler.FileTransferHandler;
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
                logger.debug("Nueva conexión TCP entrante desde {}", clientSocket.getRemoteSocketAddress());

                // 2. Triage: Leer la primera línea para saber qué tipo de conexión es
                // Usamos un pequeño timeout para que no bloquee el server si alguien conecta y no manda nada
                clientSocket.setSoTimeout(5000); 
                String primeraLinea;
                try {
                    primeraLinea = leerLinea(clientSocket.getInputStream());
                } catch (Exception e) {
                    logger.warn("Error leyendo primera línea de {}, cerrando socket.", clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    continue;
                }
                clientSocket.setSoTimeout(0); // Quitar timeout para la vida de la conexión

                if (primeraLinea == null || primeraLinea.isEmpty()) {
                    clientSocket.close();
                    continue;
                }

                if (primeraLinea.startsWith("{")) {
                    // =============== MODO CONTROL (JSON) ===============
                    logger.info("Detectada conexión de CONTROL desde {}", clientSocket.getRemoteSocketAddress());
                    
                    PooledClientConnection pooledConnection = pool.acquire();
                    if (pooledConnection == null) {
                        logger.warn("Rechazando conexión de control: Pool agotado.");
                        clientSocket.close();
                        continue;
                    }

                    pooledConnection.setSocket(clientSocket);
                    // Usamos el handler de control (en el pool)
                    ClientHandler handler = new ClientHandler(pooledConnection, pool, router, this.broadcastManager, this.transferManager, this.documentManager, primeraLinea);
                    threadPool.execute(handler);

                } else {
                    // =============== MODO ARCHIVO (TOKEN) ===============
                    logger.info("Detectada conexión de ARCHIVO (Token: {}) desde {}", primeraLinea, clientSocket.getRemoteSocketAddress());
                    
                    // Para archivos usamos hilos del sistema (no del pool) como se solicitó
                    FileTransferHandler fileHandler = new FileTransferHandler(clientSocket, primeraLinea, transferManager, documentManager, router, broadcastManager);
                    new Thread(fileHandler, "FileTransfer-" + primeraLinea.substring(0, Math.min(8, primeraLinea.length()))).start();
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error en el bucle principal de TCPSocketServer", e);
            } else {
                logger.info("TCPSocketServer detenido correctamente.");
            }
        } catch (Exception e) {
            logger.error("Error configurando la conexión del cliente.", e);
        }
    }

    private String leerLinea(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') baos.write(c);
        }
        if (c == -1 && baos.size() == 0) return null;
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8.name()).trim();
    }
}