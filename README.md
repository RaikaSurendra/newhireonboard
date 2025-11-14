# Onboarding Buddy Application

A comprehensive onboarding management system with buddy matching, task management, and progress tracking.

## 🚀 Quick Start

### Prerequisites
- Java 11+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+
- Make (optional, for using Makefile)

### First Time Setup

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env and set your JWT_SECRET and DB_PASSWORD

# 2. Quick setup (with Make)
make quickstart

# OR Manual setup:
# Install dependencies
make install  # or: cd backend && mvn install && cd ../frontend && npm install

# Setup database
make db-setup  # or: mysql -u root -p < database/schema.sql

# Build application
make build  # or: cd backend && mvn clean package && cd ../frontend && npm run build

# Run application
make run  # or: java -jar backend/target/onboard-buddy-1.0.0.jar
```

### Development Mode

```bash
# Start both backend and frontend in development mode
make dev

# Or manually in separate terminals:
# Terminal 1 - Backend
cd backend && mvn exec:java -Dexec.mainClass="com.onboardbuddy.Application"

# Terminal 2 - Frontend
cd frontend && npm run dev
```

**Access the application:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Default credentials: `admin@onboardbuddy.com` / `admin123`

### Using the Makefile

```bash
make help          # Show all available commands
make install       # Install dependencies
make build         # Build application
make test          # Run tests
make clean         # Clean build artifacts
make db-setup      # Setup database
make logs          # View application logs
make status        # Check if services are running
make stop          # Stop all services
```

## Testing

### Load Testing & Chaos Engineering

Comprehensive load testing and chaos engineering setup included:

```bash
# Quick smoke test
cd load-testing/scripts
./run-all-tests.sh

# Run Gatling load tests
cd backend
mvn gatling:test

# Run Artillery tests
cd load-testing/artillery
npm install
artillery run scenarios/api-load-test.yml

# Run k6 tests
cd load-testing/k6
k6 run scripts/api-load-test.js

# Run chaos experiments
cd load-testing/chaos
chaos run experiments/database-latency.json
```

**Testing Documentation:**
- [Quick Start Guide](load-testing/QUICK_START.md)
- [Monitoring Guide](load-testing/MONITORING.md)
- [CI/CD Integration](load-testing/CI_CD_INTEGRATION.md)
- [Full Documentation](load-testing/README.md)

## 📊 Business Flow

### Onboarding Journey

```
1. New Employee Registration
   ↓
2. HR Creates Onboarding Plan
   ↓
3. Buddy Matching Algorithm
   ↓
4. Buddy Assignment & Notification
   ↓
5. Onboarding Tasks Generated
   ↓
6. Daily Check-ins & Progress Tracking
   ↓
7. Feedback Collection
   ↓
