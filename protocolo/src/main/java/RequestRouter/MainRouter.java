package RequestRouter;
import JsonSchema.JsonSchema;
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

    public MainRouter(UserManager userManager, DocumentManager documentManager) {
        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();
        this.userManager = userManager;
        this.documentManager = documentManager;
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

    private String handleConnect(JsonNode payload, String clientIp) throws Exception {
        if (payload == null || !payload.has(JsonSchema.PAYLOAD_USERNAME)) {
            return serializer.buildErrorResponse("Falta el username.");
        }

        // Extracción de datos con Jackson
        String username = payload.get(JsonSchema.PAYLOAD_USERNAME).asText();

        long userId = userManager.conectarUsuario(username, clientIp);

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }
}