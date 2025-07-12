import asyncio
import json
import logging
from typing import Dict, Set, Optional
from fastapi import WebSocket, WebSocketDisconnect
from datetime import datetime
import uuid

logger = logging.getLogger(__name__)

class ConnectionManager:
    def __init__(self):
        # Maps debate_id to set of connected websockets
        self.debate_connections: Dict[str, Set[WebSocket]] = {}
        # Maps organization_id to set of connected websockets
        self.org_connections: Dict[str, Set[WebSocket]] = {}
        # Maps websocket to its metadata
        self.connection_metadata: Dict[WebSocket, dict] = {}
        
    async def connect(self, websocket: WebSocket, debate_id: Optional[str] = None, 
                     organization_id: Optional[str] = None):
        """Accept a new WebSocket connection"""
        await websocket.accept()
        
        # Store metadata
        self.connection_metadata[websocket] = {
            'id': str(uuid.uuid4()),
            'debate_id': debate_id,
            'organization_id': organization_id,
            'connected_at': datetime.utcnow()
        }
        
        # Add to appropriate connection pools
        if debate_id:
            if debate_id not in self.debate_connections:
                self.debate_connections[debate_id] = set()
            self.debate_connections[debate_id].add(websocket)
            
        if organization_id:
            if organization_id not in self.org_connections:
                self.org_connections[organization_id] = set()
            self.org_connections[organization_id].add(websocket)
            
        logger.info(f"WebSocket connected: {self.connection_metadata[websocket]}")
        
    def disconnect(self, websocket: WebSocket):
        """Remove a WebSocket connection"""
        metadata = self.connection_metadata.get(websocket, {})
        
        # Remove from debate connections
        if metadata.get('debate_id'):
            debate_id = metadata['debate_id']
            if debate_id in self.debate_connections:
                self.debate_connections[debate_id].discard(websocket)
                if not self.debate_connections[debate_id]:
                    del self.debate_connections[debate_id]
                    
        # Remove from org connections
        if metadata.get('organization_id'):
            org_id = metadata['organization_id']
            if org_id in self.org_connections:
                self.org_connections[org_id].discard(websocket)
                if not self.org_connections[org_id]:
                    del self.org_connections[org_id]
                    
        # Remove metadata
        if websocket in self.connection_metadata:
            del self.connection_metadata[websocket]
            
        logger.info(f"WebSocket disconnected: {metadata}")
        
    async def send_personal_message(self, message: dict, websocket: WebSocket):
        """Send a message to a specific connection"""
        try:
            await websocket.send_json(message)
        except Exception as e:
            logger.error(f"Error sending message to websocket: {e}")
            self.disconnect(websocket)
            
    async def broadcast_to_debate(self, debate_id: str, message: dict):
        """Broadcast a message to all connections watching a specific debate"""
        if debate_id in self.debate_connections:
            # Create tasks for all connections
            tasks = []
            for connection in self.debate_connections[debate_id].copy():
                tasks.append(self.send_personal_message(message, connection))
            
            # Send all messages concurrently
            if tasks:
                await asyncio.gather(*tasks, return_exceptions=True)
                
    async def broadcast_to_organization(self, organization_id: str, message: dict):
        """Broadcast a message to all connections in an organization"""
        if organization_id in self.org_connections:
            tasks = []
            for connection in self.org_connections[organization_id].copy():
                tasks.append(self.send_personal_message(message, connection))
                
            if tasks:
                await asyncio.gather(*tasks, return_exceptions=True)
                
    async def handle_client_message(self, websocket: WebSocket, data: dict):
        """Handle incoming messages from clients"""
        message_type = data.get('type')
        payload = data.get('payload', {})
        
        if message_type == 'subscribe':
            # Update subscription preferences
            debate_id = payload.get('debateId')
            organization_id = payload.get('organizationId')
            
            if debate_id or organization_id:
                # Update metadata
                if websocket in self.connection_metadata:
                    if debate_id:
                        old_debate_id = self.connection_metadata[websocket].get('debate_id')
                        if old_debate_id and old_debate_id != debate_id:
                            # Remove from old debate
                            if old_debate_id in self.debate_connections:
                                self.debate_connections[old_debate_id].discard(websocket)
                                
                        # Add to new debate
                        if debate_id not in self.debate_connections:
                            self.debate_connections[debate_id] = set()
                        self.debate_connections[debate_id].add(websocket)
                        self.connection_metadata[websocket]['debate_id'] = debate_id
                        
                    if organization_id:
                        old_org_id = self.connection_metadata[websocket].get('organization_id')
                        if old_org_id and old_org_id != organization_id:
                            # Remove from old org
                            if old_org_id in self.org_connections:
                                self.org_connections[old_org_id].discard(websocket)
                                
                        # Add to new org
                        if organization_id not in self.org_connections:
                            self.org_connections[organization_id] = set()
                        self.org_connections[organization_id].add(websocket)
                        self.connection_metadata[websocket]['organization_id'] = organization_id
                        
                await self.send_personal_message({
                    'type': 'subscription_confirmed',
                    'payload': {
                        'debateId': debate_id,
                        'organizationId': organization_id
                    },
                    'timestamp': datetime.utcnow().isoformat()
                }, websocket)
                
        elif message_type == 'ping':
            # Respond to ping with pong
            await self.send_personal_message({
                'type': 'pong',
                'timestamp': datetime.utcnow().isoformat()
            }, websocket)
            
    def get_connection_count(self, debate_id: Optional[str] = None, 
                           organization_id: Optional[str] = None) -> int:
        """Get the number of active connections"""
        if debate_id:
            return len(self.debate_connections.get(debate_id, set()))
        elif organization_id:
            return len(self.org_connections.get(organization_id, set()))
        else:
            return len(self.connection_metadata)

