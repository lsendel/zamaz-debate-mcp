# MCP Template Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-template service.

## Service Overview

The `mcp-template` service provides centralized template management for the zamaz-debate-mcp platform. It handles creation, storage, rendering, and versioning of templates used across the system, with special focus on debate prompts, responses, and evaluation templates.

## Purpose

- **Template Management**: Create, store, and version control templates
- **Dynamic Rendering**: Jinja2-based template rendering with variable substitution
- **Multi-tenant Support**: Organization-scoped template isolation
- **Debate Integration**: Specialized templates for debate orchestration
- **Usage Analytics**: Track template usage and effectiveness

## Technology Stack

- **Language**: Python 3.11+
- **Framework**: MCP SDK with async support
- **Database**: PostgreSQL with SQLAlchemy async
- **Template Engine**: Jinja2 with sandboxing
- **Validation**: Pydantic v2 models
- **Container**: Docker with health checks

## Template Categories

### System Categories
1. **debate**: Templates for debate prompts and rules
2. **prompt**: LLM prompt templates
3. **response**: Response formatting templates
4. **evaluation**: Scoring and evaluation templates
5. **moderation**: Content moderation templates
6. **system**: Internal system templates
7. **custom**: User-defined templates

### Template Types
- **jinja2**: Dynamic templates with variable substitution
- **markdown**: Formatted text templates
- **plain_text**: Simple text templates
- **json**: Structured data templates

## Architecture Components

### 1. MCP Server (`mcp_server.py`)
Implements MCP protocol with:
- **Tools**: 8 template management tools
- **Resources**: Template catalog and statistics
- **Rendering Engine**: Sandboxed Jinja2 environment
- **Validation**: Variable type checking

### 2. Data Models (`models.py`)
```python
# Core template model
Template:
  - name: str
  - category: TemplateCategory
  - type: TemplateType
  - content: str
  - variables: List[TemplateVariable]
  - status: TemplateStatus
  - version: int
  - parent_id: Optional[str]
  - metadata: Dict[str, Any]

# Variable definition
TemplateVariable:
  - name: str
  - type: str  # string, number, boolean, list, object
  - required: bool
  - default: Optional[Any]
  - description: str
  - validation: Optional[Dict]
```

### 3. Database Schema (`db/connection.py`)
PostgreSQL tables:
- **templates**: Main template storage
- **template_usage**: Usage tracking
- **template_categories**: Category management

## MCP Tools

### 1. create_template
```json
{
  "name": "debate_opening_prompt",
  "category": "debate",
  "type": "jinja2",
  "content": "As {{ participant.name }}, argue for: {{ topic }}",
  "variables": [
    {
      "name": "participant",
      "type": "object",
      "required": true
    },
    {
      "name": "topic",
      "type": "string",
      "required": true
    }
  ],
  "organization_id": "org-123"
}
```

### 2. render_template
```json
{
  "template_id": "template-123",
  "variables": {
    "participant": {
      "name": "AI Assistant",
      "role": "debater"
    },
    "topic": "AI Safety"
  }
}
```

### 3. search_templates
```json
{
  "organization_id": "org-123",
  "category": "debate",
  "tags": ["opening", "formal"],
  "status": "active"
}
```

### 4. create_debate_templates
```json
{
  "organization_id": "org-123",
  "debate_format": "oxford",
  "customize": {
    "tone": "formal",
    "length": "medium"
  }
}
```

## Template Examples

### Debate Opening Template
```jinja2
{# debate_opening.j2 #}
{% set participant_info = participant.name %}
{% if participant.credentials %}
  {% set participant_info = participant_info + ", " + participant.credentials %}
{% endif %}

As {{ participant_info }}, I will argue {{ position }} the motion: "{{ motion }}"

{% if key_points %}
My main arguments will focus on:
{% for point in key_points %}
- {{ point }}
{% endfor %}
{% endif %}

{% if time_limit %}
[Time limit: {{ time_limit }} minutes]
{% endif %}
```

### Evaluation Template
```jinja2
{# debate_evaluation.j2 #}
## Debate Evaluation: {{ debate.title }}

### Participants Performance
{% for participant in participants %}
**{{ participant.name }}** ({{ participant.role }}):
- Clarity: {{ participant.scores.clarity }}/10
- Logic: {{ participant.scores.logic }}/10
- Evidence: {{ participant.scores.evidence }}/10
- Persuasion: {{ participant.scores.persuasion }}/10
- Overall: {{ participant.scores.overall }}/10

Key Strengths:
{{ participant.strengths }}

Areas for Improvement:
{{ participant.improvements }}
{% endfor %}

### Debate Summary
{{ summary }}

### Winner: {{ winner.name }}
**Reasoning**: {{ winner_reasoning }}
```

### Prompt Template
```jinja2
{# llm_debate_turn.j2 #}
You are {{ participant.name }}, a {{ participant.role }} in a {{ debate.format }} debate.

Context:
{{ context }}

Previous speaker: {{ previous_turn.participant }} said:
"{{ previous_turn.content }}"

Your task: Provide a {{ turn_type }} that:
{% for requirement in requirements %}
- {{ requirement }}
{% endfor %}

{% if word_limit %}
Keep your response under {{ word_limit }} words.
{% endif %}

{% if tone %}
Tone: {{ tone }}
{% endif %}
```

