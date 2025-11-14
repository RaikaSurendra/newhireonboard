# Docker Deployment Guide

This guide explains how to deploy the OnboardBuddy application using Docker and Docker Compose.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB RAM minimum
- 10GB disk space

## Quick Start

### 1. Setup Environment

```bash
# Copy environment template
cp .env.docker .env

# Edit .env and set your values (IMPORTANT!)
nano .env

# Generate a secure JWT secret
openssl rand -base64 64
# Copy the output and set it as JWT_SECRET in .env
```

### 2. Build and Run

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

### 3. Access Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Database**: localhost:3306

**Default Credentials**: `admin@onboardbuddy.com` / `admin123`

## Docker Commands

### Basic Operations

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart services
docker-compose restart

# View logs
docker-compose logs -f [service_name]

# Check status
docker-compose ps

# Execute commands in container
docker-compose exec backend sh
docker-compose exec frontend sh
docker-compose exec database mysql -u appuser -p
```

### Building

```bash
# Build all images
docker-compose build

# Build specific service
docker-compose build backend
docker-compose build frontend

# Build without cache
docker-compose build --no-cache

# Pull latest images
docker-compose pull
```

### Maintenance

```bash
# Remove stopped containers
docker-compose rm

# Remove volumes (WARNING: deletes data!)
docker-compose down -v

# Prune unused images
docker image prune -a

# View resource usage
docker stats
```

## Service Details

### Database (MySQL)
- **Container**: onboard-buddy-db
- **Port**: 3306
- **Volume**: mysql_data (persistent)
- **Health Check**: mysqladmin ping

### Backend (Java/Spring)
- **Container**: onboard-buddy-backend
- **Port**: 8080
- **Health Check**: /api/health endpoint
- **Depends on**: database

### Frontend (React/Nginx)
- **Container**: onboard-buddy-frontend
- **Port**: 3000
- **Health Check**: HTTP GET /
- **Depends on**: backend

## Configuration

### Environment Variables

Key environment variables in `.env`:

```bash
# Database
DB_ROOT_PASSWORD=your_root_password
DB_PASSWORD=your_app_password

# JWT (CRITICAL - Change in production!)
JWT_SECRET=your-256-bit-secret-key

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Security
CSRF_ENABLED=false
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS_PER_MINUTE=60

# Application
APP_ENVIRONMENT=production
```

### Custom Configuration

To use custom application.properties:

```bash
# Mount custom config
docker-compose run -v ./custom.properties:/app/config/application.properties backend
```

## Production Deployment

### 1. Security Hardening

```bash
# Generate strong JWT secret
JWT_SECRET=$(openssl rand -base64 64)

# Set strong database passwords
DB_ROOT_PASSWORD=$(openssl rand -base64 32)
DB_PASSWORD=$(openssl rand -base64 32)

# Enable CSRF protection
CSRF_ENABLED=true

# Restrict CORS origins
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### 2. Use Docker Secrets (Recommended)

```yaml
# docker-compose.prod.yml
services:
  backend:
    secrets:
      - db_password
      - jwt_secret
    environment:
      DB_PASSWORD_FILE: /run/secrets/db_password
      JWT_SECRET_FILE: /run/secrets/jwt_secret

secrets:
  db_password:
    file: ./secrets/db_password.txt
  jwt_secret:
    file: ./secrets/jwt_secret.txt
```

### 3. SSL/TLS Configuration

Add nginx SSL configuration:

```nginx
server {
    listen 443 ssl http2;
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    # ... rest of config
}
```

### 4. Resource Limits

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

## Monitoring

### Health Checks

```bash
# Check all services health
docker-compose ps

# Backend health
curl http://localhost:8080/api/health

# Frontend health
curl http://localhost:3000/health

# Database health
docker-compose exec database mysqladmin ping -u appuser -p
```

### Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend

# Since timestamp
docker-compose logs --since 2024-01-01T00:00:00 backend
```

### Metrics

```bash
# Resource usage
docker stats

# Disk usage
docker system df

# Network inspection
docker network inspect onboard-buddy_onboard-network
```

## Backup and Restore

### Database Backup

```bash
# Backup database
docker-compose exec database mysqldump -u appuser -p onboard_buddy > backup.sql

# Restore database
docker-compose exec -T database mysql -u appuser -p onboard_buddy < backup.sql
```

### Volume Backup

```bash
# Backup volume
docker run --rm -v onboard-buddy_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz /data

# Restore volume
docker run --rm -v onboard-buddy_mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-backup.tar.gz -C /
```

## Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Find process using port
lsof -i :8080
lsof -i :3000
lsof -i :3306

# Kill process or change port in docker-compose.yml
```

#### Database Connection Failed
```bash
# Check database is running
docker-compose ps database

# Check logs
docker-compose logs database

# Verify credentials
docker-compose exec database mysql -u appuser -p
```

#### Backend Not Starting
```bash
# Check logs
docker-compose logs backend

# Verify database is healthy
docker-compose ps

# Rebuild image
docker-compose build --no-cache backend
```

#### Frontend Not Loading
```bash
# Check nginx logs
docker-compose logs frontend

# Verify backend is accessible
curl http://localhost:8080/api/health

# Check browser console for errors
```

### Debug Mode

```bash
# Run with debug output
docker-compose up

# Execute shell in container
docker-compose exec backend sh
docker-compose exec frontend sh

# Check environment variables
docker-compose exec backend env
```

## Scaling

### Horizontal Scaling

```bash
# Scale backend instances
docker-compose up -d --scale backend=3

# Use load balancer (nginx/traefik)
# Add to docker-compose.yml
```

### Vertical Scaling

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 4G
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy to Docker

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Build images
        run: docker-compose build
      
      - name: Run tests
        run: docker-compose run backend mvn test
      
      - name: Deploy
        run: docker-compose up -d
```

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Security Guidelines](https://docs.docker.com/engine/security/)
