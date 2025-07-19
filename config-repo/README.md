# Configuration Repository

This repository contains centralized configuration for all MCP microservices.

## Repository Structure

```
config-repo/
├── application.yml                 # Global defaults for all services
├── application-dev.yml            # Development environment defaults
├── application-staging.yml        # Staging environment defaults
├── application-prod.yml           # Production environment defaults
├── mcp-organization.yml           # Organization service configuration
├── mcp-organization-{env}.yml     # Environment-specific overrides
├── mcp-llm.yml                    # LLM service configuration
├── mcp-controller.yml             # Controller service configuration
├── mcp-rag.yml                    # RAG service configuration
├── mcp-template.yml               # Template service configuration
├── mcp-context.yml                # Context service configuration
└── shared/                        # Shared configuration modules
    ├── database-common.yml
    ├── security-common.yml
    ├── monitoring-common.yml
    └── logging-common.yml
```

## Branching Strategy

### Main Branches

- **main**: Production-ready configurations
- **develop**: Development configurations
- **staging**: Staging environment configurations

### Feature Branches

- **feature/config-{service}-{change}**: For service-specific changes
- **hotfix/config-{issue}**: For urgent production fixes

### Branch Protection Rules

1. **main branch**:
   - Requires pull request reviews (minimum 2)
   - Requires status checks to pass
   - Includes administrators in restrictions
   - Enforces linear history

2. **staging branch**:
   - Requires pull request reviews (minimum 1)
   - Requires status checks to pass

## Commit Message Standards

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: New configuration or feature
- **fix**: Configuration fix
- **refactor**: Configuration restructuring
- **security**: Security-related configuration change
- **perf**: Performance tuning
- **docs**: Documentation only changes

### Examples

```
feat(mcp-organization): add multi-tenant configuration

- Added tenant isolation settings
- Configured tenant-specific database pools
- Updated cache configuration for multi-tenancy

JIRA: MCP-123
```

```
security(jwt): rotate JWT signing keys

- Updated JWT secret for all environments
- Increased key strength to 512 bits
- Added key rotation schedule

Security-Advisory: SA-2024-001
```

## Configuration Change Process

### 1. Making Changes

```bash
# Create feature branch
git checkout -b feature/config-service-change

# Make your changes
vim mcp-service.yml

# Test locally with Config Server
./test-config.sh

# Commit with proper message
git add .
git commit -m "feat(mcp-service): add new feature configuration"
```

### 2. Testing Changes

```bash
# Run configuration validation
./validate-config.sh

# Check for sensitive data
./security-scan.sh

# Test with local Config Server
docker-compose -f docker-compose.test.yml up
```

### 3. Pull Request Process

1. Create PR with detailed description
2. Ensure all checks pass
3. Request reviews from team members
4. Address review feedback
5. Merge after approval

## Rollback Procedures

### Quick Rollback (Git Revert)

```bash
# Identify problematic commit
git log --oneline -n 10

# Revert the commit
git revert <commit-hash>

# Push to trigger refresh
git push origin main
```

### Tag-based Rollback

```bash
# List available tags
git tag -l

# Checkout specific version
git checkout tags/v1.2.3 -b rollback-v1.2.3

# Cherry-pick if needed
git cherry-pick <commit-hash>

# Create new tag
git tag -a v1.2.4 -m "Rollback to stable configuration"
git push origin v1.2.4
```

### Emergency Rollback

```bash
# For immediate rollback, use previous known good commit
git reset --hard <known-good-commit>
git push --force-with-lease origin main

# Trigger immediate refresh
curl -X POST http://config-server:8888/actuator/busrefresh
```

## Version Tagging

### Semantic Versioning

We use semantic versioning for configuration releases:

- **MAJOR.MINOR.PATCH**
- **MAJOR**: Breaking configuration changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, minor updates

### Tagging Commands

```bash
# Create annotated tag
git tag -a v1.2.3 -m "Release v1.2.3: Add monitoring configuration"

# Push tags
git push origin --tags

# List tags with messages
git tag -n
```

## Merge Conflict Resolution

### Prevention

1. Keep changes small and focused
2. Communicate with team about large changes
3. Regularly sync with main branch

### Resolution Process

```bash
# Update your branch
git fetch origin
git rebase origin/main

# If conflicts occur
git status
# Edit conflicted files
git add <resolved-files>
git rebase --continue

# Verify configuration still valid
./validate-config.sh
```

## Audit Trail

All configuration changes are tracked:

1. Git commit history provides full audit trail
2. Each commit includes author, timestamp, and reason
3. Pull requests document review process
4. Tags mark significant releases

### Viewing History

```bash
# View change history for specific file
git log -p mcp-service.yml

# View changes by author
git log --author="developer@example.com"

# View changes between tags
git diff v1.2.2..v1.2.3
```

## Security Guidelines

### Never Commit

- Plain text passwords
- API keys or tokens
- Private keys or certificates
- Internal URLs or IPs

### Always Use

- Environment variables for secrets
- `{cipher}` prefix for encrypted values
- Placeholder values in examples

### Pre-commit Hooks

Install pre-commit hooks to prevent accidental commits:

```bash
./install-hooks.sh
```

## Backup and Recovery

### Automated Backups

- Git repository is backed up daily
- Backups retained for 90 days
- Encrypted at rest

### Manual Backup

```bash
# Create local backup
git bundle create config-backup-$(date +%Y%m%d).bundle --all

# Restore from backup
git clone config-backup-20240115.bundle config-restored
```

## Monitoring Changes

### Webhooks

Configure webhooks for change notifications:

1. GitHub/GitLab webhook to Config Server
2. Slack/Email notifications for changes
3. Audit log aggregation

### Change Metrics

Track configuration change metrics:

- Change frequency
- Change success rate
- Rollback frequency
- Time to deploy

## Best Practices

1. **Test Locally First**: Always test configuration changes locally
2. **Incremental Changes**: Make small, incremental changes
3. **Document Changes**: Include clear commit messages
4. **Review Process**: Never bypass review process
5. **Environment Isolation**: Test in lower environments first
6. **Encryption**: Always encrypt sensitive values
7. **Validation**: Run validation before committing
8. **Monitoring**: Monitor impact of changes

## Tools and Scripts

### validate-config.sh

Validates configuration files:

```bash
#!/bin/bash
# Validates YAML syntax and required properties
./validate-config.sh
```

### encrypt-value.sh

Encrypts sensitive values:

```bash
#!/bin/bash
# Encrypts a value for configuration
./encrypt-value.sh "my-secret-password"
```

### diff-environments.sh

Compares configurations across environments:

```bash
#!/bin/bash
# Shows differences between environments
./diff-environments.sh dev prod
```

## Support

For questions or issues:

1. Check existing documentation
2. Review commit history
3. Contact the Platform Team
4. Create an issue in the repository