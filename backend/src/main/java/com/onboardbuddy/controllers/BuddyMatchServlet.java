package com.onboardbuddy.controllers;

import com.google.gson.*;
import com.onboardbuddy.config.DatabaseConfig;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class BuddyMatchServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BuddyMatchServlet.class);
    private final Gson gson = new Gson();
    private static final int MAX_REQUEST_SIZE = 8192;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleListMatches(req, resp);
            } else {
                String matchId = pathInfo.substring(1);
                handleGetMatch(req, resp, matchId);
            }
        } catch (Exception e) {
            logger.error("Error in BuddyMatchServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }
        
        try {
            handleCreateMatch(req, resp);
        } catch (Exception e) {
            logger.error("Error in BuddyMatchServlet POST", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            sendError(resp, 400, "Match ID is required");
            return;
        }
        
        try {
            if (pathInfo.contains("/accept")) {
                String matchId = pathInfo.substring(1, pathInfo.indexOf("/accept"));
                handleAcceptMatch(req, resp, matchId);
            } else if (pathInfo.contains("/complete")) {
                String matchId = pathInfo.substring(1, pathInfo.indexOf("/complete"));
                handleCompleteMatch(req, resp, matchId);
            } else {
                String matchId = pathInfo.substring(1);
                handleUpdateMatch(req, resp, matchId);
            }
        } catch (Exception e) {
            logger.error("Error in BuddyMatchServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            sendError(resp, 400, "Match ID is required");
            return;
        }
        
        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }
        
        try {
            String matchId = pathInfo.substring(1);
            handleEndMatch(req, resp, matchId);
        } catch (Exception e) {
            logger.error("Error in BuddyMatchServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListMatches(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String buddyId = req.getParameter("buddyId");
        String employeeId = req.getParameter("employeeId");
        String status = req.getParameter("status");
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT bm.*, " +
                "b.name as buddy_name, b.email as buddy_email, " +
                "e.name as employee_name, e.email as employee_email " +
                "FROM buddy_matches bm " +
                "LEFT JOIN users b ON bm.buddy_user_id = b.id " +
                "LEFT JOIN users e ON bm.new_employee_id = e.id " +
                "WHERE 1=1"
            );
            
            if (buddyId != null) sql.append(" AND bm.buddy_user_id = ?");
            if (employeeId != null) sql.append(" AND bm.new_employee_id = ?");
            if (status != null) sql.append(" AND bm.status = ?");
            sql.append(" ORDER BY bm.matched_at DESC LIMIT ? OFFSET ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (buddyId != null) stmt.setLong(paramIndex++, Long.parseLong(buddyId));
                if (employeeId != null) stmt.setLong(paramIndex++, Long.parseLong(employeeId));
                if (status != null) stmt.setString(paramIndex++, status);
                stmt.setInt(paramIndex++, limit);
                stmt.setInt(paramIndex, (page - 1) * limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray matches = new JsonArray();
                    while (rs.next()) {
                        JsonObject match = buildMatchJson(rs);
                        matches.add(match);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", matches);
                    response.addProperty("page", page);
                    response.addProperty("limit", limit);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing matches", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleGetMatch(HttpServletRequest req, HttpServletResponse resp, String matchId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT bm.*, " +
                        "b.name as buddy_name, b.email as buddy_email, " +
                        "e.name as employee_name, e.email as employee_email " +
                        "FROM buddy_matches bm " +
                        "LEFT JOIN users b ON bm.buddy_user_id = b.id " +
                        "LEFT JOIN users e ON bm.new_employee_id = e.id " +
                        "WHERE bm.id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(matchId));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject match = buildMatchJson(rs);
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.add("data", match);
                        resp.setStatus(200);
                        resp.getWriter().write(gson.toJson(response));
                    } else {
                        sendError(resp, 404, "Match not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting match", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleCreateMatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        String body = readBody(req, MAX_REQUEST_SIZE);
        if (body == null) {
            sendError(resp, 400, "Request body too large or invalid");
            return;
        }

        JsonObject data;
        try {
            data = gson.fromJson(body, JsonObject.class);
        } catch (JsonSyntaxException e) {
            sendError(resp, 400, "Invalid request format");
            return;
        }

        if (!data.has("buddyUserId") || !data.has("newEmployeeId")) {
            sendError(resp, 400, "buddyUserId and newEmployeeId are required");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, " +
                        "match_score, matched_at, created_by) VALUES (?, ?, ?, ?, NOW(), ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, data.get("buddyUserId").getAsLong());
                stmt.setLong(2, data.get("newEmployeeId").getAsLong());
                stmt.setString(3, data.has("status") ? data.get("status").getAsString() : "PENDING");
                
                if (data.has("matchScore")) {
                    stmt.setDouble(4, data.get("matchScore").getAsDouble());
                } else {
                    stmt.setNull(4, Types.DECIMAL);
                }
                
                stmt.setLong(5, userId);
                
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.addProperty("matchId", rs.getLong(1));
                        response.addProperty("message", "Match created successfully");
                        resp.setStatus(201);
                        resp.getWriter().write(gson.toJson(response));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating match", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleAcceptMatch(HttpServletRequest req, HttpServletResponse resp, String matchId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE buddy_matches SET status = 'ACCEPTED', accepted_at = NOW() WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(matchId));
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Match accepted successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Match not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Error accepting match", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleCompleteMatch(HttpServletRequest req, HttpServletResponse resp, String matchId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE buddy_matches SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(matchId));
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Match completed successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Match not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Error completing match", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleUpdateMatch(HttpServletRequest req, HttpServletResponse resp, String matchId) throws IOException {
        String body = readBody(req, MAX_REQUEST_SIZE);
        if (body == null) {
            sendError(resp, 400, "Request body too large or invalid");
            return;
        }

        JsonObject data;
        try {
            data = gson.fromJson(body, JsonObject.class);
        } catch (JsonSyntaxException e) {
            sendError(resp, 400, "Invalid request format");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder("UPDATE buddy_matches SET ");
            boolean hasUpdate = false;
            
            if (data.has("status")) {
                sql.append("status = ?");
                hasUpdate = true;
            }
            if (data.has("matchScore")) {
                if (hasUpdate) sql.append(", ");
                sql.append("match_score = ?");
                hasUpdate = true;
            }
            
            if (!hasUpdate) {
                sendError(resp, 400, "No fields to update");
                return;
            }
            
            sql.append(" WHERE id = ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (data.has("status")) stmt.setString(paramIndex++, data.get("status").getAsString());
                if (data.has("matchScore")) stmt.setDouble(paramIndex++, data.get("matchScore").getAsDouble());
                stmt.setLong(paramIndex, Long.parseLong(matchId));

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Match updated successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Match not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Error updating match", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleEndMatch(HttpServletRequest req, HttpServletResponse resp, String matchId) throws IOException {
        String body = readBody(req, MAX_REQUEST_SIZE);
        String endReason = null;
        
        if (body != null && !body.isEmpty()) {
            try {
                JsonObject data = gson.fromJson(body, JsonObject.class);
                if (data.has("endReason")) {
                    endReason = data.get("endReason").getAsString();
                }
            } catch (JsonSyntaxException e) {
                // Ignore, endReason is optional
            }
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE buddy_matches SET status = 'ENDED', ended_at = NOW(), end_reason = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, endReason);
                stmt.setLong(2, Long.parseLong(matchId));
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Match ended successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Match not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Error ending match", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private JsonObject buildMatchJson(ResultSet rs) throws SQLException {
        JsonObject match = new JsonObject();
        match.addProperty("id", rs.getLong("id"));
        match.addProperty("buddyUserId", rs.getLong("buddy_user_id"));
        match.addProperty("buddyName", rs.getString("buddy_name"));
        match.addProperty("buddyEmail", rs.getString("buddy_email"));
        match.addProperty("newEmployeeId", rs.getLong("new_employee_id"));
        match.addProperty("employeeName", rs.getString("employee_name"));
        match.addProperty("employeeEmail", rs.getString("employee_email"));
        match.addProperty("status", rs.getString("status"));
        
        Double matchScore = rs.getDouble("match_score");
        if (!rs.wasNull()) match.addProperty("matchScore", matchScore);
        
        match.addProperty("matchedAt", rs.getTimestamp("matched_at").toString());
        
        Timestamp acceptedAt = rs.getTimestamp("accepted_at");
        if (acceptedAt != null) match.addProperty("acceptedAt", acceptedAt.toString());
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) match.addProperty("completedAt", completedAt.toString());
        
        Timestamp endedAt = rs.getTimestamp("ended_at");
        if (endedAt != null) match.addProperty("endedAt", endedAt.toString());
        
        String endReason = rs.getString("end_reason");
        if (endReason != null) match.addProperty("endReason", endReason);
        
        return match;
    }

    private String readBody(HttpServletRequest req, int maxSize) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            int totalRead = 0;
            while ((line = reader.readLine()) != null) {
                totalRead += line.length();
                if (totalRead > maxSize) return null;
                body.append(line);
            }
        }
        return body.toString();
    }

    private int getIntParam(HttpServletRequest req, String name, int def) {
        String val = req.getParameter(name);
        try {
            return val != null ? Integer.parseInt(val) : def;
        } catch (NumberFormatException e) {
            return def;
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
