# Quick Start Guide - Zamaz Debate System

## 🚀 Getting Started

### 1. Navigate to the Project Root Directory

First, make sure you're in the project root:

```bash
cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp
```

### 2. Start Backend Services

From the project root, run:

```bash
make start
```

This will start all backend services including:
- PostgreSQL
- Redis
- Qdrant (Vector DB)
- Jaeger (Tracing)
- Other supporting services

### 3. Start the UI Development Server

In a new terminal window (from the project root):

```bash
make ui
```

Or simply:

```bash
npm run dev
```

### 4. Access the Application

Open your browser and go to:
- **UI**: http://localhost:3001
- **Login credentials**: demo / demo123

### 5. Session Management (Prevent Timeouts)

To prevent login session timeouts, run this in a separate terminal:

```bash
make keep-alive
```

This will automatically refresh your session every 5 minutes.

## 📋 Common Commands

All commands should be run from the project root directory:

```bash
# Check service status
make status

# View service health
make health

# Show all service URLs
make show-urls

# Run tests
make test-playwright

# Stop all services
make stop

# View logs
make logs
```

## 🔧 Troubleshooting

### "No targets specified and no makefile found"

You're in the wrong directory. Navigate to the project root:
```bash
cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp
```

### Docker Image Issues

If you see errors about Docker images not found, the issue has been fixed. The Jaeger image version has been updated to 1.60.

### Port Already in Use

If a port is already in use, either:
1. Stop the conflicting service
2. Or change the port in the `.env` file

### Session Timeout

Use `make keep-alive` to prevent session timeouts during development.

## 🎯 Next Steps

1. Explore the debate functionality
2. Create new debates
3. Test the organization management features
4. Check the LLM presets configuration

For more detailed documentation, see:
- `SESSION_TIMEOUT_SOLUTIONS.md`
- `PLAYWRIGHT_TEST_REPORT.md`
- `REAL_DEBATE_INTEGRATION_SUMMARY.md`