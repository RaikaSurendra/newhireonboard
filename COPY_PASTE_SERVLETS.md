# Complete Servlet Implementations - Copy & Paste Ready

## Instructions
1. Copy each servlet code below
2. Replace the corresponding file in `backend/src/main/java/com/onboardbuddy/controllers/`
3. Build: `cd backend && mvn clean package -DskipTests`
4. Run: `java -jar backend/target/onboard-buddy-1.0.0.jar`

---

## ✅ Already Complete
- AuthServlet.java
- UserServlet.java
- TaskServlet.java

---

## 🔧 TO IMPLEMENT - Copy these files:

### 1. FeedbackServlet.java (Simplest - Start Here)

```java
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
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT f.*, u.name as user_name FROM feedback f " +
                        "LEFT JOIN users u ON f.user_id = u.id " +
                        "ORDER BY f.created_at DESC LIMIT ? OFFSET ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                stmt.setInt(2, (page - 1) * limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray feedback = new JsonArray();
                    while (rs.next()) {
                        JsonObject item = new JsonObject();
                        item.addProperty("id", rs.getLong("id"));
                        item.addProperty("userId", rs.getLong("user_id"));
                        item.addProperty("userName", rs.getString("user_name"));
                        item.addProperty("rating", rs.getInt("rating"));
                        item.addProperty("comment", rs.getString("comment"));
                        item.addProperty("category", rs.getString("category"));
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
        
        if (!data.has("rating") || !data.has("comment")) {
            sendError(resp, 400, "rating and comment are required");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO feedback (user_id, rating, comment, category, created_at) " +
                        "VALUES (?, ?, ?, ?, NOW())";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, userId);
                stmt.setInt(2, data.get("rating").getAsInt());
                stmt.setString(3, data.get("comment").getAsString());
                stmt.setString(4, data.has("category") ? data.get("category").getAsString() : "GENERAL");
                
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
```

---

### 2. NotificationServlet.java (Simple)

```java
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
```

---

## 📋 Implementation Checklist

- [ ] Copy FeedbackServlet.java
- [ ] Copy NotificationServlet.java  
- [ ] Copy PlanServlet.java (from REMAINING_SERVLETS_IMPLEMENTATION.md)
- [ ] Copy BuddyMatchServlet.java (from ALL_REMAINING_SERVLETS.md)
- [ ] Copy MessageServlet.java (from ALL_REMAINING_SERVLETS.md)
- [ ] Build: `cd backend && mvn clean package -DskipTests`
- [ ] Run: `java -jar backend/target/onboard-buddy-1.0.0.jar`
- [ ] Test APIs with curl or Postman

## 🚀 Quick Build & Run

```bash
# Navigate to backend
cd backend

# Clean build
mvn clean package -DskipTests

# Run application
cd ..
java -jar backend/target/onboard-buddy-1.0.0.jar
```

## ✅ After Implementation

You will have **ALL 8 servlets complete:**
1. ✅ AuthServlet
2. ✅ UserServlet
3. ✅ TaskServlet
4. ✅ PlanServlet
5. ✅ BuddyMatchServlet
6. ✅ MessageServlet
7. ✅ FeedbackServlet
8. ✅ NotificationServlet

**Backend will be 100% complete!** 🎉
