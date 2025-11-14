#!/bin/bash
# Setup test data for load and chaos testing

set -e

echo "=========================================="
echo "Setting up test data for load testing"
echo "=========================================="
echo ""

# Database credentials
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-onboard_buddy}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-password}

# Test users to create
declare -a TEST_USERS=(
    "admin@onboardbuddy.com:Admin@123:ADMIN"
    "hr@onboardbuddy.com:Hr@123:HR"
    "manager@onboardbuddy.com:Manager@123:MANAGER"
    "buddy1@onboardbuddy.com:Buddy@123:BUDDY"
    "buddy2@onboardbuddy.com:Buddy@123:BUDDY"
    "employee1@onboardbuddy.com:Employee@123:EMPLOYEE"
    "employee2@onboardbuddy.com:Employee@123:EMPLOYEE"
    "employee3@onboardbuddy.com:Employee@123:EMPLOYEE"
    "employee4@onboardbuddy.com:Employee@123:EMPLOYEE"
    "employee5@onboardbuddy.com:Employee@123:EMPLOYEE"
)

echo "Creating test users via API..."
echo ""

BASE_URL="http://localhost:8080/api"

# Function to create user
create_user() {
    local email=$1
    local password=$2
    local role=$3
    
    echo "Creating user: $email (Role: $role)"
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$email\",
            \"password\": \"$password\",
            \"firstName\": \"Test\",
            \"lastName\": \"User\",
            \"role\": \"$role\"
        }")
    
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        echo "✓ User created successfully"
    elif [ "$http_code" -eq 409 ]; then
        echo "⚠ User already exists"
    else
        echo "✗ Failed to create user (HTTP $http_code)"
    fi
    
    echo ""
}

# Create all test users
for user_data in "${TEST_USERS[@]}"; do
    IFS=':' read -r email password role <<< "$user_data"
    create_user "$email" "$password" "$role"
done

echo "=========================================="
echo "Test data setup completed!"
echo "=========================================="
echo ""
echo "Test users created:"
echo "  - admin@onboardbuddy.com (ADMIN)"
echo "  - hr@onboardbuddy.com (HR)"
echo "  - manager@onboardbuddy.com (MANAGER)"
echo "  - buddy1@onboardbuddy.com (BUDDY)"
echo "  - buddy2@onboardbuddy.com (BUDDY)"
echo "  - employee1-5@onboardbuddy.com (EMPLOYEE)"
echo ""
echo "All passwords: See respective user entries above"
echo ""
echo "You can now run load tests!"
