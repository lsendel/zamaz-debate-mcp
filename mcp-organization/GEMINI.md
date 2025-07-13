# GEMINI.md - MCP Organization Service

This document provides a concise overview of the `mcp-organization` service.

## Service Purpose

The `mcp-organization` service is responsible for managing organizations and projects in a multi-tenant environment. It serves as the foundation for other services that require organization-level data scoping.

## Core Features

- **Organization Management**: CRUD operations for organizations (tenants).
- **Project Management**: CRUD operations for projects within organizations.
- **Multi-tenancy**: Enforces strict data isolation between organizations.
- **GitHub Integration**: Validates and stores GitHub repository information for projects.
- **Statistics**: Provides analytics and metrics at the organization level.

## Technical Stack

- **Language**: Python
- **Framework**: MCP SDK
- **Database**: PostgreSQL with SQLAlchemy
- **Validation**: Pydantic

## Key MCP Tools

The service exposes a set of MCP tools for interacting with its resources. These tools are the primary way for other services to consume the functionality of `mcp-organization`.

### Organization Tools
- `create_organization`
- `update_organization`
- `get_organization`
- `list_organizations`
- `delete_organization`
- `get_organization_stats`

### Project Tools
- `create_project`
- `update_project`
- `get_project`
- `list_projects`
- `delete_project`
- `validate_github_repo`

## Integration

Other services should use the MCP tools provided by this service to:

- Ensure that all operations are properly scoped to an organization.
- Retrieve organization and project data as needed.
- Validate the existence of organizations before performing any actions.
