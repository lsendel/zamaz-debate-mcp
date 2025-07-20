# CLAUDE.md - Important Instructions and Context

## UI Development Best Practices and Learnings

### Critical UI Migration Learnings (From @zamaz/ui to Ant Design)

#### Root Cause Analysis
- **Problem**: ReactCurrentDispatcher error - "Cannot read properties of undefined"
- **Cause**: Multiple React instances due to @zamaz/ui bundling its own React
- **Solution**: Replace problematic UI library with well-maintained alternative (Ant Design)

#### Key Lessons for Future UI Development

1. **NEVER Mock UI Components**
   - Always use real components from established libraries
   - User explicitly stated: "memorize never use mock in the ui"
   - If components are missing, fix by installing proper libraries, not mocking

2. **Check for React Version Conflicts**
   - Verify UI libraries use React as peerDependency, not dependency
   - Check package.json of UI libraries before using
   - Use `npm ls react` to detect multiple React versions

3. **Systematic Component Migration Pattern**
   ```typescript
   // BAD - Custom/problematic UI library
   import { Button, Card, Dialog } from '@custom/ui';
   
   // GOOD - Established UI library
   import { Button, Card, Modal } from 'antd';
   ```

4. **Icon Migration Pattern**
   ```typescript
   // BAD - Mixed icon libraries
   import { User, Settings } from 'lucide-react';
   
   // GOOD - Consistent with UI library
   import { UserOutlined, SettingOutlined } from '@ant-design/icons';
   ```

5. **Style Migration Pattern**
   ```typescript
   // BAD - Tailwind classes (when not using Tailwind)
   <div className="flex items-center gap-4">
   
   // GOOD - Inline styles or UI library styling
   <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
   ```

#### UI Validation Checklist
When creating or updating UI:
- [ ] All imports from single, consistent UI library
- [ ] No className with Tailwind unless Tailwind is configured
- [ ] Icons from UI library's icon package
- [ ] Form components use library's Form system
- [ ] Modals/Dialogs use library's Modal component
- [ ] Notifications use library's notification API
- [ ] Tables use library's Table component
- [ ] No hardcoded ports - use environment variables

#### Component Conversion Reference
| Old Component (@zamaz/ui) | New Component (Ant Design) |
|--------------------------|---------------------------|
| Dialog | Modal |
| DialogContent | Modal content prop |
| Card/CardContent | Card with children |
| Toast | notification API |
| Select/SelectTrigger | Select |
| FormField | Form.Item |
| Badge variant="success" | Badge status="success" |
| Button variant="primary" | Button type="primary" |
| CircularProgress | Spin |
| Progress | Progress |
| DataTable | Table |

#### Testing Strategy
1. Use Puppeteer for automated UI validation
2. Check for console errors
3. Verify component rendering
4. Screenshot each major section
5. Validate no remaining old imports

## Recent Implementation Learnings

### Hexagonal Architecture Refactoring (mcp-rag service)
- **Pattern Applied**: Domain core → Application ports → Infrastructure adapters
- **Key Benefits**: Better testability, clear boundaries, flexible implementations
- **Domain Purity**: Domain objects should have no framework dependencies
- **Event Handling**: Domain events are pure POJOs, published through EventPublisher port
- **Validation Strategy**: Value objects self-validate in constructors
- **Async Handling**: Use CompletableFuture in port definitions for async operations
- **Best Practice**: Start with domain model, then ports, then application services, adapters last

### Spring Cloud Config Implementation
- **Centralized Configuration**: All services now use Spring Cloud Config Server
- **Encryption**: Sensitive values encrypted with {cipher} prefix
- **Dynamic Refresh**: Configuration changes propagated via Spring Cloud Bus
- **Migration Pattern**: Use automated scripts to migrate existing configurations

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
- When referencing a URL, validate it works by using Puppet to ensure the screen is not blank

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

## Implementation and Testing Considerations
- Update Makefile to remove unused targets and simplify usage
- Prepare comprehensive E2E scenarios for reports
- Develop Playwright-based testing for UI navigation and report verification
- Create step-by-step navigation plan for accessing and testing reports
- Ensure regression test coverage for different report scenarios
- Prioritize real data and service connectivity over mocking