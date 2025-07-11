from abc import ABC, abstractmethod
from typing import List, Optional
import structlog

from ..models import Context, ContextWindow, Message, ContextStrategy

logger = structlog.get_logger()


class WindowStrategy(ABC):
    """Abstract base class for context window strategies"""
    
    @abstractmethod
    async def apply(self, context: Context, max_tokens: int, 
                   include_summary: bool) -> ContextWindow:
        """Apply the windowing strategy"""
        pass


class FullStrategy(WindowStrategy):
    """Return the full context without modification"""
    
    async def apply(self, context: Context, max_tokens: int, 
                   include_summary: bool) -> ContextWindow:
        return ContextWindow(
            context_id=context.id,
            messages=context.messages,
            total_messages=len(context.messages),
            included_messages=len(context.messages),
            token_count=context.token_count,
            strategy_used=ContextStrategy.FULL
        )


class SlidingWindowStrategy(WindowStrategy):
    """Use a sliding window to include most recent messages"""
    
    async def apply(self, context: Context, max_tokens: int, 
                   include_summary: bool) -> ContextWindow:
        messages = []
        current_tokens = 0
        
        # Iterate backwards through messages
        for msg in reversed(context.messages):
            # Estimate tokens (simplified - would use proper tokenizer)
            msg_tokens = len(msg.content.split()) * 1.3
            
            if current_tokens + msg_tokens > max_tokens:
                break
            
            messages.insert(0, msg)
            current_tokens += msg_tokens
        
        return ContextWindow(
            context_id=context.id,
            messages=messages,
            total_messages=len(context.messages),
            included_messages=len(messages),
            token_count=int(current_tokens),
            strategy_used=ContextStrategy.SLIDING_WINDOW
        )


class SlidingWindowWithSummaryStrategy(WindowStrategy):
    """Sliding window with summary of older messages"""
    
    async def apply(self, context: Context, max_tokens: int, 
                   include_summary: bool) -> ContextWindow:
        # Reserve tokens for summary
        summary_tokens = int(max_tokens * 0.2) if include_summary else 0
        window_tokens = max_tokens - summary_tokens
        
        # Get recent messages using sliding window
        window_strategy = SlidingWindowStrategy()
        window_result = await window_strategy.apply(context, window_tokens, False)
        
        # Generate summary of excluded messages if needed
        summary = None
        if include_summary and window_result.included_messages < len(context.messages):
            excluded_count = len(context.messages) - window_result.included_messages
            summary = f"[Summary of {excluded_count} earlier messages]"
            
            # Add summary as first message
            summary_msg = Message(
                role="system",
                content=summary,
                metadata={"is_summary": True}
            )
            window_result.messages.insert(0, summary_msg)
            window_result.summary = summary
        
        window_result.strategy_used = ContextStrategy.SLIDING_WINDOW_WITH_SUMMARY
        return window_result


class SemanticSelectionStrategy(WindowStrategy):
    """Select messages based on semantic relevance"""
    
    async def apply(self, context: Context, max_tokens: int, 
                   include_summary: bool) -> ContextWindow:
        # Simplified implementation - in production would use embeddings
        # For now, just select messages with certain keywords
        
        # Always include system messages
        selected_messages = [msg for msg in context.messages if msg.role == "system"]
        
        # Add other messages up to token limit
        current_tokens = sum(len(msg.content.split()) * 1.3 for msg in selected_messages)
        
        for msg in context.messages:
            if msg.role != "system":
                msg_tokens = len(msg.content.split()) * 1.3
                if current_tokens + msg_tokens <= max_tokens:
                    selected_messages.append(msg)
                    current_tokens += msg_tokens
        
        return ContextWindow(
            context_id=context.id,
            messages=selected_messages,
            total_messages=len(context.messages),
            included_messages=len(selected_messages),
            token_count=int(current_tokens),
            strategy_used=ContextStrategy.SEMANTIC_SELECTION
        )


class WindowStrategyFactory:
    """Factory for creating window strategies"""
    
    _strategies = {
        ContextStrategy.FULL: FullStrategy,
        ContextStrategy.SLIDING_WINDOW: SlidingWindowStrategy,
        ContextStrategy.SLIDING_WINDOW_WITH_SUMMARY: SlidingWindowWithSummaryStrategy,
        ContextStrategy.SEMANTIC_SELECTION: SemanticSelectionStrategy
    }
    
    def get_strategy(self, strategy_type: ContextStrategy) -> WindowStrategy:
        """Get a window strategy instance"""
        strategy_class = self._strategies.get(strategy_type)
        if not strategy_class:
            logger.warning("Unknown strategy, using sliding window", 
                         strategy=strategy_type)
            strategy_class = SlidingWindowStrategy
        
        return strategy_class()