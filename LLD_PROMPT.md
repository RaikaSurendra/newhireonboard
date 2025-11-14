# Low-Level Design (LLD) Prompt for Onboarding Buddy Application

## Context
Based on the approved High-Level Design (HLD), I need a detailed Low-Level Design that provides implementation-level specifications for developers.

## Objective
Create a comprehensive Low-Level Design document with detailed technical specifications, API contracts, database schemas, and component interactions.

## Tech Stack Requirements

### Backend
- **Java Servlets** - Core backend implementation
- **Embedded Tomcat** - Application server (no external server required)
  - Maven/Gradle plugin for embedded Tomcat configuration
  - Standalone JAR deployment capability
  - Hot reload support for development
- **Database** - MySQL/PostgreSQL with JDBC
- **Build Tool** - Maven or Gradle
- **Authentication** - Basic Auth, OAUTH and JWT-based authentication with servlet filters
- **Logging** - SLF4J with Logback
- **Testing** - JUnit 5, Mockito

### Frontend
- **Framework** - Modern JavaScript framework (React/Vue.js)
- **UI Theme** - ServiceNow-style design system
  - Clean, professional enterprise UI
  - Consistent color palette (blues, grays, whites)
  - Card-based layouts
  - Modern typography and spacing
  - Responsive grid system
- **UI Components** - Component library matching ServiceNow aesthetics
  - Navigation: Top navbar with breadcrumbs
  - Tables: Sortable, filterable data tables
  - Forms: Clean form layouts with validation
  - Modals: Slide-in panels and modal dialogs
  - Buttons: Primary, secondary, tertiary styles
  - Icons: Professional icon set
- **Styling** - CSS/SCSS with ServiceNow-inspired theme variables
- **State Management** - Redux/Vuex (if needed)
- **HTTP Client** - Axios or Fetch API
- **Build Tool** - Webpack/Vite

### Deployment Architecture
- **Packaging** - Single executable JAR with embedded Tomcat
- **Static Resources** - Frontend assets bundled within JAR or served separately
- **Configuration** - Externalized configuration (application.properties)
- **Port Configuration** - Configurable HTTP/HTTPS ports
- **Database Connection** - Connection pooling (HikariCP)
- **Deployment Options**:
  - Standalone: `java -jar onboard-buddy.jar`
  - Docker container with embedded Tomcat
  - Cloud deployment (AWS, Azure, GCP)

### Development Setup
- **IDE** - IntelliJ IDEA or Eclipse
- **Java Version** - Java 11 or higher
- **Local Development** - Embedded Tomcat with hot reload
- **API Testing** - Postman/Insomnia collections
- **Version Control** - Git with proper .gitignore

---

## Please provide:

### 1. **Detailed Component Design**

#### 1.1 User Management Module
- User registration and authentication flow
- Password management and security
- Profile management (new employee vs buddy vs admin)
- User roles and permissions matrix
- Session management

#### 1.2 Buddy Matching Engine
- Matching algorithm (criteria: skills, department, experience level, availability)
- Matching workflow and state transitions
- Conflict resolution (unmatching, reassignment)
- Matching history and analytics

#### 1.3 Onboarding Task Management
- Task creation and assignment workflow
- Task status tracking (pending, in-progress, completed, blocked)
- Dependency management between tasks
- Task templates and customization
- Deadline management and reminders

#### 1.4 Communication Module
- Direct messaging between buddy and new employee
- Message persistence and retrieval
- Notification system (in-app, email, SMS)
- Notification preferences and opt-out management
- Message search and filtering

#### 1.5 Feedback & Rating System
- Feedback submission workflow
- Rating scales and criteria
- Anonymous feedback option
- Feedback aggregation and analytics
- Performance metrics calculation

#### 1.6 Analytics & Reporting
- Dashboard data models
- Key metrics (onboarding completion rate, buddy effectiveness, time-to-productivity)
- Report generation and export
- Data aggregation queries

#### 1.7 Admin Panel
- User management operations (CRUD)
- System configuration and settings
- Audit logs and activity tracking
- Bulk operations (import/export)

### 2. **Database Design**

#### 2.1 Entity-Relationship Diagram (ERD)
- Detailed schema for each entity
- Primary keys, foreign keys, and indexes
- Data types and constraints
- Relationships (one-to-one, one-to-many, many-to-many)

