# Low-Level Design Document
# Onboarding Buddy Application

**Version:** 1.0  
**Date:** November 14, 2025  
**Status:** Draft

---

## Table of Contents
1. [Component Design](#1-component-design)
2. [Database Design](#2-database-design)
3. [API Specifications](#3-api-specifications)
4. [Class/Service Design](#4-classservice-design)
5. [State Machines & Workflows](#5-state-machines--workflows)
6. [Error Handling & Validation](#6-error-handling--validation)
7. [Security Implementation](#7-security-implementation)
8. [Performance Optimization](#8-performance-optimization)
9. [Testing Strategy](#9-testing-strategy)
10. [Deployment Configuration](#10-deployment-configuration)

---

## Tech Stack Summary

**Backend:**
- Java Servlets with Embedded Tomcat
- MySQL/PostgreSQL with JDBC
- Authentication: Basic Auth, OAuth 2.0, JWT
- Build: Maven with embedded Tomcat plugin

**Frontend:**
- **React 18+** with TypeScript
- **UI Framework:** Material-UI (MUI) or Ant Design with ServiceNow-style theme customization
- **State Management:** Redux Toolkit or Zustand
- **HTTP Client:** Axios with interceptors
- **Routing:** React Router v6
- **Build Tool:** Vite
- **Icons:** Lucide React or Material Icons
- **Styling:** TailwindCSS + CSS Modules
- **Form Handling:** React Hook Form + Zod validation

**Deployment:**
- Single executable JAR: `java -jar onboard-buddy.jar`
- React build served as static assets from `/webapp` in JAR
- Docker support
- Externalized configuration

---

## 1. Component Design

### 1.1 User Management Module

#### Authentication Flow

**1. Basic Authentication**
```
User submits credentials → Validate against DB → BCrypt verification 
→ Create session (30-min timeout) → Return session ID in cookie
```

**2. OAuth 2.0 (Google/Microsoft)**
```
Redirect to provider → Get authorization code → Exchange for access token
→ Fetch user profile → Create/update user → Generate internal JWT
```

**3. JWT Authentication**
```
Validate credentials → Generate JWT (24h expiry) + Refresh token (7d)
→ Token payload: {userId, role, email, exp} → Return both tokens
```

#### User Roles & Permissions Matrix

| Permission | New Employee | Buddy | Admin | HR Manager |
|------------|--------------|-------|-------|------------|
| View own profile | ✓ | ✓ | ✓ | ✓ |
| Edit own profile | ✓ | ✓ | ✓ | ✓ |
| View buddy profile | ✓ | ✓ | ✓ | ✓ |
| Create tasks | ✗ | ✓ | ✓ | ✓ |
| Complete tasks | ✓ | ✓ | ✓ | ✓ |
| Send messages | ✓ | ✓ | ✓ | ✓ |
| Submit feedback | ✓ | ✓ | ✓ | ✓ |
| View analytics | ✗ | Limited | ✓ | ✓ |
| Manage users | ✗ | ✗ | ✓ | ✓ |
| Create buddy matches | ✗ | ✗ | ✓ | ✓ |
| System configuration | ✗ | ✗ | ✓ | ✗ |

#### Password Requirements
- **Length:** Minimum 8 characters
- **Complexity:** Must contain uppercase, lowercase, digit, special character
- **History:** Last 5 passwords stored (hashed)
- **Expiration:** 90 days (configurable)
- **Hashing:** BCrypt with cost factor 12

#### Session Management
- **Storage:** In-memory ConcurrentHashMap
- **Timeout:** 30 minutes (configurable)
- **Cleanup:** Background job runs every 5 minutes
- **Remember Me:** Extends session to 30 days
- **Concurrent Sessions:** Max 3 per user

---

### 1.2 Buddy Matching Engine

#### Matching Algorithm

**Score Calculation:**
```
Total Score = (Skill Match × 0.4) + (Department Match × 0.3) + 
              (Experience Level × 0.2) + (Availability × 0.1)

Minimum Threshold: 0.6 (60%)
```

**Skill Match Formula:**
```
Jaccard Similarity = |Skills1 ∩ Skills2| / |Skills1 ∪ Skills2|
```

**Experience Level Scoring:**
- Senior buddy + Junior employee: 1.0
- Mid buddy + Junior employee: 0.8
- Senior buddy + Mid employee: 0.9
- Same level: 0.5

#### Matching Workflow

```
1. New employee completes profile
2. System runs matching algorithm
3. Top 5 matches presented to HR/Admin
4. Admin selects buddy and sends invitation
5. Buddy receives notification (48h to respond)
6. If accepted → Status: ACTIVE, create onboarding tasks
7. If rejected → Offer next suggestion
8. If no response → Auto-escalate to HR
```

#### State Transitions

```
PENDING → SUGGESTED → ACCEPTED → ACTIVE → COMPLETED
                                    ↓
                                  ENDED (with reason)
```

#### Conflict Resolution

**Conflict Types:**
- `BUDDY_UNAVAILABLE` - Buddy on leave/resigned → Auto-reassign
- `PERSONALITY_MISMATCH` - Reported by either party → Reassign with feedback
- `WORKLOAD_OVERLOAD` - Buddy has too many mentees → Redistribute
- `PERFORMANCE_ISSUE` - Poor ratings → Escalate to manager

**Resolution Actions:**
1. Log conflict in audit trail
2. Notify stakeholders (HR, managers)
3. Execute resolution strategy
4. Update match status
5. Create new match if needed

---

### 1.3 Onboarding Task Management

#### Onboarding Plan System

**Plan Hierarchy:**
```
Onboarding Plan (Parent)
├── Plan Version 1.0
│   ├── Task Template 1
│   ├── Task Template 2
│   └── Task Template 3
├── Plan Version 2.0 (updated)
│   ├── Task Template 1 (modified)
│   ├── Task Template 2 (same)
│   ├── Task Template 3 (same)
│   └── Task Template 4 (new)
```

**Plan Management:**
- **NO static/default plans** - All plans must be configured by Managers/SPOCs
- Managers create **Onboarding Plans** as parent records
- Each plan contains multiple task templates as children
- Plans support versioning - entire plan versioned together
- When plan is updated, new version created with all templates
- Onboarding runs reference specific plan version (not individual templates)
- Plans can be department-specific or organization-wide
- Plans must be created and activated before onboarding can begin

**Onboarding Plan Structure:**
```java
public class OnboardingPlan {
    private Long id;
    private String name;                    // e.g., "Engineering Onboarding 90-Day"
    private String description;
    private int version;                    // Auto-incremented on changes
    private Long createdBy;                 // Manager/SPOC user ID
    private String department;              // null for org-wide
    private int durationDays;               // Expected duration (e.g., 90)
    private boolean isActive;               // Can be activated/deactivated
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;      // When made available for use
}

public class OnboardingPlanVersion {
    private Long id;
    private Long planId;
    private int version;
    private String changeReason;
    private Long changedBy;
    private LocalDateTime createdAt;
    // Snapshot of plan metadata at this version
}
```

**Task Template System:**
- Task templates are children of Onboarding Plans
- Templates belong to a specific plan
- When plan is versioned, all templates are versioned together
- Templates cannot exist without a parent plan

**Template Structure:**
```java
public class TaskTemplate {
    private Long id;
    private Long planId;                    // Parent onboarding plan
    private int planVersion;                // Plan version this template belongs to
    private String name;
    private String description;
    private Long createdBy;                 // Manager/SPOC user ID
    private TaskPriority priority;
    private int dayOffset;                  // Days after onboarding start
    private int estimatedDuration;          // In days
    
    // Task Type and Ownership
    private TaskType taskType;              // ADMINISTRATIVE, TECHNICAL, COMPLIANCE, SOCIAL, PROJECT
    private OwnerType ownerType;            // Who owns/manages this task
    private String assigneeType;            // Who performs the task
    
    // Execution Control
    private ExecutionMode executionMode;    // SEQUENTIAL or PARALLEL
    private int sequenceOrder;              // Order within sequence (for SEQUENTIAL)
    private String parallelGroup;           // Group ID for parallel tasks
    
    // Additional metadata
    private String category;                // Custom categorization
    private List<String> tags;              // Searchable tags
    private Long dependsOnTemplateId;       // Template dependency
    private boolean isActive;               // Can be deactivated
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Execution Mode Enum
public enum ExecutionMode {
    SEQUENTIAL,     // Must complete in order (blocking)
    PARALLEL        // Can be done simultaneously (non-blocking)
}

// Task Type Enum
public enum TaskType {
    ADMINISTRATIVE,     // IT setup, paperwork, access provisioning
    TECHNICAL,          // Training, certifications, tool setup
    COMPLIANCE,         // Mandatory training, policy acknowledgment
    SOCIAL,             // Team introductions, buddy meetings
    PROJECT,            // Actual work assignments
    REVIEW              // Check-ins, performance reviews
}

// Owner Type Enum - Who owns/manages the task
public enum OwnerType {
    MANAGER_OWNED,          // Manager creates and tracks
    NEW_HIRE_OWNED,         // New hire self-manages
    INTERNAL_TEAM_OWNED,    // Internal team (HR, IT, etc.)
    INTERNAL_EMPLOYEE_OWNED,// Specific internal employee (buddy, mentor)
    EXTERNAL_TEAM_OWNED,    // External team/vendor
    SHARED_OWNERSHIP        // Multiple parties collaborate
}

// Assignee Type - Who performs the task
public enum AssigneeType {
    NEW_EMPLOYEE,           // Task assigned to new hire
    BUDDY,                  // Task assigned to buddy
    MANAGER,                // Task assigned to manager
    HR_TEAM,                // Task assigned to HR team
    IT_TEAM,                // Task assigned to IT team
    TEAM_MEMBER,            // Task assigned to specific team member
    TEAM,                   // Task assigned to entire team
    EXTERNAL_VENDOR         // Task assigned to external party
}
```

**Version Control:**
- Each template modification creates a new version
- Version history maintained for audit trail
- Onboarding runs reference specific template version
- Changes to templates only affect new runs
- Rollback capability to previous versions

#### Task Execution Modes

**SEQUENTIAL Tasks:**
- Must be completed in specific order
- Next task blocked until previous completes
- Use `sequenceOrder` to define order (1, 2, 3...)
- Ideal for dependent workflows

**PARALLEL Tasks:**
- Can be started and completed simultaneously
- No blocking between tasks in same group
- Use `parallelGroup` to group related parallel tasks
- Ideal for independent activities

**Mixed Execution Example:**
```
Day 0:
├── SEQUENTIAL Group (order matters):
│   ├── [1] Complete IT Setup (MUST complete first)
│   ├── [2] Access Badge Creation (depends on IT setup)
│   └── [3] System Access Provisioning (depends on badge)
│
└── PARALLEL Group "orientation" (can do simultaneously):
    ├── Read Company Handbook
    ├── Watch Welcome Video
    └── Complete HR Paperwork

Day 3:
└── PARALLEL Group "training" (independent trainings):
    ├── Security Training
    ├── Compliance Training
    └── Tool Training

Day 7:
└── SEQUENTIAL Group (must follow order):
    ├── [1] Shadow Buddy
    ├── [2] First Code Review
    └── [3] First Project Assignment
```

#### Template Configuration Examples

**Example 1: Sequential Task (Ordered)**
```json
{
  "name": "Complete IT Setup",
  "taskType": "ADMINISTRATIVE",
  "ownerType": "INTERNAL_TEAM_OWNED",
  "assigneeType": "IT_TEAM",
  "executionMode": "SEQUENTIAL",
  "sequenceOrder": 1,
  "parallelGroup": null,
  "dayOffset": 0,
  "priority": "HIGH",
  "category": "IT Setup",
  "tags": ["it", "setup", "hardware"]
}
```

**Example 2: Sequential Task (Must Follow Previous)**
```json
{
  "name": "System Access Provisioning",
  "taskType": "ADMINISTRATIVE",
  "ownerType": "INTERNAL_TEAM_OWNED",
  "assigneeType": "IT_TEAM",
  "executionMode": "SEQUENTIAL",
  "sequenceOrder": 2,
  "parallelGroup": null,
  "dayOffset": 0,
  "priority": "HIGH",
  "dependsOnTemplateId": 101,
  "category": "IT Setup",
  "tags": ["it", "access"]
}
```

**Example 3: Parallel Task Group**
```json
{
  "name": "Complete Security Training",
  "taskType": "COMPLIANCE",
  "ownerType": "NEW_HIRE_OWNED",
  "assigneeType": "NEW_EMPLOYEE",
  "executionMode": "PARALLEL",
  "sequenceOrder": null,
  "parallelGroup": "day3-training",
  "dayOffset": 3,
  "priority": "HIGH",
  "category": "Compliance",
  "tags": ["compliance", "security", "mandatory"]
}
```

**Example 4: Another Task in Same Parallel Group**
```json
{
  "name": "Complete Compliance Training",
  "taskType": "COMPLIANCE",
  "ownerType": "NEW_HIRE_OWNED",
  "assigneeType": "NEW_EMPLOYEE",
  "executionMode": "PARALLEL",
  "sequenceOrder": null,
  "parallelGroup": "day3-training",
  "dayOffset": 3,
  "priority": "HIGH",
  "category": "Compliance",
  "tags": ["compliance", "mandatory"]
}
```

**Example 5: External Team Task (Parallel)**
```json
{
  "name": "Background Verification",
  "taskType": "ADMINISTRATIVE",
  "ownerType": "EXTERNAL_TEAM_OWNED",
  "assigneeType": "EXTERNAL_VENDOR",
  "executionMode": "PARALLEL",
  "sequenceOrder": null,
  "parallelGroup": "pre-boarding",
  "dayOffset": -5,
  "priority": "HIGH",
  "category": "Pre-boarding",
  "tags": ["external", "verification", "pre-boarding"]
}
```

#### Onboarding Run Records

**Run Parent Record:**
```java
public class OnboardingRun {
    private Long id;
    private Long buddyMatchId;              // Reference to buddy match
    private Long planId;                    // Onboarding plan used
    private int planVersion;                // Locked plan version
    private String status;                  // ACTIVE, COMPLETED, CANCELLED
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private int totalTasks;
    private int completedTasks;
    private double completionPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Key Changes:**
- Runs now reference `planId` and `planVersion` instead of individual templates
- All tasks in the run come from the same plan version
- Atomic versioning - entire plan is locked at run creation
- Simpler relationship model

**Planned Tasks (Instances):**
- Each run creates task instances from plan's templates
- All templates from the plan version are instantiated
- Tasks are assigned to specific users based on assigneeType
- Task instances are independent of plan/template changes
- Progress tracked per instance, not template

**Task Assignment Logic:**
```
assigneeType = NEW_EMPLOYEE → Assign to new employee
assigneeType = BUDDY → Assign to assigned buddy
assigneeType = MANAGER → Assign to employee's manager
assigneeType = HR_TEAM → Assign to HR team
assigneeType = IT_TEAM → Assign to IT team
assigneeType = TEAM_MEMBER → Assign to specific team member (configurable)
assigneeType = TEAM → Assign to entire team (collaborative task)
assigneeType = EXTERNAL_VENDOR → Assign to external vendor/contractor
```

#### Task Execution Logic

**Sequential Task Execution:**
```java
public class SequentialTaskExecutor {
    
    public void createSequentialTasks(OnboardingRun run, List<TaskTemplate> templates) {
        // Sort templates by sequence_order
        templates.sort(Comparator.comparingInt(TaskTemplate::getSequenceOrder));
        
        for (int i = 0; i < templates.size(); i++) {
            TaskTemplate template = templates.get(i);
            Task task = createTaskFromTemplate(run, template);
            
            // First task is available, rest are blocked
            if (i == 0) {
                task.setStatus(TaskStatus.PENDING);
                task.setIsBlocked(false);
            } else {
                task.setStatus(TaskStatus.BLOCKED);
                task.setIsBlocked(true);
            }
            
            taskRepository.save(task);
        }
    }
    
    public void onTaskCompleted(Task completedTask) {
        if (completedTask.getExecutionMode() != ExecutionMode.SEQUENTIAL) {
            return; // Only handle sequential tasks
        }
        
        // Find next task in sequence
        Task nextTask = taskRepository.findNextSequentialTask(
            completedTask.getOnboardingRunId(),
            completedTask.getSequenceOrder() + 1
        );
        
        if (nextTask != null && nextTask.isBlocked()) {
            // Unblock next task
            nextTask.setIsBlocked(false);
            nextTask.setStatus(TaskStatus.PENDING);
            taskRepository.update(nextTask);
            
            // Notify assignee
            notificationService.notifyTaskUnblocked(nextTask);
        }
    }
}
```

**Parallel Task Execution:**
```java
public class ParallelTaskExecutor {
    
    public void createParallelTasks(OnboardingRun run, List<TaskTemplate> templates) {
        // Group by parallel_group
        Map<String, List<TaskTemplate>> groups = templates.stream()
            .collect(Collectors.groupingBy(TaskTemplate::getParallelGroup));
        
        for (Map.Entry<String, List<TaskTemplate>> entry : groups.entrySet()) {
            String groupId = entry.getKey();
            List<TaskTemplate> groupTemplates = entry.getValue();
            
            for (TaskTemplate template : groupTemplates) {
                Task task = createTaskFromTemplate(run, template);
                
                // All parallel tasks are immediately available
                task.setStatus(TaskStatus.PENDING);
                task.setIsBlocked(false);
                task.setParallelGroup(groupId);
                
                taskRepository.save(task);
            }
        }
    }
    
    public ParallelGroupProgress getGroupProgress(Long runId, String groupId) {
        List<Task> groupTasks = taskRepository.findByRunAndParallelGroup(runId, groupId);
        
        int total = groupTasks.size();
        int completed = (int) groupTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
            .count();
        
        return new ParallelGroupProgress(
            groupId,
            total,
            completed,
            (double) completed / total * 100
        );
    }
}
```

**Mixed Execution Example:**
```java
public void createOnboardingTasks(OnboardingRun run) {
    List<TaskTemplate> templates = templateRepository.findActiveByDepartment(
        run.getDepartment()
    );
    
    // Separate by execution mode
    List<TaskTemplate> sequentialTemplates = templates.stream()
        .filter(t -> t.getExecutionMode() == ExecutionMode.SEQUENTIAL)
        .collect(Collectors.toList());
    
    List<TaskTemplate> parallelTemplates = templates.stream()
        .filter(t -> t.getExecutionMode() == ExecutionMode.PARALLEL)
        .collect(Collectors.toList());
    
    // Create sequential tasks (with blocking)
    sequentialTaskExecutor.createSequentialTasks(run, sequentialTemplates);
    
    // Create parallel tasks (all available)
    parallelTaskExecutor.createParallelTasks(run, parallelTemplates);
}
```

**Business Rules:**
1. **Sequential Tasks:**
   - Must have `sequenceOrder` defined (1, 2, 3...)
   - Only first task (order=1) is available initially
   - Subsequent tasks are BLOCKED until previous completes
   - When task N completes, task N+1 is automatically unblocked
   - User cannot start blocked tasks

2. **Parallel Tasks:**
   - Must have `parallelGroup` defined
   - All tasks in group are available immediately
   - Can be started/completed in any order
   - No dependencies within the group
   - Progress tracked per group

3. **Mixed Workflows:**
   - Can have both sequential and parallel tasks in same run
   - Sequential tasks block only within their sequence
   - Parallel tasks never block each other
   - Different parallel groups are independent

**Validation Rules:**
```java
public ValidationResult validateTaskTemplate(TaskTemplate template) {
    ValidationResult result = new ValidationResult();
    
    if (template.getExecutionMode() == ExecutionMode.SEQUENTIAL) {
        if (template.getSequenceOrder() == null || template.getSequenceOrder() < 1) {
            result.addError("sequenceOrder", 
                "Sequential tasks must have sequenceOrder >= 1");
        }
        if (template.getParallelGroup() != null) {
            result.addError("parallelGroup", 
                "Sequential tasks cannot have parallelGroup");
        }
    }
    
    if (template.getExecutionMode() == ExecutionMode.PARALLEL) {
        if (template.getParallelGroup() == null || template.getParallelGroup().isEmpty()) {
            result.addError("parallelGroup", 
                "Parallel tasks must have parallelGroup defined");
        }
        if (template.getSequenceOrder() != null) {
            result.addError("sequenceOrder", 
                "Parallel tasks cannot have sequenceOrder");
        }
    }
    
    return result;
}
```

#### Template Management Workflow

**Initial Setup (Required before onboarding):**
```
1. System Admin/HR Manager logs in
2. Navigate to Template Management
3. Check if templates exist for department
4. If no templates:
   └── System shows warning: "No templates configured. Create templates to begin onboarding."
5. Create templates by owner type:
   
   Manager-Owned Templates:
   ├── Performance reviews
   ├── Project assignments
   └── Goal setting sessions
   
   New Hire-Owned Templates:
   ├── Self-paced training
   ├── Documentation reading
   └── Compliance certifications
   
   Internal Team-Owned Templates:
   ├── IT setup (IT_TEAM)
   ├── HR paperwork (HR_TEAM)
   └── Access provisioning (IT_TEAM)
   
   Internal Employee-Owned Templates:
   ├── Buddy introductions
   ├── Mentorship sessions
   └── Team shadowing
   
   External Team-Owned Templates:
   ├── Background verification
   ├── Drug screening
   └── Equipment delivery
   
6. Set dependencies between templates
7. Activate templates
8. Templates now available for onboarding runs
```

**Template Lifecycle:**
```
CREATE → ACTIVE → (MODIFY → NEW VERSION) → DEACTIVATE
                       ↓
                  Old version still used by in-progress runs
                  New version used by new runs
```

**Validation Rules:**
- At least one template must exist before creating onboarding run
- Templates must have valid assigneeType for the organization
- External vendor tasks require vendor contact information
- Pre-boarding tasks (negative dayOffset) must be completed before day 0
```

#### Task Status Flow

```
PENDING → IN_PROGRESS → COMPLETED
            ↓
          BLOCKED → (back to) IN_PROGRESS
            ↓
        CANCELLED
```

**Valid Transitions:**
- PENDING → IN_PROGRESS, CANCELLED
- IN_PROGRESS → BLOCKED, COMPLETED, CANCELLED
- BLOCKED → IN_PROGRESS, CANCELLED
- COMPLETED → (terminal state)
- CANCELLED → (terminal state)

#### Dependency Management

**Dependency Types:**
- `FINISH_TO_START` - Task B starts after Task A completes
- `START_TO_START` - Task B starts when Task A starts

**Example:**
```
Task: "First Project Assignment" 
  depends on "Complete IT Setup" (FINISH_TO_START)
  depends on "Shadow Buddy" (FINISH_TO_START)
```

#### Deadline Reminders

**Automated Notifications:**
- **3 days before:** Gentle reminder
- **1 day before:** Urgent reminder
- **Same day:** Critical alert
- **Overdue:** Daily escalation
- **7+ days overdue:** Manager escalation

---

### 1.4 Communication Module

#### Direct Messaging

**Features:**
- One-on-one messaging between buddy pairs
- Real-time delivery via WebSocket
- Message persistence in database
- Read receipts
- Offline email notifications
- Message search and filtering

**Message Flow:**
```
1. User sends message
2. Validate buddy relationship
3. Sanitize content (XSS prevention)
4. Save to database
5. Send via WebSocket if recipient online
6. Send email if recipient offline
7. Create in-app notification
```

#### Notification System

**Notification Types:**

| Type | In-App | Email | SMS | Priority |
|------|--------|-------|-----|----------|
| TASK_ASSIGNED | ✓ | ✓ | ✗ | MEDIUM |
| TASK_DUE_SOON | ✓ | ✓ | ✗ | MEDIUM |
| TASK_OVERDUE | ✓ | ✓ | ✓ | HIGH |
| MESSAGE_RECEIVED | ✓ | Optional | ✗ | LOW |
| BUDDY_MATCHED | ✓ | ✓ | ✗ | HIGH |
| FEEDBACK_RECEIVED | ✓ | ✓ | ✗ | MEDIUM |
| SYSTEM_ANNOUNCEMENT | ✓ | ✓ | ✗ | MEDIUM |

**Notification Preferences:**
- User-configurable per notification type
- Quiet hours support (e.g., 22:00 - 08:00)
- Daily digest option (consolidated email)
- Opt-out capability

---

### 1.5 Feedback & Rating System

#### Rating Criteria

**Overall Rating:** 1-5 stars

**Detailed Criteria (each 1-5):**
- Communication - How well did your buddy communicate?
- Knowledge - How knowledgeable was your buddy?
- Availability - How available was your buddy?
- Supportiveness - How supportive was your buddy?
- Responsiveness - How quickly did your buddy respond?

#### Feedback Periods

| Period | Day Offset | Purpose |
|--------|------------|---------|
| WEEK_1 | 7 | Initial impressions |
| WEEK_2 | 14 | Early progress check |
| MONTH_1 | 30 | First milestone |
| MONTH_2 | 60 | Mid-point review |
| FINAL | 90 | Complete assessment |

#### Anonymous Feedback

- User can choose to submit anonymously
- Anonymous feedback not shown to recipient with sender info
- Still counted in aggregate statistics
- Admin can view sender for audit purposes

#### Feedback Aggregation

**Metrics Calculated:**
- Average overall rating
- Average per criterion
- Rating distribution (1-5 star breakdown)
- Trend analysis (improving/declining)
- Common themes extraction from comments

---

### 1.6 Analytics & Reporting

#### Key Performance Indicators

**Onboarding Metrics:**
- **Completion Rate:** (Completed / Total) × 100
- **Average Time to Productivity:** Days from start to completion
- **Drop-off Rate:** Percentage of incomplete onboardings

**Buddy Metrics:**
- **Buddy Effectiveness:** Average feedback rating
- **Active Buddies:** Currently mentoring
- **Capacity Utilization:** Current mentees / Max capacity

**Task Metrics:**
- **Task Completion Rate:** (Completed / Total) × 100
- **Average Completion Time:** Days to complete tasks
- **Overdue Rate:** Percentage of overdue tasks

**Engagement Metrics:**
- **Message Volume:** Total messages exchanged
- **Response Time:** Average time to respond
- **Feedback Submission Rate:** Percentage who submitted feedback

**Retention Metrics:**
- **90-Day Retention:** Employees still active after 90 days
- **6-Month Retention:** Long-term retention rate

#### Dashboard Views

**Admin Dashboard:**
- Real-time overview of all onboardings
- Active matches and their status
- Task completion statistics
- Recent feedback and ratings
- System health metrics

**Buddy Dashboard:**
- Current mentees and their progress
- Upcoming tasks and deadlines
- Recent messages
- Personal performance metrics

**New Employee Dashboard:**
- Onboarding progress tracker
- Task list with priorities
- Buddy contact information
- Upcoming milestones

---

## 2. Database Design

### 2.1 Entity-Relationship Diagram

```
┌──────────────┐         ┌──────────────────┐         ┌──────────────┐
│    Users     │◄───────►│  BuddyMatches    │◄───────►│    Tasks     │
│              │         │                  │         │              │
│ - id (PK)    │         │ - id (PK)        │         │ - id (PK)    │
│ - email      │         │ - buddy_id (FK)  │         │ - match_id   │
│ - name       │         │ - employee_id    │         │ - title      │
│ - role       │         │ - status         │         │ - status     │
│ - department │         │ - match_score    │         │ - due_date   │
└──────────────┘         └──────────────────┘         └──────────────┘
      │                          │                           │
      │                          │                           │
      ▼                          ▼                           ▼
┌──────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Messages   │         │    Feedback      │         │ TaskHistory  │
│              │         │                  │         │              │
│ - id (PK)    │         │ - id (PK)        │         │ - id (PK)    │
│ - sender_id  │         │ - match_id (FK)  │         │ - task_id    │
│ - receiver_id│         │ - rating         │         │ - old_status │
│ - content    │         │ - comments       │         │ - new_status │
└──────────────┘         └──────────────────┘         └──────────────┘
```

### 2.2 Table Schemas

#### Onboarding Plans Table
```sql
CREATE TABLE onboarding_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    department VARCHAR(100),                -- NULL for org-wide
    duration_days INT NOT NULL,             -- Expected duration (e.g., 90)
    is_active BOOLEAN DEFAULT FALSE,        -- Must be activated to use
    published_at TIMESTAMP NULL,            -- When made available
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    
    INDEX idx_department (department),
    INDEX idx_active (is_active),
    INDEX idx_version (id, version),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Plan version history
CREATE TABLE onboarding_plan_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    version INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_days INT,
    changed_by BIGINT NOT NULL,
    change_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (plan_id) REFERENCES onboarding_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id),
    
    UNIQUE KEY unique_plan_version (plan_id, version),
    INDEX idx_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role ENUM('NEW_EMPLOYEE', 'BUDDY', 'ADMIN', 'HR_MANAGER') NOT NULL,
    department VARCHAR(100),
    skills JSON,
    experience_level ENUM('JUNIOR', 'MID', 'SENIOR'),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status ENUM('ACTIVE', 'INACTIVE', 'PENDING') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_department (department),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### BuddyMatches Table
```sql
CREATE TABLE buddy_matches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    buddy_user_id BIGINT NOT NULL,
    new_employee_id BIGINT NOT NULL,
    status ENUM('PENDING', 'SUGGESTED', 'ACCEPTED', 'ACTIVE', 
                'COMPLETED', 'ENDED') NOT NULL,
    match_score DECIMAL(3,2),
    matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    end_reason VARCHAR(500),
    created_by BIGINT,
    
    FOREIGN KEY (buddy_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (new_employee_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_buddy (buddy_user_id),
    INDEX idx_employee (new_employee_id),
    INDEX idx_status (status),
    INDEX idx_matched_at (matched_at),
    INDEX idx_active_match (buddy_user_id, new_employee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Task Templates Table
```sql
CREATE TABLE task_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,                -- Parent onboarding plan
    plan_version INT NOT NULL,              -- Plan version this belongs to
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    day_offset INT NOT NULL,                -- Days after onboarding start (can be negative for pre-boarding)
    estimated_duration INT,                 -- In days
    
    -- Task Type and Ownership
    task_type ENUM('ADMINISTRATIVE', 'TECHNICAL', 'COMPLIANCE', 
                   'SOCIAL', 'PROJECT', 'REVIEW') NOT NULL,
    owner_type ENUM('MANAGER_OWNED', 'NEW_HIRE_OWNED', 'INTERNAL_TEAM_OWNED',
                    'INTERNAL_EMPLOYEE_OWNED', 'EXTERNAL_TEAM_OWNED', 
                    'SHARED_OWNERSHIP') NOT NULL,
    assignee_type ENUM('NEW_EMPLOYEE', 'BUDDY', 'MANAGER', 'HR_TEAM',
                       'IT_TEAM', 'TEAM_MEMBER', 'TEAM', 'EXTERNAL_VENDOR') NOT NULL,
    
    -- Execution Control
    execution_mode ENUM('SEQUENTIAL', 'PARALLEL') NOT NULL DEFAULT 'PARALLEL',
    sequence_order INT,                     -- Order for SEQUENTIAL tasks (1, 2, 3...)
    parallel_group VARCHAR(100),            -- Group ID for PARALLEL tasks
    
    -- Additional metadata
    category VARCHAR(100),                  -- Custom categorization
    tags JSON,                              -- Array of searchable tags
    depends_on_template_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (plan_id) REFERENCES onboarding_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (depends_on_template_id) REFERENCES task_templates(id) ON DELETE SET NULL,
    
    INDEX idx_plan (plan_id, plan_version),
    INDEX idx_active (is_active),
    INDEX idx_created_by (created_by),
    INDEX idx_task_type (task_type),
    INDEX idx_owner_type (owner_type),
    INDEX idx_assignee_type (assignee_type),
    INDEX idx_category (category),
    INDEX idx_execution_mode (execution_mode),
    INDEX idx_sequence_order (sequence_order),
    INDEX idx_parallel_group (parallel_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Template version history
CREATE TABLE task_template_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    version INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20),
    day_offset INT,
    estimated_duration INT,
    assignee_type VARCHAR(50),
    changed_by BIGINT NOT NULL,
    change_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (template_id) REFERENCES task_templates(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id),
    
    UNIQUE KEY unique_template_version (template_id, version),
    INDEX idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Onboarding Runs Table
```sql
CREATE TABLE onboarding_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    buddy_match_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,                -- Onboarding plan used
    plan_version INT NOT NULL,              -- Locked plan version
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    expected_end_date DATE,
    actual_end_date DATE,
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,
    completion_percentage DECIMAL(5,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (buddy_match_id) REFERENCES buddy_matches(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES onboarding_plans(id) ON DELETE RESTRICT,
    
    INDEX idx_match (buddy_match_id),
    INDEX idx_plan (plan_id, plan_version),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Tasks Table (Updated)
```sql
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    onboarding_run_id BIGINT NOT NULL,      -- Link to run instead of match
    template_id BIGINT,                     -- Reference to template
    template_version INT,                   -- Template version used
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status ENUM('PENDING', 'IN_PROGRESS', 'BLOCKED', 
                'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    assigned_to BIGINT NOT NULL,
    assigned_to_type ENUM('NEW_EMPLOYEE', 'BUDDY', 'MANAGER', 
                          'TEAM_MEMBER', 'TEAM'),
    created_by BIGINT NOT NULL,
    due_date DATE,
    completed_at TIMESTAMP NULL,
    
    -- Execution Control (copied from template)
    execution_mode ENUM('SEQUENTIAL', 'PARALLEL') NOT NULL DEFAULT 'PARALLEL',
    sequence_order INT,                     -- Order for SEQUENTIAL tasks
    parallel_group VARCHAR(100),            -- Group ID for PARALLEL tasks
    is_blocked BOOLEAN DEFAULT FALSE,       -- Blocked by previous sequential task
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (onboarding_run_id) REFERENCES onboarding_runs(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES task_templates(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    
    INDEX idx_run (onboarding_run_id),
    INDEX idx_template (template_id, template_version),
    INDEX idx_assigned (assigned_to),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    INDEX idx_active_tasks (assigned_to, status, due_date),
    INDEX idx_execution (onboarding_run_id, execution_mode, sequence_order),
    INDEX idx_parallel_group (onboarding_run_id, parallel_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Messages Table
```sql
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_conversation (sender_id, receiver_id, created_at DESC),
    INDEX idx_unread (receiver_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Feedback Table
```sql
CREATE TABLE feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    communication_rating INT CHECK (communication_rating BETWEEN 1 AND 5),
    knowledge_rating INT CHECK (knowledge_rating BETWEEN 1 AND 5),
    availability_rating INT CHECK (availability_rating BETWEEN 1 AND 5),
    supportiveness_rating INT CHECK (supportiveness_rating BETWEEN 1 AND 5),
    comments TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    period ENUM('WEEK_1', 'WEEK_2', 'MONTH_1', 'MONTH_2', 'FINAL'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (match_id) REFERENCES buddy_matches(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_match (match_id),
    INDEX idx_to_user (to_user_id),
    INDEX idx_created_at (created_at),
    UNIQUE KEY unique_feedback_period (match_id, from_user_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Notifications Table
```sql
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('TASK_ASSIGNED', 'TASK_DUE_SOON', 'TASK_OVERDUE', 
              'MESSAGE_RECEIVED', 'BUDDY_MATCHED', 'FEEDBACK_RECEIVED', 
              'SYSTEM_ANNOUNCEMENT') NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    data JSON,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user (user_id),
    INDEX idx_unread (user_id, is_read, created_at DESC),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.3 Supporting Tables

```sql
-- Task Dependencies
CREATE TABLE task_dependencies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    depends_on_task_id BIGINT NOT NULL,
    dependency_type ENUM('FINISH_TO_START', 'START_TO_START') 
        DEFAULT 'FINISH_TO_START',
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    
    UNIQUE KEY unique_dependency (task_id, depends_on_task_id)
) ENGINE=InnoDB;

-- Task History
CREATE TABLE task_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    comment TEXT,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id),
    
    INDEX idx_task (task_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB;

-- Notification Preferences
CREATE TABLE notification_preferences (
    user_id BIGINT PRIMARY KEY,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT FALSE,
    email_task_reminders BOOLEAN DEFAULT TRUE,
    email_messages BOOLEAN DEFAULT FALSE,
    email_feedback BOOLEAN DEFAULT TRUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    daily_digest BOOLEAN DEFAULT FALSE,
    digest_time TIME DEFAULT '09:00:00',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Password Reset Tokens
CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_token (token),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB;

-- Audit Logs
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    old_value JSON,
    new_value JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at),
    INDEX idx_action (action)
) ENGINE=InnoDB;
```

### 2.4 Indexing Strategy

**Primary Indexes (already defined):**
- All primary keys (id columns)
- Unique constraints (email, tokens)

**Performance Indexes:**
```sql
-- User lookups
CREATE INDEX idx_users_role_dept ON users(role, department);
CREATE INDEX idx_users_status_role ON users(status, role);

-- Active matches query
CREATE INDEX idx_active_matches ON buddy_matches(status, matched_at DESC);

-- Task dashboard
CREATE INDEX idx_task_dashboard ON tasks(assigned_to, status, due_date);

-- Conversation retrieval
CREATE INDEX idx_messages_conversation 
    ON messages(sender_id, receiver_id, created_at DESC);

-- Unread notifications
CREATE INDEX idx_unread_notifications 
    ON notifications(user_id, is_read, created_at DESC);

-- Feedback analytics
CREATE INDEX idx_feedback_analytics 
    ON feedback(to_user_id, created_at, rating);
```

---

## 3. API Specifications

### 3.1 Authentication APIs

#### POST /api/auth/register
**Description:** Register a new user account

**Request:**
```json
{
  "email": "john.doe@company.com",
  "password": "SecurePass123!",
  "name": "John Doe",
  "role": "NEW_EMPLOYEE",
  "department": "Engineering",
  "phone": "+1234567890",
  "skills": ["Java", "Python", "React"]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "email": "john.doe@company.com",
    "name": "John Doe",
    "role": "NEW_EMPLOYEE",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
  },
  "message": "User registered successfully"
}
```

**Validation Rules:**
- Email: Valid format, unique, max 255 chars
- Password: Min 8 chars, uppercase, lowercase, digit, special char
- Name: Required, max 255 chars
- Role: Must be valid enum value
- Phone: Optional, valid format

**Error Responses:**
```json
// 400 Bad Request
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "email",
        "message": "Email already exists"
      },
      {
        "field": "password",
        "message": "Password must contain at least one uppercase letter"
      }
    ]
  }
}

// 409 Conflict
{
  "success": false,
  "error": {
    "code": "USER_EXISTS",
    "message": "User with this email already exists"
  }
}
```

---

#### POST /api/auth/login
**Description:** Authenticate user and obtain tokens

**Request:**
```json
{
  "email": "john.doe@company.com",
  "password": "SecurePass123!",
  "authType": "JWT"  // "BASIC" or "JWT"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "email": "john.doe@company.com",
    "name": "John Doe",
    "role": "NEW_EMPLOYEE",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
  }
}
```

**Error Responses:**
```json
// 401 Unauthorized
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password"
  }
}

// 403 Forbidden
{
  "success": false,
  "error": {
    "code": "ACCOUNT_INACTIVE",
    "message": "Your account is inactive. Please contact administrator."
  }
}
```

---

#### POST /api/auth/oauth/google
**Description:** Authenticate using Google OAuth

**Request:**
```json
{
  "code": "4/0AX4XfWh...",
  "redirectUri": "http://localhost:8080/oauth/callback"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "email": "john.doe@gmail.com",
    "name": "John Doe",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isNewUser": false
  }
}
```

---

#### POST /api/auth/refresh-token
**Description:** Refresh expired JWT token

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
  }
}
```

---

#### POST /api/auth/logout
**Description:** Invalidate current session/token

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

#### POST /api/auth/forgot-password
**Description:** Request password reset

**Request:**
```json
{
  "email": "john.doe@company.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset link sent to your email"
}
```

---

### 3.2 User Management APIs

#### GET /api/users/{id}
**Description:** Get user profile by ID

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "email": "john.doe@company.com",
    "name": "John Doe",
    "role": "NEW_EMPLOYEE",
    "department": "Engineering",
    "skills": ["Java", "Python", "React"],
    "experienceLevel": "MID",
    "phone": "+1234567890",
    "avatarUrl": "https://cdn.example.com/avatars/123.jpg",
    "status": "ACTIVE",
    "createdAt": "2025-01-15T10:00:00Z",
    "lastLogin": "2025-01-20T09:30:00Z"
  }
}
```

**Error Responses:**
```json
// 404 Not Found
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User not found"
  }
}
```

---

#### PUT /api/users/{id}
**Description:** Update user profile

**Request:**
```json
{
  "name": "John Smith",
  "department": "Engineering",
  "skills": ["Java", "Python", "React", "Docker"],
  "phone": "+1234567890",
  "experienceLevel": "SENIOR"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": 123,
    "name": "John Smith",
    "updatedAt": "2025-01-20T10:00:00Z"
  }
}
```

**Authorization:** User can only update their own profile unless they have ADMIN role

---

#### GET /api/users
**Description:** List users with filters and pagination

**Query Parameters:**
- `role` - Filter by role (NEW_EMPLOYEE, BUDDY, ADMIN, HR_MANAGER)
- `department` - Filter by department
- `status` - Filter by status (ACTIVE, INACTIVE, PENDING)
- `search` - Search by name or email
- `page` - Page number (default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field (default: createdAt)
- `order` - Sort order (asc/desc, default: desc)

**Example:** `GET /api/users?role=BUDDY&department=Engineering&page=0&size=20`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 456,
        "name": "Jane Smith",
        "email": "jane.smith@company.com",
        "role": "BUDDY",
        "department": "Engineering",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

---

#### DELETE /api/users/{id}
**Description:** Deactivate user account (soft delete)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User deactivated successfully"
}
```

**Authorization:** ADMIN or HR_MANAGER only

---

### 3.3 Buddy Matching APIs

#### POST /api/buddies/match
**Description:** Create a buddy match

**Request:**
```json
{
  "newEmployeeId": 123,
  "buddyId": 456
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "matchId": 789,
    "buddyUserId": 456,
    "newEmployeeId": 123,
    "status": "PENDING",
    "matchScore": 0.85,
    "matchedAt": "2025-01-15T10:00:00Z"
  },
  "message": "Match created. Invitation sent to buddy."
}
```

**Authorization:** ADMIN or HR_MANAGER only

---

#### GET /api/buddies/suggestions
**Description:** Get buddy match suggestions for a new employee

**Query Parameters:**
- `employeeId` - New employee ID (required)
- `limit` - Number of suggestions (default: 5, max: 10)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "buddyId": 456,
      "name": "Jane Smith",
      "email": "jane.smith@company.com",
      "department": "Engineering",
      "matchScore": 0.92,
      "skills": ["Java", "Microservices", "AWS"],
      "experienceLevel": "SENIOR",
      "currentMentees": 2,
      "maxCapacity": 3,
      "averageRating": 4.8,
      "totalFeedbacks": 15,
      "matchReasons": [
        "High skill overlap (85%)",
        "Same department",
        "Excellent ratings",
        "Available capacity"
      ]
    }
  ]
}
```

---

#### GET /api/buddies/{id}
**Description:** Get buddy match details

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "matchId": 789,
    "status": "ACTIVE",
    "matchScore": 0.85,
    "matchedAt": "2025-01-15T10:00:00Z",
    "acceptedAt": "2025-01-15T14:30:00Z",
    "buddy": {
      "id": 456,
      "name": "Jane Smith",
      "email": "jane.smith@company.com",
      "department": "Engineering"
    },
    "newEmployee": {
      "id": 123,
      "name": "John Doe",
      "email": "john.doe@company.com",
      "department": "Engineering"
    },
    "statistics": {
      "tasksTotal": 10,
      "tasksCompleted": 5,
      "messagesExchanged": 45,
      "lastInteraction": "2025-01-20T14:30:00Z"
    }
  }
}
```

---

#### GET /api/buddies/my-matches
**Description:** Get current user's buddy matches

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "matchId": 789,
      "status": "ACTIVE",
      "role": "BUDDY",
      "partner": {
        "id": 123,
        "name": "John Doe",
        "email": "john.doe@company.com"
      },
      "matchedAt": "2025-01-15T10:00:00Z",
      "progress": {
        "tasksCompleted": 5,
        "tasksTotal": 10,
        "completionPercentage": 50
      }
    }
  ]
}
```

---

#### PUT /api/buddies/{id}/unmatch
**Description:** End a buddy match

**Request:**
```json
{
  "reason": "BUDDY_UNAVAILABLE",
  "comments": "Buddy is going on extended leave"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Match ended successfully"
}
```

---

### 3.4 Onboarding Plan Management APIs

#### POST /api/plans
**Description:** Create a new onboarding plan (Manager/SPOC only)

**Request:**
```json
{
  "name": "Engineering Onboarding 90-Day",
  "description": "Complete onboarding plan for engineering new hires",
  "department": "Engineering",
  "durationDays": 90
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "planId": 501,
    "name": "Engineering Onboarding 90-Day",
    "version": 1,
    "department": "Engineering",
    "durationDays": 90,
    "isActive": false,
    "createdBy": 456,
    "createdAt": "2025-01-15T10:00:00Z",
    "message": "Plan created. Add task templates and then publish."
  }
}
```

**Authorization:** MANAGER, HR_MANAGER, or ADMIN role required

---

#### POST /api/plans/{planId}/templates
**Description:** Add task template to a plan

**Request:**
```json
{
  "name": "Complete IT Setup",
  "description": "Set up laptop, accounts, and access",
  "priority": "HIGH",
  "dayOffset": 0,
  "estimatedDuration": 1,
  "taskType": "ADMINISTRATIVE",
  "ownerType": "INTERNAL_TEAM_OWNED",
  "assigneeType": "IT_TEAM",
  "executionMode": "SEQUENTIAL",
  "sequenceOrder": 1,
  "parallelGroup": null,
  "category": "IT Setup",
  "tags": ["it", "setup", "hardware"]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "templateId": 1001,
    "planId": 501,
    "planVersion": 1,
    "name": "Complete IT Setup",
    "createdAt": "2025-01-15T10:05:00Z"
  }
}
```

---

#### PUT /api/plans/{planId}/publish
**Description:** Publish/activate a plan for use

**Request:**
```json
{
  "publishNotes": "Initial version ready for Engineering department"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "planId": 501,
    "version": 1,
    "isActive": true,
    "publishedAt": "2025-01-15T11:00:00Z",
    "totalTemplates": 12,
    "message": "Plan published and available for onboarding runs"
  }
}
```

---

#### PUT /api/plans/{planId}
**Description:** Update plan (creates new version)

**Request:**
```json
{
  "name": "Engineering Onboarding 90-Day (Updated)",
  "description": "Updated with new compliance requirements",
  "durationDays": 90,
  "changeReason": "Added new security training requirements"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "planId": 501,
    "version": 2,
    "previousVersion": 1,
    "isActive": false,
    "message": "Plan updated to version 2. All templates copied. Review and publish when ready."
  }
}
```

**Note:** When plan is updated:
- New version created
- All templates from previous version are copied
- Templates can be modified for new version
- Old version remains unchanged for in-progress runs

---

#### GET /api/plans
**Description:** List onboarding plans

**Query Parameters:**
- `department` - Filter by department
- `isActive` - Filter by active status
- `createdBy` - Filter by creator
- `search` - Search in name/description

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 501,
      "name": "Engineering Onboarding 90-Day",
      "version": 2,
      "department": "Engineering",
      "durationDays": 90,
      "isActive": true,
      "templateCount": 12,
      "usageCount": 25,
      "createdBy": {
        "id": 456,
        "name": "Jane Smith"
      },
      "publishedAt": "2025-01-15T11:00:00Z"
    }
  ]
}
```

---

#### GET /api/plans/{planId}
**Description:** Get plan details with all templates

**Query Parameters:**
- `version` - Get specific version (default: latest)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "planId": 501,
    "name": "Engineering Onboarding 90-Day",
    "version": 2,
    "department": "Engineering",
    "durationDays": 90,
    "isActive": true,
    "templates": [
      {
        "id": 1001,
        "name": "Complete IT Setup",
        "taskType": "ADMINISTRATIVE",
        "ownerType": "INTERNAL_TEAM_OWNED",
        "assigneeType": "IT_TEAM",
        "executionMode": "SEQUENTIAL",
        "sequenceOrder": 1,
        "dayOffset": 0
      },
      {
        "id": 1002,
        "name": "Security Training",
        "taskType": "COMPLIANCE",
        "ownerType": "NEW_HIRE_OWNED",
        "assigneeType": "NEW_EMPLOYEE",
        "executionMode": "PARALLEL",
        "parallelGroup": "day3-training",
        "dayOffset": 3
      }
    ],
    "createdBy": "Jane Smith",
    "publishedAt": "2025-01-15T11:00:00Z"
  }
}
```

---

#### GET /api/plans/{planId}/versions
**Description:** Get version history of a plan

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "planId": 501,
    "currentVersion": 2,
    "versions": [
      {
        "version": 2,
        "changedBy": "Jane Smith",
        "changeReason": "Added new security training requirements",
        "templateCount": 13,
        "createdAt": "2025-01-20T10:00:00Z"
      },
      {
        "version": 1,
        "changedBy": "Jane Smith",
        "changeReason": "Initial creation",
        "templateCount": 12,
        "createdAt": "2025-01-15T10:00:00Z"
      }
    ]
  }
}
```

---

#### DELETE /api/plans/{planId}
**Description:** Deactivate plan (soft delete)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Plan deactivated. Existing runs will not be affected."
}
```

---

### 3.5 Task Template Management APIs

#### POST /api/templates/tasks
**Description:** Create a new task template (Manager/SPOC only) - DEPRECATED
**Note:** Use `POST /api/plans/{planId}/templates` instead

**Request:**
```json
{
  "name": "Complete Security Training",
  "description": "Complete all mandatory security training modules",
  "priority": "HIGH",
  "dayOffset": 3,
  "estimatedDuration": 2,
  "taskType": "COMPLIANCE",
  "ownerType": "NEW_HIRE_OWNED",
  "assigneeType": "NEW_EMPLOYEE",
  "executionMode": "PARALLEL",
  "sequenceOrder": null,
  "parallelGroup": "day3-training",
  "department": "Engineering",
  "category": "Compliance",
  "tags": ["compliance", "security", "mandatory"],
  "dependsOnTemplateId": null
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "templateId": 101,
    "name": "Complete Security Training",
    "version": 1,
    "taskType": "COMPLIANCE",
    "ownerType": "NEW_HIRE_OWNED",
    "assigneeType": "NEW_EMPLOYEE",
    "createdBy": 456,
    "isActive": true,
    "createdAt": "2025-01-15T10:00:00Z"
  },
  "message": "Task template created successfully"
}
```

**Validation Rules:**
- `name` - Required, max 255 characters
- `taskType` - Required, must be valid enum value
- `ownerType` - Required, must be valid enum value
- `assigneeType` - Required, must be valid enum value
- `dayOffset` - Can be negative for pre-boarding tasks
- `department` - Optional, null for org-wide templates
- `tags` - Optional array of strings

**Authorization:** MANAGER, HR_MANAGER, or ADMIN role required

---

#### PUT /api/templates/tasks/{id}
**Description:** Update task template (creates new version)

**Request:**
```json
{
  "name": "Complete Security Training (Updated)",
  "description": "Updated description",
  "priority": "URGENT",
  "dayOffset": 2,
  "changeReason": "Updated to reflect new compliance requirements"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "templateId": 101,
    "version": 2,
    "previousVersion": 1,
    "message": "Template updated. New version will apply to future onboarding runs only."
  }
}
```

---

#### GET /api/templates/tasks
**Description:** List task templates with filters

**Query Parameters:**
- `department` - Filter by department (or "ALL" for org-wide)
- `taskType` - Filter by task type (ADMINISTRATIVE, TECHNICAL, COMPLIANCE, etc.)
- `ownerType` - Filter by owner type (MANAGER_OWNED, NEW_HIRE_OWNED, etc.)
- `assigneeType` - Filter by assignee type (NEW_EMPLOYEE, BUDDY, MANAGER, etc.)
- `category` - Filter by category
- `tags` - Filter by tags (comma-separated)
- `isActive` - Filter by active status
- `createdBy` - Filter by creator
- `version` - Get specific version (default: latest)
- `search` - Search in name and description
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)

**Example:** `GET /api/templates/tasks?taskType=COMPLIANCE&ownerType=NEW_HIRE_OWNED&department=Engineering`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "name": "Complete Security Training",
        "version": 2,
        "department": "Engineering",
        "priority": "URGENT",
        "dayOffset": 2,
        "estimatedDuration": 2,
        "taskType": "COMPLIANCE",
        "ownerType": "NEW_HIRE_OWNED",
        "assigneeType": "NEW_EMPLOYEE",
        "category": "Compliance",
        "tags": ["compliance", "security", "mandatory"],
        "isActive": true,
        "createdBy": {
          "id": 456,
          "name": "Jane Smith"
        },
        "usageCount": 15,
        "createdAt": "2025-01-15T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

#### GET /api/templates/tasks/grouped
**Description:** Get templates grouped by owner type

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "MANAGER_OWNED": [
      {
        "id": 102,
        "name": "30-Day Performance Review",
        "taskType": "REVIEW",
        "assigneeType": "MANAGER",
        "count": 5
      }
    ],
    "NEW_HIRE_OWNED": [
      {
        "id": 101,
        "name": "Complete Security Training",
        "taskType": "COMPLIANCE",
        "assigneeType": "NEW_EMPLOYEE",
        "count": 15
      }
    ],
    "INTERNAL_TEAM_OWNED": [
      {
        "id": 103,
        "name": "IT Setup",
        "taskType": "ADMINISTRATIVE",
        "assigneeType": "IT_TEAM",
        "count": 20
      }
    ],
    "INTERNAL_EMPLOYEE_OWNED": [],
    "EXTERNAL_TEAM_OWNED": [
      {
        "id": 104,
        "name": "Background Verification",
        "taskType": "ADMINISTRATIVE",
        "assigneeType": "EXTERNAL_VENDOR",
        "count": 10
      }
    ],
    "SHARED_OWNERSHIP": []
  }
}
```

---

#### GET /api/templates/tasks/{id}/versions
**Description:** Get version history of a template

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "templateId": 101,
    "currentVersion": 2,
    "versions": [
      {
        "version": 2,
        "changedBy": "Jane Smith",
        "changeReason": "Updated to reflect new compliance requirements",
        "changes": {
          "priority": {"old": "HIGH", "new": "URGENT"},
          "dayOffset": {"old": 3, "new": 2}
        },
        "createdAt": "2025-01-20T10:00:00Z"
      },
      {
        "version": 1,
        "changedBy": "Jane Smith",
        "changeReason": "Initial creation",
        "createdAt": "2025-01-15T10:00:00Z"
      }
    ]
  }
}
```

---

#### DELETE /api/templates/tasks/{id}
**Description:** Deactivate template (soft delete)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Template deactivated. Existing runs will not be affected."
}
```

---

### 3.5 Onboarding Run Management APIs

#### POST /api/onboarding-runs
**Description:** Create onboarding run from templates (auto-created on match activation)

**Request:**
```json
{
  "buddyMatchId": 789,
  "templateSetId": null,
  "startDate": "2025-01-15"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "runId": 5001,
    "buddyMatchId": 789,
    "templateVersion": 2,
    "status": "ACTIVE",
    "startDate": "2025-01-15",
    "expectedEndDate": "2025-04-15",
    "totalTasks": 9,
    "completedTasks": 0,
    "completionPercentage": 0.0,
    "tasksCreated": [
      {
        "taskId": 10001,
        "title": "Complete IT Setup",
        "assignedTo": "John Doe",
        "assignedToType": "NEW_EMPLOYEE",
        "dueDate": "2025-01-15"
      }
    ]
  }
}
```

---

#### GET /api/onboarding-runs/{id}
**Description:** Get onboarding run details

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "runId": 5001,
    "buddyMatchId": 789,
    "templateVersion": 2,
    "status": "ACTIVE",
    "startDate": "2025-01-15",
    "expectedEndDate": "2025-04-15",
    "actualEndDate": null,
    "totalTasks": 9,
    "completedTasks": 3,
    "completionPercentage": 33.33,
    "tasks": [
      {
        "id": 10001,
        "title": "Complete IT Setup",
        "status": "COMPLETED",
        "assignedTo": "John Doe",
        "assignedToType": "NEW_EMPLOYEE",
        "templateId": 101,
        "templateVersion": 2
      }
    ],
    "createdAt": "2025-01-15T10:00:00Z"
  }
}
```

---

#### GET /api/onboarding-runs/match/{matchId}
**Description:** Get onboarding run for a specific match

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "runId": 5001,
    "status": "ACTIVE",
    "progress": {
      "totalTasks": 9,
      "completedTasks": 3,
      "completionPercentage": 33.33,
      "tasksOverdue": 0,
      "tasksInProgress": 2,
      "tasksPending": 4,
      "tasksBlocked": 2
    },
    "executionBreakdown": {
      "sequential": {
        "total": 3,
        "completed": 1,
        "blocked": 2,
        "currentSequence": 2
      },
      "parallel": {
        "total": 6,
        "completed": 2,
        "groups": [
          {
            "groupId": "day3-training",
            "total": 3,
            "completed": 2,
            "progress": 66.67
          },
          {
            "groupId": "orientation",
            "total": 3,
            "completed": 0,
            "progress": 0
          }
        ]
      }
    }
  }
}
```

---

#### GET /api/tasks/execution-status
**Description:** Get detailed execution status for tasks in a run

**Query Parameters:**
- `runId` - Onboarding run ID (required)
- `executionMode` - Filter by SEQUENTIAL or PARALLEL

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "runId": 5001,
    "sequentialTasks": [
      {
        "taskId": 10001,
        "title": "Complete IT Setup",
        "sequenceOrder": 1,
        "status": "COMPLETED",
        "isBlocked": false,
        "completedAt": "2025-01-15T14:30:00Z"
      },
      {
        "taskId": 10002,
        "title": "Access Badge Creation",
        "sequenceOrder": 2,
        "status": "IN_PROGRESS",
        "isBlocked": false,
        "canStart": true
      },
      {
        "taskId": 10003,
        "title": "System Access Provisioning",
        "sequenceOrder": 3,
        "status": "BLOCKED",
        "isBlocked": true,
        "canStart": false,
        "blockedBy": "Access Badge Creation"
      }
    ],
    "parallelGroups": {
      "day3-training": [
        {
          "taskId": 10004,
          "title": "Security Training",
          "status": "COMPLETED",
          "isBlocked": false,
          "canStart": true
        },
        {
          "taskId": 10005,
          "title": "Compliance Training",
          "status": "COMPLETED",
          "isBlocked": false,
          "canStart": true
        },
        {
          "taskId": 10006,
          "title": "Tool Training",
          "status": "PENDING",
          "isBlocked": false,
          "canStart": true
        }
      ]
    }
  }
}
```

