#!/bin/bash

# Docker Cleanup Script - Remove all Docker artifacts
# WARNING: This will remove ALL Docker containers, images, volumes, and networks

set -e

echo "🧹 Docker Cleanup Script"
echo "======================="
echo "⚠️  WARNING: This will remove ALL Docker artifacts!"
echo "This includes:"
echo "  - All containers (running and stopped)"
echo "  - All images"
echo "  - All volumes"
echo "  - All custom networks"
echo "  - All build cache"
echo ""
read -p "Are you sure you want to continue? (yes/no): " -r
echo

if [[ ! ""$REPLY"" =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "❌ Cleanup cancelled"
    exit 1
fi

echo "📦 Stopping all running containers..."
if [ -n "$(docker ps -aq 2>/dev/null)" ]; then
    docker stop $(docker ps -aq) 2>/dev/null || true
    echo "✓ Containers stopped"
else
    echo "No running containers to stop"
fi

echo "🗑️  Removing all containers..."
if [ -n "$(docker ps -aq 2>/dev/null)" ]; then
    docker rm -f $(docker ps -aq) 2>/dev/null || true
    echo "✓ Containers removed"
else
    echo "No containers to remove"
fi

echo "🖼️  Removing all images..."
if [ -n "$(docker images -aq 2>/dev/null)" ]; then
    docker rmi -f $(docker images -aq) 2>/dev/null || true
    echo "✓ Images removed"
else
    echo "No images to remove"
fi

echo "💾 Removing all volumes..."
if [ -n "$(docker volume ls -q 2>/dev/null)" ]; then
    docker volume rm $(docker volume ls -q) 2>/dev/null || true
    echo "✓ Volumes removed"
else
    echo "No volumes to remove"
fi

echo "🌐 Removing all custom networks..."
CUSTOM_NETWORKS=$(docker network ls -q 2>/dev/null | xargs -I {} docker network inspect {} --format '{{.Name}}' 2>/dev/null | grep -v -E '^(bridge|host|none)$' || true)
if [ -n """$CUSTOM_NETWORKS""" ]; then
    echo """$CUSTOM_NETWORKS""" | xargs docker network rm 2>/dev/null || true
    echo "✓ Networks removed"
else
    echo "No custom networks to remove"
fi

echo "🧹 Pruning system (removing all unused data)..."
docker system prune -af --volumes

echo "📊 Docker disk usage after cleanup:"
docker system df

echo ""
echo "✅ Docker cleanup complete!"
echo "You now have a clean Docker environment."
echo ""
echo "Next steps:"
echo "1. Run 'make setup' to reinstall dependencies"
echo "2. Run 'make start-all' to start all services"