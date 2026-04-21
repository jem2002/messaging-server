package MessageParser;

import com.fasterxml.jackson.databind.JsonNode;

public class MessageWrapper {
    private String action;
    private JsonNode payload;

    public MessageWrapper(String action, JsonNode payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return action; }
    public JsonNode getPayload() { return payload; }
}