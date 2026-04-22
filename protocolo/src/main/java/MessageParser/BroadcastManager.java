package MessageParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BroadcastManager {
    private static final Logger logger = LoggerFactory.getLogger(BroadcastManager.class);
    // Usamos CopyOnWriteArraySet para evitar errores de concurrencia cuando muchos hilos entran y salen
    private final Set<OutputStream> activeStreams = new CopyOnWriteArraySet<>();

    public void addStream(OutputStream out) {
        activeStreams.add(out);
    }

    public void removeStream(OutputStream out) {
        activeStreams.remove(out);
    }

    public void broadcast(String jsonMessage) {
        logger.debug("Haciendo broadcast a {} clientes", activeStreams.size());
        byte[] messageBytes = (jsonMessage + "\n").getBytes(StandardCharsets.UTF_8);

        for (OutputStream out : activeStreams) {
            try {
                out.write(messageBytes);
                out.flush();
            } catch (Exception e) {
                // Si falla uno, lo ignoramos para no interrumpir el envío a los demás
                logger.error("Fallo al enviar broadcast a un stream inactivo.");
            }
        }
    }
}