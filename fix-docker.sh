#!/bin/bash

echo "=== Docker Troubleshooting Script ==="
echo ""

# 1. Check Docker Desktop status
echo "1. Checking Docker Desktop process..."
if pgrep -x "Docker" > /dev/null; then
    echo "✓ Docker Desktop is running"
else
    echo "✗ Docker Desktop is not running"
    echo "  Starting Docker Desktop..."
    open -a Docker
    echo "  Waiting 30 seconds for Docker to start..."
    sleep 30
fi

# 2. Check Docker daemon
echo ""
echo "2. Checking Docker daemon..."
if docker info > /dev/null 2>&1; then
    echo "✓ Docker daemon is responding"
else
    echo "✗ Docker daemon is not responding"
    
    # Try to reset Docker
    echo ""
    echo "3. Attempting to reset Docker..."
    
    # Kill any stuck Docker processes
    echo "  Killing stuck processes..."
    pkill -f docker
    sleep 2
    
    # Remove potentially corrupted socket
    echo "  Cleaning up Docker socket..."
    rm -f ~/.docker/run/docker.sock
    
    # Restart Docker Desktop
    echo "  Restarting Docker Desktop..."
    osascript -e 'quit app "Docker"' 2>/dev/null
    sleep 5
    open -a Docker
    
    echo "  Waiting for Docker to start (this may take up to 60 seconds)..."
    for i in {1..12}; do
        if docker info > /dev/null 2>&1; then
            echo "✓ Docker is now responding!"
            break
        fi
        echo "  Still waiting... ($((i*5))/60 seconds)"
        sleep 5
    done
fi

# 4. Final check
echo ""
echo "4. Final Docker status:"
if docker info > /dev/null 2>&1; then
    echo "✓ Docker is working!"
    docker version --format 'Client: {{.Client.Version}}, Server: {{.Server.Version}}' 2>/dev/null || echo "Version info not available yet"
    echo ""
    echo "You can now run: make start-all"
else
    echo "✗ Docker is still not working"
    echo ""
    echo "Try these manual steps:"
    echo "1. Open Docker Desktop manually from Applications"
    echo "2. In Docker Desktop, go to Troubleshoot (bug icon)"
    echo "3. Click 'Clean / Purge data'"
    echo "4. Select 'Clean' (this will remove all containers/images)"
    echo "5. Restart Docker Desktop"
    echo ""
    echo "If that doesn't work:"
    echo "1. Quit Docker Desktop"
    echo "2. Delete these files:"
    echo "   rm -rf ~/Library/Group\\ Containers/group.com.docker"
    echo "   rm -rf ~/Library/Containers/com.docker.docker"
    echo "   rm -rf ~/.docker"
    echo "3. Restart your Mac"
    echo "4. Reinstall Docker Desktop"
fi

# 5. Check disk space (common issue)
echo ""
echo "5. Checking disk space:"
df -h / | grep -E "^/|Filesystem"
echo ""
echo "Note: Docker needs at least 10GB free space to operate properly"