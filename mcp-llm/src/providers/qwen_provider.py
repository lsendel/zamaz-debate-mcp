"""
Qwen (Alibaba Cloud) LLM Provider Implementation
"""

import os
import httpx
import structlog
from typing import Dict, List, AsyncGenerator, Optional, Any

from ..models import CompletionRequest, CompletionResponse, Message, ModelInfo, LLMProvider
from .base_provider import BaseLLMProvider


logger = structlog.get_logger()


class QwenProvider(BaseLLMProvider):
    """Provider for Qwen (Alibaba) models"""
    
    # Available Qwen models
    MODELS = {
        "qwen2.5-72b-instruct": ModelInfo(
            name="qwen2.5-72b-instruct",
            provider=LLMProvider.QWEN,
            context_window=131072,  # 128K context
            max_output_tokens=8192,
            input_cost_per_1k=0.6,
            output_cost_per_1k=1.8,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "multilingual", "math"]
        ),
        "qwen2.5-32b-instruct": ModelInfo(
            name="qwen2.5-32b-instruct",
            provider=LLMProvider.QWEN,
            context_window=131072,
            max_output_tokens=8192,
            input_cost_per_1k=0.4,
            output_cost_per_1k=1.2,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "multilingual", "math"]
        ),
        "qwen2.5-14b-instruct": ModelInfo(
            name="qwen2.5-14b-instruct",
            provider=LLMProvider.QWEN,
            context_window=131072,
            max_output_tokens=8192,
            input_cost_per_1k=0.2,
            output_cost_per_1k=0.6,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "multilingual", "math"]
        ),
        "qwen2.5-7b-instruct": ModelInfo(
            name="qwen2.5-7b-instruct",
            provider=LLMProvider.QWEN,
            context_window=131072,
            max_output_tokens=8192,
            input_cost_per_1k=0.1,
            output_cost_per_1k=0.3,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "multilingual", "math"]
        ),
        "qwen2-vl-72b-instruct": ModelInfo(
            name="qwen2-vl-72b-instruct",
            provider=LLMProvider.QWEN,
            context_window=32768,
            max_output_tokens=8192,
            input_cost_per_1k=0.8,
            output_cost_per_1k=2.4,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "vision", "multilingual"]
        ),
        "qwen-max": ModelInfo(
            name="qwen-max",
            provider=LLMProvider.QWEN,
            context_window=131072,
            max_output_tokens=8192,
            input_cost_per_1k=2.0,
            output_cost_per_1k=6.0,
            supports_streaming=True,
            supports_functions=True,
            capabilities=["reasoning", "coding", "multilingual", "math", "planning"]
        )
    }
    
    def __init__(self):
        super().__init__()
        self.api_key = os.getenv("QWEN_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
        self.base_url = os.getenv("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/api/v1")
        
        if not self.api_key:
            logger.warning("QWEN_API_KEY or DASHSCOPE_API_KEY not found in environment variables")
    
    def _validate_request(self, request: CompletionRequest) -> None:
        """Validate the completion request"""
        super()._validate_request(request)
        
        if request.model not in self.MODELS:
            available_models = list(self.MODELS.keys())
            raise ValueError(f"Model {request.model} not supported. Available models: {available_models}")
    
    def _prepare_messages(self, messages: List[Message]) -> List[Dict[str, Any]]:
        """Convert messages to Qwen API format"""
        qwen_messages = []
        
        for message in messages:
            qwen_message = {
                "role": message.role,
                "content": message.content
            }
            
            # Qwen uses specific role mappings
            if message.role == "assistant":
                qwen_message["role"] = "assistant"
            elif message.role == "system":
                qwen_message["role"] = "system"
            else:
                qwen_message["role"] = "user"
            
            qwen_messages.append(qwen_message)
        
        return qwen_messages
    
    async def complete(self, request: CompletionRequest) -> CompletionResponse:
        """Generate completion using Qwen API"""
        self._validate_request(request)
        
        if not self.api_key:
            raise ValueError("Qwen API key not configured")
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        # Prepare request payload
        payload = {
            "model": request.model,
            "input": {
                "messages": self._prepare_messages(request.messages)
            },
            "parameters": {
                "max_tokens": request.max_tokens,
                "temperature": request.temperature,
                "top_p": request.top_p,
                "result_format": "message"
            }
        }
        
        if request.stop_sequences:
            payload["parameters"]["stop"] = request.stop_sequences
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                logger.info("Making Qwen API request", model=request.model)
                
                response = await client.post(
                    f"{self.base_url}/services/aigc/text-generation/generation",
                    headers=headers,
                    json=payload
                )
                response.raise_for_status()
                
                result = response.json()
                
                # Check for API errors
                if "error" in result:
                    error_msg = result["error"].get("message", "Unknown error")
                    raise Exception(f"Qwen API error: {error_msg}")
                
                # Extract response content
                output = result.get("output", {})
                content = output.get("choices", [{}])[0].get("message", {}).get("content", "")
                usage = result.get("usage", {})
                finish_reason = output.get("choices", [{}])[0].get("finish_reason")
                
                logger.info("Qwen API request completed", 
                          model=request.model, 
                          input_tokens=usage.get("input_tokens", 0),
                          output_tokens=usage.get("output_tokens", 0))
                
                return CompletionResponse(
                    provider=LLMProvider.QWEN,
                    model=request.model,
                    content=content,
                    usage={
                        "prompt_tokens": usage.get("input_tokens", 0),
                        "completion_tokens": usage.get("output_tokens", 0),
                        "total_tokens": usage.get("total_tokens", 0)
                    },
                    finish_reason=finish_reason,
                    metadata={
                        "provider_response": result,
                        "request_id": result.get("request_id"),
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
            
            logger.error("Qwen API error", error=error_detail, status_code=e.response.status_code)
            raise Exception(f"Qwen API error: {error_detail}")
            
        except Exception as e:
            logger.error("Unexpected error calling Qwen API", error=str(e))
            raise Exception(f"Failed to call Qwen API: {str(e)}")
    
    async def stream_complete(self, request: CompletionRequest) -> AsyncGenerator[str, None]:
        """Generate streaming completion using Qwen API"""
        self._validate_request(request)
        
        if not self.api_key:
            raise ValueError("Qwen API key not configured")
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        # Prepare request payload for streaming
        payload = {
            "model": request.model,
            "input": {
                "messages": self._prepare_messages(request.messages)
            },
            "parameters": {
                "max_tokens": request.max_tokens,
                "temperature": request.temperature,
                "top_p": request.top_p,
                "result_format": "message",
                "incremental_output": True
            }
        }
        
        if request.stop_sequences:
            payload["parameters"]["stop"] = request.stop_sequences
        
        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                logger.info("Making Qwen streaming API request", model=request.model)
                
                async with client.stream(
                    "POST",
                    f"{self.base_url}/services/aigc/text-generation/generation",
                    headers=headers,
                    json=payload
                ) as response:
                    response.raise_for_status()
                    
                    async for line in response.aiter_lines():
                        if line.startswith("data: "):
                            data = line[6:]
                            
                            try:
                                import json
                                chunk = json.loads(data)
                                
                                if "output" in chunk:
                                    output = chunk["output"]
                                    choices = output.get("choices", [])
                                    if choices:
                                        message = choices[0].get("message", {})
                                        content = message.get("content", "")
                                        if content:
                                            yield content
                                            
                            except json.JSONDecodeError:
                                continue
                                
        except httpx.HTTPStatusError as e:
            error_detail = "Unknown error"
            try:
                error_response = e.response.json()
                error_detail = error_response.get("error", {}).get("message", str(e))
            except:
                error_detail = str(e)
            
            logger.error("Qwen streaming API error", error=error_detail, status_code=e.response.status_code)
            raise Exception(f"Qwen streaming API error: {error_detail}")
            
        except Exception as e:
            logger.error("Unexpected error calling Qwen streaming API", error=str(e))
            raise Exception(f"Failed to call Qwen streaming API: {str(e)}")
    
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        """Get information about a specific model"""
        return self.MODELS.get(model)
    
    def list_models(self) -> List[str]:
        """List available models"""
        return list(self.MODELS.keys())