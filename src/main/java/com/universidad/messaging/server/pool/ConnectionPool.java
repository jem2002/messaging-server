package com.universidad.messaging.server.pool;

import com.universidad.messaging.server.network.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Object Pool de conexiones para el Messaging Server.
 * Gestiona un número fijo de instancias {@link ClientHandler} reutilizables
 * almacenadas en un {@link ArrayBlockingQueue}, garantizando un acceso
 * thread-safe sin sincronización explícita.
 *
 * <p>Estrategia <em>fail-fast</em>: si el pool está agotado, {@link #acquire(Socket)}
 * retorna {@code null} inmediatamente en lugar de bloquear al llamante.</p>
 *
 * <p>Ciclo de vida de un handler:
 * <pre>
 *   [Queue] --poll()--> acquire() --submit()--> ExecutorService
 *                                                   |
 *                            release() <--finally-- run()
 *                               |
 *                          [Queue] (reciclado)
 * </pre>
 */
public class ConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final int maxCapacity;
    private final ArrayBlockingQueue<ClientHandler> availableHandlers;
    private final ExecutorService executor;

    /**
     * Inicializa el Object Pool con la capacidad indicada.
     * Crea todas las instancias de {@link ClientHandler} por adelantado (pre-warming)
     * y las almacena en la cola de disponibles.
     *
     * @param maxCapacity número máximo de conexiones simultáneas soportadas.
     */
    public ConnectionPool(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.availableHandlers = new ArrayBlockingQueue<>(maxCapacity);
        this.executor = Executors.newCachedThreadPool();

        // Pre-warming: instanciar todos los handlers y agregarlos al pool
        for (int i = 0; i < maxCapacity; i++) {
            availableHandlers.offer(new ClientHandler(this));
        }

        logger.info("ConnectionPool inicializado. Capacidad máxima: {} handlers pre-creados.", maxCapacity);
    }

    /**
     * Intenta adquirir un {@link ClientHandler} disponible del pool y asignarle
     * el socket del cliente entrante. Si el pool está agotado, retorna {@code null}
     * (estrategia fail-fast, no bloqueante).
     *
     * @param clientSocket el socket TCP del cliente recién aceptado por el servidor.
     * @return el {@link ClientHandler} asignado y enviado a ejecución, o {@code null}
     *         si no hay handlers disponibles.
     */
    public ClientHandler acquire(Socket clientSocket) {
        ClientHandler handler = availableHandlers.poll();

        if (handler == null) {
            logger.warn("Pool de conexiones agotado ({}/{}). No se puede atender al cliente.",
                    0, maxCapacity);
            return null;
        }

        handler.assignSocket(clientSocket);
        executor.submit(handler);

        logger.debug("Handler adquirido del pool. Disponibles: {}/{}", availableHandlers.size(), maxCapacity);
        return handler;
    }

    /**
     * Devuelve un {@link ClientHandler} al pool tras finalizar su procesamiento.
     * Limpia el estado interno del handler antes de reinsertarlo en la cola.
     *
     * <p>Este método es invocado desde el bloque {@code finally} de
     * {@link ClientHandler#run()}, garantizando la devolución incluso en caso de error.</p>
     *
     * @param handler la instancia de handler a reciclar.
     */
    public void release(ClientHandler handler) {
        handler.resetState();
        boolean reinserted = availableHandlers.offer(handler);

        if (reinserted) {
            logger.debug("Handler devuelto al pool. Disponibles: {}/{}", availableHandlers.size(), maxCapacity);
        } else {
            logger.warn("No se pudo reinsertar el handler en el pool (cola llena). Esto no debería ocurrir.");
        }
    }

    /**
     * Despacha el datagrama recibido por el servidor UDP a un pool de procesamiento.
     * <p>Actualmente es un placeholder; la implementación UDP se completará en un hito posterior.</p>
     *
     * @param packet el paquete UDP recibido.
     * @param socket socket para enviar respuestas si es necesario.
     */
    public void dispatchDatagram(DatagramPacket packet, DatagramSocket socket) {
        logger.debug("Datagrama recibido de {}:{} (procesamiento UDP pendiente de implementación).",
                packet.getAddress().getHostAddress(), packet.getPort());
    }

    /**
     * Inicia el apagado ordenado (graceful shutdown) del pool de conexiones.
     * <ol>
     *   <li>Señaliza al {@code ExecutorService} que no acepte nuevas tareas.</li>
     *   <li>Espera hasta {@value #SHUTDOWN_TIMEOUT_SECONDS} segundos para que las tareas en curso finalicen.</li>
     *   <li>Si excede el timeout, fuerza la terminación con {@code shutdownNow()}.</li>
     * </ol>
     */
    public void shutdown() {
        logger.info("Iniciando apagado del ConnectionPool...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("El ExecutorService no finalizó en {} segundos. Forzando apagado...",
                        SHUTDOWN_TIMEOUT_SECONDS);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupción durante el apagado del ExecutorService. Forzando cierre.", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ConnectionPool apagado. Handlers disponibles restantes: {}", availableHandlers.size());
    }
}
