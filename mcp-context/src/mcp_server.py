import os
import asyncio
from typing import Any, Sequence
import mcp.server.stdio
import mcp.types as types
from mcp.server import NotificationOptions, Server
from mcp.server.models import InitializationOptions
import structlog
from dotenv import load_dotenv

from .models import (
    CreateContextRequest, AppendMessagesRequest, GetContextWindowRequest,
    ShareContextRequest, SearchContextsRequest, Context, ContextWindow
)
from .managers.context_manager import ContextManager
from .managers.auth_manager import AuthManager
from .db.connection import DatabaseManager

# Load environment variables
load_dotenv()

# Configure logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.dev.ConsoleRenderer()
    ],
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()

# Initialize managers
db_manager = DatabaseManager()
auth_manager = AuthManager()
context_manager = ContextManager(db_manager)

# Create MCP server instance
server = Server("mcp-context")


@server.list_resources()
async def handle_list_resources() -> list[types.Resource]:
    """List available resources"""
    return [
        types.Resource(
            uri="context://namespaces",
            name="Context Namespaces",
            description="List and manage context namespaces for organization",
            mimeType="application/json",
        ),
        types.Resource(
            uri="context://contexts",
            name="Contexts",
            description="List and manage contexts within namespaces",
            mimeType="application/json",
        ),
        types.Resource(
            uri="context://shared",
            name="Shared Contexts",
            description="Contexts shared with your organization",
            mimeType="application/json",
        ),
    ]


@server.read_resource()
async def handle_read_resource(uri: str) -> str:
    """Read a specific resource"""
    logger.info("Reading resource", uri=uri)
    
    if uri == "context://namespaces":
        # Get org_id from auth context
        org_id = auth_manager.get_current_org_id()
        namespaces = await context_manager.list_namespaces(org_id)
        return str(namespaces)
    
    elif uri == "context://contexts":
        org_id = auth_manager.get_current_org_id()
        contexts = await context_manager.list_contexts(org_id)
        return str(contexts)
    
    elif uri == "context://shared":
        org_id = auth_manager.get_current_org_id()
        shared = await context_manager.list_shared_contexts(org_id)
        return str(shared)
    
    elif uri.startswith("context://contexts/"):
        context_id = uri.split("/")[-1]
        context = await context_manager.get_context(context_id)
        return context.json()
    
    else:
        raise ValueError(f"Unknown resource URI: {uri}")


@server.list_tools()
async def handle_list_tools() -> list[types.Tool]:
    """List available tools"""
    return [
        types.Tool(
            name="create_context",
            description="Create a new context in a namespace",
            inputSchema={
                "type": "object",
                "properties": {
                    "namespace_id": {"type": "string"},
                    "name": {"type": "string"},
                    "description": {"type": "string"},
                    "initial_messages": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "role": {"type": "string", "enum": ["system", "user", "assistant"]},
                                "content": {"type": "string"}
                            },
                            "required": ["role", "content"]
                        }
                    }
                },
                "required": ["namespace_id", "name"]
            }
        ),
        types.Tool(
            name="append_to_context",
            description="Append messages to an existing context",
            inputSchema={
                "type": "object",
                "properties": {
                    "context_id": {"type": "string"},
                    "messages": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "role": {"type": "string", "enum": ["system", "user", "assistant"]},
                                "content": {"type": "string"}
                            },
                            "required": ["role", "content"]
                        }
                    }
                },
                "required": ["context_id", "messages"]
            }
        ),
        types.Tool(
            name="get_context_window",
            description="Get an optimized context window for LLM consumption",
            inputSchema={
                "type": "object",
                "properties": {
                    "context_id": {"type": "string"},
                    "max_tokens": {"type": "integer", "default": 8000},
                    "strategy": {
                        "type": "string",
                        "enum": ["full", "sliding_window", "sliding_window_with_summary", "semantic_selection"],
                        "default": "sliding_window"
                    }
                },
                "required": ["context_id"]
            }
        ),
        types.Tool(
            name="share_context",
            description="Share a context with another organization",
            inputSchema={
                "type": "object",
                "properties": {
                    "context_id": {"type": "string"},
                    "target_org_id": {"type": "string"},
                    "access_level": {
                        "type": "string",
                        "enum": ["read", "append", "write"],
                        "default": "read"
                    },
                    "expires_in_hours": {"type": "integer"}
                },
                "required": ["context_id", "target_org_id"]
            }
        ),
        types.Tool(
            name="search_contexts",
            description="Search contexts by name or content",
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {"type": "string"},
                    "namespace_id": {"type": "string"},
                    "limit": {"type": "integer", "default": 20},
                    "include_shared": {"type": "boolean", "default": true}
                }
            }
        ),
        types.Tool(
            name="fork_context",
            description="Create a copy of an existing context",
            inputSchema={
                "type": "object",
                "properties": {
                    "context_id": {"type": "string"},
                    "new_name": {"type": "string"},
                    "namespace_id": {"type": "string"}
                },
                "required": ["context_id", "new_name"]
            }
        ),
        types.Tool(
            name="compress_context",
            description="Compress older messages in a context to save tokens",
            inputSchema={
                "type": "object",
                "properties": {
                    "context_id": {"type": "string"},
                    "keep_recent": {"type": "integer", "default": 10},
                    "summary_style": {
                        "type": "string",
                        "enum": ["concise", "detailed", "bullet_points"],
                        "default": "concise"
                    }
                },
                "required": ["context_id"]
            }
        )
    ]


