# ╔══════════════════════════════════════════════════════════════════╗
# ║                  ZAMAZ DEBATE MCP - ROOT MAKEFILE               ║
# ║                 Orchestrator for All Sub-Projects               ║
# ╚══════════════════════════════════════════════════════════════════╝

# Load environment variables
-include .env
export

# Configuration
PROJECT_ROOT := $(shell pwd)
COMPOSE_FILE := infrastructure/docker-compose/docker-compose.yml
COMPOSE_DIR := infrastructure/docker-compose
PROJECT_NAME := zamaz-debate-mcp

# Sub-project directories
DEBATE_UI_DIR := debate-ui
WORKFLOW_EDITOR_DIR := workflow-editor
E2E_TESTS_DIR := e2e-tests

# Detect available sub-projects
HAS_DEBATE_UI := $(shell test -d $(DEBATE_UI_DIR) && echo "yes")
HAS_WORKFLOW_EDITOR := $(shell test -d $(WORKFLOW_EDITOR_DIR) && echo "yes")
HAS_E2E_TESTS := $(shell test -d $(E2E_TESTS_DIR) && echo "yes")

# Java MCP modules
MCP_MODULES := mcp-organization mcp-llm mcp-debate mcp-rag mcp-template mcp-gateway mcp-auth-server

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
PURPLE := \033[0;35m
CYAN := \033[0;36m
WHITE := \033[1;37m
NC := \033[0m

# Default ports (matching sub-project Makefile)
UI_PORT ?= 3001
WORKFLOW_UI_PORT ?= 3002
POSTGRES_PORT ?= 5432
REDIS_PORT ?= 6379
MCP_ORGANIZATION_PORT ?= 5005
MCP_LLM_PORT ?= 5002
MCP_DEBATE_PORT ?= 5013
MCP_RAG_PORT ?= 5004

.PHONY: help all setup start stop restart clean build test lint ui

# =============================================================================
# HELP & DOCUMENTATION
# =============================================================================

help: ## 📚 Show this help message
	@echo '$(CYAN)╔══════════════════════════════════════════════════════════════════╗$(NC)'
	@echo '$(CYAN)║               ZAMAZ DEBATE MCP - ROOT ORCHESTRATOR              ║$(NC)'
	@echo '$(CYAN)║                    Manage All Sub-Projects                      ║$(NC)'
	@echo '$(CYAN)╚══════════════════════════════════════════════════════════════════╝$(NC)'
	@echo ''
	@echo '$(WHITE)🚀 QUICK START:$(NC)'
	@echo '  $(GREEN)make all$(NC)           - Set up and start everything'
	@echo '  $(GREEN)make setup$(NC)         - Initial setup for all projects'
	@echo '  $(GREEN)make start$(NC)         - Start all backend services'
	@echo '  $(GREEN)make ui$(NC)            - Start UI development server'
	@echo ''
	@echo '$(WHITE)🎯 DEVELOPMENT:$(NC)'
	@echo '  $(GREEN)make dev$(NC)           - Start complete dev environment'
	@echo '  $(GREEN)make restart$(NC)       - Restart all services'
	@echo '  $(GREEN)make stop$(NC)          - Stop all services'
	@echo '  $(GREEN)make clean$(NC)         - Clean up everything'
	@echo ''
	@echo '$(WHITE)🧪 TESTING:$(NC)'
	@echo '  $(GREEN)make test$(NC)          - Run all tests'
	@echo '  $(GREEN)make test-ui$(NC)       - Run UI tests only'
	@echo '  $(GREEN)make test-java$(NC)     - Run Java tests'
	@echo '  $(GREEN)make lint$(NC)          - Run all linters'
	@echo ''
	@echo '$(WHITE)📦 BUILDING:$(NC)'
	@echo '  $(GREEN)make build$(NC)         - Build all projects'
	@echo '  $(GREEN)make build-java$(NC)    - Build Java services'
	@echo '  $(GREEN)make build-ui$(NC)      - Build UI projects'
	@echo '  $(GREEN)make build-docker$(NC)  - Build Docker images'
	@echo ''
	@echo '$(WHITE)🔍 MONITORING:$(NC)'
	@echo '  $(GREEN)make status$(NC)        - Show all service status'
	@echo '  $(GREEN)make health$(NC)        - Health check all services'
	@echo '  $(GREEN)make logs$(NC)          - View all logs'
	@echo '  $(GREEN)make show-urls$(NC)     - Show all service URLs'
	@echo ''
	@echo '$(WHITE)📋 SUB-PROJECT COMMANDS:$(NC)'
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo '  $(BLUE)make ui-*$(NC)          - Debate UI commands (ui-help for list)'; \
	fi
	@if [ "$(HAS_WORKFLOW_EDITOR)" = "yes" ]; then \
		echo '  $(BLUE)make workflow-*$(NC)    - Workflow editor commands'; \
	fi
	@echo '  $(BLUE)make java-*$(NC)        - Java service commands'
	@echo ''
	@echo '$(YELLOW)💡 Examples:$(NC)'
	@echo '  First time:     $(CYAN)make all$(NC)'
	@echo '  Daily work:     $(CYAN)make dev$(NC)'
	@echo '  Just backend:   $(CYAN)make start$(NC)'
	@echo '  Just frontend:  $(CYAN)make ui$(NC)'
	@echo ''
	@echo '$(WHITE)📋 ALL COMMANDS:$(NC)'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-20s$(NC) %s\n", $$1, $$2}'

# =============================================================================
# HIGH-LEVEL ORCHESTRATION
# =============================================================================

all: setup start ui ## 🎯 Complete setup and start everything

setup: check-root setup-env setup-projects ## 🛠️ Set up all projects
	@echo "$(GREEN)✅ All projects set up successfully!$(NC)"

dev: start ## 🚀 Start complete development environment
	@echo "$(GREEN)🚀 Development environment starting...$(NC)"
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo "$(YELLOW)Starting UI on http://localhost:$(UI_PORT)$(NC)"; \
		$(MAKE) ui; \
	else \
		echo "$(YELLOW)No UI project found. Backend services are running.$(NC)"; \
	fi

# =============================================================================
# DOCKER & SERVICES
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
	@pkill -f "mvn spring-boot:run" 2>/dev/null || true
	@echo "$(GREEN)✅ All services stopped$(NC)"

restart: stop start ## 🔄 Restart all services

build-docker: ## 🐳 Build Docker images
	@echo "$(BLUE)Building Docker images...$(NC)"
	@docker-compose -f $(COMPOSE_FILE) build
	@echo "$(GREEN)✅ Docker build complete$(NC)"

# =============================================================================
# UI PROJECTS
# =============================================================================

ui: ui-start ## 🎨 Start UI (alias for ui-start)

ui-start: ## 🎨 Start debate UI development server
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo "$(BLUE)Starting UI development server...$(NC)"; \
		cd $(DEBATE_UI_DIR) && npm run dev; \
	else \
		echo "$(RED)❌ Debate UI directory not found$(NC)"; \
		exit 1; \
	fi

ui-build: ## 🏗️ Build debate UI for production
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo "$(BLUE)Building UI...$(NC)"; \
		cd $(DEBATE_UI_DIR) && npm run build; \
	else \
		echo "$(RED)❌ Debate UI directory not found$(NC)"; \
	fi

ui-test: ## 🧪 Run UI tests
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo "$(BLUE)Running UI tests...$(NC)"; \
		cd $(DEBATE_UI_DIR) && npm run test:e2e; \
	else \
		echo "$(RED)❌ Debate UI directory not found$(NC)"; \
	fi

ui-lint: ## 🔍 Lint UI code (including React import validation)
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo "$(BLUE)Validating React imports...$(NC)"; \
		cd $(DEBATE_UI_DIR) && npm run lint:react-imports; \
		echo "$(BLUE)Linting UI code...$(NC)"; \
		cd $(DEBATE_UI_DIR) && npm run lint; \
	else \
		echo "$(RED)❌ Debate UI directory not found$(NC)"; \
	fi

ui-help: ## ❓ Show debate UI Makefile help
	@if [ "$(HAS_DEBATE_UI)" = "yes" ] && [ -f "$(DEBATE_UI_DIR)/Makefile" ]; then \
		cd $(DEBATE_UI_DIR) && make help; \
	else \
		echo "$(YELLOW)Debate UI Makefile not found$(NC)"; \
	fi

# Pass-through commands to debate-ui Makefile
ui-%:
	@if [ "$(HAS_DEBATE_UI)" = "yes" ] && [ -f "$(DEBATE_UI_DIR)/Makefile" ]; then \
		cd $(DEBATE_UI_DIR) && make $*; \
	else \
		echo "$(RED)❌ Cannot run ui-$*: Debate UI Makefile not found$(NC)"; \
	fi

# =============================================================================
# WORKFLOW EDITOR
# =============================================================================

workflow-start: ## 🔄 Start workflow editor
	@if [ "$(HAS_WORKFLOW_EDITOR)" = "yes" ]; then \
		echo "$(BLUE)Starting workflow editor...$(NC)"; \
		cd $(WORKFLOW_EDITOR_DIR)/client/workflow-editor && npm start; \
	else \
		echo "$(RED)❌ Workflow editor not found$(NC)"; \
	fi

workflow-api: ## 🔌 Start workflow API backend
	@if [ "$(HAS_WORKFLOW_EDITOR)" = "yes" ]; then \
		echo "$(BLUE)Starting workflow API...$(NC)"; \
		cd $(WORKFLOW_EDITOR_DIR) && mvn spring-boot:run; \
	else \
		echo "$(RED)❌ Workflow editor not found$(NC)"; \
	fi

# =============================================================================
# JAVA SERVICES
# =============================================================================

build-java: ## ☕ Build all Java services
	@echo "$(BLUE)Building Java services...$(NC)"
	@for module in $(MCP_MODULES); do \
		if [ -d "$$module" ]; then \
			echo "$(YELLOW)Building $$module...$(NC)"; \
			cd $$module && mvn clean package -DskipTests && cd ..; \
		fi; \
	done
	@echo "$(GREEN)✅ Java build complete$(NC)"

test-java: ## 🧪 Run Java tests
	@echo "$(BLUE)Running Java tests...$(NC)"
	@for module in $(MCP_MODULES); do \
		if [ -d "$$module" ]; then \
			echo "$(YELLOW)Testing $$module...$(NC)"; \
			cd $$module && mvn test && cd ..; \
		fi; \
	done

java-status: ## 📊 Check Java service status
	@echo "$(BLUE)Java Service Status:$(NC)"
	@for port in $(MCP_ORGANIZATION_PORT) $(MCP_LLM_PORT) $(MCP_DEBATE_PORT) $(MCP_RAG_PORT); do \
		curl -s http://localhost:$$port/actuator/health > /dev/null 2>&1 && \
		echo "  Port $$port: $(GREEN)✓ Running$(NC)" || \
		echo "  Port $$port: $(RED)✗ Not running$(NC)"; \
	done

# =============================================================================
# TESTING
# =============================================================================

test: test-java ui-test ## 🧪 Run all tests

test-integration: ## 🧪 Run integration tests
	@echo "$(BLUE)Running integration tests...$(NC)"
	@if [ -f ".linting/scripts/simple-integration-test.sh" ]; then \
		chmod +x .linting/scripts/simple-integration-test.sh && ./.linting/scripts/simple-integration-test.sh; \
	else \
		echo "$(YELLOW)Integration tests not found$(NC)"; \
	fi

# =============================================================================
# UTILITIES & MONITORING
# =============================================================================

status: ## 📊 Show all service status
	@echo "$(BLUE)=== Docker Services ===$(NC)"
	@docker-compose -f $(COMPOSE_FILE) ps
	@echo ""
	@echo "$(BLUE)=== Java Services ===$(NC)"
	@$(MAKE) java-status
	@echo ""
	@echo "$(BLUE)=== Node.js Services ===$(NC)"
	@ps aux | grep -E "node|npm" | grep -v grep || echo "  No Node.js services running"

health: ## 🏥 Health check all services
	@echo "$(BLUE)Health Check:$(NC)"
	@echo -n "PostgreSQL: "
	@docker-compose -f $(COMPOSE_FILE) exec -T postgres pg_isready -U postgres > /dev/null 2>&1 && \
		echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Down$(NC)"
	@echo -n "Redis: "
	@docker-compose -f $(COMPOSE_FILE) exec -T redis redis-cli ping > /dev/null 2>&1 && \
		echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Down$(NC)"
	@echo -n "Qdrant: "
	@curl -s http://localhost:6333/collections > /dev/null 2>&1 && \
		echo "$(GREEN)✓ Healthy$(NC)" || echo "$(RED)✗ Down$(NC)"
	@$(MAKE) java-status

logs: ## 📜 View all logs
	@echo "$(BLUE)Docker logs (Ctrl+C to stop):$(NC)"
	@docker-compose -f $(COMPOSE_FILE) logs -f --tail=50

show-urls: ## 🌐 Show all service URLs
	@echo ""
	@echo "$(CYAN)╔══════════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(CYAN)║                     🌐 SERVICE ACCESS URLs                      ║$(NC)"
	@echo "$(CYAN)╚══════════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(WHITE)🎨 USER INTERFACES:$(NC)"
	@echo "  $(GREEN)Debate UI$(NC)          http://localhost:$(UI_PORT)"
	@echo "  $(GREEN)Workflow Editor$(NC)    http://localhost:$(WORKFLOW_UI_PORT)"
	@echo ""
	@echo "$(WHITE)🔌 API SERVICES:$(NC)"
	@echo "  $(BLUE)Organization API$(NC)   http://localhost:$(MCP_ORGANIZATION_PORT)"
	@echo "  $(BLUE)LLM API$(NC)            http://localhost:$(MCP_LLM_PORT)"
	@echo "  $(BLUE)Debate API$(NC)         http://localhost:$(MCP_DEBATE_PORT)"
	@echo "  $(BLUE)RAG API$(NC)            http://localhost:$(MCP_RAG_PORT)"
	@echo ""
	@echo "$(WHITE)💾 DATABASES:$(NC)"
	@echo "  $(YELLOW)PostgreSQL$(NC)         localhost:$(POSTGRES_PORT)"
	@echo "  $(YELLOW)Redis$(NC)              localhost:$(REDIS_PORT)"
	@echo "  $(YELLOW)Qdrant$(NC)             http://localhost:6333"
	@echo ""

# =============================================================================
# SETUP HELPERS
# =============================================================================

check-root: ## 🔍 Verify we're in project root
	@if [ ! -f "$(COMPOSE_FILE)" ]; then \
		echo "$(RED)❌ Not in project root directory$(NC)"; \
		echo "$(YELLOW)Current directory: $(PROJECT_ROOT)$(NC)"; \
		exit 1; \
	fi

setup-env: ## 🔧 Set up environment files
	@echo "$(BLUE)Setting up environment...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env 2>/dev/null || echo "# Add your environment variables here" > .env; \
		echo "$(GREEN)✅ Created .env file$(NC)"; \
	fi

setup-projects: ## 📦 Install dependencies for all projects
	@echo "$(BLUE)Installing project dependencies...$(NC)"
	@if [ "$(HAS_DEBATE_UI)" = "yes" ]; then \
		echo "$(YELLOW)Installing debate-ui dependencies...$(NC)"; \
		cd $(DEBATE_UI_DIR) && npm install; \
	fi
	@if [ "$(HAS_WORKFLOW_EDITOR)" = "yes" ] && [ -d "$(WORKFLOW_EDITOR_DIR)/client/workflow-editor" ]; then \
		echo "$(YELLOW)Installing workflow editor dependencies...$(NC)"; \
		cd $(WORKFLOW_EDITOR_DIR)/client/workflow-editor && npm install; \
	fi
	@if [ "$(HAS_E2E_TESTS)" = "yes" ]; then \
		echo "$(YELLOW)Installing e2e test dependencies...$(NC)"; \
		cd $(E2E_TESTS_DIR) && npm install; \
	fi

# =============================================================================
# CLEANUP
# =============================================================================

clean: ## 🧹 Clean everything
	@echo "$(YELLOW)⚠️ This will remove all data. Continue? [y/N]$(NC)"
	@read -r response && \
	if [ "$$response" = "y" ] || [ "$$response" = "Y" ]; then \
		echo "$(BLUE)Cleaning up...$(NC)"; \
		docker-compose -f $(COMPOSE_FILE) down -v --remove-orphans; \
		docker system prune -f; \
		find . -name "node_modules" -type d -prune -exec rm -rf '{}' + 2>/dev/null || true; \
		find . -name "target" -type d -prune -exec rm -rf '{}' + 2>/dev/null || true; \
		find . -name "dist" -type d -prune -exec rm -rf '{}' + 2>/dev/null || true; \
		find . -name "build" -type d -prune -exec rm -rf '{}' + 2>/dev/null || true; \
		echo "$(GREEN)✅ Cleanup complete$(NC)"; \
	else \
		echo "$(YELLOW)Cleanup cancelled$(NC)"; \
	fi

# =============================================================================
# SPECIAL COMMANDS
# =============================================================================

# Session management commands delegated to debate-ui
login test-auth refresh-session keep-alive:
	@if [ -f "$(DEBATE_UI_DIR)/Makefile" ]; then \
		cd $(DEBATE_UI_DIR) && make $@; \
	else \
		echo "$(RED)❌ Session management requires debate-ui Makefile$(NC)"; \
	fi

# Default target
.DEFAULT_GOAL := help