#### 2.2 Key Tables
```
Users (id, email, name, role, department, created_at, updated_at)
Buddies (id, buddy_user_id, new_employee_id, matched_at, status, ended_at)
Tasks (id, buddy_id, title, description, status, due_date, created_at)
Messages (id, sender_id, receiver_id, content, created_at, read_at)
Feedback (id, from_user_id, to_user_id, rating, comments, created_at)
Notifications (id, user_id, type, content, read, created_at)
```

#### 2.3 Indexing Strategy
- Indexes on frequently queried columns
- Composite indexes for common query patterns
- Performance optimization queries

### 3. **API Specifications**

#### 3.1 Authentication APIs
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout
- POST /api/auth/refresh-token
- POST /api/auth/forgot-password

#### 3.2 User Management APIs
- GET /api/users/{id}
- PUT /api/users/{id}
- GET /api/users (with filters)
- DELETE /api/users/{id}

#### 3.3 Buddy Matching APIs
- POST /api/buddies/match
- GET /api/buddies/{id}
- GET /api/buddies/my-matches
- PUT /api/buddies/{id}/unmatch
- GET /api/buddies/suggestions

#### 3.4 Task Management APIs
- POST /api/tasks
- GET /api/tasks/{id}
- PUT /api/tasks/{id}
- DELETE /api/tasks/{id}
- GET /api/tasks (with filters, pagination)
- PUT /api/tasks/{id}/status

#### 3.5 Communication APIs
- POST /api/messages
- GET /api/messages (with pagination)
- PUT /api/messages/{id}/read
- GET /api/notifications
- PUT /api/notifications/{id}/read

#### 3.6 Feedback APIs
- POST /api/feedback
- GET /api/feedback/{id}
- GET /api/feedback/user/{user_id}
- GET /api/analytics/feedback-summary

#### 3.7 Admin APIs
- GET /api/admin/users
- POST /api/admin/users
- PUT /api/admin/users/{id}
- DELETE /api/admin/users/{id}
- GET /api/admin/reports

### 4. **API Request/Response Schemas**

For each API endpoint, provide:
- Request body schema (with validation rules)
- Response body schema
- HTTP status codes and error messages
- Example requests and responses

### 5. **Class/Service Design**

#### 5.1 Servlet Controllers
- **AuthServlet** - Handles authentication endpoints (login, register, logout)
  - doPost() for login/register
  - JWT token generation and validation
  - Session management
- **UserServlet** - User CRUD operations
  - doGet() for retrieving user profiles
  - doPut() for updating profiles
  - doDelete() for user deletion
- **BuddyServlet** - Buddy matching operations
  - POST for creating matches
  - GET for retrieving matches
  - PUT for updating match status
- **TaskServlet** - Task management
  - RESTful operations (GET, POST, PUT, DELETE)
  - Query parameter handling for filters
- **FeedbackServlet** - Feedback and ratings
  - POST for submitting feedback
  - GET for retrieving feedback analytics

#### 5.2 Core Services
- UserService (registration, profile management)
- BuddyMatchingService (matching algorithm, state management)
- TaskService (CRUD, status updates, notifications)
- MessageService (send, retrieve, mark as read)
- FeedbackService (submit, aggregate, analytics)
- NotificationService (send, track, preferences)
- AuthenticationService (login, token management)

#### 5.3 Service Method Signatures
For each service, define:
- Method name and purpose
- Input parameters and types
- Return type and structure
- Exceptions/error handling
- Business logic flow

#### 5.4 Design Patterns
- Repository pattern for data access
- Service layer for business logic
- Factory pattern for object creation
- Observer pattern for notifications
- Strategy pattern for matching algorithm

### 6. **State Machines & Workflows**

#### 6.1 Buddy Matching States
- PENDING → MATCHED → ACTIVE → COMPLETED/ENDED

#### 6.2 Task States
- PENDING → IN_PROGRESS → COMPLETED/BLOCKED

#### 6.3 User Onboarding States
- REGISTERED → ASSIGNED_BUDDY → IN_ONBOARDING → COMPLETED

### 7. **Error Handling & Validation**

- Input validation rules
- Business logic validation
- Error codes and messages
- Exception hierarchy
- Logging strategy

### 8. **Security Implementation**

- Password hashing algorithm (bcrypt, Argon2)
- JWT token structure and expiration
- CORS configuration
- SQL injection prevention
- XSS protection
- Rate limiting strategy
- Data encryption (at rest and in transit)

### 9. **Performance Optimization**

- Database query optimization
- Caching strategy (Redis, in-memory)
- Pagination implementation
- Lazy loading vs eager loading
- Connection pooling
- Asynchronous processing (message queues)

