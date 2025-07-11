import os
import asyncio
from typing import Any, Sequence
import mcp.server.stdio
import mcp.types as types
from mcp.server import NotificationOptions, Server
from mcp.server.models import InitializationOptions
import structlog
from dotenv import load_dotenv
import json

from .models import (
    CompletionRequest, LLMProvider, Message
)
from .providers.provider_factory import ProviderFactory

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

# Create MCP server instance
server = Server("mcp-llm")


@server.list_resources()
async def handle_list_resources() -> list[types.Resource]:
    """List available resources"""
    return [
        types.Resource(
            uri="llm://providers",
            name="LLM Providers",
            description="List available LLM providers and their models",
            mimeType="application/json",
        ),
        types.Resource(
            uri="llm://models",
            name="Available Models",
            description="List all available models across providers",
            mimeType="application/json",
        ),
        types.Resource(
            uri="llm://conversations",
            name="Conversation History",
            description="Access conversation history (if caching enabled)",
            mimeType="application/json",
        ),
    ]


@server.read_resource()
async def handle_read_resource(uri: str) -> str:
    """Read a specific resource"""
    logger.info("Reading resource", uri=uri)
    
    if uri == "llm://providers":
        providers = ProviderFactory.list_providers()
        return json.dumps({"providers": providers})
    
    elif uri == "llm://models":
        models = ProviderFactory.get_all_models()
        return json.dumps({"models": models})
    
    elif uri == "llm://conversations":
        # This would connect to Redis/cache in production
        return json.dumps({"conversations": []})
    
    elif uri.startswith("llm://providers/"):
        provider_name = uri.split("/")[-1]
        try:
            provider = LLMProvider(provider_name)
            provider_instance = ProviderFactory.get_provider(provider)
            models = list(provider_instance.MODELS.keys())
            return json.dumps({
                "provider": provider_name,
                "models": models
            })
        except Exception as e:
            return json.dumps({"error": str(e)})
    
    else:
        raise ValueError(f"Unknown resource URI: {uri}")


