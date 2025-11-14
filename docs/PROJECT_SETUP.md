# Onboarding Buddy Application - Project Setup Guide

## Prerequisites

### Required Software
- **Java Development Kit (JDK) 11 or higher**
  ```bash
  java -version  # Should show 11 or higher
  ```
  Download: https://adoptium.net/

- **Node.js 18+ and npm**
  ```bash
  node -v  # Should show v18 or higher
  npm -v
  ```
  Download: https://nodejs.org/

- **Maven 3.6+** (if using Maven)
  ```bash
  mvn -v
  ```
  Download: https://maven.apache.org/download.cgi

- **MySQL 8.0+** or **PostgreSQL 13+**
  ```bash
  mysql --version
  ```
  Download: https://dev.mysql.com/downloads/mysql/

- **Git**
  ```bash
  git --version
  ```
  Download: https://git-scm.com/downloads

### Optional (for Docker deployment)
- **Docker Desktop**
  ```bash
  docker --version
  docker-compose --version
  ```
  Download: https://www.docker.com/products/docker-desktop

### Recommended IDEs
- **Backend**: IntelliJ IDEA or Eclipse
- **Frontend**: Visual Studio Code

---

## Project Structure

```
onboardBuddyApp/
├── backend/                          # Java backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── onboardbuddy/
│   │   │   │           ├── Application.java
│   │   │   │           ├── config/
│   │   │   │           ├── controllers/
│   │   │   │           ├── services/
│   │   │   │           ├── repositories/
│   │   │   │           ├── models/
│   │   │   │           ├── filters/
│   │   │   │           └── utils/
│   │   │   ├── resources/
│   │   │   │   ├── application.properties
│   │   │   │   └── logback.xml
│   │   │   └── webapp/               # React build output goes here
│   │   └── test/
│   │       └── java/
│   ├── pom.xml                       # Maven config
│   └── build.gradle                  # Gradle config (alternative)
│
├── frontend/                         # React frontend
│   ├── public/
│   │   ├── index.html
│   │   └── favicon.ico
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── store/
│   │   ├── hooks/
│   │   ├── utils/
│   │   ├── types/
│   │   ├── styles/
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── tailwind.config.js
│
├── database/
│   ├── schema.sql                    # Database schema
│   └── seed-data.sql                 # Sample data
│
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── nginx.conf
│
├── docs/
│   ├── HLD_DOCUMENT.md
│   ├── LLD_DOCUMENT.md
│   └── API_DOCUMENTATION.md
│
├── scripts/
│   ├── setup.sh                      # Setup script for Unix/Mac
│   ├── setup.bat                     # Setup script for Windows
│   └── deploy.sh                     # Deployment script
│
├── .gitignore
├── README.md
└── PROJECT_SETUP.md                  # This file
```

---

## Step-by-Step Setup

### Step 1: Clone/Create Project

```bash
# Create project directory
mkdir onboardBuddyApp
cd onboardBuddyApp

# Initialize git
git init
```

### Step 2: Setup Database

**Create Database:**
```bash
# Login to MySQL
mysql -u root -p

# Create database
CREATE DATABASE onboard_buddy;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'apppassword';
GRANT ALL PRIVILEGES ON onboard_buddy.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Run Schema:**
```bash
# Create database folder
mkdir -p database

# Run schema (we'll create this file next)
mysql -u appuser -p onboard_buddy < database/schema.sql
```

### Step 3: Setup Backend (Maven)

**Create Backend Structure:**
```bash
mkdir -p backend/src/main/java/com/onboardbuddy
mkdir -p backend/src/main/resources
mkdir -p backend/src/main/webapp
mkdir -p backend/src/test/java
```

**Create pom.xml:**
```bash
cd backend
# We'll create pom.xml file next
```

**Create Application.java:**
```bash
# Create main application class
# We'll create this file next
```

### Step 4: Setup Frontend (React + Vite)

```bash
# Go to project root
cd ..

# Create React app with Vite
npm create vite@latest frontend -- --template react-ts

# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Install additional packages
npm install @mui/material @emotion/react @emotion/styled
npm install @reduxjs/toolkit react-redux
npm install axios react-router-dom
npm install react-hook-form zod @hookform/resolvers
npm install lucide-react date-fns

# Install dev dependencies
npm install -D tailwindcss postcss autoprefixer
npm install -D @types/node

