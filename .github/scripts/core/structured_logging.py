"""
Enhanced structured logging with correlation ID tracking.

This module provides structured logging capabilities with:
- Automatic correlation ID injection
- Performance metrics
- Error tracking
- Integration with diagnostic system
"""

import json
import logging
import time
from datetime import datetime
from typing import Any, Dict, Optional, Union
from contextvars import ContextVar
from pythonjsonlogger import jsonlogger
import traceback

# Context variable for correlation ID
correlation_id_var: ContextVar[Optional[str]] = ContextVar('correlation_id', default=None)


class StructuredLogger:
    """Enhanced logger with structured output and correlation tracking."""
    
    def __init__(self, name: str):
        self.logger = logging.getLogger(name)
        self._setup_json_formatter()
    
    def _setup_json_formatter(self):
        """Set up JSON formatter for structured logs."""
        formatter = CorrelationJsonFormatter(
            '%(timestamp)s %(level)s %(name)s %(message)s'
        )
        
        # Remove existing handlers
        self.logger.handlers = []
        
        # Add JSON handler
        handler = logging.StreamHandler()
        handler.setFormatter(formatter)
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)
    
    def _add_context(self, extra: Dict[str, Any]) -> Dict[str, Any]:
        """Add context information to log entry."""
        correlation_id = correlation_id_var.get()
        if correlation_id:
            extra['correlation_id'] = correlation_id
        
        extra['timestamp'] = datetime.utcnow().isoformat()
        extra['service'] = 'github-integration'
        
        return extra
    
    def info(self, message: str, **kwargs):
        """Log info message with context."""
        extra = self._add_context(kwargs)
        self.logger.info(message, extra=extra)
    
    def warning(self, message: str, **kwargs):
        """Log warning message with context."""
        extra = self._add_context(kwargs)
        self.logger.warning(message, extra=extra)
    
    def error(self, message: str, error: Optional[Exception] = None, **kwargs):
        """Log error message with exception details."""
        extra = self._add_context(kwargs)
        
        if error:
            extra['error_type'] = type(error).__name__
            extra['error_message'] = str(error)
            extra['error_traceback'] = traceback.format_exc()
        
        self.logger.error(message, extra=extra)
    
    def debug(self, message: str, **kwargs):
        """Log debug message with context."""
        extra = self._add_context(kwargs)
        self.logger.debug(message, extra=extra)
    
    def metric(self, metric_name: str, value: Union[int, float], **kwargs):
        """Log metric data."""
        extra = self._add_context(kwargs)
        extra['metric_name'] = metric_name
        extra['metric_value'] = value
        extra['metric_type'] = 'gauge'
        
        self.logger.info(f"Metric: {metric_name}={value}", extra=extra)
    
    def timing(self, operation: str, duration_ms: float, **kwargs):
        """Log timing information."""
        extra = self._add_context(kwargs)
        extra['operation'] = operation
        extra['duration_ms'] = duration_ms
        extra['metric_type'] = 'timing'
        
        self.logger.info(f"Timing: {operation} took {duration_ms}ms", extra=extra)


class CorrelationJsonFormatter(jsonlogger.JsonFormatter):
    """JSON formatter that includes correlation ID."""
    
    def add_fields(self, log_record, record, message_dict):
        """Add custom fields to log record."""
        super().add_fields(log_record, record, message_dict)
        
        # Add timestamp if not present
        if 'timestamp' not in log_record:
            log_record['timestamp'] = datetime.utcnow().isoformat()
        
        # Add level name
        log_record['level'] = record.levelname
        
        # Add logger name
        log_record['logger'] = record.name
        
        # Add correlation ID from context if available
        correlation_id = correlation_id_var.get()
        if correlation_id and 'correlation_id' not in log_record:
            log_record['correlation_id'] = correlation_id


class LogContext:
    """Context manager for structured logging with timing."""
    
    def __init__(self, logger: StructuredLogger, operation: str, **kwargs):
        self.logger = logger
        self.operation = operation
        self.extra = kwargs
        self.start_time = None
    
    def __enter__(self):
        """Start timing and log entry."""
        self.start_time = time.time()
        self.logger.info(f"Starting {self.operation}", **self.extra)
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Log exit with timing."""
        duration_ms = (time.time() - self.start_time) * 1000
        
        if exc_type:
            self.logger.error(
                f"Failed {self.operation}",
                error=exc_val,
                duration_ms=duration_ms,
                **self.extra
            )
        else:
            self.logger.info(
                f"Completed {self.operation}",
                duration_ms=duration_ms,
                **self.extra
            )
            self.logger.timing(self.operation, duration_ms, **self.extra)
        
        return False  # Don't suppress exceptions


class PerformanceLogger:
    """Logger specifically for performance metrics."""
    
    def __init__(self, logger: StructuredLogger):
        self.logger = logger
        self.timers: Dict[str, float] = {}
    
    def start_timer(self, operation: str):
        """Start timing an operation."""
        self.timers[operation] = time.time()
    
    def end_timer(self, operation: str, **kwargs):
        """End timing and log the duration."""
        if operation not in self.timers:
            self.logger.warning(f"Timer not started for {operation}")
            return
        
        duration_ms = (time.time() - self.timers[operation]) * 1000
        del self.timers[operation]
        
        self.logger.timing(operation, duration_ms, **kwargs)
    
    def record_metric(self, name: str, value: Union[int, float], **kwargs):
        """Record a metric value."""
        self.logger.metric(name, value, **kwargs)


# Factory function for creating loggers
def get_structured_logger(name: str) -> StructuredLogger:
    """Get a structured logger instance."""
    return StructuredLogger(name)


# Correlation ID management
def set_correlation_id(correlation_id: str):
    """Set correlation ID for current context."""
    correlation_id_var.set(correlation_id)


def get_correlation_id() -> Optional[str]:
    """Get current correlation ID."""
    return correlation_id_var.get()


def clear_correlation_id():
    """Clear correlation ID from context."""
    correlation_id_var.set(None)


# Middleware for FastAPI to inject correlation IDs
class CorrelationIdMiddleware:
    """Middleware to inject correlation IDs into requests."""
    
    def __init__(self, app):
        self.app = app
    
    async def __call__(self, scope, receive, send):
        """Process request with correlation ID."""
        if scope["type"] == "http":
            headers = dict(scope["headers"])
            
            # Extract or generate correlation ID
            correlation_id = None
            for name, value in headers.items():
                if name.lower() == b"x-correlation-id":
                    correlation_id = value.decode("utf-8")
                    break
            
            if not correlation_id:
                import uuid
                correlation_id = str(uuid.uuid4())
            
            # Set correlation ID in context
            set_correlation_id(correlation_id)
            
            # Add correlation ID to response headers
            async def send_with_correlation(message):
                if message["type"] == "http.response.start":
                    headers = message.setdefault("headers", [])
                    headers.append((b"x-correlation-id", correlation_id.encode("utf-8")))
                await send(message)
            
            try:
                await self.app(scope, receive, send_with_correlation)
            finally:
                clear_correlation_id()
        else:
            await self.app(scope, receive, send)