#!/bin/bash

echo "Waiting for Docker to be ready..."
echo "This may take up to 2 minutes on first start."
echo ""

# Wait up to 2 minutes
for i in {1..24}; do
    if docker info > /dev/null 2>&1; then
        echo "✓ Docker is ready!"
        echo ""
        docker version
        echo ""
        echo "You can now run: make start-all"
        exit 0
    fi
    echo "Still waiting... ($((i*5))/120 seconds)"
    sleep 5
done

echo ""
echo "✗ Docker failed to start after 2 minutes"
echo ""
echo "Please check Docker Desktop:"
echo "1. Look for the Docker whale icon in your menu bar"
echo "2. If it's not there, open Docker from Applications"
echo "3. If Docker shows an error, try:"
echo "   - Docker Desktop → Troubleshoot → Clean / Purge data"
echo "   - Or reinstall Docker Desktop"