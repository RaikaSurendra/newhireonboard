package com.onboardbuddy.controllers;

import com.google.gson.*;
import com.onboardbuddy.config.DatabaseConfig;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class FeedbackServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackServlet.class);
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            handleListFeedback(req, resp);
        } catch (Exception e) {
            logger.error("Error in FeedbackServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        Long userId = (Long) req.getAttribute("userId");
        try {
            handleSubmitFeedback(req, resp, userId);
        } catch (Exception e) {
            logger.error("Error in FeedbackServlet POST", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListFeedback(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String matchId = req.getParameter("matchId");
        String fromUserId = req.getParameter("fromUserId");
        String toUserId = req.getParameter("toUserId");
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT f.*, " +
                "fu.name as from_user_name, fu.email as from_user_email, " +
                "tu.name as to_user_name, tu.email as to_user_email " +
                "FROM feedback f " +
                "LEFT JOIN users fu ON f.from_user_id = fu.id " +
                "LEFT JOIN users tu ON f.to_user_id = tu.id " +
                "WHERE 1=1"
            );
            
            if (matchId != null) sql.append(" AND f.match_id = ?");
            if (fromUserId != null) sql.append(" AND f.from_user_id = ?");
            if (toUserId != null) sql.append(" AND f.to_user_id = ?");
            sql.append(" ORDER BY f.created_at DESC LIMIT ? OFFSET ?");
            
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (matchId != null) stmt.setLong(paramIndex++, Long.parseLong(matchId));
                if (fromUserId != null) stmt.setLong(paramIndex++, Long.parseLong(fromUserId));
                if (toUserId != null) stmt.setLong(paramIndex++, Long.parseLong(toUserId));
                stmt.setInt(paramIndex++, limit);
                stmt.setInt(paramIndex, (page - 1) * limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray feedback = new JsonArray();
                    while (rs.next()) {
                        JsonObject item = new JsonObject();
                        item.addProperty("id", rs.getLong("id"));
                        item.addProperty("matchId", rs.getLong("match_id"));
                        item.addProperty("fromUserId", rs.getLong("from_user_id"));
                        item.addProperty("fromUserName", rs.getString("from_user_name"));
                        item.addProperty("fromUserEmail", rs.getString("from_user_email"));
                        item.addProperty("toUserId", rs.getLong("to_user_id"));
                        item.addProperty("toUserName", rs.getString("to_user_name"));
                        item.addProperty("toUserEmail", rs.getString("to_user_email"));
                        item.addProperty("rating", rs.getInt("rating"));
                        item.addProperty("comments", rs.getString("comments"));
                        item.addProperty("feedbackType", rs.getString("feedback_type"));
                        item.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                        feedback.add(item);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", feedback);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing feedback", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleSubmitFeedback(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        String body = readBody(req);
        JsonObject data = gson.fromJson(body, JsonObject.class);
        
        if (!data.has("matchId") || !data.has("toUserId") || !data.has("rating") || !data.has("feedbackType")) {
            sendError(resp, 400, "matchId, toUserId, rating, and feedbackType are required");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO feedback (match_id, from_user_id, to_user_id, rating, comments, feedback_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW())";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, data.get("matchId").getAsLong());
                stmt.setLong(2, userId);
                stmt.setLong(3, data.get("toUserId").getAsLong());
                stmt.setInt(4, data.get("rating").getAsInt());
                stmt.setString(5, data.has("comments") ? data.get("comments").getAsString() : null);
                stmt.setString(6, data.get("feedbackType").getAsString());
                
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.addProperty("feedbackId", rs.getLong(1));
                        response.addProperty("message", "Feedback submitted successfully");
                        resp.setStatus(201);
                        resp.getWriter().write(gson.toJson(response));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error submitting feedback", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
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
