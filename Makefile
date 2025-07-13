# AI Debate System - User-Friendly Makefile
# Type 'make help' to see all available commands

# Load environment variables
-include .env
export

# Default values
UI_PORT ?= 3001
DEBATE_API_PORT ?= 5013
LLM_API_PORT ?= 5002

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m # No Color

.PHONY: help start-all stop-all build restart start-ui test-all logs status clean setup start-with-ollama check-ports wait-for-services test-ui-only quick-test full-test

help: ## Show this help message
	@echo '╔══════════════════════════════════════════════════════════════════╗'
	@echo '║                   AI DEBATE SYSTEM - QUICK START                 ║'
	@echo '╚══════════════════════════════════════════════════════════════════╝'
	@echo ''
	@echo '🚀 GETTING STARTED:'
	@echo '  make setup              - First time setup (do this once)'
	@echo '  make start-all          - Start all services (Docker + UI)'
	@echo '  make start-ui           - Start only the UI development server'
	@echo '  make stop-all           - Stop all services'
	@echo ''
	@echo '🔧 DEVELOPMENT:'
	@echo '  make logs               - View all logs'
	@echo '  make logs service=debate - View specific service logs'
	@echo '  make status             - Show service status'
	@echo '  make check-health       - Check all services are healthy'
	@echo ''
	@echo '🤖 OLLAMA (Local LLMs):'
	@echo '  make start-with-ollama  - Start everything including Ollama'
	@echo '  make stop-ollama        - Stop Ollama container'
	@echo ''
	@echo '🧪 TESTING:'
	@echo '  make quick-test         - Run quick UI tests'
	@echo '  make full-test          - Run comprehensive E2E tests'
	@echo '  make test-ui-only       - Test UI without backend services'
	@echo '  make test-playwright    - Run Playwright tests'
	@echo ''
	@echo '📋 ALL COMMANDS:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'
	@echo ''
	@echo '💡 EXAMPLES:'
	@echo '  First time:  make setup && make start && make ui'
	@echo '  Development: make ui (in separate terminal)'
	@echo '  Check logs:  make logs service=debate'

check-ports: ## Check if required ports are available
	@echo "$(BLUE)Checking port availability...$(NC)"
	@for port in $(UI_PORT) $(DEBATE_API_PORT) $(LLM_API_PORT) 5432 6379 6333; do \
		if lsof -Pi :$$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then \
			echo "$(RED)✗ Port $$port is already in use$(NC)"; \
			exit 1; \
		else \
			echo "$(GREEN)✓ Port $$port is available$(NC)"; \
		fi \
	done

start-all: check-ports ## Start all services including UI
	@echo "$(BLUE)🚀 Starting AI Debate System...$(NC)"
	docker-compose up -d
	@$(MAKE) wait-for-services
	@echo "$(GREEN)✅ Backend services are ready!$(NC)"
	@echo "$(YELLOW)🎨 Starting UI development server...$(NC)"
	@$(MAKE) start-ui &
	@sleep 3
	@echo "$(GREEN)✅ All services started!$(NC)"
	@echo "$(BLUE)🌐 Open http://localhost:3001 in your browser$(NC)"

start: start-all ## Alias for start-all (backward compatibility)

wait-for-services: ## Wait for all services to be healthy
	@echo "$(BLUE)Waiting for services to be healthy...$(NC)"
	@echo -n "PostgreSQL: "
	@timeout 30 bash -c 'until docker-compose exec -T postgres pg_isready -U context_user > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"
	@echo -n "Redis: "
	@timeout 30 bash -c 'until docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"
	@echo -n "LLM Service: "
	@timeout 30 bash -c 'until curl -s http://localhost:$(LLM_API_PORT)/health > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"
	@echo -n "Debate Service: "
	@timeout 30 bash -c 'until curl -s http://localhost:$(DEBATE_API_PORT)/health > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"

stop-all: ## Stop all services including UI
	@echo "$(BLUE)🛑 Stopping all services...$(NC)"
	docker-compose down
	@pkill -f "npm run dev" || true
	@echo "$(GREEN)✅ All services stopped$(NC)"

