# CI/CD Integration Guide

## Overview

Integrate load and chaos testing into your CI/CD pipeline to catch performance regressions early.

## GitHub Actions

### Load Testing Workflow

Create `.github/workflows/load-tests.yml`:

```yaml
name: Load Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    # Run every day at 2 AM
    - cron: '0 2 * * *'
  workflow_dispatch:

jobs:
  load-test:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: password
          MYSQL_DATABASE: onboard_buddy
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
          cache: maven
      
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Setup Database
        run: |
          mysql -h 127.0.0.1 -u root -ppassword onboard_buddy < database/schema.sql
      
      - name: Build Backend
        run: |
          cd backend
          mvn clean package -DskipTests
      
      - name: Start Application
        run: |
          cd backend
          java -jar target/onboard-buddy-1.0.0.jar &
          echo $! > app.pid
          sleep 30
      
      - name: Wait for Application
        run: |
          timeout 60 bash -c 'until curl -f http://localhost:8080/api/health; do sleep 2; done'
      
      - name: Setup Test Data
        run: |
          cd load-testing/scripts
          chmod +x setup-test-data.sh
          ./setup-test-data.sh
      
      - name: Run Gatling Tests
        run: |
          cd backend
          mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.BasicSimulation
      
      - name: Install Artillery
        run: npm install -g artillery
      
      - name: Run Artillery Tests
        run: |
          cd load-testing/artillery
          npm install
          artillery run scenarios/api-load-test.yml --output results.json
      
      - name: Upload Gatling Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: gatling-reports
          path: backend/target/gatling/
      
      - name: Upload Artillery Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: artillery-results
          path: load-testing/artillery/results.json
      
      - name: Check Performance Thresholds
        run: |
          # Parse results and fail if thresholds exceeded
          cd load-testing/scripts
          chmod +x check-thresholds.sh
          ./check-thresholds.sh
      
      - name: Stop Application
        if: always()
        run: |
          if [ -f backend/app.pid ]; then
            kill $(cat backend/app.pid) || true
          fi
      
      - name: Comment PR with Results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            // Read and parse test results
            // Post comment with summary
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '## Load Test Results\n\n✅ All tests passed!'
            });
```

### Chaos Testing Workflow

Create `.github/workflows/chaos-tests.yml`:

```yaml
name: Chaos Tests

on:
  schedule:
    # Run weekly on Sundays at 3 AM
    - cron: '0 3 * * 0'
  workflow_dispatch:

jobs:
  chaos-test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      
      - name: Install Chaos Toolkit
        run: |
          pip install chaostoolkit chaostoolkit-kubernetes
          pip install mysql-connector-python
      
      - name: Install Toxiproxy
        run: |
          wget -O toxiproxy.deb https://github.com/Shopify/toxiproxy/releases/download/v2.5.0/toxiproxy_2.5.0_amd64.deb
          sudo dpkg -i toxiproxy.deb
      
      - name: Start Services
        run: |
          # Start application and dependencies
          docker-compose up -d
          sleep 30
      
      - name: Setup Toxiproxy
        run: |
          cd load-testing/chaos/scripts
          chmod +x setup-toxiproxy.sh
          ./setup-toxiproxy.sh
      
      - name: Run Chaos Experiments
        run: |
          cd load-testing/chaos
          for experiment in experiments/*.json; do
            echo "Running $experiment"
            chaos run "$experiment" --journal-path "results/$(basename $experiment)"
          done
      
      - name: Upload Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: chaos-results
          path: load-testing/chaos/results/
      
      - name: Cleanup
        if: always()
        run: |
          docker-compose down
          pkill toxiproxy-server || true
```

## GitLab CI

Create `.gitlab-ci.yml`:

```yaml
stages:
  - build
  - test
  - load-test
  - chaos-test

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository
    - frontend/node_modules

build:
  stage: build
  image: maven:3.8-openjdk-11
  script:
    - cd backend
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - backend/target/*.jar

unit-tests:
  stage: test
  image: maven:3.8-openjdk-11
  script:
    - cd backend
    - mvn test

load-tests:
  stage: load-test
  image: maven:3.8-openjdk-11
  services:
    - mysql:8.0
  variables:
    MYSQL_ROOT_PASSWORD: password
    MYSQL_DATABASE: onboard_buddy
  before_script:
    - apt-get update && apt-get install -y curl nodejs npm
    - npm install -g artillery
  script:
    - mysql -h mysql -u root -p$MYSQL_ROOT_PASSWORD $MYSQL_DATABASE < database/schema.sql
    - cd backend && java -jar target/onboard-buddy-1.0.0.jar &
    - sleep 30
    - mvn gatling:test
    - cd ../load-testing/artillery
    - npm install
    - artillery run scenarios/api-load-test.yml
  artifacts:
    paths:
      - backend/target/gatling/
      - load-testing/artillery/results/
  only:
    - main
    - develop

chaos-tests:
  stage: chaos-test
  image: python:3.9
  before_script:
    - pip install chaostoolkit
  script:
    - cd load-testing/chaos
    - chmod +x scripts/run-chaos-tests.sh
    - ./scripts/run-chaos-tests.sh
  artifacts:
    paths:
      - load-testing/chaos/results/
  only:
    - schedules
  when: manual
```

