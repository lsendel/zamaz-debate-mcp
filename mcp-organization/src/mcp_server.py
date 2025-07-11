"""
Organization Management MCP Server
"""

import asyncio
import os
from typing import Any, Dict, List, Optional
from mcp.server import Server
from mcp.types import (
    Resource, Tool, TextContent, ImageContent, EmbeddedResource,
    CallToolResult, ListResourcesResult, ListToolsResult, ReadResourceResult
)
import structlog
from datetime import datetime

from .db.connection import init_database, close_database, get_db_session
from .managers.organization_manager import OrganizationManager, ProjectManager
from .models import (
    CreateOrganizationRequest, UpdateOrganizationRequest,
    CreateProjectRequest, UpdateProjectRequest,
    SearchRequest, ProjectStatus, ProjectType
)

# Configure logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    wrapper_class=structlog.stdlib.BoundLogger,
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()

# Create MCP server
app = Server("mcp-organization")


@app.list_resources()
async def list_resources() -> ListResourcesResult:
    """List available resources"""
    resources = [
        Resource(
            uri="organization://organizations",
            name="Organizations",
            description="List all organizations",
            mimeType="application/json"
        ),
        Resource(
            uri="organization://projects",
            name="Projects",
            description="List all projects",
            mimeType="application/json"
        ),
        Resource(
            uri="organization://stats",
            name="Organization Statistics",
            description="Get organization statistics and analytics",
            mimeType="application/json"
        )
    ]
    
    return ListResourcesResult(resources=resources)


@app.read_resource()
async def read_resource(uri: str) -> ReadResourceResult:
    """Read a specific resource"""
    logger.info("Reading resource", uri=uri)
    
    try:
        async with get_db_session() as session:
            if uri == "organization://organizations":
                org_manager = OrganizationManager(session)
                organizations = await org_manager.list_organizations(limit=100)
                
                return ReadResourceResult(
                    contents=[
                        TextContent(
                            type="text",
                            text=str({
                                "organizations": [org.dict() for org in organizations],
                                "total": len(organizations),
                                "timestamp": datetime.utcnow().isoformat()
                            })
                        )
                    ]
                )
            
            elif uri == "organization://projects":
                project_manager = ProjectManager(session)
                projects = await project_manager.list_projects(limit=100)
                
                return ReadResourceResult(
                    contents=[
                        TextContent(
                            type="text",
                            text=str({
                                "projects": [project.dict() for project in projects],
                                "total": len(projects),
                                "timestamp": datetime.utcnow().isoformat()
                            })
                        )
                    ]
                )
            
            elif uri == "organization://stats":
                org_manager = OrganizationManager(session)
                organizations = await org_manager.list_organizations()
                
                stats = []
                for org in organizations:
                    org_stats = await org_manager.get_organization_stats(org.id)
                    if org_stats:
                        stats.append({
                            "organization": org.dict(),
                            "stats": org_stats.dict()
                        })
                
                return ReadResourceResult(
                    contents=[
                        TextContent(
                            type="text",
                            text=str({
                                "organization_stats": stats,
                                "timestamp": datetime.utcnow().isoformat()
                            })
                        )
                    ]
                )
            
            elif uri.startswith("organization://organization/"):
                org_id = uri.split("/")[-1]
                org_manager = OrganizationManager(session)
                organization = await org_manager.get_organization(org_id)
                
                if not organization:
                    raise ValueError(f"Organization {org_id} not found")
                
                return ReadResourceResult(
                    contents=[
                        TextContent(
                            type="text",
                            text=str(organization.dict())
                        )
                    ]
                )
            
            elif uri.startswith("organization://project/"):
                project_id = uri.split("/")[-1]
                project_manager = ProjectManager(session)
                project = await project_manager.get_project(project_id)
                
                if not project:
                    raise ValueError(f"Project {project_id} not found")
                
                return ReadResourceResult(
                    contents=[
                        TextContent(
                            type="text",
                            text=str(project.dict())
                        )
                    ]
                )
            
            else:
                raise ValueError(f"Unknown resource URI: {uri}")
                
    except Exception as e:
        logger.error("Failed to read resource", uri=uri, error=str(e))
        return ReadResourceResult(
            contents=[
                TextContent(
                    type="text",
                    text=f"Error reading resource: {str(e)}"
                )
            ]
        )


