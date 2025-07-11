from typing import List, Dict, Any, Optional, Literal
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum
import uuid


class DocumentType(str, Enum):
    PDF = "pdf"
    DOCX = "docx"
    TXT = "txt"
    MD = "markdown"
    HTML = "html"
    JSON = "json"
    CSV = "csv"


class ChunkingStrategy(str, Enum):
    FIXED_SIZE = "fixed_size"
    SENTENCE = "sentence"
    PARAGRAPH = "paragraph"
    SEMANTIC = "semantic"
    SLIDING_WINDOW = "sliding_window"


class EmbeddingModel(str, Enum):
    OPENAI_ADA = "text-embedding-ada-002"
    OPENAI_3_SMALL = "text-embedding-3-small"
    OPENAI_3_LARGE = "text-embedding-3-large"
    SENTENCE_TRANSFORMER = "all-MiniLM-L6-v2"
    SENTENCE_TRANSFORMER_LARGE = "all-mpnet-base-v2"
    E5_SMALL = "e5-small-v2"
    E5_BASE = "e5-base-v2"
    E5_LARGE = "e5-large-v2"


class KnowledgeBase(BaseModel):
    id: str = Field(default_factory=lambda: f"kb-{uuid.uuid4().hex[:12]}")
    org_id: str
    name: str
    description: Optional[str] = None
    embedding_model: EmbeddingModel = EmbeddingModel.SENTENCE_TRANSFORMER
    chunking_strategy: ChunkingStrategy = ChunkingStrategy.SLIDING_WINDOW
    chunk_size: int = 512
    chunk_overlap: int = 50
    metadata: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    document_count: int = 0
    total_chunks: int = 0


class Document(BaseModel):
    id: str = Field(default_factory=lambda: f"doc-{uuid.uuid4().hex[:12]}")
    kb_id: str
    org_id: str
    name: str
    source_type: DocumentType
    source_url: Optional[str] = None
    content: str
    metadata: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    chunk_count: int = 0
    
    
class DocumentChunk(BaseModel):
    id: str = Field(default_factory=lambda: f"chunk-{uuid.uuid4().hex[:12]}")
    document_id: str
    kb_id: str
    chunk_index: int
    content: str
    embedding: Optional[List[float]] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)
    char_start: int
    char_end: int
    created_at: datetime = Field(default_factory=datetime.utcnow)


class SearchResult(BaseModel):
    chunk_id: str
    document_id: str
    document_name: str
    content: str
    score: float
    metadata: Dict[str, Any] = Field(default_factory=dict)
    highlights: List[str] = Field(default_factory=list)


class CreateKnowledgeBaseRequest(BaseModel):
    org_id: str
    name: str
    description: Optional[str] = None
    embedding_model: EmbeddingModel = EmbeddingModel.SENTENCE_TRANSFORMER
    chunking_strategy: ChunkingStrategy = ChunkingStrategy.SLIDING_WINDOW
    chunk_size: int = 512
    chunk_overlap: int = 50
    metadata: Dict[str, Any] = Field(default_factory=dict)


class IngestDocumentRequest(BaseModel):
    kb_id: str
    name: str
    content: Optional[str] = None
    file_path: Optional[str] = None
    url: Optional[str] = None
    document_type: Optional[DocumentType] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class SearchRequest(BaseModel):
    kb_id: str
    query: str
    max_results: int = 5
    min_score: float = 0.5
    filter_metadata: Optional[Dict[str, Any]] = None
    rerank: bool = False
    include_metadata: bool = True


class AugmentContextRequest(BaseModel):
    context_id: str
    kb_id: str
    query: str
    max_chunks: int = 3
    insertion_strategy: Literal["prepend", "append", "interleave"] = "prepend"


class IndexStats(BaseModel):
    kb_id: str
    total_documents: int
    total_chunks: int
    total_embeddings: int
    index_size_mb: float
    last_updated: datetime
    embedding_dimensions: int