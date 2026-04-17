package com.universidad.messaging.server.network;

import com.universidad.messaging.server.pool.ClientConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);
    private final int port;
    private final ClientConnectionPool connectionPool;
    private DatagramSocket datagramSocket;
    private volatile boolean running = false;
    
    // PUNTO CRÍTICO: Tamaño máximo permitido de payload para UDP según los estándares con IPv4 (65535 - 8(UDP) - 20(IP)) = 65507 bytes
    private static final int MAX_BUFFER_SIZE = 65507;

    public UDPServer(int port, ClientConnectionPool connectionPool) {
        this.port = port;
        this.connectionPool = connectionPool;
    }

    public void start() {
        this.running = true;
        new Thread(this, "UDPServerThread").start();
    }

    public void stop() {
        this.running = false;
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(port);
            logger.info("UDP Server iniciado en el puerto {}", port);

            /*
             * NOTA DE DISEÑO:
             * Dado que UDP no garantiza entrega, orden, ni control de congestión,
             * para archivos grandes (>1GB) la arquitectura exigirá TCP o un
             * protocolo custom que maneje ACKs (Acknowledgements), retransmisiones
             * y ensamblado de paquetes sobre UDP (como QUIC o variantes personalizadas).
             */
            while (running) {
                try {
                    byte[] buffer = new byte[MAX_BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    // Se bloquea de forma síncrona hasta recibir un datagrama entrante
                    datagramSocket.receive(packet);
                    
                    // PUNTO CRÍTICO: Despachar a un hilo del pool de procesamiento
                    // Delegamos a la capa del pool para un procesamiento concurrente rápido
                    connectionPool.dispatchDatagram(packet, datagramSocket);
                    
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error recibiendo paquete UDP", e);
                    } else {
                        logger.info("DatagramSocket UDP fue cerrado, finalizando bucle de recepción.");
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Fallo al iniciar el servidor UDP en el puerto {}", port, e);
        } finally {
            logger.info("UDP Server Thread finalizado.");
        }
    }
}
