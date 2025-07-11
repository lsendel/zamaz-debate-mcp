import httpx
from typing import List, Dict, Any
import structlog

logger = structlog.get_logger()


class ContextServiceClient:
    """Client for communicating with the context MCP service"""
    
    def __init__(self, service_url: str):
        self.service_url = service_url.rstrip('/')
        self.client = httpx.AsyncClient(timeout=30.0)
        
    async def append_to_context(self, context_id: str, messages: List[Dict[str, str]]) -> Any:
        """Append messages to a context"""
        # Simplified version - in production would use proper MCP protocol
        try:
            response = await self.client.post(
                f"{self.service_url}/tools/append_to_context",
                json={
                    "arguments": {
                        "context_id": context_id,
                        "messages": messages
                    }
                }
            )
            response.raise_for_status()
            return response.json()
        except Exception as e:
            logger.error("Failed to append to context", error=str(e))
            raise
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.client.aclose()