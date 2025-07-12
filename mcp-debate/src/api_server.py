from fastapi import FastAPI, WebSocket, Query
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional
import uvicorn
import os
from dotenv import load_dotenv

from .websocket_manager import websocket_endpoint, manager
from .orchestrators.debate_orchestrator import DebateOrchestrator

# Load environment variables
load_dotenv()

# Create FastAPI app
app = FastAPI(
    title="Debate Service API",
    description="WebSocket API for real-time debate notifications",
    version="1.0.0"
)

# Initialize orchestrator and debate store on startup
@app.on_event("startup")
async def startup_event():
    """Initialize services on startup"""
    orchestrator = DebateOrchestrator()
    await orchestrator.debate_store.initialize()
    app.state.orchestrator = orchestrator

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:3001"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "debate-service",
        "websocket_connections": manager.get_connection_count()
    }

@app.websocket("/ws")
async def websocket_route(
    websocket: WebSocket,
    debate_id: Optional[str] = Query(None),
    organization_id: Optional[str] = Query(None)
):
    """WebSocket endpoint for real-time updates"""
    await websocket_endpoint(websocket, debate_id, organization_id)

@app.get("/ws/status")
async def websocket_status():
    """Get WebSocket connection status"""
    return {
        "total_connections": manager.get_connection_count(),
        "debate_rooms": list(manager.debate_connections.keys()),
        "organization_rooms": list(manager.org_connections.keys())
    }

@app.get("/ws/debug/connections")
async def debug_connections():
    """Debug endpoint to see all connections (development only)"""
    if os.getenv("LOG_LEVEL") != "DEBUG":
        return {"error": "Debug endpoint only available in DEBUG mode"}
    
    connections = []
    for ws, metadata in manager.connection_metadata.items():
        connections.append({
            "id": metadata.get("id"),
            "debate_id": metadata.get("debate_id"),
            "organization_id": metadata.get("organization_id"),
            "connected_at": metadata.get("connected_at").isoformat() if metadata.get("connected_at") else None
        })
    
    return {
        "connections": connections,
        "count": len(connections)
    }

# MCP endpoints
@app.get("/tools")
async def list_tools():
    """List available MCP tools"""
    return [
        {
            "name": "create_debate",
            "description": "Create a new debate",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "topic": {"type": "string"},
                    "description": {"type": "string"},
                    "participants": {"type": "array"},
                    "rules": {"type": "object"}
                },
                "required": ["name", "topic", "participants"]
            }
        },
        {
            "name": "start_debate",
            "description": "Start a debate",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"}
                },
                "required": ["debate_id"]
            }
        },
        {
            "name": "add_turn",
            "description": "Add a turn to a debate",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "debate_id": {"type": "string"},
                    "participant_name": {"type": "string"},
                    "content": {"type": "string"}
                },
                "required": ["debate_id", "participant_name", "content"]
            }
        }
    ]

@app.post("/tools/{tool_name}")
async def call_tool(tool_name: str, request: dict):
    """Call an MCP tool"""
    from .models import CreateDebateRequest, AddTurnRequest
    
    orchestrator = app.state.orchestrator
    args = request.get("arguments", {})
    
    try:
        if tool_name == "create_debate":
            # Create the debate request
            create_request = CreateDebateRequest(
                org_id="default",  # TODO: Get from header
                name=args["name"],
                topic=args["topic"],
                description=args.get("description", ""),
                participants=args["participants"],
                rules=args.get("rules", {}),
                metadata=args.get("metadata", {})
            )
            
            debate = await orchestrator.create_debate(create_request)
            
            # Notify via WebSocket
            await manager.broadcast_to_organization("default", {
                "type": "debate_created",
                "payload": {
                    "debateId": debate.id,
                    "name": debate.name,
                    "topic": debate.topic
                }
            })
            
            return {"debateId": debate.id, "status": "created"}
            
        elif tool_name == "start_debate":
            debate_id = args["debate_id"]
            debate = await orchestrator.start_debate(debate_id)
            
            # Notify via WebSocket
            await manager.broadcast_to_organization("default", {
                "type": "debate_started",
                "payload": {
                    "debateId": debate.id,
                    "name": debate.name,
                    "topic": debate.topic
                }
            })
            
            return {"debateId": debate.id, "status": "started"}
            
        elif tool_name == "add_turn":
            # Get the debate to find participant ID by name
            debate = await orchestrator.debate_store.get_debate(args["debate_id"])
            if not debate:
                return {"error": f"Debate {args['debate_id']} not found"}, 404
            
            # Find participant by name
            participant = next(
                (p for p in debate.participants if p.name == args["participant_name"]),
                None
            )
            if not participant:
                return {"error": f"Participant {args['participant_name']} not found"}, 404
            
            # Create the add turn request
            add_turn_request = AddTurnRequest(
                debate_id=args["debate_id"],
                participant_id=participant.id,
                content=args["content"],
                turn_type=args.get("turn_type", "argument")
            )
            
            turn = await orchestrator.add_turn(add_turn_request)
            
            # Notify via WebSocket
            await manager.broadcast_to_debate(args["debate_id"], {
                "type": "turn_added",
                "payload": {
                    "debateId": args["debate_id"],
                    "participantName": args["participant_name"],
                    "turnNumber": turn.turn_number
                }
            })
            
            return {"status": "turn_added", "turnNumber": turn.turn_number}
            
        else:
            return {"error": f"Unknown tool: {tool_name}"}, 404
            
    except Exception as e:
        return {"error": str(e)}, 500

@app.get("/resources")
async def read_resource(uri: str):
    """Read MCP resources"""
    orchestrator = app.state.orchestrator
    
    if uri == "debate://debates":
        debates = await orchestrator.list_debates("default")  # TODO: Get org from header
        return {"debates": debates}
    
    return {"error": "Unknown resource"}, 404

def start_api_server():
    """Start the FastAPI server"""
    port = int(os.getenv("DEBATE_SERVICE_WS_PORT", "5013"))
    host = os.getenv("DEBATE_SERVICE_HOST", "0.0.0.0")
    
    uvicorn.run(
        app,
        host=host,
        port=port,
        reload=os.getenv("LOG_LEVEL") == "DEBUG",
        log_level=os.getenv("LOG_LEVEL", "INFO").lower()
    )

if __name__ == "__main__":
    start_api_server()