package com.onboardbuddy.security;

import com.onboardbuddy.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CSRF Token Manager
 * Addresses security issue #6 - CSRF protection
 */
public class CsrfTokenManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfTokenManager.class);
    private static final ConcurrentHashMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static boolean enabled;
    private static long tokenValidity;
    
    static {
        enabled = ConfigLoader.getBoolean("csrf.enabled", true);
        tokenValidity = ConfigLoader.getInt("csrf.tokenValidity", 3600) * 1000L; // Convert to milliseconds
        
        // Clean up expired tokens every 10 minutes
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            tokens.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
            logger.debug("Cleaned up expired CSRF tokens. Current size: {}", tokens.size());
        }, 10, 10, TimeUnit.MINUTES);
        
        logger.info("CSRF Token Manager initialized: enabled={}, validity={}s", enabled, tokenValidity / 1000);
    }
    
    private static class TokenInfo {
        private final String userId;
        private final long expirationTime;
        
        public TokenInfo(String userId, long expirationTime) {
            this.userId = userId;
            this.expirationTime = expirationTime;
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime > expirationTime;
        }
        
        public String getUserId() {
            return userId;
        }
    }
    
    /**
     * Generate a new CSRF token for a user
     */
    public static String generateToken(String userId) {
        if (!enabled) {
            return "csrf-disabled";
        }
        
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        long expirationTime = System.currentTimeMillis() + tokenValidity;
        tokens.put(token, new TokenInfo(userId, expirationTime));
        
        logger.debug("Generated CSRF token for user: {}", userId);
        return token;
    }
    
    /**
     * Validate a CSRF token
     */
    public static boolean validateToken(String token, String userId) {
        if (!enabled) {
            return true;
        }
        
        if (token == null || userId == null) {
            logger.warn("CSRF validation failed: null token or userId");
            return false;
        }
        
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo == null) {
            logger.warn("CSRF validation failed: token not found");
            return false;
        }
        
        long now = System.currentTimeMillis();
        if (tokenInfo.isExpired(now)) {
            tokens.remove(token);
            logger.warn("CSRF validation failed: token expired");
            return false;
        }
        
        if (!tokenInfo.getUserId().equals(userId)) {
            logger.warn("CSRF validation failed: userId mismatch");
            return false;
        }
        
        return true;
    }
    
    /**
     * Invalidate a CSRF token
     */
    public static void invalidateToken(String token) {
        if (token != null) {
            tokens.remove(token);
            logger.debug("Invalidated CSRF token");
        }
    }
    
    /**
     * Invalidate all tokens for a user
     */
    public static void invalidateUserTokens(String userId) {
        tokens.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
        logger.debug("Invalidated all CSRF tokens for user: {}", userId);
    }
    
    /**
     * Check if CSRF protection is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get token count (for monitoring)
     */
    public static int getTokenCount() {
        return tokens.size();
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
