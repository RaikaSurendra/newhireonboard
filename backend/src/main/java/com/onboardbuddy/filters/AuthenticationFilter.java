package com.onboardbuddy.filters;

import com.onboardbuddy.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Authentication Filter to validate JWT tokens
 */
public class AuthenticationFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    
    // Endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token",
        "/api/health"
    );
    
    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("Authentication Filter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Get Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(httpResponse, "Missing or invalid Authorization header");
            return;
        }
        
        // Extract token
        String token = authHeader.substring(7);
        
        try {
            // Validate token
            if (JwtUtil.validateToken(token)) {
                // Extract user info and set as request attribute
                Long userId = JwtUtil.getUserIdFromToken(token);
                String role = JwtUtil.getRoleFromToken(token);
                
                httpRequest.setAttribute("userId", userId);
                httpRequest.setAttribute("userRole", role);
                
                chain.doFilter(request, response);
            } else {
                sendUnauthorized(httpResponse, "Invalid or expired token");
            }
        } catch (Exception e) {
            logger.error("Token validation error", e);
            sendUnauthorized(httpResponse, "Token validation failed");
        }
    }
    
    private boolean isPublicEndpoint(String uri) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(uri::startsWith);
    }
    
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"success\":false,\"error\":\"%s\"}", message
        ));
    }
    
    @Override
    public void destroy() {
        logger.info("Authentication Filter destroyed");
    }
}
