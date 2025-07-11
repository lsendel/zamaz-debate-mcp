#!/usr/bin/env python3
"""
MCP Template Service - Manages templates with Jinja2 rendering
"""

import asyncio
import json
import os
import logging
import time
from typing import Dict, Any, List, Optional
from datetime import datetime
import uuid

from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Resource, Tool, TextContent
from jinja2 import Environment, Template as Jinja2Template, TemplateSyntaxError, UndefinedError, TemplateError
from jinja2.sandbox import SandboxedEnvironment

from models import (
    Template, TemplateCategory, TemplateStatus, TemplateType,
    TemplateVariable, TemplateCreateRequest, TemplateUpdateRequest,
    TemplateRenderRequest, TemplateRenderResponse, TemplateSearchRequest,
    TemplateSearchResponse, TemplateUsageStats, DebateTemplateRequest,
    DebateTemplateResponse, TemplateErrorResponse
)
from db.connection import db_manager, get_db_session, init_database, TemplateTable, TemplateUsageTable, TemplateCategoryTable
from sqlalchemy import select, update, delete, and_, or_, func
from sqlalchemy.orm import selectinload

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# MCP Server instance
server = Server("template-service")

# Jinja2 environments
unsafe_env = Environment(autoescape=True)
safe_env = SandboxedEnvironment(autoescape=True)

# Default debate templates
DEFAULT_DEBATE_TEMPLATES = {
    "debate_prompt": """
# {{ topic }}

## Context
{{ context }}

## Participants
{% for participant in participants %}
- **{{ participant }}**: {{ participant_positions[participant] }}
{% endfor %}

## Rules
{% for rule in rules %}
- {{ rule }}
{% endfor %}

## Format
- Each participant has {{ time_limit }} minutes per turn
- Total rounds: {{ rounds }}
- Responses should be constructive and evidence-based
""",
    "participant_prompt": """
You are {{ participant_name }} in a debate about "{{ topic }}".

Your position: {{ position }}

Guidelines:
- Stay in character as {{ participant_name }}
- Argue from your assigned position
- Be respectful but persuasive
- Use evidence and logical reasoning
- Respond directly to other participants' arguments

Context: {{ context }}
""",
    "moderator_prompt": """
You are moderating a debate on "{{ topic }}".

Your responsibilities:
- Ensure all participants follow the rules
- Keep discussions on topic
- Manage time limits ({{ time_limit }} minutes per turn)
- Summarize key points after each round
- Maintain a neutral stance

Participants and their positions:
{% for participant, position in participant_positions.items() %}
- {{ participant }}: {{ position }}
{% endfor %}
""",
    "evaluation_prompt": """
Evaluate the debate on "{{ topic }}" based on:

1. **Argument Quality**: Logical consistency, evidence usage
2. **Persuasiveness**: How convincing were the arguments
3. **Engagement**: Direct responses to opposing views
4. **Conduct**: Respectfulness and professionalism
5. **Innovation**: New perspectives or insights

Participants evaluated:
{% for participant in participants %}
- {{ participant }}
{% endfor %}
"""
}