---

### 3.6 Task Management APIs

#### POST /api/tasks
**Description:** Create a new task (ad-hoc, not from template)

**Request:**
```json
{
  "onboardingRunId": 5001,
  "title": "Complete Security Training",
  "description": "Complete all mandatory security training modules in the learning portal",
  "priority": "HIGH",
  "dueDate": "2025-01-25",
  "assignedTo": 123,
  "assignedToType": "NEW_EMPLOYEE"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "taskId": 1001,
    "title": "Complete Security Training",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2025-01-25",
    "assignedTo": 123,
    "createdBy": 456,
    "createdAt": "2025-01-15T10:00:00Z"
  }
}
```

---

#### GET /api/tasks/{id}
**Description:** Get task details

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "buddyMatchId": 789,
    "title": "Complete Security Training",
    "description": "Complete all mandatory security training modules",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "dueDate": "2025-01-25",
    "daysUntilDue": 10,
    "assignedTo": {
      "id": 123,
      "name": "John Doe"
    },
    "createdBy": {
      "id": 456,
      "name": "Jane Smith"
    },
    "createdAt": "2025-01-15T10:00:00Z",
    "updatedAt": "2025-01-16T09:00:00Z",
    "dependencies": [],
    "history": [
      {
        "oldStatus": "PENDING",
        "newStatus": "IN_PROGRESS",
        "changedBy": "John Doe",
        "changedAt": "2025-01-16T09:00:00Z",
        "comment": "Started working on this"
      }
    ]
  }
}
```

---

#### PUT /api/tasks/{id}
**Description:** Update task details

**Request:**
```json
{
  "title": "Complete Security Training (Updated)",
  "description": "Updated description",
  "priority": "URGENT",
  "dueDate": "2025-01-23"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Task updated successfully"
}
```

---

#### PUT /api/tasks/{id}/status
**Description:** Update task status

**Request:**
```json
{
  "status": "IN_PROGRESS",
  "comment": "Started working on this task"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Task status updated",
  "data": {
    "taskId": 1001,
    "oldStatus": "PENDING",
    "newStatus": "IN_PROGRESS",
    "updatedAt": "2025-01-16T09:00:00Z"
  }
}
```

**Validation:**
- Status transition must be valid (see state machine)
- User must be assigned to task or have ADMIN role

---

#### DELETE /api/tasks/{id}
**Description:** Delete task (or mark as CANCELLED)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Task cancelled successfully"
}
```

