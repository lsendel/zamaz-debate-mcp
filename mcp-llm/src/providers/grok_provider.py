"""
Grok (X.AI) LLM Provider Implementation
"""

import os
import httpx
import structlog
from typing import Dict, List, AsyncGenerator, Optional, Any

from ..models import CompletionRequest, CompletionResponse, Message, ModelInfo, LLMProvider
from .base_provider import BaseLLMProvider


logger = structlog.get_logger()


class GrokProvider(BaseLLMProvider):
    """Provider for Grok (X.AI) models"""
    
    # Available Grok models
    MODELS = {
        "grok-beta": ModelInfo(
            name="grok-beta",
            provider=LLMProvider.GROK,
            context_window=131072,  # 128K context
            max_output_tokens=4096,
            input_cost_per_1k=5.0,  # Estimated pricing
            output_cost_per_1k=15.0,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "creative_writing", "real_time_data"]
        ),
        "grok-vision-beta": ModelInfo(
            name="grok-vision-beta",
            provider=LLMProvider.GROK,
            context_window=131072,
            max_output_tokens=4096,
            input_cost_per_1k=5.0,
            output_cost_per_1k=15.0,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "creative_writing", "vision", "real_time_data"]
        )
    }
    
    def __init__(self):
        super().__init__()
        self.api_key = os.getenv("GROK_API_KEY")
        self.base_url = os.getenv("GROK_BASE_URL", "https://api.x.ai/v1")
        
        if not self.api_key:
            logger.warning("GROK_API_KEY not found in environment variables")
    
    def _validate_request(self, request: CompletionRequest) -> None:
        """Validate the completion request"""
        super()._validate_request(request)
        
        if request.model not in self.MODELS:
            available_models = list(self.MODELS.keys())
            raise ValueError(f"Model {request.model} not supported. Available models: {available_models}")
    
    def _prepare_messages(self, messages: List[Message]) -> List[Dict[str, Any]]:
        """Convert messages to Grok API format"""
        grok_messages = []
        
        for message in messages:
            grok_message = {
                "role": message.role,
                "content": message.content
            }
            
            if message.name:
                grok_message["name"] = message.name
            
            grok_messages.append(grok_message)
        
        return grok_messages
    
    async def complete(self, request: CompletionRequest) -> CompletionResponse:
        """Generate completion using Grok API"""
        self._validate_request(request)
        
        if not self.api_key:
            raise ValueError("Grok API key not configured")
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        # Prepare request payload
        payload = {
            "model": request.model,
            "messages": self._prepare_messages(request.messages),
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
            "top_p": request.top_p,
            "stream": False
        }
        
        if request.stop_sequences:
            payload["stop"] = request.stop_sequences
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                logger.info("Making Grok API request", model=request.model)
                
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=payload
                )
                response.raise_for_status()
                
                result = response.json()
                
                # Extract response content
                content = result["choices"][0]["message"]["content"]
                usage = result.get("usage", {})
                finish_reason = result["choices"][0].get("finish_reason")
                
                logger.info("Grok API request completed", 
                          model=request.model, 
                          input_tokens=usage.get("prompt_tokens", 0),
                          output_tokens=usage.get("completion_tokens", 0))
                
                return CompletionResponse(
                    provider=LLMProvider.GROK,
                    model=request.model,
                    content=content,
                    usage={
                        "prompt_tokens": usage.get("prompt_tokens", 0),
                        "completion_tokens": usage.get("completion_tokens", 0),
                        "total_tokens": usage.get("total_tokens", 0)
                    },
                    finish_reason=finish_reason,
                    metadata={
                        "provider_response": result,
                        "model_version": result.get("model"),
                        "request_id": response.headers.get("x-request-id"),
                        **request.metadata
                    }
                )
                
        except httpx.HTTPStatusError as e:
            error_detail = "Unknown error"
            try:
                error_response = e.response.json()
                error_detail = error_response.get("error", {}).get("message", str(e))
            except:
                error_detail = str(e)
            
            logger.error("Grok API error", error=error_detail, status_code=e.response.status_code)
            raise Exception(f"Grok API error: {error_detail}")
            
        except Exception as e:
            logger.error("Unexpected error calling Grok API", error=str(e))
            raise Exception(f"Failed to call Grok API: {str(e)}")
    
    async def stream_complete(self, request: CompletionRequest) -> AsyncGenerator[str, None]:
        """Generate streaming completion using Grok API"""
        self._validate_request(request)
        
        if not self.api_key:
            raise ValueError("Grok API key not configured")
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        # Prepare request payload for streaming
        payload = {
            "model": request.model,
            "messages": self._prepare_messages(request.messages),
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
            "top_p": request.top_p,
            "stream": True
        }
        
        if request.stop_sequences:
            payload["stop"] = request.stop_sequences
        
        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                logger.info("Making Grok streaming API request", model=request.model)
                
                async with client.stream(
                    "POST",
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=payload
                ) as response:
                    response.raise_for_status()
                    
                    async for line in response.aiter_lines():
                        if line.startswith("data: "):
                            data = line[6:]
                            
                            if data == "[DONE]":
                                break
                            
                            try:
                                import json
                                chunk = json.loads(data)
                                
                                if "choices" in chunk and len(chunk["choices"]) > 0:
                                    delta = chunk["choices"][0].get("delta", {})
                                    if "content" in delta:
                                        yield delta["content"]
                                        
                            except json.JSONDecodeError:
                                continue
                                
        except httpx.HTTPStatusError as e:
            error_detail = "Unknown error"
            try:
                error_response = e.response.json()
                error_detail = error_response.get("error", {}).get("message", str(e))
            except:
                error_detail = str(e)
            
            logger.error("Grok streaming API error", error=error_detail, status_code=e.response.status_code)
            raise Exception(f"Grok streaming API error: {error_detail}")
            
        except Exception as e:
            logger.error("Unexpected error calling Grok streaming API", error=str(e))
            raise Exception(f"Failed to call Grok streaming API: {str(e)}")
    
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        """Get information about a specific model"""
        return self.MODELS.get(model)
    
    def list_models(self) -> List[str]:
        """List available models"""
        return list(self.MODELS.keys())