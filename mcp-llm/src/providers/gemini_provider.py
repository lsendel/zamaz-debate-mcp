import os
from typing import List, AsyncIterator, Optional, Dict, Any
import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold
import structlog
import asyncio
from concurrent.futures import ThreadPoolExecutor

from ..models import Message, CompletionResponse, ModelInfo, LLMProvider
from .base_provider import BaseLLMProvider

logger = structlog.get_logger()


class GeminiProvider(BaseLLMProvider):
    """Google Gemini provider implementation"""
    
    MODELS = {
        "gemini-pro": ModelInfo(
            name="gemini-pro",
            provider=LLMProvider.GEMINI,
            context_window=32768,
            max_output_tokens=8192,
            input_cost_per_1k=0.0005,
            output_cost_per_1k=0.0015,
            capabilities=["text"]
        ),
        "gemini-pro-vision": ModelInfo(
            name="gemini-pro-vision",
            provider=LLMProvider.GEMINI,
            context_window=16384,
            max_output_tokens=4096,
            input_cost_per_1k=0.0005,
            output_cost_per_1k=0.0015,
            capabilities=["text", "vision"]
        ),
        "gemini-1.5-pro": ModelInfo(
            name="gemini-1.5-pro",
            provider=LLMProvider.GEMINI,
            context_window=1048576,  # 1M tokens
            max_output_tokens=8192,
            input_cost_per_1k=0.00125,
            output_cost_per_1k=0.00375,
            capabilities=["text", "vision", "audio", "video"]
        ),
        "gemini-1.5-flash": ModelInfo(
            name="gemini-1.5-flash",
            provider=LLMProvider.GEMINI,
            context_window=1048576,  # 1M tokens
            max_output_tokens=8192,
            input_cost_per_1k=0.00025,
            output_cost_per_1k=0.00075,
            capabilities=["text", "vision", "audio", "video"]
        ),
        "gemini-2.0-flash-exp": ModelInfo(
            name="gemini-2.0-flash-exp",
            provider=LLMProvider.GEMINI,
            context_window=1048576,  # 1M tokens
            max_output_tokens=8192,
            input_cost_per_1k=0.00025,  # Estimated
            output_cost_per_1k=0.00075,  # Estimated
            capabilities=["text", "vision", "audio", "video", "enhanced_reasoning"]
        )
    }
    
    def __init__(self, api_key: Optional[str] = None):
        api_key = api_key or os.getenv("GOOGLE_API_KEY")
        if not api_key:
            raise ValueError("Google API key not provided")
        
        genai.configure(api_key=api_key)
        self.executor = ThreadPoolExecutor(max_workers=1)
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
            # Get the model
            gemini_model = genai.GenerativeModel(model)
            
            # Convert messages to Gemini format
            gemini_messages = self._convert_messages(messages)
            
            # Configure generation settings
            generation_config = genai.types.GenerationConfig(
                max_output_tokens=max_tokens or 1000,
                temperature=temperature or 0.7,
                top_p=kwargs.get("top_p", 1.0),
                top_k=kwargs.get("top_k", 1)
            )
            
            # Safety settings - using least restrictive for debates
            safety_settings = {
                HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_NONE,
                HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_NONE,
                HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_NONE,
                HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_NONE,
            }
            
            # Run in executor since Gemini SDK is not async
            loop = asyncio.get_event_loop()
            response = await loop.run_in_executor(
                self.executor,
                lambda: gemini_model.generate_content(
                    gemini_messages,
                    generation_config=generation_config,
                    safety_settings=safety_settings
                )
            )
            
            # Extract text from response
            text = response.text if hasattr(response, 'text') else ""
            
            # Calculate token usage (approximation for Gemini)
            input_tokens = self.estimate_tokens(messages)
            output_tokens = self.estimate_tokens([Message(role="assistant", content=text)])
            
            return CompletionResponse(
                provider=LLMProvider.GEMINI,
                model=model,
                content=text,
                usage={
                    "input_tokens": input_tokens,
                    "output_tokens": output_tokens,
                    "total_tokens": input_tokens + output_tokens
                },
                finish_reason=self._get_finish_reason(response),
                metadata={
                    "safety_ratings": self._get_safety_ratings(response)
                }
            )
        except Exception as e:
            logger.error("Gemini completion failed", error=str(e), model=model)
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
            gemini_model = genai.GenerativeModel(model)
            gemini_messages = self._convert_messages(messages)
            
            generation_config = genai.types.GenerationConfig(
                max_output_tokens=max_tokens or 1000,
                temperature=temperature or 0.7,
                top_p=kwargs.get("top_p", 1.0),
                top_k=kwargs.get("top_k", 1)
            )
            
            safety_settings = {
                HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_NONE,
                HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_NONE,
                HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_NONE,
                HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_NONE,
            }
            
            # Run streaming in executor
            loop = asyncio.get_event_loop()
            
            def generate_stream():
                return gemini_model.generate_content(
                    gemini_messages,
                    generation_config=generation_config,
                    safety_settings=safety_settings,
                    stream=True
                )
            
            response_stream = await loop.run_in_executor(self.executor, generate_stream)
            
            for chunk in response_stream:
                if hasattr(chunk, 'text') and chunk.text:
                    yield chunk.text
                    
        except Exception as e:
            logger.error("Gemini streaming failed", error=str(e), model=model)
            raise
    
    def estimate_tokens(self, messages: List[Message]) -> int:
        """Estimate tokens using character count"""
        # Gemini doesn't provide direct token counting, use approximation
        total_chars = sum(len(msg.content) for msg in messages)
        return int(total_chars / 4)
    
    def get_model_info(self, model: str) -> Optional[ModelInfo]:
        return self.MODELS.get(model)
    
    def _convert_messages(self, messages: List[Message]) -> List[Dict[str, str]]:
        """Convert messages to Gemini format"""
        # Extract system message for context
        system_context = ""
        conversation = []
        
        for msg in messages:
            if msg.role == "system":
                system_context += msg.content + "\n\n"
            else:
                # Gemini uses "user" and "model" roles
                role = "user" if msg.role == "user" else "model"
                conversation.append({
                    "role": role,
                    "parts": [msg.content]
                })
        
        # If we have system context, prepend it to the first user message
        if system_context and conversation:
            if conversation[0]["role"] == "user":
                conversation[0]["parts"][0] = system_context + conversation[0]["parts"][0]
            else:
                # Insert a user message with the system context
                conversation.insert(0, {
                    "role": "user",
                    "parts": [system_context + "Please acknowledge this context."]
                })
        
        return conversation
    
    def _get_finish_reason(self, response) -> str:
        """Extract finish reason from response"""
        if hasattr(response, 'candidates') and response.candidates:
            candidate = response.candidates[0]
            if hasattr(candidate, 'finish_reason'):
                return str(candidate.finish_reason)
        return "completed"
    
    def _get_safety_ratings(self, response) -> Dict[str, Any]:
        """Extract safety ratings from response"""
        ratings = {}
        if hasattr(response, 'candidates') and response.candidates:
            candidate = response.candidates[0]
            if hasattr(candidate, 'safety_ratings'):
                for rating in candidate.safety_ratings:
                    ratings[rating.category.name] = rating.probability.name
        return ratings