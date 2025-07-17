# Development Environment Setup

This guide provides instructions for setting up your development environment for the Zamaz Debate MCP project.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Docker** and **Docker Compose** (latest version)
- **Java Development Kit (JDK) 21**
- **Maven** (latest version)
- **Python 3.11+** (for Python services)
- **Node.js 18+** (for UI components)
- **Git** (latest version)

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/zamaz-debate-mcp.git
cd zamaz-debate-mcp
```

### 2. Set Up Environment Variables

```bash
# Copy the example environment file
cp .env.example .env

# Edit the .env file with your API keys and configuration
nano .env
```

⚠️ **IMPORTANT**: Never commit your `.env` file to version control!

Required API keys:
- OpenAI API Key
- Anthropic API Key (for Claude)
- Google API Key (for Gemini)

### 3. Build the Project

```bash
# Build all Java services
mvn clean install -DskipTests

# Build Docker images
docker-compose build
```

## Running the Services

### Start All Services

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps
```

### Start Specific Services

```bash
# Start only the core services
docker-compose up -d postgres redis mcp-organization mcp-llm

# Start with monitoring
docker-compose --profile monitoring up -d
```

### View Logs

```bash
# View logs for all services
docker-compose logs -f

# View logs for a specific service
docker-compose logs -f mcp-llm
```

## Development Workflow

### Java Services

1. Make changes to Java code
2. Build the service:
   ```bash
   cd mcp-service-name
   mvn clean install
   ```
3. Restart the service:
   ```bash
   docker-compose restart mcp-service-name
   ```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific service
cd mcp-service-name
mvn test

# Run integration tests
./scripts/test-mcp-services.sh
```

## Useful Development Commands

### Database Access

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres

# Run database migrations
./scripts/run-migrations.sh
```

### Redis Commands

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli

# Monitor Redis commands
docker-compose exec redis redis-cli monitor
```

### Testing API Endpoints

```bash
# Test the LLM service
curl -X POST http://localhost:5002/mcp/tools/complete \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-3-opus-20240229",
    "prompt": "Hello, world!",
    "max_tokens": 100
  }'
```

## Troubleshooting

### Common Issues

1. **Service won't start**
   - Check logs: `docker-compose logs mcp-service-name`
   - Verify environment variables are set correctly
   - Ensure ports are not already in use

2. **Database connection issues**
   - Ensure PostgreSQL is running: `docker-compose ps postgres`
   - Check database credentials in `.env`
   - Verify database exists: `docker-compose exec postgres psql -U postgres -c '\l'`

3. **API key errors**
   - Verify API keys are correctly set in `.env`
   - Check for API key format errors
   - Ensure the service has access to the keys

### Resetting the Environment

If you need to reset your development environment:

```bash
# Stop all services
docker-compose down

# Remove volumes (caution: this deletes all data)
docker-compose down -v

# Rebuild and restart
docker-compose build
docker-compose up -d
```

## IDE Setup

### IntelliJ IDEA

1. Open the project root directory
2. Import as Maven project
3. Set JDK 21 as the project SDK
4. Enable annotation processing for Lombok

### VS Code

1. Install Java Extension Pack
2. Install Lombok Annotations Support extension
3. Open the project folder
4. Configure Java settings to use JDK 21

## Code Style and Standards

This project follows specific code style guidelines:

- Java: Google Java Style Guide
- Python: PEP 8
- JavaScript: Airbnb Style Guide

We use pre-commit hooks to enforce these standards. Install them with:

```bash
# Install pre-commit
pip install pre-commit

# Set up the git hooks
pre-commit install
```

## Additional Resources

- [Project Architecture Documentation](../architecture/overview.md)
- [API Documentation](../api/overview.md)
- [Security Guidelines](../security/guidelines.md)
- [Testing Strategy](./testing.md)
