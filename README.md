# Zamaz Debate MCP Services

A multi-service Model Context Protocol (MCP) system for managing AI-powered debates with multi-tenant support.

## Architecture Overview

The system consists of four microservices:

1. **mcp-context**: Multi-tenant context management service
2. **mcp-llm**: LLM provider gateway (Claude, OpenAI, Gemini, Llama)
3. **mcp-debate**: Debate orchestration service
4. **mcp-rag**: Retrieval Augmented Generation service

## Quick Start

### Prerequisites

- Docker and Docker Compose
- API keys for LLM providers (OpenAI, Anthropic, Google)
- Python 3.11+ (for local development)

### Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/zamaz-debate-mcp.git
cd zamaz-debate-mcp
```

2. **🔒 SECURITY CRITICAL**: Set up environment variables:
```bash
cp .env.example .env
# Edit .env with your REAL API keys - NEVER commit this file!
```

⚠️ **IMPORTANT**: The `.env` file contains sensitive API keys. Never commit it to version control!

3. Start all services:
```bash
docker-compose up -d
```

4. Check service health:
```bash
docker-compose ps
docker-compose logs -f
```

## Service Details

### MCP-Context Service (Port 5001)

Manages conversation contexts with multi-tenant isolation.

**Key Features:**
- Organization-based isolation
- Context versioning and history
- Cross-organization sharing
- Token optimization strategies

**MCP Tools:**
- `create_context`: Create new context
- `append_to_context`: Add messages
- `get_context_window`: Retrieve optimized context
- `share_context`: Share with other organizations
- `compress_context`: Reduce token usage

### MCP-LLM Service (Port 5002)

Provides unified access to multiple LLM providers.

**Supported Providers:**
- Claude (Anthropic)
- GPT-4/GPT-3.5 (OpenAI)
- Gemini (Google)
- Llama (Local via Ollama)

**MCP Tools:**
- `complete`: Generate completion
- `stream_complete`: Stream responses
- `list_models`: Available models
- `estimate_tokens`: Token counting

### MCP-Debate Service (Port 5003)

Orchestrates multi-participant debates.

**Features:**
- Multiple debate formats
- Turn management
- Rule enforcement
- Participant configuration

**MCP Tools:**
- `create_debate`: Initialize debate
- `add_message`: Add to debate
- `get_next_turn`: Orchestrate turns
- `summarize_debate`: Generate summary

### MCP-RAG Service (Port 5004)

Enhances responses with retrieval augmented generation.

**Features:**
- Document ingestion
- Vector search
- Knowledge base management
- Source attribution

**MCP Tools:**
- `create_knowledge_base`: New KB
- `ingest_documents`: Add documents
- `search`: Semantic search
- `augment_context`: Enhance with retrieved info

## Usage Examples

### Creating a Debate

```python
# Using MCP client
client = MCPClient("http://localhost:5003")

# Create debate
debate = await client.call_tool("create_debate", {
    "name": "AI Ethics Debate",
    "participants": [
        {
            "name": "Pro-Regulation",
            "llm_config": {
                "provider": "claude",
                "model": "claude-3-opus-20240229",
                "system_prompt": "Argue for AI regulation"
            }
        },
        {
            "name": "Pro-Innovation", 
            "llm_config": {
                "provider": "openai",
                "model": "gpt-4",
                "system_prompt": "Argue against excessive regulation"
            }
        }
    ],
    "max_rounds": 5
})
```

### Using Context Service

```python
# Create context
context = await context_client.call_tool("create_context", {
    "namespace_id": "debates",
    "name": "Climate Debate 2024",
    "initial_messages": [
        {"role": "system", "content": "Debate about climate policies"}
    ]
})

# Get optimized window
window = await context_client.call_tool("get_context_window", {
    "context_id": context["id"],
    "max_tokens": 8000,
    "strategy": "sliding_window_with_summary"
})
```

## Development

### Local Development

Each service can be run locally:

```bash
# Context service
cd mcp-context
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python -m src.mcp_server

# Similar for other services
```

### Testing

```bash
# Run tests for all services
./scripts/test-all.sh

# Test individual service
cd mcp-llm
pytest tests/
```

### Adding New LLM Providers

1. Create provider class in `mcp-llm/src/providers/`
2. Implement `BaseLLMProvider` interface
3. Register in provider factory
4. Add configuration

## Monitoring

Enable monitoring stack:

```bash
docker-compose --profile monitoring up -d
```

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

## Security Considerations

1. **API Keys**: Store securely, never commit
2. **Multi-tenancy**: Complete data isolation
3. **Authentication**: JWT-based inter-service auth
4. **Rate Limiting**: Per-organization limits
5. **Audit Logging**: All operations logged

## Troubleshooting

### Common Issues

1. **Service won't start**: Check logs with `docker-compose logs [service-name]`
2. **Database connection**: Ensure PostgreSQL is healthy
3. **LLM timeouts**: Increase timeout in environment
4. **Memory issues**: Adjust Docker memory limits

### Debug Mode

Enable debug logging:
```bash
LOG_LEVEL=DEBUG docker-compose up
```

## 🔒 Security

**Critical Security Information:**

- **Never commit `.env` files** - they contain sensitive API keys
- **Use `.env.example` as template** - safe to commit
- **Rotate API keys regularly** - especially in production
- **Monitor API usage** - watch for unusual patterns

See [SECURITY.md](./SECURITY.md) for comprehensive security guidelines.

### Quick Security Check:
```bash
# Verify no .env files are tracked by git
git ls-files | grep -E '\.env$'
# Should return nothing

# Run security audit
make security-audit
```

## 🚀 Development Commands

Use the provided Makefile for common operations:

```bash
# Setup development environment
make setup

# Start all services
make up

# Run tests
make test

# Run concurrency tests
make test-concurrency

# Clean everything
make clean
```

## Contributing

1. Fork the repository
2. Create feature branch
3. Make changes with tests
4. Submit pull request

## License

[Your License]

## Support

For issues and questions:
- GitHub Issues: [link]
- Documentation: See `/docs` folder
- Email: support@example.com