# Global connection manager instance
manager = ConnectionManager()

async def websocket_endpoint(websocket: WebSocket, debate_id: Optional[str] = None,
                           organization_id: Optional[str] = None):
    """WebSocket endpoint handler"""
    await manager.connect(websocket, debate_id, organization_id)
    
    try:
        while True:
            # Receive message from client
            data = await websocket.receive_json()
            await manager.handle_client_message(websocket, data)
            
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        logger.info("Client disconnected normally")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        manager.disconnect(websocket)

# Helper functions for sending notifications
async def notify_debate_started(debate_id: str, organization_id: str, topic: str):
    """Notify when a debate starts"""
    message = {
        'type': 'debate_started',
        'payload': {
            'debateId': debate_id,
            'topic': topic
        },
        'timestamp': datetime.utcnow().isoformat()
    }
    
    # Notify both debate and organization subscribers
    await asyncio.gather(
        manager.broadcast_to_debate(debate_id, message),
        manager.broadcast_to_organization(organization_id, message),
        return_exceptions=True
    )

async def notify_turn_added(debate_id: str, organization_id: str, 
                          participant_name: str, turn_number: int):
    """Notify when a turn is added"""
    message = {
        'type': 'turn_added',
        'payload': {
            'debateId': debate_id,
            'participantName': participant_name,
            'turnNumber': turn_number
        },
        'timestamp': datetime.utcnow().isoformat()
    }
    
    await asyncio.gather(
        manager.broadcast_to_debate(debate_id, message),
        manager.broadcast_to_organization(organization_id, message),
        return_exceptions=True
    )

async def notify_debate_completed(debate_id: str, organization_id: str, 
                                topic: str, summary: Optional[str] = None):
    """Notify when a debate completes"""
    message = {
        'type': 'debate_completed',
        'payload': {
            'debateId': debate_id,
            'topic': topic,
            'summary': summary
        },
        'timestamp': datetime.utcnow().isoformat()
    }
    
    await asyncio.gather(
        manager.broadcast_to_debate(debate_id, message),
        manager.broadcast_to_organization(organization_id, message),
        return_exceptions=True
    )