class TemplateManager:
    """Manages template operations"""
    
    def __init__(self):
        self.db_initialized = False
        
    async def initialize(self):
        """Initialize database connection and tables"""
        if not self.db_initialized:
            await init_database()
            await self._create_default_templates()
            self.db_initialized = True
            
    async def _create_default_templates(self):
        """Create default system templates"""
        try:
            async with db_manager.get_session() as session:
                # Check if default templates exist
                result = await session.execute(
                    select(func.count(TemplateTable.id)).where(
                        and_(
                            TemplateTable.organization_id == 'system',
                            TemplateTable.category == TemplateCategory.DEBATE.value
                        )
                    )
                )
                count = result.scalar()
                
                if count == 0:
                    # Create default debate templates
                    for name, content in DEFAULT_DEBATE_TEMPLATES.items():
                        template_db = TemplateTable(
                            id=f"system-{name}",
                            organization_id="system",
                            name=f"Default {name.replace('_', ' ').title()}",
                            description=f"System default template for {name.replace('_', ' ')}",
                            category=TemplateCategory.DEBATE.value,
                            subcategory=name,
                            template_type=TemplateType.JINJA2.value,
                            content=content,
                            status=TemplateStatus.ACTIVE.value,
                            created_at=datetime.utcnow(),
                            updated_at=datetime.utcnow()
                        )
                        session.add(template_db)
                    
                    await session.commit()
                    logger.info("Default templates created")
                    
        except Exception as e:
            logger.error(f"Error creating default templates: {e}")
            
    async def create_template(self, template: Template) -> Template:
        """Create a new template"""
        try:
            if not template.id:
                template.id = str(uuid.uuid4())
                
            template.created_at = datetime.utcnow()
            template.updated_at = datetime.utcnow()
            
            async with db_manager.get_session() as session:
                template_db = TemplateTable(
                    id=template.id,
                    organization_id=template.organization_id,
                    name=template.name,
                    description=template.description,
                    category=template.category.value,
                    subcategory=template.subcategory,
                    template_type=template.template_type.value,
                    content=template.content,
                    variables=[v.dict() for v in template.variables],
                    tags=template.tags,
                    status=template.status.value,
                    version=template.version,
                    parent_template_id=template.parent_template_id,
                    created_at=template.created_at,
                    updated_at=template.updated_at,
                    created_by=template.created_by,
                    updated_by=template.updated_by,
                    usage_count=template.usage_count,
                    metadata=template.metadata
                )
                session.add(template_db)
                await session.commit()
                
            return template
            
        except Exception as e:
            logger.error(f"Error creating template: {e}")
            raise
            
    async def get_template(self, template_id: str, organization_id: str) -> Optional[Template]:
        """Get a template by ID"""
        try:
            async with db_manager.get_session() as session:
                result = await session.execute(
                    select(TemplateTable).where(
                        and_(
                            TemplateTable.id == template_id,
                            or_(
                                TemplateTable.organization_id == organization_id,
                                TemplateTable.organization_id == 'system'
                            )
                        )
                    )
                )
                template_db = result.scalar_one_or_none()
                
                if template_db:
                    return self._db_to_template(template_db)
                return None
            
        except Exception as e:
            logger.error(f"Error getting template: {e}")
            raise
            
    async def update_template(self, template_id: str, organization_id: str, 
                            update_req: TemplateUpdateRequest) -> Optional[Template]:
        """Update a template"""
        try:
            async with db_manager.get_session() as session:
                # Get template
                result = await session.execute(
                    select(TemplateTable).where(
                        and_(
                            TemplateTable.id == template_id,
                            TemplateTable.organization_id == organization_id
                        )
                    )
                )
                template_db = result.scalar_one_or_none()
                
                if not template_db:
                    return None
                
                # Update fields
                update_dict = update_req.dict(exclude_unset=True)
                for field, value in update_dict.items():
                    if hasattr(template_db, field) and value is not None:
                        if field == 'category' and isinstance(value, TemplateCategory):
                            value = value.value
                        elif field == 'status' and isinstance(value, TemplateStatus):
                            value = value.value
                        elif field == 'variables':
                            value = [v.dict() if hasattr(v, 'dict') else v for v in value]
                        setattr(template_db, field, value)
                
                template_db.updated_at = datetime.utcnow()
                template_db.version += 1
                
                await session.commit()
                await session.refresh(template_db)
                
                return self._db_to_template(template_db)
            
        except Exception as e:
            logger.error(f"Error updating template: {e}")
            raise
            
    async def delete_template(self, template_id: str, organization_id: str) -> bool:
        """Delete a template"""
        try:
            async with db_manager.get_session() as session:
                result = await session.execute(
                    delete(TemplateTable).where(
                        and_(
                            TemplateTable.id == template_id,
                            TemplateTable.organization_id == organization_id
                        )
                    )
                )
                await session.commit()
                return result.rowcount > 0
            
        except Exception as e:
            logger.error(f"Error deleting template: {e}")
            raise
            
    async def search_templates(self, request: TemplateSearchRequest) -> TemplateSearchResponse:
        """Search templates"""
        try:
            async with db_manager.get_session() as session:
                # Build query
                query = select(TemplateTable).where(
                    or_(
                        TemplateTable.organization_id == request.organization_id,
                        TemplateTable.organization_id == 'system'
                    )
                )
                
                if request.category:
                    query = query.where(TemplateTable.category == request.category.value)
                    
                if request.subcategory:
                    query = query.where(TemplateTable.subcategory == request.subcategory)
                    
                if request.status:
                    query = query.where(TemplateTable.status == request.status.value)
                    
                if request.query:
                    search_term = f"%{request.query}%"
                    query = query.where(
                        or_(
                            TemplateTable.name.ilike(search_term),
                            TemplateTable.description.ilike(search_term),
                            TemplateTable.content.ilike(search_term)
                        )
                    )
                
                # Count total
                count_query = select(func.count()).select_from(query.subquery())
                total_result = await session.execute(count_query)
                total_count = total_result.scalar()
                
                # Get paginated results
                query = query.order_by(TemplateTable.updated_at.desc())
                query = query.limit(request.limit).offset(request.offset)
                
                result = await session.execute(query)
                template_dbs = result.scalars().all()
                
                templates = [self._db_to_template(t) for t in template_dbs]
                
                return TemplateSearchResponse(
                    templates=templates,
                    total_count=total_count,
                    has_more=(request.offset + len(templates)) < total_count
                )
            
        except Exception as e:
            logger.error(f"Error searching templates: {e}")
            raise
            
    async def render_template(self, request: TemplateRenderRequest) -> TemplateRenderResponse:
        """Render a template with variables"""
        try:
            start_time = time.time()
            warnings = []
            
            template = await self.get_template(request.template_id, request.organization_id)
            if not template:
                raise ValueError(f"Template {request.template_id} not found")
                
            # Validate required variables
            for var in template.variables:
                if var.required and var.name not in request.variables:
                    if var.default_value is not None:
                        request.variables[var.name] = var.default_value
                        warnings.append(f"Using default value for required variable: {var.name}")
                    else:
                        raise ValueError(f"Required variable missing: {var.name}")
                        
            # Choose environment based on template type
            env = safe_env if template.organization_id != "system" else unsafe_env
            
            # Render template
            jinja_template = env.from_string(template.content)
            rendered_content = jinja_template.render(**request.variables)
            
            # Update usage count and record usage
            async with db_manager.get_session() as session:
                # Update template usage count
                await session.execute(
                    update(TemplateTable)
                    .where(TemplateTable.id == template.id)
                    .values(usage_count=TemplateTable.usage_count + 1)
                )
                
                # Record usage
                usage = TemplateUsageTable(
                    id=str(uuid.uuid4()),
                    template_id=template.id,
                    organization_id=request.organization_id,
                    used_at=datetime.utcnow(),
                    render_time_ms=int(render_time_ms),
                    variables_used=request.variables,
                    success=True
                )
                session.add(usage)
                await session.commit()
            
            render_time_ms = (time.time() - start_time) * 1000
            
            return TemplateRenderResponse(
                rendered_content=rendered_content,
                template_id=template.id,
                variables_used=request.variables,
                render_time_ms=render_time_ms,
                warnings=warnings
            )
            
        except (TemplateSyntaxError, UndefinedError, TemplateError) as e:
            logger.error(f"Template rendering error: {e}")
            raise ValueError(f"Template rendering failed: {str(e)}")
        except Exception as e:
            logger.error(f"Error rendering template: {e}")
            raise
            
    async def create_debate_templates(self, request: DebateTemplateRequest) -> DebateTemplateResponse:
        """Create debate-specific templates"""
        try:
            # Prepare variables
            variables = {
                "topic": request.topic,
                "participants": request.participants,
                "participant_positions": request.participant_positions,
                "rules": request.rules or ["Be respectful", "Stay on topic", "Use evidence"],
                "time_limit": request.time_limit or 5,
                "rounds": request.rounds or 3,
                "context": request.context or ""
            }
            
            # Render debate prompt
            debate_prompt_request = TemplateRenderRequest(
                template_id="system-debate_prompt",
                organization_id=request.organization_id,
                variables=variables
            )
            debate_prompt_response = await self.render_template(debate_prompt_request)
            
            # Render participant prompts
            participant_prompts = {}
            for participant in request.participants:
                participant_vars = {
                    **variables,
                    "participant_name": participant,
                    "position": request.participant_positions.get(participant, "")
                }
                
                participant_request = TemplateRenderRequest(
                    template_id="system-participant_prompt",
                    organization_id=request.organization_id,
                    variables=participant_vars
                )
                participant_response = await self.render_template(participant_request)
                participant_prompts[participant] = participant_response.rendered_content
                
            # Render moderator prompt
            moderator_request = TemplateRenderRequest(
                template_id="system-moderator_prompt",
                organization_id=request.organization_id,
                variables=variables
            )
            moderator_response = await self.render_template(moderator_request)
            
            # Generate evaluation criteria
            evaluation_criteria = [
                "Logical consistency and coherence of arguments",
                "Use of evidence and factual support",
                "Direct engagement with opposing viewpoints",
                "Clarity and persuasiveness of communication",
                "Adherence to debate rules and time limits"
            ]
            
            return DebateTemplateResponse(
                debate_prompt=debate_prompt_response.rendered_content,
                participant_prompts=participant_prompts,
                moderator_prompt=moderator_response.rendered_content,
                evaluation_criteria=evaluation_criteria,
                template_ids_used=[
                    "system-debate_prompt",
                    "system-participant_prompt",
                    "system-moderator_prompt"
                ]
            )
            
        except Exception as e:
            logger.error(f"Error creating debate templates: {e}")
            raise
            
    def _db_to_template(self, db_obj: TemplateTable) -> Template:
        """Convert database object to Template model"""
        return Template(
            id=db_obj.id,
            organization_id=db_obj.organization_id,
            name=db_obj.name,
            description=db_obj.description,
            category=TemplateCategory(db_obj.category),
            subcategory=db_obj.subcategory,
            template_type=TemplateType(db_obj.template_type),
            content=db_obj.content,
            variables=[TemplateVariable(**v) for v in (db_obj.variables or [])],
            tags=db_obj.tags or [],
            status=TemplateStatus(db_obj.status),
            version=db_obj.version,
            parent_template_id=db_obj.parent_template_id,
            created_at=db_obj.created_at,
            updated_at=db_obj.updated_at,
            created_by=db_obj.created_by,
            updated_by=db_obj.updated_by,
            usage_count=db_obj.usage_count,
            metadata=db_obj.metadata or {}
        )

