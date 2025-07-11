import os
from typing import List, Dict, Optional
import structlog
from sentence_transformers import SentenceTransformer
import openai
import numpy as np
from tenacity import retry, stop_after_attempt, wait_exponential
import asyncio
from concurrent.futures import ThreadPoolExecutor

from ..models import EmbeddingModel

logger = structlog.get_logger()


class EmbeddingManager:
    """Manages embedding generation for different models"""
    
    def __init__(self):
        self.loaded_models: Dict[str, SentenceTransformer] = {}
        self.executor = ThreadPoolExecutor(max_workers=2)
        
        # Set OpenAI API key if available
        self.openai_api_key = os.getenv("OPENAI_API_KEY")
        if self.openai_api_key:
            openai.api_key = self.openai_api_key
    
    async def initialize(self):
        """Initialize embedding models"""
        logger.info("Initializing embedding manager")
        
        # Load default model
        await self.load_model(EmbeddingModel.SENTENCE_TRANSFORMER)
    
    async def load_model(self, model: EmbeddingModel):
        """Load a specific embedding model"""
        if model.value in self.loaded_models:
            return
        
        if self._is_sentence_transformer(model):
            logger.info("Loading sentence transformer model", model=model.value)
            
            # Map to actual model names
            model_map = {
                "all-MiniLM-L6-v2": "sentence-transformers/all-MiniLM-L6-v2",
                "all-mpnet-base-v2": "sentence-transformers/all-mpnet-base-v2",
                "e5-small-v2": "intfloat/e5-small-v2",
                "e5-base-v2": "intfloat/e5-base-v2",
                "e5-large-v2": "intfloat/e5-large-v2"
            }
            
            model_name = model_map.get(model.value, model.value)
            
            # Load in executor to avoid blocking
            loop = asyncio.get_event_loop()
            self.loaded_models[model.value] = await loop.run_in_executor(
                self.executor,
                lambda: SentenceTransformer(model_name)
            )
            
            logger.info("Model loaded", model=model.value)
    
    async def generate_embedding(self, text: str, model: EmbeddingModel) -> List[float]:
        """Generate embedding for a single text"""
        embeddings = await self.generate_embeddings([text], model)
        return embeddings[0]
    
    async def generate_embeddings(self, texts: List[str], 
                                model: EmbeddingModel) -> List[List[float]]:
        """Generate embeddings for multiple texts"""
        if not texts:
            return []
        
        if self._is_openai_model(model):
            return await self._generate_openai_embeddings(texts, model)
        else:
            return await self._generate_st_embeddings(texts, model)
    
    def _is_sentence_transformer(self, model: EmbeddingModel) -> bool:
        """Check if model is a sentence transformer"""
        return model.value not in ["text-embedding-ada-002", "text-embedding-3-small", "text-embedding-3-large"]
    
    def _is_openai_model(self, model: EmbeddingModel) -> bool:
        """Check if model is an OpenAI model"""
        return model.value in ["text-embedding-ada-002", "text-embedding-3-small", "text-embedding-3-large"]
    
    async def _generate_st_embeddings(self, texts: List[str], 
                                    model: EmbeddingModel) -> List[List[float]]:
        """Generate embeddings using sentence transformers"""
        # Load model if needed
        await self.load_model(model)
        
        st_model = self.loaded_models[model.value]
        
        # Handle E5 models special formatting
        if "e5" in model.value:
            texts = [f"query: {text}" for text in texts]
        
        # Generate embeddings in executor
        loop = asyncio.get_event_loop()
        embeddings = await loop.run_in_executor(
            self.executor,
            lambda: st_model.encode(texts, show_progress_bar=False)
        )
        
        # Convert to list format
        return embeddings.tolist()
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10)
    )
    async def _generate_openai_embeddings(self, texts: List[str], 
                                        model: EmbeddingModel) -> List[List[float]]:
        """Generate embeddings using OpenAI API"""
        if not self.openai_api_key:
            raise ValueError("OpenAI API key not configured")
        
        # OpenAI has a limit on batch size
        batch_size = 100
        all_embeddings = []
        
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            
            try:
                response = await asyncio.to_thread(
                    openai.Embedding.create,
                    input=batch,
                    model=model.value
                )
                
                embeddings = [item['embedding'] for item in response['data']]
                all_embeddings.extend(embeddings)
                
            except Exception as e:
                logger.error("OpenAI embedding generation failed", 
                           error=str(e), 
                           model=model.value)
                raise
        
        return all_embeddings
    
    def get_embedding_size(self, model: EmbeddingModel) -> int:
        """Get the embedding dimension for a model"""
        sizes = {
            EmbeddingModel.OPENAI_ADA: 1536,
            EmbeddingModel.OPENAI_3_SMALL: 1536,
            EmbeddingModel.OPENAI_3_LARGE: 3072,
            EmbeddingModel.SENTENCE_TRANSFORMER: 384,
            EmbeddingModel.SENTENCE_TRANSFORMER_LARGE: 768,
            EmbeddingModel.E5_SMALL: 384,
            EmbeddingModel.E5_BASE: 768,
            EmbeddingModel.E5_LARGE: 1024
        }
        return sizes.get(model, 768)