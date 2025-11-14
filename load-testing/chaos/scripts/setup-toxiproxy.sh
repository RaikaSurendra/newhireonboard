#!/bin/bash
# Setup Toxiproxy for chaos testing

echo "Setting up Toxiproxy..."

# Check if Toxiproxy is installed
if ! command -v toxiproxy-server &> /dev/null; then
    echo "Toxiproxy not found. Installing..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install toxiproxy
    else
        echo "Please install Toxiproxy manually: https://github.com/Shopify/toxiproxy"
        exit 1
    fi
fi

# Start Toxiproxy server in background
echo "Starting Toxiproxy server..."
toxiproxy-server &
TOXIPROXY_PID=$!
echo "Toxiproxy server started with PID: $TOXIPROXY_PID"

# Wait for server to start
sleep 2

# Create proxies
echo "Creating proxies..."
toxiproxy-cli create mysql -l localhost:3307 -u localhost:3306
toxiproxy-cli create backend -l localhost:8081 -u localhost:8080

echo "Toxiproxy setup complete!"
echo ""
echo "Proxies created:"
echo "  - MySQL: localhost:3307 -> localhost:3306"
echo "  - Backend: localhost:8081 -> localhost:8080"
echo ""
echo "To stop Toxiproxy: kill $TOXIPROXY_PID"
echo "To list proxies: toxiproxy-cli list"
echo "To add toxic: toxiproxy-cli toxic add -t latency -a latency=1000 mysql"
