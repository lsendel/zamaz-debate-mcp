import os
from typing import List, AsyncIterator, Optional, Dict, Any
import anthropic
from anthropic import AsyncAnthropic
import structlog

from ..models import Message, CompletionResponse, ModelInfo, LLMProvider
from .base_provider import BaseLLMProvider

logger = structlog.get_logger()


class ClaudeProvider(BaseLLMProvider):
    """Anthropic Claude provider implementation"""
    
    MODELS = {
        "claude-3-opus-20240229": ModelInfo(
            name="claude-3-opus-20240229",
            provider=LLMProvider.CLAUDE,
            context_window=200000,
            max_output_tokens=4096,
            input_cost_per_1k=0.015,
            output_cost_per_1k=0.075,
            capabilities=["text", "vision", "analysis"]
        ),
        "claude-3-sonnet-20240229": ModelInfo(
            name="claude-3-sonnet-20240229",
            provider=LLMProvider.CLAUDE,
            context_window=200000,
            max_output_tokens=4096,
            input_cost_per_1k=0.003,
            output_cost_per_1k=0.015,
            capabilities=["text", "vision", "analysis"]
        ),
        "claude-3-haiku-20240307": ModelInfo(
            name="claude-3-haiku-20240307",
            provider=LLMProvider.CLAUDE,
            context_window=200000,
            max_output_tokens=4096,
            input_cost_per_1k=0.00025,
            output_cost_per_1k=0.00125,
            capabilities=["text", "vision"]
        ),
        "claude-3-5-sonnet-20241022": ModelInfo(
            name="claude-3-5-sonnet-20241022",
            provider=LLMProvider.CLAUDE,
            context_window=200000,
            max_output_tokens=8192,
            input_cost_per_1k=0.003,
            output_cost_per_1k=0.015,
            capabilities=["text", "vision", "analysis", "enhanced_reasoning"]
        )
    }
    
    def __init__(self, api_key: Optional[str] = None):
        api_key = api_key or os.getenv("ANTHROPIC_API_KEY")
        if not api_key:
            raise ValueError("Anthropic API key not provided")
        
        self.client = AsyncAnthropic(api_key=api_key)
        super().__init__(config=None)
        
    async def complete(
        self,
        messages: List[Message],
        model: str,
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        **kwargs
    ) -> CompletionResponse:
        try:
            # Convert messages to Anthropic format
            anthropic_messages = self._convert_messages(messages)
            
            response = await self.client.messages.create(
                model=model,
                messages=anthropic_messages,
                max_tokens=max_tokens or 1000,
                temperature=temperature or 0.7,
                **kwargs
            )
            
            return CompletionResponse(
                provider=LLMProvider.CLAUDE,
                model=model,
                content=response.content[0].text,
                usage={
                    "input_tokens": response.usage.input_tokens,
                    "output_tokens": response.usage.output_tokens,
                    "total_tokens": response.usage.input_tokens + response.usage.output_tokens
                },
                finish_reason=response.stop_reason,
                metadata={
                    "message_id": response.id,
                    "model_version": response.model
                }
            )
        except Exception as e:
            logger.error("Claude completion failed", error=str(e), model=model)
            raise
    
    async def stream_complete(
        self,
        messages: List[Message],
        model: str,
        max_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        **kwargs
    ) -> AsyncIterator[str]:
        try:
            anthropic_messages = self._convert_messages(messages)
            
            async with self.client.messages.stream(
                model=model,
                messages=anthropic_messages,
                max_tokens=max_tokens or 1000,
                temperature=temperature or 0.7,
                **kwargs
            ) as stream:
                async for text in stream.text_stream:
                    yield text
                    
        except Exception as e:
            logger.error("Claude streaming failed", error=str(e), model=model)
            raise
    
    def estimate_tokens(self, messages: List[Message]) -> int:
        """Estimate tokens using Anthropic's method"""
        # Rough estimation: ~4 characters per token
        total_chars = sum(len(msg.content) for msg in messages)
        return int(total_chars / 4)
    
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        return self.MODELS.get(model)
    
    def _convert_messages(self, messages: List[Message]) -> List[Dict[str, Any]]:
        """Convert messages to Anthropic format"""
        # Extract system message if present
        system_message = None
        user_messages = []
        
        for msg in messages:
            if msg.role == "system":
                if system_message:
                    system_message += "\n\n" + msg.content
                else:
                    system_message = msg.content
            else:
                user_messages.append({
                    "role": msg.role,
                    "content": msg.content
                })
        
        # Anthropic requires alternating user/assistant messages
        # If first message isn't user, prepend a user message
        if user_messages and user_messages[0]["role"] != "user":
            user_messages.insert(0, {
                "role": "user",
                "content": "Continue the conversation."
            })
        
        return user_messages