## Jenkins Pipeline

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    tools {
        maven 'Maven 3.8'
        jdk 'JDK 11'
    }
    
    environment {
        MYSQL_ROOT_PASSWORD = credentials('mysql-root-password')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Start Services') {
            steps {
                sh '''
                    docker-compose up -d mysql
                    sleep 10
                    mysql -h localhost -u root -p${MYSQL_ROOT_PASSWORD} onboard_buddy < database/schema.sql
                    cd backend
                    java -jar target/onboard-buddy-1.0.0.jar &
                    echo $! > app.pid
                    sleep 30
                '''
            }
        }
        
        stage('Load Tests') {
            parallel {
                stage('Gatling') {
                    steps {
                        dir('backend') {
                            sh 'mvn gatling:test'
                        }
                    }
                }
                
                stage('Artillery') {
                    steps {
                        dir('load-testing/artillery') {
                            sh '''
                                npm install
                                artillery run scenarios/api-load-test.yml --output results.json
                            '''
                        }
                    }
                }
                
                stage('k6') {
                    steps {
                        dir('load-testing/k6') {
                            sh 'k6 run scripts/api-load-test.js'
                        }
                    }
                }
            }
        }
        
        stage('Performance Analysis') {
            steps {
                script {
                    // Parse results and check thresholds
                    def gatlingReport = readFile('backend/target/gatling/*/js/stats.json')
                    // Implement threshold checks
                }
            }
        }
        
        stage('Chaos Tests') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    cd load-testing/chaos
                    chmod +x scripts/run-chaos-tests.sh
                    ./scripts/run-chaos-tests.sh
                '''
            }
        }
    }
    
    post {
        always {
            sh '''
                if [ -f backend/app.pid ]; then
                    kill $(cat backend/app.pid) || true
                fi
                docker-compose down
            '''
            
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'backend/target/gatling',
                reportFiles: 'index.html',
                reportName: 'Gatling Report'
            ])
            
            archiveArtifacts artifacts: 'load-testing/**/results/**/*', allowEmptyArchive: true
        }
        
        success {
            echo 'Load tests passed!'
        }
        
        failure {
            echo 'Load tests failed!'
            // Send notification
        }
    }
}
```

## Performance Thresholds Script

Create `load-testing/scripts/check-thresholds.sh`:

```bash
#!/bin/bash
# Check if performance thresholds are met

set -e

GATLING_STATS="../../backend/target/gatling/*/js/stats.json"
ARTILLERY_RESULTS="../artillery/results.json"

echo "Checking performance thresholds..."

# Check Gatling results
if [ -f $GATLING_STATS ]; then
    p95=$(jq '.contents.meanResponseTime.total.percentile3' $GATLING_STATS)
    error_rate=$(jq '.contents.numberOfRequests.ko / .contents.numberOfRequests.total * 100' $GATLING_STATS)
    
    echo "Gatling Results:"
    echo "  P95 Response Time: ${p95}ms"
    echo "  Error Rate: ${error_rate}%"
    
    if (( $(echo "$p95 > 1000" | bc -l) )); then
        echo "❌ P95 response time exceeds threshold (1000ms)"
        exit 1
    fi
    
    if (( $(echo "$error_rate > 1" | bc -l) )); then
        echo "❌ Error rate exceeds threshold (1%)"
        exit 1
    fi
fi

# Check Artillery results
if [ -f $ARTILLERY_RESULTS ]; then
    echo "Artillery results found"
    # Parse and check Artillery thresholds
fi

echo "✅ All performance thresholds met!"
```

## Best Practices

### 1. Test Environment
- Use dedicated test environment
- Match production configuration
- Isolate from other tests
- Clean up after tests

### 2. Test Data
- Use realistic test data
- Reset data between runs
- Don't use production data
- Version control test data

### 3. Scheduling
- Run load tests on every merge to main
- Run chaos tests weekly
- Run soak tests nightly
- Run stress tests before releases

### 4. Reporting
- Archive test results
- Track trends over time
- Alert on threshold violations
- Share reports with team

### 5. Thresholds
- Set realistic thresholds
- Adjust based on requirements
- Monitor trends, not just absolutes
- Review and update regularly

## Notifications

### Slack Integration

```yaml
- name: Notify Slack
  uses: 8398a7/action-slack@v3
  if: always()
  with:
    status: ${{ job.status }}
    text: 'Load test results available'
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Email Notifications

```yaml
- name: Send Email
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 465
    username: ${{ secrets.EMAIL_USERNAME }}
    password: ${{ secrets.EMAIL_PASSWORD }}
    subject: Load Test Results
    body: Check the attached reports
    to: team@example.com
    from: ci@example.com
    attachments: backend/target/gatling/*/index.html
```

## Troubleshooting CI/CD

### Common Issues

1. **Timeout**: Increase wait times for service startup
2. **Resource Limits**: Adjust CI runner resources
3. **Flaky Tests**: Add retry logic
4. **Network Issues**: Use service discovery
5. **Cleanup**: Always cleanup in post steps

### Debug Mode

```yaml
- name: Debug
  if: failure()
  run: |
    docker-compose logs
    cat backend/logs/application.log
    netstat -an | grep 8080
```

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitLab CI Documentation](https://docs.gitlab.com/ee/ci/)
- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
