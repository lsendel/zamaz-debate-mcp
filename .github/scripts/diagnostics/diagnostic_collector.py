"""
Enhanced diagnostic collector for troubleshooting.

This module provides comprehensive diagnostics collection including:
- System state snapshots
- Request tracing
- Performance profiling
- Error analysis
"""

import asyncio
import json
import os
import platform
import psutil
import sys
import traceback
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass, field
import threading
import cProfile
import pstats
import io
from contextlib import contextmanager

from ..core.logging import get_logger, get_correlation_id
from ..core.interfaces import DatabaseInterface, CacheInterface


logger = get_logger(__name__)


@dataclass
class DiagnosticContext:
    """Context for diagnostic collection."""
    correlation_id: str
    operation: str
    start_time: datetime
    end_time: Optional[datetime] = None
    traces: List[Dict[str, Any]] = field(default_factory=list)
    metrics: Dict[str, Any] = field(default_factory=dict)
    errors: List[Dict[str, Any]] = field(default_factory=list)
    profiling_data: Optional[str] = None
    
    def add_trace(self, event: str, data: Dict[str, Any]):
        """Add a trace event."""
        self.traces.append({
            'timestamp': datetime.utcnow().isoformat(),
            'event': event,
            'data': data,
            'duration_ms': self._get_elapsed_ms()
        })
    
    def add_error(self, error: Exception, context: Dict[str, Any]):
        """Add error information."""
        self.errors.append({
            'timestamp': datetime.utcnow().isoformat(),
            'type': type(error).__name__,
            'message': str(error),
            'traceback': traceback.format_exc(),
            'context': context
        })
    
    def _get_elapsed_ms(self) -> float:
        """Get elapsed time in milliseconds."""
        if self.end_time:
            duration = self.end_time - self.start_time
        else:
            duration = datetime.utcnow() - self.start_time
        return duration.total_seconds() * 1000


