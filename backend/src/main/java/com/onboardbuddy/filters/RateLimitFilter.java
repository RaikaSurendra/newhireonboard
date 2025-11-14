package com.onboardbuddy.filters;

import com.onboardbuddy.utils.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rate limiting filter
 * Addresses security issue #23 - API rate limiting
 */
public class RateLimitFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("Rate Limit Filter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get identifier (IP address or user ID if authenticated)
        String identifier = getClientIdentifier(httpRequest);
        
        // Check rate limit
        if (!RateLimiter.allowRequest(identifier)) {
            sendRateLimitExceeded(httpResponse);
            return;
        }
        
        // Add rate limit headers
        int remaining = RateLimiter.getRemainingRequests(identifier);
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        
        chain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get user ID from request attribute (set by auth filter)
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }
        
        // Fall back to IP address
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first one
            ip = ip.split(",")[0].trim();
        }
        
        return "ip:" + ip;
    }
    
    private void sendRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"success\":false,\"error\":\"Rate limit exceeded. Please try again later.\"}"
        );
    }
    
    @Override
    public void destroy() {
        logger.info("Rate Limit Filter destroyed");
    }
}
