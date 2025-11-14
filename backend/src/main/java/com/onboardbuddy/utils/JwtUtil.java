package com.onboardbuddy.utils;

import com.onboardbuddy.config.ConfigLoader;
import com.onboardbuddy.security.TokenBlacklist;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Utility class for token generation and validation
 * Updated to fix initialization issues and add token revocation support
 */
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static SecretKey secretKey;
    private static long expiration;
    private static long refreshExpiration;
    private static String issuer;
    
    // Lazy initialization to avoid static initialization issues
    private static void ensureInitialized() {
        if (secretKey == null) {
            synchronized (JwtUtil.class) {
                if (secretKey == null) {
                    String secret = ConfigLoader.get("jwt.secret");
                    if (secret == null || secret.length() < 32) {
                        throw new IllegalStateException("JWT secret not properly configured");
                    }
                    secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                    expiration = Long.parseLong(ConfigLoader.get("jwt.expiration", "86400000"));
                    refreshExpiration = Long.parseLong(ConfigLoader.get("jwt.refreshExpiration", "604800000"));
                    issuer = ConfigLoader.get("jwt.issuer", "onboard-buddy-app");
                    logger.info("JWT utility initialized");
                }
            }
        }
    }
    
    public static String generateToken(Long userId, String email, String role) {
        ensureInitialized();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public static String generateRefreshToken(Long userId) {
        ensureInitialized();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public static boolean validateToken(String token) {
        ensureInitialized();
        
        // Check if token is blacklisted
        if (TokenBlacklist.isBlacklisted(token)) {
            logger.warn("Token is blacklisted");
            return false;
        }
        
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    public static Long getUserIdFromToken(String token) {
        ensureInitialized();
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return Long.parseLong(claims.getSubject());
    }
    
    public static String getRoleFromToken(String token) {
        ensureInitialized();
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("role", String.class);
    }
    
    public static String getEmailFromToken(String token) {
        ensureInitialized();
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("email", String.class);
    }
    
    public static Date getExpirationFromToken(String token) {
        ensureInitialized();
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getExpiration();
    }
    
    /**
     * Revoke a token by adding it to the blacklist
     */
    public static void revokeToken(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            TokenBlacklist.blacklistToken(token, expiration.getTime());
            logger.info("Token revoked successfully");
        } catch (Exception e) {
            logger.error("Failed to revoke token", e);
        }
    }
}
