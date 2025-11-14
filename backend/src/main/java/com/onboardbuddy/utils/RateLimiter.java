package com.onboardbuddy.utils;

import com.onboardbuddy.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for API endpoints
 * Addresses security issue #2 and #23 - rate limiting
 */
public class RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private static final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static boolean enabled;
    private static int requestsPerMinute;
    private static int loginAttemptsLimit;
    
    static {
        enabled = ConfigLoader.getBoolean("rate.limit.enabled", true);
        requestsPerMinute = ConfigLoader.getInt("rate.limit.requestsPerMinute", 60);
        loginAttemptsLimit = ConfigLoader.getInt("rate.limit.loginAttempts", 5);
        
        // Clean up old entries every minute
        scheduler.scheduleAtFixedRate(() -> {
            requestCounts.clear();
            logger.debug("Cleared rate limit counters");
        }, 1, 1, TimeUnit.MINUTES);
        
        // Clean up login attempts every 15 minutes
        scheduler.scheduleAtFixedRate(() -> {
            loginAttempts.clear();
            logger.debug("Cleared login attempt counters");
        }, 15, 15, TimeUnit.MINUTES);
        
        logger.info("Rate limiter initialized: enabled={}, requestsPerMinute={}, loginAttempts={}", 
            enabled, requestsPerMinute, loginAttemptsLimit);
    }
    
    /**
     * Check if request is allowed for the given identifier (IP or user)
     */
    public static boolean allowRequest(String identifier) {
        if (!enabled) {
            return true;
        }
        
        AtomicInteger count = requestCounts.computeIfAbsent(identifier, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        if (currentCount > requestsPerMinute) {
            logger.warn("Rate limit exceeded for: {}", identifier);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if login attempt is allowed
     */
    public static boolean allowLoginAttempt(String identifier) {
        if (!enabled) {
            return true;
        }
        
        AtomicInteger count = loginAttempts.computeIfAbsent(identifier, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        if (currentCount > loginAttemptsLimit) {
            logger.warn("Login rate limit exceeded for: {}", identifier);
            return false;
        }
        
        return true;
    }
    
    /**
     * Reset login attempts for identifier (after successful login)
     */
    public static void resetLoginAttempts(String identifier) {
        loginAttempts.remove(identifier);
    }
    
    /**
     * Get remaining requests for identifier
     */
    public static int getRemainingRequests(String identifier) {
        AtomicInteger count = requestCounts.get(identifier);
        if (count == null) {
            return requestsPerMinute;
        }
        return Math.max(0, requestsPerMinute - count.get());
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
