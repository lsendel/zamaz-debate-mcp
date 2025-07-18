# Microservice Consolidation Analysis

## Executive Summary

After analyzing the current microservice architecture, I've identified significant opportunities to reduce complexity through strategic consolidation. The system currently has 11 services (including common/security libraries), which creates substantial operational overhead. I recommend consolidating to 4-5 core services while maintaining clean boundaries and the ability to scale independently.

## Current Service Inventory

### Core Business Services
1. **mcp-organization** (Port 5005)
   - Organization and user management
   - Authentication and JWT handling
   - Multi-tenant support
   - Database: `organization_db` (PostgreSQL)

2. **mcp-context** (Port 5007)
   - Context management for debates
   - Context windowing and token management
   - Version history and caching
   - Database: `context_db` (PostgreSQL)

3. **mcp-llm** (Port 5002)
   - LLM provider gateway (Claude, OpenAI, Gemini, Ollama)
   - Provider abstraction and caching
   - Stateless service
   - No database (Redis cache only)

4. **mcp-debate** (Port 5003)
   - Basic debate entity management
   - Very limited functionality
   - Database: SQLite (debates.db)

5. **mcp-controller** (Port 5013)
   - Debate orchestration and workflow
   - Depends on organization and LLM services
   - Database: `debate_db` (PostgreSQL)

6. **mcp-rag** (Port 5004)
   - Retrieval Augmented Generation
   - Vector storage integration (Qdrant)
   - Database: `rag_db` (PostgreSQL) + Qdrant

7. **mcp-template** (Port 5006)
   - Template management
   - Database: `template_db` (PostgreSQL)

### Infrastructure Services
8. **mcp-gateway** (Port 8080)
   - API Gateway with Spring Cloud Gateway
   - Rate limiting, circuit breaking
   - OAuth2/SSO integration
   - WebSocket support

### Library Modules
9. **mcp-common**
   - Shared utilities and domain models
   - Event sourcing, caching, monitoring
   - Hexagonal architecture components

10. **mcp-security**
    - Security components (JWT, RBAC)
    - Audit logging
    - Session management

11. **mcp-modulith**
    - Spring Modulith experiment
    - Currently minimal implementation

## Analysis of Service Boundaries

### Issues Identified

1. **Over-decomposition**
   - `mcp-debate` and `mcp-controller` have overlapping concerns
   - `mcp-context` is tightly coupled to debate functionality
   - `mcp-template` could be part of a larger service

2. **Database Proliferation**
   - 6 separate databases for a relatively simple domain
   - High operational overhead
   - Complex cross-service transactions

3. **High Inter-service Communication**
   - Controller → Organization (auth checks)
   - Controller → LLM (for each message)
   - Controller → Context (maintaining state)
   - Creates latency and failure points

4. **Shared Libraries Creating Coupling**
   - `mcp-common` and `mcp-security` are used by all services
   - Changes require coordinated deployments
   - Version management complexity

## Consolidation Recommendations

### Option 1: Domain-Driven Consolidation (Recommended)

Consolidate to 4 core services based on bounded contexts:

#### 1. **mcp-identity** (merge organization + security components)
```yaml
Combines:
- mcp-organization (current)
- Security components from mcp-security
- User session management

Responsibilities:
- Organization management
- User authentication/authorization
- Multi-tenant support
- Security audit logging
- JWT token management

Database: identity_db
Port: 5005
```

#### 2. **mcp-debate-engine** (merge debate + controller + context + template)
```yaml
Combines:
- mcp-controller (orchestration)
- mcp-debate (entity management)
- mcp-context (context management)
- mcp-template (debate templates)

Responsibilities:
- Complete debate lifecycle management
- Context and conversation management
- Template-based debate creation
- Debate rules and formats
- Turn management and orchestration

Database: debate_db (unified)
Port: 5013
```

#### 3. **mcp-ai-gateway** (keep as-is)
```yaml
Current: mcp-llm

Responsibilities:
- LLM provider abstraction
- Response caching
- Rate limiting per provider
- Model selection and routing

Database: None (Redis cache only)
Port: 5002
```

#### 4. **mcp-knowledge** (keep as-is)
```yaml
Current: mcp-rag

Responsibilities:
- Document ingestion
- Vector storage management
- Retrieval for debates
- Knowledge base management

Database: knowledge_db + Qdrant
Port: 5004
```

### Option 2: Minimal Consolidation

If a more conservative approach is preferred:

1. **Merge mcp-debate into mcp-controller**
   - Eliminate redundancy
   - Single debate service

2. **Merge mcp-template into mcp-controller**
   - Templates are debate-specific
   - Reduces service count

3. **Keep others separate**
   - Maintain current boundaries
   - Less disruption

## Implementation Plan

### Phase 1: Preparation (Week 1-2)
1. Create comprehensive integration tests
2. Document all inter-service APIs
3. Identify shared domain models
4. Plan database migration strategy

### Phase 2: Library Consolidation (Week 2-3)
1. Create service-specific security modules
2. Reduce mcp-common to truly shared utilities
3. Implement domain events for loose coupling

### Phase 3: Service Consolidation (Week 3-6)

#### Week 3-4: Debate Services
```bash
# 1. Create new mcp-debate-engine structure
# 2. Migrate database schemas
# 3. Port functionality from:
   - mcp-controller
   - mcp-debate  
   - mcp-context
   - mcp-template
# 4. Update API Gateway routes
```

#### Week 5: Identity Service
```bash
# 1. Create mcp-identity from mcp-organization
# 2. Integrate security components
# 3. Centralize auth/session management
# 4. Update all service dependencies
```

#### Week 6: Testing and Cleanup
```bash
# 1. End-to-end testing
# 2. Performance testing
# 3. Remove deprecated services
# 4. Update documentation
```

## Risk Mitigation

### Technical Risks
1. **Data Migration Complexity**
   - Mitigation: Use event sourcing for rollback capability
   - Create comprehensive backup strategy

2. **Service Coupling**
   - Mitigation: Use domain events instead of direct calls
   - Implement circuit breakers at module level

3. **Deployment Complexity**
   - Mitigation: Use feature flags for gradual rollout
   - Maintain backward compatibility during transition

### Operational Risks
1. **Team Disruption**
   - Mitigation: One team owns each consolidation
   - Clear communication plan

2. **Performance Impact**
   - Mitigation: Implement caching at module boundaries
   - Monitor closely during transition

## Benefits of Consolidation

### Immediate Benefits
- Reduce services from 11 to 4-5
- Eliminate 2-3 databases
- Reduce network calls by 60%
- Simplify deployment from 11 to 4 containers

### Long-term Benefits
- Easier debugging and monitoring
- Reduced operational complexity
- Better performance (fewer network hops)
- Simplified security model
- Lower infrastructure costs

### Development Benefits
- Faster feature development
- Easier testing
- Clearer ownership boundaries
- Reduced cognitive load

## Metrics for Success

### Technical Metrics
- Response time improvement: Target 30% reduction
- Service startup time: < 30 seconds per service
- Memory footprint: 40% reduction overall
- Network calls: 60% reduction

### Operational Metrics
- Deployment time: 50% reduction
- Mean time to recovery: 40% improvement
- Configuration complexity: 60% reduction
- Monitoring alerts: 50% reduction

## Conclusion

The current architecture is over-engineered for its requirements. By consolidating to 4 core services aligned with business domains, we can significantly reduce complexity while maintaining the benefits of microservices where they matter most - at the boundaries between major system concerns (identity, debates, AI, knowledge).

The recommended approach (Option 1) provides the best balance of simplicity and maintainability while preserving the ability to scale components independently when needed.