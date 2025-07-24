#!/bin/bash
# Script to cleanup isolated test resources

set -e

RUN_ID=""
FORCE=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --run-id) RUN_ID="$2"; shift 2 ;;
    --force) FORCE=true; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

echo "🧹 Cleaning up resources for run: $RUN_ID"

# Load resource manifest
MANIFEST="resources-$RUN_ID.json"
if [[ ! -f "$MANIFEST" ]]; then
  echo "⚠️ Resource manifest not found: $MANIFEST"
  exit 0
fi

# Extract container names
DB_CONTAINER=$(jq -r '.resources.database' $MANIFEST)
REDIS_CONTAINER=$(jq -r '.resources.redis' $MANIFEST)
RABBITMQ_CONTAINER=$(jq -r '.resources.rabbitmq' $MANIFEST)
NETWORK_NAME=$(jq -r '.network' $MANIFEST)

# Stop and remove containers
if [[ "$DB_CONTAINER" != "none" ]]; then
  echo "Removing database container: $DB_CONTAINER"
  docker stop $DB_CONTAINER 2>/dev/null || true
  docker rm $DB_CONTAINER 2>/dev/null || true
fi

if [[ "$REDIS_CONTAINER" != "none" ]]; then
  echo "Removing Redis container: $REDIS_CONTAINER"
  docker stop $REDIS_CONTAINER 2>/dev/null || true
  docker rm $REDIS_CONTAINER 2>/dev/null || true
fi

if [[ "$RABBITMQ_CONTAINER" != "none" ]]; then
  echo "Removing RabbitMQ container: $RABBITMQ_CONTAINER"
  docker stop $RABBITMQ_CONTAINER 2>/dev/null || true
  docker rm $RABBITMQ_CONTAINER 2>/dev/null || true
fi

# Remove network
if [[ -n "$NETWORK_NAME" ]]; then
  echo "Removing network: $NETWORK_NAME"
  docker network rm $NETWORK_NAME 2>/dev/null || true
fi

# Clean up manifest
rm -f $MANIFEST

echo "✅ Cleanup completed!"