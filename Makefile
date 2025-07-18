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
POSTGRES_PORT ?= 5432
REDIS_PORT ?= 6379
MCP_ORGANIZATION_PORT ?= 5005
MCP_LLM_PORT ?= 5002
MCP_DEBATE_PORT ?= 5013
MCP_RAG_PORT ?= 5004
MCP_TEMPLATE_PORT ?= 5006
QDRANT_PORT ?= 6333
JAEGER_UI_PORT ?= 16686
PROMETHEUS_PORT ?= 9090
GRAFANA_PORT ?= 3000
LOKI_PORT ?= 3100
OLLAMA_PORT ?= 11434

.PHONY: help setup start stop restart build clean test lint status logs ui health check-env install

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
	@echo '  $(GREEN)make setup$(NC)     - Set up environment and install dependencies'
	@echo '  $(GREEN)make start$(NC)     - Start all backend services'
	@echo '  $(GREEN)make ui$(NC)        - Start UI development server (run in new terminal)'
	@echo ''
	@echo '$(WHITE)⚡ DAILY DEVELOPMENT:$(NC)'
	@echo '  $(GREEN)make dev$(NC)       - Start everything (services + UI) for development'
	@echo '  $(GREEN)make restart$(NC)   - Restart all services'
	@echo '  $(GREEN)make stop$(NC)      - Stop all services'
	@echo '  $(GREEN)make logs$(NC)      - View all service logs'
	@echo ''
	@echo '$(WHITE)🧪 TESTING & QUALITY:$(NC)'
	@echo '  $(GREEN)make test$(NC)      - Run all tests (quick validation)'
	@echo '  $(GREEN)make test-e2e$(NC)  - Run comprehensive end-to-end tests'
	@echo '  $(GREEN)make test-ui$(NC)   - Run UI tests only'
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

setup: check-deps check-env install ## 🛠️ Complete project setup (run this first!)
	@echo "$(GREEN)✅ Project setup complete!$(NC)"
	@echo "$(YELLOW)Next steps:$(NC)"
	@echo "  1. Edit .env with your API keys if needed"
	@echo "  2. Run '$(CYAN)make dev$(NC)' to start development"
	@echo "  3. Open $(CYAN)http://localhost:$(UI_PORT)$(NC) in your browser"

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

test-ui: ## 🖥️ Run UI tests only
	@echo "$(BLUE)Running UI tests...$(NC)"
	@if [ -d "e2e-tests" ]; then \
		cd e2e-tests && npm run test:ui 2>/dev/null || npm test; \
	else \
		echo "$(YELLOW)⚠️ UI tests not available$(NC)"; \
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

lint: ## 🔍 Run incremental linting (fast, smart analysis)
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
	@echo "  $(GREEN)Grafana Dashboard$(NC)  http://localhost:$(GRAFANA_PORT) $(YELLOW)(admin/admin)$(NC)"
	@echo "  $(GREEN)Jaeger Tracing$(NC)     http://localhost:$(JAEGER_UI_PORT)"
	@echo ""
	@echo "$(WHITE)🔌 MCP MICROSERVICES (REST APIs):$(NC)"
	@echo "  $(BLUE)Organization API$(NC)   http://localhost:$(MCP_ORGANIZATION_PORT)/actuator/health"
	@echo "  $(BLUE)LLM API$(NC)            http://localhost:$(MCP_LLM_PORT)/actuator/health"
	@echo "  $(BLUE)Debate Controller$(NC)  http://localhost:$(MCP_DEBATE_PORT)/actuator/health"
	@echo "  $(BLUE)RAG API$(NC)            http://localhost:$(MCP_RAG_PORT)/actuator/health"
	@echo "  $(BLUE)Template API$(NC)       http://localhost:$(MCP_TEMPLATE_PORT)/actuator/health"
	@echo ""
	@echo "$(WHITE)📊 MONITORING & OBSERVABILITY:$(NC)"
	@echo "  $(PURPLE)Prometheus$(NC)        http://localhost:$(PROMETHEUS_PORT)"
	@echo "  $(PURPLE)Loki Logs$(NC)         http://localhost:$(LOKI_PORT)"
	@echo "  $(PURPLE)Qdrant Vector DB$(NC)  http://localhost:$(QDRANT_PORT)/dashboard"
	@echo ""
	@echo "$(WHITE)🗄️ DATABASES & STORAGE:$(NC)"
	@echo "  $(YELLOW)PostgreSQL$(NC)        localhost:$(POSTGRES_PORT) $(YELLOW)(postgres/postgres)$(NC)"
	@echo "  $(YELLOW)Redis Cache$(NC)       localhost:$(REDIS_PORT)"
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
	@echo ""
	@echo "$(WHITE)🔧 QUICK HEALTH CHECKS:$(NC)"
	@echo "  $(GREEN)make health$(NC)        - Check all service health"
	@echo "  $(GREEN)make status$(NC)        - Show Docker container status"
	@echo "  $(GREEN)make logs$(NC)          - View all service logs"
	@echo ""
	@echo "$(YELLOW)💡 TIP: Bookmark http://localhost:$(UI_PORT) for the main application!$(NC)"
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
