package com.universidad.messaging.server.network;

import com.universidad.messaging.server.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TCPServer.class);
    private final int port;
    private final ConnectionPool connectionPool;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public TCPServer(int port, ConnectionPool connectionPool) {
        this.port = port;
        this.connectionPool = connectionPool;
    }

    public void start() {
        this.running = true;
        new Thread(this, "TCPServerThread").start();
    }

    public void stop() {
        this.running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error al cerrar ServerSocket TCP", e);
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("TCP Server iniciado en el puerto {}", port);

            while (running) {
                try {
                    // PUNTO CRÍTICO DE ARQUITECTURA: accept() es bloqueante.
                    Socket clientSocket = serverSocket.accept();
                    logger.debug("Nueva conexión TCP entrante desde {}", clientSocket.getRemoteSocketAddress());
                    
                    // Intento de adquirir un handler del pool antes de usarlo. Si es null, está lleno.
                    ClientHandler handler = connectionPool.acquire(clientSocket);
                    
                    if (handler == null) {
                        logger.warn("Pool de conexiones lleno. Rechazando conexión de {}", clientSocket.getRemoteSocketAddress());
                        rejectConnection(clientSocket);
                    } else {
                        // El connectionTask es gestionado internamente por el pool de conexiones
                        logger.debug("Conexión aceptada y encolada en el ConnectionPool.");
                    }
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error aceptando conexión TCP", e);
                    } else {
                        logger.info("ServerSocket TCP fue cerrado, finalizando bucle de aceptación.");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Fallo al iniciar el servidor TCP en el puerto {}", port, e);
        } finally {
            logger.info("TCP Server Thread finalizado.");
        }
    }

    private void rejectConnection(Socket clientSocket) {
        try (Socket socket = clientSocket;
             OutputStream out = socket.getOutputStream()) {
            String rejectJson = "{\"status\": \"error\", \"message\": \"Servidor sobrecargado. Intente más tarde.\"}";
            out.write(rejectJson.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            logger.error("Error al enviar mensaje de rechazo y/o cerrar el socket", e);
        }
    }
}
