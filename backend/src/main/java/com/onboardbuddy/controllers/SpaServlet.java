package com.onboardbuddy.controllers;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Servlet to handle Single Page Application routing
 * Serves index.html for all non-API routes
 */
public class SpaServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI();
        
        // If it's a static file (with extension), try to serve it
        if (path.contains(".") && !path.equals("/")) {
            // Try to load from classpath
            String resourcePath = path.startsWith("/") ? path.substring(1) : path;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    // Set content type based on extension
                    String contentType = getContentTypeFromPath(path);
                    resp.setContentType(contentType);
                    
                    try (OutputStream os = resp.getOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    return;
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
                    return;
                }
            }
        }
        
        // For all other routes (like /, /login, /dashboard), serve index.html
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        // Try to load index.html from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("index.html")) {
            if (is != null) {
                try (OutputStream os = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "index.html not found in classpath");
            }
        }
    }
    
    /**
     * Get content type based on file extension
     */
    private String getContentTypeFromPath(String path) {
        if (path.endsWith(".js")) {
            return "application/javascript";
        } else if (path.endsWith(".css")) {
            return "text/css";
        } else if (path.endsWith(".html")) {
            return "text/html";
        } else if (path.endsWith(".json")) {
            return "application/json";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (path.endsWith(".woff") || path.endsWith(".woff2")) {
            return "font/woff2";
        } else if (path.endsWith(".ttf")) {
            return "font/ttf";
        } else {
            return "application/octet-stream";
        }
    }
}
