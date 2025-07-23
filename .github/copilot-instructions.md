# GitHub Copilot Custom Instructions

## Project Overview
This is a debate management system built with:
- Frontend: React/TypeScript with Vite
- Backend: Java Spring Boot microservices
- Database: PostgreSQL
- Cache: Redis
- Vector DB: Qdrant
- Message Queue: RabbitMQ

## Development Guidelines

### Code Style
- Follow existing patterns in the codebase
- Use TypeScript for frontend code
- Use Java Spring Boot conventions for backend services
- Prefer functional programming patterns where appropriate

### Architecture Patterns
- Follow hexagonal architecture for services
- Domain core → Application ports → Infrastructure adapters
- Keep domain objects pure without framework dependencies
- Use Spring Cloud Config for centralized configuration

### Testing
- Write unit tests for all new functionality
- Use integration tests for API endpoints
- Use Puppeteer/Playwright for UI testing

### Security
- Never hardcode sensitive values
- Use environment variables for configuration
- Encrypt sensitive config values with {cipher} prefix

### UI Development
- Use Ant Design components consistently
- No mocking - always connect to real backend services
- All ports must be defined in .env file
- Follow the UI component migration patterns documented in CLAUDE.md

### Common Commands
- `make build` - Build all Docker images
- `make start` - Start all services
- `make status` - Check service status
- `make logs` - View service logs
- `make show-urls` - Display all service URLs

### Service Ports
- UI: http://localhost:3001
- Organization API: http://localhost:5005
- LLM API: http://localhost:5002
- Debate Controller: http://localhost:5013
- RAG Service: http://localhost:5004

### Git Commit Messages
- Use conventional commits format
- Include ticket/issue number when applicable
- Keep commits focused and atomic

### Pull Request Guidelines
- Provide clear description of changes
- Include test plan
- Reference related issues
- Ensure all tests pass before requesting review