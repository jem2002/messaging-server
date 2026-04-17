package com.universidad.messaging.server.repository.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestor de conexiones a la base de datos MySQL.
 * Administra un pool de conexiones utilizando HikariCP.
 * Implementa el patrón Singleton mediante el idiom "Initialization-on-demand holder"
 * para garantizar seguridad en entornos multihilo (thread-safe) y un rendimiento óptimo.
 * 
 * NOTA: Este pool es exclusivo para comunicación JDBC con MySQL y es independiente
 * del Object Pool de conexiones de sockets de clientes.
 */
public class DatabaseConnectionManager {

    private final HikariDataSource dataSource;

    /**
     * Constructor privado para prevenir instanciación externa.
     * Configura el pool de conexiones leyendo las propiedades y estableciendo
     * los parámetros críticos de rendimiento exigidos en la arquitectura.
     */
    private DatabaseConnectionManager() {
        HikariConfig config = new HikariConfig();
        Properties properties = loadProperties();

        // 1. Configuración de credenciales y URL desde config.properties
        config.setJdbcUrl(properties.getProperty("db.url"));
        config.setUsername(properties.getProperty("db.user"));
        config.setPassword(properties.getProperty("db.password"));

        // 2. Configuraciones de rendimiento críticas exigidas (Hardcoded por requerimiento)
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000); // 30 segundos
        config.setIdleTimeout(600000);      // 10 minutos
        
        // Optimizaciones adicionales recomendadas para MySQL y HikariCP
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Clase interna estática para el patrón Singleton (Initialization-on-demand holder).
     * Garantiza carga perezosa (lazy initialization) y seguridad en hilos sin necesidad
     * de sincronización explícita (synchronized).
     */
    private static class InstanceHolder {
        private static final DatabaseConnectionManager INSTANCE = new DatabaseConnectionManager();
    }

    /**
     * Obtiene la instancia única del administrador de conexiones.
     *
     * @return Instancia global de {@link DatabaseConnectionManager}.
     */
    public static DatabaseConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Retorna una conexión activa y válida del pool de HikariCP.
     *
     * @return Una instancia de {@link Connection} lista para ser utilizada.
     * @throws SQLException Si ocurre un error al intentar obtener la conexión del pool
     *                      o si se excede el timeout de conexión (30000ms).
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Cierra el pool de conexiones HikariCP de forma segura.
     * Debe ser invocado durante el apagado ordenado (Graceful Shutdown)
     * para liberar todos los recursos JDBC subyacentes.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Carga el archivo de propiedades de configuración "config.properties" desde el classpath.
     *
     * @return Un objeto {@link Properties} con los valores cargados.
     * @throws RuntimeException si no se puede cargar el archivo.
     */
    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró el archivo 'config.properties' en el classpath.");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error al leer el archivo 'config.properties'", ex);
        }
        return properties;
    }
}
