package RequestRouter;

import JsonSchema.JsonSchema;
import LogService.LogManager;
import DocumentService.DocumentManager;
import JsonSerializer.ResponseBuilder;
import MessageParser.BroadcastManager;
import MessageParser.JsonInputParser;
import MessageParser.MessageWrapper;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class MainRouter {
    private static final Logger logger = LoggerFactory.getLogger(MainRouter.class);

    private final JsonInputParser parser;
    private final ResponseBuilder serializer;
    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final BroadcastManager broadcastManager;
    private final LogManager logManager;
    private final TransferManager transferManager;

    public MainRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager,
            BroadcastManager broadcastManager, TransferManager transferManager) {
        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.logManager = logManager;
        this.transferManager = transferManager;
    }

    public String routeRequest(String rawJson, String clientIp) {
        MessageWrapper request = parser.parse(rawJson);

        if (request == null) {
            return serializer.buildErrorResponse("Formato JSON inválido.");
        }

        try {
            switch (request.getAction()) {
                case JsonSchema.ACTION_CONNECT:
                    return handleConnect(request.getPayload(), clientIp);
                case JsonSchema.ACTION_LIST_CLIENTS:
                    return handleListClients();
                // Futuros endpoints irán aquí
                case JsonSchema.ACTION_LIST_DOCUMENTS:
                    return handleListDocuments();
                case JsonSchema.ACTION_LIST_MESSAGES:
                    return handleListMessages();
                case JsonSchema.ACTION_LIST_LOGS:
                    return handleListLogs();
                // 1. Añadir al switch dentro de routeRequest:
                case JsonSchema.ACTION_UPLOAD_INIT:
                    return handleUploadInit(request.getPayload(), clientIp);
                case JsonSchema.ACTION_DOWNLOAD_INIT:
                    return handleDownloadInit(request.getPayload(), clientIp);
                case JsonSchema.ACTION_SEND_MESSAGE:
                    return handleSendMessage(request.getPayload(), clientIp);
                default:
                    return serializer.buildErrorResponse("Acción no soportada.");
            }
        } catch (Exception e) {
            logger.error("Error en router procesando acción: {}", request.getAction(), e);
            return serializer.buildErrorResponse("Error interno del servidor.");
        }
    }

    private String handleConnect(JsonNode payload, String rawClientIp) throws Exception {
        if (payload == null || !payload.has(JsonSchema.PAYLOAD_USERNAME)) {
            return serializer.buildErrorResponse("Falta el username.");
        }

        String username = payload.get(JsonSchema.PAYLOAD_USERNAME).asText();

        // --- LIMPIEZA DE IP Y PUERTO ---
        String cleanIp = rawClientIp.replace("/", "");
        String ipAddress = cleanIp;
        int port = 0;

        if (cleanIp.contains(":")) {
            String[] parts = cleanIp.split(":");
            ipAddress = parts[0];
            port = Integer.parseInt(parts[1]);
        }
        // -------------------------------

        // Ejecutar lógica de negocio
        long userId = userManager.conectarUsuario(username, ipAddress, port);

        // Registrar Log
        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS",
                "Usuario conectado desde " + ipAddress + ":" + port);


        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }

    private String handleListClients() {
        List<Map<String, String>> activos = userManager.obtenerClientesActivos();
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_CLIENTS, activos, "clientes");
    }

    public void notificarDesconexionFisica(String rawClientIp, OutputStream out) {
        try {
            String cleanIp = rawClientIp.replace("/", "");
            if (out != null)
                broadcastManager.removeStream(out);
            String ipAddress = cleanIp;
            int port = 0;
            if (cleanIp.contains(":")) {
                String[] parts = cleanIp.split(":");
                ipAddress = parts[0];
                port = Integer.parseInt(parts[1]);
            }

            // 1. Cerrar sesión en la BD
            long userId = userManager.desconectarPorCaidaDeRed(ipAddress, port);

            // LOG Y BROADCAST DE LOGS
            logManager.registrarAccion(null, userId, "DISCONNECT", "SUCCESS",
                    "Desconexión física: " + ipAddress + ":" + port);
            broadcastManager.broadcast(handleListLogs());

            // 2. AVISAR A TODOS QUE ALGUIEN SALIÓ (Aquí es donde debía ir)
            String listaTrasDesconexion = handleListClients();
            broadcastManager.broadcast(listaTrasDesconexion);

        } catch (Exception e) {
            logger.error("Error procesando desconexión física", e);
        }
    }

    public String handleListDocuments() {
        List<Map<String, String>> docs = documentManager.obtenerArchivosDisponibles();
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_DOCUMENTS, docs, "documentos");
    }

    public String handleListMessages() {
        List<Map<String, String>> msgs = documentManager.obtenerMensajesDisponibles();
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_MESSAGES, msgs, "mensajes");
    }

    private String handleListLogs() {
        List<Map<String, String>> logs = logManager.listarLogs();
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_LOGS, logs, "logs");
    }

    // 2. Crear el método (necesitarás inyectar TransferManager en el constructor
    // del router):
    private String handleUploadInit(JsonNode payload, String clientIp) {
        try {
            String filename = payload.get("filename").asText();
            long size = payload.get("size").asLong();
            String extension = payload.get("extension").asText();
            String mimeType = payload.get("mimeType").asText();
            String username = payload.get("username").asText(); // Quién lo sube

            // Buscar el ID del usuario en BD
            long userId = userManager.obtenerIdUsuario(username);

            // Generar un Ticket único
            String token = java.util.UUID.randomUUID().toString();

            // Guardar el ticket en memoria
            TransferTicket ticket = new TransferTicket(token, filename, size, extension, mimeType, userId, clientIp);
            transferManager.registrarTicket(ticket);

            broadcastManager.broadcast(handleListLogs());

            // Responder al cliente con su ticket
            return serializer.buildSuccessResponse(JsonSchema.ACTION_UPLOAD_INIT, token);

        } catch (Exception e) {
            logger.error("Error al generar ticket de subida", e);
            return serializer.buildErrorResponse("Datos de archivo inválidos.");
        }
    }

    private String handleDownloadInit(JsonNode payload, String clientIp) {
        try {
            long docId = payload.get("document_id").asLong();

            // Buscar los detalles
            Map<String, String> detalles = documentManager.obtenerDetallesDescarga(docId);
            String filename = detalles.get("nombre");
            long size = Long.parseLong(detalles.get("tamano"));
            String encryptedPath = detalles.get("ruta_cifrada");

            // Detectamos el formato solicitado (si no viene, asumimos descarga normal)
            String format = payload.has("format") ? payload.get("format").asText().toUpperCase() : "";
            String prefix = "DWN-";
            String ticketInfo = encryptedPath; // Por defecto mandamos el path

            if (format.equals("ORG")) {
                prefix = "DWN-ORG-";
                ticketInfo = String.valueOf(docId);
                // El tamaño ya es el original por defecto
            } else if (format.equals("HSH")) {
                prefix = "DWN-HSH-";
                ticketInfo = String.valueOf(docId);
                size = documentManager.obtenerTamanoHash(docId);
            } else if (format.equals("ENC")) {
                prefix = "DWN-ENC-";
                ticketInfo = String.valueOf(docId);
                size = documentManager.obtenerTamanoEncriptado(docId);
            }

            // ¡IMPORTANTE! Le ponemos el prefijo adecuado para distinguirlo
            String token = prefix + java.util.UUID.randomUUID().toString();

            // Reutilizamos TransferTicket. En mimeType guardamos encryptedPath o el docId según el modo
            TransferTicket ticket = new TransferTicket(token, filename, size, "", ticketInfo, 0, clientIp);
            transferManager.registrarTicket(ticket);


            return serializer.buildDownloadInitResponse(token, size, docId);

        } catch (Exception e) {
            logger.error("Error al generar ticket de descarga", e);
            return serializer.buildErrorResponse("No se pudo preparar la descarga.");
        }
    }

    private String handleSendMessage(JsonNode payload, String clientIp) {
        try {
            String fromUser = payload.get("username").asText();
            String content = payload.get("message").asText();

            // 1. Obtenemos el ID del remitente
            long userId = userManager.obtenerIdUsuario(fromUser);

            // 2. EL TRUCO ARQUITECTÓNICO: Convertimos el String de texto en un flujo de
            // bytes (InputStream)
            java.io.InputStream textStream = new java.io.ByteArrayInputStream(
                    content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Le inventamos un nombre de archivo único
            String nombreArchivo = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";

            // ¡Magia! Le pasamos el texto a tu motor de archivos pesados.
            // Él se encarga de guardarlo en disco, sacar el Hash, encriptarlo y meterlo en
            // MySQL.
            documentManager.procesarRecepcionDocumento(
                    textStream, nombreArchivo, content.length(), ".txt", "text/plain", userId, clientIp, "MESSAGE");

            // 3. Retransmisión en Tiempo Real (Chat Global)
            // Usamos tu BroadcastManager para enviarle el texto a todos los clientes que
            // estén conectados
            String mensajeRealTime = serializer.buildSuccessResponse(
                    JsonSchema.ACTION_NEW_MESSAGE,
                    "De " + fromUser + ": " + content);
            broadcastManager.broadcast(mensajeRealTime);

            // LOG Y BROADCAST DE LOGS
            logManager.registrarAccion(null, userId, "SEND_MESSAGE", "SUCCESS", "Mensaje enviado por " + fromUser);
            broadcastManager.broadcast(handleListLogs());

            // 4. Le respondemos a quien lo envió que todo salió bien
            return serializer.buildSuccessResponse(JsonSchema.ACTION_SEND_MESSAGE,
                    "Mensaje procesado, encriptado y entregado.");

        } catch (Exception e) {
            logger.error("Error procesando mensaje de texto", e);
            return serializer.buildErrorResponse("No se pudo enviar el mensaje.");
        }
    }
}