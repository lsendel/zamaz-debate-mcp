#!/usr/bin/env python3
"""
Main application entry point with diagnostics integration.

This script starts the GitHub integration service with:
- Structured logging
- Diagnostic collection
- Correlation ID tracking
- Performance monitoring
"""

import os
import signal
import sys
from contextlib import asynccontextmanager
from pathlib import Path

import uvicorn
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from scripts.api import diagnostic_endpoints, review_endpoints, webhook_endpoints
from scripts.core.container import ServiceContainer
from scripts.core.interfaces import CacheInterface, DatabaseInterface
from scripts.core.structured_logging import (
    CorrelationIdMiddleware,
    get_correlation_id,
    get_structured_logger,
)
from scripts.diagnostics.diagnostic_collector import (
    DiagnosticCollector,
    ErrorAnalyzer,
    PerformanceProfiler,
    RequestTracer,
)

logger = get_structured_logger(__name__)

# Global instances
container: ServiceContainer = None
diagnostic_collector: DiagnosticCollector = None
request_tracer: RequestTracer = None
error_analyzer: ErrorAnalyzer = None
performance_profiler: PerformanceProfiler = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    global container, diagnostic_collector, request_tracer, error_analyzer, performance_profiler

    logger.info("Starting application with diagnostics")

    try:
        # Initialize service container
        container = ServiceContainer()
        await container.initialize()

        # Initialize diagnostic components
        database = container.resolve(DatabaseInterface)
        cache = container.resolve(CacheInterface)

        diagnostic_collector = DiagnosticCollector(database, cache)
        request_tracer = RequestTracer(diagnostic_collector)
        error_analyzer = ErrorAnalyzer()
        performance_profiler = PerformanceProfiler()

        # Store in app state for access in endpoints
        app.state.container = container
        app.state.diagnostic_collector = diagnostic_collector
        app.state.request_tracer = request_tracer
        app.state.error_analyzer = error_analyzer
        app.state.performance_profiler = performance_profiler

        logger.info("Application started successfully")

        yield

    except Exception as e:
        logger.error("Failed to start application", error=e)
        raise
    finally:
        logger.info("Shutting down application")
        if container:
            await container.shutdown()


# Create FastAPI app
app = FastAPI(title="GitHub Integration Service with Diagnostics", version="2.0.0", lifespan=lifespan)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add correlation ID middleware
app.add_middleware(CorrelationIdMiddleware)


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all requests with diagnostics."""
    correlation_id = get_correlation_id()

    # Start diagnostic context
    if diagnostic_collector:
        context = diagnostic_collector.start_diagnostic(f"{request.method} {request.url.path}")
        request_tracer.trace_event(
            correlation_id,
            "request_received",
            {
                "method": request.method,
                "path": request.url.path,
                "client": request.client.host if request.client else None,
            },
        )

    # Log request
    logger.info(
        "Request received", method=request.method, path=request.url.path, query_params=dict(request.query_params)
    )

    # Start performance timing
    if performance_profiler:
        performance_profiler.start_timer(f"{request.method}_{request.url.path}")

    try:
        # Process request
        response = await call_next(request)

        # Log response
        logger.info("Request completed", method=request.method, path=request.url.path, status_code=response.status_code)

        if diagnostic_collector:
            request_tracer.trace_event(correlation_id, "request_completed", {"status_code": response.status_code})

        return response

    except Exception as e:
        # Log error
        logger.error("Request failed", error=e, method=request.method, path=request.url.path)

        # Analyze error
        if error_analyzer:
            error_analysis = error_analyzer.analyze_error(
                e, {"method": request.method, "path": request.url.path, "correlation_id": correlation_id}
            )
            logger.info("Error analysis", **error_analysis)

        if diagnostic_collector and correlation_id in diagnostic_collector.active_contexts:
            context = diagnostic_collector.active_contexts[correlation_id]
            context.add_error(e, {"request": str(request.url)})

        raise

    finally:
        # End timing
        if performance_profiler:
            performance_profiler.end_timer(
                f"{request.method}_{request.url.path}",
                status_code=response.status_code if "response" in locals() else None,
            )

        # End diagnostic
        if diagnostic_collector and correlation_id:
            diagnostic_collector.end_diagnostic(correlation_id)


# Include routers
app.include_router(webhook_endpoints.router)
app.include_router(review_endpoints.router)
app.include_router(diagnostic_endpoints.router)


@app.get("/")
async def root():
    """Root endpoint with service info."""
    return {
        "service": "GitHub Integration with Diagnostics",
        "version": "2.0.0",
        "correlation_id": get_correlation_id(),
        "endpoints": {
            "webhooks": "/webhooks",
            "reviews": "/reviews",
            "diagnostics": "/diagnostics",
            "health": "/diagnostics/health/detailed",
        },
    }


@app.get("/health")
async def health():
    """Simple health check."""
    return {"status": "healthy"}


def handle_shutdown(signum, frame):
    """Handle shutdown signals."""
    logger.info("Received shutdown signal", signal=signum)
    sys.exit(0)


def main():
    """Main entry point."""
    # Set up signal handlers
    signal.signal(signal.SIGINT, handle_shutdown)
    signal.signal(signal.SIGTERM, handle_shutdown)

    # Get configuration
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "5000"))
    workers = int(os.getenv("WORKERS", "4"))
    log_level = os.getenv("LOG_LEVEL", "info").lower()

    logger.info("Starting server", host=host, port=port, workers=workers, log_level=log_level)

    # Run server
    uvicorn.run(
        "scripts.main_with_diagnostics:app",
        host=host,
        port=port,
        workers=workers,
        log_level=log_level,
        access_log=False,  # We handle logging ourselves
        lifespan="on",
    )


if __name__ == "__main__":
    main()
