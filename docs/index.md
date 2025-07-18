# MCP Debate System Documentation

Welcome to the MCP (Model Context Protocol) Debate System documentation. This system enables AI-powered debates with multi-tenant support, real-time processing, and comprehensive monitoring.

## Quick Links

- [Getting Started](#getting-started)
- [Architecture Overview](architecture/README.md)
- [API Documentation](api/README.md)
- [Development Guide](development/README.md)
- [Operations Manual](operations/README.md)
- [Security Guide](security/README.md)

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+

### Quick Start

```bash
# Clone the repository
git clone https://github.com/zamaz/mcp-debate.git
cd mcp-debate

# Start all services
make start-all

# Access the UI
open http://localhost:3000
```

### First Steps

1. **Create an Organization**: Start by creating your organization in the UI
2. **Configure AI Providers**: Add your API keys for Claude, OpenAI, etc.
3. **Create a Debate**: Set up your first debate with chosen participants
4. **Monitor Progress**: Watch the debate unfold in real-time

## Documentation Structure

### 📐 [Architecture](architecture/README.md)
- System design and components
- Multi-tenant architecture
- MCP integration patterns
- Scalability considerations

### 🔌 [API Reference](api/README.md)
- REST API endpoints
- WebSocket protocols
- Authentication & authorization
- Example requests and responses

### 💻 [Development](development/README.md)
- Setup instructions
- Coding standards
- Testing guide
- Contributing guidelines

### 🚀 [Operations](operations/README.md)
- Deployment procedures
- Monitoring & logging
- Performance tuning
- Disaster recovery

### 🔒 [Security](security/README.md)
- Security architecture
- Best practices
- Incident response
- Compliance guidelines

## Key Features

### Multi-Tenant Support
- Organization isolation
- Resource quotas
- Usage tracking
- Billing integration

### AI Integration
- Multiple AI providers (Claude, OpenAI, Gemini, Llama)
- Provider failover
- Response caching
- Rate limiting

### Real-Time Processing
- WebSocket support
- Event streaming
- Live debate updates
- Participant notifications

### Observability
- Prometheus metrics
- Grafana dashboards
- Distributed tracing
- Centralized logging

## Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/zamaz/mcp-debate/issues)
- **Discussions**: [Community forum](https://github.com/zamaz/mcp-debate/discussions)
- **Security**: security@mcp-debate.com

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.