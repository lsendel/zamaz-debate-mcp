"""
Concurrency management for MCP services
"""
import asyncio
from typing import Dict, Any, Optional, Callable, TypeVar, ParamSpec
from functools import wraps
import time
from collections import defaultdict
import logging

logger = logging.getLogger(__name__)

P = ParamSpec('P')
T = TypeVar('T')


class RateLimiter:
    """Rate limiter for API endpoints per organization"""
    
    def __init__(self, max_requests: int = 100, window_seconds: int = 60):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self.requests: Dict[str, list] = defaultdict(list)
        self._lock = asyncio.Lock()
    
    async def check_rate_limit(self, org_id: str) -> bool:
        """Check if request is within rate limit"""
        async with self._lock:
            now = time.time()
            
            # Clean old requests
            self.requests[org_id] = [
                req_time for req_time in self.requests[org_id]
                if now - req_time < self.window_seconds
            ]
            
            # Check limit
            if len(self.requests[org_id]) >= self.max_requests:
                return False
            
            # Add new request
            self.requests[org_id].append(now)
            return True


class RequestQueue:
    """Queue for handling concurrent requests"""
    
    def __init__(self, max_concurrent: int = 10):
        self.max_concurrent = max_concurrent
        self.semaphore = asyncio.Semaphore(max_concurrent)
        self.active_requests: Dict[str, int] = defaultdict(int)
        self._lock = asyncio.Lock()
    
    async def acquire(self, request_id: str):
        """Acquire a slot in the queue"""
        await self.semaphore.acquire()
        async with self._lock:
            self.active_requests[request_id] += 1
    
    async def release(self, request_id: str):
        """Release a slot in the queue"""
        self.semaphore.release()
        async with self._lock:
            self.active_requests[request_id] -= 1
            if self.active_requests[request_id] == 0:
                del self.active_requests[request_id]
    
    @property
    def active_count(self) -> int:
        """Get count of active requests"""
        return sum(self.active_requests.values())


class DebateLockManager:
    """Manages locks for debate operations to prevent race conditions"""
    
    def __init__(self):
        self.locks: Dict[str, asyncio.Lock] = {}
        self._lock = asyncio.Lock()
    
    async def get_lock(self, debate_id: str) -> asyncio.Lock:
        """Get or create a lock for a specific debate"""
        async with self._lock:
            if debate_id not in self.locks:
                self.locks[debate_id] = asyncio.Lock()
            return self.locks[debate_id]
    
    async def remove_lock(self, debate_id: str):
        """Remove lock when debate is no longer active"""
        async with self._lock:
            if debate_id in self.locks:
                del self.locks[debate_id]


# Global instances
rate_limiter = RateLimiter()
request_queue = RequestQueue()
debate_lock_manager = DebateLockManager()


def with_rate_limit(func: Callable[P, T]) -> Callable[P, T]:
    """Decorator to apply rate limiting to an endpoint"""
    @wraps(func)
    async def wrapper(*args: P.args, **kwargs: P.kwargs) -> T:
        # Extract org_id from kwargs or request
        org_id = kwargs.get('org_id', 'default')
        
        if not await rate_limiter.check_rate_limit(org_id):
            raise Exception("Rate limit exceeded")
        
        return await func(*args, **kwargs)
    
    return wrapper


def with_request_queue(request_type: str = "general"):
    """Decorator to queue requests"""
    def decorator(func: Callable[P, T]) -> Callable[P, T]:
        @wraps(func)
        async def wrapper(*args: P.args, **kwargs: P.kwargs) -> T:
            request_id = f"{request_type}_{time.time()}"
            
            try:
                await request_queue.acquire(request_id)
                logger.info(f"Processing {request_type} request. Active: {request_queue.active_count}")
                return await func(*args, **kwargs)
            finally:
                await request_queue.release(request_id)
                logger.info(f"Completed {request_type} request. Active: {request_queue.active_count}")
        
        return wrapper
    
    return decorator


def with_debate_lock(func: Callable[P, T]) -> Callable[P, T]:
    """Decorator to ensure debate operations are synchronized"""
    @wraps(func)
    async def wrapper(*args: P.args, **kwargs: P.kwargs) -> T:
        # Extract debate_id from args or kwargs
        debate_id = kwargs.get('debate_id')
        if not debate_id and len(args) > 1:
            # Assume first arg after self is debate_id
            debate_id = args[1]
        
        if not debate_id:
            raise ValueError("debate_id required for locked operation")
        
        lock = await debate_lock_manager.get_lock(debate_id)
        
        async with lock:
            logger.info(f"Acquired lock for debate {debate_id}")
            try:
                return await func(*args, **kwargs)
            finally:
                logger.info(f"Released lock for debate {debate_id}")
    
    return wrapper


class ConnectionPool:
    """Connection pool for managing client connections"""
    
    def __init__(self, max_connections: int = 100):
        self.max_connections = max_connections
        self.connections: Dict[str, Any] = {}
        self.connection_count = 0
        self._lock = asyncio.Lock()
    
    async def add_connection(self, conn_id: str, connection: Any) -> bool:
        """Add a new connection to the pool"""
        async with self._lock:
            if self.connection_count >= self.max_connections:
                return False
            
            self.connections[conn_id] = connection
            self.connection_count += 1
            return True
    
    async def remove_connection(self, conn_id: str):
        """Remove a connection from the pool"""
        async with self._lock:
            if conn_id in self.connections:
                del self.connections[conn_id]
                self.connection_count -= 1
    
    async def get_connection(self, conn_id: str) -> Optional[Any]:
        """Get a connection by ID"""
        async with self._lock:
            return self.connections.get(conn_id)
    
    @property
    def is_full(self) -> bool:
        """Check if pool is at capacity"""
        return self.connection_count >= self.max_connections


# Global connection pool
connection_pool = ConnectionPool()


class ConcurrencyMetrics:
    """Track concurrency metrics for monitoring"""
    
    def __init__(self):
        self.total_requests = 0
        self.failed_requests = 0
        self.concurrent_peaks: list = []
        self.response_times: list = []
        self._lock = asyncio.Lock()
    
    async def record_request(self, success: bool, response_time: float):
        """Record request metrics"""
        async with self._lock:
            self.total_requests += 1
            if not success:
                self.failed_requests += 1
            
            self.response_times.append(response_time)
            
            # Keep only last 1000 response times
            if len(self.response_times) > 1000:
                self.response_times = self.response_times[-1000:]
    
    async def record_concurrent_peak(self, count: int):
        """Record peak concurrent requests"""
        async with self._lock:
            self.concurrent_peaks.append((time.time(), count))
            
            # Keep only last hour of data
            cutoff = time.time() - 3600
            self.concurrent_peaks = [
                (t, c) for t, c in self.concurrent_peaks if t > cutoff
            ]
    
    @property
    def metrics(self) -> Dict[str, Any]:
        """Get current metrics"""
        avg_response_time = (
            sum(self.response_times) / len(self.response_times)
            if self.response_times else 0
        )
        
        max_concurrent = (
            max(c for _, c in self.concurrent_peaks)
            if self.concurrent_peaks else 0
        )
        
        return {
            "total_requests": self.total_requests,
            "failed_requests": self.failed_requests,
            "success_rate": (
                (self.total_requests - self.failed_requests) / self.total_requests
                if self.total_requests > 0 else 0
            ),
            "avg_response_time_ms": avg_response_time * 1000,
            "max_concurrent_requests": max_concurrent
        }


# Global metrics instance
concurrency_metrics = ConcurrencyMetrics()