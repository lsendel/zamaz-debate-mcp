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
    CreateDebateRequest, AddTurnRequest, GetNextTurnRequest,
    SummarizeDebateRequest, DebateStatus
)
from .orchestrators.debate_orchestrator import DebateOrchestrator

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

# Initialize orchestrator
orchestrator = DebateOrchestrator()

# Create MCP server instance
server = Server("mcp-debate")


@server.list_resources()
async def handle_list_resources() -> list[types.Resource]:
    """List available resources"""
    return [
        types.Resource(
            uri="debate://debates",
            name="Debates",
            description="List and manage debates",
            mimeType="application/json",
        ),
        types.Resource(
            uri="debate://formats",
            name="Debate Formats",
            description="Available debate formats and rules",
            mimeType="application/json",
        ),
        types.Resource(
            uri="debate://templates",
            name="Debate Templates",
            description="Pre-configured debate templates",
            mimeType="application/json",
        ),
    ]


@server.read_resource()
async def handle_read_resource(uri: str) -> str:
    """Read a specific resource"""
    logger.info("Reading resource", uri=uri)
    
    if uri == "debate://debates":
        # List all debates
        debates = await orchestrator.debate_store.list_debates()
        return json.dumps({
            "debates": [d.dict() for d in debates]
        })
    
    elif uri == "debate://formats":
        # List available formats
        from .models import DebateFormat
        formats = {
            format.value: {
                "name": format.value,
                "description": get_format_description(format)
            }
            for format in DebateFormat
        }
        return json.dumps({"formats": formats})
    
    elif uri == "debate://templates":
        # Return debate templates
        templates = get_debate_templates()
        return json.dumps({"templates": templates})
    
    elif uri.startswith("debate://debates/"):
        debate_id = uri.split("/")[-1]
        debate = await orchestrator.debate_store.get_debate(debate_id)
        if debate:
            return debate.json()
        else:
            return json.dumps({"error": "Debate not found"})
    
    else:
        raise ValueError(f"Unknown resource URI: {uri}")


