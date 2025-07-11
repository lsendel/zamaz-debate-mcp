# MCP Organization Service - Claude Development Guide

## Quick Reference
- **Port**: 5005
- **Database**: PostgreSQL (`debate_org`)
- **Primary Purpose**: Multi-tenancy, user management, and access control
- **Dependencies**: PostgreSQL only

## Key Files to Check
```
mcp-organization/
├── src/
│   ├── mcp_server.py          # Main server - START HERE
│   ├── models.py              # Data models and request/response schemas
│   ├── db/
│   │   └── connection.py      # Database setup and models
│   └── managers/
│       └── organization_manager.py  # Business logic
```

## Current Implementation Status
✅ **Implemented**:
- Organization CRUD operations
- User management within organizations
- Role-based access control
- Organization hierarchy (parent/child)
- Settings management

❌ **Not Implemented**:
- API authentication/authorization
- Quota management
- Billing integration
- Cross-organization sharing
- Audit logging

## Common Development Tasks

### 1. Adding a New Organization Feature
```python
# 1. Update models.py - add to Organization model
class Organization(BaseModel):
    # ... existing fields ...
    new_feature_enabled: bool = Field(default=False)
    new_feature_config: Dict[str, Any] = Field(default_factory=dict)

# 2. Update database schema in db/connection.py
# Add column to OrganizationTable

# 3. Add tool in mcp_server.py
@server.list_tools()
# Add new tool definition

@server.call_tool()
# Add tool implementation
```

### 2. Integrating with Another Service
```python
# When another service needs to validate organization:
POST http://localhost:5005/tools/validate_organization
{
    "name": "validate_organization",
    "arguments": {
        "organization_id": "org-123",
        "user_id": "user-456"
    }
}
```

### 3. Database Queries Pattern
```python
# Always use async SQLAlchemy pattern:
async with get_db_session() as session:
    result = await session.execute(
        select(OrganizationTable).where(
            OrganizationTable.id == org_id
        )
    )
    org = result.scalar_one_or_none()
```

## Integration Points

### Other Services Should Call:
1. `validate_organization` - Check if org exists and is active
2. `get_user_organizations` - Get all orgs for a user
3. `check_user_permission` - Verify user has specific permission
4. `get_organization_settings` - Retrieve org configuration

### This Service Needs (Future):
1. Authentication service for token validation
2. Billing service for subscription management
3. Audit service for logging

## Testing Checklist
- [ ] Organization creation with valid data
- [ ] Duplicate organization name handling
- [ ] User role assignment and validation
- [ ] Organization hierarchy operations
- [ ] Settings inheritance from parent orgs
- [ ] Soft delete and restoration
- [ ] Concurrent access handling

## Common Issues & Solutions

### Issue: "Organization not found"
```python
# Check: Is org soft-deleted?
# Look for: is_deleted=True in database
# Solution: Include deleted check in queries
```

### Issue: "Permission denied"
```python
# Check: User role in organization_users table
# Common roles: owner, admin, member, viewer
# Solution: Verify role has required permission
```

## Environment Variables
```bash
POSTGRES_HOST=postgres
POSTGRES_DB=debate_org
POSTGRES_USER=context_user
POSTGRES_PASSWORD=context_pass
MCP_PORT=5005
LOG_LEVEL=INFO
```

## Quick Commands
```bash
# Test service health
curl http://localhost:5005/health

# Create organization via MCP
echo '{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "create_organization", "arguments": {"name": "Test Org", "description": "Test"}}, "id": 1}' | nc localhost 5005

# View logs
docker logs mcp-organization -f
```

## Architecture Decisions
1. **Why PostgreSQL?**: Need ACID compliance for organization data
2. **Why soft deletes?**: Audit trail and data recovery
3. **Why hierarchical orgs?**: Enterprise use cases with departments
4. **Why separate service?**: Central authority for access control

## Next Development Priorities
1. Add JWT token validation
2. Implement organization quotas
3. Add webhook support for org events
4. Create organization templates
5. Add bulk user import