8. Completion & Review
```

### Key Workflows

#### 1. **Buddy Matching Process**
- New employee profile created with skills, department, and preferences
- Matching algorithm evaluates potential buddies based on:
  - Department alignment
  - Skill overlap
  - Experience level compatibility
  - Current workload
- Match suggestions generated with confidence scores
- HR/Manager reviews and approves matches
- Buddy receives notification and accepts assignment

#### 2. **Onboarding Plan Execution**
- HR creates department-specific onboarding plan templates
- Plans include task templates with:
  - Day offsets (when task should start)
  - Priority levels
  - Assignee types (buddy, manager, HR, IT)
  - Dependencies and parallel execution groups
- When new employee starts, plan is instantiated
- Tasks automatically assigned based on roles
- Progress tracked in real-time

#### 3. **Communication & Feedback**
- Built-in messaging between buddy and new employee
- Regular check-in reminders
- Feedback collection at milestones:
  - Week 1, Week 2, Month 1, Month 3
- Manager reviews and intervention triggers
- Anonymous feedback options

## 🗄️ Database Schema

### Core Tables

#### **users**
```sql
- id (PK)
- email (unique)
- password_hash
- name, role, department
- skills (JSON), experience_level
- manager_id (FK → users)
- status (ACTIVE, INACTIVE, ON_LEAVE)
```

#### **onboarding_plans**
```sql
- id (PK)
- name, description, department
- version, duration_days
- is_active, published_at
- created_by (FK → users)
```

#### **task_templates**
```sql
- id (PK)
- plan_id (FK → onboarding_plans)
- name, description, priority
- day_offset, estimated_duration
- task_type, owner_type, assignee_type
- execution_mode (SEQUENTIAL, PARALLEL)
- sequence_order, parallel_group
```

#### **buddy_matches**
```sql
- id (PK)
- buddy_user_id (FK → users)
- new_employee_id (FK → users)
- status (PENDING, ACCEPTED, ACTIVE, COMPLETED)
- match_score, matched_at, accepted_at
```

#### **onboarding_runs**
```sql
- id (PK)
- plan_id (FK → onboarding_plans)
- employee_id (FK → users)
- buddy_id (FK → users)
- status, start_date, expected_end_date
- completion_percentage
```

#### **tasks**
```sql
- id (PK)
- run_id (FK → onboarding_runs)
- template_id (FK → task_templates)
- assigned_to (FK → users)
- status, priority, due_date
- completed_at, completion_notes
```

#### **messages**
```sql
- id (PK)
- sender_id (FK → users)
- receiver_id (FK → users)
- content, is_read
- created_at, read_at
```

#### **feedback**
```sql
- id (PK)
- match_id (FK → buddy_matches)
- from_user_id (FK → users)
- to_user_id (FK → users)
- rating (1-5), comments
- feedback_type (BUDDY_TO_EMPLOYEE, EMPLOYEE_TO_BUDDY, MANAGER_REVIEW)
```

#### **notifications**
```sql
- id (PK)
- user_id (FK → users)
- title, message, type
- is_read, priority
- related_entity_type, related_entity_id
```

### Key Relationships
- **One-to-Many**: User → Buddy Matches (as buddy)
- **One-to-Many**: User → Buddy Matches (as new employee)
- **One-to-Many**: Onboarding Plan → Task Templates
- **One-to-Many**: Onboarding Run → Tasks
- **Many-to-Many**: Users ↔ Messages (sender/receiver)

### Indexes for Performance
- `idx_user_email` on users(email)
- `idx_match_status` on buddy_matches(status)
- `idx_task_assigned` on tasks(assigned_to, status)
- `idx_message_unread` on messages(receiver_id, is_read)
- `idx_notification_user` on notifications(user_id, is_read)

## Documentation
- [Installation Guide](docs/INSTALLATION_GUIDE.md)
- [Project Setup](docs/PROJECT_SETUP.md)
- [Build and Run Guide](docs/BUILD_AND_RUN.md)
- [High-Level Design](docs/HLD_DOCUMENT.md)
- [Low-Level Design](docs/LLD_DOCUMENT.md)

## 🔒 Security Features

This application includes comprehensive security features:

- ✅ **Environment-based Configuration** - Secrets externalized via environment variables
- ✅ **Input Validation** - All user inputs validated against security policies
- ✅ **Rate Limiting** - Protection against brute force and DoS attacks
- ✅ **Password Policy Enforcement** - Strong password requirements
- ✅ **JWT Token Revocation** - Proper logout with token blacklisting
- ✅ **Request Size Limits** - Protection against large payload attacks
- ✅ **Sanitized Logging** - No sensitive data in logs
- ✅ **Graceful Shutdown** - Proper resource cleanup

See [FIXES_APPLIED.md](FIXES_APPLIED.md) for complete details on all security improvements.

## 🔧 Configuration

The application uses a three-tier configuration system:
1. **Default** - `application.properties` (committed to repo)
2. **External File** - Specified via `-Dconfig.file=path/to/config.properties`
3. **Environment Variables** - Highest priority (recommended for production)

**Critical Environment Variables:**
```bash
export JWT_SECRET=$(openssl rand -base64 64)  # Required: min 256 bits
export DB_PASSWORD=your_secure_password       # Required
export DB_URL=jdbc:mysql://localhost:3306/onboard_buddy
export RATE_LIMIT_ENABLED=true
export RATE_LIMIT_LOGIN_ATTEMPTS=5
```

See `.env.example` for all available configuration options.

## License
MIT
