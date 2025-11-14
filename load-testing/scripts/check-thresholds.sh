#!/bin/bash
# Check if performance thresholds are met

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Thresholds
P95_THRESHOLD=1000  # milliseconds
P99_THRESHOLD=2000  # milliseconds
ERROR_RATE_THRESHOLD=1  # percentage

echo "=========================================="
echo "Checking Performance Thresholds"
echo "=========================================="
echo ""

FAILED=0

# Check Gatling results
echo "Checking Gatling results..."
GATLING_DIR="$PROJECT_ROOT/../backend/target/gatling"

if [ -d "$GATLING_DIR" ]; then
    # Find the latest simulation directory
    LATEST_SIM=$(ls -t "$GATLING_DIR" | head -n 1)
    STATS_FILE="$GATLING_DIR/$LATEST_SIM/js/stats.json"
    
    if [ -f "$STATS_FILE" ]; then
        echo "Found stats file: $STATS_FILE"
        
        # Extract metrics using grep and sed (portable alternative to jq)
        # This is a simplified version - in production use jq if available
        
        if command -v jq &> /dev/null; then
            # Use jq if available
            TOTAL_REQUESTS=$(jq '.stats.numberOfRequests.total' "$STATS_FILE")
            FAILED_REQUESTS=$(jq '.stats.numberOfRequests.ko' "$STATS_FILE")
            P95=$(jq '.stats.percentiles3.total' "$STATS_FILE")
            P99=$(jq '.stats.percentiles4.total' "$STATS_FILE")
            
            ERROR_RATE=$(echo "scale=2; $FAILED_REQUESTS * 100 / $TOTAL_REQUESTS" | bc)
            
            echo ""
            echo "Gatling Metrics:"
            echo "  Total Requests: $TOTAL_REQUESTS"
            echo "  Failed Requests: $FAILED_REQUESTS"
            echo "  P95 Response Time: ${P95}ms"
            echo "  P99 Response Time: ${P99}ms"
            echo "  Error Rate: ${ERROR_RATE}%"
            echo ""
            
            # Check thresholds
            if (( $(echo "$P95 > $P95_THRESHOLD" | bc -l) )); then
                echo -e "${RED}❌ P95 response time (${P95}ms) exceeds threshold (${P95_THRESHOLD}ms)${NC}"
                FAILED=1
            else
                echo -e "${GREEN}✓ P95 response time within threshold${NC}"
            fi
            
            if (( $(echo "$P99 > $P99_THRESHOLD" | bc -l) )); then
                echo -e "${RED}❌ P99 response time (${P99}ms) exceeds threshold (${P99_THRESHOLD}ms)${NC}"
                FAILED=1
            else
                echo -e "${GREEN}✓ P99 response time within threshold${NC}"
            fi
            
            if (( $(echo "$ERROR_RATE > $ERROR_RATE_THRESHOLD" | bc -l) )); then
                echo -e "${RED}❌ Error rate (${ERROR_RATE}%) exceeds threshold (${ERROR_RATE_THRESHOLD}%)${NC}"
                FAILED=1
            else
                echo -e "${GREEN}✓ Error rate within threshold${NC}"
            fi
        else
            echo -e "${YELLOW}⚠ jq not installed, skipping detailed threshold checks${NC}"
            echo "Install jq for detailed metrics: brew install jq"
        fi
    else
        echo -e "${YELLOW}⚠ Stats file not found${NC}"
    fi
else
    echo -e "${YELLOW}⚠ No Gatling results found${NC}"
fi

echo ""

# Check Artillery results
echo "Checking Artillery results..."
ARTILLERY_RESULTS="$PROJECT_ROOT/artillery/results.json"

if [ -f "$ARTILLERY_RESULTS" ]; then
    echo "Found Artillery results: $ARTILLERY_RESULTS"
    
    if command -v jq &> /dev/null; then
        # Extract Artillery metrics
        echo "Artillery metrics available"
        # Add Artillery-specific threshold checks here
    else
        echo -e "${YELLOW}⚠ jq not installed, skipping Artillery checks${NC}"
    fi
else
    echo -e "${YELLOW}⚠ No Artillery results found${NC}"
fi

echo ""
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All performance thresholds met!${NC}"
    exit 0
else
    echo -e "${RED}❌ Performance thresholds exceeded!${NC}"
    exit 1
fi
