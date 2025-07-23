# Makefile Architecture - Zamaz Debate MCP

## Overview

The project now uses a **hierarchical Makefile structure** that allows you to run all commands from the project root while intelligently delegating to sub-project Makefiles when needed.

## Architecture

```
zamaz-debate-mcp/
├── Makefile                    # Root orchestrator Makefile
├── debate-ui/
│   └── Makefile               # UI-specific commands
├── workflow-editor/
│   └── Makefile (optional)    # Workflow editor commands
└── e2e-tests/
    └── Makefile (optional)    # E2E test commands
```

## Root Makefile Features

### 1. **Intelligent Sub-Project Detection**
The root Makefile automatically detects which sub-projects exist:
```makefile
HAS_DEBATE_UI := $(shell test -d $(DEBATE_UI_DIR) && echo "yes")
HAS_WORKFLOW_EDITOR := $(shell test -d $(WORKFLOW_EDITOR_DIR) && echo "yes")
```

### 2. **Command Delegation**
Commands are intelligently routed to sub-projects:
- `make ui` → Runs UI in debate-ui directory
- `make ui-test` → Delegates to debate-ui/Makefile
- `make workflow-start` → Starts workflow editor

### 3. **Pass-Through Pattern**
Any command with `ui-*` prefix is passed to the debate-ui Makefile:
```bash
make ui-lint        # Runs: cd debate-ui && make lint
make ui-test        # Runs: cd debate-ui && make test
make ui-help        # Shows debate-ui Makefile help
```

## Command Categories

### Quick Start Commands (Root Level)
```bash
make all            # Complete setup and start
make setup          # Initial project setup
make start          # Start backend services
make ui             # Start UI development server
make dev            # Start complete dev environment
```

### Service Management
```bash
make stop           # Stop all services
make restart        # Restart all services
make status         # Show service status
make health         # Health check all services
make logs           # View all logs
```

### Building & Testing
```bash
make build          # Build all projects
make build-java     # Build Java services
make build-ui       # Build UI projects
make test           # Run all tests
make test-ui        # Run UI tests
make test-java      # Run Java tests
```

### Sub-Project Commands
```bash
# UI Commands (debate-ui)
make ui-start       # Start UI dev server
make ui-build       # Build for production
make ui-test        # Run UI tests
make ui-lint        # Lint UI code
make ui-help        # Show UI Makefile help

# Session Management (delegated to debate-ui)
make login          # Login with demo credentials
make keep-alive     # Keep session alive
make test-auth      # Check auth status

# Workflow Editor
make workflow-start # Start workflow editor
make workflow-api   # Start workflow API
```

## Best Practices

### 1. **Always Run from Root**
```bash
cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp
make start
make ui
```

### 2. **Use Sub-Project Prefixes**
To run sub-project specific commands:
```bash
make ui-lint        # Instead of: cd debate-ui && make lint
make ui-test        # Instead of: cd debate-ui && make test
```

### 3. **Check Available Commands**
```bash
make help           # Root level help
make ui-help        # Debate UI specific help
```

### 4. **Session Management**
For long development sessions:
```bash
make login          # Initial login
make keep-alive     # In separate terminal
```

## How It Works

### Command Resolution
1. Root Makefile checks if command exists locally
2. If not found, checks for prefix pattern (ui-*, workflow-*)
3. Delegates to appropriate sub-project Makefile
4. Falls back with helpful error message

### Environment Variables
All environment variables are loaded at root and passed down:
```makefile
-include .env
export
```

### Port Configuration
Ports are defined once at root level:
```makefile
UI_PORT ?= 3001
MCP_ORGANIZATION_PORT ?= 5005
MCP_DEBATE_PORT ?= 5013
```

## Troubleshooting

### Command Not Found
If a command isn't working:
1. Check you're in the root directory
2. Run `make help` to see available commands
3. Use `make ui-help` for UI-specific commands

### Adding New Sub-Projects
To add a new sub-project:
1. Create directory with Makefile
2. Add detection in root Makefile:
   ```makefile
   HAS_NEW_PROJECT := $(shell test -d new-project && echo "yes")
   ```
3. Add delegation pattern for commands

## Benefits

1. **Single Entry Point**: All commands available from root
2. **No Directory Navigation**: No need to `cd` into sub-projects
3. **Consistent Interface**: Same command style across all projects
4. **Smart Delegation**: Commands automatically routed to correct location
5. **Backwards Compatible**: Existing commands still work

## Migration from Old Structure

If you were used to running commands from `debate-ui`:
```bash
# Old way:
cd debate-ui
make start
make ui

# New way:
make start    # From root
make ui       # From root
```

All commands now work from the project root!