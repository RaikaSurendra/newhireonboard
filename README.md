# Onboarding Buddy Application

A comprehensive onboarding management system with buddy matching, task management, and progress tracking.

## 🚀 Quick Start

### Prerequisites
- Java 11+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+
- Make (optional, for using Makefile)

### First Time Setup

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env and set your JWT_SECRET and DB_PASSWORD

# 2. Quick setup (with Make)
make quickstart

# OR Manual setup:
# Install dependencies
make install  # or: cd backend && mvn install && cd ../frontend && npm install

# Setup database
make db-setup  # or: mysql -u root -p < database/schema.sql

# Build application
make build  # or: cd backend && mvn clean package && cd ../frontend && npm run build

# Run application
make run  # or: java -jar backend/target/onboard-buddy-1.0.0.jar
```

### Development Mode

```bash
# Start both backend and frontend in development mode
make dev

# Or manually in separate terminals:
# Terminal 1 - Backend
cd backend && mvn exec:java -Dexec.mainClass="com.onboardbuddy.Application"

# Terminal 2 - Frontend
cd frontend && npm run dev
```

**Access the application:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Default credentials: `admin@onboardbuddy.com` / `admin123`

### Using the Makefile

```bash
make help          # Show all available commands
make install       # Install dependencies
make build         # Build application
make test          # Run tests
make clean         # Clean build artifacts
make db-setup      # Setup database
make logs          # View application logs
make status        # Check if services are running
make stop          # Stop all services
```

## Testing

### Load Testing & Chaos Engineering

Comprehensive load testing and chaos engineering setup included:

```bash
# Quick smoke test
cd load-testing/scripts
./run-all-tests.sh

# Run Gatling load tests
cd backend
mvn gatling:test

# Run Artillery tests
cd load-testing/artillery
npm install
artillery run scenarios/api-load-test.yml

# Run k6 tests
cd load-testing/k6
k6 run scripts/api-load-test.js

# Run chaos experiments
cd load-testing/chaos
chaos run experiments/database-latency.json
```

**Testing Documentation:**
- [Quick Start Guide](load-testing/QUICK_START.md)
- [Monitoring Guide](load-testing/MONITORING.md)
- [CI/CD Integration](load-testing/CI_CD_INTEGRATION.md)
- [Full Documentation](load-testing/README.md)

## Documentation
- [Installation Guide](docs/INSTALLATION_GUIDE.md)
- [Project Setup](docs/PROJECT_SETUP.md)
- [Build and Run Guide](docs/BUILD_AND_RUN.md)
- [High-Level Design](docs/HLD_DOCUMENT.md)
- [Low-Level Design](docs/LLD_DOCUMENT.md)
- **[Security Fixes Applied](FIXES_APPLIED.md)** ⭐ NEW

## 🔒 Security Features

This application includes comprehensive security features:

- ✅ **Environment-based Configuration** - Secrets externalized via environment variables
- ✅ **Input Validation** - All user inputs validated against security policies
- ✅ **Rate Limiting** - Protection against brute force and DoS attacks
- ✅ **Password Policy Enforcement** - Strong password requirements
- ✅ **JWT Token Revocation** - Proper logout with token blacklisting
- ✅ **Request Size Limits** - Protection against large payload attacks
- ✅ **Sanitized Logging** - No sensitive data in logs
- ✅ **Graceful Shutdown** - Proper resource cleanup

See [FIXES_APPLIED.md](FIXES_APPLIED.md) for complete details on all security improvements.

## 🔧 Configuration

The application uses a three-tier configuration system:
1. **Default** - `application.properties` (committed to repo)
2. **External File** - Specified via `-Dconfig.file=path/to/config.properties`
3. **Environment Variables** - Highest priority (recommended for production)

**Critical Environment Variables:**
```bash
export JWT_SECRET=$(openssl rand -base64 64)  # Required: min 256 bits
export DB_PASSWORD=your_secure_password       # Required
export DB_URL=jdbc:mysql://localhost:3306/onboard_buddy
export RATE_LIMIT_ENABLED=true
export RATE_LIMIT_LOGIN_ATTEMPTS=5
```

See `.env.example` for all available configuration options.

## License
MIT
