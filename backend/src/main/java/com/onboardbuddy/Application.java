package com.onboardbuddy;

import com.onboardbuddy.config.ConfigLoader;
import com.onboardbuddy.config.DatabaseConfig;
import com.onboardbuddy.controllers.*;
import com.onboardbuddy.filters.CorsFilter;
import com.onboardbuddy.filters.AuthenticationFilter;
import com.onboardbuddy.filters.RateLimitFilter;
import com.onboardbuddy.filters.CsrfFilter;
import com.onboardbuddy.utils.RateLimiter;
import com.onboardbuddy.security.TokenBlacklist;
import com.onboardbuddy.security.CsrfTokenManager;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Main Application class for Onboard Buddy
 * Starts embedded Tomcat server and configures servlets
 */
public class Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static Properties config;
    private static Tomcat tomcat;
    
    public static void main(String[] args) {
        try {
            // Load configuration with environment variable support
            config = ConfigLoader.loadConfiguration();
            
            // Add shutdown hook for graceful shutdown
            addShutdownHook();
            
            // Initialize database
            DatabaseConfig.initialize();
            
            // Start Tomcat server
            startServer();
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
    
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            
            try {
                // Stop Tomcat
                if (tomcat != null) {
                    tomcat.stop();
                    tomcat.destroy();
                }
                
                // Close database connections
                DatabaseConfig.shutdown();
                
                // Shutdown rate limiter
                RateLimiter.shutdown();
                
                // Shutdown token blacklist
                TokenBlacklist.shutdown();
                
                // Shutdown CSRF token manager
                CsrfTokenManager.shutdown();
                
                logger.info("Application shutdown complete");
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }, "shutdown-hook"));
        
        logger.info("Shutdown hook registered");
    }
    
    private static void startServer() throws LifecycleException {
        int port = Integer.parseInt(config.getProperty("server.port", "8080"));
        
        tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();
        
        // Configure context
        String webappDir = "src/main/webapp/";
        File webappDirFile = new File(webappDir);
        
        Context ctx;
        if (webappDirFile.exists()) {
            ctx = tomcat.addWebapp("", webappDirFile.getAbsolutePath());
        } else {
            // For JAR deployment
            ctx = tomcat.addContext("", System.getProperty("java.io.tmpdir"));
        }
        
        ctx.setReloadable(false);
        
        // Add CORS Filter (first)
        ctx.addFilterDef(createFilterDef("CorsFilter", CorsFilter.class));
        ctx.addFilterMap(createFilterMap("CorsFilter", "/*"));
        
        // Add Rate Limit Filter (second)
        ctx.addFilterDef(createFilterDef("RateLimitFilter", RateLimitFilter.class));
        ctx.addFilterMap(createFilterMap("RateLimitFilter", "/api/*"));
        
        // Add Authentication Filter (third)
        ctx.addFilterDef(createFilterDef("AuthenticationFilter", AuthenticationFilter.class));
        ctx.addFilterMap(createFilterMap("AuthenticationFilter", "/api/*"));
        
        // Add CSRF Filter (fourth - after authentication)
        ctx.addFilterDef(createFilterDef("CsrfFilter", CsrfFilter.class));
        ctx.addFilterMap(createFilterMap("CsrfFilter", "/api/*"));
        
        // Register Servlets
        registerServlets(tomcat, ctx);
        
        // Start server
        tomcat.start();
        logger.info("===========================================");
        logger.info("Onboard Buddy Application Started");
        logger.info("Port: {}", port);
        logger.info("Environment: {}", config.getProperty("app.environment", "development"));
        logger.info("Access: http://localhost:{}", port);
        logger.info("===========================================");
        
        tomcat.getServer().await();
    }
    
    private static void registerServlets(Tomcat tomcat, Context ctx) {
        // Health Check
        Tomcat.addServlet(ctx, "HealthServlet", new HealthServlet());
        ctx.addServletMappingDecoded("/api/health", "HealthServlet");
        
        // Authentication
        Tomcat.addServlet(ctx, "AuthServlet", new AuthServlet());
        ctx.addServletMappingDecoded("/api/auth/*", "AuthServlet");
        
        // Users
        Tomcat.addServlet(ctx, "UserServlet", new UserServlet());
        ctx.addServletMappingDecoded("/api/users/*", "UserServlet");
        
        // Buddy Matches
        Tomcat.addServlet(ctx, "BuddyMatchServlet", new BuddyMatchServlet());
        ctx.addServletMappingDecoded("/api/matches/*", "BuddyMatchServlet");
        
        // Onboarding Plans
        Tomcat.addServlet(ctx, "PlanServlet", new PlanServlet());
        ctx.addServletMappingDecoded("/api/plans/*", "PlanServlet");
        
        // Task Templates - handle as part of PlanServlet path
        Tomcat.addServlet(ctx, "TaskTemplateServlet", new TaskTemplateServlet());
        ctx.addServletMappingDecoded("/api/templates/*", "TaskTemplateServlet");
        
        // Tasks
        Tomcat.addServlet(ctx, "TaskServlet", new TaskServlet());
        ctx.addServletMappingDecoded("/api/tasks/*", "TaskServlet");
        
        // Messages
        Tomcat.addServlet(ctx, "MessageServlet", new MessageServlet());
        ctx.addServletMappingDecoded("/api/messages/*", "MessageServlet");
        
        // Feedback
        Tomcat.addServlet(ctx, "FeedbackServlet", new FeedbackServlet());
        ctx.addServletMappingDecoded("/api/feedback/*", "FeedbackServlet");
        
        // Notifications
        Tomcat.addServlet(ctx, "NotificationServlet", new NotificationServlet());
        ctx.addServletMappingDecoded("/api/notifications/*", "NotificationServlet");
        
        // SPA Servlet - catches all non-API routes for React Router
        Tomcat.addServlet(ctx, "SpaServlet", new SpaServlet());
        ctx.addServletMappingDecoded("/*", "SpaServlet");
        
        logger.info("Registered {} servlets", 11);
    }
    
    private static org.apache.tomcat.util.descriptor.web.FilterDef createFilterDef(
            String filterName, Class<?> filterClass) {
        org.apache.tomcat.util.descriptor.web.FilterDef filterDef = 
            new org.apache.tomcat.util.descriptor.web.FilterDef();
        filterDef.setFilterName(filterName);
        filterDef.setFilterClass(filterClass.getName());
        return filterDef;
    }
    
    private static org.apache.tomcat.util.descriptor.web.FilterMap createFilterMap(
            String filterName, String urlPattern) {
        org.apache.tomcat.util.descriptor.web.FilterMap filterMap = 
            new org.apache.tomcat.util.descriptor.web.FilterMap();
        filterMap.setFilterName(filterName);
        filterMap.addURLPattern(urlPattern);
        return filterMap;
    }
    
    public static Properties getConfig() {
        return config;
    }
}
