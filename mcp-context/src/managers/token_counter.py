from typing import List
import tiktoken
import structlog

from ..models import Message

logger = structlog.get_logger()


class TokenCounter:
    """Counts tokens for messages"""
    
    def __init__(self):
        # Use cl100k_base encoding (GPT-4 default)
        try:
            self.encoding = tiktoken.get_encoding("cl100k_base")
        except Exception as e:
            logger.warning("Failed to load tiktoken, using approximation", error=str(e))
            self.encoding = None
    
    def count_text(self, text: str) -> int:
        """Count tokens in text"""
        if self.encoding:
            return len(self.encoding.encode(text))
        else:
            # Fallback approximation
            return int(len(text.split()) * 1.3)
    
    def count_message(self, message: Message) -> int:
        """Count tokens in a single message"""
        # Each message has overhead tokens
        overhead = 4  # <im_start>, role, \n, <im_end>
        
        tokens = overhead
        tokens += self.count_text(message.content)
        
        if message.name:
            tokens += self.count_text(message.name) + 1
        
        return tokens
    
    def count_messages(self, messages: List[Message]) -> int:
        """Count tokens in multiple messages"""
        total = 0
        for message in messages:
            total += self.count_message(message)
        
        # Add tokens for reply
        total += 3
        
        return total