## Rendering Engine

### Jinja2 Configuration
```python
# Sandboxed environment for user templates
env = SandboxedEnvironment(
    autoescape=True,
    trim_blocks=True,
    lstrip_blocks=True,
    undefined=StrictUndefined
)

# Custom filters
env.filters['format_date'] = format_date
env.filters['markdown'] = markdown_to_html
env.filters['truncate_words'] = truncate_words
```

### Security Features
- Sandboxed execution prevents code injection
- Automatic HTML escaping
- Restricted access to Python objects
- No file system or network access
- Template size limits

## Configuration

### Environment Variables
```bash
# Service Configuration
MCP_HOST=0.0.0.0
MCP_PORT=5006

# Database
DATABASE_URL=postgresql+asyncpg://user:pass@localhost/templates

# Template Engine
MAX_TEMPLATE_SIZE_KB=100
MAX_RENDER_TIME_MS=1000
TEMPLATE_CACHE_SIZE=1000

# Security
ENABLE_CUSTOM_FILTERS=false
ALLOW_SYSTEM_TEMPLATES=true
```

### Running the Service
```bash
# Development
python -m src.mcp_server

# Docker
docker build -t mcp-template .
docker run -p 5006:5006 mcp-template

# With PostgreSQL
docker-compose up postgres mcp-template
```

## Integration Patterns

### With Debate Service
```python
# Get debate templates
templates = await template_client.get_debate_templates(
    organization_id=org_id,
    debate_format="oxford"
)

# Render opening prompt
prompt = await template_client.render_template(
    template_id=templates.opening_id,
    variables={
        "participant": participant,
        "motion": debate.motion,
        "position": "for"
    }
)
```

### With LLM Service
```python
# Get and render LLM prompt template
template = await template_client.get_template(
    name="llm_debate_turn",
    category="prompt"
)

rendered = await template_client.render_template(
    template_id=template.id,
    variables={
        "participant": current_participant,
        "context": context_window,
        "turn_type": "rebuttal"
    }
)
```

## Template Versioning

### Version Management
- Each edit creates a new version
- Parent tracking for version history
- Rollback to previous versions
- Diff view between versions
- Active version selection

### Version Control Flow
```python
1. Create template v1
2. Edit creates v2 with parent=v1
3. Set v2 as active
4. Can rollback to v1
5. Fork from v1 creates parallel version
```

## Development Guidelines

### Creating New Template Categories
1. Add category to enum in `models.py`
2. Create default templates for category
3. Add validation rules
4. Update documentation
5. Add category-specific filters

### Adding Custom Filters
```python
# In template_filters.py
def custom_filter(value, arg):
    """Custom filter documentation"""
    return processed_value

# Register in Jinja2 environment
env.filters['custom_filter'] = custom_filter
```

### Testing Templates
```bash
# Unit tests
pytest tests/test_template_rendering.py

# Integration tests
pytest tests/integration/test_debate_templates.py

# Performance tests
python tests/performance/test_render_speed.py
```

## Best Practices

### Template Design
1. **Use meaningful variable names**
2. **Provide defaults for optional variables**
3. **Include comments for complex logic**
4. **Keep templates focused and modular**
5. **Version significant changes**

### Variable Naming
```jinja2
{# Good #}
{{ participant.name }}
{{ debate.rules.time_limit }}

{# Avoid #}
{{ p }}
{{ d.r.tl }}
```

### Error Handling
```jinja2
{# Check for required variables #}
{% if not participant %}
  ERROR: Participant information required
{% else %}
  {{ participant.name }}
{% endif %}

{# Provide fallbacks #}
{{ participant.bio|default("No bio provided") }}
```

## Performance Optimization

1. **Template Caching**: Compiled templates cached in memory
2. **Variable Pre-processing**: Validate before rendering
3. **Async Rendering**: Non-blocking for large templates
4. **Batch Operations**: Render multiple templates together
5. **Database Indexes**: Optimized for common queries

## Security Considerations

- Organization-scoped template access
- Sandboxed template execution
- Input validation for all variables
- XSS prevention with auto-escaping
- Template size and complexity limits
- Audit logging for template changes

## Monitoring and Metrics

### Key Metrics
- Templates created per organization
- Render requests per minute
- Average render time
- Cache hit rate
- Error rate by template
- Most used templates

### Health Checks
- Database connectivity
- Template cache status
- Render engine availability
- Memory usage
- Queue depths

## Known Limitations

1. **Template Size**: 100KB maximum
2. **Render Timeout**: 1 second maximum
3. **Variable Depth**: 5 levels of nesting
4. **Custom Filters**: Disabled by default
5. **Async Rendering**: Not for streaming

## Future Enhancements

- Template marketplace for sharing
- AI-powered template generation
- Visual template editor
- A/B testing for templates
- Template analytics dashboard
- Multi-language template support
- Template composition/inheritance
- Real-time collaborative editing
- Template performance profiling
- Export/import template sets