package RequestRouter;
import JsonSchema.JsonSchema;
import LogService.LogManager;
import DocumentService.DocumentManager;
import JsonSerializer.ResponseBuilder;
import MessageParser.JsonInputParser;
import MessageParser.MessageWrapper;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainRouter {
    private static final Logger logger = LoggerFactory.getLogger(MainRouter.class);

    private final JsonInputParser parser;
    private final ResponseBuilder serializer;
    private final UserManager userManager;
    private final DocumentManager documentManager;

    // 1. AGREGAR EL ATRIBUTO
    private final LogManager logManager;

    // 2. PEDIRLO EN EL CONSTRUCTOR
    public MainRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager) {
        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();
        this.userManager = userManager;
        this.documentManager = documentManager;

        // 3. INICIALIZARLO
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

                // Futuros endpoints irán aquí

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
        String cleanIp = rawClientIp.replace("/", ""); // Quitamos el '/'
        String ipAddress = cleanIp;
        int port = 0;

        if (cleanIp.contains(":")) {
            String[] parts = cleanIp.split(":");
            ipAddress = parts[0];
            port = Integer.parseInt(parts[1]); // Extraemos el 47124
        }
        // -------------------------------

        // Ejecutar lógica de negocio con los datos separados
        long userId = userManager.conectarUsuario(username, ipAddress, port);

        // Registrar Log
        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS", "Usuario conectado desde " + ipAddress + ":" + port);

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }
    // Añade este método al final de tu clase MainRouter
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
            // Llamar al servicio
            userManager.desconectarPorCaidaDeRed(ipAddress, port);
        } catch (Exception e) {
            logger.error("Error procesando desconexión física", e);
        }
    }
}