import os
from typing import List, AsyncIterator, Optional, Dict, Any
from openai import AsyncOpenAI
import tiktoken
import structlog

from ..models import Message, CompletionResponse, ModelInfo, LLMProvider
from .base_provider import BaseLLMProvider

logger = structlog.get_logger()


class OpenAIProvider(BaseLLMProvider):
    """OpenAI GPT provider implementation"""
    
    MODELS = {
        "gpt-4-turbo-preview": ModelInfo(
            name="gpt-4-turbo-preview",
            provider=LLMProvider.OPENAI,
            context_window=128000,
            max_output_tokens=4096,
            input_cost_per_1k=0.01,
            output_cost_per_1k=0.03,
            capabilities=["text", "vision", "function_calling"]
        ),
        "gpt-4": ModelInfo(
            name="gpt-4",
            provider=LLMProvider.OPENAI,
            context_window=8192,
            max_output_tokens=4096,
            input_cost_per_1k=0.03,
            output_cost_per_1k=0.06,
            capabilities=["text", "function_calling"]
        ),
        "gpt-3.5-turbo": ModelInfo(
            name="gpt-3.5-turbo",
            provider=LLMProvider.OPENAI,
            context_window=16385,
            max_output_tokens=4096,
            input_cost_per_1k=0.0005,
            output_cost_per_1k=0.0015,
            capabilities=["text", "function_calling"]
        ),
        "gpt-4o": ModelInfo(
            name="gpt-4o",
            provider=LLMProvider.OPENAI,
            context_window=128000,
            max_output_tokens=4096,
            input_cost_per_1k=0.005,
            output_cost_per_1k=0.015,
            capabilities=["text", "vision", "function_calling"]
        ),
        "gpt-4o-mini": ModelInfo(
            name="gpt-4o-mini",
            provider=LLMProvider.OPENAI,
            context_window=128000,
            max_output_tokens=16384,
            input_cost_per_1k=0.00015,
            output_cost_per_1k=0.0006,
            capabilities=["text", "vision", "function_calling"]
        )
    }
    
    def __init__(self, api_key: Optional[str] = None, organization: Optional[str] = None):
        api_key = api_key or os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise ValueError("OpenAI API key not provided")
        
        self.client = AsyncOpenAI(
            api_key=api_key,
            organization=organization or os.getenv("OPENAI_ORGANIZATION")
        )
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
            response = await self.client.chat.completions.create(
                model=model,
                messages=self._format_messages(messages),
                max_tokens=max_tokens or 1000,
                temperature=temperature or 0.7,
                **kwargs
            )
            
            choice = response.choices[0]
            
            return CompletionResponse(
                provider=LLMProvider.OPENAI,
                model=model,
                content=choice.message.content or "",
                usage={
                    "input_tokens": response.usage.prompt_tokens,
                    "output_tokens": response.usage.completion_tokens,
                    "total_tokens": response.usage.total_tokens
                },
                finish_reason=choice.finish_reason,
                metadata={
                    "message_id": response.id,
                    "created": response.created,
                    "system_fingerprint": response.system_fingerprint
                }
            )
        except Exception as e:
            logger.error("OpenAI completion failed", error=str(e), model=model)
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
            stream = await self.client.chat.completions.create(
                model=model,
                messages=self._format_messages(messages),
                max_tokens=max_tokens or 1000,
                temperature=temperature or 0.7,
                stream=True,
                **kwargs
            )
            
            async for chunk in stream:
                if chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content
                    
        except Exception as e:
            logger.error("OpenAI streaming failed", error=str(e), model=model)
            raise
    
    def estimate_tokens(self, messages: List[Message]) -> int:
        """Estimate tokens using tiktoken"""
        try:
            # Use cl100k_base encoding for GPT-4 and GPT-3.5-turbo
            encoding = tiktoken.encoding_for_model("gpt-3.5-turbo")
            
            total_tokens = 0
            for message in messages:
                # Each message has ~4 tokens of overhead
                total_tokens += 4
                total_tokens += len(encoding.encode(message.content))
                if message.name:
                    total_tokens += len(encoding.encode(message.name))
            
            # Add 3 tokens for the reply
            total_tokens += 3
            
            return total_tokens
        except Exception as e:
            logger.warning("Token estimation failed, using fallback", error=str(e))
            # Fallback to character-based estimation
            total_chars = sum(len(msg.content) for msg in messages)
            return int(total_chars / 4)
    
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        return self.MODELS.get(model)