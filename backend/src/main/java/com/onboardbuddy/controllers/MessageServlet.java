package com.onboardbuddy.controllers;

import com.google.gson.*;
import com.onboardbuddy.config.DatabaseConfig;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class MessageServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MessageServlet.class);
    private final Gson gson = new Gson();
    private static final int MAX_REQUEST_SIZE = 16384;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        Long userId = (Long) req.getAttribute("userId");
        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleListMessages(req, resp, userId);
            } else {
                String messageId = pathInfo.substring(1);
                handleGetMessage(req, resp, messageId, userId);
            }
        } catch (Exception e) {
            logger.error("Error in MessageServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        Long userId = (Long) req.getAttribute("userId");
        try {
            handleSendMessage(req, resp, userId);
        } catch (Exception e) {
            logger.error("Error in MessageServlet POST", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            sendError(resp, 400, "Message ID is required");
            return;
        }
        
        Long userId = (Long) req.getAttribute("userId");
        
        try {
            if (pathInfo.contains("/read")) {
                String messageId = pathInfo.substring(1, pathInfo.indexOf("/read"));
                handleMarkAsRead(req, resp, messageId, userId);
            } else {
                sendError(resp, 400, "Invalid operation");
            }
        } catch (Exception e) {
            logger.error("Error in MessageServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            sendError(resp, 400, "Message ID is required");
            return;
        }
        
        Long userId = (Long) req.getAttribute("userId");
        
        try {
            String messageId = pathInfo.substring(1);
            handleDeleteMessage(req, resp, messageId, userId);
        } catch (Exception e) {
            logger.error("Error in MessageServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListMessages(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        String type = req.getParameter("type"); // "inbox" or "sent"
        String otherUserId = req.getParameter("otherUserId"); // For conversation view
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 50);

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT m.*, " +
                "s.name as sender_name, s.email as sender_email, " +
                "r.name as receiver_name, r.email as receiver_email " +
                "FROM messages m " +
                "LEFT JOIN users s ON m.sender_id = s.id " +
                "LEFT JOIN users r ON m.receiver_id = r.id " +
                "WHERE 1=1"
            );
            
            if (otherUserId != null) {
                // Conversation view: messages between current user and other user
                sql.append(" AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))");
            } else if ("sent".equals(type)) {
                sql.append(" AND m.sender_id = ?");
            } else {
                // Default to inbox
                sql.append(" AND m.receiver_id = ?");
            }
            
            sql.append(" ORDER BY m.created_at DESC LIMIT ? OFFSET ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                
                if (otherUserId != null) {
                    long otherId = Long.parseLong(otherUserId);
                    stmt.setLong(paramIndex++, userId);
                    stmt.setLong(paramIndex++, otherId);
                    stmt.setLong(paramIndex++, otherId);
                    stmt.setLong(paramIndex++, userId);
                } else {
                    stmt.setLong(paramIndex++, userId);
                }
                
                stmt.setInt(paramIndex++, limit);
                stmt.setInt(paramIndex, (page - 1) * limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray messages = new JsonArray();
                    while (rs.next()) {
                        JsonObject message = buildMessageJson(rs);
                        messages.add(message);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", messages);
                    response.addProperty("page", page);
                    response.addProperty("limit", limit);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing messages", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleGetMessage(HttpServletRequest req, HttpServletResponse resp, String messageId, Long userId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT m.*, " +
                        "s.name as sender_name, s.email as sender_email, " +
                        "r.name as receiver_name, r.email as receiver_email " +
                        "FROM messages m " +
                        "LEFT JOIN users s ON m.sender_id = s.id " +
                        "LEFT JOIN users r ON m.receiver_id = r.id " +
                        "WHERE m.id = ? AND (m.sender_id = ? OR m.receiver_id = ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(messageId));
                stmt.setLong(2, userId);
                stmt.setLong(3, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject message = buildMessageJson(rs);
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.add("data", message);
                        resp.setStatus(200);
                        resp.getWriter().write(gson.toJson(response));
                    } else {
                        sendError(resp, 404, "Message not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting message", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleSendMessage(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
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

        if (!data.has("receiverId") || !data.has("content")) {
            sendError(resp, 400, "receiverId and content are required");
            return;
        }

        String content = data.get("content").getAsString();
        if (content == null || content.trim().isEmpty()) {
            sendError(resp, 400, "Message content cannot be empty");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO messages (sender_id, receiver_id, content, is_read, created_at) " +
                        "VALUES (?, ?, ?, FALSE, NOW())";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, data.get("receiverId").getAsLong());
                stmt.setString(3, content.trim());
                
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.addProperty("messageId", rs.getLong(1));
                        response.addProperty("message", "Message sent successfully");
                        logger.info("Message sent from user {} to user {}", userId, data.get("receiverId").getAsLong());
                        resp.setStatus(201);
                        resp.getWriter().write(gson.toJson(response));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error sending message", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleMarkAsRead(HttpServletRequest req, HttpServletResponse resp, String messageId, Long userId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Only the receiver can mark a message as read
            String sql = "UPDATE messages SET is_read = TRUE, read_at = NOW() " +
                        "WHERE id = ? AND receiver_id = ? AND is_read = FALSE";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(messageId));
                stmt.setLong(2, userId);
                
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Message marked as read");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    // Check if message exists
                    String checkSql = "SELECT id FROM messages WHERE id = ? AND receiver_id = ?";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setLong(1, Long.parseLong(messageId));
                        checkStmt.setLong(2, userId);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next()) {
                                // Message exists but already read
                                JsonObject response = new JsonObject();
                                response.addProperty("success", true);
                                response.addProperty("message", "Message already marked as read");
                                resp.setStatus(200);
                                resp.getWriter().write(gson.toJson(response));
                            } else {
                                sendError(resp, 404, "Message not found");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error marking message as read", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleDeleteMessage(HttpServletRequest req, HttpServletResponse resp, String messageId, Long userId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Users can only delete messages they received
            String sql = "DELETE FROM messages WHERE id = ? AND receiver_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(messageId));
                stmt.setLong(2, userId);
                
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Message deleted successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Message not found or you don't have permission to delete it");
                }
            }
        } catch (SQLException e) {
            logger.error("Error deleting message", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private JsonObject buildMessageJson(ResultSet rs) throws SQLException {
        JsonObject message = new JsonObject();
        message.addProperty("id", rs.getLong("id"));
        message.addProperty("senderId", rs.getLong("sender_id"));
        message.addProperty("senderName", rs.getString("sender_name"));
        message.addProperty("senderEmail", rs.getString("sender_email"));
        message.addProperty("receiverId", rs.getLong("receiver_id"));
        message.addProperty("receiverName", rs.getString("receiver_name"));
        message.addProperty("receiverEmail", rs.getString("receiver_email"));
        message.addProperty("content", rs.getString("content"));
        message.addProperty("isRead", rs.getBoolean("is_read"));
        message.addProperty("createdAt", rs.getTimestamp("created_at").toString());
        
        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            message.addProperty("readAt", readAt.toString());
        }
        
        return message;
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
