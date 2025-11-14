package com.onboardbuddy.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.onboardbuddy.config.DatabaseConfig;
import com.onboardbuddy.utils.ValidationUtil;
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

/**
 * User Management Servlet
 * Handles user CRUD operations
 */
public class UserServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(UserServlet.class);
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/users - List all users
                handleListUsers(req, resp);
            } else {
                // GET /api/users/{id} - Get specific user
                String userId = pathInfo.substring(1);
                handleGetUser(req, resp, userId);
            }
        } catch (Exception e) {
            logger.error("Error in UserServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 400, "User ID is required");
            return;
        }

        try {
            String userId = pathInfo.substring(1);
            handleUpdateUser(req, resp, userId);
        } catch (Exception e) {
            logger.error("Error in UserServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 400, "User ID is required");
            return;
        }

        try {
            String userId = pathInfo.substring(1);
            handleDeleteUser(req, resp, userId);
        } catch (Exception e) {
            logger.error("Error in UserServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check authorization - only admins and HR can list all users
        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        // Get pagination parameters
        int page = getIntParameter(req, "page", 1);
        int limit = getIntParameter(req, "limit", 20);
        int offset = (page - 1) * limit;

        // Get filter parameters
        String role = req.getParameter("role");
        String department = req.getParameter("department");
        String status = req.getParameter("status");

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Build query with filters
            StringBuilder sql = new StringBuilder(
                "SELECT id, email, name, role, department, status, phone, created_at " +
                "FROM users WHERE 1=1"
            );
            
            if (role != null && !role.isEmpty()) {
                sql.append(" AND role = ?");
            }
            if (department != null && !department.isEmpty()) {
                sql.append(" AND department = ?");
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
            }
            
            sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                
                if (role != null && !role.isEmpty()) {
                    stmt.setString(paramIndex++, role);
                }
                if (department != null && !department.isEmpty()) {
                    stmt.setString(paramIndex++, department);
                }
                if (status != null && !status.isEmpty()) {
                    stmt.setString(paramIndex++, status);
                }
                
                stmt.setInt(paramIndex++, limit);
                stmt.setInt(paramIndex, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray users = new JsonArray();
                    
                    while (rs.next()) {
                        JsonObject user = new JsonObject();
                        user.addProperty("id", rs.getLong("id"));
                        user.addProperty("email", rs.getString("email"));
                        user.addProperty("name", rs.getString("name"));
                        user.addProperty("role", rs.getString("role"));
                        user.addProperty("department", rs.getString("department"));
                        user.addProperty("status", rs.getString("status"));
                        user.addProperty("phone", rs.getString("phone"));
                        user.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                        users.add(user);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", users);
                    response.addProperty("page", page);
                    response.addProperty("limit", limit);

                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error listing users", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleGetUser(HttpServletRequest req, HttpServletResponse resp, String userId) throws IOException {
        // Users can only view their own profile unless they're admin/HR
        Long requestingUserId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");
        
        boolean isAdmin = "ADMIN".equals(userRole) || "HR_MANAGER".equals(userRole);
        boolean isSelf = requestingUserId.toString().equals(userId);
        
        if (!isAdmin && !isSelf) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, email, name, role, department, status, phone, " +
                        "experience_level, avatar_url, created_at, last_login " +
                        "FROM users WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(userId));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject user = new JsonObject();
                        user.addProperty("id", rs.getLong("id"));
                        user.addProperty("email", rs.getString("email"));
                        user.addProperty("name", rs.getString("name"));
                        user.addProperty("role", rs.getString("role"));
                        user.addProperty("department", rs.getString("department"));
                        user.addProperty("status", rs.getString("status"));
                        user.addProperty("phone", rs.getString("phone"));
                        user.addProperty("experienceLevel", rs.getString("experience_level"));
                        user.addProperty("avatarUrl", rs.getString("avatar_url"));
                        user.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                        if (rs.getTimestamp("last_login") != null) {
                            user.addProperty("lastLogin", rs.getTimestamp("last_login").toString());
                        }

                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.add("data", user);

                        resp.setStatus(200);
                        resp.getWriter().write(gson.toJson(response));
                    } else {
                        sendError(resp, 404, "User not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error getting user", e);
            sendError(resp, 500, "An error occurred");
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid user ID");
        }
    }

    private void handleUpdateUser(HttpServletRequest req, HttpServletResponse resp, String userId) throws IOException {
        // Users can only update their own profile unless they're admin
        Long requestingUserId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");
        
        boolean isAdmin = "ADMIN".equals(userRole);
        boolean isSelf = requestingUserId.toString().equals(userId);
        
        if (!isAdmin && !isSelf) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        // Read request body
        String requestBodyStr = readRequestBody(req, 2048);
        if (requestBodyStr == null) {
            sendError(resp, 400, "Request body too large or invalid");
            return;
        }

        JsonObject requestBody;
        try {
            requestBody = gson.fromJson(requestBodyStr, JsonObject.class);
        } catch (JsonSyntaxException e) {
            sendError(resp, 400, "Invalid request format");
            return;
        }

        // Validate inputs if provided
        if (requestBody.has("name")) {
            ValidationUtil.ValidationResult nameValidation = 
                ValidationUtil.validateName(requestBody.get("name").getAsString());
            if (!nameValidation.isValid()) {
                sendError(resp, 400, nameValidation.getMessage());
                return;
            }
        }

        if (requestBody.has("phone")) {
            ValidationUtil.ValidationResult phoneValidation = 
                ValidationUtil.validatePhone(requestBody.get("phone").getAsString());
            if (!phoneValidation.isValid()) {
                sendError(resp, 400, phoneValidation.getMessage());
                return;
            }
        }

        if (requestBody.has("department")) {
            ValidationUtil.ValidationResult deptValidation = 
                ValidationUtil.validateDepartment(requestBody.get("department").getAsString());
            if (!deptValidation.isValid()) {
                sendError(resp, 400, deptValidation.getMessage());
                return;
            }
        }

        // Non-admins cannot change role or status
        if (!isAdmin && (requestBody.has("role") || requestBody.has("status"))) {
            sendError(resp, 403, "Only admins can change role or status");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Build dynamic UPDATE query
            StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = NOW()");
            
            if (requestBody.has("name")) {
                sql.append(", name = ?");
            }
            if (requestBody.has("phone")) {
                sql.append(", phone = ?");
            }
            if (requestBody.has("department")) {
                sql.append(", department = ?");
            }
            if (requestBody.has("avatarUrl")) {
                sql.append(", avatar_url = ?");
            }
            if (isAdmin && requestBody.has("role")) {
                sql.append(", role = ?");
            }
            if (isAdmin && requestBody.has("status")) {
                sql.append(", status = ?");
            }
            
            sql.append(" WHERE id = ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                
                if (requestBody.has("name")) {
                    stmt.setString(paramIndex++, requestBody.get("name").getAsString());
                }
                if (requestBody.has("phone")) {
                    stmt.setString(paramIndex++, requestBody.get("phone").getAsString());
                }
                if (requestBody.has("department")) {
                    stmt.setString(paramIndex++, requestBody.get("department").getAsString());
                }
                if (requestBody.has("avatarUrl")) {
                    stmt.setString(paramIndex++, requestBody.get("avatarUrl").getAsString());
                }
                if (isAdmin && requestBody.has("role")) {
                    stmt.setString(paramIndex++, requestBody.get("role").getAsString());
                }
                if (isAdmin && requestBody.has("status")) {
                    stmt.setString(paramIndex++, requestBody.get("status").getAsString());
                }
                
                stmt.setLong(paramIndex, Long.parseLong(userId));

                int updated = stmt.executeUpdate();
                
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "User updated successfully");

                    logger.info("User updated: {}", userId);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "User not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error updating user", e);
            sendError(resp, 500, "An error occurred");
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid user ID");
        }
    }

    private void handleDeleteUser(HttpServletRequest req, HttpServletResponse resp, String userId) throws IOException {
        // Only admins can delete users
        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            sendError(resp, 403, "Only admins can delete users");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Soft delete - set status to INACTIVE
            String sql = "UPDATE users SET status = 'INACTIVE', updated_at = NOW() WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(userId));
                
                int updated = stmt.executeUpdate();
                
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "User deactivated successfully");

                    logger.info("User deactivated: {}", userId);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "User not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error deleting user", e);
            sendError(resp, 500, "An error occurred");
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid user ID");
        }
    }

    private String readRequestBody(HttpServletRequest req, int maxSize) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            int totalSize = 0;
            while ((line = reader.readLine()) != null) {
                totalSize += line.length();
                if (totalSize > maxSize) {
                    return null;
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private int getIntParameter(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(error));
    }
}
