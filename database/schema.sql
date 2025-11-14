-- Onboarding Buddy Application - Database Schema
-- MySQL 8.0+

-- Drop existing tables (in reverse order of dependencies)
DROP TABLE IF EXISTS task_history;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS onboarding_runs;
DROP TABLE IF EXISTS task_template_versions;
DROP TABLE IF EXISTS task_templates;
DROP TABLE IF EXISTS onboarding_plan_versions;
DROP TABLE IF EXISTS onboarding_plans;
DROP TABLE IF EXISTS buddy_matches;
DROP TABLE IF EXISTS users;

-- Users Table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role ENUM('NEW_EMPLOYEE', 'BUDDY', 'ADMIN', 'HR_MANAGER', 'MANAGER', 'ONBOARDING_SPOC') NOT NULL,
    department VARCHAR(100),
    skills JSON,
    experience_level ENUM('JUNIOR', 'MID', 'SENIOR'),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status ENUM('ACTIVE', 'INACTIVE', 'PENDING') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_department (department),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Buddy Matches Table
CREATE TABLE buddy_matches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    buddy_user_id BIGINT NOT NULL,
    new_employee_id BIGINT NOT NULL,
    status ENUM('PENDING', 'SUGGESTED', 'ACCEPTED', 'ACTIVE', 'COMPLETED', 'ENDED') NOT NULL,
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

-- Onboarding Plans Table
CREATE TABLE onboarding_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    department VARCHAR(100),
    duration_days INT NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    
    INDEX idx_department (department),
    INDEX idx_active (is_active),
    INDEX idx_version (id, version),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Plan Version History
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

-- Task Templates Table
CREATE TABLE task_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    plan_version INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    day_offset INT NOT NULL,
    estimated_duration INT,
    
    -- Task Type and Ownership
    task_type ENUM('ADMINISTRATIVE', 'TECHNICAL', 'COMPLIANCE', 'SOCIAL', 'PROJECT', 'REVIEW') NOT NULL,
    owner_type ENUM('MANAGER_OWNED', 'NEW_HIRE_OWNED', 'INTERNAL_TEAM_OWNED',
                    'INTERNAL_EMPLOYEE_OWNED', 'EXTERNAL_TEAM_OWNED', 'SHARED_OWNERSHIP') NOT NULL,
    assignee_type ENUM('NEW_EMPLOYEE', 'BUDDY', 'MANAGER', 'HR_TEAM',
                       'IT_TEAM', 'TEAM_MEMBER', 'TEAM', 'EXTERNAL_VENDOR') NOT NULL,
    
    -- Execution Control
    execution_mode ENUM('SEQUENTIAL', 'PARALLEL') NOT NULL DEFAULT 'PARALLEL',
    sequence_order INT,
    parallel_group VARCHAR(100),
    
    -- Additional metadata
    category VARCHAR(100),
    tags JSON,
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

-- Template Version History
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

-- Onboarding Runs Table
CREATE TABLE onboarding_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    buddy_match_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    plan_version INT NOT NULL,
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

-- Tasks Table
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    onboarding_run_id BIGINT NOT NULL,
    template_id BIGINT,
    template_version INT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status ENUM('PENDING', 'IN_PROGRESS', 'BLOCKED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    assigned_to BIGINT NOT NULL,
    assigned_to_type ENUM('NEW_EMPLOYEE', 'BUDDY', 'MANAGER', 'TEAM_MEMBER', 'TEAM'),
    created_by BIGINT NOT NULL,
    due_date DATE,
    completed_at TIMESTAMP NULL,
    
    -- Execution Control
    execution_mode ENUM('SEQUENTIAL', 'PARALLEL') NOT NULL DEFAULT 'PARALLEL',
    sequence_order INT,
    parallel_group VARCHAR(100),
    is_blocked BOOLEAN DEFAULT FALSE,
    
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

-- Messages Table
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
    INDEX idx_unread (receiver_id, is_read),
    INDEX idx_conversation (sender_id, receiver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Feedback Table
CREATE TABLE feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comments TEXT,
    feedback_type ENUM('BUDDY_TO_EMPLOYEE', 'EMPLOYEE_TO_BUDDY', 'MANAGER_REVIEW') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (match_id) REFERENCES buddy_matches(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_match (match_id),
    INDEX idx_from_user (from_user_id),
    INDEX idx_to_user (to_user_id),
    INDEX idx_type (feedback_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications Table
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user (user_id),
    INDEX idx_unread (user_id, is_read),
    INDEX idx_type (type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Task History Table (for audit trail)
CREATE TABLE task_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    changed_by BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_task (task_id),
    INDEX idx_changed_by (changed_by),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create default admin user (password: admin123)
INSERT INTO users (email, password_hash, name, role, department, status) VALUES
('admin@onboardbuddy.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'System Admin', 'ADMIN', 'IT', 'ACTIVE');

-- Success message
SELECT 'Database schema created successfully!' AS message;
