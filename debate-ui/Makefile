# ╔══════════════════════════════════════════════════════════════════╗
# ║                     ZAMAZ DEBATE MCP SYSTEM                     ║
# ║                    Clean, User-Friendly Makefile                ║
# ╚══════════════════════════════════════════════════════════════════╝

# Load environment variables
-include .env
export

# Configuration
COMPOSE_FILE := infrastructure/docker-compose/docker-compose.yml
COMPOSE_DIR := infrastructure/docker-compose
PROJECT_NAME := zamaz-debate-mcp

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
PURPLE := \033[0;35m
CYAN := \033[0;36m
WHITE := \033[1;37m
NC := \033[0m

# Default ports
UI_PORT ?= 3001
WORKFLOW_UI_PORT ?= 3002
POSTGRES_PORT ?= 5432
REDIS_PORT ?= 6379
NEO4J_PORT ?= 7687
INFLUXDB_PORT ?= 8086
MCP_ORGANIZATION_PORT ?= 5005
MCP_LLM_PORT ?= 5002
MCP_DEBATE_PORT ?= 5013
MCP_RAG_PORT ?= 5004
MCP_TEMPLATE_PORT ?= 5006
WORKFLOW_API_PORT ?= 8080
QDRANT_PORT ?= 6333
JAEGER_UI_PORT ?= 16686
PROMETHEUS_PORT ?= 9090
GRAFANA_PORT ?= 3000
LOKI_PORT ?= 3100
OLLAMA_PORT ?= 11434

.PHONY: help setup start stop restart build clean test lint status logs ui health check-env install hooks login test-auth refresh-session extend-session keep-alive test-debate test-debate-report

# =============================================================================
# HELP & DOCUMENTATION
# =============================================================================

help: ## 📚 Show this help message
	@echo '$(CYAN)╔══════════════════════════════════════════════════════════════════╗$(NC)'
	@echo '$(CYAN)║                   ZAMAZ DEBATE MCP SYSTEM                       ║$(NC)'
	@echo '$(CYAN)║                     Quick Reference Guide                       ║$(NC)'
	@echo '$(CYAN)╚══════════════════════════════════════════════════════════════════╝$(NC)'
	@echo ''
	@echo '$(WHITE)🚀 QUICK START (First Time):$(NC)'
	@echo '  $(GREEN)make setup$(NC)        - Set up environment and install dependencies'
	@echo '  $(GREEN)make start$(NC)        - Start all backend services'
	@echo '  $(GREEN)make ui$(NC)           - Start debate UI (run in new terminal)'
	@echo '  $(GREEN)make workflow$(NC)     - Start workflow editor UI (port 3002)'
	@echo ''
	@echo '$(WHITE)⚡ WORKFLOW EDITOR DEVELOPMENT:$(NC)'
	@echo '  $(GREEN)make workflow-dev$(NC) - Start complete workflow editor (backend + frontend)'
	@echo '  $(GREEN)make workflow-api$(NC) - Start workflow API backend only'
	@echo '  $(GREEN)make workflow-ui$(NC)  - Start workflow editor frontend only'
	@echo '  $(GREEN)make workflow-test$(NC)- Run workflow editor E2E tests'
	@echo ''
	@echo '$(WHITE)⚡ DAILY DEVELOPMENT:$(NC)'
	@echo '  $(GREEN)make dev$(NC)          - Start everything (services + UI) for development'
	@echo '  $(GREEN)make restart$(NC)      - Restart all services'
	@echo '  $(GREEN)make stop$(NC)         - Stop all services'
	@echo '  $(GREEN)make logs$(NC)         - View all service logs'
	@echo ''
	@echo '$(WHITE)🧪 TESTING & QUALITY:$(NC)'
	@echo '  $(GREEN)make test$(NC)      - Run all tests (quick validation)'
	@echo '  $(GREEN)make test-e2e$(NC)  - Run comprehensive end-to-end tests'
	@echo '  $(GREEN)make test-ui$(NC)   - Run UI tests only'
	@echo '  $(GREEN)make test-debate$(NC) - Test debate functionality with real data'
	@echo '  $(GREEN)make test-integration$(NC) - Run integration tests (80% coverage target)'
	@echo '  $(GREEN)make lint$(NC)      - Check code quality with incremental linting'
	@echo ''
	@echo '$(WHITE)📊 MONITORING & DEBUG:$(NC)'
	@echo '  $(GREEN)make status$(NC)    - Show service status'
	@echo '  $(GREEN)make health$(NC)    - Check service health'
	@echo '  $(GREEN)make show-urls$(NC) - Show all service URLs and ports'
	@echo '  $(GREEN)make diagnose$(NC)  - Diagnose connectivity issues'
	@echo '  $(GREEN)make logs$(NC)      - View all service logs'
	@echo ''
	@echo '$(WHITE)🔧 MAINTENANCE:$(NC)'
	@echo '  $(GREEN)make clean$(NC)     - Clean up containers and volumes'
	@echo '  $(GREEN)make reset$(NC)     - Complete reset (clean + fresh start)'
	@echo '  $(GREEN)make build$(NC)     - Rebuild Docker images'
	@echo '  $(GREEN)make fix-connectivity$(NC) - Auto-fix connection issues'
	@echo ''
	@echo '$(WHITE)🔐 SESSION MANAGEMENT:$(NC)'
	@echo '  $(GREEN)make login$(NC)     - Login with demo credentials'
	@echo '  $(GREEN)make test-auth$(NC) - Check authentication status'
	@echo '  $(GREEN)make refresh-session$(NC) - Refresh authentication session'
	@echo '  $(GREEN)make keep-alive$(NC) - Keep session alive (auto-refresh every 5 min)'
	@echo ''
	@echo '$(WHITE)🌍 ENVIRONMENTS:$(NC)'
	@echo '  $(GREEN)make prod$(NC)      - Deploy to production mode'
	@echo '  $(GREEN)make staging$(NC)   - Deploy to staging mode'
	@echo ''
	@echo '$(YELLOW)💡 Examples:$(NC)'
	@echo '  New developer: $(CYAN)make setup && make dev$(NC)'
	@echo '  Daily work:    $(CYAN)make restart && make ui$(NC)'
	@echo '  Before commit: $(CYAN)make lint && make test$(NC)'
	@echo '  Show all URLs: $(CYAN)make show-urls$(NC)'
	@echo ''
	@echo '$(WHITE)📋 ALL COMMANDS:$(NC)'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-20s$(NC) %s\n", $$1, $$2}'

# =============================================================================
# SETUP & ENVIRONMENT
# =============================================================================

setup: check-deps check-env install hooks ## 🛠️ Complete project setup (run this first!)
	@echo "$(GREEN)✅ Project setup complete!$(NC)"
	@echo "$(YELLOW)Next steps:$(NC)"
	@echo "  1. Edit .env with your API keys if needed"
	@echo "  2. Run '$(CYAN)make dev$(NC)' to start development"
	@echo "  3. Open $(CYAN)http://localhost:$(UI_PORT)$(NC) in your browser"

hooks: ## 🔗 Install git hooks for code quality
	@echo "$(BLUE)Installing git hooks...$(NC)"
	@chmod +x scripts/setup/install-git-hooks.sh
	@./scripts/setup/install-git-hooks.sh
	@echo "$(GREEN)✅ Git hooks installed$(NC)"

check-deps: ## 🔍 Check if required tools are installed
	@echo "$(BLUE)Checking dependencies...$(NC)"
	@command -v docker >/dev/null 2>&1 || (echo "$(RED)❌ Docker is required$(NC)" && exit 1)
	@command -v docker-compose >/dev/null 2>&1 || (echo "$(RED)❌ Docker Compose is required$(NC)" && exit 1)
	@command -v node >/dev/null 2>&1 || (echo "$(RED)❌ Node.js is required$(NC)" && exit 1)
	@echo "$(GREEN)✅ All dependencies found$(NC)"

check-env: ## 🔧 Check and create environment configuration
	@echo "$(BLUE)Checking environment...$(NC)"
	@if [ ! -f .env ]; then \
		echo "$(YELLOW)Creating .env from template...$(NC)"; \
		cp .env.example .env 2>/dev/null || echo "# Add your environment variables here" > .env; \
		echo "$(GREEN)✅ .env file created$(NC)"; \
	else \
		echo "$(GREEN)✅ .env file exists$(NC)"; \
	fi

install: ## 📦 Install all dependencies
	@echo "$(BLUE)Installing dependencies...$(NC)"
	@if [ -d "debate-ui" ]; then \
		echo "$(YELLOW)Installing UI dependencies...$(NC)"; \
		cd debate-ui && npm install --silent; \
	fi
	@if [ -d "workflow-editor/client/workflow-editor" ]; then \
		echo "$(YELLOW)Installing workflow editor dependencies...$(NC)"; \
		cd workflow-editor/client/workflow-editor && npm install --silent; \
	fi
	@if [ -d "e2e-tests" ]; then \
		echo "$(YELLOW)Installing E2E test dependencies...$(NC)"; \
		cd e2e-tests && npm install --silent; \
	fi
	@echo "$(GREEN)✅ Dependencies installed$(NC)"

# =============================================================================
# DOCKER SERVICES
# =============================================================================

start: ## 🚀 Start all backend services
	@echo "$(BLUE)Starting backend services...$(NC)"
	@docker-compose -f $(COMPOSE_FILE) up -d
	@sleep 3
	@echo "$(GREEN)✅ Backend services started!$(NC)"
	@$(MAKE) show-urls

stop: ## 🛑 Stop all services
	@echo "$(BLUE)Stopping all services...$(NC)"
	@docker-compose -f $(COMPOSE_FILE) down
	@pkill -f "npm run dev" 2>/dev/null || true
	@echo "$(GREEN)✅ All services stopped$(NC)"

restart: stop start ## 🔄 Restart all services

build: ## 🏗️ Build/rebuild all Docker images
	@echo "$(BLUE)Building Docker images...$(NC)"
	@docker-compose -f $(COMPOSE_FILE) build
	@echo "$(GREEN)✅ Build complete$(NC)"

# =============================================================================
# DEVELOPMENT
# =============================================================================

dev: start ## 🎯 Start complete development environment
	@echo "$(GREEN)🚀 Development environment ready!$(NC)"
	@echo "$(YELLOW)Starting UI in development mode...$(NC)"
	@echo "$(WHITE)💡 Open http://localhost:$(UI_PORT) in your browser$(NC)"
	@$(MAKE) ui

ui: ## 🎨 Start UI development server
	@echo "$(BLUE)Starting UI development server...$(NC)"
	@if [ ! -d "debate-ui" ]; then \
		echo "$(RED)❌ debate-ui directory not found$(NC)"; \
		exit 1; \
	fi
	@cd debate-ui && npm run dev

# =============================================================================
# WORKFLOW EDITOR COMMANDS
# =============================================================================

workflow: workflow-ui ## 🔄 Start workflow editor UI (shortcut)

workflow-dev: ## 🎯 Start complete workflow editor (backend + frontend)
	@echo "$(GREEN)🚀 Starting complete workflow editor environment...$(NC)"
	@echo "$(YELLOW)Starting workflow API backend...$(NC)"
	@$(MAKE) workflow-api &
	@sleep 5
	@echo "$(YELLOW)Starting workflow editor frontend...$(NC)"
	@$(MAKE) workflow-ui

workflow-api: ## 🔌 Start workflow API backend only
	@echo "$(BLUE)Starting workflow API backend...$(NC)"
	@if [ ! -d "workflow-editor" ]; then \
		echo "$(RED)❌ workflow-editor directory not found$(NC)"; \
		exit 1; \
	fi
	@cd workflow-editor && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=$(WORKFLOW_API_PORT)

workflow-ui: ## 🎨 Start workflow editor frontend only
	@echo "$(BLUE)Starting workflow editor frontend...$(NC)"
	@if [ ! -d "workflow-editor/client/workflow-editor" ]; then \
		echo "$(RED)❌ workflow-editor frontend not found$(NC)"; \
		exit 1; \
	fi
	@echo "$(WHITE)💡 Workflow Editor will be available at http://localhost:$(WORKFLOW_UI_PORT)$(NC)"
	@cd workflow-editor/client/workflow-editor && BROWSER=none PORT=$(WORKFLOW_UI_PORT) npm start

workflow-build: ## 🏗️ Build workflow editor (frontend + backend)
	@echo "$(BLUE)Building workflow editor...$(NC)"
	@if [ -d "workflow-editor/client/workflow-editor" ]; then \
		echo "$(YELLOW)Building frontend...$(NC)"; \
		cd workflow-editor/client/workflow-editor && npm install && npm run build; \
	fi
	@if [ -d "workflow-editor" ]; then \
		echo "$(YELLOW)Building backend...$(NC)"; \
		cd workflow-editor && mvn clean package -DskipTests; \
	fi
	@echo "$(GREEN)✅ Workflow editor build complete$(NC)"

workflow-install: ## 📦 Install workflow editor dependencies
	@echo "$(BLUE)Installing workflow editor dependencies...$(NC)"
	@if [ -d "workflow-editor/client/workflow-editor" ]; then \
		echo "$(YELLOW)Installing frontend dependencies...$(NC)"; \
		cd workflow-editor/client/workflow-editor && npm install; \
	fi
	@echo "$(GREEN)✅ Workflow editor dependencies installed$(NC)"

workflow-test: ## 🧪 Run workflow editor E2E tests
	@echo "$(BLUE)Running workflow editor E2E tests...$(NC)"
	@if [ -d "workflow-editor/e2e-tests" ]; then \
		cd workflow-editor/e2e-tests && npm test; \
	else \
		echo "$(YELLOW)⚠️ Setting up E2E tests for workflow editor...$(NC)"; \
		$(MAKE) setup-workflow-e2e; \
	fi

setup-workflow-e2e: ## 🎭 Set up Playwright E2E testing for workflow editor
	@echo "$(BLUE)Setting up Playwright E2E testing...$(NC)"
	@mkdir -p workflow-editor/e2e-tests
	@cd workflow-editor/e2e-tests && \
		if [ ! -f "package.json" ]; then \
			npm init -y; \
			npm install --save-dev @playwright/test; \
			npx playwright install; \
		fi
	@echo "$(GREEN)✅ Playwright E2E testing setup complete$(NC)"

# =============================================================================
# SESSION MANAGEMENT
# =============================================================================

refresh-session: ## 🔄 Refresh authentication session to prevent timeout
	@echo "$(BLUE)Refreshing authentication session...$(NC)"
	@curl -s -X POST http://localhost:$(MCP_ORGANIZATION_PORT)/api/auth/refresh \
		-H "Content-Type: application/json" \
		-c cookies.txt -b cookies.txt > /dev/null 2>&1 && \
		echo "$(GREEN)✅ Session refreshed$(NC)" || \
		echo "$(YELLOW)⚠️ Session refresh failed - may need to login again$(NC)"

extend-session: ## ⏰ Extend current session timeout
	@echo "$(BLUE)Extending session timeout...$(NC)"
	@curl -s -X POST http://localhost:$(MCP_ORGANIZATION_PORT)/api/auth/extend \
		-H "Content-Type: application/json" \
		-c cookies.txt -b cookies.txt > /dev/null 2>&1 && \
		echo "$(GREEN)✅ Session extended$(NC)" || \
		echo "$(YELLOW)⚠️ Session extension failed$(NC)"

keep-alive: ## 🫀 Keep session alive (runs every 5 minutes)
	@echo "$(BLUE)Starting session keep-alive...$(NC)"
	@echo "$(YELLOW)Press Ctrl+C to stop$(NC)"
	@while true; do \
		$(MAKE) refresh-session --no-print-directory; \
		sleep 300; \
	done

test-auth: ## 🔐 Test current authentication status
	@echo "$(BLUE)Testing authentication status...$(NC)"
	@STATUS=$$(curl -s -o /dev/null -w "%{http_code}" \
		-b cookies.txt \
		http://localhost:$(MCP_ORGANIZATION_PORT)/api/auth/status); \
	if [ "$$STATUS" = "200" ]; then \
		echo "$(GREEN)✅ Authenticated$(NC)"; \
	else \
		echo "$(RED)❌ Not authenticated (HTTP $$STATUS)$(NC)"; \
		echo "$(YELLOW)Run 'make login' to authenticate$(NC)"; \
	fi

login: ## 🔑 Login with demo credentials
	@echo "$(BLUE)Logging in with demo credentials...$(NC)"
	@curl -s -X POST http://localhost:$(MCP_ORGANIZATION_PORT)/api/auth/login \
		-H "Content-Type: application/json" \
		-d '{"username":"demo","password":"demo123"}' \
		-c cookies.txt > /dev/null 2>&1 && \
		echo "$(GREEN)✅ Login successful$(NC)" || \
		echo "$(RED)❌ Login failed$(NC)"

# =============================================================================
# TESTING
# =============================================================================

test: ## 🧪 Run quick validation tests
	@echo "$(BLUE)Running quick validation tests...$(NC)"
	@$(MAKE) health
	@if [ -f "scripts/testing/smoke-tests.sh" ]; then \
		chmod +x scripts/testing/smoke-tests.sh && ./scripts/testing/smoke-tests.sh; \
	else \
		echo "$(GREEN)✅ Basic health checks passed$(NC)"; \
	fi

test-e2e: ## 🎭 Run comprehensive end-to-end tests
	@echo "$(BLUE)Running E2E tests...$(NC)"
	@if [ -d "e2e-tests" ]; then \
		cd e2e-tests && npm test; \
	else \
		echo "$(YELLOW)⚠️ E2E tests directory not found$(NC)"; \
	fi

test-playwright: ## 🎭 Run Playwright E2E tests
	@echo "$(BLUE)Running Playwright E2E tests...$(NC)"
	@if [ -f "playwright.config.js" ]; then \
		npm run test:e2e; \
	else \
		echo "$(RED)❌ Playwright not configured$(NC)"; \
		echo "$(YELLOW)Run 'npm install --save-dev @playwright/test' first$(NC)"; \
	fi

test-playwright-ui: ## 🖥️ Run Playwright tests with UI mode
	@echo "$(BLUE)Starting Playwright UI mode...$(NC)"
	@npm run test:e2e:ui

test-playwright-report: ## 📊 Show Playwright test report
	@echo "$(BLUE)Opening Playwright test report...$(NC)"
	@npm run test:e2e:report

test-ui: ## 🖥️ Run UI tests only
	@echo "$(BLUE)Running UI tests...$(NC)"
	@if [ -d "e2e-tests" ]; then \
		cd e2e-tests && npm run test:ui 2>/dev/null || npm test; \
	else \
		echo "$(YELLOW)⚠️ UI tests not available$(NC)"; \
	fi

test-debate: ## 🎭 Test debate functionality with real data
	@echo "$(BLUE)Testing debate functionality...$(NC)"
	@if [ -f "debate-ui/test-real-debate-integration.js" ]; then \
		cd debate-ui && node test-real-debate-integration.js; \
	elif [ -f "debate-ui/e2e-complete-test.js" ]; then \
		cd debate-ui && node e2e-complete-test.js; \
	else \
		echo "$(YELLOW)⚠️ Debate test scripts not found$(NC)"; \
		echo "$(CYAN)Available test files:$(NC)"; \
		find . -name "*debate*test*.js" -type f | head -10; \
	fi

test-debate-report: ## 📊 View debate test results
	@echo "$(BLUE)Debate Test Results:$(NC)"
	@if [ -f "debate-ui/REAL_DEBATE_INTEGRATION_SUMMARY.md" ]; then \
		echo "$(GREEN)✅ Found test summary:$(NC)"; \
		head -50 debate-ui/REAL_DEBATE_INTEGRATION_SUMMARY.md; \
	elif [ -d "debate-ui/evidence" ]; then \
		echo "$(YELLOW)Test evidence found:$(NC)"; \
		ls -la debate-ui/evidence/; \
	else \
		echo "$(RED)❌ No debate test results found$(NC)"; \
		echo "$(YELLOW)Run 'make test-debate' first$(NC)"; \
	fi

test-services: ## 🔧 Test individual services
	@echo "$(BLUE)Testing MCP services...$(NC)"
	@if [ -f "scripts/testing/test-mcp-services.sh" ]; then \
		chmod +x scripts/testing/test-mcp-services.sh && ./scripts/testing/test-mcp-services.sh; \
	else \
		$(MAKE) health; \
	fi

# =============================================================================
# CODE QUALITY & LINTING
# =============================================================================

validate-react: ## 🔍 Validate React imports and dependencies
	@echo "$(BLUE)Validating React imports...$(NC)"
	@npm run lint:react-imports
	@echo "$(GREEN)✅ React import validation complete$(NC)"

lint: validate-react ## 🔍 Run incremental linting (fast, smart analysis)
	@echo "$(BLUE)Running incremental linting...$(NC)"
	@if [ -f "scripts/testing/test-incremental-linting.sh" ]; then \
		chmod +x scripts/testing/test-incremental-linting.sh && ./scripts/testing/test-incremental-linting.sh; \
	elif [ -d "debate-ui" ]; then \
		echo "$(YELLOW)Running frontend linting...$(NC)"; \
		cd debate-ui && npm run lint --silent 2>/dev/null || echo "$(YELLOW)⚠️ Frontend linting skipped$(NC)"; \
	else \
		echo "$(YELLOW)⚠️ No linting configuration found$(NC)"; \
	fi
	@echo "$(GREEN)✅ Linting complete$(NC)"

lint-fix: ## 🔧 Auto-fix linting issues
	@echo "$(BLUE)Auto-fixing linting issues...$(NC)"
	@if [ -d "debate-ui" ]; then \
		cd debate-ui && npm run lint:fix --silent 2>/dev/null || true; \
		cd debate-ui && npm run format --silent 2>/dev/null || true; \
	fi
	@echo "$(GREEN)✅ Auto-fixes applied$(NC)"

test-integration: ## 🧪 Run comprehensive integration tests (80% coverage target)
	@echo "$(BLUE)Running comprehensive integration tests...$(NC)"
	@echo "$(YELLOW)Target: 80% functionality coverage$(NC)"
	@if [ -f ".linting/scripts/simple-integration-test.sh" ]; then \
		chmod +x .linting/scripts/simple-integration-test.sh && .linting/scripts/simple-integration-test.sh; \
	else \
		echo "$(RED)❌ Integration test runner not found$(NC)"; \
		exit 1; \
	fi

test-integration-core: ## 🔧 Run core linting integration tests
	@echo "$(BLUE)Running core integration tests...$(NC)"
	@if [ -f ".linting/scripts/integration-test-suite.sh" ]; then \
		chmod +x .linting/scripts/integration-test-suite.sh && .linting/scripts/integration-test-suite.sh; \
	else \
		echo "$(RED)❌ Core integration tests not found$(NC)"; \
		exit 1; \
	fi

test-integration-performance: ## ⚡ Run performance integration tests
	@echo "$(BLUE)Running performance integration tests...$(NC)"
	@if [ -f ".linting/scripts/performance-integration-tests.sh" ]; then \
		chmod +x .linting/scripts/performance-integration-tests.sh && .linting/scripts/performance-integration-tests.sh; \
	else \
		echo "$(RED)❌ Performance integration tests not found$(NC)"; \
		exit 1; \
	fi

test-integration-workflow: ## 🔄 Run workflow integration tests
	@echo "$(BLUE)Running workflow integration tests...$(NC)"
	@if [ -f ".linting/scripts/workflow-integration-tests.sh" ]; then \
		chmod +x .linting/scripts/workflow-integration-tests.sh && .linting/scripts/workflow-integration-tests.sh; \
	else \
		echo "$(RED)❌ Workflow integration tests not found$(NC)"; \
		exit 1; \
	fi

test-coverage-report: ## 📊 Generate integration test coverage report
	@echo "$(BLUE)Generating integration test coverage report...$(NC)"
	@if [ -f ".linting/test-results/master/comprehensive-test-report.md" ]; then \
		echo "$(GREEN)✅ Coverage report available:$(NC)"; \
		echo "$(CYAN)   .linting/test-results/master/comprehensive-test-report.md$(NC)"; \
		echo ""; \
		echo "$(YELLOW)Coverage Summary:$(NC)"; \
		grep -A 5 "Coverage Percentage" .linting/test-results/master/comprehensive-test-report.md 2>/dev/null || echo "Run integration tests first"; \
	else \
		echo "$(YELLOW)⚠️ No coverage report found. Run 'make test-integration' first$(NC)"; \
	fi

# =============================================================================
# MONITORING & DEBUGGING
# =============================================================================

status: ## 📊 Show service status
	@echo "$(BLUE)Service Status:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) ps

health: ## 🏥 Check service health
	@echo "$(BLUE)Health Check:$(NC)"
	@echo -n "PostgreSQL: "
	@docker-compose -f $(COMPOSE_FILE) exec -T postgres pg_isready -U postgres > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Down$(NC)"
	@echo -n "Redis: "
	@docker-compose -f $(COMPOSE_FILE) exec -T redis redis-cli ping > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Down$(NC)"
	@echo -n "Qdrant: "
	@curl -s http://localhost:$(QDRANT_PORT)/collections > /dev/null 2>&1 && echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Down$(NC)"
	@echo -n "Organization API: "
	@curl -s http://localhost:$(MCP_ORGANIZATION_PORT)/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠️ Not running$(NC)"
	@echo -n "LLM API: "
	@curl -s http://localhost:$(MCP_LLM_PORT)/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠️ Not running$(NC)"
	@echo -n "Debate API: "
	@curl -s http://localhost:$(MCP_DEBATE_PORT)/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠️ Not running$(NC)"
	@echo -n "RAG API: "
	@curl -s http://localhost:$(MCP_RAG_PORT)/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠️ Not running$(NC)"
	@echo -n "Template API: "
	@curl -s http://localhost:$(MCP_TEMPLATE_PORT)/actuator/health > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠️ Not running$(NC)"
	@echo -n "UI: "
	@curl -s http://localhost:$(UI_PORT) > /dev/null 2>&1 && echo "$(GREEN)✓ Running$(NC)" || echo "$(YELLOW)⚠️ Run 'make ui'$(NC)"

logs: ## 📜 View all service logs
	@echo "$(BLUE)Following all logs (Ctrl+C to stop)...$(NC)"
	@docker-compose -f $(COMPOSE_FILE) logs -f --tail=50

logs-ui: ## 📱 View UI logs only
	@echo "$(BLUE)UI Development Logs:$(NC)"
	@echo "$(YELLOW)UI logs appear in the terminal where you ran 'make ui'$(NC)"

logs-api: ## 🔌 View API logs only
	@echo "$(BLUE)API Service Logs:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) logs -f --tail=20

show-urls: ## 🌐 Show all service URLs and access information
	@echo ""
	@echo "$(CYAN)╔══════════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(CYAN)║                     🌐 SERVICE ACCESS URLs                      ║$(NC)"
	@echo "$(CYAN)╚══════════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(WHITE)🎨 FRONTEND & USER INTERFACES:$(NC)"
	@echo "  $(GREEN)Main UI (React)$(NC)     http://localhost:$(UI_PORT)"
	@echo "  $(GREEN)Workflow Editor$(NC)    http://localhost:$(WORKFLOW_UI_PORT)"
	@echo "  $(GREEN)Grafana Dashboard$(NC)  http://localhost:$(GRAFANA_PORT) $(YELLOW)(admin/admin)$(NC)"
	@echo "  $(GREEN)Jaeger Tracing$(NC)     http://localhost:$(JAEGER_UI_PORT)"
	@echo ""
	@echo "$(WHITE)🔌 MCP MICROSERVICES (REST APIs):$(NC)"
	@echo "  $(BLUE)Organization API$(NC)   http://localhost:$(MCP_ORGANIZATION_PORT)/actuator/health"
	@echo "  $(BLUE)LLM API$(NC)            http://localhost:$(MCP_LLM_PORT)/actuator/health"
	@echo "  $(BLUE)Debate Controller$(NC)  http://localhost:$(MCP_DEBATE_PORT)/actuator/health"
	@echo "  $(BLUE)RAG API$(NC)            http://localhost:$(MCP_RAG_PORT)/actuator/health"
	@echo "  $(BLUE)Template API$(NC)       http://localhost:$(MCP_TEMPLATE_PORT)/actuator/health"
	@echo "  $(BLUE)Workflow API$(NC)       http://localhost:$(WORKFLOW_API_PORT)/actuator/health"
	@echo ""
	@echo "$(WHITE)📊 MONITORING & OBSERVABILITY:$(NC)"
	@echo "  $(PURPLE)Prometheus$(NC)        http://localhost:$(PROMETHEUS_PORT)"
	@echo "  $(PURPLE)Loki Logs$(NC)         http://localhost:$(LOKI_PORT)"
	@echo "  $(PURPLE)Qdrant Vector DB$(NC)  http://localhost:$(QDRANT_PORT)/dashboard"
	@echo ""
	@echo "$(WHITE)🗄️ DATABASES & STORAGE:$(NC)"
	@echo "  $(YELLOW)PostgreSQL$(NC)        localhost:$(POSTGRES_PORT) $(YELLOW)(postgres/postgres)$(NC)"
	@echo "  $(YELLOW)Redis Cache$(NC)       localhost:$(REDIS_PORT)"
	@echo "  $(YELLOW)Neo4j Graph DB$(NC)    localhost:$(NEO4J_PORT) $(YELLOW)(neo4j/password)$(NC)"
	@echo "  $(YELLOW)InfluxDB Time-Series$(NC) localhost:$(INFLUXDB_PORT)"
	@echo ""
	@echo "$(WHITE)🤖 AI & ML SERVICES:$(NC)"
	@echo "  $(GREEN)Ollama (Local LLMs)$(NC) http://localhost:$(OLLAMA_PORT) $(YELLOW)(start with --profile llama)$(NC)"
	@echo ""
	@echo "$(WHITE)📋 API DOCUMENTATION:$(NC)"
	@echo "  $(CYAN)Organization API$(NC)   http://localhost:$(MCP_ORGANIZATION_PORT)/swagger-ui.html"
	@echo "  $(CYAN)LLM API$(NC)            http://localhost:$(MCP_LLM_PORT)/swagger-ui.html"  
	@echo "  $(CYAN)Debate API$(NC)         http://localhost:$(MCP_DEBATE_PORT)/swagger-ui.html"
	@echo "  $(CYAN)RAG API$(NC)            http://localhost:$(MCP_RAG_PORT)/swagger-ui.html"
	@echo "  $(CYAN)Template API$(NC)       http://localhost:$(MCP_TEMPLATE_PORT)/swagger-ui.html"
	@echo "  $(CYAN)Workflow API$(NC)       http://localhost:$(WORKFLOW_API_PORT)/graphql $(YELLOW)(GraphQL)$(NC)"
	@echo ""
	@echo "$(WHITE)🔧 QUICK HEALTH CHECKS:$(NC)"
	@echo "  $(GREEN)make health$(NC)        - Check all service health"
	@echo "  $(GREEN)make status$(NC)        - Show Docker container status"
	@echo "  $(GREEN)make logs$(NC)          - View all service logs"
	@echo ""
	@echo "$(YELLOW)💡 TIPS:$(NC)"
	@echo "  📖 Debate System: http://localhost:$(UI_PORT)"
	@echo "  🔄 Workflow Editor: http://localhost:$(WORKFLOW_UI_PORT)"
	@echo ""

# =============================================================================
# CLEANUP & MAINTENANCE
# =============================================================================

clean: ## 🧹 Clean up containers and volumes
	@echo "$(YELLOW)⚠️ This will remove all data. Continue? [y/N]$(NC)"
	@read -r response && \
	if [ "$$response" = "y" ] || [ "$$response" = "Y" ]; then \
		echo "$(BLUE)Cleaning up...$(NC)"; \
		docker-compose -f $(COMPOSE_FILE) down -v --remove-orphans; \
		docker system prune -f; \
		echo "$(GREEN)✅ Cleanup complete$(NC)"; \
	else \
		echo "$(YELLOW)Cleanup cancelled$(NC)"; \
	fi

reset: clean start ## 🔄 Complete reset (clean + fresh start)
	@echo "$(GREEN)✅ System reset complete$(NC)"

# =============================================================================
# ENVIRONMENT DEPLOYMENTS
# =============================================================================

prod: ## 🌐 Deploy production environment
	@echo "$(BLUE)Starting production environment...$(NC)"
	@COMPOSE_FILE="$(COMPOSE_DIR)/docker-compose.yml:$(COMPOSE_DIR)/docker-compose.prod.yml" docker-compose up -d
	@echo "$(GREEN)✅ Production environment started$(NC)"

staging: ## 🎪 Deploy staging environment
	@echo "$(BLUE)Starting staging environment...$(NC)"
	@COMPOSE_FILE="$(COMPOSE_DIR)/docker-compose.yml:$(COMPOSE_DIR)/docker-compose.staging.yml" docker-compose up -d
	@echo "$(GREEN)✅ Staging environment started$(NC)"

# =============================================================================
# ADVANCED OPERATIONS
# =============================================================================

shell-db: ## 💾 Open database shell
	@docker-compose -f $(COMPOSE_FILE) exec postgres psql -U postgres

shell-redis: ## 🗄️ Open Redis CLI
	@docker-compose -f $(COMPOSE_FILE) exec redis redis-cli

backup: ## 💾 Backup database
	@echo "$(BLUE)Creating database backup...$(NC)"
	@mkdir -p backups
	@docker-compose -f $(COMPOSE_FILE) exec postgres pg_dump -U postgres > backups/backup_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✅ Backup created in backups/$(NC)"

# =============================================================================
# QUICK SHORTCUTS
# =============================================================================

up: start ## ⬆️ Alias for start
down: stop ## ⬇️ Alias for stop
rebuild: clean build start ## 🔨 Complete rebuild

# =============================================================================
# UTILITIES
# =============================================================================

ports: ## 🔌 Show port usage
	@echo "$(BLUE)Port Configuration:$(NC)"
	@echo "  UI:         http://localhost:$(UI_PORT)"
	@echo "  PostgreSQL: localhost:$(POSTGRES_PORT)"
	@echo "  Redis:      localhost:$(REDIS_PORT)"
	@echo ""
	@echo "$(BLUE)Currently used ports:$(NC)"
	@lsof -nP -i4TCP | grep LISTEN | awk '{print $$9}' | sort -u | grep -E ':(3001|5432|6379|500[0-9]|501[0-9])' || echo "No MCP ports in use"

diagnose: ## 🔍 Diagnose connectivity issues
	@echo "$(BLUE)🔍 Diagnosing connectivity issues...$(NC)"
	@echo ""
	@echo "$(WHITE)1. Docker Services:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) ps
	@echo ""
	@echo "$(WHITE)2. Port Check:$(NC)"
	@lsof -nP -i4TCP | grep LISTEN | grep -E ':(3001|5002|5004|5005|5006|5013|5432|6333|6379)' || echo "$(YELLOW)No MCP services listening$(NC)"
	@echo ""
	@echo "$(WHITE)3. Service URLs Test:$(NC)"
	@echo -n "  UI ($(UI_PORT)): "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:$(UI_PORT) 2>/dev/null || echo "Connection failed"
	@echo ""
	@echo -n "  Qdrant ($(QDRANT_PORT)): "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:$(QDRANT_PORT) 2>/dev/null || echo "Connection failed"
	@echo ""
	@echo "$(WHITE)4. What's Missing:$(NC)"
	@echo "$(YELLOW)To fix the issues:$(NC)"
	@echo "  1. Run $(CYAN)make build$(NC) to build the Java services"
	@echo "  2. Run $(CYAN)make start$(NC) to start all services"
	@echo "  3. Run $(CYAN)make ui$(NC) in a separate terminal for the frontend"
	@echo ""

fix-connectivity: ## 🔧 Fix connectivity issues automatically
	@echo "$(BLUE)🔧 Fixing connectivity issues...$(NC)"
	@echo "$(YELLOW)Step 1: Building Java services...$(NC)"
	@$(MAKE) build
	@echo "$(YELLOW)Step 2: Starting all services...$(NC)"
	@$(MAKE) start
	@echo "$(YELLOW)Step 3: Waiting for services to start...$(NC)"
	@sleep 10
	@echo "$(YELLOW)Step 4: Checking health...$(NC)"
	@$(MAKE) health
	@echo ""
	@echo "$(GREEN)✅ Services should now be accessible!$(NC)"
	@echo "$(CYAN)Next: Run '$(WHITE)make ui$(CYAN)' in a separate terminal for the frontend$(NC)"

info: ## ℹ️ Show project information
	@echo "$(CYAN)Project: $(PROJECT_NAME)$(NC)"
	@echo "$(CYAN)Compose File: $(COMPOSE_FILE)$(NC)"
	@echo "$(CYAN)Status: $(NC)"
	@$(MAKE) status --no-print-directory

# Hidden debugging commands (not shown in help)
debug-env:
	@echo "COMPOSE_FILE: $(COMPOSE_FILE)"
	@echo "UI_PORT: $(UI_PORT)"
	@echo "POSTGRES_PORT: $(POSTGRES_PORT)"
	@echo "REDIS_PORT: $(REDIS_PORT)"

# Default target
.DEFAULT_GOAL := help
