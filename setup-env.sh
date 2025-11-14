#!/bin/bash

# Onboard Buddy - Environment Setup Script
# This script helps you set up your environment configuration

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Onboard Buddy - Environment Setup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if .env already exists
if [ -f ".env" ]; then
    echo -e "${YELLOW}⚠️  .env file already exists!${NC}"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}Setup cancelled. Existing .env file preserved.${NC}"
        exit 0
    fi
fi

# Copy template
echo -e "${BLUE}Creating .env file from template...${NC}"
cp .env.example .env

# Generate JWT secret
echo -e "${BLUE}Generating secure JWT secret...${NC}"
if command -v openssl &> /dev/null; then
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    # Escape special characters for sed
    JWT_SECRET_ESCAPED=$(echo "$JWT_SECRET" | sed 's/[\/&]/\\&/g')
    
    # Update .env file with generated secret
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s/JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET_ESCAPED/" .env
    else
        # Linux
        sed -i "s/JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET_ESCAPED/" .env
    fi
    echo -e "${GREEN}✓ JWT secret generated and saved${NC}"
else
    echo -e "${YELLOW}⚠️  openssl not found. Please manually set JWT_SECRET in .env${NC}"
fi

# Prompt for database password
echo ""
echo -e "${BLUE}Database Configuration${NC}"
read -sp "Enter database password (or press Enter to use default 'apppassword'): " DB_PASSWORD
echo ""

if [ ! -z "$DB_PASSWORD" ]; then
    DB_PASSWORD_ESCAPED=$(echo "$DB_PASSWORD" | sed 's/[\/&]/\\&/g')
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/DB_PASSWORD=.*/DB_PASSWORD=$DB_PASSWORD_ESCAPED/" .env
    else
        sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=$DB_PASSWORD_ESCAPED/" .env
    fi
    echo -e "${GREEN}✓ Database password updated${NC}"
else
    echo -e "${YELLOW}⚠️  Using default database password. Change this for production!${NC}"
fi

# Prompt for environment
echo ""
echo -e "${BLUE}Application Environment${NC}"
echo "1) development (default)"
echo "2) production"
read -p "Select environment [1-2]: " ENV_CHOICE

case $ENV_CHOICE in
    2)
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/APP_ENVIRONMENT=.*/APP_ENVIRONMENT=production/" .env
        else
            sed -i "s/APP_ENVIRONMENT=.*/APP_ENVIRONMENT=production/" .env
        fi
        echo -e "${GREEN}✓ Environment set to production${NC}"
        echo -e "${RED}⚠️  IMPORTANT: Review all settings in .env for production use!${NC}"
        ;;
    *)
        echo -e "${GREEN}✓ Environment set to development${NC}"
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ Environment setup complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. Review and customize .env file if needed"
echo "2. Run: ${GREEN}make install${NC} to install dependencies"
echo "3. Run: ${GREEN}make db-setup${NC} to setup database"
echo "4. Run: ${GREEN}make build${NC} to build the application"
echo "5. Run: ${GREEN}make run${NC} to start the application"
echo ""
echo "Or use: ${GREEN}make quickstart${NC} to do all steps at once"
echo ""
echo -e "${YELLOW}⚠️  Remember: Never commit your .env file to version control!${NC}"
echo ""
