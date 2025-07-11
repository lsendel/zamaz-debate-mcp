import os
from typing import List, Optional, Dict, Any
from datetime import datetime
import structlog
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct, Filter, FieldCondition, Range
import uuid
from sentence_transformers import SentenceTransformer
import hashlib

from ..models import (
    KnowledgeBase, Document, DocumentChunk, SearchResult,
    CreateKnowledgeBaseRequest, IngestDocumentRequest,
    SearchRequest, AugmentContextRequest, IndexStats
)
from .document_processor import DocumentProcessor
from .embedding_manager import EmbeddingManager
from ..clients.context_client import ContextServiceClient

logger = structlog.get_logger()


class RAGManager:
    """Manages RAG operations including document ingestion, embedding, and search"""
    
    def __init__(self):
        # Initialize Qdrant client
        qdrant_url = os.getenv("QDRANT_URL", "http://localhost:6333")
        self.qdrant = QdrantClient(url=qdrant_url)
        
        # Initialize processors
        self.doc_processor = DocumentProcessor()
        self.embedding_manager = EmbeddingManager()
        
        # Initialize context client
        self.context_client = ContextServiceClient(
            os.getenv("CONTEXT_SERVICE_URL", "http://localhost:5001")
        )
        
        # Knowledge base cache
        self.kb_cache: Dict[str, KnowledgeBase] = {}
        
    async def initialize(self):
        """Initialize the RAG system"""
        logger.info("Initializing RAG system")
        
        # Load embedding models
        await self.embedding_manager.initialize()
        
        # Verify Qdrant connection
        try:
            collections = self.qdrant.get_collections()
            logger.info("Connected to Qdrant", collections=len(collections.collections))
        except Exception as e:
            logger.error("Failed to connect to Qdrant", error=str(e))
            raise
    
    async def create_knowledge_base(self, request: CreateKnowledgeBaseRequest) -> KnowledgeBase:
        """Create a new knowledge base"""
        logger.info("Creating knowledge base", name=request.name)
        
        # Create KB object
        kb = KnowledgeBase(
            org_id=request.org_id,
            name=request.name,
            description=request.description,
            embedding_model=request.embedding_model,
            chunking_strategy=request.chunking_strategy,
            chunk_size=request.chunk_size,
            chunk_overlap=request.chunk_overlap,
            metadata=request.metadata
        )
        
        # Create Qdrant collection
        collection_name = self._get_collection_name(kb.id)
        embedding_size = self.embedding_manager.get_embedding_size(kb.embedding_model)
        
        self.qdrant.create_collection(
            collection_name=collection_name,
            vectors_config=VectorParams(
                size=embedding_size,
                distance=Distance.COSINE
            )
        )
        
        # Cache KB
        self.kb_cache[kb.id] = kb
        
        # TODO: Persist KB metadata to database
        
        logger.info("Knowledge base created", kb_id=kb.id)
        return kb
    
    async def ingest_document(self, request: IngestDocumentRequest) -> Document:
        """Ingest a document into a knowledge base"""
        logger.info("Ingesting document", kb_id=request.kb_id, name=request.name)
        
        # Get knowledge base
        kb = await self._get_knowledge_base(request.kb_id)
        if not kb:
            raise ValueError(f"Knowledge base {request.kb_id} not found")
        
        # Process document
        content = await self.doc_processor.process_document(request)
        
        # Create document
        document = Document(
            kb_id=kb.id,
            org_id=kb.org_id,
            name=request.name,
            source_type=request.document_type or "txt",
            source_url=request.url,
            content=content,
            metadata=request.metadata
        )
        
        # Chunk document
        chunks = await self.doc_processor.chunk_document(
            document,
            strategy=kb.chunking_strategy,
            chunk_size=kb.chunk_size,
            chunk_overlap=kb.chunk_overlap
        )
        
        # Generate embeddings
        embeddings = await self.embedding_manager.generate_embeddings(
            [chunk.content for chunk in chunks],
            model=kb.embedding_model
        )
        
        # Store in Qdrant
        collection_name = self._get_collection_name(kb.id)
        points = []
        
        for i, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
            chunk.embedding = embedding
            point = PointStruct(
                id=chunk.id,
                vector=embedding,
                payload={
                    "document_id": document.id,
                    "document_name": document.name,
                    "chunk_index": i,
                    "content": chunk.content,
                    "metadata": {
                        **document.metadata,
                        **chunk.metadata,
                        "char_start": chunk.char_start,
                        "char_end": chunk.char_end
                    }
                }
            )
            points.append(point)
        
        self.qdrant.upsert(
            collection_name=collection_name,
            points=points
        )
        
        # Update document and KB stats
        document.chunk_count = len(chunks)
        kb.document_count += 1
        kb.total_chunks += len(chunks)
        
        # TODO: Persist document metadata
        
        logger.info("Document ingested", 
                   document_id=document.id, 
                   chunks=len(chunks))
        return document
    
    async def search(self, request: SearchRequest) -> List[SearchResult]:
        """Search for relevant chunks in a knowledge base"""
        logger.info("Searching", kb_id=request.kb_id, query=request.query)
        
        # Get knowledge base
        kb = await self._get_knowledge_base(request.kb_id)
        if not kb:
            raise ValueError(f"Knowledge base {request.kb_id} not found")
        
        # Generate query embedding
        query_embedding = await self.embedding_manager.generate_embedding(
            request.query,
            model=kb.embedding_model
        )
        
        # Build filter if metadata provided
        filter_conditions = None
        if request.filter_metadata:
            conditions = []
            for key, value in request.filter_metadata.items():
                conditions.append(
                    FieldCondition(
                        key=f"metadata.{key}",
                        match={"value": value}
                    )
                )
            if conditions:
                filter_conditions = Filter(must=conditions)
        
        # Search in Qdrant
        collection_name = self._get_collection_name(kb.id)
        search_results = self.qdrant.search(
            collection_name=collection_name,
            query_vector=query_embedding,
            limit=request.max_results,
            score_threshold=request.min_score,
            query_filter=filter_conditions
        )
        
        # Convert to SearchResult objects
        results = []
        for hit in search_results:
            result = SearchResult(
                chunk_id=hit.id,
                document_id=hit.payload["document_id"],
                document_name=hit.payload["document_name"],
                content=hit.payload["content"],
                score=hit.score,
                metadata=hit.payload.get("metadata", {})
            )
            results.append(result)
        
        # Rerank if requested
        if request.rerank and results:
            results = await self._rerank_results(results, request.query)
        
        logger.info("Search completed", results=len(results))
        return results
    
    async def augment_context(self, request: AugmentContextRequest) -> Dict[str, Any]:
        """Augment a context with RAG results"""
        logger.info("Augmenting context", 
                   context_id=request.context_id,
                   kb_id=request.kb_id)
        
        # Search for relevant chunks
        search_request = SearchRequest(
            kb_id=request.kb_id,
            query=request.query,
            max_results=request.max_chunks
        )
        search_results = await self.search(search_request)
        
        if not search_results:
            return {"augmented": False, "reason": "No relevant results found"}
        
        # Format results for context
        rag_content = self._format_rag_results(search_results)
        
        # Create message based on insertion strategy
        messages = []
        if request.insertion_strategy == "prepend":
            messages.append({
                "role": "system",
                "content": f"Relevant information from knowledge base:\n\n{rag_content}"
            })
        elif request.insertion_strategy == "append":
            messages.append({
                "role": "system",
                "content": f"Additional context:\n\n{rag_content}"
            })
        else:  # interleave
            # Split results across multiple messages
            for i, result in enumerate(search_results):
                messages.append({
                    "role": "system",
                    "content": f"Reference {i+1}: {result.content}"
                })
        
        # Append to context using context service
        try:
            await self.context_client.append_to_context(
                request.context_id,
                messages
            )
            
            return {
                "augmented": True,
                "chunks_added": len(search_results),
                "total_tokens": len(rag_content.split())  # Rough estimate
            }
        except Exception as e:
            logger.error("Failed to augment context", error=str(e))
            return {
                "augmented": False,
                "reason": str(e)
            }
    
    async def delete_document(self, kb_id: str, document_id: str) -> bool:
        """Delete a document from a knowledge base"""
        logger.info("Deleting document", kb_id=kb_id, document_id=document_id)
        
        collection_name = self._get_collection_name(kb_id)
        
        # Delete all chunks for this document
        self.qdrant.delete(
            collection_name=collection_name,
            points_selector=Filter(
                must=[
                    FieldCondition(
                        key="document_id",
                        match={"value": document_id}
                    )
                ]
            )
        )
        
        # Update KB stats
        kb = await self._get_knowledge_base(kb_id)
        if kb:
            kb.document_count -= 1
            # TODO: Update chunk count properly
        
        return True
    
    async def update_embeddings(self, kb_id: str, 
                              new_model: Optional[str] = None) -> int:
        """Re-generate embeddings for a knowledge base"""
        logger.info("Updating embeddings", kb_id=kb_id, model=new_model)
        
        kb = await self._get_knowledge_base(kb_id)
        if not kb:
            raise ValueError(f"Knowledge base {kb_id} not found")
        
        # Update model if specified
        if new_model:
            kb.embedding_model = new_model
        
        collection_name = self._get_collection_name(kb_id)
        
        # Get all points
        # Note: In production, this should be done in batches
        results = self.qdrant.scroll(
            collection_name=collection_name,
            limit=1000
        )
        
        updated_count = 0
        for batch in results:
            points = batch[0]
            if not points:
                break
            
            # Extract content
            contents = [point.payload["content"] for point in points]
            
            # Generate new embeddings
            embeddings = await self.embedding_manager.generate_embeddings(
                contents,
                model=kb.embedding_model
            )
            
            # Update points
            updated_points = []
            for point, embedding in zip(points, embeddings):
                updated_points.append(
                    PointStruct(
                        id=point.id,
                        vector=embedding,
                        payload=point.payload
                    )
                )
            
            self.qdrant.upsert(
                collection_name=collection_name,
                points=updated_points
            )
            
            updated_count += len(updated_points)
        
        logger.info("Embeddings updated", count=updated_count)
        return updated_count
    
    async def get_system_stats(self) -> Dict[str, Any]:
        """Get system-wide statistics"""
        collections = self.qdrant.get_collections()
        
        total_kbs = len(collections.collections)
        total_vectors = sum(
            coll.vectors_count or 0 
            for coll in collections.collections
        )
        
        return {
            "total_knowledge_bases": total_kbs,
            "total_vectors": total_vectors,
            "embedding_models_loaded": len(self.embedding_manager.loaded_models),
            "qdrant_status": "connected"
        }
    
    async def get_kb_stats(self, kb_id: str) -> Optional[IndexStats]:
        """Get statistics for a knowledge base"""
        kb = await self._get_knowledge_base(kb_id)
        if not kb:
            return None
        
        collection_name = self._get_collection_name(kb_id)
        
        try:
            info = self.qdrant.get_collection(collection_name)
            
            return IndexStats(
                kb_id=kb_id,
                total_documents=kb.document_count,
                total_chunks=kb.total_chunks,
                total_embeddings=info.vectors_count or 0,
                index_size_mb=(info.indexed_vectors_count or 0) * 4 * 
                             self.embedding_manager.get_embedding_size(kb.embedding_model) / 1024 / 1024,
                last_updated=kb.updated_at,
                embedding_dimensions=self.embedding_manager.get_embedding_size(kb.embedding_model)
            )
        except Exception as e:
            logger.error("Failed to get KB stats", kb_id=kb_id, error=str(e))
            return None
    
    async def export_knowledge_base(self, kb_id: str, format: str = "json",
                                  include_embeddings: bool = False) -> Dict[str, Any]:
        """Export a knowledge base"""
        # Simplified export - in production would handle different formats
        kb = await self._get_knowledge_base(kb_id)
        if not kb:
            raise ValueError(f"Knowledge base {kb_id} not found")
        
        collection_name = self._get_collection_name(kb_id)
        
        # Get all points
        all_points = []
        offset = None
        
        while True:
            results, offset = self.qdrant.scroll(
                collection_name=collection_name,
                limit=100,
                offset=offset
            )
            
            if not results:
                break
            
            for point in results:
                export_point = {
                    "id": point.id,
                    "content": point.payload["content"],
                    "metadata": point.payload.get("metadata", {})
                }
                
                if include_embeddings:
                    export_point["embedding"] = point.vector
                
                all_points.append(export_point)
        
        return {
            "knowledge_base": kb.dict(),
            "chunks": all_points,
            "export_date": datetime.utcnow().isoformat()
        }
    
    # Helper methods
    
    def _get_collection_name(self, kb_id: str) -> str:
        """Get Qdrant collection name for a KB"""
        return f"kb_{kb_id}"
    
    async def _get_knowledge_base(self, kb_id: str) -> Optional[KnowledgeBase]:
        """Get knowledge base by ID"""
        # Check cache first
        if kb_id in self.kb_cache:
            return self.kb_cache[kb_id]
        
        # TODO: Load from database
        return None
    
    async def list_knowledge_bases(self) -> List[KnowledgeBase]:
        """List all knowledge bases"""
        # TODO: Load from database
        return list(self.kb_cache.values())
    
    async def get_knowledge_base(self, kb_id: str) -> Optional[KnowledgeBase]:
        """Get a specific knowledge base"""
        return await self._get_knowledge_base(kb_id)
    
    def _format_rag_results(self, results: List[SearchResult]) -> str:
        """Format search results for context insertion"""
        formatted_parts = []
        
        for i, result in enumerate(results):
            source = result.metadata.get("source", result.document_name)
            formatted_parts.append(
                f"[{i+1}] From '{source}' (relevance: {result.score:.2f}):\n{result.content}"
            )
        
        return "\n\n".join(formatted_parts)
    
    async def _rerank_results(self, results: List[SearchResult], 
                            query: str) -> List[SearchResult]:
        """Rerank results using a more sophisticated method"""
        # Simple reranking based on keyword overlap
        # In production, would use a cross-encoder model
        
        query_words = set(query.lower().split())
        
        for result in results:
            content_words = set(result.content.lower().split())
            overlap = len(query_words & content_words)
            # Boost score based on keyword overlap
            result.score = result.score * (1 + overlap * 0.1)
        
        # Sort by new score
        results.sort(key=lambda x: x.score, reverse=True)
        
        return results