---

#### GET /api/tasks
**Description:** List tasks with filters

**Query Parameters:**
- `assignedTo` - Filter by assigned user ID
- `createdBy` - Filter by creator user ID
- `buddyMatchId` - Filter by buddy match
- `status` - Filter by status (PENDING, IN_PROGRESS, etc.)
- `priority` - Filter by priority
- `dueDateFrom` - Filter by due date range start
- `dueDateTo` - Filter by due date range end
- `overdue` - Show only overdue tasks (true/false)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sort` - Sort field (default: dueDate)

**Example:** `GET /api/tasks?assignedTo=123&status=PENDING&overdue=false&page=0&size=20`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1001,
        "title": "Complete Security Training",
        "status": "PENDING",
        "priority": "HIGH",
        "dueDate": "2025-01-25",
        "daysUntilDue": 10,
        "assignedTo": {
          "id": 123,
          "name": "John Doe"
        },
        "createdBy": {
          "id": 456,
          "name": "Jane Smith"
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 8,
    "totalPages": 1
  }
}
```

---

### 3.5 Communication APIs

#### POST /api/messages
**Description:** Send a message

**Request:**
```json
{
  "receiverId": 456,
  "content": "Hi! I have a question about the onboarding process."
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "messageId": 5001,
    "senderId": 123,
    "receiverId": 456,
    "content": "Hi! I have a question about the onboarding process.",
    "createdAt": "2025-01-15T10:00:00Z",
    "isRead": false
  }
}
```

