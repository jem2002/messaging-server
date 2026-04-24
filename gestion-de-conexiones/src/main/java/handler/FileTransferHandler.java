package handler;

import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import RequestRouter.TransferTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FileTransferHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferHandler.class);

    private final Socket socket;
    private final String token;
    private final TransferManager transferManager;
    private final DocumentManager documentManager;
    private final MainRouter router;
    private final BroadcastManager broadcastManager;

    public FileTransferHandler(Socket socket, String token, TransferManager transferManager, 
                               DocumentManager documentManager, MainRouter router, BroadcastManager broadcastManager) {
        this.socket = socket;
        this.token = token;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
        this.router = router;
        this.broadcastManager = broadcastManager;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            TransferTicket ticket = transferManager.validarYConsumirTicket(token);

            if (ticket == null) {
                logger.warn("Intento de transferencia con token inválido: {}", token);
                return;
            }

            if (token.startsWith("DWN-")) {
                // --- MODO DESCARGA ---
                if (token.startsWith("DWN-ORG-")) {
                    logger.info("Enviando ARCHIVO ORIGINAL. Token: {}", token);
                    long docId = Long.parseLong(ticket.mimeType);
                    documentManager.enviarDocumentoOriginal(docId, out);

                } else if (token.startsWith("DWN-ENC-")) {
                    logger.info("Enviando ARCHIVO ENCRIPTADO. Token: {}", token);
                    long docId = Long.parseLong(ticket.mimeType);
                    documentManager.enviarDocumentoEncriptado(docId, out);

                } else if (token.startsWith("DWN-HSH-")) {
                    logger.info("Enviando HASH. Token: {}", token);
                    long docId = Long.parseLong(ticket.mimeType);
                    documentManager.enviarDocumentoHash(docId, out);

                } else {
                    logger.info("Enviando ARCHIVO DESCIFRADO. Token: {}", token);
                    String encryptedPath = ticket.mimeType;
                    documentManager.enviarDocumentoAlCliente(encryptedPath, out);
                }
            } else {
                // --- MODO SUBIDA ---
                logger.info("Recibiendo archivo pesado. Token: {}", token);
                boolean exito = documentManager.procesarRecepcionDocumento(
                        in, ticket.filename, ticket.sizeBytes, ticket.extension,
                        ticket.mimeType, ticket.ownerUserId, ticket.ownerIp
                );

                if (exito) {
                    broadcastManager.broadcast(router.handleListDocuments());
                    broadcastManager.broadcast(router.handleListMessages());
                }

                String status = exito ? "{\"status\":\"UPLOAD_SUCCESS\"}\n" : "{\"status\":\"UPLOAD_FAILED\"}\n";
                out.write(status.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

        } catch (Exception e) {
            logger.error("Error en transferencia de archivo para el token: {}", token, e);
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }
}