@server.list_tools()
async def handle_list_tools() -> list[types.Tool]:
    """List available tools"""
    return [
        types.Tool(
            name="create_debate",
            description="Create a new debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "topic": {"type": "string"},
                    "description": {"type": "string"},
                    "participants": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string"},
                                "role": {
                                    "type": "string",
                                    "enum": ["debater", "moderator", "judge", "observer"],
                                    "default": "debater"
                                },
                                "position": {"type": "string"},
                                "llm_config": {
                                    "type": "object",
                                    "properties": {
                                        "provider": {
                                            "type": "string",
                                            "enum": ["claude", "openai", "gemini", "llama"]
                                        },
                                        "model": {"type": "string"},
                                        "temperature": {"type": "number", "default": 0.7},
                                        "system_prompt": {"type": "string"}
                                    },
                                    "required": ["provider", "model"]
                                }
                            },
                            "required": ["name", "llm_config"]
                        }
                    },
                    "rules": {
                        "type": "object",
                        "properties": {
                            "format": {
                                "type": "string",
                                "enum": ["round_robin", "free_form", "oxford", "panel", "socratic", "adversarial"],
                                "default": "round_robin"
                            },
                            "max_rounds": {"type": "integer"},
                            "max_turns_per_participant": {"type": "integer"},
                            "turn_time_limit_seconds": {"type": "integer"}
                        }
                    }
                },
                "required": ["name", "topic", "participants"]
            }
        ),
        types.Tool(
            name="start_debate",
            description="Start a debate that's in draft status",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"}
                },
                "required": ["debate_id"]
            }
        ),
        types.Tool(
            name="add_turn",
            description="Add a turn to an active debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"},
                    "participant_id": {"type": "string"},
                    "turn_type": {
                        "type": "string",
                        "enum": ["opening", "argument", "rebuttal", "question", "answer", "closing"],
                        "default": "argument"
                    },
                    "content": {"type": "string"},
                    "use_rag": {"type": "boolean", "default": false},
                    "rag_query": {"type": "string"}
                },
                "required": ["debate_id"]
            }
        ),
        types.Tool(
            name="get_next_turn",
            description="Automatically orchestrate the next turn in a debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"},
                    "include_rag": {"type": "boolean", "default": false},
                    "rag_knowledge_base": {"type": "string"}
                },
                "required": ["debate_id"]
            }
        ),
        types.Tool(
            name="summarize_debate",
            description="Generate a summary of a debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"},
                    "summary_style": {
                        "type": "string",
                        "enum": ["concise", "detailed", "bullet_points"],
                        "default": "concise"
                    },
                    "include_consensus": {"type": "boolean", "default": true},
                    "include_disagreements": {"type": "boolean", "default": true}
                },
                "required": ["debate_id"]
            }
        ),
        types.Tool(
            name="get_debate_status",
            description="Get the current status of a debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"}
                },
                "required": ["debate_id"]
            }
        ),
        types.Tool(
            name="pause_debate",
            description="Pause an active debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"}
                },
                "required": ["debate_id"]
            }
        ),
        types.Tool(
            name="resume_debate",
            description="Resume a paused debate",
            inputSchema={
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"}
                },
                "required": ["debate_id"]
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
        if name == "create_debate":
            # Add org_id from auth context
            request = CreateDebateRequest(
                org_id="default",  # Would get from auth in production
                **arguments
            )
            debate = await orchestrator.create_debate(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "debate_id": debate.id,
                    "status": debate.status.value,
                    "participants": [p.name for p in debate.participants]
                })
            )]
        
        elif name == "start_debate":
            debate = await orchestrator.start_debate(arguments["debate_id"])
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "debate_id": debate.id,
                    "status": debate.status.value,
                    "started_at": debate.started_at.isoformat() if debate.started_at else None,
                    "next_participant": debate.next_participant_id
                })
            )]
        
        elif name == "add_turn":
            request = AddTurnRequest(**arguments)
            turn = await orchestrator.add_turn(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "turn_id": turn.id,
                    "participant_id": turn.participant_id,
                    "content": turn.content,
                    "turn_number": turn.turn_number,
                    "round_number": turn.round_number
                })
            )]
        
        elif name == "get_next_turn":
            request = GetNextTurnRequest(**arguments)
            turn = await orchestrator.get_next_turn(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "turn_id": turn.id,
                    "participant_id": turn.participant_id,
                    "content": turn.content,
                    "turn_type": turn.turn_type.value
                })
            )]
        
        elif name == "summarize_debate":
            request = SummarizeDebateRequest(**arguments)
            summary = await orchestrator.summarize_debate(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "summary": summary.summary,
                    "key_points": summary.key_points,
                    "consensus_points": summary.consensus_points,
                    "disagreement_points": summary.disagreement_points
                })
            )]
        
        elif name == "get_debate_status":
            debate = await orchestrator.debate_store.get_debate(arguments["debate_id"])
            if debate:
                return [types.TextContent(
                    type="text",
                    text=json.dumps({
                        "debate_id": debate.id,
                        "status": debate.status.value,
                        "current_round": debate.current_round,
                        "current_turn": debate.current_turn,
                        "next_participant": debate.next_participant_id
                    })
                )]
            else:
                return [types.TextContent(
                    type="text",
                    text=json.dumps({"error": "Debate not found"})
                )]
        
        elif name == "pause_debate":
            debate = await orchestrator.debate_store.get_debate(arguments["debate_id"])
            if debate and debate.status == DebateStatus.ACTIVE:
                debate.status = DebateStatus.PAUSED
                await orchestrator.debate_store.save_debate(debate)
                return [types.TextContent(
                    type="text",
                    text=json.dumps({"status": "paused"})
                )]
            else:
                return [types.TextContent(
                    type="text",
                    text=json.dumps({"error": "Cannot pause debate"})
                )]
        
        elif name == "resume_debate":
            debate = await orchestrator.debate_store.get_debate(arguments["debate_id"])
            if debate and debate.status == DebateStatus.PAUSED:
                debate.status = DebateStatus.ACTIVE
                await orchestrator.debate_store.save_debate(debate)
                return [types.TextContent(
                    type="text",
                    text=json.dumps({"status": "active"})
                )]
            else:
                return [types.TextContent(
                    type="text",
                    text=json.dumps({"error": "Cannot resume debate"})
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


def get_format_description(format) -> str:
    """Get description for debate format"""
    descriptions = {
        "round_robin": "Each participant takes turns in order",
        "free_form": "No strict turn order, participants contribute freely",
        "oxford": "Formal Oxford-style debate with structured rounds",
        "panel": "Panel discussion format with moderator",
        "socratic": "Question-driven format focusing on inquiry",
        "adversarial": "Two-sided argument with clear opposing positions"
    }
    return descriptions.get(format.value, "")


def get_debate_templates() -> list[dict]:
    """Get pre-configured debate templates"""
    return [
        {
            "name": "Climate Policy Debate",
            "topic": "Should governments implement carbon taxes?",
            "format": "oxford",
            "participant_template": [
                {"role": "debater", "position": "Pro carbon tax"},
                {"role": "debater", "position": "Against carbon tax"},
                {"role": "moderator", "position": "Neutral"}
            ]
        },
        {
            "name": "AI Ethics Discussion",
            "topic": "How should AI development be regulated?",
            "format": "panel",
            "participant_template": [
                {"role": "debater", "position": "Industry perspective"},
                {"role": "debater", "position": "Academic perspective"},
                {"role": "debater", "position": "Policy perspective"},
                {"role": "moderator", "position": "Neutral"}
            ]
        },
        {
            "name": "Philosophical Inquiry",
            "topic": "What is the nature of consciousness?",
            "format": "socratic",
            "participant_template": [
                {"role": "debater", "position": "Materialist view"},
                {"role": "debater", "position": "Dualist view"},
                {"role": "debater", "position": "Panpsychist view"}
            ]
        }
    ]


async def main():
    """Main entry point"""
    logger.info("Starting mcp-debate server")
    
    # Initialize database
    await orchestrator.debate_store.initialize()
    
    # Run the server
    async with mcp.server.stdio.stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="mcp-debate",
                server_version="0.1.0",
                capabilities=server.get_capabilities(
                    notification_options=NotificationOptions(),
                    experimental_capabilities={},
                ),
            ),
        )


if __name__ == "__main__":
    asyncio.run(main())