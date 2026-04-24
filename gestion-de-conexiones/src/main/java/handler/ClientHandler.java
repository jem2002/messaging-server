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
    private final String primeraLinea;

    public ClientHandler(PooledClientConnection connection, IConnectionPool pool, MainRouter router, 
                         BroadcastManager broadcastManager, TransferManager transferManager, 
                         DocumentManager documentManager, String primeraLinea) {
        this.connection = connection;
        this.pool = pool;
        this.router = router;
        this.broadcastManager = broadcastManager;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
        this.primeraLinea = primeraLinea;
    }

    @Override
    public void run() {
        String clientIp = "UNKNOWN";
        OutputStream out = null;
        try {
            clientIp = connection.getSocket().getRemoteSocketAddress().toString();
            InputStream in = connection.getInputStream();
            out = connection.getOutputStream();

            // MODO CONTROL PERSISTENTE
            broadcastManager.addStream(out);
            logger.info("Atendiendo conexión de CONTROL desde {}", clientIp);

            // 1. Procesar la primera línea que ya leímos en el Triage
            procesarJson(primeraLinea, out, clientIp);

            // 2. Bucle para seguir recibiendo comandos JSON
            while (true) {
                String linea = leerLinea(in);
                if (linea == null) {
                    break; // Fin del stream (cliente se desconectó)
                }

                if (linea.isEmpty()) {
                    continue; 
                }

                if (linea.startsWith("{")) {
                    procesarJson(linea, out, clientIp);
                } else {
                    logger.warn("Recibido dato no-JSON en conexión de CONTROL desde {}: {}", clientIp, linea);
                }
            }

        } catch (Exception e) {
            logger.error("Conexión de CONTROL perdida con {}", clientIp);
        } finally {
            router.notificarDesconexionFisica(clientIp, out);
            if (out != null) broadcastManager.removeStream(out);
            pool.release(connection);
        }
    }

    private void procesarJson(String json, OutputStream out, String clientIp) throws Exception {
        String jsonResponse = router.routeRequest(json, clientIp);
        out.write((jsonResponse + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // MÉTODO AUXILIAR PARA LEER UNA LÍNEA
    private String leerLinea(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') baos.write(c);
        }
        if (c == -1 && baos.size() == 0) return null;
        return baos.toString(StandardCharsets.UTF_8.name()).trim();
    }
}