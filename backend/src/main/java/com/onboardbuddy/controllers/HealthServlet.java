package com.onboardbuddy.controllers;

import com.google.gson.Gson;
import com.onboardbuddy.config.DatabaseConfig;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint
 */
public class HealthServlet extends HttpServlet {
    
    private final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "Onboard Buddy");
        health.put("version", "1.0.0");
        health.put("timestamp", System.currentTimeMillis());
        
        // Check database connection
        try (Connection conn = DatabaseConfig.getConnection()) {
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("status", "DEGRADED");
        }
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(health));
    }
}