@server.list_tools()
async def handle_list_tools() -> list[types.Tool]:
    """List available tools"""
    return [
        types.Tool(
            name="complete",
            description="Generate a completion from any supported LLM",
            inputSchema={
                "type": "object",
                "properties": {
                    "provider": {
                        "type": "string",
                        "enum": ["claude", "openai", "gemini", "llama"],
                        "description": "LLM provider to use"
                    },
                    "model": {
                        "type": "string",
                        "description": "Model name (e.g., 'gpt-4', 'claude-3-opus')"
                    },
                    "messages": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "role": {
                                    "type": "string",
                                    "enum": ["system", "user", "assistant"]
                                },
                                "content": {"type": "string"}
                            },
                            "required": ["role", "content"]
                        },
                        "description": "Conversation messages"
                    },
                    "max_tokens": {
                        "type": "integer",
                        "description": "Maximum tokens to generate",
                        "default": 1000
                    },
                    "temperature": {
                        "type": "number",
                        "description": "Sampling temperature",
                        "default": 0.7,
                        "minimum": 0,
                        "maximum": 2
                    }
                },
                "required": ["provider", "model", "messages"]
            }
        ),
        types.Tool(
            name="stream_complete",
            description="Stream a completion from any supported LLM",
            inputSchema={
                "type": "object",
                "properties": {
                    "provider": {
                        "type": "string",
                        "enum": ["claude", "openai", "gemini", "llama"]
                    },
                    "model": {"type": "string"},
                    "messages": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "role": {
                                    "type": "string",
                                    "enum": ["system", "user", "assistant"]
                                },
                                "content": {"type": "string"}
                            },
                            "required": ["role", "content"]
                        }
                    },
                    "max_tokens": {"type": "integer", "default": 1000},
                    "temperature": {"type": "number", "default": 0.7}
                },
                "required": ["provider", "model", "messages"]
            }
        ),
        types.Tool(
            name="list_models",
            description="List available models for a specific provider",
            inputSchema={
                "type": "object",
                "properties": {
                    "provider": {
                        "type": "string",
                        "enum": ["claude", "openai", "gemini", "llama"]
                    }
                },
                "required": ["provider"]
            }
        ),
        types.Tool(
            name="estimate_tokens",
            description="Estimate token count for messages",
            inputSchema={
                "type": "object",
                "properties": {
                    "provider": {
                        "type": "string",
                        "enum": ["claude", "openai", "gemini", "llama"]
                    },
                    "messages": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "role": {
                                    "type": "string",
                                    "enum": ["system", "user", "assistant"]
                                },
                                "content": {"type": "string"}
                            },
                            "required": ["role", "content"]
                        }
                    }
                },
                "required": ["provider", "messages"]
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
        if name == "complete":
            # Parse arguments
            provider = LLMProvider(arguments["provider"])
            model = arguments["model"]
            messages = [Message(**msg) for msg in arguments["messages"]]
            max_tokens = arguments.get("max_tokens", 1000)
            temperature = arguments.get("temperature", 0.7)
            
            # Get provider instance
            provider_instance = ProviderFactory.get_provider(provider)
            
            # Generate completion
            response = await provider_instance.complete(
                messages=messages,
                model=model,
                max_tokens=max_tokens,
                temperature=temperature
            )
            
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "content": response.content,
                    "usage": response.usage,
                    "finish_reason": response.finish_reason,
                    "provider": response.provider,
                    "model": response.model
                })
            )]
        
        elif name == "stream_complete":
            # Streaming would be handled differently in a real implementation
            # For now, we'll use the regular complete endpoint
            provider = LLMProvider(arguments["provider"])
            model = arguments["model"]
            messages = [Message(**msg) for msg in arguments["messages"]]
            
            provider_instance = ProviderFactory.get_provider(provider)
            
            # Collect stream into a single response
            chunks = []
            async for chunk in provider_instance.stream_complete(
                messages=messages,
                model=model,
                max_tokens=arguments.get("max_tokens", 1000),
                temperature=arguments.get("temperature", 0.7)
            ):
                chunks.append(chunk)
            
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "content": "".join(chunks),
                    "provider": provider.value,
                    "model": model,
                    "streamed": True
                })
            )]
        
        elif name == "list_models":
            provider = LLMProvider(arguments["provider"])
            provider_instance = ProviderFactory.get_provider(provider)
            
            models = []
            for model_name, model_info in provider_instance.MODELS.items():
                models.append({
                    "name": model_name,
                    "context_window": model_info.context_window,
                    "max_output_tokens": model_info.max_output_tokens,
                    "capabilities": model_info.capabilities,
                    "input_cost_per_1k": model_info.input_cost_per_1k,
                    "output_cost_per_1k": model_info.output_cost_per_1k
                })
            
            return [types.TextContent(
                type="text",
                text=json.dumps({"provider": provider.value, "models": models})
            )]
        
        elif name == "estimate_tokens":
            provider = LLMProvider(arguments["provider"])
            messages = [Message(**msg) for msg in arguments["messages"]]
            
            provider_instance = ProviderFactory.get_provider(provider)
            token_count = provider_instance.estimate_tokens(messages)
            
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "provider": provider.value,
                    "token_count": token_count,
                    "messages": len(messages)
                })
            )]
        
        else:
            raise ValueError(f"Unknown tool: {name}")
            
    except Exception as e:
        logger.error("Tool execution failed", tool=name, error=str(e))
        return [types.TextContent(
            type="text",
            text=json.dumps({
                "error": str(e),
                "error_type": type(e).__name__
            })
        )]


async def main():
    """Main entry point"""
    logger.info("Starting mcp-llm server")
    
    # Verify API keys are available
    missing_keys = []
    if not os.getenv("OPENAI_API_KEY"):
        missing_keys.append("OPENAI_API_KEY")
    if not os.getenv("ANTHROPIC_API_KEY"):
        missing_keys.append("ANTHROPIC_API_KEY")
    if not os.getenv("GOOGLE_API_KEY"):
        missing_keys.append("GOOGLE_API_KEY")
    
    if missing_keys:
        logger.warning("Missing API keys", keys=missing_keys)
    
    # Run the server
    async with mcp.server.stdio.stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="mcp-llm",
                server_version="0.1.0",
                capabilities=server.get_capabilities(
                    notification_options=NotificationOptions(),
                    experimental_capabilities={},
                ),
            ),
        )


if __name__ == "__main__":
    asyncio.run(main())