# CLAUDE.md - Important Instructions and Context

## Critical User Requirements

### UI Development Rules
- **NEVER mock anything in the UI, even for testing**
- Always use real data from backend services
- If backend services are not running, help start them instead of creating mocks
- The user explicitly stated: "dont mock things i want real information all the time"

### Port Configuration Requirements
- **NO hardcoded ports in the application**
- All ports must be defined in the .env file
- Use environment variables for all service URLs and ports
- The user explicitly stated: "make sure all ports are defined in the.env file there is no harcoded port on the app"

### Testing Requirements
- Use automated tools (like Puppeteer) to verify screens and flows before asking user to check
- Test all UI components, not just login
- Ensure comprehensive testing coverage

### Development Preferences
- User prefers real functionality over mocks
- Fix issues by connecting to real services, not by creating mock data
- When dropdowns or UI elements are empty, investigate and fix backend connectivity

## Project Context

### Architecture
- Frontend: React/TypeScript with Vite
- Backend: Java Spring Boot microservices
- Database: PostgreSQL
- Cache: Redis
- Vector DB: Qdrant
- Message Queue: RabbitMQ

### Key Services and Ports
- UI: http://localhost:3001
- Organization API: http://localhost:5005
- LLM API: http://localhost:5002
- Debate Controller: http://localhost:5013
- RAG Service: http://localhost:5004

### Common Commands
- `make build` - Build all Docker images
- `make start` - Start all services
- `make status` - Check service status
- `make logs` - View service logs
- `make show-urls` - Display all service URLs

### Known Issues Fixed
- URI malformed errors due to %PUBLIC_URL% placeholders
- Empty provider/model dropdowns need real backend data
- Login credentials for development: demo/demo123