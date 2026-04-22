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

    public MainRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager, BroadcastManager broadcastManager) {
        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.logManager = logManager;
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
        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS", "Usuario conectado desde " + ipAddress + ":" + port);

        // AVISAR A TODOS QUE ALGUIEN NUEVO ENTRÓ (Solo 1 vez)
        String listaActualizada = handleListClients();
        broadcastManager.broadcast(listaActualizada);

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }

    private String handleListClients() {
        List<Map<String, String>> activos = userManager.obtenerClientesActivos();
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_CLIENTS, activos, "clientes");
    }

    public void notificarDesconexionFisica(String rawClientIp) {
        try {
            String cleanIp = rawClientIp.replace("/", "");
            String ipAddress = cleanIp;
            int port = 0;
            if (cleanIp.contains(":")) {
                String[] parts = cleanIp.split(":");
                ipAddress = parts[0];
                port = Integer.parseInt(parts[1]);
            }

            // 1. Cerrar sesión en la BD
            userManager.desconectarPorCaidaDeRed(ipAddress, port);

            // 2. AVISAR A TODOS QUE ALGUIEN SALIÓ (Aquí es donde debía ir)
            String listaTrasDesconexion = handleListClients();
            broadcastManager.broadcast(listaTrasDesconexion);

        } catch (Exception e) {
            logger.error("Error procesando desconexión física", e);
        }
    }

    private String handleListDocuments() {
        List<Map<String, String>> docs = documentManager.obtenerDocumentosDisponibles();
        // Usamos el mismo método del serializador, pero le pasamos la lista de docs y le llamamos "documentos"
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_DOCUMENTS, docs, "documentos");
    }
}