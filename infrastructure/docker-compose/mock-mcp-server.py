#!/usr/bin/env python3
"""
Mock MCP Server for Testing Endpoints
Simulates MCP endpoints for all services
"""

import uuid
from datetime import datetime

from flask import Flask, jsonify, request

# Create Flask apps for each service
app_org = Flask(__name__ + "_org")
app_llm = Flask(__name__ + "_llm")
app_controller = Flask(__name__ + "_controller")
app_rag = Flask(__name__ + "_rag")


# Organization Service Endpoints (Port 5005)
@app_org.route("/mcp", methods=["GET"])
def org_server_info():
    return jsonify(
        {
            "name": "mcp-organization",
            "version": "1.0.0",
            "description": "Organization management service",
            "capabilities": {"tools": True, "resources": True},
        }
    )


@app_org.route("/mcp/list-tools", methods=["POST"])
def org_list_tools():
    return jsonify(
        {
            "tools": [
                {
                    "name": "create_organization",
                    "description": "Create a new organization",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string", "description": "Organization name"},
                            "description": {"type": "string", "description": "Organization description"},
                        },
                        "required": ["name"],
                    },
                },
                {
                    "name": "list_organizations",
                    "description": "List all organizations",
                    "parameters": {"type": "object"},
                },
            ]
        }
    )


@app_org.route("/mcp/call-tool", methods=["POST"])
def org_call_tool():
    data = request.json
    tool_name = data.get("name")
    args = data.get("arguments", {})

    if tool_name == "create_organization":
        return jsonify(
            {
                "organizationId": str(uuid.uuid4()),
                "name": args.get("name"),
                "description": args.get("description"),
                "createdAt": datetime.now().isoformat(),
            }
        )
    elif tool_name == "list_organizations":
        return jsonify(
            {
                "organizations": [
                    {"id": "test-org-001", "name": "Test Organization", "createdAt": "2024-01-01T00:00:00Z"}
                ]
            }
        )
    else:
        return jsonify({"error": f"Unknown tool: {tool_name}"}), 400


@app_org.route("/health", methods=["GET"])
@app_org.route("/actuator/health", methods=["GET"])
def org_health():
    return jsonify({"status": "UP"})


# LLM Service Endpoints (Port 5002)
@app_llm.route("/mcp", methods=["GET"])
def llm_server_info():
    return jsonify(
        {
            "name": "mcp-llm",
            "version": "1.0.0",
            "description": "LLM Gateway service for multiple AI providers",
            "capabilities": {"tools": True, "resources": True},
        }
    )


@app_llm.route("/mcp/list-tools", methods=["POST"])
def llm_list_tools():
    return jsonify(
        {
            "tools": [
                {
                    "name": "list_providers",
                    "description": "List available LLM providers",
                    "parameters": {"type": "object"},
                },
                {
                    "name": "generate_completion",
                    "description": "Generate text completion using specified provider",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "provider": {"type": "string", "description": "LLM provider"},
                            "prompt": {"type": "string", "description": "Input prompt"},
                            "maxTokens": {"type": "integer", "description": "Maximum tokens"},
                            "temperature": {"type": "number", "description": "Temperature"},
                        },
                        "required": ["provider", "prompt"],
                    },
                },
                {
                    "name": "get_provider_status",
                    "description": "Get status of a specific provider",
                    "parameters": {
                        "type": "object",
                        "properties": {"provider": {"type": "string", "description": "Provider name"}},
                        "required": ["provider"],
                    },
                },
            ]
        }
    )


@app_llm.route("/mcp/call-tool", methods=["POST"])
def llm_call_tool():
    data = request.json
    tool_name = data.get("name")
    args = data.get("arguments", {})

    if tool_name == "list_providers":
        return jsonify(
            {
                "providers": [
                    {"name": "claude", "enabled": True},
                    {"name": "openai", "enabled": True},
                    {"name": "gemini", "enabled": True},
                    {"name": "ollama", "enabled": False},
                ]
            }
        )
    elif tool_name == "generate_completion":
        return jsonify(
            {
                "text": "The sky is blue.",
                "provider": args.get("provider", "openai"),
                "model": "gpt-3.5-turbo",
                "usage": {"promptTokens": 5, "completionTokens": 4, "totalTokens": 9},
            }
        )
    elif tool_name == "get_provider_status":
        return jsonify({"provider": args.get("provider"), "status": "available", "healthCheck": "OK"})
    else:
        return jsonify({"error": f"Unknown tool: {tool_name}"}), 400


@app_llm.route("/health", methods=["GET"])
@app_llm.route("/actuator/health", methods=["GET"])
def llm_health():
    return jsonify({"status": "UP"})


# Controller/Debate Service Endpoints (Port 5013)
@app_controller.route("/mcp", methods=["GET"])
def controller_server_info():
    return jsonify(
        {
            "name": "mcp-debate-controller",
            "version": "1.0.0",
            "description": "Debate orchestration and management service",
            "capabilities": {"tools": True, "resources": True},
        }
    )


