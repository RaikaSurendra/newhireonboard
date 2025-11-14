package com.onboardbuddy.config;

import com.onboardbuddy.Application;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration and connection pool management
 */
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    
    public static void initialize() {
        Properties config = Application.getConfig();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getProperty("db.url"));
        hikariConfig.setUsername(config.getProperty("db.username"));
        hikariConfig.setPassword(config.getProperty("db.password"));
        hikariConfig.setDriverClassName(config.getProperty("db.driver"));
        
        // Pool configuration
        hikariConfig.setMaximumPoolSize(
            Integer.parseInt(config.getProperty("db.pool.maxSize", "20"))
        );
        hikariConfig.setMinimumIdle(
            Integer.parseInt(config.getProperty("db.pool.minIdle", "5"))
        );
        hikariConfig.setConnectionTimeout(
            Long.parseLong(config.getProperty("db.pool.connectionTimeout", "30000"))
        );
        hikariConfig.setIdleTimeout(
            Long.parseLong(config.getProperty("db.pool.idleTimeout", "600000"))
        );
        hikariConfig.setMaxLifetime(
            Long.parseLong(config.getProperty("db.pool.maxLifetime", "1800000"))
        );
        
        // Connection test query
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // Pool name
        hikariConfig.setPoolName("OnboardBuddyPool");
        
        dataSource = new HikariDataSource(hikariConfig);
        
        logger.info("Database connection pool initialized");
        logger.info("JDBC URL: {}", config.getProperty("db.url"));
        logger.info("Max Pool Size: {}", hikariConfig.getMaximumPoolSize());
        
        // Test connection
        try (Connection conn = dataSource.getConnection()) {
            logger.info("Database connection test successful");
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }
    
    public static DataSource getDataSource() {
        return dataSource;
    }
    
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}
