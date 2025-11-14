# 🚀 Build and Run Guide

## ✅ Prerequisites Completed
- ✓ Java 11 installed
- ✓ Node.js 18+ installed
- ✓ Maven installed
- ✓ MySQL installed and running
- ✓ Database created and schema loaded

## 📦 Install Dependencies

### Backend Dependencies
```bash
cd backend
mvn clean install
```

This will download all Java dependencies (Tomcat, MySQL connector, JWT, etc.)

### Frontend Dependencies
```bash
cd frontend
npm install
```

This will download all React dependencies (React, Material-UI, Redux, etc.)

## 🏃 Running the Application

### Option 1: Development Mode (Recommended for Development)

**Terminal 1 - Start Backend:**
```bash
cd backend
mvn compile exec:java -Dexec.mainClass="com.onboardbuddy.Application"
```

Backend will start on: **http://localhost:8080**

**Terminal 2 - Start Frontend:**
```bash
cd frontend
npm run dev
```

Frontend will start on: **http://localhost:3000**

### Option 2: Production Mode (Single JAR)

**Step 1: Build Frontend**
```bash
cd frontend
npm run build
```

This builds React app into `backend/src/main/webapp/`

**Step 2: Build Backend JAR**
```bash
cd backend
mvn clean package
```

This creates: `backend/target/onboard-buddy-1.0.0.jar`

**Step 3: Run the JAR**
```bash
java -jar backend/target/onboard-buddy-1.0.0.jar
```

Access application at: **http://localhost:8080**

## 🧪 Testing the Application

### 1. Health Check
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "application": "Onboard Buddy",
  "version": "1.0.0",
  "database": "UP",
  "timestamp": 1234567890
}
```

### 2. Frontend
Open browser: **http://localhost:3000** (dev mode) or **http://localhost:8080** (production)

You should see the Onboard Buddy welcome page.

## 🐛 Troubleshooting

### Backend won't start

**Issue: Port 8080 already in use**
```bash
# Find process
lsof -i :8080

# Kill it
kill -9 <PID>
```

**Issue: Database connection failed**
```bash
# Check MySQL is running
brew services list | grep mysql

# Start MySQL if needed
brew services start mysql

# Test connection
mysql -u appuser -papppassword onboard_buddy
```

**Issue: Maven build fails**
```bash
# Clean and rebuild
cd backend
mvn clean
mvn install -U
```

### Frontend won't start

**Issue: Dependencies not installed**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

**Issue: Port 3000 already in use**
```bash
# Kill process on port 3000
lsof -i :3000
kill -9 <PID>
```

**Issue: Build fails**
```bash
# Clear Vite cache
cd frontend
rm -rf node_modules/.vite
npm run dev
```

## 📝 Quick Commands Reference

### Backend
```bash
# Compile only
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Clean build
mvn clean package

# Skip tests
mvn package -DskipTests
```

### Frontend
```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

## 🔄 Development Workflow

1. **Start both servers** (backend + frontend in dev mode)
2. **Make changes** to code
3. **Hot reload** happens automatically:
   - Frontend: Vite hot reload
   - Backend: Restart manually or use IDE hot reload
4. **Test** your changes
5. **Commit** to git

## 🚢 Production Deployment

```bash
# 1. Build frontend
cd frontend && npm run build

# 2. Build backend (includes frontend)
cd ../backend && mvn clean package

# 3. Run JAR
java -jar target/onboard-buddy-1.0.0.jar

# Or with custom config
java -jar target/onboard-buddy-1.0.0.jar \
  -Dconfig.file=/path/to/application.properties
```

## 📊 Monitoring

### Logs
```bash
# Application logs
tail -f logs/onboard-buddy.log

# Error logs
tail -f logs/error.log
```

### Database
```bash
# Connect to database
mysql -u appuser -papppassword onboard_buddy

# Check tables
SHOW TABLES;

# Check users
SELECT * FROM users;
```

## 🎯 Next Steps

1. **Login** with default credentials:
   - Email: `admin@onboardbuddy.com`
   - Password: `admin123`

2. **Explore** the application

3. **Start developing** new features based on LLD_DOCUMENT.md

4. **Add** more servlets, services, and React components

---

**Happy Coding! 🎉**