**Validation:**
- Sender and receiver must be in a buddy relationship
- Content max length: 5000 characters
- Content is sanitized for XSS

---

#### GET /api/messages
**Description:** Get messages (conversation)

**Query Parameters:**
- `conversationWith` - User ID to get conversation with (required)
- `page` - Page number (default: 0)
- `size` - Page size (default: 50, max: 100)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5001,
        "senderId": 123,
        "senderName": "John Doe",
        "receiverId": 456,
        "receiverName": "Jane Smith",
        "content": "Hi! I have a question...",
        "createdAt": "2025-01-15T10:00:00Z",
        "isRead": true,
        "readAt": "2025-01-15T10:05:00Z"
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 25,
    "totalPages": 1
  }
}
```

---

#### PUT /api/messages/{id}/read
**Description:** Mark message as read

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Message marked as read"
}
```

---

#### GET /api/notifications
**Description:** Get user notifications

**Query Parameters:**
- `unreadOnly` - Show only unread (true/false, default: false)
- `type` - Filter by notification type
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 3001,
        "type": "TASK_ASSIGNED",
        "title": "New Task Assigned",
        "content": "You have been assigned: Complete Security Training",
        "isRead": false,
        "createdAt": "2025-01-15T10:00:00Z",
        "data": {
          "taskId": 1001,
          "taskTitle": "Complete Security Training"
        }
      }
    ],
    "unreadCount": 5,
    "page": 0,
    "totalElements": 15
  }
}
```

---

#### PUT /api/notifications/{id}/read
**Description:** Mark notification as read

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Notification marked as read"
}
```