@app.list_tools()
async def list_tools() -> ListToolsResult:
    """List available tools"""
    tools = [
        Tool(
            name="create_organization",
            description="Create a new organization",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Organization name"},
                    "description": {"type": "string", "description": "Organization description"},
                    "website": {"type": "string", "description": "Organization website URL"},
                    "github_org": {"type": "string", "description": "GitHub organization name"},
                    "contact_email": {"type": "string", "description": "Contact email"},
                    "metadata": {"type": "object", "description": "Additional metadata"}
                },
                "required": ["name"]
            }
        ),
        Tool(
            name="update_organization",
            description="Update an existing organization",
            inputSchema={
                "type": "object",
                "properties": {
                    "org_id": {"type": "string", "description": "Organization ID"},
                    "name": {"type": "string", "description": "Organization name"},
                    "description": {"type": "string", "description": "Organization description"},
                    "website": {"type": "string", "description": "Organization website URL"},
                    "github_org": {"type": "string", "description": "GitHub organization name"},
                    "contact_email": {"type": "string", "description": "Contact email"},
                    "is_active": {"type": "boolean", "description": "Whether organization is active"},
                    "metadata": {"type": "object", "description": "Additional metadata"}
                },
                "required": ["org_id"]
            }
        ),
        Tool(
            name="get_organization",
            description="Get organization details",
            inputSchema={
                "type": "object",
                "properties": {
                    "org_id": {"type": "string", "description": "Organization ID"}
                },
                "required": ["org_id"]
            }
        ),
        Tool(
            name="list_organizations",
            description="List organizations with optional filtering",
            inputSchema={
                "type": "object",
                "properties": {
                    "limit": {"type": "integer", "description": "Maximum number of results", "default": 50},
                    "offset": {"type": "integer", "description": "Number of results to skip", "default": 0},
                    "active_only": {"type": "boolean", "description": "Only return active organizations", "default": True}
                }
            }
        ),
        Tool(
            name="delete_organization",
            description="Delete (deactivate) an organization",
            inputSchema={
                "type": "object",
                "properties": {
                    "org_id": {"type": "string", "description": "Organization ID"}
                },
                "required": ["org_id"]
            }
        ),
        Tool(
            name="create_project",
            description="Create a new project",
            inputSchema={
                "type": "object",
                "properties": {
                    "organization_id": {"type": "string", "description": "Organization ID"},
                    "name": {"type": "string", "description": "Project name"},
                    "description": {"type": "string", "description": "Project description"},
                    "project_type": {"type": "string", "enum": [t.value for t in ProjectType], "description": "Project type"},
                    "status": {"type": "string", "enum": [s.value for s in ProjectStatus], "description": "Project status"},
                    "github_repo": {"type": "string", "description": "GitHub repository URL"},
                    "github_owner": {"type": "string", "description": "GitHub owner/organization"},
                    "github_repo_name": {"type": "string", "description": "GitHub repository name"},
                    "default_branch": {"type": "string", "description": "Default Git branch", "default": "main"},
                    "tech_stack": {"type": "array", "items": {"type": "string"}, "description": "Technologies used"},
                    "tags": {"type": "array", "items": {"type": "string"}, "description": "Project tags"},
                    "priority": {"type": "integer", "minimum": 1, "maximum": 10, "description": "Project priority", "default": 5},
                    "metadata": {"type": "object", "description": "Additional metadata"}
                },
                "required": ["organization_id", "name"]
            }
        ),
        Tool(
            name="update_project",
            description="Update an existing project",
            inputSchema={
                "type": "object",
                "properties": {
                    "project_id": {"type": "string", "description": "Project ID"},
                    "name": {"type": "string", "description": "Project name"},
                    "description": {"type": "string", "description": "Project description"},
                    "project_type": {"type": "string", "enum": [t.value for t in ProjectType], "description": "Project type"},
                    "status": {"type": "string", "enum": [s.value for s in ProjectStatus], "description": "Project status"},
                    "github_repo": {"type": "string", "description": "GitHub repository URL"},
                    "github_owner": {"type": "string", "description": "GitHub owner/organization"},
                    "github_repo_name": {"type": "string", "description": "GitHub repository name"},
                    "default_branch": {"type": "string", "description": "Default Git branch"},
                    "tech_stack": {"type": "array", "items": {"type": "string"}, "description": "Technologies used"},
                    "tags": {"type": "array", "items": {"type": "string"}, "description": "Project tags"},
                    "priority": {"type": "integer", "minimum": 1, "maximum": 10, "description": "Project priority"},
                    "metadata": {"type": "object", "description": "Additional metadata"}
                },
                "required": ["project_id"]
            }
        ),
        Tool(
            name="get_project",
            description="Get project details",
            inputSchema={
                "type": "object",
                "properties": {
                    "project_id": {"type": "string", "description": "Project ID"}
                },
                "required": ["project_id"]
            }
        ),
        Tool(
            name="list_projects",
            description="List projects with optional filtering",
            inputSchema={
                "type": "object",
                "properties": {
                    "organization_id": {"type": "string", "description": "Filter by organization ID"},
                    "status": {"type": "string", "enum": [s.value for s in ProjectStatus], "description": "Filter by status"},
                    "project_type": {"type": "string", "enum": [t.value for t in ProjectType], "description": "Filter by project type"},
                    "limit": {"type": "integer", "description": "Maximum number of results", "default": 50},
                    "offset": {"type": "integer", "description": "Number of results to skip", "default": 0}
                }
            }
        ),
        Tool(
            name="delete_project",
            description="Delete a project",
            inputSchema={
                "type": "object",
                "properties": {
                    "project_id": {"type": "string", "description": "Project ID"}
                },
                "required": ["project_id"]
            }
        ),
        Tool(
            name="get_organization_stats",
            description="Get detailed statistics for an organization",
            inputSchema={
                "type": "object",
                "properties": {
                    "org_id": {"type": "string", "description": "Organization ID"}
                },
                "required": ["org_id"]
            }
        ),
        Tool(
            name="validate_github_repo",
            description="Validate a GitHub repository URL",
            inputSchema={
                "type": "object",
                "properties": {
                    "github_url": {"type": "string", "description": "GitHub repository URL"}
                },
                "required": ["github_url"]
            }
        )
    ]
    
    return ListToolsResult(tools=tools)


