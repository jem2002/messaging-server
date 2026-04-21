package JsonSerializer;
import JsonSchema.JsonSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class ResponseBuilder {
    private final ObjectMapper mapper;

    public ResponseBuilder() {
        this.mapper = new ObjectMapper();
    }

    public String buildSuccessResponse(String action, String message) {
        ObjectNode root = mapper.createObjectNode();
        root.put(JsonSchema.KEY_ACTION, action + "_ACK");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "SUCCESS");
        payload.put("message", message);

        root.set(JsonSchema.KEY_PAYLOAD, payload);

        return root.toString();
    }

    public String buildErrorResponse(String reason) {
        ObjectNode root = mapper.createObjectNode();
        root.put(JsonSchema.KEY_ACTION, JsonSchema.ACTION_ERROR);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "ERROR");
        payload.put(JsonSchema.PAYLOAD_REASON, reason);

        root.set(JsonSchema.KEY_PAYLOAD, payload);

        return root.toString();
    }
}