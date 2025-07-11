from abc import ABC, abstractmethod
from typing import List, AsyncIterator, Optional, Dict, Any
import structlog
from tenacity import retry, stop_after_attempt, wait_exponential

from ..models import Message, CompletionResponse, ModelInfo, ProviderConfig, ErrorResponse

logger = structlog.get_logger()


class BaseLLMProvider(ABC):
    """Abstract base class for all LLM providers"""
    
    def __init__(self, config: ProviderConfig):
        self.config = config
        self.logger = logger.bind(provider=config.provider)
        
    @abstractmethod
    async def complete(
        self,
        messages: List[Message],
        model: str,
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        **kwargs
    ) -> CompletionResponse:
        """Generate a completion from the LLM"""
        pass
    
    @abstractmethod
    async def stream_complete(
        self,
        messages: List[Message],
        model: str,
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        **kwargs
    ) -> AsyncIterator[str]:
        """Stream a completion from the LLM"""
        pass
    
    @abstractmethod
    def estimate_tokens(self, messages: List[Message]) -> int:
        """Estimate token count for messages"""
        pass
    
    @abstractmethod
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        """Get information about a specific model"""
        pass
    
    def validate_model(self, model: str) -> bool:
        """Check if model is supported by this provider"""
        return any(m.name == model for m in self.config.models)
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10)
    )
    async def _retry_request(self, func, *args, **kwargs):
        """Retry logic for API requests"""
        try:
            return await func(*args, **kwargs)
        except Exception as e:
            self.logger.error("Request failed", error=str(e))
            raise
    
    def _format_messages(self, messages: List[Message]) -> List[Dict[str, Any]]:
        """Convert messages to provider-specific format"""
        return [
            {
                "role": msg.role,
                "content": msg.content,
                **({"name": msg.name} if msg.name else {})
            }
            for msg in messages
        ]