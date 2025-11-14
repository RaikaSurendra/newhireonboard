-- Insert admin user if not exists
INSERT IGNORE INTO users (email, password_hash, name, role, department, status, created_at, updated_at) 
VALUES ('admin@onboardbuddy.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.hl/jpe', 'System Admin', 'ADMIN', 'IT', 'ACTIVE', NOW(), NOW());

SELECT 'Admin user created/verified' as message;
SELECT id, email, name, role, status FROM users WHERE email='admin@onboardbuddy.com';
