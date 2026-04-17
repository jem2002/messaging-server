package com.universidad.messaging.server.network;

import com.universidad.messaging.server.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Worker reutilizable del Object Pool de conexiones.
 * Cada instancia es creada una sola vez por el {@link ConnectionPool} y reciclada
 * tras completar el procesamiento de un cliente TCP.
 *
 * <p>Ciclo de vida:
 * <ol>
 *   <li>El pool invoca {@link #assignSocket(Socket)} para inyectar la conexión.</li>
 *   <li>El pool envía esta instancia al {@code ExecutorService} para su ejecución.</li>
 *   <li>Al finalizar ({@code finally}), el handler cierra el socket y se devuelve al pool.</li>
 * </ol>
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final ConnectionPool pool;
    private volatile Socket socket;

    /**
     * Construye un ClientHandler asociado al pool que lo gestiona.
     *
     * @param pool referencia al {@link ConnectionPool} para la devolución automática.
     */
    public ClientHandler(ConnectionPool pool) {
        this.pool = pool;
    }

    /**
     * Inyecta el socket del cliente que será procesado en la próxima ejecución.
     *
     * @param socket socket TCP del cliente recién aceptado por el servidor.
     */
    public void assignSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Ciclo de vida principal del worker.
     * Procesa la conexión del cliente y garantiza la devolución al pool
     * mediante el bloque {@code finally}, independientemente de éxito o fallo.
     */
    @Override
    public void run() {
        try {
            if (socket == null || socket.isClosed()) {
                logger.warn("ClientHandler ejecutado sin un socket válido asignado.");
                return;
            }

            logger.info("Procesando conexión del cliente: {}", socket.getRemoteSocketAddress());

            // --- Simulación del ciclo de vida de la conexión ---
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                // Leer datos del cliente (simulación básica)
                byte[] buffer = new byte[4096];
                int bytesRead = in.read(buffer);

                if (bytesRead > 0) {
                    String received = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    logger.debug("Datos recibidos del cliente {}: {}", socket.getRemoteSocketAddress(), received);

                    // Respuesta de confirmación (placeholder para lógica de negocio futura)
                    String response = "{\"status\": \"ok\", \"message\": \"Mensaje recibido.\"}";
                    out.write(response.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    logger.debug("Respuesta enviada al cliente {}.", socket.getRemoteSocketAddress());
                } else {
                    logger.debug("Cliente {} cerró la conexión sin enviar datos.", socket.getRemoteSocketAddress());
                }
            }

            logger.info("Procesamiento finalizado para el cliente: {}", socket.getRemoteSocketAddress());

        } catch (IOException e) {
            logger.error("Error de I/O procesando al cliente.", e);
        } catch (Exception e) {
            logger.error("Error inesperado en ClientHandler.", e);
        } finally {
            // CRÍTICO: Cerrar el socket y devolver este handler al pool SIEMPRE
            closeSocket();
            pool.release(this);
            logger.debug("ClientHandler devuelto al pool de conexiones.");
        }
    }

    /**
     * Cierra el socket del cliente de forma segura.
     */
    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error al cerrar el socket del cliente.", e);
            }
        }
    }

    /**
     * Limpia el estado interno del handler para su reutilización.
     * Invocado por el pool al reciclar la instancia.
     */
    public void resetState() {
        this.socket = null;
    }
}
