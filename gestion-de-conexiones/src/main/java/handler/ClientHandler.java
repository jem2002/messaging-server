package handler;
import RequestRouter.MainRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
import pool.PooledClientConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final PooledClientConnection connection;
    private final IConnectionPool pool;
    private final MainRouter router;

    public ClientHandler(PooledClientConnection connection, IConnectionPool pool, MainRouter router) {
        this.connection = connection;
        this.pool = pool;
        this.router = router;
    }

    @Override
    public void run() {
        String clientIp = "UNKNOWN";
        try {
            clientIp = connection.getSocket().getRemoteSocketAddress().toString();
            logger.info("Atendiendo cliente desde {}", clientIp);

            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            // 1. Leer los datos crudos del socket (Para este prototipo leemos un chunk inicial)
            byte[] buffer = new byte[4096];
            int bytesRead = in.read(buffer);

            if (bytesRead > 0) {
                String rawJson = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
                logger.debug("Datos recibidos de {}: {}", clientIp, rawJson);

                // 2. ENVIAR AL PROTOCOLO (Traductor/Negocio)
                String jsonResponse = router.routeRequest(rawJson, clientIp);

                // 3. Enviar respuesta de vuelta al cliente
                out.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                out.write('\n'); // Delimitador de fin de línea
                out.flush();

                logger.debug("Respuesta enviada a {}: {}", clientIp, jsonResponse);
            }

        } catch (Exception e) {
            logger.error("Error en la comunicación con el cliente {}", clientIp, e);
        } finally {
            // 4. CRÍTICO: Devolver la conexión al pool siempre, pase lo que pase
            pool.release(connection);
            logger.info("Conexión de {} liberada y reciclada.", clientIp);
        }
    }
}