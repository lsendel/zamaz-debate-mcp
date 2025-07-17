"""
Structured logging configuration for the Kiro GitHub Integration.

This module provides centralized logging with structured output,
correlation IDs, and proper formatting for different environments.
"""

import json
import logging
import sys
import traceback
from contextvars import ContextVar
from datetime import datetime
from typing import Any, Dict, Optional
import uuid


# Context variable for correlation ID
correlation_id_var: ContextVar[Optional[str]] = ContextVar('correlation_id', default=None)


class StructuredFormatter(logging.Formatter):
    """
    Custom formatter that outputs structured JSON logs.
    
    Includes:
    - Timestamp in ISO format
    - Log level
    - Logger name
    - Correlation ID
    - Message
    - Extra fields
    - Exception info if present
    """
    
    def format(self, record: logging.LogRecord) -> str:
        """Format log record as JSON."""
        # Base log structure
        log_data = {
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'level': record.levelname,
            'logger': record.name,
            'message': record.getMessage(),
            'correlation_id': correlation_id_var.get()
        }
        
        # Add extra fields
        for key, value in record.__dict__.items():
            if key not in ['name', 'msg', 'args', 'created', 'filename', 
                          'funcName', 'levelname', 'levelno', 'lineno', 
                          'module', 'msecs', 'message', 'pathname', 'process',
                          'processName', 'relativeCreated', 'thread', 'threadName',
                          'exc_info', 'exc_text', 'stack_info']:
                log_data[key] = value
        
        # Add source location in development
        if record.levelno >= logging.WARNING:
            log_data['source'] = {
                'file': record.pathname,
                'line': record.lineno,
                'function': record.funcName
            }
        
        # Add exception info if present
        if record.exc_info:
            log_data['exception'] = {
                'type': record.exc_info[0].__name__,
                'message': str(record.exc_info[1]),
                'traceback': traceback.format_exception(*record.exc_info)
            }
        
        return json.dumps(log_data, default=str)


class HumanReadableFormatter(logging.Formatter):
    """
    Human-readable formatter for development environments.
    
    Uses colors and clean formatting for better readability.
    """
    
    # ANSI color codes
    COLORS = {
        'DEBUG': '\033[36m',    # Cyan
        'INFO': '\033[32m',     # Green
        'WARNING': '\033[33m',  # Yellow
        'ERROR': '\033[31m',    # Red
        'CRITICAL': '\033[35m', # Magenta
    }
    RESET = '\033[0m'
    
    def format(self, record: logging.LogRecord) -> str:
        """Format log record for human readability."""
        # Get color for level
        color = self.COLORS.get(record.levelname, '')
        
        # Format timestamp
        timestamp = datetime.fromtimestamp(record.created).strftime('%Y-%m-%d %H:%M:%S')
        
        # Get correlation ID
        correlation_id = correlation_id_var.get()
        correlation_str = f"[{correlation_id[:8]}] " if correlation_id else ""
        
        # Base format
        prefix = f"{timestamp} {color}{record.levelname:8}{self.RESET} {correlation_str}{record.name}"
        message = f"{prefix} - {record.getMessage()}"
        
        # Add extra fields
        extras = []
        for key, value in record.__dict__.items():
            if key not in ['name', 'msg', 'args', 'created', 'filename', 
                          'funcName', 'levelname', 'levelno', 'lineno', 
                          'module', 'msecs', 'message', 'pathname', 'process',
                          'processName', 'relativeCreated', 'thread', 'threadName',
                          'exc_info', 'exc_text', 'stack_info']:
                extras.append(f"{key}={value}")
        
        if extras:
            message += f" | {' '.join(extras)}"
        
        # Add exception info
        if record.exc_info:
            exception_text = '\n'.join(traceback.format_exception(*record.exc_info))
            message += f"\n{exception_text}"
        
        return message


class ContextFilter(logging.Filter):
    """Filter that adds context information to log records."""
    
    def filter(self, record: logging.LogRecord) -> bool:
        """Add context to log record."""
        # Add correlation ID if not already present
        if not hasattr(record, 'correlation_id'):
            record.correlation_id = correlation_id_var.get()
        
        return True


