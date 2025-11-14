# Quick Start Guide - Load & Chaos Testing

## Prerequisites Installation

### macOS
```bash
# Install Gatling (via Maven - already configured)
# Install Artillery
npm install -g artillery

# Install k6
brew install k6

# Install Chaos Toolkit
pip3 install chaostoolkit chaostoolkit-kubernetes

# Install Toxiproxy
brew install toxiproxy

# Install MySQL connector for Python (for chaos tests)
pip3 install mysql-connector-python
```

### Verify Installations
```bash
artillery --version
k6 version
chaos --version
toxiproxy-server --version
```

## Running Load Tests

### 1. Gatling (Java Backend)

**Run all simulations:**
```bash
cd backend
mvn gatling:test
```

**Run specific simulation:**
```bash
cd backend
mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.BasicSimulation
mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.StressTestSimulation
mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.SpikeTestSimulation
```

**View reports:**
```bash
# Reports are generated at: backend/target/gatling/
open backend/target/gatling/basicsimulation-*/index.html
```

### 2. Artillery (API Testing)

**Install dependencies:**
```bash
cd load-testing/artillery
npm install
```

**Run tests:**
```bash
# API load test
npm run test:api

# User journey test
npm run test:user-journey

# Spike test
npm run test:spike

# Run all tests
npm run test:all
```

**Generate HTML report:**
```bash
artillery run scenarios/api-load-test.yml --output results/report.json
artillery report results/report.json
```

### 3. k6 (Lightweight Testing)

```bash
cd load-testing/k6

# Basic load test
k6 run scripts/api-load-test.js

# Stress test
k6 run scripts/stress-test.js

# Soak test (30 minutes)
k6 run scripts/soak-test.js

# With custom output
k6 run --out json=results/k6-results.json scripts/api-load-test.js
```

## Running Chaos Tests

### 1. Setup Toxiproxy

**Start Toxiproxy server:**
```bash
cd load-testing/chaos
chmod +x scripts/setup-toxiproxy.sh
./scripts/setup-toxiproxy.sh
```

**Or manually:**
```bash
# Start server
toxiproxy-server &

# Create proxies
toxiproxy-cli create mysql -l localhost:3307 -u localhost:3306
toxiproxy-cli create backend -l localhost:8081 -u localhost:8080

# List proxies
toxiproxy-cli list
```

### 2. Update Application Configuration

**For chaos testing, update your database connection to use Toxiproxy:**
```
# Instead of: localhost:3306
# Use: localhost:3307
```

### 3. Run Chaos Experiments

**Run individual experiment:**
```bash
cd load-testing/chaos

# Database latency
chaos run experiments/database-latency.json

# API failures
chaos run experiments/api-failure.json

# Network partition
chaos run experiments/network-partition.json

# Cascading failures
chaos run experiments/cascading-failure.json
```

**Run all experiments:**
```bash
cd load-testing/chaos
chmod +x scripts/run-chaos-tests.sh
./scripts/run-chaos-tests.sh
```

**With authentication token:**
```bash
# First, get auth token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@onboardbuddy.com","password":"Admin@123"}' \
  | jq -r '.data.token')

# Run experiment with token
AUTH_TOKEN=$TOKEN chaos run experiments/database-latency.json
```

## Test Scenarios Overview

### Load Testing Scenarios

| Test Type | Tool | Duration | Users | Purpose |
|-----------|------|----------|-------|---------|
| Basic Load | Gatling | 3 min | 100 | Baseline performance |
| Stress Test | Gatling/k6 | 10 min | 300+ | Find breaking point |
| Spike Test | Gatling/Artillery | 5 min | 1000 | Sudden traffic spikes |
| Soak Test | k6 | 30 min | 50 | Memory leaks, stability |
| User Journey | Artillery | 3 min | 10 | Realistic workflows |

### Chaos Testing Scenarios

| Experiment | Impact | Recovery Time | Purpose |
|------------|--------|---------------|---------|
| Database Latency | 1000ms delay | Immediate | Test timeout handling |
| API Failure | 503 errors | 10s | Test error handling |
| Network Partition | Connection timeout | 15s | Test resilience |
| Cascading Failure | Multiple issues | 20s | Test system degradation |

## Monitoring During Tests

### Application Metrics
```bash
# Watch application logs
tail -f backend/logs/application.log

# Monitor JVM
jstat -gc <pid> 1000

# Check database connections
mysql -u root -p -e "SHOW PROCESSLIST;"
```

### System Metrics
```bash
# CPU and Memory
top

# Network connections
netstat -an | grep 8080

# Database connections
mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"
```

## Interpreting Results

### Success Criteria

**Load Tests:**
- ✅ p95 response time < 500ms
- ✅ p99 response time < 1000ms
- ✅ Error rate < 0.1%
- ✅ Throughput > 100 req/s
- ✅ CPU usage < 80%
- ✅ Memory stable (no leaks)

**Chaos Tests:**
- ✅ System recovers automatically
- ✅ No data corruption
- ✅ Graceful degradation
- ✅ Error messages are clear
- ✅ Logs capture failures

### Common Issues

**High Response Times:**
- Check database query performance
- Review connection pool settings
- Look for N+1 queries
- Check for blocking operations

**Memory Issues:**
- Check for connection leaks
- Review object lifecycle
- Monitor garbage collection
- Look for large object allocations

**Database Bottlenecks:**
- Add indexes
- Optimize queries
- Increase connection pool
- Consider caching

## Cleanup

```bash
# Stop Toxiproxy
pkill toxiproxy-server

# Clear test results
rm -rf load-testing/results/*
rm -rf backend/target/gatling/*

# Reset database (if needed)
mysql -u root -p < database/schema.sql
```

## Next Steps

1. **Establish Baselines**: Run tests to establish performance baselines
2. **CI/CD Integration**: Add load tests to your pipeline
3. **Regular Testing**: Schedule weekly load tests
4. **Chaos in Staging**: Run chaos tests in staging environment
5. **Monitor Production**: Set up APM tools (New Relic, DataDog)
6. **Iterate**: Use results to optimize application

## Troubleshooting

### Gatling Issues
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx2g"
mvn gatling:test
```

### Artillery Issues
```bash
# Enable debug mode
DEBUG=* artillery run scenarios/api-load-test.yml
```

### Chaos Toolkit Issues
```bash
# Verify installation
chaos discover chaostoolkit-kubernetes

# Check experiment syntax
chaos validate experiments/database-latency.json
```

## Support

For issues or questions:
1. Check logs in `load-testing/results/`
2. Review application logs
3. Verify all services are running
4. Check network connectivity
