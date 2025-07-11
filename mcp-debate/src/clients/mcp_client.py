import httpx
import json
from typing import Any, Dict, List, Optional
import structlog
from tenacity import retry, stop_after_attempt, wait_exponential

logger = structlog.get_logger()


class MCPClient:
    """Client for communicating with other MCP services"""
    
    def __init__(self, service_url: str, timeout: int = 30):
        self.service_url = service_url.rstrip('/')
        self.client = httpx.AsyncClient(timeout=timeout)
        
    async def call_tool(self, tool_name: str, arguments: Dict[str, Any]) -> Any:
        """Call a tool on the MCP service"""
        try:
            response = await self._make_request(
                "POST",
                f"/tools/{tool_name}",
                json={"arguments": arguments}
            )
            return response
        except Exception as e:
            logger.error("MCP tool call failed", 
                        service=self.service_url, 
                        tool=tool_name, 
                        error=str(e))
            raise
    
    async def read_resource(self, resource_uri: str) -> Any:
        """Read a resource from the MCP service"""
        try:
            response = await self._make_request(
                "GET",
                f"/resources",
                params={"uri": resource_uri}
            )
            return response
        except Exception as e:
            logger.error("MCP resource read failed",
                        service=self.service_url,
                        resource=resource_uri,
                        error=str(e))
            raise
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10)
    )
    async def _make_request(self, method: str, path: str, **kwargs) -> Any:
        """Make HTTP request to MCP service with retries"""
        url = f"{self.service_url}{path}"
        response = await self.client.request(method, url, **kwargs)
        response.raise_for_status()
        return response.json()
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.client.aclose()


class ContextServiceClient(MCPClient):
    """Specialized client for the context service"""
    
    async def create_context(self, org_id: str, namespace_id: str, name: str, 
                           initial_messages: List[Dict[str, str]] = None) -> Dict[str, Any]:
        """Create a new context"""
        return await self.call_tool("create_context", {
            "namespace_id": namespace_id,
            "name": name,
            "initial_messages": initial_messages or []
        })
    
    async def append_to_context(self, context_id: str, messages: List[Dict[str, str]]) -> Any:
        """Append messages to context"""
        return await self.call_tool("append_to_context", {
            "context_id": context_id,
            "messages": messages
        })
    
    async def get_context_window(self, context_id: str, max_tokens: int = 8000,
                               strategy: str = "sliding_window") -> Dict[str, Any]:
        """Get optimized context window"""
        result = await self.call_tool("get_context_window", {
            "context_id": context_id,
            "max_tokens": max_tokens,
            "strategy": strategy
        })
        return json.loads(result) if isinstance(result, str) else result


class LLMServiceClient(MCPClient):
    """Specialized client for the LLM service"""
    
    async def complete(self, provider: str, model: str, messages: List[Dict[str, str]],
                      max_tokens: int = 1000, temperature: float = 0.7) -> Dict[str, Any]:
        """Get completion from LLM"""
        result = await self.call_tool("complete", {
            "provider": provider,
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature
        })
        return json.loads(result) if isinstance(result, str) else result
    
    async def estimate_tokens(self, provider: str, messages: List[Dict[str, str]]) -> int:
        """Estimate token count"""
        result = await self.call_tool("estimate_tokens", {
            "provider": provider,
            "messages": messages
        })
        data = json.loads(result) if isinstance(result, str) else result
        return data.get("token_count", 0)


class RAGServiceClient(MCPClient):
    """Specialized client for the RAG service"""
    
    async def search(self, kb_id: str, query: str, max_results: int = 5) -> List[Dict[str, Any]]:
        """Search knowledge base"""
        return await self.call_tool("search", {
            "kb_id": kb_id,
            "query": query,
            "max_results": max_results
        })
    
    async def augment_context(self, context_id: str, kb_id: str, 
                            query: str) -> Dict[str, Any]:
        """Augment context with RAG results"""
        return await self.call_tool("augment_context", {
            "context_id": context_id,
            "kb_id": kb_id,
            "query": query
        })