package com.onboardbuddy.controllers;

import com.google.gson.*;
import com.onboardbuddy.config.DatabaseConfig;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class TaskTemplateServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TaskTemplateServlet.class);
    private final Gson gson = new Gson();
    private static final int MAX_REQUEST_SIZE = 8192;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        try {
            // URL: /api/templates?planId=X or /api/templates/{templateId}
            if (pathInfo == null || pathInfo.equals("/")) {
                // List templates - get planId from query parameter
                String planIdStr = req.getParameter("planId");
                if (planIdStr != null) {
                    handleListTemplates(req, resp, planIdStr);
                } else {
                    sendError(resp, 400, "planId parameter is required");
                }
                return;
            }

            String[] parts = pathInfo.substring(1).split("/");
            
            if (parts.length == 1) {
                // /{templateId} - get specific template
                String templateId = parts[0];
                handleGetTemplate(req, resp, templateId);
            } else {
                sendError(resp, 400, "Invalid path");
            }
        } catch (Exception e) {
            logger.error("Error in TaskTemplateServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole) && !"MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        try {
            // planId will be in the request body
            handleCreateTemplate(req, resp, null);
        } catch (Exception e) {
            logger.error("Error in TaskTemplateServlet POST", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole) && !"MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(resp, 400, "Template ID is required");
                return;
            }

            String[] parts = pathInfo.substring(1).split("/");
            if (parts.length >= 1) {
                String templateId = parts[0];
                handleUpdateTemplate(req, resp, templateId);
            } else {
                sendError(resp, 400, "Invalid path format");
            }
        } catch (Exception e) {
            logger.error("Error in TaskTemplateServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(resp, 400, "Template ID is required");
                return;
            }

            String[] parts = pathInfo.substring(1).split("/");
            if (parts.length >= 1) {
                String templateId = parts[0];
                handleDeleteTemplate(req, resp, templateId);
            } else {
                sendError(resp, 400, "Invalid path format");
            }
        } catch (Exception e) {
            logger.error("Error in TaskTemplateServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListTemplates(HttpServletRequest req, HttpServletResponse resp, String planId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM task_templates WHERE plan_id = ? AND is_active = TRUE ORDER BY day_offset, sequence_order";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(planId));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray templates = new JsonArray();
                    while (rs.next()) {
                        templates.add(buildTemplateJson(rs));
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", templates);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error listing templates", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleGetTemplate(HttpServletRequest req, HttpServletResponse resp, String templateId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM task_templates WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(templateId));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.add("data", buildTemplateJson(rs));
                        resp.setStatus(200);
                        resp.getWriter().write(gson.toJson(response));
                    } else {
                        sendError(resp, 404, "Template not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error getting template", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleCreateTemplate(HttpServletRequest req, HttpServletResponse resp, String planId) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
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

        if (!requestBody.has("planId")) {
            sendError(resp, 400, "planId is required");
            return;
        }
        
        planId = String.valueOf(requestBody.get("planId").getAsLong());

        if (!requestBody.has("name") || !requestBody.has("taskType") || !requestBody.has("assigneeType")) {
            sendError(resp, 400, "name, taskType, and assigneeType are required");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Get plan version
            int planVersion = 1;
            String versionSql = "SELECT version FROM onboarding_plans WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(versionSql)) {
                stmt.setLong(1, Long.parseLong(planId));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        planVersion = rs.getInt("version");
                    }
                }
            }

            String sql = "INSERT INTO task_templates (plan_id, plan_version, name, description, created_by, " +
                        "priority, day_offset, estimated_duration, task_type, owner_type, assignee_type, " +
                        "execution_mode, sequence_order, parallel_group, category, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, NOW(), NOW())";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, Long.parseLong(planId));
                stmt.setInt(2, planVersion);
                stmt.setString(3, requestBody.get("name").getAsString());
                stmt.setString(4, requestBody.has("description") ? requestBody.get("description").getAsString() : null);
                stmt.setLong(5, userId);
                stmt.setString(6, requestBody.has("priority") ? requestBody.get("priority").getAsString() : "MEDIUM");
                stmt.setInt(7, requestBody.has("dayOffset") ? requestBody.get("dayOffset").getAsInt() : 0);
                stmt.setInt(8, requestBody.has("estimatedDuration") ? requestBody.get("estimatedDuration").getAsInt() : 60);
                stmt.setString(9, requestBody.get("taskType").getAsString());
                stmt.setString(10, requestBody.has("ownerType") ? requestBody.get("ownerType").getAsString() : "MANAGER_OWNED");
                stmt.setString(11, requestBody.get("assigneeType").getAsString());
                stmt.setString(12, requestBody.has("executionMode") ? requestBody.get("executionMode").getAsString() : "PARALLEL");
                
                if (requestBody.has("sequenceOrder")) {
                    stmt.setInt(13, requestBody.get("sequenceOrder").getAsInt());
                } else {
                    stmt.setNull(13, Types.INTEGER);
                }
                
                stmt.setString(14, requestBody.has("parallelGroup") ? requestBody.get("parallelGroup").getAsString() : null);
                stmt.setString(15, requestBody.has("category") ? requestBody.get("category").getAsString() : null);
                
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long templateId = generatedKeys.getLong(1);
                            JsonObject response = new JsonObject();
                            response.addProperty("success", true);
                            response.addProperty("templateId", templateId);
                            response.addProperty("message", "Template created successfully");
                            resp.setStatus(201);
                            resp.getWriter().write(gson.toJson(response));
                            return;
                        }
                    }
                }
                sendError(resp, 500, "Failed to create template");
            }
        } catch (SQLException e) {
            logger.error("Database error creating template", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleUpdateTemplate(HttpServletRequest req, HttpServletResponse resp, String templateId) throws IOException {
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
            StringBuilder sql = new StringBuilder("UPDATE task_templates SET updated_at = NOW()");
            if (requestBody.has("name")) sql.append(", name = ?");
            if (requestBody.has("description")) sql.append(", description = ?");
            if (requestBody.has("priority")) sql.append(", priority = ?");
            if (requestBody.has("dayOffset")) sql.append(", day_offset = ?");
            if (requestBody.has("estimatedDuration")) sql.append(", estimated_duration = ?");
            if (requestBody.has("taskType")) sql.append(", task_type = ?");
            if (requestBody.has("assigneeType")) sql.append(", assignee_type = ?");
            sql.append(" WHERE id = ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (requestBody.has("name")) stmt.setString(paramIndex++, requestBody.get("name").getAsString());
                if (requestBody.has("description")) stmt.setString(paramIndex++, requestBody.get("description").getAsString());
                if (requestBody.has("priority")) stmt.setString(paramIndex++, requestBody.get("priority").getAsString());
                if (requestBody.has("dayOffset")) stmt.setInt(paramIndex++, requestBody.get("dayOffset").getAsInt());
                if (requestBody.has("estimatedDuration")) stmt.setInt(paramIndex++, requestBody.get("estimatedDuration").getAsInt());
                if (requestBody.has("taskType")) stmt.setString(paramIndex++, requestBody.get("taskType").getAsString());
                if (requestBody.has("assigneeType")) stmt.setString(paramIndex++, requestBody.get("assigneeType").getAsString());
                stmt.setLong(paramIndex, Long.parseLong(templateId));

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Template updated successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Template not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error updating template", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleDeleteTemplate(HttpServletRequest req, HttpServletResponse resp, String templateId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE task_templates SET is_active = FALSE WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(templateId));
                
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Template deleted successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Template not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error deleting template", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private JsonObject buildTemplateJson(ResultSet rs) throws SQLException {
        JsonObject template = new JsonObject();
        template.addProperty("id", rs.getLong("id"));
        template.addProperty("planId", rs.getLong("plan_id"));
        template.addProperty("planVersion", rs.getInt("plan_version"));
        template.addProperty("name", rs.getString("name"));
        template.addProperty("description", rs.getString("description"));
        template.addProperty("priority", rs.getString("priority"));
        template.addProperty("dayOffset", rs.getInt("day_offset"));
        template.addProperty("estimatedDuration", rs.getInt("estimated_duration"));
        template.addProperty("taskType", rs.getString("task_type"));
        template.addProperty("ownerType", rs.getString("owner_type"));
        template.addProperty("assigneeType", rs.getString("assignee_type"));
        template.addProperty("executionMode", rs.getString("execution_mode"));
        
        int sequenceOrder = rs.getInt("sequence_order");
        if (!rs.wasNull()) {
            template.addProperty("sequenceOrder", sequenceOrder);
        }
        
        template.addProperty("parallelGroup", rs.getString("parallel_group"));
        template.addProperty("category", rs.getString("category"));
        template.addProperty("createdAt", rs.getTimestamp("created_at").toString());
        template.addProperty("updatedAt", rs.getTimestamp("updated_at").toString());
        return template;
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

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        resp.getWriter().write(gson.toJson(error));
    }
}
