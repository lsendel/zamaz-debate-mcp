# MCP Debate System

A comprehensive Model Context Protocol (MCP) system for managing AI-powered debates with enterprise-grade multi-tenant support, real-time processing, and advanced monitoring.

## 🚀 Features

- **Multi-AI Debates**: Support for Claude, OpenAI, Google Gemini, and Llama
- **Real-time Processing**: WebSocket-based live debate updates
- **Multi-tenant Architecture**: Complete organization isolation and resource management
- **Enterprise Security**: JWT authentication, rate limiting, audit logging
- **Comprehensive Monitoring**: Prometheus metrics, Grafana dashboards, distributed tracing
- **High Performance**: Redis caching, database optimization, async processing
- **Production Ready**: CI/CD pipelines, blue-green deployments, disaster recovery

## Architecture Overview

The system follows a microservices architecture with consolidated services for better performance:

1. **mcp-organization**: Multi-tenant organization and user management
2. **mcp-gateway**: API gateway with authentication and rate limiting
3. **mcp-debate-engine**: Consolidated debate orchestration (replaces controller, context, debate)
4. **mcp-llm**: LLM provider gateway with failover and caching
5. **mcp-rag**: Retrieval Augmented Generation with vector search
6. **mcp-security**: Cross-cutting security services

## Quick Start

### Prerequisites

- Docker and Docker Compose
- API keys for LLM providers (OpenAI, Anthropic, Google)
- Java 21 (for development)
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

## 📚 Documentation

Comprehensive documentation is organized for easy navigation:

- **[📖 Complete Documentation Index](./docs/index.md)** - Start here!
- **[🏗️ Architecture Guide](./docs/architecture/README.md)** - System design and components
- **[🔌 API Reference](./docs/api/README.md)** - REST APIs, WebSocket, examples
- **[💻 Development Guide](./docs/development/README.md)** - Setup, workflow, testing
- **[🚀 Operations Manual](./docs/operations/README.md)** - Deployment, monitoring, maintenance
- **[🔒 Security Documentation](./docs/security/README.md)** - Security architecture and best practices

### Quick Links
- [Getting Started](./docs/development/setup.md)
- [API Examples](./docs/api/DEBATE_CURL_EXAMPLES.md)
- [Performance Testing](./performance-tests/README.md)
- [CI/CD Pipeline](./docs/ci-cd-pipeline-guide.md)

## Service Details

### MCP-Organization Service (Port 5005)

Manages organizations and multi-tenant functionality.

**Key Features:**
- Organization management
- API key management
- User management
- Role-based access control

**MCP Tools:**
- `create_organization`: Create new organization
- `get_organization`: Get organization details
- `create_api_key`: Create new API key
- `verify_api_key`: Verify API key validity

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

### MCP-Controller Service (Port 5013)

Orchestrates debates and manages debate flow.

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

### MCP-Template Service (Port 5006)

Manages templates for debates and other content.

**Features:**
- Template management
- Template versioning
- Template instantiation
- Template sharing

**MCP Tools:**
- `create_template`: Create new template
- `get_template`: Get template details
- `update_template`: Update template
- `instantiate_template`: Create instance from template

## Usage Examples

### Creating a Debate

```python
# Using MCP client
client = MCPClient("http://localhost:5013")

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

### Using RAG Service

```python
# Create knowledge base
kb = await rag_client.call_tool("create_knowledge_base", {
    "name": "Climate Research",
    "description": "Scientific papers on climate change",
    "embedding_model": "text-embedding-3-large"
})

# Ingest documents
docs = await rag_client.call_tool("ingest_documents", {
    "knowledge_base_id": kb["id"],
    "documents": [
        {"content": "Climate change is causing sea levels to rise...", "metadata": {"source": "IPCC Report"}}
    ]
})

# Search knowledge base
results = await rag_client.call_tool("search", {
    "knowledge_base_id": kb["id"],
    "query": "impact of rising sea levels",
    "top_k": 3
})
```

## Development

For detailed development instructions, see the [Development Guide](./docs/development/setup.md).

### Local Development

Each service can be run locally:

```bash
# Java services
cd mcp-service-name
mvn spring-boot:run
```

### Testing

```bash
# Run tests for all services
./scripts/test-all.sh

# Test individual service
cd mcp-llm
mvn test
```

## Monitoring

Enable monitoring stack:

```bash
docker-compose --profile monitoring up -d
```

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

For detailed monitoring instructions, see the [Monitoring Guide](./docs/operations/monitoring.md).

## Security Considerations

1. **API Keys**: Store securely, never commit
2. **Multi-tenancy**: Complete data isolation
3. **Authentication**: JWT-based inter-service auth
4. **Rate Limiting**: Per-organization limits
5. **Audit Logging**: All operations logged

For comprehensive security guidelines, see the [Security Documentation](./docs/security/guidelines.md).

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
