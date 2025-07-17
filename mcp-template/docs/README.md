# MCP-Template Service Documentation

The MCP-Template service manages templates for debates and other content in the Zamaz Debate MCP system. It provides a way to create, store, and instantiate templates for various use cases.

## Overview

The MCP-Template service enables organizations to create reusable templates for debates, prompts, and other content. Templates can include variables that are replaced when instantiated, allowing for customization while maintaining consistency.

## Features

- **Template Management**: Create, update, and manage templates
- **Template Versioning**: Track template versions and changes
- **Template Categories**: Organize templates by category
- **Variable Substitution**: Replace variables when instantiating templates
- **Template Sharing**: Share templates between organizations
- **Access Control**: Control who can access and modify templates
- **Template Library**: Pre-built templates for common scenarios

## Architecture

The Template service follows a clean architecture pattern:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic for template management
- **Repositories**: Manage data persistence
- **Models**: Define template-related data structures
- **Validation**: Ensure template integrity and security

## API Endpoints

### Templates

- `POST /api/v1/templates`: Create template
- `GET /api/v1/templates`: List templates
- `GET /api/v1/templates/{id}`: Get template details
- `PUT /api/v1/templates/{id}`: Update template
- `DELETE /api/v1/templates/{id}`: Delete template

### Template Versions

- `GET /api/v1/templates/{id}/versions`: List template versions
- `GET /api/v1/templates/{id}/versions/{versionId}`: Get specific version
- `POST /api/v1/templates/{id}/versions/{versionId}/restore`: Restore version

### Template Categories

- `POST /api/v1/template-categories`: Create category
- `GET /api/v1/template-categories`: List categories
- `PUT /api/v1/template-categories/{id}`: Update category
- `DELETE /api/v1/template-categories/{id}`: Delete category

### Template Instantiation

- `POST /api/v1/templates/{id}/instantiate`: Instantiate template
- `POST /api/v1/templates/{id}/validate`: Validate template variables

### Template Sharing

- `POST /api/v1/templates/{id}/share`: Share template
- `GET /api/v1/templates/shared`: List shared templates
- `DELETE /api/v1/templates/{id}/share/{organizationId}`: Revoke sharing

### MCP Tools

The service exposes the following MCP tools:

- `create_template`: Create new template
- `get_template`: Get template details
- `update_template`: Update template
- `list_templates`: List templates
- `instantiate_template`: Create instance from template
- `share_template`: Share template with organization

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | postgres |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | PostgreSQL database name | template_db |
| `DB_USER` | PostgreSQL username | postgres |
| `DB_PASSWORD` | PostgreSQL password | postgres |
| `SERVER_PORT` | Server port | 5006 |
| `LOG_LEVEL` | Logging level | INFO |

### Template Configuration

Template-specific settings can be configured in `config/template.yml`:

```yaml
template:
  validation:
    max_template_size_kb: 100
    max_variables: 50
    allowed_variable_patterns:
      - "^[a-zA-Z0-9_]+$"
    disallowed_content:
      - "script"
      - "iframe"
      - "eval"
  
  versioning:
    max_versions_per_template: 20
    auto_version_on_update: true
    
  sharing:
    default_sharing_permission: "read"
    allowed_sharing_permissions:
      - "read"
      - "use"
      - "edit"
    
  categories:
    default_categories:
      - "Debate"
      - "Prompt"
      - "System"
      - "Custom"
```

## Usage Examples

### Create a Template

```bash
curl -X POST http://localhost:5006/api/v1/templates \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Climate Debate Template",
    "description": "Template for climate policy debates",
    "category": "Debate",
    "content": {
      "name": "{{debate_name}}",
      "topic": "Climate Policy: {{specific_topic}}",
      "format": "oxford",
      "participants": [
        {
          "name": "{{team_1_name}}",
          "role": "proposition",
          "llmConfig": {
            "provider": "{{provider_1|claude}}",
            "model": "{{model_1|claude-3-opus-20240229}}",
            "systemPrompt": "You are an expert advocating for {{position_1}}"
          }
        },
        {
          "name": "{{team_2_name}}",
          "role": "opposition",
          "llmConfig": {
            "provider": "{{provider_2|openai}}",
            "model": "{{model_2|gpt-4}}",
            "systemPrompt": "You are an expert advocating for {{position_2}}"
          }
        }
      ],
      "maxRounds": "{{max_rounds|6}}"
    },
    "variables": [
      {"name": "debate_name", "description": "Name of the debate", "required": true},
      {"name": "specific_topic", "description": "Specific climate policy topic", "required": true},
      {"name": "team_1_name", "description": "Name for team 1", "required": true},
      {"name": "team_2_name", "description": "Name for team 2", "required": true},
      {"name": "position_1", "description": "Position for team 1", "required": true},
      {"name": "position_2", "description": "Position for team 2", "required": true},
      {"name": "provider_1", "description": "LLM provider for team 1", "required": false, "default": "claude"},
      {"name": "model_1", "description": "LLM model for team 1", "required": false, "default": "claude-3-opus-20240229"},
      {"name": "provider_2", "description": "LLM provider for team 2", "required": false, "default": "openai"},
      {"name": "model_2", "description": "LLM model for team 2", "required": false, "default": "gpt-4"},
      {"name": "max_rounds", "description": "Maximum debate rounds", "required": false, "default": "6"}
    ],
    "tags": ["climate", "debate", "oxford"]
  }'
```

