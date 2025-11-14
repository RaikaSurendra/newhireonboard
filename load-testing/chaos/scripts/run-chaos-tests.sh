#!/bin/bash
# Run all chaos experiments

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
EXPERIMENTS_DIR="$SCRIPT_DIR/../experiments"
RESULTS_DIR="$SCRIPT_DIR/../results"

# Create results directory
mkdir -p "$RESULTS_DIR"

echo "=========================================="
echo "Running Chaos Engineering Experiments"
echo "=========================================="
echo ""

# Set auth token (replace with actual token or get from login)
export AUTH_TOKEN="your-auth-token-here"

# Array of experiments
experiments=(
    "database-latency.json"
    "api-failure.json"
    "network-partition.json"
    "cascading-failure.json"
)

# Run each experiment
for experiment in "${experiments[@]}"; do
    experiment_path="$EXPERIMENTS_DIR/$experiment"
    experiment_name="${experiment%.json}"
    result_file="$RESULTS_DIR/${experiment_name}-$(date +%Y%m%d-%H%M%S).json"
    
    echo "Running experiment: $experiment_name"
    echo "Results will be saved to: $result_file"
    
    chaos run "$experiment_path" --journal-path "$result_file"
    
    if [ $? -eq 0 ]; then
        echo "✓ Experiment completed successfully"
    else
        echo "✗ Experiment failed"
    fi
    
    echo ""
    echo "Waiting 30 seconds before next experiment..."
    sleep 30
done

echo "=========================================="
echo "All chaos experiments completed!"
echo "Results saved in: $RESULTS_DIR"
echo "=========================================="
