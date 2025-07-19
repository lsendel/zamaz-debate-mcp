# Security Review Checklist for Configuration Changes

This checklist must be completed for all configuration changes before merging to main branch.

## Pre-Commit Checks

### 1. Sensitive Data Protection
- [ ] All passwords are encrypted using `{cipher}` prefix
- [ ] All API keys are encrypted using `{cipher}` prefix
- [ ] All secrets/tokens are encrypted using `{cipher}` prefix
- [ ] No hardcoded credentials in any configuration file
- [ ] No sensitive data in comments or descriptions

### 2. Environment Variables
- [ ] All environment-specific values use placeholders `${VARIABLE_NAME}`
- [ ] Default values are safe and non-sensitive
- [ ] Required environment variables are documented
- [ ] No production values in development configurations

### 3. File Permissions
- [ ] Configuration files have appropriate permissions (644 or less)
- [ ] No executable permissions on configuration files
- [ ] Git file mode changes are reviewed

### 4. Encryption Standards
- [ ] Using strong encryption keys (minimum 256-bit)
- [ ] Encryption keys are stored securely (not in repo)
- [ ] No weak encryption algorithms (DES, MD5, SHA1)
- [ ] Encrypted values can be decrypted by Config Server

## Code Review Checks

### 5. Configuration Structure
- [ ] Follows established naming conventions
- [ ] Proper YAML/Properties syntax
- [ ] No duplicate keys or conflicting values
- [ ] Appropriate use of profiles (dev, staging, prod)

### 6. Security Headers
- [ ] CORS configurations are restrictive
- [ ] Security headers are properly configured
- [ ] TLS/SSL settings are secure
- [ ] Authentication/Authorization settings reviewed

### 7. Connection Security
- [ ] Database connections use SSL/TLS
- [ ] API endpoints use HTTPS
- [ ] Message queues use secure connections
- [ ] Redis connections are password-protected

### 8. Audit and Compliance
- [ ] Changes are logged in commit message
- [ ] Business justification documented
- [ ] Compliance requirements checked
- [ ] Security team approval (if required)

## Post-Merge Checks

### 9. Deployment Verification
- [ ] Configuration loads correctly in target environment
- [ ] No startup errors related to configuration
- [ ] Services can decrypt encrypted values
- [ ] Health checks pass after deployment

### 10. Monitoring and Alerts
- [ ] Configuration access is monitored
- [ ] Alerts configured for configuration errors
- [ ] Audit logs capture configuration changes
- [ ] Performance impact assessed

## Emergency Procedures

### In Case of Exposed Credentials:
1. Immediately rotate the exposed credential
2. Update encrypted value in configuration
3. Force refresh all affected services
4. Review audit logs for unauthorized access
5. Document incident and remediation

### Rollback Procedure:
1. Revert configuration commit in Git
2. Tag the revert with incident number
3. Trigger configuration refresh
4. Verify services are using previous configuration
5. Investigate root cause

## Sign-Off

- **Developer**: _________________ Date: _______
- **Reviewer**: _________________ Date: _______
- **Security**: _________________ Date: _______ (if required)

## Notes
_Add any additional security considerations or exceptions here_