@app.call_tool()
async def call_tool(name: str, arguments: Dict[str, Any]) -> CallToolResult:
    """Handle tool calls"""
    logger.info("Tool called", name=name, arguments=arguments)
    
    try:
        async with get_db_session() as session:
            if name == "create_organization":
                org_manager = OrganizationManager(session)
                request = CreateOrganizationRequest(**arguments)
                organization = await org_manager.create_organization(request)
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=f"Organization '{organization.name}' created successfully with ID: {organization.id}"
                        )
                    ],
                    isError=False
                )
            
            elif name == "update_organization":
                org_manager = OrganizationManager(session)
                org_id = arguments.pop("org_id")
                request = UpdateOrganizationRequest(**arguments)
                organization = await org_manager.update_organization(org_id, request)
                
                if not organization:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Organization {org_id} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=f"Organization '{organization.name}' updated successfully"
                        )
                    ],
                    isError=False
                )
            
            elif name == "get_organization":
                org_manager = OrganizationManager(session)
                organization = await org_manager.get_organization(arguments["org_id"])
                
                if not organization:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Organization {arguments['org_id']} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=str(organization.dict())
                        )
                    ],
                    isError=False
                )
            
            elif name == "list_organizations":
                org_manager = OrganizationManager(session)
                organizations = await org_manager.list_organizations(
                    limit=arguments.get("limit", 50),
                    offset=arguments.get("offset", 0),
                    active_only=arguments.get("active_only", True)
                )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=str({
                                "organizations": [org.dict() for org in organizations],
                                "total": len(organizations)
                            })
                        )
                    ],
                    isError=False
                )
            
            elif name == "delete_organization":
                org_manager = OrganizationManager(session)
                success = await org_manager.delete_organization(arguments["org_id"])
                
                if not success:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Organization {arguments['org_id']} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=f"Organization {arguments['org_id']} deleted successfully"
                        )
                    ],
                    isError=False
                )
            
            elif name == "create_project":
                project_manager = ProjectManager(session)
                request = CreateProjectRequest(**arguments)
                project = await project_manager.create_project(request)
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=f"Project '{project.name}' created successfully with ID: {project.id}"
                        )
                    ],
                    isError=False
                )
            
            elif name == "update_project":
                project_manager = ProjectManager(session)
                project_id = arguments.pop("project_id")
                request = UpdateProjectRequest(**arguments)
                project = await project_manager.update_project(project_id, request)
                
                if not project:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Project {project_id} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=f"Project '{project.name}' updated successfully"
                        )
                    ],
                    isError=False
                )
            
            elif name == "get_project":
                project_manager = ProjectManager(session)
                project = await project_manager.get_project(arguments["project_id"])
                
                if not project:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Project {arguments['project_id']} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=str(project.dict())
                        )
                    ],
                    isError=False
                )
            
            elif name == "list_projects":
                project_manager = ProjectManager(session)
                
                status = None
                if arguments.get("status"):
                    status = ProjectStatus(arguments["status"])
                
                project_type = None
                if arguments.get("project_type"):
                    project_type = ProjectType(arguments["project_type"])
                
                projects = await project_manager.list_projects(
                    organization_id=arguments.get("organization_id"),
                    status=status,
                    project_type=project_type,
                    limit=arguments.get("limit", 50),
                    offset=arguments.get("offset", 0)
                )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=str({
                                "projects": [project.dict() for project in projects],
                                "total": len(projects)
                            })
                        )
                    ],
                    isError=False
                )
            
            elif name == "delete_project":
                project_manager = ProjectManager(session)
                success = await project_manager.delete_project(arguments["project_id"])
                
                if not success:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Project {arguments['project_id']} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=f"Project {arguments['project_id']} deleted successfully"
                        )
                    ],
                    isError=False
                )
            
            elif name == "get_organization_stats":
                org_manager = OrganizationManager(session)
                stats = await org_manager.get_organization_stats(arguments["org_id"])
                
                if not stats:
                    return CallToolResult(
                        content=[TextContent(type="text", text=f"Organization {arguments['org_id']} not found")],
                        isError=True
                    )
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=str(stats.dict())
                        )
                    ],
                    isError=False
                )
            
            elif name == "validate_github_repo":
                project_manager = ProjectManager(session)
                github_info = await project_manager.validate_github_repo(arguments["github_url"])
                
                return CallToolResult(
                    content=[
                        TextContent(
                            type="text",
                            text=str(github_info.dict())
                        )
                    ],
                    isError=False
                )
            
            else:
                return CallToolResult(
                    content=[TextContent(type="text", text=f"Unknown tool: {name}")],
                    isError=True
                )
                
    except Exception as e:
        logger.error("Tool call failed", name=name, error=str(e))
        return CallToolResult(
            content=[TextContent(type="text", text=f"Tool execution failed: {str(e)}")],
            isError=True
        )


async def main():
    """Main entry point"""
    logger.info("Starting Organization MCP Server")
    
    # Initialize database
    await init_database()
    
    try:
        # Import uvicorn here to avoid issues
        import uvicorn
        
        # Get configuration
        host = os.getenv("MCP_HOST", "0.0.0.0")
        port = int(os.getenv("MCP_PORT", "5005"))
        
        logger.info("Server starting", host=host, port=port)
        
        # Run server
        config = uvicorn.Config(
            app=app,
            host=host,
            port=port,
            log_level="info"
        )
        server = uvicorn.Server(config)
        await server.serve()
        
    except KeyboardInterrupt:
        logger.info("Server interrupted by user")
    except Exception as e:
        logger.error("Server error", error=str(e))
        raise
    finally:
        # Close database connections
        await close_database()
        logger.info("Organization MCP Server stopped")


if __name__ == "__main__":
    asyncio.run(main())