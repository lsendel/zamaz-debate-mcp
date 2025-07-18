#!/usr/bin/env python3
"""
Mock API Server for testing MCP Services
Simulates the responses without requiring actual services to be running
"""

import os
import uuid
from datetime import datetime

from flask import Flask, jsonify, request

app = Flask(__name__)

# Enable CSRF protection in production
if os.environ.get("FLASK_ENV", "production") != "development":
    # In production, you would use Flask-WTF for CSRF protection
    # This is a placeholder to show the security consideration
    app.config["WTF_CSRF_ENABLED"] = True

# Storage for mock data
organizations = {}
contexts = {}
debates = {}


# Organization Service Mock (Port 5005)
@app.route("/actuator/health", methods=["GET"])
def health():
    return jsonify(
        {
            "status": "UP",
            "components": {"db": {"status": "UP"}, "diskSpace": {"status": "UP"}, "ping": {"status": "UP"}},
        }
    )


@app.route("/mcp/tools/create_organization", methods=["POST"])
def create_organization():
    data = request.json
    org_id = str(uuid.uuid4())

    org = {
        "organizationId": org_id,
        "name": data.get("name"),
        "slug": data.get("slug"),
        "description": data.get("description"),
        "settings": data.get("settings", {}),
        "createdAt": datetime.now().isoformat(),
    }

    organizations[org_id] = org

    return jsonify({"content": [org]})


@app.route("/api/organizations", methods=["GET"])
def list_organizations():
    return jsonify(
        {"content": list(organizations.values()), "totalElements": len(organizations), "totalPages": 1, "number": 0}
    )


@app.route("/api/organizations/<org_id>", methods=["GET"])
def get_organization(org_id):
    if org_id in organizations:
        return jsonify(organizations[org_id])
    return jsonify({"error": "Organization not found"}), 404


# Context Service Mock (Port 5007)
@app.route("/api/contexts", methods=["POST"])
def create_context():
    data = request.json
    context_id = str(uuid.uuid4())
    org_id = request.headers.get("X-Organization-Id", "default")

    context = {
        "id": context_id,
        "organizationId": org_id,
        "name": data.get("name"),
        "type": data.get("type"),
        "metadata": data.get("metadata", {}),
        "messages": [],
        "createdAt": datetime.now().isoformat(),
    }

    contexts[context_id] = context
    return jsonify(context)


@app.route("/api/contexts/<context_id>/messages", methods=["POST"])
def add_message(context_id):
    if context_id not in contexts:
        return jsonify({"error": "Context not found"}), 404

    data = request.json
    message = {
        "id": str(uuid.uuid4()),
        "role": data.get("role"),
        "content": data.get("content"),
        "metadata": data.get("metadata", {}),
        "timestamp": datetime.now().isoformat(),
    }

    contexts[context_id]["messages"].append(message)
    return jsonify(message)


@app.route("/api/contexts/<context_id>/window", methods=["GET"])
def get_context_window(context_id):
    if context_id not in contexts:
        return jsonify({"error": "Context not found"}), 404

    max_tokens = request.args.get("maxTokens", 1000, type=int)
    context = contexts[context_id]

    return jsonify(
        {
            "contextId": context_id,
            "messages": context["messages"][-10:],  # Last 10 messages
            "tokenCount": len(" ".join([m["content"] for m in context["messages"]])),
            "maxTokens": max_tokens,
        }
    )


# Controller Service Mock (Port 5013)
@app.route("/api/debates", methods=["POST"])
def create_debate():
    data = request.json
    debate_id = str(uuid.uuid4())

    debate = {
        "id": debate_id,
        "organizationId": data.get("organizationId"),
        "title": data.get("title"),
        "description": data.get("description"),
        "topic": data.get("topic"),
        "format": data.get("format"),
        "maxRounds": data.get("maxRounds", 3),
        "currentRound": 0,
        "status": "CREATED",
        "settings": data.get("settings", {}),
        "participants": [],
        "createdAt": datetime.now().isoformat(),
    }

    debates[debate_id] = debate
    return jsonify(debate)


@app.route("/api/debates/<debate_id>/participants", methods=["POST"])
def add_participant(debate_id):
    if debate_id not in debates:
        return jsonify({"error": "Debate not found"}), 404

    data = request.json
    participant = {
        "id": str(uuid.uuid4()),
        "debateId": debate_id,
        "name": data.get("name"),
        "type": data.get("type"),
        "provider": data.get("provider"),
        "model": data.get("model"),
        "position": data.get("position"),
        "createdAt": datetime.now().isoformat(),
    }

    debates[debate_id]["participants"].append(participant)
    return jsonify(participant)


@app.route("/api/debates/<debate_id>/start", methods=["POST"])
def start_debate(debate_id):
    if debate_id not in debates:
        return jsonify({"error": "Debate not found"}), 404

    debate = debates[debate_id]

    if len(debate["participants"]) < 2:
        return jsonify({"error": "Debate must have at least 2 participants"}), 400

    debate["status"] = "IN_PROGRESS"
    debate["currentRound"] = 1
    debate["startedAt"] = datetime.now().isoformat()

    return jsonify(debate)


@app.route("/api/debates", methods=["GET"])
def list_debates():
    org_id = request.args.get("organizationId")

    filtered_debates = debates.values()
    if org_id:
        filtered_debates = [d for d in filtered_debates if d["organizationId"] == org_id]

    return jsonify(
        {"content": list(filtered_debates), "totalElements": len(filtered_debates), "totalPages": 1, "number": 0}
    )


# MCP Protocol endpoints
@app.route("/mcp/resources", methods=["GET"])
def list_resources():
    return jsonify(
        {
            "resources": [
                {"type": "organization", "count": len(organizations)},
                {"type": "context", "count": len(contexts)},
                {"type": "debate", "count": len(debates)},
            ]
        }
    )


@app.route("/mcp/tools", methods=["GET"])
def list_tools():
    return jsonify(
        {
            "tools": [
                {
                    "name": "create_organization",
                    "description": "Create a new organization",
                    "inputSchema": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string"},
                            "slug": {"type": "string"},
                            "description": {"type": "string"},
                        },
                    },
                }
            ]
        }
    )


@app.route("/mcp/prompts", methods=["GET"])
def get_prompts():
    return jsonify(
        {
            "prompts": [
                {
                    "name": "debate_opening",
                    "description": "Generate an opening statement for a debate",
                    "arguments": ["topic", "position", "format"],
                },
                {
                    "name": "debate_rebuttal",
                    "description": "Generate a rebuttal in a debate",
                    "arguments": ["opponent_argument", "position", "context"],
                },
            ]
        }
    )


if __name__ == "__main__":
    import os
    import sys

    port = int(sys.argv[1]) if len(sys.argv) > 1 else 5005
    # Only enable debug mode in development
    debug_mode = os.environ.get("FLASK_ENV", "production") == "development"
    app.run(port=port, debug=debug_mode)
