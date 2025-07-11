import os
from typing import List, AsyncIterator, Optional, Dict, Any
import httpx
import json
import structlog
from tenacity import retry, stop_after_attempt, wait_exponential

from ..models import Message, CompletionResponse, ModelInfo, LLMProvider
from .base_provider import BaseLLMProvider

logger = structlog.get_logger()


class LlamaProvider(BaseLLMProvider):
    """Llama provider implementation (via Ollama or custom endpoint)"""
    
    MODELS = {
        "llama2": ModelInfo(
            name="llama2",
            provider=LLMProvider.LLAMA,
            context_window=4096,
            max_output_tokens=4096,
            input_cost_per_1k=0.0,  # Free for local
            output_cost_per_1k=0.0,
            capabilities=["text"]
        ),
        "llama2:7b": ModelInfo(
            name="llama2:7b",
            provider=LLMProvider.LLAMA,
            context_window=4096,
            max_output_tokens=4096,
            capabilities=["text"]
        ),
        "llama2:13b": ModelInfo(
            name="llama2:13b",
            provider=LLMProvider.LLAMA,
            context_window=4096,
            max_output_tokens=4096,
            capabilities=["text"]
        ),
        "llama2:70b": ModelInfo(
            name="llama2:70b",
            provider=LLMProvider.LLAMA,
            context_window=4096,
            max_output_tokens=4096,
            capabilities=["text"]
        ),
        "codellama": ModelInfo(
            name="codellama",
            provider=LLMProvider.LLAMA,
            context_window=16384,
            max_output_tokens=16384,
            capabilities=["text", "code"]
        ),
        "mistral": ModelInfo(
            name="mistral",
            provider=LLMProvider.LLAMA,
            context_window=8192,
            max_output_tokens=8192,
            capabilities=["text"]
        ),
        "mixtral": ModelInfo(
            name="mixtral",
            provider=LLMProvider.LLAMA,
            context_window=32768,
            max_output_tokens=32768,
            capabilities=["text"]
        )
    }
    
    def __init__(self, endpoint: Optional[str] = None):
        self.endpoint = endpoint or os.getenv("LLAMA_ENDPOINT", "http://localhost:11434")
        self.client = httpx.AsyncClient(timeout=120.0)
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
            # Convert messages to Ollama format
            prompt = self._messages_to_prompt(messages)
            
            # Prepare request
            request_data = {
                "model": model,
                "prompt": prompt,
                "stream": False,
                "options": {
                    "temperature": temperature or 0.7,
                    "num_predict": max_tokens or 1000,
                    "top_p": kwargs.get("top_p", 1.0),
                    "top_k": kwargs.get("top_k", 40),
                    "repeat_penalty": kwargs.get("repeat_penalty", 1.1)
                }
            }
            
            # Add system message if present
            system_message = next((msg.content for msg in messages if msg.role == "system"), None)
            if system_message:
                request_data["system"] = system_message
            
            # Make request
            response = await self._make_request("/api/generate", request_data)
            
            # Parse response
            content = response.get("response", "")
            
            # Calculate token usage
            eval_count = response.get("eval_count", 0)
            prompt_eval_count = response.get("prompt_eval_count", 0)
            
            return CompletionResponse(
                provider=LLMProvider.LLAMA,
                model=model,
                content=content,
                usage={
                    "input_tokens": prompt_eval_count,
                    "output_tokens": eval_count,
                    "total_tokens": prompt_eval_count + eval_count
                },
                finish_reason="stop" if response.get("done") else "length",
                metadata={
                    "model": response.get("model"),
                    "created_at": response.get("created_at"),
                    "total_duration": response.get("total_duration"),
                    "load_duration": response.get("load_duration"),
                    "eval_duration": response.get("eval_duration")
                }
            )
        except Exception as e:
            logger.error("Llama completion failed", error=str(e), model=model)
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
            prompt = self._messages_to_prompt(messages)
            
            request_data = {
                "model": model,
                "prompt": prompt,
                "stream": True,
                "options": {
                    "temperature": temperature or 0.7,
                    "num_predict": max_tokens or 1000,
                    "top_p": kwargs.get("top_p", 1.0),
                    "top_k": kwargs.get("top_k", 40),
                    "repeat_penalty": kwargs.get("repeat_penalty", 1.1)
                }
            }
            
            system_message = next((msg.content for msg in messages if msg.role == "system"), None)
            if system_message:
                request_data["system"] = system_message
            
            # Stream response
            async with self.client.stream(
                "POST",
                f"{self.endpoint}/api/generate",
                json=request_data
            ) as response:
                response.raise_for_status()
                
                async for line in response.aiter_lines():
                    if line:
                        try:
                            chunk = json.loads(line)
                            if "response" in chunk:
                                yield chunk["response"]
                        except json.JSONDecodeError:
                            logger.warning("Failed to parse streaming chunk", line=line)
                            
        except Exception as e:
            logger.error("Llama streaming failed", error=str(e), model=model)
            raise
    
    def estimate_tokens(self, messages: List[Message]) -> int:
        """Estimate tokens using character count"""
        # Rough estimation for Llama models
        total_chars = sum(len(msg.content) for msg in messages)
        return int(total_chars / 4)
    
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        # Check if it's a known model
        if model in self.MODELS:
            return self.MODELS[model]
        
        # For custom models, return generic info
        return ModelInfo(
            name=model,
            provider=LLMProvider.LLAMA,
            context_window=4096,  # Conservative default
            max_output_tokens=4096,
            capabilities=["text"]
        )
    
    async def list_available_models(self) -> List[str]:
        """List models available in Ollama"""
        try:
            response = await self.client.get(f"{self.endpoint}/api/tags")
            response.raise_for_status()
            data = response.json()
            return [model["name"] for model in data.get("models", [])]
        except Exception as e:
            logger.error("Failed to list Ollama models", error=str(e))
            return list(self.MODELS.keys())
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10)
    )
    async def _make_request(self, path: str, data: Dict[str, Any]) -> Dict[str, Any]:
        """Make request to Ollama API with retries"""
        response = await self.client.post(
            f"{self.endpoint}{path}",
            json=data
        )
        response.raise_for_status()
        return response.json()
    
    def _messages_to_prompt(self, messages: List[Message]) -> str:
        """Convert messages to a single prompt string"""
        # Skip system messages as they're handled separately
        prompt_parts = []
        
        for msg in messages:
            if msg.role == "system":
                continue
            elif msg.role == "user":
                prompt_parts.append(f"User: {msg.content}")
            elif msg.role == "assistant":
                prompt_parts.append(f"Assistant: {msg.content}")
        
        # Add final "Assistant:" to prompt completion
        prompt = "\n\n".join(prompt_parts)
        if not prompt.endswith("Assistant:"):
            prompt += "\n\nAssistant:"
        
        return prompt
    
    async def pull_model(self, model: str) -> Dict[str, Any]:
        """Pull a model from Ollama registry"""
        try:
            response = await self.client.post(
                f"{self.endpoint}/api/pull",
                json={"name": model}
            )
            response.raise_for_status()
            return response.json()
        except Exception as e:
            logger.error("Failed to pull model", model=model, error=str(e))
            raise
    
    async def __aenter__(self):
        """Async context manager entry"""
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit - close HTTP client"""
        await self.client.aclose()