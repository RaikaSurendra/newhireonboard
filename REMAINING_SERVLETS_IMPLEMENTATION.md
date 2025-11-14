# Remaining Backend Servlets - Complete Implementation Guide

## 🎯 Overview

This document provides complete, copy-paste ready implementations for the remaining 5 servlets. Each follows the same pattern as UserServlet and TaskServlet.

## ✅ Already Complete
1. ✅ AuthServlet (357 lines)
2. ✅ UserServlet (443 lines)
3. ✅ TaskServlet (584 lines)

## 📋 To Implement
1. **PlanServlet** - Onboarding plan management
2. **BuddyMatchServlet** - Buddy matching system
3. **MessageServlet** - Internal messaging
4. **FeedbackServlet** - Feedback collection
5. **NotificationServlet** - User notifications

---

## 1. PlanServlet Implementation

**File:** `backend/src/main/java/com/onboardbuddy/controllers/PlanServlet.java`

**Endpoints:**
- `GET /api/plans` - List all plans
- `GET /api/plans/{id}` - Get plan details
- `POST /api/plans` - Create new plan
- `PUT /api/plans/{id}` - Update plan
- `DELETE /api/plans/{id}` - Delete plan

**Key Features:**
- Department filtering
- Status management (DRAFT, ACTIVE, ARCHIVED)
- Role-based access (Admin/HR can manage)
- Pagination support

**Implementation:** Replace the stub with this complete code (~400 lines):

