# MCP Organization Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-organization service.

## Service Overview

The `mcp-organization` service is the foundational multi-tenant management service for the zamaz-debate-mcp system. It provides organization and project management capabilities with complete tenant isolation.

## Purpose

- **Organization Management**: Create, update, and manage organizations (tenants)
- **Project Management**: Handle projects within organizations with GitHub integration
- **Multi-tenant Foundation**: Provides the organizational structure other services depend on
- **Statistics & Analytics**: Organization-level metrics and project analytics

## Technology Stack

- **Language**: Python 3.11+
- **Framework**: MCP SDK (Model Context Protocol)
- **Database**: PostgreSQL with SQLAlchemy async ORM
- **Validation**: Pydantic v2 for data models
- **Logging**: Structured logging with structlog
- **GitHub Integration**: httpx for GitHub API validation
- **Container**: Docker with health checks

## Key Features

### Organization Management
- Create organizations with unique names and slugs
- Update organization details (name, description, website, GitHub org)
- Soft delete (deactivate) organizations
- Organization statistics and analytics

### Project Management
- Create projects within organizations
- Support for different project types (frontend, backend, fullstack, mobile, etc.)
- Project status tracking (active, inactive, archived, planned, on_hold)
- GitHub repository integration with URL parsing and validation
- Technology stack tracking
- Project tagging and priority system

### MCP Protocol Implementation
- **Resources**: Exposes organizations, projects, and stats as MCP resources
- **Tools**: 14 MCP tools for CRUD operations on organizations and projects
- **Error Handling**: Structured error responses with proper error types

## Database Models

### Organizations
- Unique ID and slug generation
- Basic info (name, description, website, contact)
- GitHub organization linking
- Created/updated timestamps
- Soft delete with is_active flag
- JSON metadata field for extensibility

### Projects
- Organization scoping for multi-tenant isolation
- Project type and status enums
- GitHub integration (repo URL, owner, repo name, default branch)
- Technology stack as JSON array
- Tags and priority system
- Automatic slug generation

## MCP Tools Available

### Organization Tools
- `create_organization` - Create new organization
- `update_organization` - Update organization details
- `get_organization` - Get organization by ID
- `list_organizations` - List with pagination and filtering
- `delete_organization` - Soft delete organization
- `get_organization_stats` - Detailed organization analytics

### Project Tools
- `create_project` - Create new project in organization
- `update_project` - Update project details
- `get_project` - Get project by ID
- `list_projects` - List with filtering by org, status, type
- `delete_project` - Hard delete project
- `validate_github_repo` - Validate GitHub repository URL

## Development Guidelines

### Database Operations
```python
# Always use async session context
async with get_db_session() as session:
    manager = OrganizationManager(session)
    org = await manager.create_organization(request)
```

### Error Handling
- Use structured logging with context (org_id, project_id, etc.)
- Return proper MCP error responses with isError=True
- Validate organization existence before project operations

### GitHub Integration
- Parse GitHub URLs to extract owner/repo
- Support multiple URL formats (HTTPS, SSH, shortened)
- Optional GitHub API validation with token
- Store parsed components separately for querying

### Multi-tenant Considerations
- All operations are scoped to organizations
- Projects cannot exist without valid organization
- Use organization_id for all project queries
- Statistics are calculated per organization

## Configuration

### Environment Variables
- `MCP_HOST` - Server host (default: 0.0.0.0)
- `MCP_PORT` - Server port (default: 5005)
- `DATABASE_URL` - PostgreSQL connection string
- `GITHUB_TOKEN` - Optional GitHub API token for validation

### Running the Service

```bash
# Development
python -m src.mcp_server

# Docker
docker build -t mcp-organization .
docker run -p 5005:5005 mcp-organization
```

## Testing

- Unit tests for managers and models
- Integration tests for MCP protocol compliance
- GitHub integration tests (with/without API token)
- Multi-tenant isolation tests

## Data Flow

1. **Organization Creation**: Creates tenant namespace with unique slug
2. **Project Creation**: Requires valid organization, parses GitHub URLs
3. **Statistics**: Aggregates project data per organization
4. **MCP Exposure**: All data accessible via MCP resources and tools

## Dependencies on Other Services

- **Consumed by**: mcp-context, mcp-debate services for organization scoping
- **Database**: Requires PostgreSQL for persistent storage
- **External**: Optional GitHub API for repository validation

## Service Integration

Other services should use this service's MCP tools to:
- Validate organization existence before operations
- Retrieve project information for context
- Get organization statistics for analytics
- Ensure proper multi-tenant scoping