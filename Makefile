# Simple MCP Debate System Makefile
.PHONY: help start stop build test ui clean logs

# Default target
.DEFAULT_GOAL := help

## Show help
help:
	@echo "🤖 MCP Debate System - Simple Commands"
	@echo "======================================"
	@echo ""
	@echo "start    - Start all services"
	@echo "stop     - Stop all services"
	@echo "build    - Build all services"
	@echo "restart  - Restart all services"
	@echo "ui       - Start UI development server"
	@echo "test     - Run Puppeteer tests"
	@echo "logs     - Show logs"
	@echo "clean    - Clean up containers and volumes"
	@echo "status   - Show service status"
	@echo ""

## Start all services
start:
	@echo "🚀 Starting all services..."
	docker-compose up -d
	@echo "✅ Services started!"
	@echo "UI: http://localhost:3001"

## Stop all services
stop:
	@echo "🛑 Stopping services..."
	docker-compose down

## Build all services
build:
	@echo "🏗️ Building services..."
	docker-compose build

## Restart all services
restart: stop start

## Start UI development server
ui:
	@echo "🎨 Starting UI..."
	cd debate-ui && npm run dev

## Run tests
test:
	@echo "🧪 Running tests..."
	cd e2e-tests && npm test

## Show logs
logs:
	docker-compose logs -f

## Show service status
status:
	docker-compose ps

## Clean up
clean:
	@echo "🧹 Cleaning up..."
	docker-compose down -v
	docker system prune -f

## Setup - one time only
setup:
	@echo "📦 Setting up project..."
	@if [ ! -f .env ]; then cp .env.example .env; fi
	cd debate-ui && npm install
	cd e2e-tests && npm install
	@echo "✅ Setup complete! Edit .env file with your API keys"