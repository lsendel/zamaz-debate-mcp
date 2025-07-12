# AI Debate System - User-Friendly Makefile
# Type 'make help' to see all available commands

# Load environment variables
-include .env
export

.PHONY: help start stop build restart ui test logs status clean setup start-ollama
help: ## Show this help message
	@echo '╔══════════════════════════════════════════════════════════════════╗'
	@echo '║                   AI DEBATE SYSTEM - QUICK START                 ║'
	@echo '╚══════════════════════════════════════════════════════════════════╝'
	@echo ''
	@echo '🚀 GETTING STARTED:'
	@echo '  make setup              - First time setup (do this once)'
	@echo '  make start              - Start everything'
	@echo '  make stop               - Stop everything'
	@echo ''
	@echo '🔧 DEVELOPMENT:'
	@echo '  make ui                 - Start UI development server'
	@echo '  make logs               - View all logs'
	@echo '  make logs service=debate - View specific service logs'
	@echo ''
	@echo '🤖 OLLAMA (Local LLMs):'
	@echo '  make start-ollama       - Start with Ollama support'
	@echo '  make stop-ollama        - Stop including Ollama'
	@echo ''
	@echo '🧪 TESTING:'
	@echo '  make test               - Run all tests'
	@echo ''
	@echo '📋 ALL COMMANDS:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ''
	@echo '💡 EXAMPLES:'
	@echo '  First time:  make setup && make start'
	@echo '  Development: make ui'
	@echo '  Check logs:  make logs service=debate'

start: ## Start all services
	@echo "🚀 Starting all services..."
	docker-compose up -d
	@echo "✅ Services started!"
	@echo "UI: http://localhost:$${UI_PORT:-3000} (run 'make ui' to start)"

stop: ## Stop all services
	@echo "🛑 Stopping services..."
	docker-compose down

build: ## Build all services
	@echo "🏗️ Building services..."
	docker-compose build

restart: stop start ## Restart all services

ui: ## Start UI development server
	@echo "🎨 Starting UI on port $${UI_PORT:-3000}..."
	cd debate-ui && PORT=$${UI_PORT:-3000} npm run dev

test: ## Run all tests in Docker with full reporting
	@echo "🧪 Running comprehensive tests with Docker..."
	@echo "📁 Test results will be saved to: ./test_probe"
	@mkdir -p test_probe
	docker-compose --profile test build test-runner
	docker-compose --profile test run --rm test-runner
	@echo "✅ Test results saved to ./test_probe"
	@echo "📊 View latest results: ls -la test_probe/"

test-local: ## Run tests locally (requires services running)
	@echo "🧪 Running tests locally..."
	cd e2e-tests && npm test

test-llm: ## Test LLM service connectivity and functionality
	@echo "🤖 Testing LLM service..."
	@chmod +x test-llm.sh
	@./test-llm.sh

test-quick: ## Run quick smoke tests
	@echo "🚀 Running quick smoke tests..."
	cd playwright-tests && npm run test:smoke

test-ui: ## Run UI tests with headed browser
	@echo "🖥️ Running UI tests with visible browser..."
	cd playwright-tests && npm run test:headed

test-debug: ## Run tests in debug mode
	@echo "🐛 Running tests in debug mode..."
	cd playwright-tests && npm run test:debug

logs: ## Show logs (use service=NAME to filter)
	docker-compose logs -f

status: ## Show service status
	docker-compose ps

clean: ## Clean up containers and volumes
	@echo "🧹 Cleaning up..."
	docker-compose down -v
	docker system prune -f

setup: ## First time setup (install dependencies)
	@echo "📦 Setting up project..."
	@if [ ! -f .env ]; then cp .env.example .env; fi
	cd debate-ui && npm install
	cd e2e-tests && npm install
	@echo "✅ Setup complete! Edit .env file with your API keys"

start-ollama: ## Start services with Ollama for local LLMs
	@echo "🤖 Starting services with Ollama..."
	docker-compose --profile llama up -d
	@echo "✅ Services started with Ollama!"
	@echo "Ollama: http://localhost:$${OLLAMA_PORT:-11434}"
	@echo "UI: http://localhost:$${UI_PORT:-3000} (run 'make ui' to start)"

stop-ollama: ## Stop all services including Ollama
	@echo "🛑 Stopping all services..."
	docker-compose --profile llama down