---

#### PUT /api/notifications/read-all
**Description:** Mark all notifications as read

**Response (200 OK):**
```json
{
  "success": true,
  "message": "All notifications marked as read"
}
```

---

### 3.6 Feedback APIs

#### POST /api/feedback
**Description:** Submit feedback

**Request:**
```json
{
  "toUserId": 456,
  "rating": 5,
  "communicationRating": 5,
  "knowledgeRating": 4,
  "availabilityRating": 5,
  "supportivenessRating": 5,
  "comments": "Excellent buddy! Very helpful and responsive.",
  "isAnonymous": false,
  "period": "WEEK_1"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Feedback submitted successfully",
  "data": {
    "feedbackId": 2001
  }
}
```

**Validation:**
- All ratings must be 1-5
- Can only submit feedback for active buddy relationship
- Cannot submit duplicate feedback for same period

---

#### GET /api/feedback/{id}
**Description:** Get feedback details

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 2001,
    "matchId": 789,
    "fromUser": {
      "id": 123,
      "name": "John Doe"
    },
    "toUser": {
      "id": 456,
      "name": "Jane Smith"
    },
    "rating": 5,
    "communicationRating": 5,
    "knowledgeRating": 4,
    "availabilityRating": 5,
    "supportivenessRating": 5,
    "comments": "Excellent buddy!",
    "isAnonymous": false,
    "period": "WEEK_1",
    "createdAt": "2025-01-22T10:00:00Z"
  }
}
```

---

#### GET /api/feedback/user/{userId}
**Description:** Get feedback summary for a user

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": 456,
    "userName": "Jane Smith",
    "totalFeedbacks": 15,
    "averageRating": 4.7,
    "averageCommunication": 4.8,
    "averageKnowledge": 4.6,
    "averageAvailability": 4.7,
    "averageSupportiveness": 4.8,
    "ratingDistribution": {
      "5": 12,
      "4": 2,
      "3": 1,
      "2": 0,
      "1": 0
    },
    "recentFeedbacks": [
      {
        "id": 2001,
        "rating": 5,
        "comments": "Excellent buddy!",
        "period": "WEEK_1",
        "createdAt": "2025-01-22T10:00:00Z",
        "fromUser": "John Doe"
      }
    ]
  }
}
```

---

#### GET /api/analytics/feedback-summary
**Description:** Get overall feedback analytics

**Query Parameters:**
- `startDate` - Start date (YYYY-MM-DD)
- `endDate` - End date (YYYY-MM-DD)
- `department` - Filter by department

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "totalFeedbacks": 150,
    "averageRating": 4.5,
    "feedbackSubmissionRate": 85.5,
    "topBuddies": [
      {
        "userId": 456,
        "name": "Jane Smith",
        "averageRating": 4.9,
        "totalFeedbacks": 20
      }
    ],
    "ratingTrend": [
      {
        "month": "2025-01",
        "averageRating": 4.5
      }
    ]
  }
}
```

---

### 3.7 Admin APIs

#### GET /api/admin/users
**Description:** Admin user management (same as GET /api/users but with more details)

**Authorization:** ADMIN or HR_MANAGER only

---

#### POST /api/admin/users
**Description:** Create user (admin function)

**Request:**
```json
{
  "email": "new.user@company.com",
  "name": "New User",
  "role": "BUDDY",
  "department": "Engineering",
  "status": "ACTIVE"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "userId": 789,
    "temporaryPassword": "TempPass123!"
  },
  "message": "User created. Temporary password sent via email."
}
```

---

#### GET /api/admin/reports/onboarding-summary
**Description:** Get onboarding summary report

**Query Parameters:**
- `startDate` - Start date (YYYY-MM-DD)
- `endDate` - End date (YYYY-MM-DD)
- `department` - Filter by department
- `format` - Response format (json/pdf/csv)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "period": {
      "startDate": "2025-01-01",
      "endDate": "2025-01-31"
    },
    "metrics": {
      "totalOnboardings": 45,
      "completedOnboardings": 38,
      "completionRate": 84.4,
      "averageTimeToProductivity": 62.5,
      "averageBuddyRating": 4.6,
      "taskCompletionRate": 92.3,
      "engagementScore": 78.5,
      "retentionRate": 95.5
    },
    "departmentBreakdown": [
      {
        "department": "Engineering",
        "totalOnboardings": 20,
        "completionRate": 90.0
      }
    ]
  }
}
```

---

#### GET /api/admin/reports/buddy-performance
**Description:** Get buddy performance report

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "topPerformers": [
      {
        "buddyId": 456,
        "name": "Jane Smith",
        "department": "Engineering",
        "totalMentees": 8,
        "completedOnboardings": 7,
        "averageRating": 4.8,
        "averageCompletionTime": 58.5
      }
    ],
    "needsImprovement": []
  }
}
```

---

#### GET /api/admin/audit-logs
**Description:** Get system audit logs

**Query Parameters:**
- `userId` - Filter by user
- `action` - Filter by action type
- `startDate` - Start date
- `endDate` - End date
- `page` - Page number
- `size` - Page size

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10001,
        "userId": 123,
        "userName": "John Doe",
        "action": "USER_LOGIN",
        "entityType": "USER",
        "entityId": 123,
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "createdAt": "2025-01-20T09:30:00Z"
      }
    ],
    "page": 0,
    "totalElements": 1000
  }
}
```

---

### 3.8 Common Response Patterns

#### Success Response Structure
```json
{
  "success": true,
  "data": { ... },
  "message": "Optional success message"
}
```

