#!/bin/bash
# Script to provision isolated test resources

set -e

# Parse arguments
RUN_ID=""
SERVICE=""
ISOLATION="container"

while [[ $# -gt 0 ]]; do
  case $1 in
    --run-id) RUN_ID="$2"; shift 2 ;;
    --service) SERVICE="$2"; shift 2 ;;
    --isolation) ISOLATION="$2"; shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

echo "📦 Provisioning isolated resources for $SERVICE..."
echo "Run ID: $RUN_ID"
echo "Isolation Level: $ISOLATION"

# Create isolated network
NETWORK_NAME="test-network-$RUN_ID"
docker network create $NETWORK_NAME || true

# Provision isolated database
if [[ "$SERVICE" == "mcp-"* ]]; then
  echo "🗄️ Creating isolated PostgreSQL database..."
  
  DB_CONTAINER="test-db-$RUN_ID"
  DB_NAME="test_${SERVICE//-/_}_${RUN_ID: -8}"
  DB_PORT=$((5432 + RANDOM % 1000))
  
  docker run -d \
    --name $DB_CONTAINER \
    --network $NETWORK_NAME \
    -e POSTGRES_DB=$DB_NAME \
    -e POSTGRES_USER=test \
    -e POSTGRES_PASSWORD=test \
    -p $DB_PORT:5432 \
    postgres:15-alpine
  
  # Wait for database to be ready
  echo "Waiting for database to be ready..."
  for i in {1..30}; do
    if docker exec $DB_CONTAINER pg_isready -U test > /dev/null 2>&1; then
      echo "✅ Database is ready!"
      break
    fi
    sleep 1
  done
  
  # Export connection details
  echo "DATABASE_URL=postgresql://test:test@localhost:$DB_PORT/$DB_NAME" >> $GITHUB_ENV
  echo "TEST_DB_CONTAINER=$DB_CONTAINER" >> $GITHUB_ENV
fi

# Provision isolated Redis
if [[ "$SERVICE" == "mcp-gateway" ]] || [[ "$SERVICE" == "mcp-controller" ]]; then
  echo "🔴 Creating isolated Redis instance..."
  
  REDIS_CONTAINER="test-redis-$RUN_ID"
  REDIS_PORT=$((6379 + RANDOM % 1000))
  
  docker run -d \
    --name $REDIS_CONTAINER \
    --network $NETWORK_NAME \
    -p $REDIS_PORT:6379 \
    redis:7-alpine
  
  echo "REDIS_URL=redis://localhost:$REDIS_PORT" >> $GITHUB_ENV
  echo "TEST_REDIS_CONTAINER=$REDIS_CONTAINER" >> $GITHUB_ENV
fi

# Provision isolated RabbitMQ
if [[ "$SERVICE" == "mcp-"* ]]; then
  echo "🐰 Creating isolated RabbitMQ instance..."
  
  RABBITMQ_CONTAINER="test-rabbitmq-$RUN_ID"
  AMQP_PORT=$((5672 + RANDOM % 1000))
  
  docker run -d \
    --name $RABBITMQ_CONTAINER \
    --network $NETWORK_NAME \
    -e RABBITMQ_DEFAULT_USER=test \
    -e RABBITMQ_DEFAULT_PASS=test \
    -p $AMQP_PORT:5672 \
    rabbitmq:3-alpine
  
  # Wait for RabbitMQ to be ready
  sleep 10
  
  echo "RABBITMQ_URL=amqp://test:test@localhost:$AMQP_PORT" >> $GITHUB_ENV
  echo "TEST_RABBITMQ_CONTAINER=$RABBITMQ_CONTAINER" >> $GITHUB_ENV
fi

# Create container isolation if requested
if [[ "$ISOLATION" == "container" ]]; then
  echo "🐳 Setting up container isolation..."
  
  # Build test container
  TEST_CONTAINER="test-runner-$RUN_ID"
  
  cat > Dockerfile.test << EOF
FROM openjdk:21-slim
RUN apt-get update && apt-get install -y nodejs npm maven
WORKDIR /app
COPY . .
EOF
  
  docker build -f Dockerfile.test -t $TEST_CONTAINER .
  echo "TEST_CONTAINER=$TEST_CONTAINER" >> $GITHUB_ENV
fi

# Save resource manifest
cat > resources-$RUN_ID.json << EOF
{
  "runId": "$RUN_ID",
  "service": "$SERVICE",
  "isolation": "$ISOLATION",
  "network": "$NETWORK_NAME",
  "resources": {
    "database": "${DB_CONTAINER:-none}",
    "redis": "${REDIS_CONTAINER:-none}",
    "rabbitmq": "${RABBITMQ_CONTAINER:-none}",
    "container": "${TEST_CONTAINER:-none}"
  },
  "created": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

echo "✅ Resources provisioned successfully!"