# Initialize Tailwind
npx tailwindcss init -p
```

### Step 5: Configure Build Integration

**Update vite.config.ts:**
```typescript
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
    outDir: '../backend/src/main/webapp',
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

### Step 6: Create Configuration Files

**Backend application.properties:**
```properties
# Database
db.url=jdbc:mysql://localhost:3306/onboard_buddy
db.username=appuser
db.password=apppassword
db.driver=com.mysql.cj.jdbc.Driver

# Server
server.port=8080

# JWT
jwt.secret=change-this-secret-key-in-production-min-256-bits
jwt.expiration=86400000
jwt.refreshExpiration=604800000

# CORS
cors.allowedOrigins=http://localhost:3000
```

**Create .gitignore:**
```
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
```

### Step 7: Build and Run

**Terminal 1 - Backend:**
```bash
cd backend

# Build
mvn clean package

# Run
java -jar target/onboard-buddy-1.0.0.jar
```

**Terminal 2 - Frontend (Development):**
```bash
cd frontend

# Run dev server
npm run dev

# Access at: http://localhost:3000
```

**Production Build:**
```bash
# Build frontend
cd frontend
npm run build

# Build backend (includes frontend)
cd ../backend
mvn clean package

# Run single JAR
java -jar target/onboard-buddy-1.0.0.jar

# Access at: http://localhost:8080
```

---

## Docker Setup (Alternative)

### Quick Start with Docker

```bash
# Build and run
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### Docker Files Required

1. **Dockerfile** (in project root)
2. **docker-compose.yml** (in project root)
3. **.env** file with environment variables

---

## Verification Checklist

### Backend Verification
- [ ] Java 11+ installed
- [ ] Maven/Gradle installed
- [ ] MySQL running
- [ ] Database created
- [ ] Backend compiles: `mvn clean compile`
- [ ] Tests pass: `mvn test`
- [ ] Backend runs: `mvn spring-boot:run` or `java -jar`
- [ ] Health endpoint works: `curl http://localhost:8080/api/health`

### Frontend Verification
- [ ] Node.js 18+ installed
- [ ] Dependencies installed: `npm install`
- [ ] Frontend compiles: `npm run build`
- [ ] Dev server runs: `npm run dev`
- [ ] Can access: http://localhost:3000
- [ ] API proxy works (check browser console)

### Database Verification
- [ ] MySQL service running
- [ ] Database `onboard_buddy` exists
- [ ] User `appuser` has permissions
- [ ] Schema tables created
- [ ] Can connect from backend

### Integration Verification
- [ ] Frontend can call backend APIs
- [ ] Authentication works
- [ ] CORS configured correctly
- [ ] WebSocket connection works (if implemented)

---

## Common Issues and Solutions

### Issue: Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Issue: Database Connection Failed
- Check MySQL is running: `sudo systemctl status mysql`
- Verify credentials in application.properties
- Check firewall settings

### Issue: Frontend Build Fails
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Issue: CORS Errors
- Check `cors.allowedOrigins` in application.properties
- Verify frontend proxy configuration in vite.config.ts

---

## Next Steps

1. **Review Documentation**
   - Read HLD_DOCUMENT.md for high-level architecture
   - Read LLD_DOCUMENT.md for detailed implementation specs

2. **Start Development**
   - Implement authentication module
   - Create database repositories
   - Build REST API endpoints
   - Develop React components

3. **Testing**
   - Write unit tests for services
   - Create integration tests for APIs
   - Add frontend component tests

4. **Deployment**
   - Configure production database
   - Set up CI/CD pipeline
   - Deploy to cloud or on-premise

---

## Useful Commands

### Development
```bash
# Backend hot reload (if using spring-boot-devtools)
mvn spring-boot:run

# Frontend hot reload
npm run dev

# Run tests
mvn test                    # Backend
npm test                    # Frontend
```

### Production
```bash
# Build everything
npm run build && mvn clean package

# Run production
java -jar backend/target/onboard-buddy-1.0.0.jar
```

### Docker
```bash
# Build image
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Clean everything
docker-compose down -v
```

---

## Support and Resources

- **Documentation**: See `/docs` folder
- **API Reference**: See API_DOCUMENTATION.md
- **Issues**: Create GitHub issues for bugs
- **Contributing**: See CONTRIBUTING.md

---

**Setup Complete!** 🎉

Your Onboarding Buddy Application is ready for development.
