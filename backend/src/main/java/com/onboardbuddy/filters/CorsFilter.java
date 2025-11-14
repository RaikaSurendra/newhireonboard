package com.onboardbuddy.filters;

import com.onboardbuddy.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

/**
 * CORS Filter to handle Cross-Origin Resource Sharing
 */
public class CorsFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);
    private String allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private String maxAge;
    
    @Override
    public void init(FilterConfig filterConfig) {
        Properties config = Application.getConfig();
        allowedOrigins = config.getProperty("cors.allowedOrigins", "*");
        allowedMethods = config.getProperty("cors.allowedMethods", "GET,POST,PUT,DELETE,OPTIONS");
        allowedHeaders = config.getProperty("cors.allowedHeaders", "*");
        maxAge = config.getProperty("cors.maxAge", "3600");
        
        logger.info("CORS Filter initialized");
        logger.info("Allowed Origins: {}", allowedOrigins);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String origin = httpRequest.getHeader("Origin");
        
        // Set CORS headers
        if ("*".equals(allowedOrigins)) {
            // When using wildcard, echo back the origin and don't use credentials
            if (origin != null) {
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            } else {
                httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            }
        } else if (origin != null && isOriginAllowed(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        httpResponse.setHeader("Access-Control-Allow-Methods", allowedMethods);
        httpResponse.setHeader("Access-Control-Allow-Headers", allowedHeaders);
        httpResponse.setHeader("Access-Control-Max-Age", maxAge);
        
        // Handle preflight request
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isOriginAllowed(String origin) {
        if ("*".equals(allowedOrigins)) {
            return true;
        }
        String[] origins = allowedOrigins.split(",");
        for (String allowed : origins) {
            if (allowed.trim().equals(origin)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void destroy() {
        logger.info("CORS Filter destroyed");
    }
}
