# Quick Reference Card

## 🚀 Getting Started (30 seconds)

```bash
./setup-env.sh    # Interactive setup
make quickstart   # Build and run everything
```

Access: http://localhost:3000  
Login: `admin@onboardbuddy.com` / `admin123`

## 📋 Common Commands

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make dev` | Start development servers |
| `make build` | Build application |
| `make test` | Run tests |
| `make clean` | Clean build artifacts |
| `make logs` | View application logs |
| `make status` | Check if services running |
| `make stop` | Stop all services |

## 🔧 Configuration

### Required Environment Variables
```bash
export JWT_SECRET=$(openssl rand -base64 64)
export DB_PASSWORD=your_password
```

### Configuration Priority
1. **Environment Variables** (highest)
2. External Config File (`-Dconfig.file=...`)
3. `application.properties` (lowest)

## 🔒 Security Features

- ✅ Rate Limiting: 60 req/min, 5 login attempts
- ✅ Password Policy: 8+ chars, upper, lower, digit, special
- ✅ Token Revocation: Logout invalidates JWT
- ✅ Input Validation: All inputs validated
- ✅ Request Limits: 1KB login, 2KB registration

## 🐛 Troubleshooting

### Application won't start
```bash
# Check if ports are in use
make status

# Stop conflicting services
make stop

# Check logs
make logs
```

### Database connection failed
```bash
# Verify MySQL is running
mysql -u root -p -e "SELECT 1"

# Reset database
make db-reset
```

### Build fails
```bash
# Clean and rebuild
make clean
make build

# Check Java version
java -version  # Should be 11+
```

## 📁 Project Structure

```
onboardBuddyApp/
├── backend/
│   ├── src/main/java/com/onboardbuddy/
│   │   ├── config/          # Configuration
│   │   ├── controllers/     # API endpoints
│   │   ├── filters/         # Request filters
│   │   ├── security/        # Security utilities
│   │   └── utils/           # Helper utilities
│   └── src/main/resources/
│       └── application.properties
├── frontend/
│   └── src/
│       ├── api/             # API clients
│       ├── components/      # React components
│       └── pages/           # Page components
├── database/
│   └── schema.sql           # Database schema
├── Makefile                 # Build automation
├── .env.example             # Config template
└── setup-env.sh             # Setup script
```

## 🔑 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/logout` - User logout

### Health
- `GET /api/health` - Health check

## 🧪 Testing

```bash
# Run all tests
make test

# Run backend tests only
make test-backend

# Run frontend tests only
make test-frontend

# Run load tests
make test-load
```

## 📊 Monitoring

```bash
# View logs
make logs

# View error logs only
make logs-error

# Check application status
make status
```

## 🔐 Security Checklist

Before deploying to production:

- [ ] Set strong `JWT_SECRET` (min 256 bits)
- [ ] Change `DB_PASSWORD` from default
- [ ] Set `APP_ENVIRONMENT=production`
- [ ] Review all `.env` settings
- [ ] Enable HTTPS
- [ ] Configure firewall rules
- [ ] Set up monitoring
- [ ] Run security audit
- [ ] Test rate limiting
- [ ] Verify input validation

## 💡 Tips & Tricks

### Development
```bash
# Auto-reload on changes
make dev

# Build without tests
cd backend && mvn package -DskipTests

# Clear all caches
make clean-all
```

### Production
```bash
# Build optimized
make build

# Run with custom config
java -Dconfig.file=/path/to/prod.properties -jar backend/target/onboard-buddy-1.0.0.jar

# Run in background
nohup make run > app.log 2>&1 &
```

### Database
```bash
# Backup database
make db-backup

# Reset database
make db-reset

# Run migrations
make db-migrate
```

## 📚 Documentation Links

- [Complete Fixes](FIXES_APPLIED.md)
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md)
- [Main README](README.md)
- [Environment Template](.env.example)

## 🆘 Getting Help

1. **Check logs:** `make logs`
2. **View status:** `make status`
3. **Read docs:** See links above
4. **Clean start:** `make clean && make build`

## 🎯 Common Workflows

### First Time Setup
```bash
git clone <repo>
cd onboardBuddyApp
./setup-env.sh
make quickstart
```

### Daily Development
```bash
make dev          # Start servers
# Make changes...
make test         # Run tests
make build        # Build
```

### Deployment
```bash
# Set production env vars
export JWT_SECRET=...
export DB_PASSWORD=...
export APP_ENVIRONMENT=production

make build
make run
```

### Debugging
```bash
make logs         # View logs
make status       # Check status
make stop         # Stop services
make clean        # Clean build
make build        # Rebuild
```

---

**Quick Help:** `make help`  
**Full Docs:** [FIXES_APPLIED.md](FIXES_APPLIED.md)  
**Version:** 1.0.0