stop: stop-all ## Alias for stop-all (backward compatibility)

build: ## Build all services
	@echo "$(BLUE)🏗️ Building services...$(NC)"
	docker-compose build
	@echo "$(GREEN)✅ Build complete$(NC)"

restart: stop start ## Restart all services

start-ui: ## Start only the UI development server
	@echo "$(BLUE)🎨 Starting UI development server...$(NC)"
	@echo "$(YELLOW)Checking if port $(UI_PORT) is available...$(NC)"
	@if lsof -Pi :$(UI_PORT) -sTCP:LISTEN -t >/dev/null 2>&1; then \
		echo "$(RED)Port $(UI_PORT) is in use. Trying port 3001...$(NC)"; \
		cd debate-ui && PORT=3001 npm run dev; \
	else \
		cd debate-ui && PORT=$(UI_PORT) npm run dev; \
	fi

ui: start-ui ## Alias for start-ui (backward compatibility)

check-health: ## Check health of all services
	@echo "$(BLUE)🏥 Checking service health...$(NC)"
	@echo -n "PostgreSQL: " && \
		(docker-compose exec -T postgres pg_isready -U context_user > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "Redis: " && \
		(docker-compose exec -T redis redis-cli ping > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "LLM Service: " && \
		(curl -s http://localhost:$(LLM_API_PORT)/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "Debate Service: " && \
		(curl -s http://localhost:$(DEBATE_API_PORT)/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "UI (if running): " && \
		(curl -s http://localhost:$(UI_PORT) > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠ Not running (run 'make ui')$(NC)")

full-test: ## Run comprehensive E2E tests
	@echo "$(BLUE)🧪 Running comprehensive tests with Docker...$(NC)"
	@echo "$(YELLOW)📁 Test results will be saved to: ./test_probe$(NC)"
	@mkdir -p test_probe
	docker-compose --profile test build test-runner
	docker-compose --profile test run --rm test-runner
	@echo "$(GREEN)✅ Test results saved to ./test_probe$(NC)"
	@echo "$(BLUE)📊 View latest results: ls -la test_probe/$(NC)"

test: full-test ## Alias for full-test (backward compatibility)

quick-test: ## Run quick UI tests
	@echo "$(BLUE)🧪 Running quick UI tests...$(NC)"
	@cd debate-ui && npm run test:e2e

test-ui-only: ## Run UI tests without backend (for UI-only testing)
	@echo "$(BLUE)🧪 Running UI-only tests...$(NC)"
	cd debate-ui && npm test
	cd debate-ui/playwright-tests && npm test

test-e2e: ## Run E2E tests locally (requires services running)
	@echo "$(BLUE)🧪 Running E2E tests locally...$(NC)"
	@echo "$(YELLOW)Checking if services are running...$(NC)"
	@$(MAKE) check-health
	cd e2e-tests && npm test

test-playwright: ## Run Playwright tests locally
	@echo "$(BLUE)🧪 Running Playwright tests...$(NC)"
	cd playwright-tests && npm test

test-llm: ## Test LLM service connectivity and functionality
	@echo "$(BLUE)🤖 Testing LLM service...$(NC)"
	@chmod +x test-llm.sh
	@./test-llm.sh

test-quick: ## Run quick smoke tests
	@echo "$(BLUE)🚀 Running quick smoke tests...$(NC)"
	cd playwright-tests && npm run test:smoke

test-ui-headed: ## Run UI tests with headed browser
	@echo "$(BLUE)🖥️ Running UI tests with visible browser...$(NC)"
	cd playwright-tests && npm run test:headed

test-debug: ## Run tests in debug mode
	@echo "$(BLUE)🐛 Running tests in debug mode...$(NC)"
	cd playwright-tests && npm run test:debug

logs: ## Show logs (use service=NAME to filter)
	@if [ -z "$(service)" ]; then \
		docker-compose logs -f --tail=100; \
	else \
		docker-compose logs -f --tail=100 $(service); \
	fi

status: ## Show service status
	@echo "$(BLUE)📊 Service Status:$(NC)"
	@docker-compose ps

clean: ## Clean up containers and volumes
	@echo "$(YELLOW)🧹 Cleaning up...$(NC)"
	@echo "$(RED)⚠️  This will delete all data! Continue? [y/N]$(NC)"
	@read -r response && \
	if [ "$$response" = "y" ]; then \
		docker-compose down -v; \
		docker system prune -f; \
		echo "$(GREEN)✅ Cleanup complete$(NC)"; \
	else \
		echo "$(YELLOW)Cleanup cancelled$(NC)"; \
	fi

setup: ## First time setup (install dependencies)
	@echo "$(BLUE)📦 Setting up project...$(NC)"
	@if [ ! -f .env ]; then \
		echo "$(YELLOW)Creating .env file from template...$(NC)"; \
		cp .env.example .env; \
		echo "$(GREEN)✅ .env file created$(NC)"; \
		echo "$(YELLOW)⚠️  Please edit .env file with your API keys$(NC)"; \
	else \
		echo "$(GREEN)✓ .env file already exists$(NC)"; \
	fi
	@echo "$(BLUE)Installing UI dependencies...$(NC)"
	cd debate-ui && npm install
	@echo "$(BLUE)Installing E2E test dependencies...$(NC)"
	cd e2e-tests && npm install
	@echo "$(BLUE)Installing Playwright test dependencies...$(NC)"
	cd playwright-tests && npm install
	@echo "$(GREEN)✅ Setup complete!$(NC)"
	@echo "$(YELLOW)Next steps:$(NC)"
	@echo "  1. Edit .env file with your API keys"
	@echo "  2. Run 'make start' to start services"
	@echo "  3. Run 'make ui' in a new terminal to start the UI"

start-with-ollama: check-ports ## Start all services including Ollama
	@echo "$(BLUE)🤖 Starting services with Ollama support...$(NC)"
	docker-compose --profile llama up -d
	@$(MAKE) wait-for-services
	@echo "$(GREEN)✅ All services including Ollama are ready!$(NC)"
	@echo "$(YELLOW)🎨 Starting UI development server...$(NC)"
	@$(MAKE) start-ui &
	@sleep 3
	@echo "$(GREEN)✅ System ready with Ollama support!$(NC)"
	@echo "Ollama: http://localhost:$${OLLAMA_PORT:-11434}"

start-ollama: start-with-ollama ## Alias for start-with-ollama (backward compatibility)

stop-ollama: ## Stop all services including Ollama
	@echo "$(BLUE)🛑 Stopping all services...$(NC)"
	docker-compose --profile llama down
	@echo "$(GREEN)✅ All services stopped$(NC)"

# Development helpers
shell-postgres: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U context_user -d context_db

shell-redis: ## Open Redis CLI
	docker-compose exec redis redis-cli

inspect-debates: ## Show debates in database
	docker-compose exec mcp-debate sqlite3 /app/data/debates.db "SELECT * FROM debates;"

# Evidence collection for testing
collect-evidence: ## Collect evidence of working system
	@echo "$(BLUE)📸 Collecting evidence...$(NC)"
	@mkdir -p evidence/$(shell date +%Y%m%d_%H%M%S)
	@echo "$(YELLOW)Taking screenshots and collecting data...$(NC)"
	cd debate-ui && node capture-screenshots.js
	@echo "$(GREEN)✅ Evidence collected in evidence/ directory$(NC)"

# Troubleshooting commands
fix-ui: ## Fix common UI issues
	@echo "$(BLUE)🔧 Fixing common UI issues...$(NC)"
	cd debate-ui && node fix-ui-issues.js
	@echo "$(GREEN)✅ UI fixes applied$(NC)"

reset-db: ## Reset all databases (WARNING: deletes all data)
	@echo "$(RED)⚠️  This will delete all data! Continue? [y/N]$(NC)"
	@read -r response && \
	if [ "$$response" = "y" ]; then \
		docker-compose down -v; \
		docker volume rm zamaz-debate-mcp_postgres_data zamaz-debate-mcp_redis_data 2>/dev/null || true; \
		echo "$(GREEN)✅ Databases reset$(NC)"; \
	else \
		echo "$(YELLOW)Reset cancelled$(NC)"; \
	fi