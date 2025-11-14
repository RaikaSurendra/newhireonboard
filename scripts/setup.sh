#!/bin/bash

# Onboarding Buddy Application - Setup Script
# For Unix/Linux/MacOS

set -e  # Exit on error

echo "🚀 Onboarding Buddy Application - Setup Script"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check prerequisites
echo "Checking prerequisites..."
echo ""

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 11 ]; then
        print_success "Java $JAVA_VERSION found"
    else
        print_error "Java 11 or higher required. Found Java $JAVA_VERSION"
        exit 1
    fi
else
    print_error "Java not found. Please install Java 11 or higher"
    exit 1
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        print_success "Node.js v$NODE_VERSION found"
    else
        print_warning "Node.js 18+ recommended. Found v$NODE_VERSION"
    fi
else
    print_error "Node.js not found. Please install Node.js 18 or higher"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    print_success "Maven found"
else
    print_warning "Maven not found. Will need to install Maven or use Gradle"
fi

# Check MySQL
if command -v mysql &> /dev/null; then
    print_success "MySQL found"
else
    print_warning "MySQL not found. Please install MySQL 8.0+"
fi

echo ""
echo "================================================"
echo "Setting up project structure..."
echo ""

# Create directory structure
mkdir -p backend/src/main/java/com/onboardbuddy/{config,controllers,services,repositories,models,filters,utils}
mkdir -p backend/src/main/resources
mkdir -p backend/src/main/webapp
mkdir -p backend/src/test/java

mkdir -p frontend/src/{api,components,pages,store,hooks,utils,types,styles}
mkdir -p frontend/public

mkdir -p database
mkdir -p docker
mkdir -p docs
mkdir -p logs

print_success "Project structure created"

# Setup database
echo ""
echo "================================================"
echo "Database Setup"
echo ""

read -p "Do you want to setup the database now? (y/n): " setup_db

if [ "$setup_db" = "y" ] || [ "$setup_db" = "Y" ]; then
    read -p "MySQL root password: " -s mysql_password
    echo ""
    
    echo "Creating database..."
    mysql -u root -p"$mysql_password" <<EOF
CREATE DATABASE IF NOT EXISTS onboard_buddy;
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'apppassword';
GRANT ALL PRIVILEGES ON onboard_buddy.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
EOF
    
    if [ $? -eq 0 ]; then
        print_success "Database created"
        
        if [ -f "database/schema.sql" ]; then
            echo "Running schema..."
            mysql -u appuser -papppassword onboard_buddy < database/schema.sql
            print_success "Schema created"
        fi
    else
        print_error "Database creation failed"
    fi
fi

# Setup backend
echo ""
echo "================================================"
echo "Backend Setup"
echo ""

if [ -f "backend/pom.xml" ]; then
    echo "Installing backend dependencies..."
    cd backend
    mvn clean install -DskipTests
    cd ..
    print_success "Backend dependencies installed"
else
    print_warning "pom.xml not found. Please create Maven configuration"
fi

# Setup frontend
echo ""
echo "================================================"
echo "Frontend Setup"
echo ""

if [ -f "frontend/package.json" ]; then
    echo "Installing frontend dependencies..."
    cd frontend
    npm install
    cd ..
    print_success "Frontend dependencies installed"
else
    print_warning "package.json not found. Please initialize React project"
fi

# Create .gitignore
echo ""
echo "Creating .gitignore..."
cat > .gitignore <<'EOF'
# Java
*.class
*.jar
*.war
target/
build/
.gradle/

# Node
node_modules/
dist/
build/
.env.local

# IDE
.idea/
.vscode/
*.iml
.DS_Store

# Logs
logs/
*.log

# Database
*.db
*.sqlite

# Environment
.env
application-local.properties
EOF

print_success ".gitignore created"

# Create README
echo ""
echo "Creating README.md..."
cat > README.md <<'EOF'
# Onboarding Buddy Application

A comprehensive onboarding management system with buddy matching, task management, and progress tracking.

## Quick Start

### Prerequisites
- Java 11+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+

### Setup
```bash
# Run setup script
./scripts/setup.sh

# Or manually:
# 1. Setup database
mysql -u root -p < database/schema.sql

# 2. Build backend
cd backend && mvn clean package

# 3. Build frontend
cd frontend && npm install && npm run build

# 4. Run application
java -jar backend/target/onboard-buddy-1.0.0.jar
```

### Development
```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm run dev
```

Access the application at: http://localhost:3000

## Documentation
- [Project Setup](PROJECT_SETUP.md)
- [High-Level Design](docs/HLD_DOCUMENT.md)
- [Low-Level Design](docs/LLD_DOCUMENT.md)

## License
MIT
EOF

print_success "README.md created"

# Final message
echo ""
echo "================================================"
echo -e "${GREEN}✓ Setup Complete!${NC}"
echo "================================================"
echo ""
echo "Next steps:"
echo "1. Review PROJECT_SETUP.md for detailed instructions"
echo "2. Configure backend/src/main/resources/application.properties"
echo "3. Build and run the application"
echo ""
echo "Development:"
echo "  Backend:  cd backend && mvn spring-boot:run"
echo "  Frontend: cd frontend && npm run dev"
echo ""
echo "Production:"
echo "  Build:    npm run build && mvn clean package"
echo "  Run:      java -jar backend/target/onboard-buddy-1.0.0.jar"
echo ""
echo "Happy coding! 🎉"
