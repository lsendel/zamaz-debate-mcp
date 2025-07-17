"""
Diagnostic endpoints for troubleshooting and monitoring.

Provides endpoints for:
- Health checks with detailed component status
- Diagnostic data collection
- Request tracing
- Performance profiling
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
import asyncio
import psutil

from ..core.interfaces import (
    DatabaseInterface, CacheInterface, QueueInterface,
    MetricsInterface, GitHubClientInterface
)
from ..core.container import ServiceContainer
from ..diagnostics.diagnostic_collector import (
    DiagnosticCollector, RequestTracer, ErrorAnalyzer, PerformanceProfiler
)
from ..core.logging import get_logger

logger = get_logger(__name__)

router = APIRouter(prefix="/diagnostics", tags=["diagnostics"])


def get_diagnostic_collector(container: ServiceContainer = Depends()) -> DiagnosticCollector:
    """Get diagnostic collector instance."""
    database = container.resolve(DatabaseInterface)
    cache = container.resolve(CacheInterface)
    return DiagnosticCollector(database, cache)


def get_request_tracer(
    collector: DiagnosticCollector = Depends(get_diagnostic_collector)
) -> RequestTracer:
    """Get request tracer instance."""
    return RequestTracer(collector)


def get_error_analyzer() -> ErrorAnalyzer:
    """Get error analyzer instance."""
    return ErrorAnalyzer()


def get_performance_profiler() -> PerformanceProfiler:
    """Get performance profiler instance."""
    return PerformanceProfiler()


@router.get("/health/detailed")
async def detailed_health_check(
    container: ServiceContainer = Depends()
) -> Dict[str, Any]:
    """
    Detailed health check with component status.
    
    Returns comprehensive health information including:
    - Service status
    - Component health
    - Resource usage
    - Recent errors
    """
    health_status = {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "components": {},
        "resources": {},
        "errors": []
    }
    
    # Check database
    try:
        database = container.resolve(DatabaseInterface)
        await database.execute("SELECT 1", {})
        health_status["components"]["database"] = {
            "status": "healthy",
            "latency_ms": 0  # Would measure actual latency
        }
    except Exception as e:
        health_status["status"] = "degraded"
        health_status["components"]["database"] = {
            "status": "unhealthy",
            "error": str(e)
        }
        health_status["errors"].append({
            "component": "database",
            "error": str(e),
            "timestamp": datetime.utcnow().isoformat()
        })
    
    # Check cache
    try:
        cache = container.resolve(CacheInterface)
        await cache.set("health_check", "ok", ttl_seconds=10)
        await cache.get("health_check")
        health_status["components"]["cache"] = {
            "status": "healthy",
            "latency_ms": 0
        }
    except Exception as e:
        health_status["status"] = "degraded"
        health_status["components"]["cache"] = {
            "status": "unhealthy",
            "error": str(e)
        }
    
    # Check queue
    try:
        queue = container.resolve(QueueInterface)
        queue_size = await queue.size()
        health_status["components"]["queue"] = {
            "status": "healthy",
            "size": queue_size
        }
    except Exception as e:
        health_status["status"] = "degraded"
        health_status["components"]["queue"] = {
            "status": "unhealthy",
            "error": str(e)
        }
    
    # Check GitHub API
    try:
        github_client = container.resolve(GitHubClientInterface)
        rate_limit = await github_client.get_rate_limit()
        health_status["components"]["github_api"] = {
            "status": "healthy",
            "rate_limit": {
                "remaining": rate_limit.get("remaining", 0),
                "limit": rate_limit.get("limit", 5000),
                "reset": rate_limit.get("reset", 0)
            }
        }
    except Exception as e:
        health_status["components"]["github_api"] = {
            "status": "degraded",
            "error": str(e)
        }
    
    # Resource usage
    process = psutil.Process()
    health_status["resources"] = {
        "cpu_percent": process.cpu_percent(interval=0.1),
        "memory_mb": process.memory_info().rss / 1024 / 1024,
        "threads": process.num_threads(),
        "open_files": len(process.open_files()),
        "connections": len(process.connections())
    }
    
    # Overall status
    unhealthy_components = [
        name for name, info in health_status["components"].items()
        if info["status"] != "healthy"
    ]
    
    if len(unhealthy_components) > len(health_status["components"]) / 2:
        health_status["status"] = "unhealthy"
    
    return health_status


@router.get("/system-state")
async def get_system_state(
    collector: DiagnosticCollector = Depends(get_diagnostic_collector)
) -> Dict[str, Any]:
    """Get current system state snapshot."""
    return collector.collect_system_state()


@router.get("/diagnostics/{correlation_id}")
async def get_diagnostic_data(
    correlation_id: str,
    collector: DiagnosticCollector = Depends(get_diagnostic_collector)
) -> Dict[str, Any]:
    """Get diagnostic data for a specific correlation ID."""
    data = await collector.get_diagnostic(correlation_id)
    if not data:
        raise HTTPException(status_code=404, detail="Diagnostic data not found")
    return data


@router.get("/trace/{correlation_id}")
async def get_request_trace(
    correlation_id: str,
    tracer: RequestTracer = Depends(get_request_tracer)
) -> Dict[str, Any]:
    """Get request trace for a correlation ID."""
    trace = tracer.get_trace(correlation_id)
    timeline = tracer.create_trace_timeline(correlation_id)
    
    return {
        "correlation_id": correlation_id,
        "trace": trace,
        "timeline": timeline
    }


@router.get("/errors/summary")
async def get_error_summary(
    analyzer: ErrorAnalyzer = Depends(get_error_analyzer)
) -> Dict[str, Any]:
    """Get error analysis summary."""
    return analyzer.get_error_summary()


@router.get("/performance/summary")
async def get_performance_summary(
    profiler: PerformanceProfiler = Depends(get_performance_profiler)
) -> Dict[str, Any]:
    """Get performance profiling summary."""
    summary = profiler.get_performance_summary()
    bottlenecks = profiler.identify_bottlenecks()
    
    return {
        "operations": summary,
        "bottlenecks": [
            {"operation": op, "p95_ms": ms}
            for op, ms in bottlenecks
        ]
    }


@router.get("/metrics/current")
async def get_current_metrics(
    container: ServiceContainer = Depends()
) -> Dict[str, Any]:
    """Get current metrics snapshot."""
    metrics = container.resolve(MetricsInterface)
    
    return {
        "timestamp": datetime.utcnow().isoformat(),
        "counters": {
            "pr_received": await metrics.get_counter("pr_received"),
            "pr_processed": await metrics.get_counter("pr_processed"),
            "errors_total": await metrics.get_counter("errors"),
            "issues_found": await metrics.get_counter("issues_found")
        },
        "gauges": {
            "queue_size": await metrics.get_gauge("queue_size"),
            "active_reviews": await metrics.get_gauge("active_reviews")
        },
        "histograms": {
            "pr_processing_duration": await metrics.get_histogram_summary("pr_processing_duration"),
            "analysis_duration": await metrics.get_histogram_summary("analysis_duration")
        }
    }


@router.post("/profile/{operation}")
async def start_profiling(
    operation: str,
    duration_seconds: int = Query(default=60, le=300),
    collector: DiagnosticCollector = Depends(get_diagnostic_collector)
) -> Dict[str, Any]:
    """
    Start profiling for a specific operation.
    
    Args:
        operation: Operation name to profile
        duration_seconds: Duration to profile (max 300 seconds)
    
    Returns:
        Profiling session information
    """
    context = collector.start_diagnostic(f"profile_{operation}")
    
    # Schedule profiling end
    async def end_profiling():
        await asyncio.sleep(duration_seconds)
        collector.end_diagnostic(context.correlation_id)
    
    asyncio.create_task(end_profiling())
    
    return {
        "correlation_id": context.correlation_id,
        "operation": operation,
        "duration_seconds": duration_seconds,
        "status": "profiling_started",
        "end_time": (datetime.utcnow() + timedelta(seconds=duration_seconds)).isoformat()
    }


@router.get("/active-diagnostics")
async def get_active_diagnostics(
    collector: DiagnosticCollector = Depends(get_diagnostic_collector)
) -> List[Dict[str, Any]]:
    """Get list of active diagnostic sessions."""
    active = []
    
    for correlation_id, context in collector.active_contexts.items():
        active.append({
            "correlation_id": correlation_id,
            "operation": context.operation,
            "start_time": context.start_time.isoformat(),
            "duration_ms": context._get_elapsed_ms(),
            "trace_count": len(context.traces),
            "error_count": len(context.errors)
        })
    
    return active


@router.post("/test-connection/{component}")
async def test_component_connection(
    component: str,
    container: ServiceContainer = Depends()
) -> Dict[str, Any]:
    """Test connection to a specific component."""
    start_time = datetime.utcnow()
    
    try:
        if component == "database":
            database = container.resolve(DatabaseInterface)
            await database.execute("SELECT 1", {})
        elif component == "cache":
            cache = container.resolve(CacheInterface)
            test_key = f"test_{datetime.utcnow().timestamp()}"
            await cache.set(test_key, "test", ttl_seconds=1)
            value = await cache.get(test_key)
            if value != "test":
                raise Exception("Cache test failed")
        elif component == "github":
            github_client = container.resolve(GitHubClientInterface)
            await github_client.get_rate_limit()
        else:
            raise HTTPException(status_code=400, detail=f"Unknown component: {component}")
        
        duration_ms = (datetime.utcnow() - start_time).total_seconds() * 1000
        
        return {
            "component": component,
            "status": "connected",
            "latency_ms": duration_ms,
            "timestamp": datetime.utcnow().isoformat()
        }
    
    except Exception as e:
        duration_ms = (datetime.utcnow() - start_time).total_seconds() * 1000
        
        return {
            "component": component,
            "status": "failed",
            "error": str(e),
            "latency_ms": duration_ms,
            "timestamp": datetime.utcnow().isoformat()
        }


@router.delete("/diagnostics/cleanup")
async def cleanup_old_diagnostics(
    older_than_hours: int = Query(default=24, ge=1, le=168),
    container: ServiceContainer = Depends()
) -> Dict[str, Any]:
    """Clean up old diagnostic data."""
    database = container.resolve(DatabaseInterface)
    cache = container.resolve(CacheInterface)
    
    cutoff_time = datetime.utcnow() - timedelta(hours=older_than_hours)
    
    # Clean database
    db_result = await database.execute(
        "DELETE FROM diagnostics WHERE created_at < ?",
        {"created_at": cutoff_time.isoformat()}
    )
    
    # Clean cache (would need to implement pattern-based deletion)
    # For now, we'll just report what would be cleaned
    
    return {
        "status": "completed",
        "database_records_deleted": db_result.get("rows_affected", 0),
        "cutoff_time": cutoff_time.isoformat(),
        "older_than_hours": older_than_hours
    }