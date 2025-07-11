# MCP Template Service - Claude Development Guide

## Quick Reference
- **Port**: 5006
- **Database**: PostgreSQL (`template_db`)
- **Primary Purpose**: Jinja2 template management for debates and prompts
- **Dependencies**: PostgreSQL, Organization Service

## Key Files to Check
```
mcp-template/
├── src/
│   ├── mcp_server.py          # Main server with Jinja2 - START HERE
│   ├── models.py              # Template models and schemas
│   └── db/
│       └── connection.py      # SQLAlchemy models
```

## Current Implementation Status
✅ **Implemented**:
- Template CRUD operations
- Jinja2 rendering (sandboxed)
- Template categories and subcategories
- Variable validation
- Default system templates
- Usage tracking
- Debate template generation

❌ **Not Implemented**:
- Template versioning UI
- Template marketplace
- Organization integration
- Template approval workflow
- Template analytics dashboard

## Template Categories
```python
class TemplateCategory(str, Enum):
    DEBATE = "debate"          # Debate-related templates
    PROMPT = "prompt"          # LLM prompts
    RESPONSE = "response"      # Response formatting
    EVALUATION = "evaluation"  # Evaluation criteria
    MODERATION = "moderation"  # Moderation rules
    SYSTEM = "system"          # System prompts
    CUSTOM = "custom"          # User-defined
```

## Common Development Tasks

### 1. Creating a New Template Type
```python
# 1. Add to DEFAULT_DEBATE_TEMPLATES in mcp_server.py
DEFAULT_DEBATE_TEMPLATES = {
    "your_new_template": """
    {% if condition %}
        {{ variable }}
    {% endif %}
    """
}

# 2. System templates auto-create on startup
# No migration needed!
```

### 2. Adding Template Variables
```python
# Define variables in template creation:
{
    "variables": [
        {
            "name": "participant_name",
            "type": "string",
            "description": "Name of the participant",
            "required": true,
            "default_value": "Debater"
        }
    ]
}
```

### 3. Rendering Templates Safely
```python
# Sandboxed environment for user templates:
safe_env = SandboxedEnvironment(autoescape=True)

# Unsafe environment for system templates only:
unsafe_env = Environment(autoescape=True)
```

## Jinja2 Template Examples

### Debate Opening Template
```jinja2
# {{ topic }}

Welcome to this debate between:
{% for participant in participants %}
- **{{ participant }}**: {{ participant_positions[participant] }}
{% endfor %}

## Rules
{% for rule in rules %}
{{ loop.index }}. {{ rule }}
{% endfor %}

Time limit per turn: {{ time_limit }} minutes
```

### Participant Prompt Template
```jinja2
You are {{ participant_name }} arguing for: {{ position }}

Remember to:
- Stay in character
- Support your position with evidence
- Address counterarguments
- Be respectful but persuasive

Context: {{ context }}
```

## Integration with Debate Service

### Creating Debate Templates
```python
# Debate service calls:
POST http://localhost:5006/tools/create_debate_templates
{
    "name": "create_debate_templates",
    "arguments": {
        "organization_id": "org-123",
        "topic": "AI Ethics",
        "participants": ["Optimist", "Skeptic"],
        "participant_positions": {
            "Optimist": "AI will benefit humanity",
            "Skeptic": "AI poses risks"
        }
    }
}

# Returns all needed templates for a debate
```

## Database Schema Key Points
```sql
-- Templates are scoped by organization
-- System templates have organization_id = 'system'
-- Templates track usage_count for popularity
-- Variables stored as JSON array
-- Soft delete not implemented (should be?)
```

## Testing Templates

### Test Rendering
```python
# Quick test for template syntax:
from jinja2 import Template
template = Template("Hello {{ name }}!")
result = template.render(name="World")
```

### Common Jinja2 Patterns
```jinja2
{# Conditionals #}
{% if user.is_premium %}
    Premium content
{% endif %}

{# Loops #}
{% for item in items %}
    {{ loop.index }}: {{ item }}
{% endfor %}

{# Filters #}
{{ text|upper }}
{{ date|format_date }}

{# Macros #}
{% macro render_participant(name, role) %}
    <div>{{ name }} - {{ role }}</div>
{% endmacro %}
```

## Environment Variables
```bash
POSTGRES_HOST=postgres
POSTGRES_DB=template_db
POSTGRES_USER=context_user
POSTGRES_PASSWORD=context_pass
MCP_PORT=5006
LOG_LEVEL=INFO
```

## Common Issues & Solutions

### Issue: "Template syntax error"
```python
# Jinja2 provides good error messages
# Check: Missing closing tags {% endif %}, {% endfor %}
# Check: Undefined variables - add defaults
```

### Issue: "Variable validation failed"
```python
# Required variables must be provided
# Check template.variables for requirements
# Solution: Provide defaults or make optional
```

## Security Considerations
1. **Sandboxed Environment**: User templates can't execute arbitrary code
2. **Autoescape**: Prevents XSS in rendered content
3. **System Templates**: Only admins should modify
4. **Variable Validation**: Prevents injection attacks

## Quick Commands
```bash
# Test template rendering
curl -X POST http://localhost:5006/tools/render_template \
  -d '{"template_id": "system-debate_prompt", "variables": {"topic": "Test"}}'

# List all templates
curl http://localhost:5006/resources/template://templates
```

## UI Integration Points

### Template Selection UI
```typescript
// Fetch available templates
const templates = await fetch('/api/template/search', {
    method: 'POST',
    body: JSON.stringify({
        organization_id: currentOrg.id,
        category: 'debate'
    })
});

// Render template preview
const preview = await fetch('/api/template/render', {
    method: 'POST',
    body: JSON.stringify({
        template_id: selectedTemplate.id,
        variables: formData
    })
});
```

## Next Development Priorities
1. Create UI for template management
2. Add template versioning with diffs
3. Implement template inheritance
4. Add template testing interface
5. Create template import/export
6. Add real-time preview in UI