### Instantiate a Template

```bash
curl -X POST http://localhost:5006/api/v1/templates/template-123/instantiate \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "variables": {
      "debate_name": "Carbon Tax Debate 2025",
      "specific_topic": "Implementation of Carbon Taxation",
      "team_1_name": "Carbon Tax Advocates",
      "team_2_name": "Market Solution Advocates",
      "position_1": "strong carbon taxation",
      "position_2": "market-based climate solutions"
    }
  }'
```

### Share a Template

```bash
curl -X POST http://localhost:5006/api/v1/templates/template-123/share \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "organizationId": "org-456",
    "permission": "use",
    "expiresAt": "2026-07-16T00:00:00Z"
  }'
```

### List Templates by Category

```bash
curl -X GET "http://localhost:5006/api/v1/templates?category=Debate" \
  -H "X-Organization-ID: org-123"
```

## Data Models

### Template

```json
{
  "id": "template-123",
  "organizationId": "org-456",
  "name": "Climate Debate Template",
  "description": "Template for climate policy debates",
  "category": "Debate",
  "content": {
    "name": "{{debate_name}}",
    "topic": "Climate Policy: {{specific_topic}}",
    "format": "oxford",
    "participants": [
      {
        "name": "{{team_1_name}}",
        "role": "proposition",
        "llmConfig": {
          "provider": "{{provider_1|claude}}",
          "model": "{{model_1|claude-3-opus-20240229}}",
          "systemPrompt": "You are an expert advocating for {{position_1}}"
        }
      },
      {
        "name": "{{team_2_name}}",
        "role": "opposition",
        "llmConfig": {
          "provider": "{{provider_2|openai}}",
          "model": "{{model_2|gpt-4}}",
          "systemPrompt": "You are an expert advocating for {{position_2}}"
        }
      }
    ],
    "maxRounds": "{{max_rounds|6}}"
  },
  "variables": [
    {"name": "debate_name", "description": "Name of the debate", "required": true},
    {"name": "specific_topic", "description": "Specific climate policy topic", "required": true},
    {"name": "team_1_name", "description": "Name for team 1", "required": true},
    {"name": "team_2_name", "description": "Name for team 2", "required": true},
    {"name": "position_1", "description": "Position for team 1", "required": true},
    {"name": "position_2", "description": "Position for team 2", "required": true},
    {"name": "provider_1", "description": "LLM provider for team 1", "required": false, "default": "claude"},
    {"name": "model_1", "description": "LLM model for team 1", "required": false, "default": "claude-3-opus-20240229"},
    {"name": "provider_2", "description": "LLM provider for team 2", "required": false, "default": "openai"},
    {"name": "model_2", "description": "LLM model for team 2", "required": false, "default": "gpt-4"},
    {"name": "max_rounds", "description": "Maximum debate rounds", "required": false, "default": "6"}
  ],
  "version": 2,
  "tags": ["climate", "debate", "oxford"],
  "createdAt": "2025-06-15T10:30:00Z",
  "updatedAt": "2025-07-10T14:22:15Z",
  "createdBy": "user-789"
}
```

### Template Version