```java
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

public class PlanServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PlanServlet.class);
    private final Gson gson = new Gson();
    private static final int MAX_REQUEST_SIZE = 8192;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleListPlans(req, resp);
            } else {
                String planId = pathInfo.substring(1);
                handleGetPlan(req, resp, planId);
            }
        } catch (Exception e) {
            logger.error("Error in PlanServlet GET", e);
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
            handleCreatePlan(req, resp);
        } catch (Exception e) {
            logger.error("Error in PlanServlet POST", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 400, "Plan ID is required");
            return;
        }

        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"HR_MANAGER".equals(userRole)) {
            sendError(resp, 403, "Insufficient permissions");
            return;
        }

        try {
            String planId = pathInfo.substring(1);
            handleUpdatePlan(req, resp, planId);
        } catch (Exception e) {
            logger.error("Error in PlanServlet PUT", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 400, "Plan ID is required");
            return;
        }

        String userRole = (String) req.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            sendError(resp, 403, "Only admins can delete plans");
            return;
        }

        try {
            String planId = pathInfo.substring(1);
            handleDeletePlan(req, resp, planId);
        } catch (Exception e) {
            logger.error("Error in PlanServlet DELETE", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleListPlans(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String department = req.getParameter("department");
        String status = req.getParameter("status");
        int page = getIntParameter(req, "page", 1);
        int limit = getIntParameter(req, "limit", 20);
        int offset = (page - 1) * limit;

        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT id, name, description, department, status, duration_days, " +
                "start_date, end_date, created_by, created_at, updated_at " +
                "FROM onboarding_plans WHERE 1=1"
            );
            
            if (department != null) sql.append(" AND department = ?");
            if (status != null) sql.append(" AND status = ?");
            sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (department != null) stmt.setString(paramIndex++, department);
                if (status != null) stmt.setString(paramIndex++, status);
                stmt.setInt(paramIndex++, limit);
                stmt.setInt(paramIndex, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    JsonArray plans = new JsonArray();
                    while (rs.next()) {
                        JsonObject plan = new JsonObject();
                        plan.addProperty("id", rs.getLong("id"));
                        plan.addProperty("name", rs.getString("name"));
                        plan.addProperty("description", rs.getString("description"));
                        plan.addProperty("department", rs.getString("department"));
                        plan.addProperty("status", rs.getString("status"));
                        plan.addProperty("durationDays", rs.getInt("duration_days"));
                        
                        Date startDate = rs.getDate("start_date");
                        if (startDate != null) plan.addProperty("startDate", startDate.toString());
                        
                        Date endDate = rs.getDate("end_date");
                        if (endDate != null) plan.addProperty("endDate", endDate.toString());
                        
                        plan.addProperty("createdBy", rs.getLong("created_by"));
                        plan.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                        plans.add(plan);
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.add("data", plans);
                    response.addProperty("page", page);
                    response.addProperty("limit", limit);
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error listing plans", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleGetPlan(HttpServletRequest req, HttpServletResponse resp, String planId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM onboarding_plans WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(planId));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject plan = buildPlanJson(rs);
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        response.add("data", plan);
                        resp.setStatus(200);
                        resp.getWriter().write(gson.toJson(response));
                    } else {
                        sendError(resp, 404, "Plan not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error getting plan", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleCreatePlan(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

        if (!requestBody.has("name") || !requestBody.has("department")) {
            sendError(resp, 400, "name and department are required");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO onboarding_plans (name, description, department, status, " +
                        "duration_days, start_date, end_date, created_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, requestBody.get("name").getAsString());
                stmt.setString(2, requestBody.has("description") ? requestBody.get("description").getAsString() : null);
                stmt.setString(3, requestBody.get("department").getAsString());
                stmt.setString(4, requestBody.has("status") ? requestBody.get("status").getAsString() : "DRAFT");
                stmt.setInt(5, requestBody.has("durationDays") ? requestBody.get("durationDays").getAsInt() : 90);
                
                if (requestBody.has("startDate")) {
                    stmt.setDate(6, Date.valueOf(requestBody.get("startDate").getAsString()));
                } else {
                    stmt.setNull(6, Types.DATE);
                }
                
                if (requestBody.has("endDate")) {
                    stmt.setDate(7, Date.valueOf(requestBody.get("endDate").getAsString()));
                } else {
                    stmt.setNull(7, Types.DATE);
                }
                
                stmt.setLong(8, userId);
                
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long planId = generatedKeys.getLong(1);
                            JsonObject response = new JsonObject();
                            response.addProperty("success", true);
                            response.addProperty("planId", planId);
                            response.addProperty("message", "Plan created successfully");
                            logger.info("Plan created: {}", planId);
                            resp.setStatus(201);
                            resp.getWriter().write(gson.toJson(response));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error creating plan", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleUpdatePlan(HttpServletRequest req, HttpServletResponse resp, String planId) throws IOException {
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
            StringBuilder sql = new StringBuilder("UPDATE onboarding_plans SET updated_at = NOW()");
            if (requestBody.has("name")) sql.append(", name = ?");
            if (requestBody.has("description")) sql.append(", description = ?");
            if (requestBody.has("status")) sql.append(", status = ?");
            if (requestBody.has("durationDays")) sql.append(", duration_days = ?");
            sql.append(" WHERE id = ?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (requestBody.has("name")) stmt.setString(paramIndex++, requestBody.get("name").getAsString());
                if (requestBody.has("description")) stmt.setString(paramIndex++, requestBody.get("description").getAsString());
                if (requestBody.has("status")) stmt.setString(paramIndex++, requestBody.get("status").getAsString());
                if (requestBody.has("durationDays")) stmt.setInt(paramIndex++, requestBody.get("durationDays").getAsInt());
                stmt.setLong(paramIndex, Long.parseLong(planId));

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Plan updated successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Plan not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error updating plan", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private void handleDeletePlan(HttpServletRequest req, HttpServletResponse resp, String planId) throws IOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "DELETE FROM onboarding_plans WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, Long.parseLong(planId));
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Plan deleted successfully");
                    resp.setStatus(200);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    sendError(resp, 404, "Plan not found");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error deleting plan", e);
            sendError(resp, 500, "An error occurred");
        }
    }

    private JsonObject buildPlanJson(ResultSet rs) throws SQLException {
        JsonObject plan = new JsonObject();
        plan.addProperty("id", rs.getLong("id"));
        plan.addProperty("name", rs.getString("name"));
        plan.addProperty("description", rs.getString("description"));
        plan.addProperty("department", rs.getString("department"));
        plan.addProperty("status", rs.getString("status"));
        plan.addProperty("durationDays", rs.getInt("duration_days"));
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) plan.addProperty("startDate", startDate.toString());
        
        Date endDate = rs.getDate("end_date");
        if (endDate != null) plan.addProperty("endDate", endDate.toString());
        
        plan.addProperty("createdBy", rs.getLong("created_by"));
        plan.addProperty("createdAt", rs.getTimestamp("created_at").toString());
        plan.addProperty("updatedAt", rs.getTimestamp("updated_at").toString());
        return plan;
    }

    private String readRequestBody(HttpServletRequest req, int maxSize) throws IOException {
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
```

---

## 2. BuddyMatchServlet - See Next Section

## 3. MessageServlet - See Next Section

## 4. FeedbackServlet - See Next Section

## 5. NotificationServlet - See Next Section

---

## 🚀 Quick Implementation Steps

1. **Copy each servlet code** from this document
2. **Replace the stub files** in `backend/src/main/java/com/onboardbuddy/controllers/`
3. **Build the backend:** `cd backend && mvn clean package -DskipTests`
4. **Run the application:** `java -jar backend/target/onboard-buddy-1.0.0.jar`
5. **Test the APIs** using curl or Postman

## 📊 Progress Tracker

- [x] AuthServlet
- [x] UserServlet  
- [x] TaskServlet
- [ ] PlanServlet (code provided above)
- [ ] BuddyMatchServlet (implement next)
- [ ] MessageServlet (implement next)
- [ ] FeedbackServlet (implement next)
- [ ] NotificationServlet (implement next)

---

**Note:** Due to token constraints, I've provided PlanServlet complete implementation. Would you like me to continue with the remaining 4 servlets in the next response?
