#!/usr/bin/env python3
"""
Advanced Context Manager for Kiro GitHub Integration
Provides intelligent context management with semantic understanding,
context pruning, and relevance scoring.
"""

import asyncio
import hashlib
import json
import logging
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from typing import Any

import aioredis
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ContextType(Enum):
    """Types of context that can be managed"""
    CODE_REVIEW = "code_review"
    ISSUE = "issue"
    DISCUSSION = "discussion"
    COMMIT = "commit"
    DOCUMENTATION = "documentation"
    USER_PREFERENCE = "user_preference"
    PROJECT_HISTORY = "project_history"


class RelevanceLevel(Enum):
    """Relevance levels for context items"""
    CRITICAL = 5
    HIGH = 4
    MEDIUM = 3
    LOW = 2
    MINIMAL = 1


@dataclass
class ContextItem:
    """Individual context item with metadata"""
    id: str
    type: ContextType
    content: str
    embedding: np.ndarray | None = None
    metadata: dict[str, Any] = field(default_factory=dict)
    created_at: datetime = field(default_factory=datetime.utcnow)
    accessed_at: datetime = field(default_factory=datetime.utcnow)
    access_count: int = 0
    relevance_score: float = 0.0
    token_count: int = 0

    def to_dict(self) -> dict[str, Any]:
        """Convert to dictionary for storage"""
        return {
            "id": self.id,
            "type": self.type.value,
            "content": self.content,
            "metadata": self.metadata,
            "created_at": self.created_at.isoformat(),
            "accessed_at": self.accessed_at.isoformat(),
            "access_count": self.access_count,
            "relevance_score": self.relevance_score,
            "token_count": self.token_count
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ContextItem":
        """Create from dictionary"""
        return cls(
            id=data["id"],
            type=ContextType(data["type"]),
            content=data["content"],
            metadata=data.get("metadata", {}),
            created_at=datetime.fromisoformat(data["created_at"]),
            accessed_at=datetime.fromisoformat(data["accessed_at"]),
            access_count=data.get("access_count", 0),
            relevance_score=data.get("relevance_score", 0.0),
            token_count=data.get("token_count", 0)
        )


class ContextWindow:
    """Manages a sliding context window with intelligent pruning"""

    def __init__(self, max_tokens: int = 8000, target_tokens: int = 6000):
        self.max_tokens = max_tokens
        self.target_tokens = target_tokens
        self.items: list[ContextItem] = []
        self.total_tokens = 0

    def add_item(self, item: ContextItem) -> bool:
        """Add item to context window"""
        if self.total_tokens + item.token_count > self.max_tokens:
            self._prune_to_target()

        if self.total_tokens + item.token_count <= self.max_tokens:
            self.items.append(item)
            self.total_tokens += item.token_count
            return True
        return False

    def _prune_to_target(self):
        """Prune context to target size based on relevance"""
        if self.total_tokens <= self.target_tokens:
            return

        # Sort by relevance score and recency
        self.items.sort(
            key=lambda x: (x.relevance_score, x.accessed_at),
            reverse=True
        )

        # Keep most relevant items up to target tokens
        new_items = []
        new_tokens = 0

        for item in self.items:
            if new_tokens + item.token_count <= self.target_tokens:
                new_items.append(item)
                new_tokens += item.token_count
            else:
                break

        self.items = new_items
        self.total_tokens = new_tokens

    def get_context_string(self) -> str:
        """Get formatted context string"""
        context_parts = []
        for item in self.items:
            context_parts.append(f"[{item.type.value}]\n{item.content}\n")
        return "\n---\n".join(context_parts)


class AdvancedContextManager:
    """Advanced context management with semantic understanding"""

    def __init__(self, redis_url: str = "redis://localhost:6379"):
        self.redis_url = redis_url
        self.redis = None
        self.model = SentenceTransformer("all-MiniLM-L6-v2")
        self.context_cache: dict[str, ContextItem] = {}
        self.embedding_cache: dict[str, np.ndarray] = {}

    async def initialize(self):
        """Initialize Redis connection"""
        self.redis = await aioredis.create_redis_pool(self.redis_url)

    async def close(self):
        """Close Redis connection"""
        if self.redis:
            self.redis.close()
            await self.redis.wait_closed()

    def _calculate_token_count(self, text: str) -> int:
        """Estimate token count (rough approximation)"""
        # Rough estimation: 1 token ≈ 4 characters
        return len(text) // 4

    def _generate_embedding(self, text: str) -> np.ndarray:
        """Generate sentence embedding for text"""
        cache_key = hashlib.md5(text.encode()).hexdigest()

        if cache_key in self.embedding_cache:
            return self.embedding_cache[cache_key]

        embedding = self.model.encode([text])[0]
        self.embedding_cache[cache_key] = embedding

        return embedding

    def _calculate_relevance(
        self,
        item: ContextItem,
        query: str,
        context_type_weight: dict[ContextType, float]
    ) -> float:
        """Calculate relevance score for a context item"""
        # Semantic similarity
        query_embedding = self._generate_embedding(query)
        item_embedding = item.embedding or self._generate_embedding(item.content)

        similarity = cosine_similarity(
            query_embedding.reshape(1, -1),
            item_embedding.reshape(1, -1)
        )[0][0]

        # Type-based weight
        type_weight = context_type_weight.get(item.type, 1.0)

        # Recency factor (exponential decay)
        age_hours = (datetime.utcnow() - item.accessed_at).total_seconds() / 3600
        recency_factor = np.exp(-age_hours / 24)  # Half-life of 24 hours

        # Access frequency factor
        access_factor = min(1.0 + (item.access_count * 0.1), 2.0)

        # Combined relevance score
        relevance = similarity * type_weight * recency_factor * access_factor

        return relevance

    async def add_context(
        self,
        context_id: str,
        context_type: ContextType,
        content: str,
        metadata: dict[str, Any] | None = None
    ) -> ContextItem:
        """Add new context item"""
        item = ContextItem(
            id=context_id,
            type=context_type,
            content=content,
            metadata=metadata or {},
            token_count=self._calculate_token_count(content)
        )

        # Generate embedding
        item.embedding = self._generate_embedding(content)

        # Store in cache and Redis
        self.context_cache[context_id] = item

        if self.redis:
            await self.redis.setex(
                f"context:{context_id}",
                86400,  # 24 hour TTL
                json.dumps(item.to_dict())
            )

        logger.info(f"Added context item: {context_id} ({context_type.value})")
        return item

    async def get_relevant_context(
        self,
        query: str,
        max_items: int = 10,
        context_types: list[ContextType] | None = None,
        max_tokens: int = 8000
    ) -> ContextWindow:
        """Get relevant context for a query"""
        # Define context type weights based on query
        context_type_weight = self._infer_context_weights(query)

        # Filter by context types if specified
        items = list(self.context_cache.values())
        if context_types:
            items = [item for item in items if item.type in context_types]

        # Calculate relevance scores
        for item in items:
            item.relevance_score = self._calculate_relevance(
                item, query, context_type_weight
            )

        # Sort by relevance
        items.sort(key=lambda x: x.relevance_score, reverse=True)

        # Create context window
        window = ContextWindow(max_tokens=max_tokens)

        for item in items[:max_items]:
            # Update access metadata
            item.accessed_at = datetime.utcnow()
            item.access_count += 1

            # Add to window
            if window.add_item(item):
                logger.debug(
                    f"Added to context: {item.id} "
                    f"(relevance: {item.relevance_score:.3f})"
                )

        return window

    def _infer_context_weights(self, query: str) -> dict[ContextType, float]:
        """Infer context type weights based on query content"""
        weights = defaultdict(lambda: 1.0)

        # Keywords for different context types
        code_keywords = ["function", "class", "method", "variable", "bug", "error"]
        issue_keywords = ["issue", "problem", "bug", "feature", "request"]
        discussion_keywords = ["discuss", "question", "opinion", "feedback"]
        doc_keywords = ["document", "readme", "guide", "tutorial", "api"]

        query_lower = query.lower()

        # Adjust weights based on keywords
        if any(kw in query_lower for kw in code_keywords):
            weights[ContextType.CODE_REVIEW] = 2.0
            weights[ContextType.COMMIT] = 1.5

        if any(kw in query_lower for kw in issue_keywords):
            weights[ContextType.ISSUE] = 2.0

        if any(kw in query_lower for kw in discussion_keywords):
            weights[ContextType.DISCUSSION] = 1.8

        if any(kw in query_lower for kw in doc_keywords):
            weights[ContextType.DOCUMENTATION] = 2.0

        return dict(weights)

    async def prune_old_context(self, max_age_days: int = 7):
        """Prune context older than specified days"""
        cutoff_date = datetime.utcnow() - timedelta(days=max_age_days)

        items_to_remove = []
        for context_id, item in self.context_cache.items():
            if item.accessed_at < cutoff_date:
                items_to_remove.append(context_id)

        for context_id in items_to_remove:
            del self.context_cache[context_id]
            if self.redis:
                await self.redis.delete(f"context:{context_id}")

        logger.info(f"Pruned {len(items_to_remove)} old context items")

    async def get_context_summary(self) -> dict[str, Any]:
        """Get summary statistics about current context"""
        type_counts = defaultdict(int)
        total_tokens = 0

        for item in self.context_cache.values():
            type_counts[item.type.value] += 1
            total_tokens += item.token_count

        return {
            "total_items": len(self.context_cache),
            "total_tokens": total_tokens,
            "type_distribution": dict(type_counts),
            "cache_size_mb": len(str(self.context_cache)) / (1024 * 1024)
        }

    async def export_context(self, context_ids: list[str]) -> dict[str, Any]:
        """Export specific context items"""
        exported_items = []

        for context_id in context_ids:
            if context_id in self.context_cache:
                item = self.context_cache[context_id]
                exported_items.append(item.to_dict())

        return {
            "exported_at": datetime.utcnow().isoformat(),
            "items": exported_items
        }

    async def import_context(self, context_data: dict[str, Any]):
        """Import context items"""
        imported_count = 0

        for item_data in context_data.get("items", []):
            try:
                item = ContextItem.from_dict(item_data)
                item.embedding = self._generate_embedding(item.content)
                self.context_cache[item.id] = item
                imported_count += 1
            except Exception as e:
                logger.error(f"Failed to import context item: {e}")

        logger.info(f"Imported {imported_count} context items")


# Example usage
async def main():
    """Example usage of the Advanced Context Manager"""
    manager = AdvancedContextManager()
    await manager.initialize()

    try:
        # Add some context
        await manager.add_context(
            "pr-123",
            ContextType.CODE_REVIEW,
            "Fixed memory leak in authentication module",
            {"pr_number": 123, "author": "john_doe"}
        )

        await manager.add_context(
            "issue-456",
            ContextType.ISSUE,
            "Users experiencing timeout errors during login",
            {"issue_number": 456, "severity": "high"}
        )

        # Get relevant context
        query = "authentication problems and memory issues"
        await manager.get_relevant_context(query)


        # Get summary
        await manager.get_context_summary()

    finally:
        await manager.close()


if __name__ == "__main__":
    asyncio.run(main())
