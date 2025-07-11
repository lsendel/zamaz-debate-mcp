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
    CreateKnowledgeBaseRequest, IngestDocumentRequest,
    SearchRequest, AugmentContextRequest
)
from .managers.rag_manager import RAGManager

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

# Initialize RAG manager
rag_manager = RAGManager()

# Create MCP server instance
server = Server("mcp-rag")


@server.list_resources()
async def handle_list_resources() -> list[types.Resource]:
    """List available resources"""
    return [
        types.Resource(
            uri="rag://knowledge-bases",
            name="Knowledge Bases",
            description="List and manage knowledge bases",
            mimeType="application/json",
        ),
        types.Resource(
            uri="rag://embeddings",
            name="Embedding Models",
            description="Available embedding models",
            mimeType="application/json",
        ),
        types.Resource(
            uri="rag://stats",
            name="RAG Statistics",
            description="System statistics and metrics",
            mimeType="application/json",
        ),
    ]


@server.read_resource()
async def handle_read_resource(uri: str) -> str:
    """Read a specific resource"""
    logger.info("Reading resource", uri=uri)
    
    if uri == "rag://knowledge-bases":
        kbs = await rag_manager.list_knowledge_bases()
        return json.dumps({"knowledge_bases": [kb.dict() for kb in kbs]})
    
    elif uri == "rag://embeddings":
        from .models import EmbeddingModel
        models = {
            model.value: {
                "name": model.value,
                "provider": get_embedding_provider(model),
                "dimensions": get_embedding_dimensions(model)
            }
            for model in EmbeddingModel
        }
        return json.dumps({"embedding_models": models})
    
    elif uri == "rag://stats":
        stats = await rag_manager.get_system_stats()
        return json.dumps({"stats": stats})
    
    elif uri.startswith("rag://knowledge-bases/"):
        kb_id = uri.split("/")[-1]
        kb = await rag_manager.get_knowledge_base(kb_id)
        if kb:
            stats = await rag_manager.get_kb_stats(kb_id)
            return json.dumps({
                "knowledge_base": kb.dict(),
                "stats": stats.dict() if stats else None
            })
        else:
            return json.dumps({"error": "Knowledge base not found"})
    
    else:
        raise ValueError(f"Unknown resource URI: {uri}")


