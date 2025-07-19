# MCP Config Server

Spring Cloud Config Server for centralized configuration management across all MCP microservices.

## Overview

This module provides a centralized configuration server that serves configuration files from a Git repository to all MCP microservices. It supports:

- Environment-specific configuration profiles (dev, staging, prod)
- Encrypted sensitive properties
- Dynamic configuration refresh
- Git-backed configuration storage
- Basic authentication for security

## Configuration

### Environment Variables

The following environment variables should be set in production:

- `CONFIG_GIT_REPO_URI`: Git repository URI containing configuration files
- `CONFIG_SERVER_USERNAME`: Username for Config Server authentication
- `CONFIG_SERVER_PASSWORD`: Password for Config Server authentication  
- `CONFIG_ENCRYPTION_KEY`: Key for encrypting/decrypting sensitive properties

### Default Settings

- **Port**: 8888
- **Git Repository**: <https://github.com/zamaz/mcp-config-repo.git> (default)
- **Authentication**: Basic auth with configurable credentials
- **Health Check**: Available at `/actuator/health`

## Usage

### Starting the Server

```bash
# Using Maven
mvn spring-boot:run

# Using Docker
docker build -t mcp-config-server .
docker run -p 8888:8888 \
  -e CONFIG_GIT_REPO_URI=https://github.com/your-org/config-repo.git \
  -e CONFIG_SERVER_PASSWORD=your-secure-password \
  -e CONFIG_ENCRYPTION_KEY=your-encryption-key \
  mcp-config-server
```

### Accessing Configuration

Services can access their configuration at:

- `http://localhost:8888/{application}/{profile}`
- `http://localhost:8888/{application}/{profile}/{label}`

Examples:

- `http://localhost:8888/mcp-organization/dev`
- `http://localhost:8888/mcp-llm/prod`

### Health Check

Check server health at: `http://localhost:8888/actuator/health`

## Security

- Basic authentication is required for all endpoints except health checks
- Sensitive properties should be encrypted using the `{cipher}` prefix
- HTTPS should be used in production environments
- Git repository should be private and access-controlled

## Testing

Run tests with:

```bash
mvn test
```

The test suite includes:

- Application context loading verification
- Basic configuration serving functionality
- Security configuration validation
