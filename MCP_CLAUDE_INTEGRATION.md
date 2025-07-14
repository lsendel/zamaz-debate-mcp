# MCP Claude CLI Integration Guide

## Overview
This guide shows how to connect the Zamaz Debate MCP services to Claude Desktop using the Model Context Protocol (MCP).

## Available MCP Services

### 1. Organization Service (Port 5005)
- **Endpoint**: `http://localhost:5005/mcp`
- **Features**: Organization and user management
- **Tools**:
  - `create_organization` - Create a new organization
  - `list_organizations` - List all organizations
  - `create_user` - Create a new user

### 2. LLM Gateway Service (Port 5002)
- **Endpoint**: `http://localhost:5002/mcp`
- **Features**: Multi-provider LLM gateway
- **Tools**:
  - `list_providers` - List available LLM providers
  - `generate_completion` - Generate text completion
  - `get_provider_status` - Check provider status

### 3. Debate Controller Service (Port 5013)
- **Endpoint**: `http://localhost:5013/mcp`
- **Features**: Debate orchestration and management
- **Tools**:
  - `create_debate` - Create a new debate
  - `get_debate` - Get debate details
  - `list_debates` - List debates for an organization
  - `submit_turn` - Submit a turn in a debate

## Setup Instructions

### 1. Start the Services
```bash
# Make the script executable
chmod +x start-mcp-services.sh

# Start all services
./start-mcp-services.sh
```

### 2. Configure Claude Desktop

Add to your Claude Desktop config file:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "zamaz-organization": {
      "url": "http://localhost:5005/mcp",
      "transport": "http"
    },
    "zamaz-llm": {
      "url": "http://localhost:5002/mcp",
      "transport": "http"
    },
    "zamaz-debate": {
      "url": "http://localhost:5013/mcp",
      "transport": "http"
    }
  }
}
```

### 3. Restart Claude Desktop
After updating the config, restart Claude Desktop to load the MCP servers.

## Usage Examples

### Using Claude CLI

```bash
# List available tools from a service
claude --mcp zamaz-organization list-tools

# Create an organization
claude --mcp zamaz-organization call-tool create_organization \
  '{"name": "Tech Debates Inc", "description": "AI Ethics Debate Organization"}'

# List LLM providers
claude --mcp zamaz-llm list-tools
claude --mcp zamaz-llm call-tool list_providers

# Generate text using a specific provider
claude --mcp zamaz-llm call-tool generate_completion \
  '{"provider": "claude", "prompt": "What are the ethics of AI?", "maxTokens": 100}'

# Create a debate
claude --mcp zamaz-debate call-tool create_debate \
  '{"topic": "AI Should Have Rights", "format": "OXFORD", "organizationId": "org-123", "participants": ["claude", "gpt-4"]}'

# Get debate status
claude --mcp zamaz-debate call-tool get_debate \
  '{"debateId": "debate-123"}'

# Submit a turn in a debate
claude --mcp zamaz-debate call-tool submit_turn \
  '{"debateId": "debate-123", "participantId": "part-456", "content": "I argue that AI consciousness..."}'
```

### Using in Claude Desktop Chat

Once configured, you can use natural language in Claude Desktop:

```
"Using the zamaz-organization service, create a new organization called 'AI Ethics Society'"

"List all available LLM providers using the zamaz-llm service"

"Create a new debate about 'The Future of Work' using the zamaz-debate service"
```

## API Documentation

Each service provides Swagger UI documentation:
- Organization API: http://localhost:5005/swagger-ui.html
- LLM Gateway API: http://localhost:5002/swagger-ui.html
- Debate Controller API: http://localhost:5013/swagger-ui.html

## Health Checks

Verify services are running:
```bash
# Check individual service health
curl http://localhost:5005/actuator/health
curl http://localhost:5002/actuator/health
curl http://localhost:5013/actuator/health

# Check MCP endpoints
curl http://localhost:5005/mcp
curl http://localhost:5002/mcp
curl http://localhost:5013/mcp
```

## Troubleshooting

### Services not responding
1. Check if Docker services are running: `docker-compose ps`
2. Check logs: `docker-compose logs -f [service-name]`
3. Ensure ports are not blocked by firewall

### Claude Desktop not connecting
1. Verify config file location and JSON syntax
2. Check Claude Desktop logs for MCP errors
3. Ensure services are accessible at the configured URLs

### Port conflicts
If ports are already in use, update the `.env` file:
```env
MCP_ORGANIZATION_PORT=5006
MCP_LLM_PORT=5003
MCP_CONTROLLER_PORT=5014
```

## Advanced Configuration

### Custom MCP Transport
For production, consider using stdio transport with Docker:

```json
{
  "mcpServers": {
    "zamaz-debate": {
      "command": "docker",
      "args": ["exec", "-i", "zamaz-debate-mcp-mcp-controller-j-1", "java", "-jar", "/app/mcp-controller.jar", "--mcp-stdio"],
      "transport": "stdio"
    }
  }
}
```

### Environment Variables
Set API keys in `.env` file:
```env
ANTHROPIC_API_KEY=your-key-here
OPENAI_API_KEY=your-key-here
GOOGLE_API_KEY=your-key-here
```

## Security Notes
- MCP endpoints are exposed on localhost only by default
- Use proper authentication in production
- Never expose MCP endpoints to public internet without authentication
- Consider using TLS for production deployments