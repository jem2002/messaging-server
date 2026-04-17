package com.universidad.messaging.server.network;

import com.universidad.messaging.server.pool.ClientConnectionPool;
import com.universidad.messaging.server.pool.PooledClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Worker que procesa una conexión de cliente TCP utilizando una
 * {@link PooledClientConnection} obtenida del {@link ClientConnectionPool}.
 *
 * <p>Relación con el diagrama de clases:
 * <pre>
 *   TCPServer --lanza--&gt; ClientHandler --solicita conexión--&gt; ClientConnectionPool
 * </pre>
 *
 * <p><b>PUNTO CRÍTICO ({@code finally}):</b> El bloque {@code finally} de {@link #run()}
 * invoca obligatoriamente {@link ClientConnectionPool#release(PooledClientConnection)}
 * para devolver la conexión al pool, garantizando el reciclaje incluso ante excepciones.</p>
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final PooledClientConnection connection;
    private final ClientConnectionPool pool;

    /**
     * Construye un ClientHandler para procesar la conexión pooleable indicada.
     *
     * @param connection la conexión del cliente adquirida del pool (ya con socket asignado).
     * @param pool       referencia al pool para la devolución automática en {@code finally}.
     */
    public ClientHandler(PooledClientConnection connection, ClientConnectionPool pool) {
        this.connection = connection;
        this.pool = pool;
    }

    /**
     * Ciclo de vida principal del worker.
     * Procesa la conexión del cliente y garantiza la devolución al pool
     * mediante el bloque {@code finally}, independientemente de éxito o fallo.
     */
    @Override
    public void run() {
        try {
            if (connection.getSocket() == null || connection.getSocket().isClosed()) {
                logger.warn("ClientHandler ejecutado sin un socket válido asignado.");
                return;
            }

            logger.info("Procesando conexión del cliente: {}",
                    connection.getSocket().getRemoteSocketAddress());

            // Obtener streams desde la conexión pooleable (ya inicializados por setSocket)
            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            if (in == null || out == null) {
                logger.error("Streams no inicializados en la conexión pooleable.");
                return;
            }

            // --- Simulación del ciclo de vida de la conexión ---
            byte[] buffer = new byte[4096];
            int bytesRead = in.read(buffer);

            if (bytesRead > 0) {
                String received = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                logger.debug("Datos recibidos del cliente {}: {}",
                        connection.getSocket().getRemoteSocketAddress(), received);

                // Respuesta de confirmación (placeholder para lógica de negocio futura)
                String response = "{\"status\": \"ok\", \"message\": \"Mensaje recibido.\"}";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
                logger.debug("Respuesta enviada al cliente {}.",
                        connection.getSocket().getRemoteSocketAddress());
            } else {
                logger.debug("Cliente {} cerró la conexión sin enviar datos.",
                        connection.getSocket().getRemoteSocketAddress());
            }

            logger.info("Procesamiento finalizado para el cliente: {}",
                    connection.getSocket().getRemoteSocketAddress());

        } catch (IOException e) {
            logger.error("Error de I/O procesando al cliente.", e);
        } catch (Exception e) {
            logger.error("Error inesperado en ClientHandler.", e);
        } finally {
            // CRÍTICO: Devolver la conexión al pool SIEMPRE (reset + requeue)
            pool.release(connection);
            logger.debug("Conexión devuelta al pool de conexiones.");
        }
    }
}
