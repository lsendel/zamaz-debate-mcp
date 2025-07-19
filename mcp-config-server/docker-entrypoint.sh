#!/bin/bash

# Docker entrypoint script for MCP Config Server
set -e

echo "Starting MCP Config Server..."

# Configure Git for SSH access if SSH key is provided
if [ -n "$GIT_SSH_KEY" ]; then
    echo "Configuring SSH for Git access..."
    echo "$GIT_SSH_KEY" | base64 -d > /app/.ssh/id_rsa
    chmod 600 /app/.ssh/id_rsa
    
    # Add Git host to known hosts
    if [ -n "$GIT_HOST" ]; then
        ssh-keyscan -H "$GIT_HOST" >> /app/.ssh/known_hosts 2>/dev/null
    fi
fi

# Configure Git credentials if provided
if [ -n "$GIT_USERNAME" ] && [ -n "$GIT_PASSWORD" ]; then
    echo "Configuring Git credentials..."
    git config --global credential.helper store
    echo "https://${GIT_USERNAME}:${GIT_PASSWORD}@${GIT_HOST}" > ~/.git-credentials
fi

# Wait for dependencies if needed
if [ -n "$WAIT_FOR_SERVICES" ]; then
    echo "Waiting for dependent services..."
    for service in $WAIT_FOR_SERVICES; do
        echo "Waiting for $service..."
        while ! nc -z ${service%:*} ${service#*:} 2>/dev/null; do
            sleep 2
        done
        echo "$service is available"
    done
fi

# Set JVM options
export JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

# Add Spring profiles
if [ -n "$SPRING_PROFILES_ACTIVE" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}"
fi

# Add Config Server specific options
if [ -n "$CONFIG_GIT_REPO_URI" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dspring.cloud.config.server.git.uri=${CONFIG_GIT_REPO_URI}"
fi

if [ -n "$CONFIG_ENCRYPTION_KEY" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dencrypt.key=${CONFIG_ENCRYPTION_KEY}"
fi

# Enable Cloud Bus if RabbitMQ is configured
if [ -n "$RABBITMQ_HOST" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.include=bus"
fi

# Enable Vault if configured
if [ -n "$VAULT_TOKEN" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.include=vault"
fi

# Enable AWS Secrets Manager if configured
if [ -n "$AWS_REGION" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.include=aws"
fi

echo "Starting application with JAVA_OPTS: $JAVA_OPTS"

# Execute the application
exec java $JAVA_OPTS -jar /app/app.jar "$@"