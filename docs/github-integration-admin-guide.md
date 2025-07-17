# Kiro GitHub Integration - Administrator Guide

## Overview

This guide provides comprehensive information for administrators setting up and managing the Kiro GitHub Integration across their organization.

## Installation and Setup

### System Requirements

- **Server Requirements**:
  - Python 3.8+
  - 4GB RAM minimum (8GB recommended)
  - 10GB disk space
  - Network access to GitHub API

- **GitHub Requirements**:
  - GitHub organization admin access
  - Ability to install GitHub Apps
  - Webhook endpoint accessibility

### GitHub App Setup

1. **Create GitHub App**
   ```bash
   # Navigate to GitHub Settings > Developer settings > GitHub Apps
   # Click "New GitHub App"
   ```

2. **Configure App Settings**
   - **App Name**: Kiro AI Code Review
   - **Homepage URL**: Your organization's URL
   - **Webhook URL**: `https://your-server.com/api/webhooks/github`
   - **Webhook Secret**: Generate a secure random string

3. **Set Permissions**
   ```yaml
   Repository Permissions:
     - Pull requests: Read & write
     - Contents: Read & write
     - Issues: Read
     - Metadata: Read
     - Workflows: Read
   
   Organization Permissions:
     - Members: Read (optional)
   
   Subscribe to Events:
     - Pull request
     - Pull request review
     - Issue comment
     - Push
   ```

4. **Generate Credentials**
   - Download the private key
   - Note the App ID and Client ID
   - Generate and save the Client Secret

### Server Deployment

#### Docker Deployment (Recommended)

1. **Create Docker Compose File**
   ```yaml
   version: '3.8'
   services:
     kiro-github:
       image: kiro/github-integration:latest
       ports:
         - "5000:5000"
       environment:
         - GITHUB_APP_ID=${GITHUB_APP_ID}
         - GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}
         - GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}
         - GITHUB_WEBHOOK_SECRET=${GITHUB_WEBHOOK_SECRET}
         - GITHUB_PRIVATE_KEY_PATH=/app/private-key.pem
       volumes:
         - ./private-key.pem:/app/private-key.pem:ro
         - ./data:/app/data
       restart: unless-stopped
   
     postgres:
       image: postgres:13
       environment:
         - POSTGRES_DB=kiro_github
         - POSTGRES_USER=kiro
         - POSTGRES_PASSWORD=${DB_PASSWORD}
       volumes:
         - postgres_data:/var/lib/postgresql/data
   
   volumes:
     postgres_data:
   ```

2. **Environment Configuration**
   ```bash
   # Create .env file
   GITHUB_APP_ID=123456
   GITHUB_CLIENT_ID=Iv1.abc123def456
   GITHUB_CLIENT_SECRET=your_client_secret
   GITHUB_WEBHOOK_SECRET=your_webhook_secret
   DB_PASSWORD=secure_password
   ```

3. **Deploy**
   ```bash
   docker-compose up -d
   ```

#### Manual Deployment

1. **Install Dependencies**
   ```bash
   pip install -r requirements.txt
   ```

2. **Configure Environment**
   ```bash
   export GITHUB_APP_ID=123456
   export GITHUB_CLIENT_ID=Iv1.abc123def456
   export GITHUB_CLIENT_SECRET=your_client_secret
   export GITHUB_WEBHOOK_SECRET=your_webhook_secret
   export GITHUB_PRIVATE_KEY_PATH=/path/to/private-key.pem
   ```

3. **Initialize Database**
   ```bash
   python -m github.scripts.analytics_collector --init-db
   python -m github.scripts.security_manager --init-db
   ```

4. **Start Services**
   ```bash
   # Start webhook server
   python .github/scripts/webhook_server.py &
   
   # Start analytics dashboard
   python .github/scripts/analytics_dashboard.py &
   ```

### SSL/TLS Configuration

For production deployments, configure SSL/TLS:

```nginx
server {
    listen 443 ssl;
    server_name your-kiro-server.com;
    
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    location /api/webhooks/github {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /dashboard {
        proxy_pass http://localhost:5001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Configuration Management

### Global Configuration

Create `/etc/kiro/github-config.yml`:

```yaml
# Global Kiro GitHub Integration Configuration
global:
  rate_limits:
    github_api: 5000  # Requests per hour
    webhook_processing: 100  # Concurrent webhooks
  
  security:
    token_expiry_hours: 24
    audit_retention_days: 90
    encryption_key_rotation_days: 30
  
  performance:
    max_files_per_review: 50
    max_review_time_seconds: 300
    worker_threads: 5

defaults:
  review:
    depth: standard
    focus_areas: [security, performance, style]
    auto_fix: true
    comment_style: educational
  
  rules:
    custom_rules_enabled: true
    rule_sets:
      - name: "Security Rules"
        enabled: true
      - name: "Performance Rules"
        enabled: true
      - name: "Style Guide"
        enabled: true
```

### Repository-Level Configuration

Repositories can override global settings with `.kiro/config/github.yml`:

```yaml
# Repository-specific overrides
review:
  depth: thorough  # Override global setting
  focus_areas:
    - security
    - performance
    - documentation  # Add documentation focus

rules:
  rule_sets:
    - name: "Custom Team Rules"
      enabled: true
      rules:
        - id: "team-naming-convention"
          severity: minor
          description: "Follow team naming conventions"
```

### Configuration Validation

Use the CLI tool to validate configurations:

```bash
# Validate global configuration
python .github/scripts/config_cli.py validate --config /etc/kiro/github-config.yml

# Validate repository configuration
python .github/scripts/config_cli.py validate --config .kiro/config/github.yml

# Test configuration for a repository
python .github/scripts/config_cli.py get --repo owner/repo --local .kiro/config/github.yml
```

## User Management

### Access Control

Configure user permissions:

```bash
# Grant repository access
python .github/scripts/security_manager.py grant-permission \
  --user john.doe \
  --resource repository \
  --permission read \
  --resource-id owner/repo

# Grant admin access
python .github/scripts/security_manager.py grant-permission \
  --user admin.user \
  --resource system \
  --permission admin
```

### Token Management

```bash
# Create access token
python .github/scripts/security_manager.py create-token \
  --user john.doe \
  --scope "repo:read,repo:write" \
  --expires-hours 24

# Revoke token
python .github/scripts/security_manager.py revoke-token \
  --token kiro_abc123_1234567890
```

## Monitoring and Analytics

### Analytics Dashboard

Access the analytics dashboard at `https://your-server.com/dashboard`

Key metrics include:
- Review completion rates
- Issue detection statistics
- Suggestion acceptance rates
- Performance metrics
- User engagement statistics

### Health Monitoring

Monitor system health:

```bash
# Check webhook server status
curl https://your-server.com/api/webhooks/status

# Check analytics dashboard
curl https://your-server.com/api/stats

# View system metrics
python .github/scripts/analytics_collector.py --metrics
```

### Log Management

Configure log rotation and retention:

```bash
# Configure logrotate
cat > /etc/logrotate.d/kiro-github << EOF
/var/log/kiro/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 kiro kiro
}
EOF
```

## Security and Compliance

### Security Best Practices

1. **Network Security**
   - Use HTTPS for all communications
   - Implement IP allowlisting for webhooks
   - Configure firewall rules appropriately

2. **Access Control**
   - Use principle of least privilege
   - Regularly rotate access tokens
   - Monitor access patterns

3. **Data Protection**
   - Enable encryption at rest
   - Use secure key management
   - Implement data retention policies

### Audit and Compliance

Generate compliance reports:

```bash
# Generate audit report
python .github/scripts/security_manager.py audit-report \
  --start-date 2024-01-01 \
  --end-date 2024-01-31 \
  --format json

# Generate data processing report (GDPR)
python .github/scripts/security_manager.py data-processing-report \
  --start-date 2024-01-01 \
  --end-date 2024-01-31
```

### Backup and Recovery

1. **Database Backup**
   ```bash
   # Backup analytics database
   sqlite3 .kiro/data/analytics.db ".backup backup-$(date +%Y%m%d).db"
   
   # Backup audit database
   sqlite3 .kiro/data/audit.db ".backup audit-backup-$(date +%Y%m%d).db"
   ```

