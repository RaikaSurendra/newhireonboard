#!/bin/bash
# Master script to run all load and chaos tests

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="$PROJECT_ROOT/results"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create results directory
mkdir -p "$RESULTS_DIR"/{gatling,artillery,k6,chaos}

echo "=========================================="
echo "OnboardBuddy - Load & Chaos Testing Suite"
echo "=========================================="
echo ""

# Function to check if service is running
check_service() {
    local url=$1
    local name=$2
    
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|401"; then
        echo -e "${GREEN}✓${NC} $name is running"
        return 0
    else
        echo -e "${RED}✗${NC} $name is not running"
        return 1
    fi
}

# Pre-flight checks
echo "Running pre-flight checks..."
echo ""

if ! check_service "http://localhost:8080/api/health" "Backend API"; then
    echo -e "${RED}Error: Backend is not running. Please start it first.${NC}"
    exit 1
fi

echo ""
echo "All services are running!"
echo ""

# Menu
echo "Select test suite to run:"
echo "1) Gatling Load Tests (Java)"
echo "2) Artillery Load Tests (Node.js)"
echo "3) k6 Load Tests"
echo "4) All Load Tests"
echo "5) Chaos Tests (requires Toxiproxy)"
echo "6) Full Test Suite (Load + Chaos)"
echo "7) Quick Smoke Test"
echo ""
read -p "Enter choice [1-7]: " choice

case $choice in
    1)
        echo -e "${YELLOW}Running Gatling tests...${NC}"
        cd "$PROJECT_ROOT/../backend"
        mvn gatling:test
        echo -e "${GREEN}Gatling tests completed!${NC}"
        echo "Reports: backend/target/gatling/"
        ;;
    
    2)
        echo -e "${YELLOW}Running Artillery tests...${NC}"
        cd "$PROJECT_ROOT/artillery"
        npm install
        artillery run scenarios/api-load-test.yml --output "$RESULTS_DIR/artillery/api-test.json"
        artillery run scenarios/user-journey.yml --output "$RESULTS_DIR/artillery/user-journey.json"
        echo -e "${GREEN}Artillery tests completed!${NC}"
        echo "Results: $RESULTS_DIR/artillery/"
        ;;
    
    3)
        echo -e "${YELLOW}Running k6 tests...${NC}"
        cd "$PROJECT_ROOT/k6"
        k6 run --out json="$RESULTS_DIR/k6/api-load-test.json" scripts/api-load-test.js
        echo -e "${GREEN}k6 tests completed!${NC}"
        echo "Results: $RESULTS_DIR/k6/"
        ;;
    
    4)
        echo -e "${YELLOW}Running all load tests...${NC}"
        
        # Gatling
        echo "1/3 - Gatling tests..."
        cd "$PROJECT_ROOT/../backend"
        mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.BasicSimulation
        
        # Artillery
        echo "2/3 - Artillery tests..."
        cd "$PROJECT_ROOT/artillery"
        npm install
        artillery run scenarios/api-load-test.yml --output "$RESULTS_DIR/artillery/api-test.json"
        
        # k6
        echo "3/3 - k6 tests..."
        cd "$PROJECT_ROOT/k6"
        k6 run --out json="$RESULTS_DIR/k6/api-load-test.json" scripts/api-load-test.js
        
        echo -e "${GREEN}All load tests completed!${NC}"
        ;;
    
    5)
        echo -e "${YELLOW}Running chaos tests...${NC}"
        
        # Check if Toxiproxy is running
        if ! pgrep -x "toxiproxy-serv" > /dev/null; then
            echo -e "${YELLOW}Starting Toxiproxy...${NC}"
            cd "$PROJECT_ROOT/chaos"
            ./scripts/setup-toxiproxy.sh
            sleep 3
        fi
        
        # Get auth token
        echo "Getting authentication token..."
        TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"email":"admin@onboardbuddy.com","password":"Admin@123"}' \
            | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        
        if [ -z "$TOKEN" ]; then
            echo -e "${RED}Failed to get auth token${NC}"
            exit 1
        fi
        
        export AUTH_TOKEN=$TOKEN
        
        cd "$PROJECT_ROOT/chaos"
        ./scripts/run-chaos-tests.sh
        
        echo -e "${GREEN}Chaos tests completed!${NC}"
        echo "Results: $RESULTS_DIR/chaos/"
        ;;
    
    6)
        echo -e "${YELLOW}Running full test suite...${NC}"
        
        # Run load tests first
        echo "Phase 1: Load Testing"
        cd "$PROJECT_ROOT/../backend"
        mvn gatling:test -Dgatling.simulationClass=com.onboardbuddy.loadtest.BasicSimulation
        
        cd "$PROJECT_ROOT/artillery"
        npm install
        artillery run scenarios/api-load-test.yml --output "$RESULTS_DIR/artillery/api-test.json"
        
        # Wait before chaos tests
        echo "Waiting 60 seconds before chaos tests..."
        sleep 60
        
        # Run chaos tests
        echo "Phase 2: Chaos Testing"
        cd "$PROJECT_ROOT/chaos"
        ./scripts/run-chaos-tests.sh
        
        echo -e "${GREEN}Full test suite completed!${NC}"
        ;;
    
    7)
        echo -e "${YELLOW}Running quick smoke test...${NC}"
        
        # Quick k6 test with minimal load
        cd "$PROJECT_ROOT/k6"
        k6 run --vus 5 --duration 30s scripts/api-load-test.js
        
        echo -e "${GREEN}Smoke test completed!${NC}"
        ;;
    
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Results directory: $RESULTS_DIR"
echo ""
echo "To view Gatling reports:"
echo "  open backend/target/gatling/*/index.html"
echo ""
echo "To generate Artillery HTML report:"
echo "  artillery report $RESULTS_DIR/artillery/api-test.json"
echo ""
echo "=========================================="