@server.call_tool()
async def handle_call_tool(
    name: str, arguments: dict | None
) -> Sequence[types.TextContent | types.ImageContent | types.EmbeddedResource]:
    """Handle tool calls"""
    logger.info("Tool called", tool=name, args=arguments)
    
    try:
        # Get org_id from auth context for all operations
        org_id = auth_manager.get_current_org_id()
        
        if name == "create_context":
            request = CreateContextRequest(
                org_id=org_id,
                **arguments
            )
            context = await context_manager.create_context(request)
            return [types.TextContent(
                type="text",
                text=f"Created context: {context.id}"
            )]
        
        elif name == "append_to_context":
            request = AppendMessagesRequest(**arguments)
            updated = await context_manager.append_messages(request)
            return [types.TextContent(
                type="text",
                text=f"Appended {len(request.messages)} messages to context"
            )]
        
        elif name == "get_context_window":
            request = GetContextWindowRequest(**arguments)
            window = await context_manager.get_context_window(request)
            return [types.TextContent(
                type="text",
                text=window.json()
            )]
        
        elif name == "share_context":
            request = ShareContextRequest(**arguments)
            share = await context_manager.share_context(request)
            return [types.TextContent(
                type="text",
                text=f"Shared context with {request.target_org_id}"
            )]
        
        elif name == "search_contexts":
            request = SearchContextsRequest(org_id=org_id, **arguments)
            results = await context_manager.search_contexts(request)
            return [types.TextContent(
                type="text",
                text=str(results)
            )]
        
        elif name == "fork_context":
            new_context = await context_manager.fork_context(
                arguments["context_id"],
                arguments["new_name"],
                arguments.get("namespace_id")
            )
            return [types.TextContent(
                type="text",
                text=f"Forked context: {new_context.id}"
            )]
        
        elif name == "compress_context":
            compressed = await context_manager.compress_context(
                arguments["context_id"],
                arguments.get("keep_recent", 10),
                arguments.get("summary_style", "concise")
            )
            return [types.TextContent(
                type="text",
                text=f"Compressed context, saved {compressed['tokens_saved']} tokens"
            )]
        
        else:
            raise ValueError(f"Unknown tool: {name}")
            
    except Exception as e:
        logger.error("Tool execution failed", tool=name, error=str(e))
        return [types.TextContent(
            type="text",
            text=f"Error: {str(e)}"
        )]


async def main():
    """Main entry point"""
    logger.info("Starting mcp-context server")
    
    # Initialize database
    await db_manager.initialize()
    
    # Run the server
    async with mcp.server.stdio.stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="mcp-context",
                server_version="0.1.0",
                capabilities=server.get_capabilities(
                    notification_options=NotificationOptions(),
                    experimental_capabilities={},
                ),
            ),
        )


if __name__ == "__main__":
    asyncio.run(main())