#### Error Response Structure
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": [ ... ]  // Optional array of detailed errors
  }
}
```

#### HTTP Status Codes
- `200 OK` - Successful GET, PUT, DELETE
- `201 Created` - Successful POST (resource created)
- `204 No Content` - Successful DELETE (no response body)
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate email)
- `422 Unprocessable Entity` - Business logic validation error
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

---

*Document continues in next section...*

**Status:** Sections 1-3 completed (Component Design, Database Design, API Specifications).
**Next:** Section 4 (Class/Service Design) - Ready to add when requested.

---

## 4. Class/Service Design

### 4.1 Servlet Architecture

**Base Servlet Pattern:**
- All servlets extend `HttpServlet`
- Use `@WebServlet` annotation for URL mapping
- Implement `doGet()`, `doPost()`, `doPut()`, `doDelete()` as needed
- Delegate business logic to service layer
- Use utility classes for JSON parsing and response formatting

**Key Servlets:**
- `AuthServlet` - `/api/auth/*` - Authentication endpoints
- `UserServlet` - `/api/users/*` - User management
- `BuddyServlet` - `/api/buddies/*` - Buddy matching
- `TaskServlet` - `/api/tasks/*` - Task management
- `MessageServlet` - `/api/messages/*` - Messaging
- `FeedbackServlet` - `/api/feedback/*` - Feedback system
- `AdminServlet` - `/api/admin/*` - Admin operations

### 4.2 Service Layer

#### Core Services

**AuthenticationService**
- `register(RegisterRequest)` - Register new user
- `authenticate(email, password, authType)` - Login user
- `authenticateWithGoogle(code, redirectUri)` - OAuth login
- `refreshToken(refreshToken)` - Refresh JWT token
- `invalidateToken(token)` - Logout/blacklist token
- `initiatePasswordReset(email)` - Password reset flow

**UserService**
- `getUserById(userId)` - Get user profile
- `updateUser(userId, request)` - Update profile
- `listUsers(filters, page, size)` - List with pagination
- `deactivateUser(userId)` - Soft delete user

**BuddyMatchingService**
- `getSuggestions(employeeId, limit)` - Get match suggestions
- `createMatch(employeeId, buddyId)` - Create match
- `getMatchById(matchId)` - Get match details
- `getMatchesForUser(userId)` - Get user's matches
- `endMatch(matchId, reason, comments)` - End match
- `calculateMatchScore(employee, buddy)` - Matching algorithm

**TaskService**
- `createTask(request)` - Create new task
- `getTaskById(taskId)` - Get task details
- `updateTask(taskId, request)` - Update task
- `updateTaskStatus(taskId, status, comment)` - Change status
- `listTasks(filters, page, size)` - List with filters
- `cancelTask(taskId)` - Cancel task
- `createOnboardingTasks(matchId)` - Create default tasks

**MessageService**
- `sendMessage(senderId, receiverId, content)` - Send message
- `getConversation(user1Id, user2Id, page, size)` - Get messages
- `markAsRead(messageId, userId)` - Mark read

**FeedbackService**
- `submitFeedback(request)` - Submit feedback
- `getFeedbackById(feedbackId)` - Get feedback
- `getFeedbackSummary(userId)` - Get user summary
- `getAnalytics(startDate, endDate, department)` - Analytics

**NotificationService**
- `sendNotification(userId, type, title, content, data)` - Send notification
- `getNotifications(userId, filters, page, size)` - Get notifications
- `markAsRead(notificationId)` - Mark read
- `markAllAsRead(userId)` - Mark all read

### 4.3 Repository Layer

**Repository Pattern:**
- Data access abstraction
- JDBC-based implementation
- Connection pooling via HikariCP
- Prepared statements for SQL injection prevention

**Key Repositories:**
- `UserRepository` - User CRUD operations
- `BuddyMatchRepository` - Match operations
- `TaskRepository` - Task operations
- `MessageRepository` - Message operations
- `FeedbackRepository` - Feedback operations
- `NotificationRepository` - Notification operations

### 4.4 Utility Classes

**JsonUtil**
- `toJson(object)` - Serialize to JSON
- `fromJson(json, class)` - Deserialize from JSON
- Uses Gson or Jackson library

**JwtUtil**
- `generateToken(user)` - Create JWT token
- `generateRefreshToken(user)` - Create refresh token
- `validateToken(token)` - Validate token
- `getUserIdFromToken(token)` - Extract user ID
- Uses JJWT library

**PasswordUtil**
- `hashPassword(password)` - BCrypt hashing
- `verifyPassword(password, hash)` - Verify password
- Cost factor: 12

**ServletUtil**
- `sendSuccess(response, status, data, message)` - Success response
- `sendError(response, status, message)` - Error response
- `extractToken(request)` - Extract JWT from header
- `extractId(pathInfo)` - Extract ID from URL
- `getIntParameter(request, name, default)` - Parse int param

**SecurityContext**
- `getCurrentUserId()` - Get current user ID
- `getCurrentUser()` - Get current user
- `hasRole(role)` - Check role
- `hasAnyRole(roles...)` - Check multiple roles

### 4.5 Design Patterns

**Patterns Used:**
1. **Singleton** - Service instances
2. **Repository Pattern** - Data access layer
3. **Factory Pattern** - Object creation
4. **Strategy Pattern** - Matching algorithm
5. **Observer Pattern** - Notification system
6. **Builder Pattern** - Complex object construction
7. **Dependency Injection** - Service dependencies

### 4.6 Exception Hierarchy

```
RuntimeException
├── ApplicationException (base)
│   ├── AuthenticationException (401)
│   ├── AuthorizationException (403)
│   ├── NotFoundException (404)
│   ├── ValidationException (400)
│   ├── ConflictException (409)
│   └── BusinessLogicException (422)
```

---

## 5. State Machines & Workflows

### 5.1 Buddy Match State Machine

#### States
```
PENDING → SUGGESTED → ACCEPTED → ACTIVE → COMPLETED
                                    ↓
                                  ENDED
```

#### State Definitions

| State | Description | Entry Actions | Exit Actions |
|-------|-------------|---------------|--------------|
| **PENDING** | New employee registered, awaiting match | Create user profile | - |
| **SUGGESTED** | System generated suggestions | Run matching algorithm, notify HR | - |
| **ACCEPTED** | Buddy accepted invitation | Send welcome notification, create tasks | - |
| **ACTIVE** | Onboarding in progress | Start tracking progress | - |
| **COMPLETED** | Onboarding successfully finished | Send completion notification, request final feedback | Archive match |
| **ENDED** | Match terminated early | Log reason, notify stakeholders | Clean up pending tasks |

#### Valid Transitions

```java
public enum MatchStatus {
    PENDING, SUGGESTED, ACCEPTED, ACTIVE, COMPLETED, ENDED
}

private static final Map<MatchStatus, Set<MatchStatus>> VALID_TRANSITIONS = Map.of(
    PENDING, Set.of(SUGGESTED, ENDED),
    SUGGESTED, Set.of(ACCEPTED, ENDED),
    ACCEPTED, Set.of(ACTIVE, ENDED),
    ACTIVE, Set.of(COMPLETED, ENDED),
    COMPLETED, Set.of(),  // Terminal state
    ENDED, Set.of()       // Terminal state
);

public void transitionState(BuddyMatch match, MatchStatus newStatus, String reason) {
    MatchStatus currentStatus = match.getStatus();
    
    if (!VALID_TRANSITIONS.get(currentStatus).contains(newStatus)) {
        throw new InvalidStateTransitionException(
            String.format("Cannot transition from %s to %s", currentStatus, newStatus)
        );
    }
    
    // Execute transition
    match.setStatus(newStatus);
    
    // Trigger side effects
    switch (newStatus) {
        case ACCEPTED:
            onMatchAccepted(match);
            break;
        case ACTIVE:
            onMatchActivated(match);
            break;
        case COMPLETED:
            onMatchCompleted(match);
            break;
        case ENDED:
            onMatchEnded(match, reason);
            break;
    }
    
    matchRepository.update(match);
}

private void onMatchAccepted(BuddyMatch match) {
    match.setAcceptedAt(LocalDateTime.now());
    
    // Send notifications
    notificationService.sendNotification(
        match.getNewEmployeeId(),
        NotificationType.BUDDY_MATCHED,
        "Buddy Assigned!",
        "Your buddy has accepted the match",
        Map.of("matchId", match.getId())
    );
    
    // Transition to ACTIVE immediately
    transitionState(match, MatchStatus.ACTIVE, null);
}

private void onMatchActivated(BuddyMatch match) {
    // Create default onboarding tasks
    taskService.createOnboardingTasks(match);
    
    // Send welcome email
    emailService.sendOnboardingWelcomeEmail(match);
}

private void onMatchCompleted(BuddyMatch match) {
    match.setCompletedAt(LocalDateTime.now());
    
    // Request final feedback
    notificationService.sendFeedbackRequest(match, FeedbackPeriod.FINAL);
    
    // Send completion certificates
    emailService.sendCompletionCertificate(match);
}

private void onMatchEnded(BuddyMatch match, String reason) {
    match.setEndedAt(LocalDateTime.now());
    match.setEndReason(reason);
    
    // Cancel pending tasks
    taskService.cancelPendingTasks(match.getId());
    
    // Notify stakeholders
    notificationService.notifyMatchEnded(match, reason);
}
```

---

### 5.2 Task State Machine

#### States
```
PENDING → IN_PROGRESS → COMPLETED
            ↓
          BLOCKED → IN_PROGRESS
            ↓
        CANCELLED
```

#### State Definitions

| State | Description | Can Transition To |
|-------|-------------|-------------------|
| **PENDING** | Task created, not started | IN_PROGRESS, CANCELLED |
| **IN_PROGRESS** | Work in progress | BLOCKED, COMPLETED, CANCELLED |
| **BLOCKED** | Waiting on dependency or issue | IN_PROGRESS, CANCELLED |
| **COMPLETED** | Successfully finished | (terminal) |
| **CANCELLED** | No longer needed | (terminal) |

#### State Transition Logic

```java
public class TaskStateMachine {
    
    private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS = Map.of(
        TaskStatus.PENDING, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
        TaskStatus.IN_PROGRESS, Set.of(TaskStatus.BLOCKED, TaskStatus.COMPLETED, TaskStatus.CANCELLED),
        TaskStatus.BLOCKED, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
        TaskStatus.COMPLETED, Set.of(),
        TaskStatus.CANCELLED, Set.of()
    );
    
    public void updateStatus(Task task, TaskStatus newStatus, String comment) {
        TaskStatus oldStatus = task.getStatus();
        
        // Validate transition
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition task from %s to %s", oldStatus, newStatus)
            );
        }
        
        // Check dependencies
        if (newStatus == TaskStatus.IN_PROGRESS && hasPendingDependencies(task)) {
            throw new BusinessLogicException("Cannot start task with pending dependencies");
        }
        
        // Record history
        TaskHistory history = new TaskHistory();
        history.setTaskId(task.getId());
        history.setOldStatus(oldStatus.name());
        history.setNewStatus(newStatus.name());
        history.setComment(comment);
        history.setChangedBy(SecurityContext.getCurrentUserId());
        history.setChangedAt(LocalDateTime.now());
        taskHistoryRepository.save(history);
        
        // Update task
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        
        if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
            onTaskCompleted(task);
        }
        
        taskRepository.update(task);
        
        // Send notifications
        notificationService.notifyTaskStatusChange(task, oldStatus, newStatus);
    }
    
    private void onTaskCompleted(Task task) {
        // Check and unblock dependent tasks
        List<TaskDependency> dependencies = 
            dependencyRepository.findByDependsOnTaskId(task.getId());
        
        for (TaskDependency dep : dependencies) {
            Task dependentTask = taskRepository.findById(dep.getTaskId());
            
            if (dependentTask.getStatus() == TaskStatus.BLOCKED) {
                if (allDependenciesMet(dependentTask)) {
                    updateStatus(dependentTask, TaskStatus.PENDING, "Dependencies resolved");
                }
            }
        }
        
        // Check if all tasks in match are completed
        checkMatchCompletion(task.getBuddyMatchId());
    }
    
    private boolean hasPendingDependencies(Task task) {
        List<TaskDependency> dependencies = dependencyRepository.findByTaskId(task.getId());
        
        for (TaskDependency dep : dependencies) {
            Task dependencyTask = taskRepository.findById(dep.getDependsOnTaskId());
            if (dependencyTask.getStatus() != TaskStatus.COMPLETED) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean allDependenciesMet(Task task) {
        return !hasPendingDependencies(task);
    }
    
    private void checkMatchCompletion(Long matchId) {
        List<Task> tasks = taskRepository.findByMatchId(matchId);
        
        boolean allCompleted = tasks.stream()
            .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED || 
                          t.getStatus() == TaskStatus.CANCELLED);
        
        if (allCompleted) {
            BuddyMatch match = matchRepository.findById(matchId);
            if (match.getStatus() == MatchStatus.ACTIVE) {
                matchingService.transitionState(match, MatchStatus.COMPLETED, "All tasks completed");
            }
        }
    }
}
```

---

### 5.3 User Onboarding Workflow

#### Workflow Stages

```
REGISTERED → PROFILE_COMPLETE → BUDDY_ASSIGNED → IN_ONBOARDING → COMPLETED
```

#### Detailed Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                    ONBOARDING WORKFLOW                          │
└─────────────────────────────────────────────────────────────────┘

Day 0: Registration
├── User registers account
├── Email verification sent
├── Profile creation form
└── Status: REGISTERED

Day 0-1: Profile Completion
├── User completes profile (skills, experience, department)
├── System runs matching algorithm
├── Top 5 buddy suggestions generated
└── Status: PROFILE_COMPLETE

Day 1-2: Buddy Assignment
├── HR/Admin reviews suggestions
├── Selects buddy and sends invitation
├── Buddy receives notification (48h to respond)
├── If accepted → Status: BUDDY_ASSIGNED
└── If rejected → Offer next suggestion

Day 2: Onboarding Kickoff
├── Match status → ACTIVE
├── Default tasks created (9 tasks)
├── Welcome email sent to both parties
├── First task: "Complete IT Setup" (Day 0)
└── Status: IN_ONBOARDING

Week 1 (Day 7)
├── Task: "Shadow Buddy"
├── Feedback request: WEEK_1
└── Progress check-in notification

Week 2 (Day 14)
├── Feedback request: WEEK_2
└── Mid-point review

Month 1 (Day 30)
├── Task: "30-Day Check-in"
├── Feedback request: MONTH_1
├── Performance review with manager
└── Progress: 33% complete

Month 2 (Day 60)
├── Task: "60-Day Review"
├── Feedback request: MONTH_2
├── Advanced project assignments
└── Progress: 66% complete

Month 3 (Day 90)
├── Task: "90-Day Final Assessment"
├── Feedback request: FINAL
├── All tasks completed
├── Match status → COMPLETED
├── Completion certificate issued
└── Status: COMPLETED

Post-Completion
├── Buddy available for new matches
├── New employee can become buddy
└── Analytics updated
```

---

### 5.4 Feedback Collection Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                  FEEDBACK COLLECTION FLOW                        │
└─────────────────────────────────────────────────────────────────┘

Trigger: Scheduled job runs daily at 9 AM

For each active match:
  ├── Calculate days since match started
  │
  ├── If Day 7 (Week 1):
  │   ├── Send feedback request to new employee
  │   ├── Period: WEEK_1
  │   └── Deadline: 3 days
  │
  ├── If Day 14 (Week 2):
  │   ├── Send feedback request to new employee
  │   ├── Period: WEEK_2
  │   └── Deadline: 3 days
  │
  ├── If Day 30 (Month 1):
  │   ├── Send feedback request to both parties
  │   ├── Period: MONTH_1
  │   └── Deadline: 5 days
  │
  ├── If Day 60 (Month 2):
  │   ├── Send feedback request to both parties
  │   ├── Period: MONTH_2
  │   └── Deadline: 5 days
  │
  └── If Day 90 (Final):
      ├── Send feedback request to both parties
      ├── Period: FINAL
      ├── Deadline: 7 days
      └── Include comprehensive survey

Feedback Submission:
  ├── User receives notification
  ├── Clicks link to feedback form
  ├── Fills ratings (1-5) for each criterion
  ├── Adds comments (optional)
  ├── Chooses anonymous option
  ├── Submits feedback
  │
  ├── System validates:
  │   ├── All required ratings provided
  │   ├── No duplicate for same period
  │   └── Active buddy relationship exists
  │
  ├── Save feedback to database
  ├── Update buddy statistics
  ├── Send confirmation to submitter
  │
  └── If not anonymous:
      └── Notify recipient of new feedback

Reminder Flow:
  ├── If no feedback after 2 days → Send reminder
  ├── If no feedback after 4 days → Send urgent reminder
  └── If no feedback after deadline → Mark as missed, notify HR
```

---

### 5.5 Notification Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                   NOTIFICATION DISPATCH                          │
└─────────────────────────────────────────────────────────────────┘

Event Triggered:
  ├── Task assigned
  ├── Message received
  ├── Buddy matched
  ├── Feedback received
  ├── Task due soon
  └── System announcement

Notification Creation:
  ├── Determine notification type
  ├── Get user preferences
  ├── Check quiet hours
  │   └── If in quiet hours → Queue for later
  │
  ├── Create notification record
  │   ├── user_id
  │   ├── type
  │   ├── title
  │   ├── content
  │   ├── data (JSON)
  │   └── is_read: false
  │
  └── Save to database

Dispatch Channels:
  │
  ├── In-App Notification:
  │   ├── Always sent (if enabled)
  │   ├── Real-time via WebSocket
  │   └── Badge count updated
  │
  ├── Email Notification:
  │   ├── Check user preference
  │   ├── Check notification type settings
  │   ├── If daily digest enabled:
  │   │   └── Add to digest queue
  │   └── Else:
  │       └── Send immediate email
  │
  └── SMS Notification:
      ├── Only for critical types
      ├── Check user preference
      └── Send via SMS gateway

Daily Digest (9 AM):
  ├── For users with digest enabled:
  │   ├── Collect all notifications from last 24h
  │   ├── Group by type
  │   ├── Generate summary email
  │   └── Send single consolidated email
  │
  └── Mark digest notifications as sent

User Actions:
  ├── View notification → Mark as read
  ├── Click notification → Navigate to related item
  ├── Dismiss notification → Mark as read
  └── Mark all as read → Bulk update
```

---

### 5.6 Conflict Resolution Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                  CONFLICT RESOLUTION FLOW                        │
└─────────────────────────────────────────────────────────────────┘

Conflict Reported:
  ├── By new employee
  ├── By buddy
  ├── By manager
  └── Detected by system (low ratings, inactivity)

Conflict Types:
  ├── BUDDY_UNAVAILABLE
  ├── PERSONALITY_MISMATCH
  ├── WORKLOAD_OVERLOAD
  └── PERFORMANCE_ISSUE

Resolution Process:

1. Log Conflict:
   ├── Create conflict record
   ├── Capture: type, reporter, reason, evidence
   └── Timestamp and track

2. Notify Stakeholders:
   ├── HR Manager
   ├── Department Manager
   └── Admin (if escalated)

3. Assess Severity:
   ├── Critical → Immediate action
   ├── High → Action within 24h
   ├── Medium → Action within 3 days
   └── Low → Monitor and review

4. Resolution Strategy:

   If BUDDY_UNAVAILABLE:
   ├── End current match
   ├── Status → ENDED
   ├── Reason: "Buddy unavailable"
   ├── Run matching algorithm
   ├── Assign new buddy
   └── Resume onboarding

   If PERSONALITY_MISMATCH:
   ├── Schedule mediation call
   ├── If unresolved:
   │   ├── End current match
   │   ├── Collect feedback from both
   │   ├── Update matching preferences
   │   ├── Find new match with different profile
   │   └── Resume onboarding
   └── If resolved:
       └── Continue with monitoring

   If WORKLOAD_OVERLOAD:
   ├── Review buddy's current load
   ├── If > 3 active mentees:
   │   ├── Redistribute workload
   │   ├── Assign co-buddy (temporary support)
   │   └── Adjust capacity limits
   └── If capacity ok:
       └── Provide time management support

   If PERFORMANCE_ISSUE:
   ├── Review feedback history
   ├── Review task completion rates
   ├── Schedule performance discussion
   ├── If buddy issue:
   │   ├── Provide additional training
   │   ├── Assign mentor to buddy
   │   └── Monitor improvement
   ├── If employee issue:
   │   ├── Adjust task difficulty
   │   ├── Provide additional resources
   │   └── Extend timeline
   └── Escalate to manager if needed

5. Follow-up:
   ├── Schedule check-in after 1 week
   ├── Monitor progress
   ├── Collect feedback
   └── Close conflict if resolved

6. Documentation:
   ├── Update audit log
   ├── Record resolution actions
   ├── Update user profiles
   └── Generate report for HR
```

---

### 5.7 Scheduled Jobs

```java
// Daily at 9:00 AM - Check deadlines and send reminders
@Scheduled(cron = "0 0 9 * * *")
public void checkTaskDeadlines() {
    // Implementation in Section 1.3
}

// Daily at 9:00 AM - Send feedback requests
@Scheduled(cron = "0 0 9 * * *")
public void sendFeedbackRequests() {
    List<BuddyMatch> activeMatches = matchRepository.findByStatus(MatchStatus.ACTIVE);
    
    for (BuddyMatch match : activeMatches) {
        long daysSinceStart = ChronoUnit.DAYS.between(
            match.getMatchedAt().toLocalDate(),
            LocalDate.now()
        );
        
        FeedbackPeriod period = determineFeedbackPeriod(daysSinceStart);
        if (period != null && !feedbackExists(match.getId(), period)) {
            sendFeedbackRequest(match, period);
        }
    }
}

// Daily at 9:00 AM - Send daily digest emails
@Scheduled(cron = "0 0 9 * * *")
public void sendDailyDigests() {
    List<User> digestUsers = userRepository.findByDailyDigestEnabled(true);
    
    for (User user : digestUsers) {
        List<Notification> notifications = notificationRepository
            .findUnsentDigestNotifications(user.getId());
        
        if (!notifications.isEmpty()) {
            emailService.sendDailyDigest(user, notifications);
            notificationRepository.markAsSentInDigest(notifications);
        }
    }
}

// Every 5 minutes - Clean up expired sessions
@Scheduled(fixedRate = 300000)
public void cleanupExpiredSessions() {
    sessionManager.removeExpiredSessions();
}

// Every hour - Update analytics cache
@Scheduled(cron = "0 0 * * * *")
public void updateAnalyticsCache() {
    analyticsService.refreshCache();
}

// Daily at midnight - Archive completed matches (>90 days old)
@Scheduled(cron = "0 0 0 * * *")
public void archiveOldMatches() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
    List<BuddyMatch> oldMatches = matchRepository
        .findCompletedBefore(cutoffDate);
    
    for (BuddyMatch match : oldMatches) {
        archiveService.archiveMatch(match);
    }
}
```

---

*Document continues in next section...*

**Status:** Sections 1-5 completed.
**Remaining:** Sections 6-10 will cover Error Handling, Security, Performance, Testing, and Deployment.

---

## Document Summary

This Low-Level Design document provides comprehensive implementation specifications for the Onboarding Buddy Application, including:

✅ **Component Design** - Detailed module specifications with authentication flows, matching algorithms, task management, communication, feedback, and analytics

✅ **Database Design** - Complete schema with 14 tables (including task templates, template versions, and onboarding runs), relationships, indexes, and optimization strategies

✅ **API Specifications** - 50+ REST endpoints with request/response schemas, validation rules, and error handling

✅ **Class/Service Design** - Servlet architecture, service layer methods, repository pattern, utility classes, and design patterns

✅ **State Machines & Workflows** - State transitions for matches and tasks, complete onboarding workflow (90 days), feedback collection, notifications, conflict resolution, and scheduled jobs

### 🆕 **Enhanced Features**

**1. Onboarding Plan System (Parent-Child Hierarchy)**
- **Onboarding Plans** are parent records that group all task templates
- Managers create plans (e.g., "Engineering 90-Day Onboarding")
- Each plan contains multiple task templates as children
- Plans support atomic versioning - entire plan versioned together
- When plan is updated, new version created with all templates
- Onboarding runs reference specific **plan version** (not individual templates)
- Simpler relationship model: Run → Plan → Templates
- Plans can be department-specific or organization-wide
- Plans must be published/activated before use

**2. Fully Configurable Task Template System (NO Static Templates)**
- **Zero default templates** - All templates must be configured by Managers/SPOCs
- Templates are children of Onboarding Plans
- Templates cannot exist without a parent plan
- System enforces plan creation before onboarding can begin
- Complete audit trail for plan and template changes
- Template changes only affect NEW onboarding runs
- In-progress runs remain on their original plan version
- Rollback capability to previous plan versions

**3. Task Type Segregation**
Six distinct task types for better organization:
- `ADMINISTRATIVE` - IT setup, paperwork, access provisioning
- `TECHNICAL` - Training, certifications, tool setup
- `COMPLIANCE` - Mandatory training, policy acknowledgment
- `SOCIAL` - Team introductions, buddy meetings
- `PROJECT` - Actual work assignments
- `REVIEW` - Check-ins, performance reviews

**4. Comprehensive Ownership Model**
Tasks categorized by who owns/manages them:
- `MANAGER_OWNED` - Manager creates and tracks (e.g., performance reviews)
- `NEW_HIRE_OWNED` - New hire self-manages (e.g., self-paced training)
- `INTERNAL_TEAM_OWNED` - Internal teams like HR, IT (e.g., IT setup, paperwork)
- `INTERNAL_EMPLOYEE_OWNED` - Specific employees like buddy, mentor (e.g., shadowing)
- `EXTERNAL_TEAM_OWNED` - External vendors/contractors (e.g., background checks)
- `SHARED_OWNERSHIP` - Multiple parties collaborate

**5. Flexible Task Assignment**
Tasks can be assigned to 8 different assignee types:
- `NEW_EMPLOYEE` - Assigned to the new hire
- `BUDDY` - Assigned to the assigned buddy
- `MANAGER` - Assigned to employee's manager
- `HR_TEAM` - Assigned to HR team
- `IT_TEAM` - Assigned to IT team
- `TEAM_MEMBER` - Assigned to specific team member
- `TEAM` - Assigned to entire team (collaborative)
- `EXTERNAL_VENDOR` - Assigned to external party

**6. Onboarding Run Records**
- Separate parent record (`onboarding_runs`) tracks each onboarding instance
- Links to specific **plan version** (locked at creation time)
- All tasks instantiated from plan's templates
- Progress tracking at run level (completion percentage, task counts)
- Run status: ACTIVE, COMPLETED, CANCELLED
- Clear separation between plan definition and execution

**7. Sequential and Parallel Task Execution**
Two execution modes for flexible workflows:
- `SEQUENTIAL` - Tasks must complete in order (1→2→3)
  - Only first task available initially
  - Subsequent tasks BLOCKED until previous completes
  - Auto-unblocking when predecessor finishes
  - Ideal for dependent workflows
- `PARALLEL` - Tasks can run simultaneously
  - All tasks in group available immediately
  - No blocking between tasks
  - Group-based progress tracking
  - Ideal for independent activities
- **Mixed workflows** - Combine both modes in same onboarding run

**8. Advanced Features**
- Pre-boarding tasks support (negative dayOffset)
- Custom categories and searchable tags
- Template dependencies for complex workflows
- Grouped views by owner type and parallel groups
- Rich filtering and search capabilities
- Real-time blocking/unblocking notifications
- Plan publish/activate workflow
- Atomic plan versioning (all templates versioned together)

**Visual Example - Mixed Execution:**
```
Onboarding Run for John Doe
│
├─ Day 0: IT Setup (SEQUENTIAL - must follow order)
│  ├─ [1] Complete IT Setup ✓ COMPLETED
│  ├─ [2] Access Badge ⚙️ IN_PROGRESS (unblocked)
│  └─ [3] System Access 🔒 BLOCKED (waiting for #2)
│
├─ Day 0: Orientation (PARALLEL - do simultaneously)
│  ├─ Read Handbook ⏳ PENDING (can start)
│  ├─ Watch Video ⏳ PENDING (can start)
│  └─ HR Paperwork ⏳ PENDING (can start)
│
├─ Day 3: Training (PARALLEL - independent)
│  ├─ Security Training ✓ COMPLETED
│  ├─ Compliance Training ✓ COMPLETED
│  └─ Tool Training ⏳ PENDING (can start)
│
└─ Day 7: Project Work (SEQUENTIAL - ordered)
   ├─ [1] Shadow Buddy ⏳ PENDING (can start)
   ├─ [2] Code Review 🔒 BLOCKED (waiting for #1)
   └─ [3] First Project 🔒 BLOCKED (waiting for #2)

Legend:
✓ = Completed
⚙️ = In Progress
⏳ = Pending (can start)
🔒 = Blocked (cannot start yet)
```

**Benefits:**
- ✅ Complete flexibility - no hardcoded workflows
- ✅ Sequential tasks ensure proper order for dependent activities
- ✅ Parallel tasks maximize efficiency for independent activities
- ✅ Standardized processes across organization
- ✅ Easy updates without affecting in-progress onboardings
- ✅ Complete audit trail of template changes
- ✅ Department-specific customization
- ✅ Clear ownership and accountability
- ✅ Support for external vendors and contractors
- ✅ Reusability and consistency
- ✅ Clear separation of concerns (template vs execution)
- ✅ Automatic blocking/unblocking reduces manual coordination

**Implementation Ready:** Development team has all necessary details to begin implementation with embedded Tomcat servlets and ServiceNow-style frontend.

---

## 11. React Frontend Architecture

### 11.1 Project Structure

```
frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/                    # API client layer
│   │   ├── axiosConfig.ts
│   │   ├── authApi.ts
│   │   ├── userApi.ts
│   │   ├── planApi.ts
│   │   ├── taskApi.ts
│   │   ├── buddyApi.ts
│   │   └── feedbackApi.ts
│   ├── components/             # Reusable components
│   │   ├── common/
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── Table.tsx
│   │   │   └── Loader.tsx
│   │   ├── layout/
│   │   │   ├── Header.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Footer.tsx
│   │   │   └── Layout.tsx
│   │   └── features/
│   │       ├── TaskCard.tsx
│   │       ├── PlanCard.tsx
│   │       ├── UserAvatar.tsx
│   │       └── ProgressBar.tsx
│   ├── pages/                  # Page components
│   │   ├── auth/
│   │   │   ├── Login.tsx
│   │   │   ├── Register.tsx
│   │   │   └── ForgotPassword.tsx
│   │   ├── dashboard/
│   │   │   └── Dashboard.tsx
│   │   ├── plans/
│   │   │   ├── PlanList.tsx
│   │   │   ├── PlanCreate.tsx
│   │   │   ├── PlanEdit.tsx
│   │   │   └── PlanDetails.tsx
│   │   ├── tasks/
│   │   │   ├── TaskList.tsx
│   │   │   ├── TaskDetails.tsx
│   │   │   └── TaskBoard.tsx
│   │   ├── buddies/
│   │   │   ├── BuddyMatch.tsx
│   │   │   └── BuddyProfile.tsx
│   │   ├── feedback/
│   │   │   ├── FeedbackForm.tsx
│   │   │   └── FeedbackList.tsx
│   │   └── admin/
│   │       ├── UserManagement.tsx
│   │       ├── Analytics.tsx
│   │       └── Settings.tsx
│   ├── store/                  # Redux/Zustand store
│   │   ├── slices/
│   │   │   ├── authSlice.ts
│   │   │   ├── userSlice.ts
│   │   │   ├── planSlice.ts
│   │   │   ├── taskSlice.ts
│   │   │   └── notificationSlice.ts
│   │   └── store.ts
│   ├── hooks/                  # Custom React hooks
│   │   ├── useAuth.ts
│   │   ├── useApi.ts
│   │   ├── usePagination.ts
│   │   └── useWebSocket.ts
│   ├── utils/                  # Utility functions
│   │   ├── dateUtils.ts
│   │   ├── validators.ts
│   │   ├── formatters.ts
│   │   └── constants.ts
│   ├── types/                  # TypeScript types
│   │   ├── auth.types.ts
│   │   ├── user.types.ts
│   │   ├── plan.types.ts
│   │   ├── task.types.ts
│   │   └── api.types.ts
│   ├── styles/                 # Global styles
│   │   ├── servicenow-theme.ts
│   │   ├── tailwind.config.js
│   │   └── global.css
│   ├── App.tsx
│   ├── main.tsx
│   └── vite-env.d.ts
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

### 11.2 Font Setup

**Gilroy Font Import:**

Add to `public/index.html`:
```html
<head>
  <!-- Gilroy Font -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.cdnfonts.com/css/gilroy-bold" rel="stylesheet">
</head>
```

Or add to `src/styles/global.css`:
```css
/* Import Gilroy font */
@import url('https://fonts.cdnfonts.com/css/gilroy-bold');

