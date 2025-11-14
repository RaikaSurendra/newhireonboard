package com.onboardbuddy.filters;

import com.onboardbuddy.security.CsrfTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CSRF Protection Filter
 * Addresses security issue #6 - CSRF protection
 */
public class CsrfFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);
    
    // Methods that require CSRF protection
    private static final List<String> PROTECTED_METHODS = Arrays.asList("POST", "PUT", "DELETE", "PATCH");
    
    // Endpoints that don't require CSRF protection
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token",
        "/api/health"
    );
    
    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("CSRF Filter initialized: enabled={}", CsrfTokenManager.isEnabled());
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();
        
        // Skip CSRF check for safe methods (GET, HEAD, OPTIONS)
        if (!PROTECTED_METHODS.contains(method)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Skip CSRF check for excluded paths
        if (isExcludedPath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Skip if CSRF is disabled
        if (!CsrfTokenManager.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        
        // Get CSRF token from header
        String csrfToken = httpRequest.getHeader("X-CSRF-Token");
        if (csrfToken == null) {
            csrfToken = httpRequest.getHeader("X-XSRF-Token"); // Alternative header name
        }
        
        // Get user ID from request attribute (set by authentication filter)
        Object userIdObj = httpRequest.getAttribute("userId");
        if (userIdObj == null) {
            logger.warn("CSRF check failed: No user ID in request");
            sendCsrfError(httpResponse);
            return;
        }
        
        String userId = userIdObj.toString();
        
        // Validate CSRF token
        if (!CsrfTokenManager.validateToken(csrfToken, userId)) {
            logger.warn("CSRF validation failed for user: {} on path: {}", userId, requestURI);
            sendCsrfError(httpResponse);
            return;
        }
        
        // CSRF token is valid, proceed with request
        chain.doFilter(request, response);
    }
    
    private boolean isExcludedPath(String uri) {
        return EXCLUDED_PATHS.stream().anyMatch(uri::startsWith);
    }
    
    private void sendCsrfError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"success\":false,\"error\":\"CSRF token validation failed\"}"
        );
    }
    
    @Override
    public void destroy() {
        logger.info("CSRF Filter destroyed");
    }
}