### 10. **Testing Strategy**

- Unit test cases for each service
- Integration test scenarios
- API endpoint testing
- Database testing
- Performance testing approach

### 11. **Deployment & DevOps**

#### 11.1 Embedded Tomcat Configuration
- Main class setup with embedded Tomcat initialization
- Servlet registration and URL mapping
- Filter chain configuration
- Context path and port configuration
- SSL/TLS configuration (optional)
- Static resource handling
- Session management configuration
- Connection pool setup (HikariCP)

#### 11.2 Maven/Gradle Build Configuration
- Embedded Tomcat dependencies
- Maven Shade/Assembly plugin for fat JAR creation
- Build profiles (dev, test, prod)
- Resource filtering and property substitution
- Frontend build integration

#### 11.3 Deployment Strategy
- Docker containerization
- Environment configuration management
- Database migration strategy (Flyway/Liquibase)
- Monitoring and logging (ELK stack integration)
- Health check endpoints (/health, /metrics)
- Graceful shutdown handling

#### 11.4 ServiceNow Theme Implementation
- CSS variables for theme customization:
  - Primary colors: #0066B3 (ServiceNow blue), #2E3A4A (dark gray)
  - Secondary colors: #5A6C7D, #F5F7F9 (light backgrounds)
  - Success: #00A651, Warning: #FF9800, Error: #D93025
- Typography: System fonts (Segoe UI, Roboto, sans-serif)
- Component styling guidelines:
  - Card shadows: subtle elevation (0 2px 4px rgba(0,0,0,0.1))
  - Border radius: 4px for cards, 2px for inputs
  - Spacing: 8px base unit (8, 16, 24, 32px)
  - Button heights: 32px (default), 40px (large)
- Responsive breakpoints: 768px (tablet), 1024px (desktop)
- Dark mode support (optional)

### 12. **Code Structure & Organization**

```
onboard-buddy-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/onboardbuddy/
│   │   │       ├── servlet/          # Servlet controllers
│   │   │       │   ├── AuthServlet.java
│   │   │       │   ├── UserServlet.java
│   │   │       │   ├── BuddyServlet.java
│   │   │       │   ├── TaskServlet.java
│   │   │       │   └── FeedbackServlet.java
│   │   │       ├── filter/           # Servlet filters
│   │   │       │   ├── AuthenticationFilter.java
│   │   │       │   ├── CorsFilter.java
│   │   │       │   └── LoggingFilter.java
│   │   │       ├── service/          # Business logic
│   │   │       │   ├── UserService.java
│   │   │       │   ├── BuddyMatchingService.java
│   │   │       │   ├── TaskService.java
│   │   │       │   └── NotificationService.java
│   │   │       ├── repository/       # Data access layer
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── BuddyRepository.java
│   │   │       │   └── TaskRepository.java
│   │   │       ├── model/            # Domain entities
│   │   │       │   ├── User.java
│   │   │       │   ├── Buddy.java
│   │   │       │   └── Task.java
│   │   │       ├── dto/              # Data transfer objects
│   │   │       │   ├── request/
│   │   │       │   └── response/
│   │   │       ├── exception/        # Custom exceptions
│   │   │       ├── util/             # Utility classes
│   │   │       │   ├── JsonUtil.java
│   │   │       │   ├── JwtUtil.java
│   │   │       │   └── DatabaseUtil.java
│   │   │       ├── config/           # Configuration
│   │   │       │   └── DatabaseConfig.java
│   │   │       └── Main.java         # Embedded Tomcat launcher
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── logback.xml
│   │   │   └── db/
│   │   │       └── schema.sql
│   │   └── webapp/                   # Frontend assets
│   │       ├── index.html
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── servicenow-theme.css
│   │       │   ├── js/
│   │       │   │   ├── app.js
│   │       │   │   └── components/
│   │       │   └── images/
│   │       └── WEB-INF/
│   │           └── web.xml           # Servlet mappings
│   └── test/
│       └── java/
│           └── com/onboardbuddy/
│               ├── servlet/
│               ├── service/
│               └── repository/
├── pom.xml                           # Maven configuration
├── Dockerfile                        # Docker deployment
└── README.md
```

---

## Output Format
- Detailed specifications with code examples where applicable
- Clear diagrams (ER diagrams, sequence diagrams, state diagrams)
- API documentation with examples
- Implementation guidelines and best practices

## Implementation Ready
After LLD approval, the development team should have all necessary details to start implementation.
