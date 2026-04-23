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

            // MODO UNIFICADO
            broadcastManager.addStream(out);
            logger.info("Atendiendo cliente unificado desde {}", clientIp);

            while (true) {
                String linea = leerLinea(in);
                if (linea == null) {
                    break; // Fin del stream (cliente se desconectó)
                }

                if (linea.isEmpty()) {
                    continue; // Ignorar líneas vacías
                }

                if (linea.startsWith("{")) {
                    // =============== MODO NORMAL (JSON CHAT) ===============
                    String jsonResponse = router.routeRequest(linea, clientIp);
                    out.write((jsonResponse + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } else {
                    // =============== MODO TRANSFERENCIA DE ARCHIVOS ===============
                    String token = linea;
                    TransferTicket ticket = transferManager.validarYConsumirTicket(token);

                    if (ticket != null) {
                        if (token.startsWith("DWN-")) {
                            // --- LA NUEVA LÓGICA DE DESCARGA ---
                            logger.info("Iniciando envío de archivo pesado al cliente. Token: {}", token);
                            String encryptedPath = ticket.mimeType; // Donde guardamos la ruta temporalmente

                            // Enviar el archivo descifrándolo al vuelo
                            documentManager.enviarDocumentoAlCliente(encryptedPath, out);

                        } else {
                            // --- LÓGICA ACTUAL DE SUBIDA (UPL) ---
                            logger.info("Iniciando recepción de archivo pesado. Token: {}", token);
                            boolean exito = documentManager.procesarRecepcionDocumento(
                                    in, ticket.filename, ticket.sizeBytes, ticket.extension,
                                    ticket.mimeType, ticket.ownerUserId, ticket.ownerIp
                            );
                            String status = exito ? "{\"status\":\"UPLOAD_SUCCESS\"}\n" : "{\"status\":\"UPLOAD_FAILED\"}\n";
                            out.write(status.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        }
                    } else {
                        logger.warn("Ticket inválido o ignorado desde {}", clientIp);
                    }
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

    // MÉTODO AUXILIAR PARA LEER UNA LÍNEA SIN ROMPER LOS BYTES
    private String leerLinea(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            sb.append((char) c);
        }
        if (c == -1 && sb.length() == 0) {
            return null; // EOF
        }
        return sb.toString().trim();
    }
}