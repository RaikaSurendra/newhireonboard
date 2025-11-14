# Backend Servlet Implementation Plan

## 🎯 Overview

Implementing all remaining backend servlets with full CRUD operations to match the database schema.

## 📊 Implementation Priority

1. **TaskServlet** - Core functionality (High Priority)
2. **PlanServlet** - Required for task management (High Priority)
3. **BuddyMatchServlet** - Core feature (High Priority)
4. **MessageServlet** - Communication (Medium Priority)
5. **FeedbackServlet** - Feedback system (Medium Priority)
6. **NotificationServlet** - Notifications (Medium Priority)

## 🔧 Implementation Strategy

Each servlet will follow the UserServlet pattern:
- Full CRUD operations (GET, POST, PUT, DELETE)
- Input validation using ValidationUtil
- Role-based access control
- Pagination support
- Error handling
- Request size limits

## 📝 Servlets to Implement

### 1. TaskServlet (Priority: HIGH)

**Endpoints:**
- `GET /api/tasks` - List tasks (filtered by user, status, run)
- `GET /api/tasks/{id}` - Get task details
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/{id}` - Update task
- `PUT /api/tasks/{id}/status` - Update task status
- `DELETE /api/tasks/{id}` - Cancel task

**Key Features:**
- Filter by assigned user, status, onboarding run
- Status transitions (PENDING → IN_PROGRESS → COMPLETED)
- Task history logging
- Due date management

**Estimated Size:** ~500 lines

### 2. PlanServlet (Priority: HIGH)

**Endpoints:**
- `GET /api/plans` - List onboarding plans
- `GET /api/plans/{id}` - Get plan details with tasks
- `POST /api/plans` - Create new plan
- `PUT /api/plans/{id}` - Update plan
- `DELETE /api/plans/{id}` - Delete plan

**Key Features:**
- Department-specific plans
- Plan status management
- Task template association
- Start/end date tracking

**Estimated Size:** ~400 lines

### 3. BuddyMatchServlet (Priority: HIGH)

**Endpoints:**
- `GET /api/matches` - List buddy matches
- `GET /api/matches/{id}` - Get match details
- `POST /api/matches` - Create match
- `PUT /api/matches/{id}/accept` - Accept match
- `PUT /api/matches/{id}/complete` - Complete match
- `DELETE /api/matches/{id}` - End match

**Key Features:**
- Match status workflow
- Skill-based matching
- Match scoring
- History tracking

**Estimated Size:** ~400 lines

### 4. MessageServlet (Priority: MEDIUM)

**Endpoints:**
- `GET /api/messages` - List messages (inbox/sent)
- `GET /api/messages/{id}` - Get message
- `POST /api/messages` - Send message
- `PUT /api/messages/{id}/read` - Mark as read
- `DELETE /api/messages/{id}` - Delete message

**Estimated Size:** ~300 lines

### 5. FeedbackServlet (Priority: MEDIUM)

**Endpoints:**
- `GET /api/feedback` - List feedback
- `GET /api/feedback/{id}` - Get feedback details
- `POST /api/feedback` - Submit feedback
- `GET /api/feedback/stats` - Get statistics

**Estimated Size:** ~250 lines

### 6. NotificationServlet (Priority: MEDIUM)

**Endpoints:**
- `GET /api/notifications` - List notifications
- `PUT /api/notifications/{id}/read` - Mark as read
- `PUT /api/notifications/read-all` - Mark all as read
- `DELETE /api/notifications/{id}` - Delete notification

**Estimated Size:** ~200 lines

## 🚀 Implementation Approach

Given the size constraints (each servlet is 200-500 lines), I'll implement them one at a time, starting with the highest priority.

**Current Status:**
- ✅ AuthServlet - Complete
- ✅ UserServlet - Complete
- ⚠️ TaskServlet - Needs implementation (NEXT)
- ⚠️ PlanServlet - Needs implementation
- ⚠️ BuddyMatchServlet - Needs implementation
- ⚠️ MessageServlet - Needs implementation
- ⚠️ FeedbackServlet - Needs implementation
- ⚠️ NotificationServlet - Needs implementation

## 📋 Next Steps

1. Implement TaskServlet (most critical)
2. Test with frontend TaskList page
3. Implement PlanServlet
4. Test with frontend PlanList page
5. Continue with remaining servlets

**Ready to start implementing TaskServlet?**