```json
{
  "id": "version-456",
  "templateId": "template-123",
  "versionNumber": 1,
  "content": {
    "name": "{{debate_name}}",
    "topic": "Climate Policy: {{specific_topic}}",
    "format": "standard",
    "participants": [
      {
        "name": "{{team_1_name}}",
        "llmConfig": {
          "provider": "{{provider_1|claude}}",
          "model": "{{model_1|claude-3-opus-20240229}}",
          "systemPrompt": "You are an expert advocating for {{position_1}}"
        }
      },
      {
        "name": "{{team_2_name}}",
        "llmConfig": {
          "provider": "{{provider_2|openai}}",
          "model": "{{model_2|gpt-4}}",
          "systemPrompt": "You are an expert advocating for {{position_2}}"
        }
      }
    ],
    "maxRounds": "{{max_rounds|5}}"
  },
  "variables": [
    {"name": "debate_name", "description": "Name of the debate", "required": true},
    {"name": "specific_topic", "description": "Specific climate policy topic", "required": true},
    {"name": "team_1_name", "description": "Name for team 1", "required": true},
    {"name": "team_2_name", "description": "Name for team 2", "required": true},
    {"name": "position_1", "description": "Position for team 1", "required": true},
    {"name": "position_2", "description": "Position for team 2", "required": true},
    {"name": "provider_1", "description": "LLM provider for team 1", "required": false, "default": "claude"},
    {"name": "model_1", "description": "LLM model for team 1", "required": false, "default": "claude-3-opus-20240229"},
    {"name": "provider_2", "description": "LLM provider for team 2", "required": false, "default": "openai"},
    {"name": "model_2", "description": "LLM model for team 2", "required": false, "default": "gpt-4"},
    {"name": "max_rounds", "description": "Maximum debate rounds", "required": false, "default": "5"}
  ],
  "createdAt": "2025-06-15T10:30:00Z",
  "createdBy": "user-789"
}
```

### Template Sharing

```json
{
  "id": "share-789",
  "templateId": "template-123",
  "ownerOrganizationId": "org-123",
  "sharedWithOrganizationId": "org-456",
  "permission": "use",
  "createdAt": "2025-07-15T09:45:00Z",
  "expiresAt": "2026-07-16T00:00:00Z",
  "createdBy": "user-789"
}
```

## Template Variable Syntax

The template service supports a flexible variable syntax:

### Basic Variables

```
{{variable_name}}
```

### Variables with Default Values

```
{{variable_name|default_value}}
```

### Conditional Content

```
{{#if variable_name}}
  Content to include if variable exists
{{/if}}
```

### Loops

```
{{#each items}}
  {{this.name}}
{{/each}}
```

## Template Categories

The service provides several built-in template categories:

- **Debate**: Templates for debate structures
- **Prompt**: Templates for LLM prompts
- **System**: System-level templates
- **Custom**: User-defined templates

Organizations can also create custom categories.

## Template Library

The service includes a library of pre-built templates:

- **Standard Debate**: Basic two-participant debate
- **Oxford Debate**: Formal debate with proposition and opposition
- **Panel Discussion**: Multi-participant panel format
- **Interview**: Question and answer format
- **Socratic Dialogue**: Philosophical dialogue format

## Monitoring and Metrics

The service exposes the following metrics:

- Template count by organization
- Template instantiation count
- Template sharing statistics
- Template size distribution
- Template category distribution

Access metrics at: `http://localhost:5006/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Variable Substitution Issues**
   - Check variable names match exactly
   - Verify all required variables are provided
   - Check for syntax errors in template

2. **Template Size Issues**
   - Verify template is within size limits
   - Consider breaking large templates into smaller ones
   - Check for unnecessary content

3. **Sharing Issues**
   - Verify organization IDs are correct
   - Check sharing permissions
   - Ensure template is not expired

### Logs

Service logs can be accessed via:

```bash
docker-compose logs mcp-template
```

## Development

### Building the Service

```bash
cd mcp-template
mvn clean install
```

### Running Tests

```bash
cd mcp-template
mvn test
```

### Local Development

```bash
cd mcp-template
mvn spring-boot:run
```

## Advanced Features

### Template Inheritance

Create templates that inherit from base templates:

```bash
curl -X POST http://localhost:5006/api/v1/templates \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Specialized Climate Debate",
    "baseTemplateId": "template-123",
    "overrides": {
      "format": "panel_discussion",
      "maxRounds": "{{max_rounds|8}}"
    },
    "additionalVariables": [
      {"name": "moderator_name", "description": "Name of the debate moderator", "required": true}
    ]
  }'
```

### Template Export/Import

Export templates for backup or sharing:

```bash
curl -X GET http://localhost:5006/api/v1/templates/template-123/export \
  -H "X-Organization-ID: org-123"
```

Import templates from JSON:

```bash
curl -X POST http://localhost:5006/api/v1/templates/import \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d @template-export.json
```

### Template Analytics

Get usage analytics for templates:

```bash
curl -X GET http://localhost:5006/api/v1/templates/template-123/analytics \
  -H "X-Organization-ID: org-123" \
  -d '{
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-07-01T00:00:00Z"
  }'
```
