from typing import Dict, Type, Optional
import structlog

from ..models import LLMProvider
from .base_provider import BaseLLMProvider
from .claude_provider import ClaudeProvider
from .openai_provider import OpenAIProvider
from .gemini_provider import GeminiProvider
from .llama_provider import LlamaProvider

logger = structlog.get_logger()


class ProviderFactory:
    """Factory for creating LLM provider instances"""
    
    _providers: Dict[LLMProvider, Type[BaseLLMProvider]] = {
        LLMProvider.CLAUDE: ClaudeProvider,
        LLMProvider.OPENAI: OpenAIProvider,
        LLMProvider.GEMINI: GeminiProvider,
        LLMProvider.LLAMA: LlamaProvider
    }
    
    _instances: Dict[LLMProvider, BaseLLMProvider] = {}
    
    @classmethod
    def get_provider(cls, provider: LLMProvider) -> BaseLLMProvider:
        """Get or create a provider instance"""
        if provider not in cls._instances:
            if provider not in cls._providers:
                raise ValueError(f"Unknown provider: {provider}")
            
            logger.info("Creating provider instance", provider=provider)
            provider_class = cls._providers[provider]
            
            try:
                cls._instances[provider] = provider_class()
            except Exception as e:
                logger.error("Failed to create provider", provider=provider, error=str(e))
                raise
        
        return cls._instances[provider]
    
    @classmethod
    def register_provider(cls, provider: LLMProvider, provider_class: Type[BaseLLMProvider]):
        """Register a custom provider"""
        cls._providers[provider] = provider_class
        logger.info("Registered custom provider", provider=provider)
    
    @classmethod
    def list_providers(cls) -> list[str]:
        """List available providers"""
        return [p.value for p in cls._providers.keys()]
    
    @classmethod
    def get_all_models(cls) -> Dict[str, list[str]]:
        """Get all available models grouped by provider"""
        models = {}
        
        for provider in LLMProvider:
            try:
                provider_instance = cls.get_provider(provider)
                provider_models = list(provider_instance.MODELS.keys())
                models[provider.value] = provider_models
            except Exception as e:
                logger.warning("Failed to get models for provider", provider=provider, error=str(e))
                models[provider.value] = []
        
        return models
    
    @classmethod
    def clear_cache(cls):
        """Clear provider instance cache"""
        cls._instances.clear()
        logger.info("Cleared provider cache")