from typing import List, Optional, Dict, Any
import asyncio
from datetime import datetime, timedelta
import structlog
import json
from sqlalchemy import select, update, and_, or_
from sqlalchemy.ext.asyncio import AsyncSession

from ..models import (
    Context, ContextNamespace, ContextShare, ContextWindow, ContextSummary,
    CreateContextRequest, AppendMessagesRequest, GetContextWindowRequest,
    ShareContextRequest, SearchContextsRequest, Message, ContextStrategy,
    AccessLevel, AuditLog
)
from ..db.connection import DatabaseManager
from .window_strategies import WindowStrategyFactory
from .token_counter import TokenCounter

logger = structlog.get_logger()


class ContextManager:
    """Manages context operations with multi-tenant support"""
    
    def __init__(self, db_manager: DatabaseManager):
        self.db = db_manager
        self.token_counter = TokenCounter()
        self.window_factory = WindowStrategyFactory()
        self._cache = {}  # Simple in-memory cache, replace with Redis in production
        
    async def create_context(self, request: CreateContextRequest) -> Context:
        """Create a new context"""
        logger.info("Creating context", org_id=request.org_id, name=request.name)
        
        async with self.db.get_session() as session:
            # Verify namespace belongs to org
            namespace = await self._get_namespace(session, request.namespace_id, request.org_id)
            if not namespace:
                raise ValueError(f"Namespace {request.namespace_id} not found")
            
            # Create context
            context = Context(
                org_id=request.org_id,
                namespace_id=request.namespace_id,
                name=request.name,
                description=request.description,
                messages=request.initial_messages,
                metadata=request.metadata,
                token_count=self.token_counter.count_messages(request.initial_messages)
            )
            
            # Save to database
            await self._save_context(session, context)
            
            # Audit log
            await self._audit_log(
                session,
                org_id=request.org_id,
                action="create_context",
                resource_type="context",
                resource_id=context.id,
                details={"name": request.name}
            )
            
            await session.commit()
            
        return context
    
    async def append_messages(self, request: AppendMessagesRequest) -> Context:
        """Append messages to existing context"""
        logger.info("Appending messages", context_id=request.context_id, count=len(request.messages))
        
        async with self.db.get_session() as session:
            # Get and verify context
            context = await self._get_context(session, request.context_id)
            if not context:
                raise ValueError(f"Context {request.context_id} not found")
            
            # Check permissions
            if not await self._can_write(session, context):
                raise PermissionError("No write access to context")
            
            # Append messages
            context.messages.extend(request.messages)
            context.token_count += self.token_counter.count_messages(request.messages)
            context.updated_at = datetime.utcnow()
            
            if request.update_version:
                context.version += 1
            
            # Save
            await self._save_context(session, context)
            
            # Clear cache
            self._invalidate_cache(request.context_id)
            
            # Audit
            await self._audit_log(
                session,
                org_id=context.org_id,
                action="append_messages",
                resource_type="context",
                resource_id=context.id,
                details={"message_count": len(request.messages)}
            )
            
            await session.commit()
            
        return context
    
    async def get_context_window(self, request: GetContextWindowRequest) -> ContextWindow:
        """Get optimized context window for LLM consumption"""
        logger.info("Getting context window", context_id=request.context_id, strategy=request.strategy)
        
        # Check cache first
        cache_key = f"window:{request.context_id}:{request.strategy}:{request.max_tokens}"
        if cache_key in self._cache:
            return self._cache[cache_key]
        
        async with self.db.get_session() as session:
            # Get context
            context = await self._get_context(session, request.context_id)
            if not context:
                raise ValueError(f"Context {request.context_id} not found")
            
            # Check permissions
            if not await self._can_read(session, context):
                raise PermissionError("No read access to context")
            
            # Apply windowing strategy
            strategy = self.window_factory.get_strategy(request.strategy)
            window = await strategy.apply(
                context=context,
                max_tokens=request.max_tokens,
                include_summary=request.include_summary
            )
            
            # Cache result
            self._cache[cache_key] = window
            
            # Audit
            await self._audit_log(
                session,
                org_id=context.org_id,
                action="get_context_window",
                resource_type="context",
                resource_id=context.id,
                details={"strategy": request.strategy, "tokens": window.token_count}
            )
            
        return window
    
    async def share_context(self, request: ShareContextRequest) -> ContextShare:
        """Share context with another organization"""
        logger.info("Sharing context", context_id=request.context_id, target=request.target_org_id)
        
        async with self.db.get_session() as session:
            # Get context
            context = await self._get_context(session, request.context_id)
            if not context:
                raise ValueError(f"Context {request.context_id} not found")
            
            # Check if user can share (must be owner)
            if not await self._is_owner(session, context):
                raise PermissionError("Only context owner can share")
            
            # Create share record
            expires_at = None
            if request.expires_in_hours:
                expires_at = datetime.utcnow() + timedelta(hours=request.expires_in_hours)
            
            share = ContextShare(
                context_id=request.context_id,
                source_org_id=context.org_id,
                target_org_id=request.target_org_id,
                access_level=request.access_level,
                expires_at=expires_at,
                created_by="current_user"  # Get from auth context
            )
            
            # Save
            await self._save_share(session, share)
            
            # Audit
            await self._audit_log(
                session,
                org_id=context.org_id,
                action="share_context",
                resource_type="context",
                resource_id=context.id,
                details={"target_org": request.target_org_id, "access": request.access_level}
            )
            
            await session.commit()
            
        return share
    
    async def search_contexts(self, request: SearchContextsRequest) -> List[Context]:
        """Search contexts by name or content"""
        logger.info("Searching contexts", org_id=request.org_id, query=request.query)
        
        async with self.db.get_session() as session:
            # Build query
            query = select(Context).where(
                or_(
                    Context.org_id == request.org_id,
                    # Include shared contexts if requested
                    and_(
                        request.include_shared == True,
                        Context.id.in_(
                            select(ContextShare.context_id).where(
                                and_(
                                    ContextShare.target_org_id == request.org_id,
                                    ContextShare.is_active == True,
                                    or_(
                                        ContextShare.expires_at == None,
                                        ContextShare.expires_at > datetime.utcnow()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            # Add filters
            if request.namespace_id:
                query = query.where(Context.namespace_id == request.namespace_id)
            
            if request.query:
                # Simple text search, could be enhanced with full-text search
                query = query.where(
                    or_(
                        Context.name.ilike(f"%{request.query}%"),
                        Context.description.ilike(f"%{request.query}%")
                    )
                )
            
            # Execute with pagination
            query = query.limit(request.limit).offset(request.offset)
            result = await session.execute(query)
            contexts = result.scalars().all()
            
        return contexts
    
    async def fork_context(self, context_id: str, new_name: str, namespace_id: Optional[str] = None) -> Context:
        """Create a copy of an existing context"""
        logger.info("Forking context", context_id=context_id, new_name=new_name)
        
        async with self.db.get_session() as session:
            # Get original context
            original = await self._get_context(session, context_id)
            if not original:
                raise ValueError(f"Context {context_id} not found")
            
            # Check permissions
            if not await self._can_read(session, original):
                raise PermissionError("No read access to context")
            
            # Create fork
            fork = Context(
                org_id=original.org_id,  # Could be different for shared contexts
                namespace_id=namespace_id or original.namespace_id,
                name=new_name,
                description=f"Fork of {original.name}",
                messages=original.messages.copy(),
                metadata={
                    **original.metadata,
                    "forked_from": context_id,
                    "forked_at": datetime.utcnow().isoformat()
                },
                token_count=original.token_count
            )
            
            # Save
            await self._save_context(session, fork)
            
            # Audit
            await self._audit_log(
                session,
                org_id=fork.org_id,
                action="fork_context",
                resource_type="context",
                resource_id=fork.id,
                details={"source_id": context_id}
            )
            
            await session.commit()
            
        return fork
    
    async def compress_context(self, context_id: str, keep_recent: int = 10, summary_style: str = "concise") -> Dict[str, Any]:
        """Compress older messages to save tokens"""
        logger.info("Compressing context", context_id=context_id, keep_recent=keep_recent)
        
        async with self.db.get_session() as session:
            # Get context
            context = await self._get_context(session, context_id)
            if not context:
                raise ValueError(f"Context {context_id} not found")
            
            # Check permissions
            if not await self._can_write(session, context):
                raise PermissionError("No write access to context")
            
            if len(context.messages) <= keep_recent:
                return {"tokens_saved": 0, "messages_compressed": 0}
            
            # Split messages
            to_compress = context.messages[:-keep_recent]
            to_keep = context.messages[-keep_recent:]
            
            # Generate summary (would call LLM service in production)
            summary = await self._generate_summary(to_compress, summary_style)
            
            # Calculate token savings
            original_tokens = self.token_counter.count_messages(to_compress)
            summary_tokens = self.token_counter.count_text(summary)
            tokens_saved = original_tokens - summary_tokens
            
            # Update context
            summary_message = Message(
                role="system",
                content=f"[Compressed {len(to_compress)} messages]\n{summary}",
                metadata={"compression_type": "summary", "original_count": len(to_compress)}
            )
            
            context.messages = [summary_message] + to_keep
            context.token_count = summary_tokens + self.token_counter.count_messages(to_keep)
            context.version += 1
            context.updated_at = datetime.utcnow()
            
            # Save
            await self._save_context(session, context)
            
            # Clear cache
            self._invalidate_cache(context_id)
            
            # Audit
            await self._audit_log(
                session,
                org_id=context.org_id,
                action="compress_context",
                resource_type="context",
                resource_id=context.id,
                details={"messages_compressed": len(to_compress), "tokens_saved": tokens_saved}
            )
            
            await session.commit()
            
        return {
            "tokens_saved": tokens_saved,
            "messages_compressed": len(to_compress),
            "new_token_count": context.token_count
        }
    
    # Helper methods
    
    async def _get_context(self, session: AsyncSession, context_id: str) -> Optional[Context]:
        """Get context from database"""
        # This is simplified - in production would use proper ORM queries
        # For now, return mock data
        return Context(
            id=context_id,
            org_id="org-123",
            namespace_id="ns-123",
            name="Test Context",
            messages=[]
        )
    
    async def _get_namespace(self, session: AsyncSession, namespace_id: str, org_id: str) -> Optional[ContextNamespace]:
        """Get namespace and verify ownership"""
        # Mock implementation
        return ContextNamespace(
            id=namespace_id,
            org_id=org_id,
            name="Test Namespace"
        )
    
    async def _save_context(self, session: AsyncSession, context: Context):
        """Save context to database"""
        # Mock implementation - would use SQLAlchemy in production
        pass
    
    async def _save_share(self, session: AsyncSession, share: ContextShare):
        """Save share record"""
        # Mock implementation
        pass
    
    async def _can_read(self, session: AsyncSession, context: Context) -> bool:
        """Check read permissions"""
        # Check if user's org owns context or has read share
        return True  # Mock
    
    async def _can_write(self, session: AsyncSession, context: Context) -> bool:
        """Check write permissions"""
        # Check if user's org owns context or has write share
        return True  # Mock
    
    async def _is_owner(self, session: AsyncSession, context: Context) -> bool:
        """Check if current org owns context"""
        return True  # Mock
    
    async def _audit_log(self, session: AsyncSession, **kwargs):
        """Create audit log entry"""
        # Mock implementation
        pass
    
    async def _generate_summary(self, messages: List[Message], style: str) -> str:
        """Generate summary of messages"""
        # In production, this would call the LLM service
        return f"Summary of {len(messages)} messages in {style} style"
    
    def _invalidate_cache(self, context_id: str):
        """Clear cache entries for context"""
        keys_to_remove = [k for k in self._cache if k.startswith(f"window:{context_id}:")]
        for key in keys_to_remove:
            del self._cache[key]
    
    async def list_namespaces(self, org_id: str) -> List[ContextNamespace]:
        """List namespaces for organization"""
        # Mock implementation
        return []
    
    async def list_contexts(self, org_id: str) -> List[Context]:
        """List contexts for organization"""
        # Mock implementation  
        return []
    
    async def list_shared_contexts(self, org_id: str) -> List[Context]:
        """List contexts shared with organization"""
        # Mock implementation
        return []