def configure_logging(
    level: str = 'INFO',
    format_type: str = 'auto',
    log_file: Optional[str] = None
) -> None:
    """
    Configure logging for the application.
    
    Args:
        level: Log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
        format_type: Log format ('json', 'human', 'auto')
        log_file: Optional log file path
    """
    # Determine format type
    if format_type == 'auto':
        # Use JSON in production, human-readable in development
        format_type = 'json' if sys.stdout.isatty() else 'human'
    
    # Create formatter
    if format_type == 'json':
        formatter = StructuredFormatter()
    else:
        formatter = HumanReadableFormatter()
    
    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, level.upper()))
    
    # Remove existing handlers
    root_logger.handlers.clear()
    
    # Console handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    console_handler.addFilter(ContextFilter())
    root_logger.addHandler(console_handler)
    
    # File handler if specified
    if log_file:
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(StructuredFormatter())  # Always use JSON for files
        file_handler.addFilter(ContextFilter())
        root_logger.addHandler(file_handler)
    
    # Set levels for third-party libraries
    logging.getLogger('urllib3').setLevel(logging.WARNING)
    logging.getLogger('requests').setLevel(logging.WARNING)
    logging.getLogger('asyncio').setLevel(logging.WARNING)


def get_logger(name: str) -> logging.Logger:
    """
    Get a logger instance with the given name.
    
    Args:
        name: Logger name (usually __name__)
        
    Returns:
        Configured logger instance
    """
    return logging.getLogger(name)


def set_correlation_id(correlation_id: Optional[str] = None) -> str:
    """
    Set correlation ID for the current context.
    
    Args:
        correlation_id: Correlation ID to set (generates one if not provided)
        
    Returns:
        The correlation ID that was set
    """
    if correlation_id is None:
        correlation_id = str(uuid.uuid4())
    
    correlation_id_var.set(correlation_id)
    return correlation_id


def get_correlation_id() -> Optional[str]:
    """Get the current correlation ID."""
    return correlation_id_var.get()


class LoggerAdapter(logging.LoggerAdapter):
    """
    Logger adapter that adds extra context to all log messages.
    
    Usage:
        logger = LoggerAdapter(get_logger(__name__), {'service': 'webhook'})
        logger.info("Processing webhook", event_type="pull_request")
    """
    
    def process(self, msg: str, kwargs: Dict[str, Any]) -> tuple:
        """Process log message and kwargs."""
        # Merge extra context
        extra = kwargs.get('extra', {})
        extra.update(self.extra)
        kwargs['extra'] = extra
        
        # Extract custom fields from kwargs
        for key in list(kwargs.keys()):
            if key not in ['exc_info', 'stack_info', 'extra']:
                extra[key] = kwargs.pop(key)
        
        return msg, kwargs


# Convenience functions for structured logging
def log_event(
    logger: logging.Logger,
    event_type: str,
    message: str,
    **kwargs: Any
) -> None:
    """
    Log a structured event.
    
    Args:
        logger: Logger instance
        event_type: Type of event
        message: Log message
        **kwargs: Additional fields to include
    """
    logger.info(
        message,
        extra={
            'event_type': event_type,
            **kwargs
        }
    )


def log_error(
    logger: logging.Logger,
    error: Exception,
    message: str,
    **kwargs: Any
) -> None:
    """
    Log an error with structured information.
    
    Args:
        logger: Logger instance
        error: Exception that occurred
        message: Log message
        **kwargs: Additional fields to include
    """
    logger.error(
        message,
        exc_info=error,
        extra={
            'error_type': type(error).__name__,
            'error_message': str(error),
            **kwargs
        }
    )


def log_performance(
    logger: logging.Logger,
    operation: str,
    duration_ms: float,
    **kwargs: Any
) -> None:
    """
    Log performance metrics.
    
    Args:
        logger: Logger instance
        operation: Operation name
        duration_ms: Duration in milliseconds
        **kwargs: Additional fields to include
    """
    logger.info(
        f"{operation} completed in {duration_ms:.2f}ms",
        extra={
            'operation': operation,
            'duration_ms': duration_ms,
            'performance_metric': True,
            **kwargs
        }
    )