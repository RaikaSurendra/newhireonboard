package com.onboardbuddy.utils;

import com.onboardbuddy.config.ConfigLoader;

import java.util.regex.Pattern;

/**
 * Input validation utility
 * Addresses security issues #4 and #12 - input validation and password policy
 */
public class ValidationUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(
        "[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]"
    );
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Validate email format
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Email is required");
        }
        
        if (email.length() > 255) {
            return new ValidationResult(false, "Email is too long (max 255 characters)");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new ValidationResult(false, "Invalid email format");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    /**
     * Validate password against policy
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }
        
        int minLength = ConfigLoader.getInt("password.minLength", 8);
        if (password.length() < minLength) {
            return new ValidationResult(false, 
                String.format("Password must be at least %d characters long", minLength));
        }
        
        if (password.length() > 128) {
            return new ValidationResult(false, "Password is too long (max 128 characters)");
        }
        
        boolean requireUppercase = ConfigLoader.getBoolean("password.requireUppercase", true);
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            return new ValidationResult(false, "Password must contain at least one uppercase letter");
        }
        
        boolean requireLowercase = ConfigLoader.getBoolean("password.requireLowercase", true);
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            return new ValidationResult(false, "Password must contain at least one lowercase letter");
        }
        
        boolean requireDigit = ConfigLoader.getBoolean("password.requireDigit", true);
        if (requireDigit && !password.matches(".*\\d.*")) {
            return new ValidationResult(false, "Password must contain at least one digit");
        }
        
        boolean requireSpecialChar = ConfigLoader.getBoolean("password.requireSpecialChar", true);
        if (requireSpecialChar && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return new ValidationResult(false, "Password must contain at least one special character");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    /**
     * Validate name (alphanumeric and spaces only)
     */
    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, "Name is required");
        }
        
        if (name.length() > 255) {
            return new ValidationResult(false, "Name is too long (max 255 characters)");
        }
        
        if (!name.matches("^[a-zA-Z\\s'-]+$")) {
            return new ValidationResult(false, "Name contains invalid characters");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    /**
     * Validate role
     */
    public static ValidationResult validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return new ValidationResult(false, "Role is required");
        }
        
        String[] validRoles = {
            "NEW_EMPLOYEE", "BUDDY", "ADMIN", "HR_MANAGER", "MANAGER", "ONBOARDING_SPOC"
        };
        
        for (String validRole : validRoles) {
            if (validRole.equals(role)) {
                return new ValidationResult(true, "Valid");
            }
        }
        
        return new ValidationResult(false, "Invalid role");
    }
    
    /**
     * Sanitize string input to prevent XSS
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }
    
    /**
     * Validate string length
     */
    public static ValidationResult validateLength(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            return new ValidationResult(false, 
                String.format("%s is too long (max %d characters)", fieldName, maxLength));
        }
        return new ValidationResult(true, "Valid");
    }
    
    /**
     * Validate department
     */
    public static ValidationResult validateDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return new ValidationResult(true, "Valid"); // Optional field
        }
        
        if (department.length() > 100) {
            return new ValidationResult(false, "Department name is too long (max 100 characters)");
        }
        
        if (!department.matches("^[a-zA-Z\\s&-]+$")) {
            return new ValidationResult(false, "Department contains invalid characters");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    /**
     * Validate phone number
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return new ValidationResult(true, "Valid"); // Optional field
        }
        
        // Remove common formatting characters
        String cleanPhone = phone.replaceAll("[\\s\\-().]", "");
        
        if (!cleanPhone.matches("^\\+?\\d{10,15}$")) {
            return new ValidationResult(false, "Invalid phone number format");
        }
        
        return new ValidationResult(true, "Valid");
    }
}