@app_controller.route("/mcp/list-tools", methods=["POST"])
def controller_list_tools():
    return jsonify(
        {
            "tools": [
                {
                    "name": "create_debate",
                    "description": "Create a new debate",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "topic": {"type": "string", "description": "Debate topic"},
                            "format": {"type": "string", "description": "Debate format"},
                            "organizationId": {"type": "string", "description": "Organization ID"},
                            "maxRounds": {"type": "integer", "description": "Maximum rounds"},
                        },
                        "required": ["topic", "format", "organizationId"],
                    },
                },
                {
                    "name": "get_debate",
                    "description": "Get debate details by ID",
                    "parameters": {
                        "type": "object",
                        "properties": {"debateId": {"type": "string", "description": "Debate ID"}},
                        "required": ["debateId"],
                    },
                },
                {
                    "name": "list_debates",
                    "description": "List debates for an organization",
                    "parameters": {
                        "type": "object",
                        "properties": {"organizationId": {"type": "string", "description": "Organization ID"}},
                        "required": ["organizationId"],
                    },
                },
                {
                    "name": "submit_turn",
                    "description": "Submit a turn in a debate",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "debateId": {"type": "string", "description": "Debate ID"},
                            "participantId": {"type": "string", "description": "Participant ID"},
                            "content": {"type": "string", "description": "Turn content"},
                        },
                        "required": ["debateId", "participantId", "content"],
                    },
                },
            ]
        }
    )


@app_controller.route("/mcp/call-tool", methods=["POST"])
def controller_call_tool():
    data = request.json
    tool_name = data.get("name")
    args = data.get("arguments", {})

    if tool_name == "create_debate":
        debate_id = str(uuid.uuid4())
        return jsonify({"debateId": debate_id, "status": "CREATED", "topic": args.get("topic")})
    elif tool_name == "get_debate":
        return jsonify(
            {
                "id": args.get("debateId"),
                "topic": "Should AI be regulated?",
                "format": "OXFORD",
                "status": "IN_PROGRESS",
                "currentRound": 1,
                "maxRounds": 3,
                "participants": [],
            }
        )
    elif tool_name == "list_debates":
        return jsonify(
            {
                "debates": [
                    {
                        "id": "test-debate-001",
                        "topic": "Test Debate",
                        "status": "CREATED",
                        "createdAt": datetime.now().isoformat(),
                    }
                ]
            }
        )
    elif tool_name == "submit_turn":
        return jsonify({"responseId": str(uuid.uuid4()), "roundId": str(uuid.uuid4()), "status": "submitted"})
    else:
        return jsonify({"error": f"Unknown tool: {tool_name}"}), 400


@app_controller.route("/health", methods=["GET"])
@app_controller.route("/actuator/health", methods=["GET"])
def controller_health():
    return jsonify({"status": "UP"})


# RAG Service Endpoints (Port 5018)
@app_rag.route("/mcp", methods=["GET"])
def rag_server_info():
    return jsonify(
        {
            "name": "mcp-rag",
            "version": "1.0.0",
            "description": "Retrieval Augmented Generation service",
            "capabilities": {"tools": True, "resources": True},
        }
    )


@app_rag.route("/mcp/list-tools", methods=["POST"])
def rag_list_tools():
    return jsonify(
        {
            "tools": [
                {
                    "name": "index_document",
                    "description": "Index a document for RAG",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "documentId": {"type": "string", "description": "Document ID"},
                            "content": {"type": "string", "description": "Document content"},
                            "metadata": {"type": "object", "description": "Document metadata"},
                        },
                        "required": ["documentId", "content"],
                    },
                },
                {
                    "name": "search",
                    "description": "Search indexed documents",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "query": {"type": "string", "description": "Search query"},
                            "limit": {"type": "integer", "description": "Result limit"},
                        },
                        "required": ["query"],
                    },
                },
                {
                    "name": "get_context",
                    "description": "Get context for a query",
                    "parameters": {
                        "type": "object",
                        "properties": {"query": {"type": "string", "description": "Context query"}},
                        "required": ["query"],
                    },
                },
            ]
        }
    )


@app_rag.route("/mcp/call-tool", methods=["POST"])
def rag_call_tool():
    data = request.json
    tool_name = data.get("name")
    args = data.get("arguments", {})

    if tool_name == "index_document":
        return jsonify(
            {"documentId": args.get("documentId"), "status": "indexed", "timestamp": datetime.now().isoformat()}
        )
    elif tool_name == "search":
        return jsonify({"results": [{"documentId": "doc-001", "score": 0.95, "content": "Sample search result"}]})
    elif tool_name == "get_context":
        return jsonify({"context": "This is the context for your query.", "sources": ["doc-001", "doc-002"]})
    else:
        return jsonify({"error": f"Unknown tool: {tool_name}"}), 400


@app_rag.route("/health", methods=["GET"])
@app_rag.route("/actuator/health", methods=["GET"])
def rag_health():
    return jsonify({"status": "UP"})


if __name__ == "__main__":
    import threading

    # Start all services on different ports
    threading.Thread(target=lambda: app_org.run(port=5005, debug=False)).start()
    threading.Thread(target=lambda: app_llm.run(port=5002, debug=False)).start()
    threading.Thread(target=lambda: app_controller.run(port=5013, debug=False)).start()
    threading.Thread(target=lambda: app_rag.run(port=5018, debug=False)).start()

    # Keep main thread alive
    try:
        while True:
            import time

            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down mock MCP server gracefully...")
        server.shutdown()
        print("Mock MCP server stopped.")
