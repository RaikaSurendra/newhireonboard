package com.onboardbuddy.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.onboardbuddy.config.DatabaseConfig;
import com.onboardbuddy.utils.JwtUtil;
import com.onboardbuddy.utils.ValidationUtil;
import com.onboardbuddy.utils.RateLimiter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 404, "Endpoint not found");
            return;
        }

        try {
            if (pathInfo.equals("/login")) {
                handleLogin(req, resp);
            } else if (pathInfo.equals("/register")) {
                handleRegister(req, resp);
            } else if (pathInfo.equals("/logout")) {
                handleLogout(req, resp);
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AuthServlet", e);
            sendError(resp, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Get client identifier for rate limiting
        String clientIp = getClientIp(req);
        
        // Check rate limit
        if (!RateLimiter.allowLoginAttempt(clientIp)) {
            logger.warn("Login rate limit exceeded for IP: {}", clientIp);
            sendError(resp, 429, "Too many login attempts. Please try again later.");
            return;
        }
        
        // Read request body with size limit
        String requestBodyStr = readRequestBody(req, 1024); // 1KB limit
        if (requestBodyStr == null) {
            sendError(resp, 400, "Request body too large or invalid");
            return;
        }

        JsonObject requestBody;
        try {
            requestBody = gson.fromJson(requestBodyStr, JsonObject.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON in login request");
            sendError(resp, 400, "Invalid request format");
            return;
        }
        
        // Validate required fields
        if (!requestBody.has("email") || !requestBody.has("password")) {
            sendError(resp, 400, "Email and password are required");
            return;
        }
        
        String email = requestBody.get("email").getAsString();
        String password = requestBody.get("password").getAsString();
        
        // Validate email format
        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            sendError(resp, 400, emailValidation.getMessage());
            return;
        }

        logger.info("Login attempt for user");

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, email, name, password_hash, role, department FROM users WHERE email = ? AND status = 'ACTIVE'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String passwordHash = rs.getString("password_hash");
                        
                        // Verify password
                        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), passwordHash);
                        
                        if (result.verified) {
                            // Generate JWT token
                            int userId = rs.getInt("id");
                            String userName = rs.getString("name");
                            String userRole = rs.getString("role");
                            String department = rs.getString("department");
                            
                            String token = JwtUtil.generateToken(Long.valueOf(userId), email, userRole);
                            String refreshToken = JwtUtil.generateRefreshToken(Long.valueOf(userId));
                            
                            // Reset login attempts on successful login
                            RateLimiter.resetLoginAttempts(clientIp);
                            
                            // Build response
                            JsonObject user = new JsonObject();
                            user.addProperty("id", userId);
                            user.addProperty("email", email);
                            user.addProperty("name", userName);
                            user.addProperty("role", userRole);
                            user.addProperty("department", department);
                            
                            JsonObject data = new JsonObject();
                            data.addProperty("token", token);
                            data.addProperty("refreshToken", refreshToken);
                            data.add("user", user);
                            
                            JsonObject response = new JsonObject();
                            response.addProperty("success", true);
                            response.add("data", data);
                            
                            logger.info("Login successful for user ID: {}", userId);
                            resp.setStatus(200);
                            resp.getWriter().write(gson.toJson(response));
                        } else {
                            logger.warn("Invalid password attempt from IP: {}", clientIp);
                            sendError(resp, 401, "Invalid email or password");
                        }
                    } else {
                        logger.warn("User not found from IP: {}", clientIp);
                        sendError(resp, 401, "Invalid email or password");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error during login", e);
            sendError(resp, 500, "An error occurred. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            sendError(resp, 500, "An error occurred. Please try again later.");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Read request body with size limit
        String requestBodyStr = readRequestBody(req, 2048); // 2KB limit
        if (requestBodyStr == null) {
            sendError(resp, 400, "Request body too large or invalid");
            return;
        }

        JsonObject requestBody;
        try {
            requestBody = gson.fromJson(requestBodyStr, JsonObject.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON in registration request");
            sendError(resp, 400, "Invalid request format");
            return;
        }
        
        // Validate required fields
        if (!requestBody.has("email") || !requestBody.has("password") || 
            !requestBody.has("name") || !requestBody.has("role")) {
            sendError(resp, 400, "Email, password, name, and role are required");
            return;
        }
        
        String email = requestBody.get("email").getAsString();
        String password = requestBody.get("password").getAsString();
        String name = requestBody.get("name").getAsString();
        String role = requestBody.get("role").getAsString();
        String department = requestBody.has("department") ? requestBody.get("department").getAsString() : null;
        
        // Validate all inputs
        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            sendError(resp, 400, emailValidation.getMessage());
            return;
        }
        
        ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(password);
        if (!passwordValidation.isValid()) {
            sendError(resp, 400, passwordValidation.getMessage());
            return;
        }
        
        ValidationUtil.ValidationResult nameValidation = ValidationUtil.validateName(name);
        if (!nameValidation.isValid()) {
            sendError(resp, 400, nameValidation.getMessage());
            return;
        }
        
        ValidationUtil.ValidationResult roleValidation = ValidationUtil.validateRole(role);
        if (!roleValidation.isValid()) {
            sendError(resp, 400, roleValidation.getMessage());
            return;
        }
        
        if (department != null) {
            ValidationUtil.ValidationResult deptValidation = ValidationUtil.validateDepartment(department);
            if (!deptValidation.isValid()) {
                sendError(resp, 400, deptValidation.getMessage());
                return;
            }
        }

        logger.info("Registration attempt");

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check if user already exists
            String checkSql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        sendError(resp, 400, "User with this email already exists");
                        return;
                    }
                }
            }

            // Hash password
            String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            // Insert new user
            String insertSql = "INSERT INTO users (email, password_hash, name, role, department, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, email);
                insertStmt.setString(2, passwordHash);
                insertStmt.setString(3, name);
                insertStmt.setString(4, role);
                insertStmt.setString(5, department);
                
                int affectedRows = insertStmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int userId = generatedKeys.getInt(1);
                            
                            // Generate tokens
                            String token = JwtUtil.generateToken(Long.valueOf(userId), email, role);
                            String refreshToken = JwtUtil.generateRefreshToken(Long.valueOf(userId));
                            
                            // Build response
                            JsonObject user = new JsonObject();
                            user.addProperty("id", userId);
                            user.addProperty("email", email);
                            user.addProperty("name", name);
                            user.addProperty("role", role);
                            user.addProperty("department", department);
                            
                            JsonObject data = new JsonObject();
                            data.addProperty("token", token);
                            data.addProperty("refreshToken", refreshToken);
                            data.add("user", user);
                            
                            JsonObject response = new JsonObject();
                            response.addProperty("success", true);
                            response.add("data", data);
                            
                            logger.info("Registration successful for user ID: {}", userId);
                            resp.setStatus(201);
                            resp.getWriter().write(gson.toJson(response));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error during registration", e);
            sendError(resp, 500, "An error occurred. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            sendError(resp, 500, "An error occurred. Please try again later.");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Get token from header and revoke it
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            JwtUtil.revokeToken(token);
            logger.info("User logged out and token revoked");
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Logged out successfully");
        
        resp.setStatus(200);
        resp.getWriter().write(gson.toJson(response));
    }
    
    /**
     * Read request body with size limit to prevent DoS
     */
    private String readRequestBody(HttpServletRequest req, int maxSize) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            int totalSize = 0;
            while ((line = reader.readLine()) != null) {
                totalSize += line.length();
                if (totalSize > maxSize) {
                    logger.warn("Request body exceeds size limit: {}", maxSize);
                    return null;
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }
    
    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(error));
    }
}
