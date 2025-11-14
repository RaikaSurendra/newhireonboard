package com.onboardbuddy.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Token blacklist for JWT revocation
 * Addresses security issue #9 - JWT token revocation
 */
public class TokenBlacklist {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklist.class);
    private static final ConcurrentHashMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    static {
        // Clean up expired tokens every hour
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
            logger.debug("Cleaned up expired blacklisted tokens. Current size: {}", blacklistedTokens.size());
        }, 1, 1, TimeUnit.HOURS);
        
        logger.info("Token blacklist initialized");
    }
    
    /**
     * Add token to blacklist with expiration time
     */
    public static void blacklistToken(String token, long expirationTime) {
        blacklistedTokens.put(token, expirationTime);
        logger.info("Token blacklisted until: {}", expirationTime);
    }
    
    /**
     * Check if token is blacklisted
     */
    public static boolean isBlacklisted(String token) {
        Long expirationTime = blacklistedTokens.get(token);
        if (expirationTime == null) {
            return false;
        }
        
        // If token has expired, remove it and return false
        if (expirationTime < System.currentTimeMillis()) {
            blacklistedTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Remove token from blacklist
     */
    public static void removeToken(String token) {
        blacklistedTokens.remove(token);
    }
    
    /**
     * Clear all blacklisted tokens
     */
    public static void clear() {
        blacklistedTokens.clear();
        logger.info("Token blacklist cleared");
    }
    
    /**
     * Get blacklist size
     */
    public static int size() {
        return blacklistedTokens.size();
    }
    
    /**
     * Shutdown the scheduler
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
