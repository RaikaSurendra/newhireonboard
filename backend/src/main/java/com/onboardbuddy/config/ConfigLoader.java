package com.onboardbuddy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader that prioritizes environment variables over properties files
 * This addresses security issue #1 - externalized configuration
 */
public class ConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static Properties config;
    
    public static Properties loadConfiguration() throws IOException {
        config = new Properties();
        
        // 1. Load from classpath (defaults)
        try (InputStream is = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                config.load(is);
                logger.info("Loaded default configuration from classpath");
            }
        }
        
        // 2. Load from external file if specified
        String externalConfig = System.getProperty("config.file");
        if (externalConfig != null) {
            try (FileInputStream fis = new FileInputStream(externalConfig)) {
                config.load(fis);
                logger.info("Loaded configuration from: {}", externalConfig);
            } catch (IOException e) {
                logger.warn("Could not load external config file: {}", externalConfig);
            }
        }
        
        // 3. Override with environment variables (highest priority)
        overrideWithEnvironmentVariables();
        
        // 4. Validate critical configuration
        validateConfiguration();
        
        return config;
    }
    
    private static void overrideWithEnvironmentVariables() {
        // Database
        overrideIfPresent("db.url", "DB_URL");
        overrideIfPresent("db.username", "DB_USERNAME");
        overrideIfPresent("db.password", "DB_PASSWORD");
        overrideIfPresent("db.driver", "DB_DRIVER");
        
        // Connection Pool
        overrideIfPresent("db.pool.maxSize", "DB_POOL_MAX_SIZE");
        overrideIfPresent("db.pool.minIdle", "DB_POOL_MIN_IDLE");
        overrideIfPresent("db.pool.connectionTimeout", "DB_POOL_CONNECTION_TIMEOUT");
        overrideIfPresent("db.pool.idleTimeout", "DB_POOL_IDLE_TIMEOUT");
        overrideIfPresent("db.pool.maxLifetime", "DB_POOL_MAX_LIFETIME");
        
        // Server
        overrideIfPresent("server.port", "SERVER_PORT");
        overrideIfPresent("server.contextPath", "SERVER_CONTEXT_PATH");
        overrideIfPresent("server.maxThreads", "SERVER_MAX_THREADS");
        
        // JWT
        overrideIfPresent("jwt.secret", "JWT_SECRET");
        overrideIfPresent("jwt.expiration", "JWT_EXPIRATION");
        overrideIfPresent("jwt.refreshExpiration", "JWT_REFRESH_EXPIRATION");
        overrideIfPresent("jwt.issuer", "JWT_ISSUER");
        
        // CORS
        overrideIfPresent("cors.allowedOrigins", "CORS_ALLOWED_ORIGINS");
        overrideIfPresent("cors.allowedMethods", "CORS_ALLOWED_METHODS");
        overrideIfPresent("cors.allowedHeaders", "CORS_ALLOWED_HEADERS");
        
        // Application
        overrideIfPresent("app.environment", "APP_ENVIRONMENT");
        overrideIfPresent("app.baseUrl", "APP_BASE_URL");
        
        // Rate Limiting
        overrideIfPresent("rate.limit.enabled", "RATE_LIMIT_ENABLED");
        overrideIfPresent("rate.limit.requestsPerMinute", "RATE_LIMIT_REQUESTS_PER_MINUTE");
        overrideIfPresent("rate.limit.loginAttempts", "RATE_LIMIT_LOGIN_ATTEMPTS");
        
        // Security
        overrideIfPresent("csrf.enabled", "CSRF_ENABLED");
        overrideIfPresent("csrf.tokenValidity", "CSRF_TOKEN_VALIDITY");
        
        logger.info("Configuration overridden with environment variables");
    }
    
    private static void overrideIfPresent(String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            config.setProperty(propertyKey, envValue);
            logger.debug("Overridden {} from environment variable {}", propertyKey, envKey);
        }
    }
    
    private static void validateConfiguration() {
        // Validate JWT secret
        String jwtSecret = config.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters long. " +
                "Set JWT_SECRET environment variable or update application.properties"
            );
        }
        
        if (jwtSecret.contains("change-this") || jwtSecret.contains("CHANGE_THIS")) {
            logger.warn("WARNING: Using default JWT secret! This is insecure for production.");
        }
        
        // Validate database configuration
        if (config.getProperty("db.url") == null) {
            throw new IllegalStateException("Database URL not configured");
        }
        
        if (config.getProperty("db.username") == null) {
            throw new IllegalStateException("Database username not configured");
        }
        
        // Warn about production environment
        String environment = config.getProperty("app.environment", "development");
        if ("production".equalsIgnoreCase(environment)) {
            logger.info("Running in PRODUCTION mode");
            
            // Additional production checks
            String dbPassword = config.getProperty("db.password");
            if (dbPassword != null && dbPassword.equals("apppassword")) {
                logger.error("CRITICAL: Using default database password in production!");
            }
        }
        
        logger.info("Configuration validated successfully");
    }
    
    public static Properties getConfig() {
        return config;
    }
    
    public static String get(String key) {
        return config.getProperty(key);
    }
    
    public static String get(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
    
    public static int getInt(String key, int defaultValue) {
        String value = config.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = config.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
