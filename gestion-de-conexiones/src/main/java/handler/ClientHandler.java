package handler;
import DocumentService.DocumentManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import RequestRouter.TransferTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
import pool.PooledClientConnection;
import UserService.UserManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import MessageParser.BroadcastManager;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final PooledClientConnection connection;
    private final IConnectionPool pool;
    private final MainRouter router;
    private final BroadcastManager broadcastManager;
    private final TransferManager transferManager;
    private final DocumentManager documentManager;

    public ClientHandler(PooledClientConnection connection, IConnectionPool pool, MainRouter router, BroadcastManager broadcastManager,TransferManager transferManager, DocumentManager documentManager) {
        this.connection = connection;
        this.pool = pool;
        this.router = router;
        this.broadcastManager = broadcastManager;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
    }
    @Override
    public void run() {
        String clientIp = "UNKNOWN";
        OutputStream out = null;
        try {
            clientIp = connection.getSocket().getRemoteSocketAddress().toString();
            InputStream in = connection.getInputStream();
            out = connection.getOutputStream();

            // LECTURA DE LA PRIMERA LÍNEA (Byte por byte hasta encontrar un Enter)
            String primeraLinea = leerPrimeraLinea(in);

            if (primeraLinea.startsWith("{")) {
                // =============== MODO NORMAL (JSON CHAT) ===============
                broadcastManager.addStream(out);
                logger.info("Atendiendo cliente normal desde {}", clientIp);

                // Procesar esa primera línea
                String resp = router.routeRequest(primeraLinea, clientIp);
                out.write((resp + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();

                // Seguir en el bucle normal
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    String rawJson = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
                    String jsonResponse = router.routeRequest(rawJson, clientIp);
                    out.write((jsonResponse + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

            } else {
                // =============== MODO TRANSFERENCIA DE ARCHIVOS ===============
                String token = primeraLinea.trim();
                TransferTicket ticket = transferManager.validarYConsumirTicket(token);

                if (ticket != null) {
                    logger.info("Iniciando recepción de archivo pesado. Token: {}", token);

                    // ¡LE PASAMOS EL SOCKET DIRECTO AL MOTOR PESADO!
                    boolean exito = documentManager.procesarRecepcionDocumento(
                            in, ticket.filename, ticket.sizeBytes, ticket.extension,
                            ticket.mimeType, ticket.ownerUserId, ticket.ownerIp
                    );

                    // Avisar al cliente si salió bien (usando JSON rápido antes de cerrar)
                    String status = exito ? "{\"status\":\"UPLOAD_SUCCESS\"}\n" : "{\"status\":\"UPLOAD_FAILED\"}\n";
                    out.write(status.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    // (Opcional) Hacer un broadcast por el router avisando a todos que hay un nuevo archivo
                } else {
                    logger.warn("Ticket inválido o expirado desde {}", clientIp);
                }
            }

        } catch (Exception e) {
            logger.error("Conexión perdida con {}", clientIp);
        } finally {
            router.notificarDesconexionFisica(clientIp);
            if (out != null) broadcastManager.removeStream(out);
            pool.release(connection);
        }
    }

    // MÉTODO AUXILIAR PARA LEER LA PRIMERA LÍNEA SIN ROMPER LOS BYTES
    private String leerPrimeraLinea(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            sb.append((char) c);
        }
        return sb.toString().trim();
    }
}