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
import java.sql.*;

/**
 * Task Management Servlet
 * Handles task CRUD operations, status management, and task history
 */
public class TaskServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TaskServlet.class);
    private final Gson gson = new Gson();
    private static final int MAX_REQUEST_SIZE = 8192; // 8KB

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleListTasks(req, resp);
            } else {
                String taskId = pathInfo.substring(1);
                handleGetTask(req, resp, taskId);
            }
        } catch (Exception e) {
            logger.error("Error in TaskServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            handleCreateTask(req, resp);
        } catch (Exception e) {
            logger.error("Error in TaskServlet POST", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 400, "Task ID is required");
            return;
        }

        try {
            if (pathInfo.contains("/status")) {
                String taskId = pathInfo.substring(1, pathInfo.indexOf("/status"));
                handleUpdateTaskStatus(req, resp, taskId);
            } else {
                String taskId = pathInfo.substring(1);
                handleUpdateTask(req, resp, taskId);
            }
        } catch (Exception e) {
            logger.error("Error in TaskServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 400, "Task ID is required");
            return;
        }

        try {
            String taskId = pathInfo.substring(1);
            handleDeleteTask(req, resp, taskId);
        } catch (Exception e) {
            logger.error("Error in TaskServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListTasks(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");

        String assignedTo = req.getParameter("assignedTo");
        String status = req.getParameter("status");
        String runId = req.getParameter("runId");
        String priority = req.getParameter("priority");
        
        int page = getIntParameter(req, "page", 1);
        int limit = getIntParameter(req, "limit", 20);
        int offset = (page - 1) * limit;

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT t.id, t.title, t.description, t.status, t.priority, " +
                "t.assigned_to, t.assigned_to_type, t.due_date, t.completed_at, " +
                "t.execution_mode, t.sequence_order, t.is_blocked, t.created_at, " +
                "u.name as assigned_to_name, t.onboarding_run_id " +
                "FROM tasks t " +
                "LEFT JOIN users u ON t.assigned_to = u.id " +
                "WHERE 1=1"
            );
            
            if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
                sql.append(" AND (t.assigned_to = ? OR t.created_by = ?)");
            }
            
            if (assignedTo != null) sql.append(" AND t.assigned_to = ?");
            if (status != null) sql.append(" AND t.status = ?");
            if (runId != null) sql.append(" AND t.onboarding_run_id = ?");
            if (priority != null) sql.append(" AND t.priority = ?");
            
            sql.append(" ORDER BY t.due_date ASC, t.priority DESC LIMIT ? OFFSET ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                
                if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
                    stmt.setLong(paramIndex++, userId);
                    stmt.setLong(paramIndex++, userId);
                }
                
                if (assignedTo != null) stmt.setLong(paramIndex++, Long.parseLong(assignedTo));
                if (status != null) stmt.setString(paramIndex++, status);
                if (runId != null) stmt.setLong(paramIndex++, Long.parseLong(runId));
                if (priority != null) stmt.setString(paramIndex++, priority);
                
                stmt.setInt(paramIndex++, limit);
                stmt.setInt(paramIndex, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray tasks = new JsonArray();
                    
                    while (rs.next()) {
                        JsonObject task = new JsonObject();
                        task.addProperty("id", rs.getLong("id"));
                        task.addProperty("title", rs.getString("title"));
                        task.addProperty("description", rs.getString("description"));
                        task.addProperty("status", rs.getString("status"));
                        task.addProperty("priority", rs.getString("priority"));
                        task.addProperty("assignedTo", rs.getLong("assigned_to"));
                        task.addProperty("assignedToName", rs.getString("assigned_to_name"));
                        task.addProperty("assignedToType", rs.getString("assigned_to_type"));
                        task.addProperty("onboardingRunId", rs.getLong("onboarding_run_id"));
                        
                        Date dueDate = rs.getDate("due_date");
                        if (dueDate != null) task.addProperty("dueDate", dueDate.toString());
                        
                        Timestamp completedAt = rs.getTimestamp("completed_at");
                        if (completedAt != null) task.addProperty("completedAt", completedAt.toString());
                        
                        task.addProperty("executionMode", rs.getString("execution_mode"));
                        task.addProperty("sequenceOrder", rs.getInt("sequence_order"));
                        task.addProperty("isBlocked", rs.getBoolean("is_blocked"));
                        task.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                        
                        tasks.add(task);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", tasks);
                    response.addProperty("page", page);
                    response.addProperty("limit", limit);

                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error listing tasks", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleGetTask(HttpServletRequest req, HttpServletResponse resp, String taskId) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT t.*, u.name as assigned_to_name, c.name as created_by_name " +
                "FROM tasks t " +
                "LEFT JOIN users u ON t.assigned_to = u.id " +
                "LEFT JOIN users c ON t.created_by = c.id " +
                "WHERE t.id = ?"
            );
            
            if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
                sql.append(" AND (t.assigned_to = ? OR t.created_by = ?)");
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                stmt.setLong(1, Long.parseLong(taskId));
                
                if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
                    stmt.setLong(2, userId);
                    stmt.setLong(3, userId);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject task = buildTaskJson(rs);
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.add("data", task);
                        resp.setStatus(200);
                        resp.getWriter().write(gson.toJson(response));
                    } else {
                        sendError(resp, 404, "Task not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error getting task", e);
            sendError(resp, 500, "An error occurred");
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid task ID");
        }
    }

    private void handleCreateTask(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");

        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole) && !"MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        String requestBodyStr = readRequestBody(req, MAX_REQUEST_SIZE);
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

        if (!requestBody.has("onboardingRunId") || !requestBody.has("title") || !requestBody.has("assignedTo")) {
            sendError(resp, 400, "onboardingRunId, title, and assignedTo are required");
            return;
        }

        ValidationUtil.ValidationResult titleValidation = 
            ValidationUtil.validateLength(requestBody.get("title").getAsString(), "Title", 255);
        if (!titleValidation.isValid()) {
            sendError(resp, 400, titleValidation.getMessage());
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO tasks (onboarding_run_id, template_id, title, description, " +
                        "status, priority, assigned_to, assigned_to_type, created_by, due_date, " +
                        "execution_mode, sequence_order, parallel_group, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, requestBody.get("onboardingRunId").getAsLong());
                stmt.setObject(2, requestBody.has("templateId") ? requestBody.get("templateId").getAsLong() : null);
                stmt.setString(3, requestBody.get("title").getAsString());
                stmt.setString(4, requestBody.has("description") ? requestBody.get("description").getAsString() : null);
                stmt.setString(5, requestBody.has("status") ? requestBody.get("status").getAsString() : "PENDING");
                stmt.setString(6, requestBody.has("priority") ? requestBody.get("priority").getAsString() : "MEDIUM");
                stmt.setLong(7, requestBody.get("assignedTo").getAsLong());
                stmt.setString(8, requestBody.has("assignedToType") ? requestBody.get("assignedToType").getAsString() : "NEW_EMPLOYEE");
                stmt.setLong(9, userId);
                
                if (requestBody.has("dueDate")) {
                    stmt.setDate(10, Date.valueOf(requestBody.get("dueDate").getAsString()));
                } else {
                    stmt.setNull(10, Types.DATE);
                }
                
                stmt.setString(11, requestBody.has("executionMode") ? requestBody.get("executionMode").getAsString() : "PARALLEL");
                stmt.setObject(12, requestBody.has("sequenceOrder") ? requestBody.get("sequenceOrder").getAsInt() : null);
                stmt.setString(13, requestBody.has("parallelGroup") ? requestBody.get("parallelGroup").getAsString() : null);
                
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long taskId = generatedKeys.getLong(1);
                            
                            JsonObject response = new JsonObject();
                            response.addProperty("success", true);
                            response.addProperty("taskId", taskId);
                            response.addProperty("message", "Task created successfully");

                            logger.info("Task created: {} by user: {}", taskId, userId);
                            resp.setStatus(201);
                            resp.getWriter().write(gson.toJson(response));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error creating task", e);
            sendError(resp, 500, "An error occurred");
        } catch (Exception e) {
            logger.error("Error creating task", e);
            sendError(resp, 400, "Invalid data format");
        }
    }

    private void handleUpdateTask(HttpServletRequest req, HttpServletResponse resp, String taskId) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");

        String requestBodyStr = readRequestBody(req, MAX_REQUEST_SIZE);
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

        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!canModifyTask(conn, taskId, userId, userRole)) {
                sendError(resp, 403, "Insufficient permissions");
                return;
            }

            StringBuilder sql = new StringBuilder("UPDATE tasks SET updated_at = NOW()");
            
            if (requestBody.has("title")) sql.append(", title = ?");
            if (requestBody.has("description")) sql.append(", description = ?");
            if (requestBody.has("priority")) sql.append(", priority = ?");
            if (requestBody.has("assignedTo")) sql.append(", assigned_to = ?");
            if (requestBody.has("dueDate")) sql.append(", due_date = ?");
            
            sql.append(" WHERE id = ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                
                if (requestBody.has("title")) stmt.setString(paramIndex++, requestBody.get("title").getAsString());
                if (requestBody.has("description")) stmt.setString(paramIndex++, requestBody.get("description").getAsString());
                if (requestBody.has("priority")) stmt.setString(paramIndex++, requestBody.get("priority").getAsString());
                if (requestBody.has("assignedTo")) stmt.setLong(paramIndex++, requestBody.get("assignedTo").getAsLong());
                if (requestBody.has("dueDate")) stmt.setDate(paramIndex++, Date.valueOf(requestBody.get("dueDate").getAsString()));
                
                stmt.setLong(paramIndex, Long.parseLong(taskId));

                int updated = stmt.executeUpdate();
                
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Task updated successfully");
                    logger.info("Task updated: {}", taskId);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Task not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error updating task", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleUpdateTaskStatus(HttpServletRequest req, HttpServletResponse resp, String taskId) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        String userRole = (String) req.getAttribute("userRole");

        String requestBodyStr = readRequestBody(req, 1024);
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

        if (!requestBody.has("status")) {
            sendError(resp, 400, "Status is required");
            return;
        }

        String newStatus = requestBody.get("status").getAsString();

        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!canModifyTask(conn, taskId, userId, userRole)) {
                sendError(resp, 403, "Insufficient permissions");
                return;
            }

            String sql = "UPDATE tasks SET status = ?, updated_at = NOW()";
            if ("COMPLETED".equals(newStatus)) {
                sql += ", completed_at = NOW()";
            }
            sql += " WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newStatus);
                stmt.setLong(2, Long.parseLong(taskId));

                int updated = stmt.executeUpdate();
                
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Task status updated successfully");
                    logger.info("Task status updated: {} to {}", taskId, newStatus);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Task not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error updating task status", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleDeleteTask(HttpServletRequest req, HttpServletResponse resp, String taskId) throws IOException {
        String userRole = (String) req.getAttribute("userRole");

        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
            sendError(resp, 403, "Only admins can delete tasks");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE tasks SET status = 'CANCELLED', updated_at = NOW() WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(taskId));
                
                int updated = stmt.executeUpdate();
                
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Task cancelled successfully");
                    logger.info("Task cancelled: {}", taskId);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Task not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error deleting task", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private boolean canModifyTask(Connection conn, String taskId, Long userId, String userRole) throws SQLException {
        if ("ADMIN".equals(userRole) || "HR_MANAGER".equals(userRole)) {
            return true;
        }

        String sql = "SELECT assigned_to, created_by FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(taskId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long assignedTo = rs.getLong("assigned_to");
                    long createdBy = rs.getLong("created_by");
                    return userId.equals(assignedTo) || userId.equals(createdBy);
                }
            }
        }
        return false;
    }

    private JsonObject buildTaskJson(ResultSet rs) throws SQLException {
        JsonObject task = new JsonObject();
        task.addProperty("id", rs.getLong("id"));
        task.addProperty("onboardingRunId", rs.getLong("onboarding_run_id"));
        task.addProperty("templateId", rs.getLong("template_id"));
        task.addProperty("title", rs.getString("title"));
        task.addProperty("description", rs.getString("description"));
        task.addProperty("status", rs.getString("status"));
        task.addProperty("priority", rs.getString("priority"));
        task.addProperty("assignedTo", rs.getLong("assigned_to"));
        task.addProperty("assignedToName", rs.getString("assigned_to_name"));
        task.addProperty("assignedToType", rs.getString("assigned_to_type"));
        task.addProperty("createdBy", rs.getLong("created_by"));
        task.addProperty("createdByName", rs.getString("created_by_name"));
        
        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) task.addProperty("dueDate", dueDate.toString());
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) task.addProperty("completedAt", completedAt.toString());
        
        task.addProperty("executionMode", rs.getString("execution_mode"));
        task.addProperty("sequenceOrder", rs.getInt("sequence_order"));
        task.addProperty("parallelGroup", rs.getString("parallel_group"));
        task.addProperty("isBlocked", rs.getBoolean("is_blocked"));
        task.addProperty("createdAt", rs.getTimestamp("created_at").toString());
        task.addProperty("updatedAt", rs.getTimestamp("updated_at").toString());
        
        return task;
    }

    private String readRequestBody(HttpServletRequest req, int maxSize) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            int totalRead = 0;
            while ((line = reader.readLine()) != null) {
                totalRead += line.length();
                if (totalRead > maxSize) {
                    return null;
                }
                body.append(line);
            }
        }
        return body.toString();
    }

    private int getIntParameter(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(error));
    }
}