2. **Configuration Backup**
   ```bash
   # Backup configurations
   tar -czf config-backup-$(date +%Y%m%d).tar.gz .kiro/config/
   ```

## Troubleshooting

### Common Issues

1. **Webhook Delivery Failures**
   ```bash
   # Check webhook logs
   tail -f /var/log/kiro/webhook.log
   
   # Test webhook endpoint
   curl -X POST https://your-server.com/api/webhooks/github \
     -H "Content-Type: application/json" \
     -d '{"test": true}'
   ```

2. **Performance Issues**
   ```bash
   # Check system resources
   htop
   
   # Monitor database performance
   sqlite3 .kiro/data/analytics.db "EXPLAIN QUERY PLAN SELECT * FROM reviews;"
   
   # Check queue status
   python .github/scripts/pr_processor.py --status
   ```

3. **Authentication Problems**
   ```bash
   # Validate GitHub App credentials
   python .github/scripts/github_auth.py --test
   
   # Check token validity
   python .github/scripts/security_manager.py validate-token --token kiro_abc123
   ```

### Diagnostic Tools

1. **System Health Check**
   ```bash
   python .github/scripts/diagnostics.py --full-check
   ```

2. **Configuration Validation**
   ```bash
   python .github/scripts/config_cli.py validate-all
   ```

3. **Performance Analysis**
   ```bash
   python .github/scripts/analytics_collector.py --performance-report
   ```

## Maintenance

### Regular Maintenance Tasks

1. **Daily**
   - Monitor system health
   - Check error logs
   - Verify webhook delivery

2. **Weekly**
   - Review analytics reports
   - Clean up expired tokens
   - Update rule effectiveness

3. **Monthly**
   - Rotate encryption keys
   - Generate compliance reports
   - Review user access permissions
   - Update system dependencies

### Automated Maintenance

Set up cron jobs for automated maintenance:

```bash
# Daily cleanup
0 2 * * * /usr/local/bin/python /app/.github/scripts/security_manager.py cleanup-tokens

# Weekly analytics update
0 3 * * 0 /usr/local/bin/python /app/.github/scripts/learning_system.py update-learning

# Monthly backup
0 1 1 * * /app/scripts/backup.sh
```

## Scaling and Performance

### Horizontal Scaling

For high-volume deployments:

1. **Load Balancer Configuration**
   ```nginx
   upstream kiro_backend {
       server kiro-1:5000;
       server kiro-2:5000;
       server kiro-3:5000;
   }
   
   server {
       location / {
           proxy_pass http://kiro_backend;
       }
   }
   ```

2. **Database Scaling**
   - Use PostgreSQL for better performance
   - Implement read replicas
   - Consider database sharding for very large deployments

3. **Queue Management**
   - Use Redis for job queuing
   - Implement worker pools
   - Monitor queue depths

### Performance Optimization

1. **Caching**
   - Enable repository configuration caching
   - Cache GitHub API responses
   - Use CDN for static assets

2. **Resource Allocation**
   - Tune worker thread counts
   - Optimize memory usage
   - Monitor CPU utilization

## Support and Updates

### Getting Support

1. **Documentation**: Check this guide and user documentation
2. **Logs**: Review system logs for error details
3. **Community**: Check GitHub issues and discussions
4. **Professional Support**: Contact your Kiro support team

### Updates and Upgrades

1. **Backup Before Updates**
   ```bash
   ./scripts/backup-all.sh
   ```

2. **Update Process**
   ```bash
   # Pull latest version
   docker-compose pull
   
   # Update services
   docker-compose up -d
   
   # Run migrations if needed
   python .github/scripts/migrate.py
   ```

3. **Rollback Procedure**
   ```bash
   # Rollback to previous version
   docker-compose down
   docker-compose up -d --scale kiro-github=0
   # Restore from backup if needed
   ./scripts/restore-backup.sh backup-20240101
   ```

This administrator guide provides comprehensive information for managing the Kiro GitHub Integration in production environments. For user-facing documentation, refer to the [User Guide](github-integration-user-guide.md).