class DiagnosticCollector:
    """Collects diagnostic information for troubleshooting."""
    
    def __init__(self, database: DatabaseInterface, cache: CacheInterface):
        self.database = database
        self.cache = cache
        self.active_contexts: Dict[str, DiagnosticContext] = {}
        self._lock = threading.Lock()
    
    def start_diagnostic(self, operation: str, correlation_id: Optional[str] = None) -> DiagnosticContext:
        """Start diagnostic collection for an operation."""
        if not correlation_id:
            correlation_id = get_correlation_id() or f"diag-{datetime.utcnow().timestamp()}"
        
        context = DiagnosticContext(
            correlation_id=correlation_id,
            operation=operation,
            start_time=datetime.utcnow()
        )
        
        with self._lock:
            self.active_contexts[correlation_id] = context
        
        logger.info(f"Started diagnostic collection", 
                   correlation_id=correlation_id,
                   operation=operation)
        
        return context
    
    def end_diagnostic(self, correlation_id: str) -> Optional[DiagnosticContext]:
        """End diagnostic collection."""
        with self._lock:
            context = self.active_contexts.pop(correlation_id, None)
        
        if context:
            context.end_time = datetime.utcnow()
            logger.info(f"Ended diagnostic collection",
                       correlation_id=correlation_id,
                       duration_ms=context._get_elapsed_ms())
        
        return context
    
    @contextmanager
    def diagnostic_context(self, operation: str):
        """Context manager for diagnostic collection."""
        context = self.start_diagnostic(operation)
        try:
            yield context
        except Exception as e:
            context.add_error(e, {'operation': operation})
            raise
        finally:
            self.end_diagnostic(context.correlation_id)
            # Store diagnostic data
            asyncio.create_task(self._store_diagnostic(context))
    
    async def _store_diagnostic(self, context: DiagnosticContext):
        """Store diagnostic data for analysis."""
        try:
            # Store in cache for immediate access
            await self.cache.set(
                f"diagnostic:{context.correlation_id}",
                json.dumps({
                    'correlation_id': context.correlation_id,
                    'operation': context.operation,
                    'start_time': context.start_time.isoformat(),
                    'end_time': context.end_time.isoformat() if context.end_time else None,
                    'duration_ms': context._get_elapsed_ms(),
                    'traces': context.traces,
                    'metrics': context.metrics,
                    'errors': context.errors,
                    'profiling_data': context.profiling_data
                }),
                ttl_seconds=86400  # 24 hours
            )
            
            # Store summary in database for long-term analysis
            await self.database.execute(
                """
                INSERT INTO diagnostics (
                    correlation_id, operation, start_time, duration_ms,
                    error_count, trace_count, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                {
                    'correlation_id': context.correlation_id,
                    'operation': context.operation,
                    'start_time': context.start_time.isoformat(),
                    'duration_ms': context._get_elapsed_ms(),
                    'error_count': len(context.errors),
                    'trace_count': len(context.traces),
                    'created_at': datetime.utcnow().isoformat()
                }
            )
        except Exception as e:
            logger.error(f"Failed to store diagnostic data: {e}")
    
    async def get_diagnostic(self, correlation_id: str) -> Optional[Dict[str, Any]]:
        """Retrieve diagnostic data."""
        # Try cache first
        cached = await self.cache.get(f"diagnostic:{correlation_id}")
        if cached:
            return json.loads(cached)
        
        # Fall back to database
        result = await self.database.execute(
            "SELECT * FROM diagnostics WHERE correlation_id = ?",
            {'correlation_id': correlation_id}
        )
        
        return result[0] if result else None
    
    def collect_system_state(self) -> Dict[str, Any]:
        """Collect current system state."""
        process = psutil.Process()
        
        return {
            'timestamp': datetime.utcnow().isoformat(),
            'system': {
                'platform': platform.platform(),
                'python_version': sys.version,
                'cpu_count': psutil.cpu_count(),
                'cpu_percent': psutil.cpu_percent(interval=0.1),
                'memory': {
                    'total': psutil.virtual_memory().total,
                    'available': psutil.virtual_memory().available,
                    'percent': psutil.virtual_memory().percent
                }
            },
            'process': {
                'pid': process.pid,
                'cpu_percent': process.cpu_percent(interval=0.1),
                'memory_info': {
                    'rss': process.memory_info().rss,
                    'vms': process.memory_info().vms
                },
                'num_threads': process.num_threads(),
                'open_files': len(process.open_files()),
                'connections': len(process.connections())
            },
            'active_diagnostics': len(self.active_contexts)
        }
    
    @contextmanager
    def profile(self, context: DiagnosticContext):
        """Profile code execution."""
        profiler = cProfile.Profile()
        profiler.enable()
        
        try:
            yield
        finally:
            profiler.disable()
            
            # Capture profiling results
            s = io.StringIO()
            ps = pstats.Stats(profiler, stream=s).sort_stats('cumulative')
            ps.print_stats(30)  # Top 30 functions
            
            context.profiling_data = s.getvalue()


class RequestTracer:
    """Traces requests through the system."""
    
    def __init__(self, collector: DiagnosticCollector):
        self.collector = collector
        self.traces: Dict[str, List[Dict[str, Any]]] = {}
    
    def trace_event(self, correlation_id: str, event: str, data: Dict[str, Any]):
        """Add a trace event."""
        context = self.collector.active_contexts.get(correlation_id)
        if context:
            context.add_trace(event, data)
        
        # Also store in local traces
        if correlation_id not in self.traces:
            self.traces[correlation_id] = []
        
        self.traces[correlation_id].append({
            'timestamp': datetime.utcnow().isoformat(),
            'event': event,
            'data': data
        })
    
    def get_trace(self, correlation_id: str) -> List[Dict[str, Any]]:
        """Get trace for a correlation ID."""
        return self.traces.get(correlation_id, [])
    
    def create_trace_timeline(self, correlation_id: str) -> str:
        """Create a visual timeline of the trace."""
        trace = self.get_trace(correlation_id)
        if not trace:
            return "No trace found"
        
        timeline = []
        start_time = datetime.fromisoformat(trace[0]['timestamp'])
        
        for event in trace:
            event_time = datetime.fromisoformat(event['timestamp'])
            elapsed = (event_time - start_time).total_seconds() * 1000
            timeline.append(f"{elapsed:>8.2f}ms | {event['event']}")
            
            # Add important data
            for key, value in event.get('data', {}).items():
                if key in ['status', 'error', 'result']:
                    timeline.append(f"{'':>11} | └─ {key}: {value}")
        
        return "\n".join(timeline)


class ErrorAnalyzer:
    """Analyzes errors for patterns and root causes."""
    
    def __init__(self):
        self.error_patterns: Dict[str, int] = {}
        self.error_contexts: Dict[str, List[Dict[str, Any]]] = {}
    
    def analyze_error(self, error: Exception, context: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze an error and its context."""
        error_type = type(error).__name__
        error_msg = str(error)
        
        # Track error patterns
        pattern_key = f"{error_type}:{error_msg[:50]}"
        self.error_patterns[pattern_key] = self.error_patterns.get(pattern_key, 0) + 1
        
        # Store error context
        if error_type not in self.error_contexts:
            self.error_contexts[error_type] = []
        
        self.error_contexts[error_type].append({
            'timestamp': datetime.utcnow().isoformat(),
            'message': error_msg,
            'context': context,
            'traceback': traceback.format_exc()
        })
        
        # Analyze root cause
        root_cause = self._identify_root_cause(error, context)
        
        return {
            'error_type': error_type,
            'message': error_msg,
            'pattern_count': self.error_patterns[pattern_key],
            'root_cause': root_cause,
            'suggestions': self._get_suggestions(error_type, root_cause)
        }
    
    def _identify_root_cause(self, error: Exception, context: Dict[str, Any]) -> str:
        """Identify likely root cause of error."""
        error_type = type(error).__name__
        error_msg = str(error).lower()
        
        # Common root causes
        if 'timeout' in error_msg:
            return 'timeout'
        elif 'connection' in error_msg or 'network' in error_msg:
            return 'network'
        elif 'rate limit' in error_msg:
            return 'rate_limit'
        elif 'permission' in error_msg or 'unauthorized' in error_msg:
            return 'permission'
        elif 'not found' in error_msg or '404' in error_msg:
            return 'not_found'
        elif 'memory' in error_msg:
            return 'memory'
        elif 'disk' in error_msg or 'space' in error_msg:
            return 'disk_space'
        else:
            return 'unknown'
    
    def _get_suggestions(self, error_type: str, root_cause: str) -> List[str]:
        """Get suggestions for fixing the error."""
        suggestions = []
        
        if root_cause == 'timeout':
            suggestions.extend([
                "Increase timeout duration",
                "Optimize the operation for better performance",
                "Check if the target service is responsive"
            ])
        elif root_cause == 'network':
            suggestions.extend([
                "Check network connectivity",
                "Verify service endpoints are correct",
                "Implement retry logic with exponential backoff"
            ])
        elif root_cause == 'rate_limit':
            suggestions.extend([
                "Implement rate limit handling",
                "Add caching to reduce API calls",
                "Use batch operations where possible"
            ])
        elif root_cause == 'permission':
            suggestions.extend([
                "Verify authentication credentials",
                "Check permission scopes",
                "Ensure tokens are not expired"
            ])
        elif root_cause == 'memory':
            suggestions.extend([
                "Increase memory allocation",
                "Optimize memory usage",
                "Implement memory cleanup"
            ])
        
        return suggestions
    
    def get_error_summary(self) -> Dict[str, Any]:
        """Get summary of all errors."""
        return {
            'total_errors': sum(self.error_patterns.values()),
            'unique_patterns': len(self.error_patterns),
            'top_errors': sorted(
                self.error_patterns.items(),
                key=lambda x: x[1],
                reverse=True
            )[:10],
            'error_types': list(self.error_contexts.keys())
        }


class PerformanceProfiler:
    """Enhanced performance profiling."""
    
    def __init__(self):
        self.operation_stats: Dict[str, List[float]] = {}
    
    def record_operation(self, operation: str, duration_ms: float, metadata: Dict[str, Any] = None):
        """Record operation performance."""
        if operation not in self.operation_stats:
            self.operation_stats[operation] = []
        
        self.operation_stats[operation].append(duration_ms)
        
        # Log slow operations
        if duration_ms > 1000:  # > 1 second
            logger.warning(f"Slow operation detected",
                          operation=operation,
                          duration_ms=duration_ms,
                          metadata=metadata)
    
    def get_performance_summary(self) -> Dict[str, Any]:
        """Get performance summary."""
        summary = {}
        
        for operation, durations in self.operation_stats.items():
            if durations:
                sorted_durations = sorted(durations)
                summary[operation] = {
                    'count': len(durations),
                    'min_ms': min(durations),
                    'max_ms': max(durations),
                    'avg_ms': sum(durations) / len(durations),
                    'p50_ms': sorted_durations[len(sorted_durations) // 2],
                    'p95_ms': sorted_durations[int(len(sorted_durations) * 0.95)],
                    'p99_ms': sorted_durations[int(len(sorted_durations) * 0.99)]
                }
        
        return summary
    
    def identify_bottlenecks(self) -> List[Tuple[str, float]]:
        """Identify performance bottlenecks."""
        bottlenecks = []
        
        for operation, stats in self.get_performance_summary().items():
            if stats['p95_ms'] > 500:  # Operations taking > 500ms at P95
                bottlenecks.append((operation, stats['p95_ms']))
        
        return sorted(bottlenecks, key=lambda x: x[1], reverse=True)