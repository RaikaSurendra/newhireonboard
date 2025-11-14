# Load Testing & Chaos Testing

This directory contains load testing and chaos testing configurations for the OnboardBuddy application.

## Tools Used

### Load Testing
- **Gatling**: Java-based load testing for backend APIs
- **Artillery**: Node.js-based load testing for frontend and API endpoints
- **k6**: Alternative lightweight load testing tool

### Chaos Testing
- **Chaos Toolkit**: Chaos engineering experiments
- **Toxiproxy**: Network chaos (latency, timeouts, connection failures)

## Directory Structure

```
load-testing/
├── gatling/              # Gatling load tests for backend
│   ├── simulations/      # Test scenarios
│   └── resources/        # Test data
├── artillery/            # Artillery load tests
│   ├── scenarios/        # Test scenarios
│   └── data/            # Test data files
├── k6/                  # k6 load tests
│   └── scripts/         # Test scripts
├── chaos/               # Chaos testing experiments
│   ├── experiments/     # Chaos experiments
│   └── toxiproxy/      # Network chaos configs
└── results/            # Test results output

```

## Quick Start

### Prerequisites
```bash
# Install Gatling (via Maven - already configured in pom.xml)
# Install Artillery
npm install -g artillery

# Install k6 (macOS)
brew install k6

# Install Chaos Toolkit
pip3 install chaostoolkit chaostoolkit-kubernetes

# Install Toxiproxy
brew install toxiproxy
```

### Running Load Tests

#### Gatling (Backend)
```bash
cd backend
mvn gatling:test
```

#### Artillery (API/Frontend)
```bash
cd load-testing/artillery
artillery run scenarios/api-load-test.yml
artillery run scenarios/user-journey.yml
```

#### k6
```bash
cd load-testing/k6
k6 run scripts/api-load-test.js
```

### Running Chaos Tests

#### Start Toxiproxy
```bash
toxiproxy-server
```

#### Run Chaos Experiments
```bash
cd load-testing/chaos
chaos run experiments/database-latency.json
chaos run experiments/api-failure.json
chaos run experiments/network-partition.json
```

## Test Scenarios

### Load Testing Scenarios

1. **Authentication Flow** - Login/logout with token refresh
2. **User Journey** - Complete onboarding workflow
3. **CRUD Operations** - Create, read, update, delete operations
4. **Concurrent Users** - Multiple users performing various actions
5. **Spike Testing** - Sudden traffic spikes
6. **Stress Testing** - Push system to limits
7. **Soak Testing** - Extended duration testing

### Chaos Testing Scenarios

1. **Database Latency** - Simulate slow database responses
2. **API Failures** - Random API endpoint failures
3. **Network Partition** - Simulate network splits
4. **Resource Exhaustion** - CPU/Memory pressure
5. **Cascading Failures** - Multiple component failures
6. **Timeout Scenarios** - Connection timeouts

## Performance Metrics

### Key Metrics to Monitor
- Response time (p50, p95, p99)
- Throughput (requests/second)
- Error rate
- Concurrent users
- Resource utilization (CPU, Memory, DB connections)

### Acceptance Criteria
- p95 response time < 500ms for API calls
- p99 response time < 1000ms
- Error rate < 0.1%
- Support 1000 concurrent users
- Database connection pool utilization < 80%

## Reports

Test results are saved in the `results/` directory:
- Gatling: HTML reports in `backend/target/gatling/`
- Artillery: JSON/HTML reports in `load-testing/results/artillery/`
- k6: JSON reports in `load-testing/results/k6/`
- Chaos: JSON logs in `load-testing/results/chaos/`

## CI/CD Integration

Load tests can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run Load Tests
  run: |
    cd backend && mvn gatling:test
    cd load-testing/artillery && artillery run scenarios/api-load-test.yml
```

## Best Practices

1. **Start Small**: Begin with low load and gradually increase
2. **Monitor Resources**: Watch CPU, memory, database connections
3. **Realistic Data**: Use production-like test data
4. **Test in Stages**: Smoke → Load → Stress → Soak
5. **Baseline Metrics**: Establish performance baselines
6. **Chaos in Non-Prod**: Never run chaos tests in production
7. **Gradual Rollout**: Increase chaos intensity gradually

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure backend is running on correct port
2. **Authentication Failures**: Check token generation in test data
3. **Database Errors**: Verify database is accessible and has test data
4. **Memory Issues**: Increase JVM heap size for Gatling tests

### Debug Mode

```bash
# Gatling with debug logging
mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.BasicSimulation -Dlogback.configurationFile=logback-debug.xml

# Artillery with debug
DEBUG=* artillery run scenarios/api-load-test.yml

# k6 with verbose output
k6 run --verbose scripts/api-load-test.js
```

## Resources

- [Gatling Documentation](https://gatling.io/docs/)
- [Artillery Documentation](https://www.artillery.io/docs)
- [k6 Documentation](https://k6.io/docs/)
- [Chaos Toolkit Documentation](https://chaostoolkit.org/)
- [Toxiproxy Documentation](https://github.com/Shopify/toxiproxy)
