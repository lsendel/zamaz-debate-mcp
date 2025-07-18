#!/bin/bash

# Script to run Spring Boot applications with .env file

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
else
    echo "Warning: .env file not found!"
    exit 1
fi

# Check if DB_PASSWORD is set
if [ -z """$DB_PASSWORD""" ]; then
    echo "Error: DB_PASSWORD not set in .env file"
    exit 1
fi

echo "Database password loaded from .env file"

# Run the specified service or command
if [ "$#" -eq 0 ]; then
    echo "Usage: ./run-with-env.sh <service-name>"
    echo "Examples:"
    echo "  ./run-with-env.sh mcp-rag"
    echo "  ./run-with-env.sh mcp-template"
    echo "  ./run-with-env.sh mcp-modulith"
    exit 1
fi

SERVICE=$1

case ""$SERVICE"" in
    mcp-rag)
        echo "Starting mcp-rag service..."
        cd mcp-rag && mvn spring-boot:run
        ;;
    mcp-template)
        echo "Starting mcp-template service..."
        cd mcp-template && mvn spring-boot:run
        ;;
    mcp-modulith)
        echo "Starting mcp-modulith service..."
        cd mcp-modulith && mvn spring-boot:run
        ;;
    all)
        echo "Starting all services with docker-compose..."
        docker-compose up
        ;;
    *)
        echo "Unknown service: ""$SERVICE"""
        echo "Available services: mcp-rag, mcp-template, mcp-modulith, all"
        exit 1
        ;;
esac