# Global template manager
template_manager = TemplateManager()

@server.list_resources()
async def list_resources() -> List[Resource]:
    """List available template resources"""
    await template_manager.initialize()
    
    return [
        Resource(
            uri="template://templates",
            name="Template Library",
            description="Access and manage templates",
            mime_type="application/json"
        ),
        Resource(
            uri="template://categories",
            name="Template Categories",
            description="Available template categories",
            mime_type="application/json"
        )
    ]

@server.read_resource()
async def read_resource(uri: str) -> str:
    """Read template resources"""
    await template_manager.initialize()
    
    if uri == "template://categories":
        categories = {
            "categories": [
                {
                    "name": cat.value,
                    "description": f"Templates for {cat.value}"
                }
                for cat in TemplateCategory
            ]
        }
        return json.dumps(categories, indent=2)
    
    elif uri == "template://templates":
        # Return system templates
        search_request = TemplateSearchRequest(
            organization_id="system",
            limit=100
        )
        response = await template_manager.search_templates(search_request)
        
        return json.dumps({
            "templates": [t.dict() for t in response.templates],
            "total": response.total_count
        }, indent=2, default=str)
    
    else:
        return json.dumps({"error": "Unknown resource"})

@server.list_tools()
async def list_tools() -> List[Tool]:
    """List available template tools"""
    return [
        Tool(
            name="create_template",
            description="Create a new template",
            inputSchema={
                "type": "object",
                "properties": {
                    "organization_id": {"type": "string"},
                    "name": {"type": "string"},
                    "description": {"type": "string"},
                    "category": {"type": "string", "enum": [c.value for c in TemplateCategory]},
                    "subcategory": {"type": "string"},
                    "content": {"type": "string"},
                    "variables": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "type": {"type": "string"},
                                "description": {"type": "string"},
                                "required": {"type": "boolean"},
                                "default_value": {}
                            }
                        }
                    },
                    "tags": {"type": "array", "items": {"type": "string"}}
                },
                "required": ["organization_id", "name", "category", "content"]
            }
        ),
        Tool(
            name="render_template",
            description="Render a template with variables",
            inputSchema={
                "type": "object",
                "properties": {
                    "template_id": {"type": "string"},
                    "organization_id": {"type": "string"},
                    "variables": {"type": "object"}
                },
                "required": ["template_id", "organization_id"]
            }
        ),
        Tool(
            name="search_templates",
            description="Search for templates",
            inputSchema={
                "type": "object",
                "properties": {
                    "organization_id": {"type": "string"},
                    "query": {"type": "string"},
                    "category": {"type": "string"},
                    "subcategory": {"type": "string"},
                    "tags": {"type": "array", "items": {"type": "string"}},
                    "limit": {"type": "integer", "default": 50},
                    "offset": {"type": "integer", "default": 0}
                },
                "required": ["organization_id"]
            }
        ),
        Tool(
            name="create_debate_templates",
            description="Create templates for a debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "organization_id": {"type": "string"},
                    "topic": {"type": "string"},
                    "participants": {"type": "array", "items": {"type": "string"}},
                    "participant_positions": {"type": "object"},
                    "rules": {"type": "array", "items": {"type": "string"}},
                    "time_limit": {"type": "integer"},
                    "rounds": {"type": "integer"},
                    "context": {"type": "string"}
                },
                "required": ["organization_id", "topic", "participants", "participant_positions"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: Dict[str, Any]) -> List[TextContent]:
    """Execute template tools"""
    await template_manager.initialize()
    
    try:
        if name == "create_template":
            request = TemplateCreateRequest(**arguments)
            template = Template(
                organization_id=request.organization_id,
                name=request.name,
                description=request.description,
                category=request.category,
                subcategory=request.subcategory,
                template_type=request.template_type,
                content=request.content,
                variables=request.variables,
                tags=request.tags,
                metadata=request.metadata
            )
            
            created = await template_manager.create_template(template)
            return [TextContent(
                type="text",
                text=json.dumps(created.dict(), indent=2, default=str)
            )]
            
        elif name == "render_template":
            request = TemplateRenderRequest(**arguments)
            response = await template_manager.render_template(request)
            return [TextContent(
                type="text",
                text=json.dumps(response.dict(), indent=2, default=str)
            )]
            
        elif name == "search_templates":
            request = TemplateSearchRequest(**arguments)
            response = await template_manager.search_templates(request)
            return [TextContent(
                type="text",
                text=json.dumps({
                    "templates": [t.dict() for t in response.templates],
                    "total_count": response.total_count,
                    "has_more": response.has_more
                }, indent=2, default=str)
            )]
            
        elif name == "create_debate_templates":
            request = DebateTemplateRequest(**arguments)
            response = await template_manager.create_debate_templates(request)
            return [TextContent(
                type="text",
                text=json.dumps(response.dict(), indent=2)
            )]
            
        else:
            return [TextContent(
                type="text",
                text=json.dumps({"error": f"Unknown tool: {name}"})
            )]
            
    except Exception as e:
        logger.error(f"Tool execution error: {e}")
        return [TextContent(
            type="text",
            text=json.dumps({
                "error": str(e),
                "error_type": type(e).__name__
            })
        )]

async def main():
    """Main entry point"""
    logger.info("Starting Template MCP Service...")
    
    # Initialize template manager
    await template_manager.initialize()
    
    # Run the server
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream)

if __name__ == "__main__":
    asyncio.run(main())