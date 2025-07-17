# Security Guidelines for MCP Debate System

## 🔒 Environment Variables Security

### ⚠️ CRITICAL: Never Commit API Keys

This project uses sensitive API keys and credentials that must **NEVER** be committed to version control.

### Environment File Structure

```
.env.example    ← Template file (safe to commit)
.env           ← Your actual credentials (NEVER commit)
.env.local     ← Local overrides (NEVER commit)
.env.*.local   ← Environment-specific (NEVER commit)
```

### Setting Up Your Environment

1. **Copy the template:**
   ```bash
   cp .env.example .env
   ```

2. **Fill in your actual credentials:**
   ```bash
   # Edit .env with your real API keys
   nano .env
   ```

3. **Required API Keys:**
   - **OpenAI API Key**: Get from [OpenAI Platform](https://platform.openai.com/api-keys)
   - **Anthropic API Key**: Get from [Anthropic Console](https://console.anthropic.com/)
   - **Google API Key**: Get from [Google Cloud Console](https://console.cloud.google.com/)

### API Key Security Best Practices

#### ✅ DO:
- Keep API keys in `.env` files only
- Use different keys for development/production
- Rotate API keys regularly
- Monitor API key usage
- Use environment-specific configurations
- Generate strong random values for JWT secrets

#### ❌ DON'T:
- Commit `.env` files to git
- Share API keys in chat/email
- Use the same keys across environments
- Hard-code API keys in source code
- Use weak or default passwords

### GitIgnore Protection

The following files are automatically excluded from git:

```
# All .env files
.env
.env.local
.env.*.local
*.env

# Backup files that might contain secrets
api_keys.txt
keys.json
secrets.json
```

### Emergency API Key Exposure

If you accidentally commit API keys:

1. **Immediately revoke the exposed keys:**
   - OpenAI: [Revoke at OpenAI Platform](https://platform.openai.com/api-keys)
   - Anthropic: [Revoke at Anthropic Console](https://console.anthropic.com/)
   - Google: [Revoke at Google Cloud Console](https://console.cloud.google.com/)

2. **Generate new API keys**

3. **Update your `.env` file with new keys**

4. **Remove the commit with sensitive data:**
   ```bash
   # If the commit hasn't been pushed yet
   git reset --soft HEAD~1
   git add .
   git commit -m "Remove sensitive data and update security"
   
   # If already pushed, consider repository cleanup
   # Contact your repository administrator
   ```

## 🛡️ Multi-Tenant Security

### Organization Isolation

The system implements strict organization isolation:

- **Data Separation**: Each organization's data is completely isolated
- **API Authentication**: All requests require `X-Organization-ID` header
- **Rate Limiting**: Per-organization rate limits prevent abuse
- **Context Isolation**: Debate contexts are scoped to organizations

### Security Headers

Required headers for API requests:
```
X-Organization-ID: your-org-id
Content-Type: application/json
Authorization: Bearer your-jwt-token (if using auth)
```

## 🔐 Authentication & Authorization

### JWT Configuration



### API Key Management

For production deployments:

1. **Use environment-specific keys**
2. **Implement key rotation**
3. **Monitor usage patterns**
4. **Set up usage alerts**

## 🚨 Security Monitoring

### Rate Limiting

The system includes built-in rate limiting:
- **60 requests/minute** per organization
- **1000 requests/hour** per organization
- **10 concurrent requests** globally

### Monitoring Endpoints

Check security status:
```bash
# System health
curl http://localhost:8003/health

# Concurrency metrics
curl http://localhost:8003/metrics

# Organization metrics
curl -H "X-Organization-ID: your-org" http://localhost:8003/org-metrics
```

## 🔧 Production Security Checklist

### Before Deployment:

- [ ] All API keys are in `.env` files (not committed)
- [ ] Strong JWT secret generated
- [ ] Database passwords changed from defaults
- [ ] Rate limiting configured appropriately
- [ ] Monitoring and alerting set up
- [ ] SSL/TLS certificates configured
- [ ] Firewall rules configured
- [ ] Regular security updates scheduled

### Environment Variables Audit:

```bash
# Check for any .env files in git
git ls-files | grep -E '\.env'

# Should return nothing except .env.example

# Check for any hardcoded secrets
grep -r "sk-" --exclude-dir=node_modules --exclude="*.md" .
grep -r "api.*key" --exclude-dir=node_modules --exclude="*.md" .
```

## 📋 Security Incident Response

If you suspect a security breach:

1. **Immediately revoke all API keys**
2. **Change all passwords**
3. **Review access logs**
4. **Update all environment variables**
5. **Restart all services**
6. **Monitor for unusual activity**

## 🔍 Regular Security Maintenance

### Weekly:
- Review API key usage
- Check for failed authentication attempts
- Monitor rate limiting patterns

### Monthly:
- Rotate API keys
- Review organization access
- Update dependencies

### Quarterly:
- Security audit
- Penetration testing
- Update security documentation

---

**Remember: Security is everyone's responsibility. When in doubt, err on the side of caution.**