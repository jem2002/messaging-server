package handler;
import RequestRouter.MainRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
import pool.PooledClientConnection;
import UserService.UserManager;
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

            byte[] buffer = new byte[4096];
            int bytesRead;

            // Bucle que mantiene viva la conexión hasta que el cliente hace Ctrl+C (devuelve -1)
            while ((bytesRead = in.read(buffer)) != -1) {
                String rawJson = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();

                String jsonResponse = router.routeRequest(rawJson, clientIp);

                out.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                out.write('\n');
                out.flush();
            }
            logger.info("El cliente {} ha cerrado la conexión educadamente.", clientIp);

        } catch (Exception e) {
            logger.error("Conexión perdida abruptamente con {} (Ej: Ctrl+C)", clientIp);
        } finally {
            // OPERACIÓN DE LIMPIEZA CRÍTICA: Avisar al router y liberar el recurso
            router.notificarDesconexionFisica(clientIp);
            pool.release(connection);
            logger.info("Conexión de {} liberada y reciclada.", clientIp);
        }
    }
}