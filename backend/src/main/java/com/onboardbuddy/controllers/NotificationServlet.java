package com.onboardbuddy.controllers;

import com.google.gson.*;
import com.onboardbuddy.config.DatabaseConfig;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class NotificationServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServlet.class);
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        Long userId = (Long) req.getAttribute("userId");
        try {
            handleListNotifications(req, resp, userId);
        } catch (Exception e) {
            logger.error("Error in NotificationServlet GET", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        Long userId = (Long) req.getAttribute("userId");
        
        try {
            if (pathInfo != null && pathInfo.contains("/read-all")) {
                handleMarkAllAsRead(req, resp, userId);
            } else if (pathInfo != null) {
                String notifId = pathInfo.substring(1, pathInfo.contains("/") ? pathInfo.indexOf("/", 1) : pathInfo.length());
                handleMarkAsRead(req, resp, notifId, userId);
            }
        } catch (Exception e) {
            logger.error("Error in NotificationServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            sendError(resp, 400, "Notification ID is required");
            return;
        }
        
        try {
            String notifId = pathInfo.substring(1);
            handleDeleteNotification(req, resp, notifId);
        } catch (Exception e) {
            logger.error("Error in NotificationServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListNotifications(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM notifications WHERE user_id = ? " +
                        "ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                stmt.setInt(2, limit);
                stmt.setInt(3, (page - 1) * limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray notifications = new JsonArray();
                    while (rs.next()) {
                        JsonObject notif = new JsonObject();
                        notif.addProperty("id", rs.getLong("id"));
                        notif.addProperty("type", rs.getString("type"));
                        notif.addProperty("title", rs.getString("title"));
                        notif.addProperty("message", rs.getString("message"));
                        notif.addProperty("isRead", rs.getBoolean("is_read"));
                        notif.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                        notifications.add(notif);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", notifications);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing notifications", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleMarkAsRead(HttpServletRequest req, HttpServletResponse resp, String notifId, Long userId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE notifications SET is_read = true WHERE id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(notifId));
                stmt.setLong(2, userId);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Notification marked as read");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Notification not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Error marking notification as read", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleMarkAllAsRead(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE notifications SET is_read = true WHERE user_id = ? AND is_read = false";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                int updated = stmt.executeUpdate();
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "All notifications marked as read");
                response.addProperty("count", updated);
                resp.setStatus(200);
                resp.getWriter().write(gson.toJson(response));
            }
        } catch (SQLException e) {
            logger.error("Error marking all notifications as read", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleDeleteNotification(HttpServletRequest req, HttpServletResponse resp, String notifId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "DELETE FROM notifications WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(notifId));
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Notification deleted");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Notification not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Error deleting notification", e);
            sendError(resp, 500, "An error occurred");
        }
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