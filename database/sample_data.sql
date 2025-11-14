-- Sample Data for OnboardBuddy Application
-- Run this after creating the schema

-- Insert Buddies (experienced employees who will mentor new hires)
INSERT INTO users (email, password_hash, name, role, department, skills, experience_level, phone, status) VALUES
-- Password for all users: Password@123
('john.smith@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'John Smith', 'BUDDY', 'Engineering', '["Java", "Spring Boot", "Microservices", "AWS"]', 'SENIOR', '+1-555-0101', 'ACTIVE'),
('sarah.johnson@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Sarah Johnson', 'BUDDY', 'Engineering', '["React", "TypeScript", "Node.js", "GraphQL"]', 'SENIOR', '+1-555-0102', 'ACTIVE'),
('michael.chen@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Michael Chen', 'BUDDY', 'Product', '["Product Management", "Agile", "User Research"]', 'MID', '+1-555-0103', 'ACTIVE'),
('emily.davis@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Emily Davis', 'BUDDY', 'Design', '["UI/UX Design", "Figma", "Design Systems"]', 'SENIOR', '+1-555-0104', 'ACTIVE'),
('david.wilson@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'David Wilson', 'BUDDY', 'Engineering', '["Python", "Machine Learning", "Data Science"]', 'SENIOR', '+1-555-0105', 'ACTIVE'),
('lisa.anderson@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Lisa Anderson', 'BUDDY', 'Marketing', '["Digital Marketing", "SEO", "Content Strategy"]', 'MID', '+1-555-0106', 'ACTIVE');

-- Insert New Employees (new hires who need onboarding)
INSERT INTO users (email, password_hash, name, role, department, skills, experience_level, phone, status) VALUES
('alex.martinez@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Alex Martinez', 'NEW_EMPLOYEE', 'Engineering', '["Java", "Spring", "REST APIs"]', 'JUNIOR', '+1-555-0201', 'ACTIVE'),
('jessica.taylor@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Jessica Taylor', 'NEW_EMPLOYEE', 'Engineering', '["JavaScript", "React", "HTML/CSS"]', 'JUNIOR', '+1-555-0202', 'ACTIVE'),
('ryan.brown@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Ryan Brown', 'NEW_EMPLOYEE', 'Product', '["Product Management", "Analytics"]', 'JUNIOR', '+1-555-0203', 'ACTIVE'),
('sophia.garcia@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Sophia Garcia', 'NEW_EMPLOYEE', 'Design', '["UI Design", "Adobe XD", "Prototyping"]', 'JUNIOR', '+1-555-0204', 'ACTIVE'),
('james.lee@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'James Lee', 'NEW_EMPLOYEE', 'Engineering', '["Python", "Django", "PostgreSQL"]', 'MID', '+1-555-0205', 'ACTIVE'),
('olivia.white@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Olivia White', 'NEW_EMPLOYEE', 'Marketing', '["Social Media", "Content Creation"]', 'JUNIOR', '+1-555-0206', 'ACTIVE'),
('daniel.thomas@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Daniel Thomas', 'NEW_EMPLOYEE', 'Engineering', '["Go", "Kubernetes", "Docker"]', 'MID', '+1-555-0207', 'ACTIVE'),
('emma.harris@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'Emma Harris', 'NEW_EMPLOYEE', 'Engineering', '["React Native", "Mobile Development"]', 'JUNIOR', '+1-555-0208', 'ACTIVE');

-- Insert HR Managers and Admins
INSERT INTO users (email, password_hash, name, role, department, phone, status) VALUES
('hr.manager@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'HR Manager', 'HR_MANAGER', 'HR', '+1-555-0301', 'ACTIVE'),
('system.admin@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'System Admin', 'ADMIN', 'IT', '+1-555-0302', 'ACTIVE');

-- Create some buddy matches
-- Get the user IDs first (assuming sequential IDs starting from existing users)
-- Match 1: John Smith (Buddy) with Alex Martinez (New Employee)
INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, match_score, matched_at, accepted_at, created_by)
SELECT 
    (SELECT id FROM users WHERE email = 'john.smith@company.com'),
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    'ACTIVE',
    0.95,
    NOW() - INTERVAL 7 DAY,
    NOW() - INTERVAL 6 DAY,
    (SELECT id FROM users WHERE email = 'hr.manager@company.com');

-- Match 2: Sarah Johnson (Buddy) with Jessica Taylor (New Employee)
INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, match_score, matched_at, accepted_at, created_by)
SELECT 
    (SELECT id FROM users WHERE email = 'sarah.johnson@company.com'),
    (SELECT id FROM users WHERE email = 'jessica.taylor@company.com'),
    'ACTIVE',
    0.92,
    NOW() - INTERVAL 5 DAY,
    NOW() - INTERVAL 4 DAY,
    (SELECT id FROM users WHERE email = 'hr.manager@company.com');

-- Match 3: Michael Chen (Buddy) with Ryan Brown (New Employee)
INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, match_score, matched_at, accepted_at, created_by)
SELECT 
    (SELECT id FROM users WHERE email = 'michael.chen@company.com'),
    (SELECT id FROM users WHERE email = 'ryan.brown@company.com'),
    'ACCEPTED',
    0.88,
    NOW() - INTERVAL 3 DAY,
    NOW() - INTERVAL 2 DAY,
    (SELECT id FROM users WHERE email = 'hr.manager@company.com');

-- Match 4: Emily Davis (Buddy) with Sophia Garcia (New Employee)
INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, match_score, matched_at, created_by)
SELECT 
    (SELECT id FROM users WHERE email = 'emily.davis@company.com'),
    (SELECT id FROM users WHERE email = 'sophia.garcia@company.com'),
    'PENDING',
    0.90,
    NOW() - INTERVAL 1 DAY,
    (SELECT id FROM users WHERE email = 'hr.manager@company.com');

-- Match 5: David Wilson (Buddy) with James Lee (New Employee)
INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, match_score, matched_at, accepted_at, created_by)
SELECT 
    (SELECT id FROM users WHERE email = 'david.wilson@company.com'),
    (SELECT id FROM users WHERE email = 'james.lee@company.com'),
    'ACTIVE',
    0.87,
    NOW() - INTERVAL 10 DAY,
    NOW() - INTERVAL 9 DAY,
    (SELECT id FROM users WHERE email = 'hr.manager@company.com');

-- Match 6: Lisa Anderson (Buddy) with Olivia White (New Employee)
INSERT INTO buddy_matches (buddy_user_id, new_employee_id, status, match_score, matched_at, created_by)
SELECT 
    (SELECT id FROM users WHERE email = 'lisa.anderson@company.com'),
    (SELECT id FROM users WHERE email = 'olivia.white@company.com'),
    'SUGGESTED',
    0.85,
    NOW(),
    (SELECT id FROM users WHERE email = 'hr.manager@company.com');

-- Add some messages between buddies and new employees
INSERT INTO messages (sender_id, receiver_id, content, is_read, created_at)
SELECT 
    (SELECT id FROM users WHERE email = 'john.smith@company.com'),
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    'Welcome to the team! I''m excited to be your buddy. Let''s schedule a meeting this week to discuss your onboarding plan.',
    TRUE,
    NOW() - INTERVAL 6 DAY;

INSERT INTO messages (sender_id, receiver_id, content, is_read, created_at, read_at)
SELECT 
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    (SELECT id FROM users WHERE email = 'john.smith@company.com'),
    'Thank you! I''m looking forward to working with you. How about Tuesday at 2 PM?',
    TRUE,
    NOW() - INTERVAL 6 DAY,
    NOW() - INTERVAL 6 DAY;

INSERT INTO messages (sender_id, receiver_id, content, is_read, created_at)
SELECT 
    (SELECT id FROM users WHERE email = 'sarah.johnson@company.com'),
    (SELECT id FROM users WHERE email = 'jessica.taylor@company.com'),
    'Hi Jessica! Welcome aboard! I''ve prepared some resources for you to get started with our frontend stack.',
    FALSE,
    NOW() - INTERVAL 1 DAY;

-- Add some feedback
INSERT INTO feedback (match_id, from_user_id, to_user_id, rating, comments, feedback_type, created_at)
SELECT 
    (SELECT id FROM buddy_matches WHERE buddy_user_id = (SELECT id FROM users WHERE email = 'john.smith@company.com') LIMIT 1),
    (SELECT id FROM users WHERE email = 'john.smith@company.com'),
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    5,
    'Alex is doing great! Very eager to learn and asks good questions. Making excellent progress.',
    'BUDDY_TO_EMPLOYEE',
    NOW() - INTERVAL 2 DAY;

INSERT INTO feedback (match_id, from_user_id, to_user_id, rating, comments, feedback_type, created_at)
SELECT 
    (SELECT id FROM buddy_matches WHERE new_employee_id = (SELECT id FROM users WHERE email = 'alex.martinez@company.com') LIMIT 1),
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    (SELECT id FROM users WHERE email = 'john.smith@company.com'),
    5,
    'John has been an amazing mentor! Very patient and always available to help. Really appreciate his guidance.',
    'EMPLOYEE_TO_BUDDY',
    NOW() - INTERVAL 1 DAY;

-- Add some notifications
INSERT INTO notifications (user_id, title, message, type, is_read, related_entity_type, related_entity_id, created_at)
SELECT 
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    'Welcome to OnboardBuddy!',
    'Your onboarding journey begins today. Check out your buddy match and tasks.',
    'SUCCESS',
    TRUE,
    'USER',
    (SELECT id FROM users WHERE email = 'alex.martinez@company.com'),
    NOW() - INTERVAL 7 DAY;

INSERT INTO notifications (user_id, title, message, type, is_read, related_entity_type, related_entity_id, created_at)
SELECT 
    (SELECT id FROM users WHERE email = 'sophia.garcia@company.com'),
    'New Buddy Match Suggested',
    'You have been matched with Emily Davis as your onboarding buddy!',
    'INFO',
    FALSE,
    'BUDDY_MATCH',
    (SELECT id FROM buddy_matches WHERE new_employee_id = (SELECT id FROM users WHERE email = 'sophia.garcia@company.com') LIMIT 1),
    NOW() - INTERVAL 1 DAY;

INSERT INTO notifications (user_id, title, message, type, is_read, related_entity_type, related_entity_id, created_at)
SELECT 
    (SELECT id FROM users WHERE email = 'emily.davis@company.com'),
    'New Buddy Match Assignment',
    'You have been assigned as a buddy to Sophia Garcia. Please review and accept the match.',
    'INFO',
    FALSE,
    'BUDDY_MATCH',
    (SELECT id FROM buddy_matches WHERE buddy_user_id = (SELECT id FROM users WHERE email = 'emily.davis@company.com') LIMIT 1),
    NOW() - INTERVAL 1 DAY;

-- Success message
SELECT 'Sample data inserted successfully!' AS message;
SELECT CONCAT('Total Users: ', COUNT(*)) AS summary FROM users;
SELECT CONCAT('Total Buddy Matches: ', COUNT(*)) AS summary FROM buddy_matches;
SELECT CONCAT('Total Messages: ', COUNT(*)) AS summary FROM messages;
SELECT CONCAT('Total Feedback: ', COUNT(*)) AS summary FROM feedback;
SELECT CONCAT('Total Notifications: ', COUNT(*)) AS summary FROM notifications;