/* Or use local font files */
@font-face {
  font-family: 'Gilroy';
  src: url('/fonts/Gilroy-Regular.woff2') format('woff2'),
       url('/fonts/Gilroy-Regular.woff') format('woff');
  font-weight: 400;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'Gilroy';
  src: url('/fonts/Gilroy-Medium.woff2') format('woff2'),
       url('/fonts/Gilroy-Medium.woff') format('woff');
  font-weight: 500;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'Gilroy';
  src: url('/fonts/Gilroy-SemiBold.woff2') format('woff2'),
       url('/fonts/Gilroy-SemiBold.woff') format('woff');
  font-weight: 600;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'Gilroy';
  src: url('/fonts/Gilroy-Bold.woff2') format('woff2'),
       url('/fonts/Gilroy-Bold.woff') format('woff');
  font-weight: 700;
  font-style: normal;
  font-display: swap;
}

* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

### 11.3 ServiceNow-Style Theme Configuration

```typescript
// servicenow-theme.ts
import { createTheme } from '@mui/material/styles';

export const serviceNowTheme = createTheme({
  palette: {
    primary: {
      main: '#0F62FE',      // ServiceNow blue
      light: '#4589FF',
      dark: '#0043CE',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#6929C4',      // ServiceNow purple
      light: '#8A3FFC',
      dark: '#491D8B',
    },
    success: {
      main: '#24A148',      // ServiceNow green
      light: '#42BE65',
      dark: '#198038',
    },
    warning: {
      main: '#F1C21B',      // ServiceNow yellow
      light: '#FDD13A',
      dark: '#D2A106',
    },
    error: {
      main: '#DA1E28',      // ServiceNow red
      light: '#FA4D56',
      dark: '#A2191F',
    },
    background: {
      default: '#F4F4F4',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#161616',
      secondary: '#525252',
    },
  },
  typography: {
    fontFamily: '"Gilroy", "Inter", "Helvetica Neue", Arial, sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 700,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
    },
    body1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.5,
    },
    body2: {
      fontSize: '0.875rem',
      fontWeight: 400,
      lineHeight: 1.43,
    },
    button: {
      fontWeight: 600,
      textTransform: 'none',
    },
  },
  shape: {
    borderRadius: 4,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 500,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
        },
      },
    },
  },
});
```