@server.list_tools()
async def handle_list_tools() -> list[types.Tool]:
    """List available tools"""
    return [
        types.Tool(
            name="create_knowledge_base",
            description="Create a new knowledge base for document storage",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "description": {"type": "string"},
                    "embedding_model": {
                        "type": "string",
                        "enum": [
                            "text-embedding-ada-002",
                            "text-embedding-3-small",
                            "all-MiniLM-L6-v2",
                            "all-mpnet-base-v2",
                            "e5-small-v2",
                            "e5-base-v2"
                        ],
                        "default": "all-MiniLM-L6-v2"
                    },
                    "chunking_strategy": {
                        "type": "string",
                        "enum": ["fixed_size", "sentence", "paragraph", "semantic", "sliding_window"],
                        "default": "sliding_window"
                    },
                    "chunk_size": {"type": "integer", "default": 512},
                    "chunk_overlap": {"type": "integer", "default": 50}
                },
                "required": ["name"]
            }
        ),
        types.Tool(
            name="ingest_document",
            description="Add a document to a knowledge base",
            inputSchema={
                "type": "object",
                "properties": {
                    "kb_id": {"type": "string"},
                    "name": {"type": "string"},
                    "content": {"type": "string"},
                    "file_path": {"type": "string"},
                    "url": {"type": "string"},
                    "document_type": {
                        "type": "string",
                        "enum": ["pdf", "docx", "txt", "markdown", "html", "json", "csv"]
                    },
                    "metadata": {"type": "object"}
                },
                "required": ["kb_id", "name"],
                "oneOf": [
                    {"required": ["content"]},
                    {"required": ["file_path"]},
                    {"required": ["url"]}
                ]
            }
        ),
        types.Tool(
            name="search",
            description="Search for relevant information in a knowledge base",
            inputSchema={
                "type": "object",
                "properties": {
                    "kb_id": {"type": "string"},
                    "query": {"type": "string"},
                    "max_results": {"type": "integer", "default": 5},
                    "min_score": {"type": "number", "default": 0.5},
                    "filter_metadata": {"type": "object"},
                    "rerank": {"type": "boolean", "default": false}
                },
                "required": ["kb_id", "query"]
            }
        ),
        types.Tool(
            name="augment_context",
            description="Augment a context with relevant information from RAG",
            inputSchema={
                "type": "object",
                "properties": {
                    "context_id": {"type": "string"},
                    "kb_id": {"type": "string"},
                    "query": {"type": "string"},
                    "max_chunks": {"type": "integer", "default": 3},
                    "insertion_strategy": {
                        "type": "string",
                        "enum": ["prepend", "append", "interleave"],
                        "default": "prepend"
                    }
                },
                "required": ["context_id", "kb_id", "query"]
            }
        ),
        types.Tool(
            name="delete_document",
            description="Remove a document from a knowledge base",
            inputSchema={
                "type": "object",
                "properties": {
                    "kb_id": {"type": "string"},
                    "document_id": {"type": "string"}
                },
                "required": ["kb_id", "document_id"]
            }
        ),
        types.Tool(
            name="update_embeddings",
            description="Re-generate embeddings for a knowledge base",
            inputSchema={
                "type": "object",
                "properties": {
                    "kb_id": {"type": "string"},
                    "embedding_model": {
                        "type": "string",
                        "enum": [
                            "text-embedding-ada-002",
                            "text-embedding-3-small",
                            "all-MiniLM-L6-v2",
                            "all-mpnet-base-v2"
                        ]
                    }
                },
                "required": ["kb_id"]
            }
        ),
        types.Tool(
            name="export_knowledge_base",
            description="Export a knowledge base for backup or transfer",
            inputSchema={
                "type": "object",
                "properties": {
                    "kb_id": {"type": "string"},
                    "format": {
                        "type": "string",
                        "enum": ["json", "parquet", "csv"],
                        "default": "json"
                    },
                    "include_embeddings": {"type": "boolean", "default": false}
                },
                "required": ["kb_id"]
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
        if name == "create_knowledge_base":
            request = CreateKnowledgeBaseRequest(
                org_id="default",  # Would get from auth in production
                **arguments
            )
            kb = await rag_manager.create_knowledge_base(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "kb_id": kb.id,
                    "name": kb.name,
                    "embedding_model": kb.embedding_model.value
                })
            )]
        
        elif name == "ingest_document":
            request = IngestDocumentRequest(**arguments)
            document = await rag_manager.ingest_document(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "document_id": document.id,
                    "chunk_count": document.chunk_count,
                    "kb_id": document.kb_id
                })
            )]
        
        elif name == "search":
            request = SearchRequest(**arguments)
            results = await rag_manager.search(request)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "results": [
                        {
                            "content": r.content,
                            "score": r.score,
                            "document_name": r.document_name,
                            "metadata": r.metadata
                        }
                        for r in results
                    ],
                    "count": len(results)
                })
            )]
        
        elif name == "augment_context":
            request = AugmentContextRequest(**arguments)
            result = await rag_manager.augment_context(request)
            return [types.TextContent(
                type="text",
                text=json.dumps(result)
            )]
        
        elif name == "delete_document":
            success = await rag_manager.delete_document(
                arguments["kb_id"],
                arguments["document_id"]
            )
            return [types.TextContent(
                type="text",
                text=json.dumps({"success": success})
            )]
        
        elif name == "update_embeddings":
            kb_id = arguments["kb_id"]
            model = arguments.get("embedding_model")
            
            updated = await rag_manager.update_embeddings(kb_id, model)
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "kb_id": kb_id,
                    "chunks_updated": updated
                })
            )]
        
        elif name == "export_knowledge_base":
            export_data = await rag_manager.export_knowledge_base(
                arguments["kb_id"],
                arguments.get("format", "json"),
                arguments.get("include_embeddings", False)
            )
            return [types.TextContent(
                type="text",
                text=json.dumps({
                    "export": export_data,
                    "format": arguments.get("format", "json")
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


def get_embedding_provider(model) -> str:
    """Get the provider for an embedding model"""
    if "openai" in model.value or "ada" in model.value:
        return "openai"
    elif "e5" in model.value:
        return "intfloat"
    else:
        return "sentence-transformers"


def get_embedding_dimensions(model) -> int:
    """Get the dimensions for an embedding model"""
    dimensions = {
        "text-embedding-ada-002": 1536,
        "text-embedding-3-small": 1536,
        "text-embedding-3-large": 3072,
        "all-MiniLM-L6-v2": 384,
        "all-mpnet-base-v2": 768,
        "e5-small-v2": 384,
        "e5-base-v2": 768,
        "e5-large-v2": 1024
    }
    return dimensions.get(model.value, 768)


async def main():
    """Main entry point"""
    logger.info("Starting mcp-rag server")
    
    # Initialize vector database
    await rag_manager.initialize()
    
    # Run the server
    async with mcp.server.stdio.stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="mcp-rag",
                server_version="0.1.0",
                capabilities=server.get_capabilities(
                    notification_options=NotificationOptions(),
                    experimental_capabilities={},
                ),
            ),
        )


if __name__ == "__main__":
    asyncio.run(main())