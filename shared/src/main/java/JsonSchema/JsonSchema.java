package JsonSchema;

public final class JsonSchema {

    private JsonSchema() {
        // Prevenir instanciación
    }

    // --- Estructura base del JSON ---
    public static final String KEY_ACTION = "action";
    public static final String KEY_PAYLOAD = "payload";

    // --- Valores permitidos para 'action' ---
    public static final String ACTION_CONNECT = "CONNECT";
    public static final String ACTION_ERROR = "ERROR";

    // Aquí agregaremos luego las demás (LIST_CLIENTS, SEND_DOC, etc.)

    // --- Llaves permitidas dentro del 'payload' ---
    public static final String PAYLOAD_USERNAME = "username";
    public static final String PAYLOAD_REASON = "reason";
}