### 11.4 API Client Setup

```typescript
// api/axiosConfig.ts
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle errors
apiClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    if (error.response?.status === 401) {
      // Try refresh token
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh-token`, {
            refreshToken,
          });
          localStorage.setItem('authToken', response.data.data.token);
          // Retry original request
          error.config.headers.Authorization = `Bearer ${response.data.data.token}`;
          return axios(error.config);
        } catch (refreshError) {
          // Refresh failed, logout
          localStorage.clear();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

### 11.5 Key React Components

#### Dashboard Component
```typescript
// pages/dashboard/Dashboard.tsx
import React, { useEffect } from 'react';
import { Grid, Card, CardContent, Typography } from '@mui/material';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchDashboardData } from '@/store/slices/dashboardSlice';
import TaskCard from '@/components/features/TaskCard';
import ProgressBar from '@/components/features/ProgressBar';

const Dashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const { tasks, progress, loading } = useAppSelector((state) => state.dashboard);
  const { user } = useAppSelector((state) => state.auth);

  useEffect(() => {
    dispatch(fetchDashboardData());
  }, [dispatch]);

  return (
    <div className="p-6">
      <Typography variant="h4" className="mb-6">
        Welcome back, {user?.name}!
      </Typography>

      <Grid container spacing={3}>
        {/* Progress Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6">Onboarding Progress</Typography>
              <ProgressBar 
                value={progress.completionPercentage} 
                total={progress.totalTasks}
                completed={progress.completedTasks}
              />
            </CardContent>
          </Card>
        </Grid>

        {/* Tasks */}
        <Grid item xs={12}>
          <Typography variant="h5" className="mb-4">Your Tasks</Typography>
          <Grid container spacing={2}>
            {tasks.map((task) => (
              <Grid item xs={12} md={6} lg={4} key={task.id}>
                <TaskCard task={task} />
              </Grid>
            ))}
          </Grid>
        </Grid>
      </Grid>
    </div>
  );
};

export default Dashboard;
```

#### Plan Management Component
```typescript
// pages/plans/PlanCreate.tsx
import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button, TextField, Card, CardContent } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { createPlan } from '@/api/planApi';

const planSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  department: z.string().min(1, 'Department is required'),
  durationDays: z.number().min(1).max(365),
});

type PlanFormData = z.infer<typeof planSchema>;

const PlanCreate: React.FC = () => {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm<PlanFormData>({
    resolver: zodResolver(planSchema),
  });

  const onSubmit = async (data: PlanFormData) => {
    try {
      const response = await createPlan(data);
      navigate(`/plans/${response.data.planId}`);
    } catch (error) {
      console.error('Failed to create plan:', error);
    }
  };

  return (
    <div className="p-6">
      <Card>
        <CardContent>
          <Typography variant="h5" className="mb-4">Create Onboarding Plan</Typography>
          <form onSubmit={handleSubmit(onSubmit)}>
            <TextField
              {...register('name')}
              label="Plan Name"
              fullWidth
              error={!!errors.name}
              helperText={errors.name?.message}
              className="mb-4"
            />
            <TextField
              {...register('description')}
              label="Description"
              multiline
              rows={4}
              fullWidth
              error={!!errors.description}
              helperText={errors.description?.message}
              className="mb-4"
            />
            <Button type="submit" variant="contained" color="primary">
              Create Plan
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default PlanCreate;
```

### 11.6 Build Configuration

```javascript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: '../backend/src/main/webapp',  // Build into Java webapp folder
    emptyOutDir: true,
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### 11.7 Package.json

```json
{
  "name": "onboard-buddy-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext ts,tsx"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@mui/material": "^5.14.0",
    "@emotion/react": "^11.11.0",
    "@emotion/styled": "^11.11.0",
    "@reduxjs/toolkit": "^1.9.7",
    "react-redux": "^8.1.3",
    "axios": "^1.6.0",
    "react-hook-form": "^7.48.0",
    "zod": "^3.22.0",
    "@hookform/resolvers": "^3.3.0",
    "lucide-react": "^0.294.0",
    "date-fns": "^2.30.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "tailwindcss": "^3.3.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "eslint": "^8.54.0"
  }
}
```

---

## Next Steps for Implementation

1. **Environment Setup**
   - Install Java 11+, Maven, MySQL/PostgreSQL
   - Install Node.js 18+ and npm/yarn
   - Configure IDE (IntelliJ IDEA/Eclipse for backend, VS Code for frontend)
   - Set up version control (Git)

2. **Backend Development**
   - Create Maven project with embedded Tomcat
   - Implement database schema
   - Develop servlet controllers and services
   - Add authentication filters (Basic, OAuth, JWT)
   - Implement business logic and validation

3. **Frontend Development (React)**
   - Initialize React project with Vite and TypeScript
   - Set up ServiceNow-style theme with Material-UI
   - Configure Redux Toolkit for state management
   - Implement authentication flow with JWT
   - Create reusable components (cards, tables, forms)
   - Build page components (dashboard, plans, tasks, buddies)
   - Set up Axios interceptors for API calls
   - Implement WebSocket for real-time notifications
   - Add form validation with React Hook Form + Zod

4. **Integration**
   - Configure Vite to build into Java webapp folder
   - Set up proxy for development
   - Test API integration
   - Implement error handling and loading states

5. **Testing**
   - Backend: JUnit 5, Mockito for unit tests
   - Frontend: Vitest, React Testing Library
   - Integration tests for APIs
   - End-to-end testing with Playwright
   - Performance testing

6. **Deployment**
   - Build React app: `npm run build`
   - Build backend JAR with frontend assets included
   - Configure production database
   - Set up monitoring and logging
   - Deploy to cloud/on-premise

---

## 12. Deployment Configuration

### 12.1 Conventional Deployment (Standalone JAR)

#### Build Process

**Step 1: Build Frontend**
```bash
cd frontend
npm install
npm run build
# Output: ../backend/src/main/webapp/
```

**Step 2: Build Backend JAR**
```bash
cd backend
mvn clean package
# Output: target/onboard-buddy-1.0.0.jar
```

#### Maven Configuration (pom.xml)

```xml
<project>
    <groupId>com.onboardbuddy</groupId>
    <artifactId>onboard-buddy</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <tomcat.version>9.0.80</tomcat.version>
    </properties>

    <dependencies>
        <!-- Embedded Tomcat -->
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-core</artifactId>
            <version>${tomcat.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-jasper</artifactId>
            <version>${tomcat.version}</version>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>5.0.1</version>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        
        <!-- Logging -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.11</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Shade Plugin for executable JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.onboardbuddy.Application</mainClass>
                                </transformer>
                            </transformers>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Application Main Class

```java
package com.onboardbuddy;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Application {
    
    private static final int PORT = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "8080")
    );
    
    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector();
        
        // Add webapp context
        String webappDirLocation = "src/main/webapp/";
        Context ctx = tomcat.addWebapp("", new File(webappDirLocation).getAbsolutePath());
        
        // Enable hot reload in development
        ctx.setReloadable(true);
        
        tomcat.start();
        System.out.println("Onboard Buddy Application started on port " + PORT);
        tomcat.getServer().await();
    }
}
```

#### Configuration File (application.properties)

```properties
# Database Configuration
db.url=jdbc:mysql://localhost:3306/onboard_buddy
db.username=root
db.password=password
db.driver=com.mysql.cj.jdbc.Driver

# Connection Pool
db.pool.maxSize=20
db.pool.minIdle=5
db.pool.connectionTimeout=30000

# JWT Configuration
jwt.secret=your-secret-key-change-in-production
jwt.expiration=86400000
jwt.refreshExpiration=604800000

# Server Configuration
server.port=8080
server.contextPath=/

# CORS Configuration
cors.allowedOrigins=http://localhost:3000,https://yourdomain.com

# File Upload
upload.maxFileSize=10485760
upload.directory=/var/onboard-buddy/uploads

# Logging
logging.level=INFO
logging.file=/var/log/onboard-buddy/application.log
```

#### Running the Application

**Development:**
```bash
java -jar onboard-buddy-1.0.0.jar
```

**Production with custom config:**
```bash
java -jar onboard-buddy-1.0.0.jar \
  --spring.config.location=file:/etc/onboard-buddy/application.properties
```

**As a service (systemd):**
```ini
# /etc/systemd/system/onboard-buddy.service
[Unit]
Description=Onboard Buddy Application
After=network.target

[Service]
Type=simple
User=onboard-buddy
WorkingDirectory=/opt/onboard-buddy
ExecStart=/usr/bin/java -jar /opt/onboard-buddy/onboard-buddy-1.0.0.jar
Restart=on-failure
RestartSec=10

Environment="JAVA_OPTS=-Xmx512m -Xms256m"
Environment="DB_URL=jdbc:mysql://localhost:3306/onboard_buddy"
Environment="DB_USERNAME=appuser"
Environment="DB_PASSWORD=securepassword"

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable onboard-buddy
sudo systemctl start onboard-buddy
sudo systemctl status onboard-buddy
```

---

### 12.2 Docker Deployment

#### Dockerfile

```dockerfile
# Multi-stage build

# Stage 1: Build Frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend
FROM maven:3.9-eclipse-temurin-11 AS backend-build
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /app/frontend/dist ./src/main/webapp
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR from build stage
COPY --from=backend-build /app/backend/target/onboard-buddy-*.jar ./app.jar

# Create directories for logs and uploads
RUN mkdir -p /var/log/onboard-buddy /var/onboard-buddy/uploads && \
    chown -R appuser:appgroup /var/log/onboard-buddy /var/onboard-buddy

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Docker Compose (docker-compose.yml)

```yaml
version: '3.8'

services:
  # Database
  mysql:
    image: mysql:8.0
    container_name: onboard-buddy-db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: onboard_buddy
      MYSQL_USER: appuser
      MYSQL_PASSWORD: apppassword
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - onboard-buddy-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: onboard-buddy-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:mysql://mysql:3306/onboard_buddy
      - DB_USERNAME=appuser
      - DB_PASSWORD=apppassword
      - JWT_SECRET=${JWT_SECRET:-change-me-in-production}
      - CORS_ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com
    depends_on:
      mysql:
        condition: service_healthy
    volumes:
      - app_logs:/var/log/onboard-buddy
      - app_uploads:/var/onboard-buddy/uploads
    networks:
      - onboard-buddy-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # Nginx (Optional - for production)
  nginx:
    image: nginx:alpine
    container_name: onboard-buddy-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - app
    networks:
      - onboard-buddy-network

volumes:
  mysql_data:
    driver: local
  app_logs:
    driver: local
  app_uploads:
    driver: local

networks:
  onboard-buddy-network:
    driver: bridge
```

#### Environment Variables (.env)

```bash
# Database
DB_URL=jdbc:mysql://mysql:3306/onboard_buddy
DB_USERNAME=appuser
DB_PASSWORD=securepassword123

# JWT
JWT_SECRET=your-super-secret-jwt-key-change-in-production-min-256-bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com

# Server
PORT=8080

# Logging
LOG_LEVEL=INFO
```

#### Nginx Configuration (nginx.conf)

```nginx
events {
    worker_connections 1024;
}

http {
    upstream app_backend {
        server app:8080;
    }

    server {
        listen 80;
        server_name yourdomain.com;

        # Redirect HTTP to HTTPS
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name yourdomain.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        # Gzip compression
        gzip on;
        gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

        # API proxy
        location /api/ {
            proxy_pass http://app_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # WebSocket support
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        # Static files (React app)
        location / {
            proxy_pass http://app_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

#### Docker Commands

**Build and run:**
```bash
# Build image
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

**Production deployment:**
```bash
# Build with production config
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Scale application
docker-compose up -d --scale app=3

# Update application
docker-compose pull
docker-compose up -d --no-deps --build app
```

---

### 12.3 Deployment Comparison

| Feature | Standalone JAR | Docker |
|---------|---------------|--------|
| **Ease of Setup** | Simple | Requires Docker |
| **Dependencies** | Java 11+ only | Docker + Docker Compose |
| **Portability** | Platform-specific | Fully portable |
| **Resource Usage** | Lower overhead | Slight overhead |
| **Scaling** | Manual | Easy with orchestration |
| **Isolation** | Process-level | Container-level |
| **Database** | External setup | Bundled in compose |
| **Updates** | Replace JAR | Pull new image |
| **Monitoring** | External tools | Container metrics |
| **Best For** | Traditional servers | Cloud/Kubernetes |

---

### 12.4 Cloud Deployment Options

#### AWS Deployment
- **Standalone JAR**: EC2 instance with systemd service
- **Docker**: ECS/Fargate or EKS (Kubernetes)
- **Database**: RDS MySQL

#### Azure Deployment
- **Standalone JAR**: Azure App Service or VM
- **Docker**: Azure Container Instances or AKS
- **Database**: Azure Database for MySQL

#### Google Cloud Deployment
- **Standalone JAR**: Compute Engine with startup script
- **Docker**: Cloud Run or GKE
- **Database**: Cloud SQL for MySQL

#### Kubernetes Deployment (k8s-deployment.yaml)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: onboard-buddy
spec:
  replicas: 3
  selector:
    matchLabels:
      app: onboard-buddy
  template:
    metadata:
      labels:
        app: onboard-buddy
    spec:
      containers:
      - name: app
        image: onboard-buddy:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: onboard-buddy-service
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: onboard-buddy
```

---

**Document Version:** 1.0  
**Last Updated:** November 14, 2025  
**Status:** Complete (Sections 1-12)  
**Total Pages:** ~140 pages of detailed specifications
