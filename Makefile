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
	@echo '  make test-setup         - Set up test environment'
	@echo '  make test-unit          - Run unit tests for GitHub integration'
	@echo '  make test-integration   - Run integration tests'
	@echo '  make test-api           - Test API endpoints with curl'
	@echo '  make test-docker        - Test Docker containers and health'
	@echo '  make test-e2e-full      - Run full end-to-end tests'
	@echo '  make test-performance   - Run performance and load tests'
	@echo ''
	@echo '🔍 CODE QUALITY & LINTING:'
	@echo '  make lint-all           - Run all linting checks'
	@echo '  make lint-java          - Lint Java code (Checkstyle, SpotBugs, PMD)'
	@echo '  make lint-frontend      - Lint React TypeScript code'
	@echo '  make lint-config        - Lint configuration files (YAML, JSON)'
	@echo '  make lint-docs          - Lint documentation (Markdown)'
	@echo '  make lint-fix           - Auto-fix linting issues where possible'
	@echo '  make lint-report        - Generate comprehensive linting report'
	@echo '  make test-security      - Run security tests'
	@echo '  make test-all-github    - Run all GitHub integration tests'
	@echo '  make test-report        - Generate test report with evidence'
	@echo '  make quick-test         - Run quick UI tests'
	@echo '  make full-test          - Run comprehensive E2E tests'
	@echo '  make test-ui-only       - Test UI without backend services'
	@echo '  make test-playwright    - Run Playwright tests'
	@echo '  make test-mcp-all       - Test all MCP services'
	@echo '  make test-mcp-detail    - Run detailed MCP service tests'
	@echo '  make test-services      - Quick test of all services'
	@echo '  make test-services-detail - Enhanced quick test with details'
	@echo ''
	@echo '🧹 CLEANUP:'
	@echo '  make clean              - Remove all Docker containers/volumes'
	@echo '  make clean-all          - Complete Docker cleanup (ALL data)'
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
	@timeout 30 bash -c 'until docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"
	@echo -n "Redis: "
	@timeout 30 bash -c 'until docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"
	@echo -n "LLM Service: "
	@timeout 30 bash -c 'until curl -s http://localhost:$(LLM_API_PORT)/actuator/health > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"
	@echo -n "Debate Service: "
	@timeout 30 bash -c 'until curl -s http://localhost:$(DEBATE_API_PORT)/actuator/health > /dev/null 2>&1; do sleep 1; done' && echo "$(GREEN)✓$(NC)" || echo "$(RED)✗$(NC)"

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
		(docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "Redis: " && \
		(docker-compose exec -T redis redis-cli ping > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "MCP Organization: " && \
		(curl -s http://localhost:5005/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "MCP Controller (Debate): " && \
		(curl -s http://localhost:5013/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "MCP LLM: " && \
		(curl -s http://localhost:$(LLM_API_PORT)/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "MCP RAG: " && \
		(curl -s http://localhost:5004/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
	@echo -n "MCP Template: " && \
		(curl -s http://localhost:5006/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Not responding$(NC)")
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
	cd playwright-tests && npm run test:smoke

test-ui-only: ## Run UI tests without backend (for UI-only testing)
	@echo "$(BLUE)🧪 Running UI-only tests...$(NC)"
	cd playwright-tests && npm test

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

# MCP Service Testing Commands
test-mcp-all: ## Test all MCP services
	@echo "$(BLUE)🧪 Testing all MCP services...$(NC)"
	@$(MAKE) test-mcp-organization
	@$(MAKE) test-mcp-llm
	@$(MAKE) test-mcp-controller
	@$(MAKE) test-mcp-rag
	@$(MAKE) test-mcp-template
	@echo "$(GREEN)✅ All MCP service tests complete!$(NC)"

test-mcp-organization: ## Test MCP Organization service
	@echo "$(BLUE)🏢 Testing MCP Organization service...$(NC)"
	@curl -s http://localhost:5005/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Health check passed$(NC)" || echo "$(RED)✗ Health check failed$(NC)"
	@curl -s http://localhost:5005/api-docs > /dev/null 2>&1 && echo "$(GREEN)✓ API docs available$(NC)" || echo "$(RED)✗ API docs not available$(NC)"

test-mcp-controller: ## Test MCP Controller service
	@echo "$(BLUE)⚔️ Testing MCP Controller service...$(NC)"
	@curl -s http://localhost:5013/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Health check passed$(NC)" || echo "$(RED)✗ Health check failed$(NC)"
	@curl -s http://localhost:5013/api/v1/debates \
		| jq '.' 2>/dev/null && echo "$(GREEN)✓ List debates test passed$(NC)" || echo "$(RED)✗ List debates test failed$(NC)"

test-mcp-llm: ## Test MCP LLM service
	@echo "$(BLUE)🤖 Testing MCP LLM service...$(NC)"
	@curl -s http://localhost:5002/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Health check passed$(NC)" || echo "$(RED)✗ Health check failed$(NC)"
	@curl -s http://localhost:5002/api/v1/providers \
		| jq '.' 2>/dev/null && echo "$(GREEN)✓ List providers test passed$(NC)" || echo "$(RED)✗ List providers test failed$(NC)"


test-mcp-rag: ## Test MCP RAG service
	@echo "$(BLUE)🔍 Testing MCP RAG service...$(NC)"
	@curl -s http://localhost:5004/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Health check passed$(NC)" || echo "$(RED)✗ Health check failed$(NC)"
	@curl -s http://localhost:5004/api-docs > /dev/null 2>&1 && echo "$(GREEN)✓ API docs available$(NC)" || echo "$(RED)✗ API docs not available$(NC)"

test-mcp-template: ## Test MCP Template service
	@echo "$(BLUE)📋 Testing MCP Template service...$(NC)"
	@curl -s http://localhost:5006/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Health check passed$(NC)" || echo "$(RED)✗ Health check failed$(NC)"
	@curl -s http://localhost:5006/api-docs > /dev/null 2>&1 && echo "$(GREEN)✓ API docs available$(NC)" || echo "$(RED)✗ API docs not available$(NC)"

test-mcp-detail: ## Run detailed tests for all MCP services
	@echo "$(BLUE)🧪 Running detailed MCP service tests...$(NC)"
	@if [ ! -f mcp-tests/test-all-mcp-detailed.sh ]; then \
		echo "$(RED)✗ Test scripts not found. Please ensure mcp-tests directory exists.$(NC)"; \
		exit 1; \
	fi
	@bash mcp-tests/test-all-mcp-detailed.sh

test-mcp-detail-%: ## Run detailed test for specific MCP service (e.g., make test-mcp-detail-llm)
	@echo "$(BLUE)🧪 Running detailed test for MCP $* service...$(NC)"
	@if [ ! -f mcp-tests/test-mcp-$*.sh ]; then \
		echo "$(RED)✗ Test script for $* not found.$(NC)"; \
		exit 1; \
	fi
	@bash mcp-tests/test-mcp-$*.sh

test-services: ## Quick test of all services
	@echo "$(BLUE)🚀 Running quick service test...$(NC)"
	@if [ ! -x mcp-tests/quick-test.sh ]; then \
		chmod +x mcp-tests/quick-test.sh; \
	fi
	@./mcp-tests/quick-test.sh

test-services-detail: ## Enhanced quick test with details
	@echo "$(BLUE)🚀 Running enhanced quick service test...$(NC)"
	@if [ ! -x mcp-tests/quick-test-enhanced.sh ]; then \
		chmod +x mcp-tests/quick-test-enhanced.sh; \
	fi
	@./mcp-tests/quick-test-enhanced.sh

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

clean-all: ## Complete Docker cleanup (removes ALL Docker data)
	@echo "$(RED)🚨 COMPLETE DOCKER CLEANUP$(NC)"
	@echo "$(RED)This will remove ALL Docker containers, images, volumes, and networks!$(NC)"
	@echo "$(RED)This affects ALL Docker projects on your system, not just this one!$(NC)"
	@echo "$(RED)Type 'yes' to confirm: $(NC)"
	@read -r response && \
	if [ "$$response" = "yes" ]; then \
		./scripts/deployment/docker-cleanup.sh; \
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

# Individual MCP Service Commands
start-mcp-organization: ## Start only MCP Organization service
	@echo "$(BLUE)🏢 Starting MCP Organization service...$(NC)"
	docker-compose up -d mcp-organization
	@echo "$(GREEN)✅ MCP Organization service started$(NC)"

stop-mcp-organization: ## Stop MCP Organization service
	@echo "$(BLUE)🛑 Stopping MCP Organization service...$(NC)"
	docker-compose stop mcp-organization
	@echo "$(GREEN)✅ MCP Organization service stopped$(NC)"

start-mcp-controller: ## Start only MCP Controller service
	@echo "$(BLUE)⚔️ Starting MCP Controller service...$(NC)"
	docker-compose up -d postgres redis mcp-organization mcp-llm mcp-controller
	@echo "$(GREEN)✅ MCP Controller service started$(NC)"

stop-mcp-controller: ## Stop MCP Controller service
	@echo "$(BLUE)🛑 Stopping MCP Controller service...$(NC)"
	docker-compose stop mcp-controller
	@echo "$(GREEN)✅ MCP Controller service stopped$(NC)"

start-mcp-llm: ## Start only MCP LLM service
	@echo "$(BLUE)🤖 Starting MCP LLM service...$(NC)"
	docker-compose up -d redis mcp-llm
	@echo "$(GREEN)✅ MCP LLM service started$(NC)"

stop-mcp-llm: ## Stop MCP LLM service
	@echo "$(BLUE)🛑 Stopping MCP LLM service...$(NC)"
	docker-compose stop mcp-llm
	@echo "$(GREEN)✅ MCP LLM service stopped$(NC)"

start-mcp-debate: ## Start only MCP Debate service (alias for controller)
	@$(MAKE) start-mcp-controller

stop-mcp-debate: ## Stop MCP Debate service (alias for controller)
	@$(MAKE) stop-mcp-controller

start-mcp-rag: ## Start only MCP RAG service
	@echo "$(BLUE)🔍 Starting MCP RAG service...$(NC)"
	docker-compose up -d qdrant redis mcp-llm mcp-rag
	@echo "$(GREEN)✅ MCP RAG service started$(NC)"

stop-mcp-rag: ## Stop MCP RAG service
	@echo "$(BLUE)🛑 Stopping MCP RAG service...$(NC)"
	docker-compose stop mcp-rag
	@echo "$(GREEN)✅ MCP RAG service stopped$(NC)"

start-mcp-template: ## Start only MCP Template service
	@echo "$(BLUE)📋 Starting MCP Template service...$(NC)"
	docker-compose up -d postgres mcp-organization mcp-template
	@echo "$(GREEN)✅ MCP Template service started$(NC)"

stop-mcp-template: ## Stop MCP Template service
	@echo "$(BLUE)🛑 Stopping MCP Template service...$(NC)"
	docker-compose stop mcp-template
	@echo "$(GREEN)✅ MCP Template service stopped$(NC)"

restart-mcp-%: ## Restart specific MCP service (e.g., make restart-mcp-llm)
	@echo "$(BLUE)🔄 Restarting MCP $* service...$(NC)"
	@$(MAKE) stop-mcp-$*
	@sleep 2
	@$(MAKE) start-mcp-$*
	@echo "$(GREEN)✅ MCP $* service restarted$(NC)"

# Development helpers
shell-postgres: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U postgres

shell-redis: ## Open Redis CLI
	docker-compose exec redis redis-cli

inspect-debates: ## Show debates in database
	docker-compose exec postgres psql -U postgres -d debate_db -c "SELECT * FROM debates;"

# Evidence collection for testing
collect-evidence: ## Collect evidence of working system
	@echo "$(BLUE)📸 Collecting evidence...$(NC)"
	@mkdir -p evidence/$(shell date +%Y%m%d_%H%M%S)
	@echo "$(YELLOW)Taking screenshots and collecting data...$(NC)"
	cd playwright-tests && npm run screenshots
	@echo "$(GREEN)✅ Evidence collected in evidence/ directory$(NC)"

# Troubleshooting commands
fix-ui: ## Fix common UI issues
	@echo "$(BLUE)🔧 Fixing common UI issues...$(NC)"
	@echo "$(YELLOW)Restarting UI service...$(NC)"
	@pkill -f "npm run dev" || true
	@sleep 2
	@$(MAKE) start-ui &
	@echo "$(GREEN)✅ UI restarted$(NC)"

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

# GitHub Integration Testing Commands
test-setup: ## Set up test environment
	@echo "$(BLUE)🔧 Setting up test environment...$(NC)"
	@chmod +x .github/tests/setup_test_env.sh
	@./.github/tests/setup_test_env.sh
	@echo "$(GREEN)✅ Test environment ready$(NC)"

test-unit: ## Run unit tests for GitHub integration
	@echo "$(BLUE)🧪 Running unit tests...$(NC)"
	@cd .github/tests && python -m pytest test_*.py -v --cov=../scripts --cov-report=html --html=../test-results/unit/report.html --self-contained-html
	@echo "$(GREEN)✅ Unit tests complete. Report: test-results/unit/report.html$(NC)"

test-integration: ## Run integration tests
	@echo "$(BLUE)🔗 Running integration tests...$(NC)"
	@cd .github/tests && python -m pytest integration/ -v -m integration --html=../test-results/integration/report.html --self-contained-html
	@echo "$(GREEN)✅ Integration tests complete$(NC)"

test-api: ## Test API endpoints with curl
	@echo "$(BLUE)🌐 Testing API endpoints...$(NC)"
	@chmod +x .github/tests/api_tests.sh
	@./.github/tests/api_tests.sh
	@echo "$(GREEN)✅ API tests complete$(NC)"

test-docker: ## Test Docker containers and health
	@echo "$(BLUE)🐳 Testing Docker containers...$(NC)"
	@chmod +x .github/tests/test_docker.sh
	@./.github/tests/test_docker.sh
	@echo "$(GREEN)✅ Docker tests complete$(NC)"

test-e2e-full: ## Run full end-to-end tests
	@echo "$(BLUE)🚀 Running full E2E tests...$(NC)"
	@cd .github/tests && python -m pytest e2e/ -v --html=../test-results/e2e/report.html --self-contained-html
	@echo "$(GREEN)✅ E2E tests complete$(NC)"

test-performance: ## Run performance and load tests
	@echo "$(BLUE)⚡ Running performance tests...$(NC)"
	@cd .github/tests && python -m pytest performance/ -v --benchmark-only --benchmark-autosave
	@echo "$(GREEN)✅ Performance tests complete$(NC)"

test-security: ## Run security tests
	@echo "$(BLUE)🔒 Running security tests...$(NC)"
	@bandit -r .github/scripts -f json -o test-results/security/bandit-report.json
	@safety check --json > test-results/security/safety-report.json || true
	@echo "$(GREEN)✅ Security tests complete$(NC)"

test-all-github: test-setup test-unit test-integration test-api test-docker test-e2e-full test-performance test-security ## Run all GitHub integration tests
	@echo "$(GREEN)✅ All GitHub integration tests complete!$(NC)"
	@$(MAKE) test-report

test-report: ## Generate test report with evidence
	@echo "$(BLUE)📊 Generating test report...$(NC)"
	@mkdir -p test-evidence/reports
	@echo "# Test Report - $(shell date)" > test-evidence/reports/summary.md
	@echo "" >> test-evidence/reports/summary.md
	@echo "## Test Results Summary" >> test-evidence/reports/summary.md
	@echo "" >> test-evidence/reports/summary.md
	@if [ -f test-results/unit/report.html ]; then echo "- ✅ Unit Tests: [View Report](../test-results/unit/report.html)" >> test-evidence/reports/summary.md; fi
	@if [ -f test-results/integration/report.html ]; then echo "- ✅ Integration Tests: [View Report](../test-results/integration/report.html)" >> test-evidence/reports/summary.md; fi
	@if [ -f test-evidence/api/api_test_results_*.txt ]; then echo "- ✅ API Tests: Completed" >> test-evidence/reports/summary.md; fi
	@if [ -f test-results/e2e/report.html ]; then echo "- ✅ E2E Tests: [View Report](../test-results/e2e/report.html)" >> test-evidence/reports/summary.md; fi
	@echo "" >> test-evidence/reports/summary.md
	@echo "## Evidence Location" >> test-evidence/reports/summary.md
	@echo "- Screenshots: test-evidence/screenshots/" >> test-evidence/reports/summary.md
	@echo "- Logs: test-evidence/logs/" >> test-evidence/reports/summary.md
	@echo "- API Responses: test-evidence/api/" >> test-evidence/reports/summary.md
	@echo "$(GREEN)✅ Test report generated: test-evidence/reports/summary.md$(NC)"

test-github-quick: ## Quick smoke test for GitHub integration
	@echo "$(BLUE)🚀 Running quick GitHub integration tests...$(NC)"
	@# Check if test environment is set up
	@if [ ! -f .env.test ]; then $(MAKE) test-setup; fi
	@# Run basic health checks
	@echo "Testing webhook handler..."
	@curl -s -f http://localhost:8080/health || echo "$(YELLOW)⚠️  Webhook handler not running$(NC)"
	@echo "Testing metrics..."
	@curl -s -f http://localhost:9090/metrics || echo "$(YELLOW)⚠️  Metrics not available$(NC)"
	@# Run quick unit tests
	@cd .github/tests && python -m pytest test_webhook_handler.py -v -k "test_webhook_signature_validation"
	@echo "$(GREEN)✅ Quick tests complete$(NC)"

test-playwright-github: ## Run Playwright tests for GitHub UI
	@echo "$(BLUE)🎭 Running Playwright tests for GitHub integration...$(NC)"
	@cd .github/tests && npx playwright test --project=chromium
	@echo "$(GREEN)✅ Playwright tests complete$(NC)"

test-github-monitoring: ## Test monitoring stack (Prometheus, Grafana, Loki)
	@echo "$(BLUE)📊 Testing monitoring stack...$(NC)"
	@echo -n "Prometheus: "
	@curl -s http://localhost:9091/api/v1/query?query=up > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(RED)✗ Not running$(NC)"
	@echo -n "Grafana: "
	@curl -s http://localhost:3000/api/health > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(RED)✗ Not running$(NC)"
	@echo -n "Loki: "
	@curl -s http://localhost:3100/ready > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(RED)✗ Not running$(NC)"
	@echo "$(GREEN)✅ Monitoring stack test complete$(NC)"

# Code Quality and Linting Commands
lint-all: ## Run all linting checks
	@echo "$(BLUE)🔍 Running comprehensive linting...$(NC)"
	@$(MAKE) lint-java
	@$(MAKE) lint-frontend
	@$(MAKE) lint-config
	@$(MAKE) lint-docs
	@echo "$(GREEN)✅ All linting checks complete!$(NC)"

lint-java: ## Lint Java code (Checkstyle, SpotBugs, PMD)
	@echo "$(BLUE)☕ Linting Java code...$(NC)"
	@echo "$(YELLOW)Running Checkstyle...$(NC)"
	@mvn checkstyle:check -q || (echo "$(RED)✗ Checkstyle issues found$(NC)" && exit 1)
	@echo "$(GREEN)✓ Checkstyle passed$(NC)"
	@echo "$(YELLOW)Running SpotBugs...$(NC)"
	@mvn spotbugs:check -q || (echo "$(RED)✗ SpotBugs issues found$(NC)" && exit 1)
	@echo "$(GREEN)✓ SpotBugs passed$(NC)"
	@echo "$(YELLOW)Running PMD...$(NC)"
	@mvn pmd:check -q || (echo "$(RED)✗ PMD issues found$(NC)" && exit 1)
	@echo "$(GREEN)✓ PMD passed$(NC)"
	@echo "$(GREEN)✅ Java linting complete$(NC)"

lint-java-report: ## Generate Java linting reports
	@echo "$(BLUE)📊 Generating Java linting reports...$(NC)"
	@mkdir -p .linting/reports/java
	@mvn checkstyle:checkstyle -q
	@mvn spotbugs:spotbugs -q
	@mvn pmd:pmd -q
	@echo "$(GREEN)✅ Java reports generated in target/site/$(NC)"

lint-frontend: ## Lint React TypeScript code
	@echo "$(BLUE)⚛️ Linting frontend code...$(NC)"
	@if [ ! -d "debate-ui/node_modules" ]; then \
		echo "$(YELLOW)Installing frontend dependencies...$(NC)"; \
		cd debate-ui && npm install; \
	fi
	@echo "$(YELLOW)Running ESLint...$(NC)"
	@cd debate-ui && npm run lint:check || (echo "$(RED)✗ ESLint issues found$(NC)" && exit 1)
	@echo "$(GREEN)✓ ESLint passed$(NC)"
	@echo "$(YELLOW)Running Prettier check...$(NC)"
	@cd debate-ui && npm run format:check || (echo "$(RED)✗ Prettier formatting issues found$(NC)" && exit 1)
	@echo "$(GREEN)✓ Prettier check passed$(NC)"
	@echo "$(YELLOW)Running TypeScript check...$(NC)"
	@cd debate-ui && npm run type-check || (echo "$(RED)✗ TypeScript issues found$(NC)" && exit 1)
	@echo "$(GREEN)✓ TypeScript check passed$(NC)"
	@echo "$(GREEN)✅ Frontend linting complete$(NC)"

lint-config: ## Lint configuration files (YAML, JSON, Docker)
	@echo "$(BLUE)⚙️ Linting configuration files...$(NC)"
	@echo "$(YELLOW)Checking YAML files...$(NC)"
	@if command -v yamllint >/dev/null 2>&1; then \
		yamllint -c .linting/config/yaml-lint.yml . || (echo "$(RED)✗ YAML linting issues found$(NC)" && exit 1); \
		echo "$(GREEN)✓ YAML files passed$(NC)"; \
	else \
		echo "$(YELLOW)⚠️ yamllint not installed, skipping YAML checks$(NC)"; \
	fi
	@echo "$(YELLOW)Checking JSON files...$(NC)"
	@find . -name "*.json" -not -path "./node_modules/*" -not -path "./target/*" -not -path "./build/*" | while read -r file; do \
		if ! python -m json.tool "$$file" > /dev/null 2>&1; then \
			echo "$(RED)✗ Invalid JSON: $$file$(NC)"; \
			exit 1; \
		fi; \
	done && echo "$(GREEN)✓ JSON files passed$(NC)"
	@echo "$(YELLOW)Checking Dockerfile syntax...$(NC)"
	@if command -v hadolint >/dev/null 2>&1; then \
		find . -name "Dockerfile*" -not -path "./node_modules/*" | while read -r file; do \
			hadolint "$$file" || (echo "$(RED)✗ Dockerfile issues in $$file$(NC)" && exit 1); \
		done && echo "$(GREEN)✓ Dockerfiles passed$(NC)"; \
	else \
		echo "$(YELLOW)⚠️ hadolint not installed, skipping Dockerfile checks$(NC)"; \
	fi
	@echo "$(GREEN)✅ Configuration linting complete$(NC)"

lint-docs: ## Lint documentation (Markdown)
	@echo "$(BLUE)📚 Linting documentation...$(NC)"
	@echo "$(YELLOW)Checking Markdown files...$(NC)"
	@if command -v markdownlint >/dev/null 2>&1; then \
		markdownlint -c .linting/docs/markdownlint.json **/*.md || (echo "$(RED)✗ Markdown linting issues found$(NC)" && exit 1); \
		echo "$(GREEN)✓ Markdown files passed$(NC)"; \
	else \
		echo "$(YELLOW)⚠️ markdownlint not installed, skipping Markdown checks$(NC)"; \
	fi
	@echo "$(YELLOW)Checking for broken links...$(NC)"
	@if command -v markdown-link-check >/dev/null 2>&1; then \
		find . -name "*.md" -not -path "./node_modules/*" -not -path "./target/*" | while read -r file; do \
			markdown-link-check "$$file" -c .linting/docs/link-check.json || (echo "$(RED)✗ Broken links in $$file$(NC)" && exit 1); \
		done && echo "$(GREEN)✓ Link check passed$(NC)"; \
	else \
		echo "$(YELLOW)⚠️ markdown-link-check not installed, skipping link checks$(NC)"; \
	fi
	@echo "$(GREEN)✅ Documentation linting complete$(NC)"

lint-fix: ## Auto-fix linting issues where possible
	@echo "$(BLUE)🔧 Auto-fixing linting issues...$(NC)"
	@echo "$(YELLOW)Fixing frontend code...$(NC)"
	@cd debate-ui && npm run lint:fix || true
	@cd debate-ui && npm run format || true
	@echo "$(GREEN)✓ Frontend auto-fixes applied$(NC)"
	@echo "$(YELLOW)Note: Java issues need manual fixing$(NC)"
	@echo "$(GREEN)✅ Auto-fix complete$(NC)"

lint-report: ## Generate comprehensive linting report
	@echo "$(BLUE)📊 Generating comprehensive linting report...$(NC)"
	@mkdir -p .linting/reports
	@echo "# Linting Report - $(shell date)" > .linting/reports/summary.md
	@echo "" >> .linting/reports/summary.md
	@echo "## Java Linting" >> .linting/reports/summary.md
	@$(MAKE) lint-java-report || echo "- ❌ Java linting failed" >> .linting/reports/summary.md
	@echo "- ✅ Java linting completed" >> .linting/reports/summary.md
	@echo "" >> .linting/reports/summary.md
	@echo "## Frontend Linting" >> .linting/reports/summary.md
	@$(MAKE) lint-frontend || echo "- ❌ Frontend linting failed" >> .linting/reports/summary.md
	@echo "- ✅ Frontend linting completed" >> .linting/reports/summary.md
	@echo "" >> .linting/reports/summary.md
	@echo "## Configuration Linting" >> .linting/reports/summary.md
	@$(MAKE) lint-config || echo "- ❌ Configuration linting failed" >> .linting/reports/summary.md
	@echo "- ✅ Configuration linting completed" >> .linting/reports/summary.md
	@echo "" >> .linting/reports/summary.md
	@echo "## Documentation Linting" >> .linting/reports/summary.md
	@$(MAKE) lint-docs || echo "- ❌ Documentation linting failed" >> .linting/reports/summary.md
	@echo "- ✅ Documentation linting completed" >> .linting/reports/summary.md
	@echo "$(GREEN)✅ Linting report generated: .linting/reports/summary.md$(NC)"

lint-setup: ## Install linting tools
	@echo "$(BLUE)🔧 Installing linting tools...$(NC)"
	@echo "$(YELLOW)Installing Node.js linting tools...$(NC)"
	@npm install -g markdownlint-cli markdown-link-check
	@echo "$(YELLOW)Installing Python linting tools...$(NC)"
	@pip install yamllint
	@echo "$(YELLOW)Installing Docker linting tools...$(NC)"
	@if command -v brew >/dev/null 2>&1; then \
		brew install hadolint; \
	elif command -v apt-get >/dev/null 2>&1; then \
		wget -O /tmp/hadolint https://github.com/hadolint/hadolint/releases/latest/download/hadolint-Linux-x86_64 && \
		chmod +x /tmp/hadolint && \
		sudo mv /tmp/hadolint /usr/local/bin/hadolint; \
	else \
		echo "$(YELLOW)⚠️ Please install hadolint manually$(NC)"; \
	fi
	@echo "$(GREEN)✅ Linting tools installation complete$(NC)"

lint-service-%: ## Lint specific service (e.g., make lint-service-mcp-llm)
	@echo "$(BLUE)🔍 Linting service: $*$(NC)"
	@if [ -d "$*" ]; then \
		cd $* && mvn checkstyle:check spotbugs:check pmd:check -q; \
		echo "$(GREEN)✅ Service $* linting complete$(NC)"; \
	else \
		echo "$(RED)✗ Service directory $* not found$(NC)"; \
		exit 1; \
	fi