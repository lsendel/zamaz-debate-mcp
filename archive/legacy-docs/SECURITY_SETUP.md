# Security Setup Guide

## Quick Start

1. **Copy environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Generate secure secrets:**
   ```bash
   # Generate JWT secret
   openssl rand -base64 32
   
   # Generate API key salt  
   openssl rand -base64 32
   ```

3. **Get API keys from providers:**
   - [OpenAI](https://platform.openai.com/api-keys)
   - [Anthropic](https://console.anthropic.com/)
   - [Google](https://console.cloud.google.com/)

4. **Install security hooks:**
   ```bash
   git config core.hooksPath .githooks
   chmod +x .githooks/pre-commit
   ```

## Environment Variables Reference

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `POSTGRES_PASSWORD` | ✅ | Database password | `your_secure_db_password` |
| `OPENAI_API_KEY` | ⚠️ | OpenAI API key | `sk-proj-...` |
| `ANTHROPIC_API_KEY` | ⚠️ | Anthropic API key | `sk-ant-api03-...` |
| `JWT_SECRET` | ✅ | JWT signing secret | (32-byte random) |
| `SONAR_TOKEN` | ❌ | SonarCloud token | `your_token_here` |

⚠️ = Required if using that LLM provider

## Security Checklist

- [ ] `.env` file created from template
- [ ] All placeholder values replaced with real values
- [ ] Secrets are 32+ characters random strings
- [ ] API keys are valid and not shared
- [ ] Pre-commit hooks installed
- [ ] `.env` file is in `.gitignore` (already done)

## Troubleshooting

**Q: Service fails to start with "Database password must be provided"**  
A: Set `POSTGRES_PASSWORD` in your `.env` file

**Q: LLM requests fail with authentication error**  
A: Check your API keys are valid and have sufficient credits

**Q: Pre-commit hook prevents my commit**  
A: The hook found potential secrets. Review and fix before committing.