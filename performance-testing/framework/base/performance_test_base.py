"""
Base classes and utilities for performance testing framework.

This module provides the foundation for comprehensive performance testing
of the MCP debate system including metrics collection, analysis, and reporting.
"""

import asyncio
import gc
import json
import logging
import multiprocessing
import os
import psutil
import time
import traceback
from abc import ABC, abstractmethod
from contextlib import asynccontextmanager
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Dict, List, Optional, Union, Callable, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Event, Lock

import aiohttp
import pytest
from prometheus_client import Counter, Histogram, Gauge, CollectorRegistry, generate_latest


@dataclass
class PerformanceMetrics:
    """Container for performance metrics."""
    
    # Timing metrics
    start_time: float = 0.0
    end_time: float = 0.0
    duration_ms: float = 0.0
    
    # Memory metrics
    memory_start_mb: float = 0.0
    memory_end_mb: float = 0.0
    memory_peak_mb: float = 0.0
    memory_delta_mb: float = 0.0
    
    # CPU metrics
    cpu_start_percent: float = 0.0
    cpu_end_percent: float = 0.0
    cpu_peak_percent: float = 0.0
    cpu_avg_percent: float = 0.0
    
    # Network metrics
    network_bytes_sent: int = 0
    network_bytes_recv: int = 0
    
    # System metrics
    thread_count: int = 0
    open_files: int = 0
    
    # Test-specific metrics
    operations_count: int = 0
    operations_per_second: float = 0.0
    success_rate: float = 0.0
    error_count: int = 0
    
    # Custom metrics
    custom_metrics: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        """Convert metrics to dictionary."""
        return asdict(self)


@dataclass
class TestConfiguration:
    """Configuration for performance tests."""
    
    # Test execution parameters
    duration_seconds: int = 60
    warmup_seconds: int = 10
    cooldown_seconds: int = 5
    
    # Load parameters
    concurrent_users: int = 10
    requests_per_second: float = 100.0
    batch_size: int = 100
    
    # System parameters
    max_memory_mb: int = 512
    max_cpu_percent: float = 80.0
    max_response_time_ms: int = 5000
    
    # Database parameters
    db_connections: int = 10
    db_pool_size: int = 20
    
    # Monitoring parameters
    metrics_interval_seconds: float = 1.0
    enable_detailed_logging: bool = False
    enable_prometheus_metrics: bool = True
    
    # Test environment
    test_data_size: int = 1000
    cleanup_after_test: bool = True


class MetricsCollector:
    """Collects and aggregates performance metrics."""
    
    def __init__(self, config: TestConfiguration):
        self.config = config
        self.metrics = PerformanceMetrics()
        self.operations: List[Dict[str, Any]] = []
        self.error_log: List[Dict[str, Any]] = []
        self.monitoring_active = False
        self.monitoring_thread = None
        self.cpu_samples: List[float] = []
        self.memory_samples: List[float] = []
        self.lock = Lock()
        
        # Prometheus metrics
        if config.enable_prometheus_metrics:
            self.registry = CollectorRegistry()
            self.request_counter = Counter(
                'test_requests_total',
                'Total test requests',
                ['method', 'endpoint', 'status'],
                registry=self.registry
            )
            self.request_duration = Histogram(
                'test_request_duration_seconds',
                'Request duration in seconds',
                ['method', 'endpoint'],
                registry=self.registry
            )
            self.memory_gauge = Gauge(
                'test_memory_usage_mb',
                'Memory usage in MB',
                registry=self.registry
            )
            self.cpu_gauge = Gauge(
                'test_cpu_usage_percent',
                'CPU usage percentage',
                registry=self.registry
            )
    
    def start_monitoring(self):
        """Start system monitoring."""
        self.metrics.start_time = time.time()
        self.metrics.memory_start_mb = self._get_memory_usage_mb()
        self.metrics.cpu_start_percent = self._get_cpu_usage_percent()
        
        if self.config.enable_prometheus_metrics:
            self.monitoring_active = True
            self.monitoring_thread = threading.Thread(target=self._monitor_system)
            self.monitoring_thread.daemon = True
            self.monitoring_thread.start()
        
        gc.collect()  # Clean baseline
    
    def stop_monitoring(self):
        """Stop system monitoring."""
        self.monitoring_active = False
        if self.monitoring_thread:
            self.monitoring_thread.join(timeout=1.0)
        
        self.metrics.end_time = time.time()
        self.metrics.duration_ms = (self.metrics.end_time - self.metrics.start_time) * 1000
        self.metrics.memory_end_mb = self._get_memory_usage_mb()
        self.metrics.memory_delta_mb = self.metrics.memory_end_mb - self.metrics.memory_start_mb
        self.metrics.cpu_end_percent = self._get_cpu_usage_percent()
        
        # Calculate aggregated metrics
        if self.cpu_samples:
            self.metrics.cpu_avg_percent = sum(self.cpu_samples) / len(self.cpu_samples)
            self.metrics.cpu_peak_percent = max(self.cpu_samples)
        
        if self.memory_samples:
            self.metrics.memory_peak_mb = max(self.memory_samples)
        
        if self.operations:
            self.metrics.operations_count = len(self.operations)
            if self.metrics.duration_ms > 0:
                self.metrics.operations_per_second = (
                    self.metrics.operations_count / (self.metrics.duration_ms / 1000)
                )
            
            success_count = sum(1 for op in self.operations if op.get('success', False))
            self.metrics.success_rate = success_count / self.metrics.operations_count if self.metrics.operations_count > 0 else 0
            self.metrics.error_count = len(self.error_log)
    
    def record_operation(self, name: str, duration_ms: float, success: bool = True, metadata: Optional[Dict] = None):
        """Record an operation's performance."""
        with self.lock:
            operation = {
                'name': name,
                'duration_ms': duration_ms,
                'success': success,
                'timestamp': time.time(),
                'metadata': metadata or {}
            }
            self.operations.append(operation)
            
            if self.config.enable_prometheus_metrics:
                status = 'success' if success else 'error'
                self.request_counter.labels(
                    method=metadata.get('method', 'unknown'),
                    endpoint=name,
                    status=status
                ).inc()
                
                self.request_duration.labels(
                    method=metadata.get('method', 'unknown'),
                    endpoint=name
                ).observe(duration_ms / 1000)
    
    def record_error(self, error: Exception, context: str = ""):
        """Record an error occurrence."""
        with self.lock:
            error_entry = {
                'error': str(error),
                'type': type(error).__name__,
                'context': context,
                'timestamp': time.time(),
                'traceback': traceback.format_exc()
            }
            self.error_log.append(error_entry)
    
    def _monitor_system(self):
        """Monitor system resources in background."""
        while self.monitoring_active:
            try:
                cpu_percent = self._get_cpu_usage_percent()
                memory_mb = self._get_memory_usage_mb()
                
                with self.lock:
                    self.cpu_samples.append(cpu_percent)
                    self.memory_samples.append(memory_mb)
                
                if self.config.enable_prometheus_metrics:
                    self.cpu_gauge.set(cpu_percent)
                    self.memory_gauge.set(memory_mb)
                
                time.sleep(self.config.metrics_interval_seconds)
            except Exception as e:
                logging.error(f"Error in system monitoring: {e}")
    
    def _get_memory_usage_mb(self) -> float:
        """Get current memory usage in MB."""
        process = psutil.Process()
        return process.memory_info().rss / (1024 * 1024)
    
    def _get_cpu_usage_percent(self) -> float:
        """Get current CPU usage percentage."""
        return psutil.cpu_percent(interval=0.1)
    
    def get_prometheus_metrics(self) -> str:
        """Get Prometheus-formatted metrics."""
        if not self.config.enable_prometheus_metrics:
            return ""
        return generate_latest(self.registry).decode('utf-8')
    
    def get_summary_report(self) -> Dict[str, Any]:
        """Generate comprehensive summary report."""
        operation_stats = self._calculate_operation_stats()
        
        return {
            'test_summary': {
                'duration_seconds': self.metrics.duration_ms / 1000,
                'operations_count': self.metrics.operations_count,
                'operations_per_second': self.metrics.operations_per_second,
                'success_rate': self.metrics.success_rate,
                'error_count': self.metrics.error_count
            },
            'performance_metrics': self.metrics.to_dict(),
            'operation_stats': operation_stats,
            'error_summary': self._get_error_summary(),
            'resource_usage': {
                'memory_peak_mb': self.metrics.memory_peak_mb,
                'memory_delta_mb': self.metrics.memory_delta_mb,
                'cpu_peak_percent': self.metrics.cpu_peak_percent,
                'cpu_avg_percent': self.metrics.cpu_avg_percent
            }
        }
    
    def _calculate_operation_stats(self) -> Dict[str, Any]:
        """Calculate statistics for each operation type."""
        stats = {}
        
        for operation in self.operations:
            name = operation['name']
            if name not in stats:
                stats[name] = {
                    'count': 0,
                    'total_duration_ms': 0,
                    'min_duration_ms': float('inf'),
                    'max_duration_ms': 0,
                    'success_count': 0,
                    'error_count': 0
                }
            
            stat = stats[name]
            stat['count'] += 1
            stat['total_duration_ms'] += operation['duration_ms']
            stat['min_duration_ms'] = min(stat['min_duration_ms'], operation['duration_ms'])
            stat['max_duration_ms'] = max(stat['max_duration_ms'], operation['duration_ms'])
            
            if operation['success']:
                stat['success_count'] += 1
            else:
                stat['error_count'] += 1
        
        # Calculate averages and percentiles
        for stat in stats.values():
            if stat['count'] > 0:
                stat['avg_duration_ms'] = stat['total_duration_ms'] / stat['count']
                stat['success_rate'] = stat['success_count'] / stat['count']
        
        return stats
    
    def _get_error_summary(self) -> Dict[str, Any]:
        """Get summary of errors encountered."""
        error_types = {}
        
        for error in self.error_log:
            error_type = error['type']
            if error_type not in error_types:
                error_types[error_type] = {
                    'count': 0,
                    'examples': []
                }
            
            error_types[error_type]['count'] += 1
            if len(error_types[error_type]['examples']) < 3:
                error_types[error_type]['examples'].append({
                    'error': error['error'],
                    'context': error['context']
                })
        
        return {
            'total_errors': len(self.error_log),
            'error_types': error_types
        }


class PerformanceTestBase(ABC):
    """Base class for performance tests."""
    
    def __init__(self, config: TestConfiguration):
        self.config = config
        self.metrics = MetricsCollector(config)
        self.logger = logging.getLogger(self.__class__.__name__)
        self.test_data_dir = Path("test_data")
        self.test_data_dir.mkdir(exist_ok=True)
    
    @abstractmethod
    async def setup_test(self):
        """Setup test environment and data."""
        pass
    
    @abstractmethod
    async def run_test(self):
        """Execute the actual test."""
        pass
    
    @abstractmethod
    async def cleanup_test(self):
        """Cleanup test environment."""
        pass
    
    async def execute_test(self) -> Dict[str, Any]:
        """Execute complete test lifecycle."""
        self.logger.info(f"Starting performance test: {self.__class__.__name__}")
        
        try:
            # Setup
            await self.setup_test()
            
            # Warmup
            if self.config.warmup_seconds > 0:
                self.logger.info(f"Warming up for {self.config.warmup_seconds} seconds...")
                await self._warmup()
            
            # Start monitoring
            self.metrics.start_monitoring()
            
            # Run test
            await self.run_test()
            
            # Stop monitoring
            self.metrics.stop_monitoring()
            
            # Cooldown
            if self.config.cooldown_seconds > 0:
                self.logger.info(f"Cooling down for {self.config.cooldown_seconds} seconds...")
                await asyncio.sleep(self.config.cooldown_seconds)
            
            # Generate report
            report = self.metrics.get_summary_report()
            
            # Validate results
            self._validate_performance_requirements(report)
            
            return report
            
        except Exception as e:
            self.metrics.record_error(e, "test_execution")
            raise
        finally:
            if self.config.cleanup_after_test:
                await self.cleanup_test()
    
    async def _warmup(self):
        """Perform warmup operations."""
        # Default warmup - can be overridden
        warmup_duration = self.config.warmup_seconds
        warmup_ops = max(1, int(warmup_duration * 10))  # 10 ops per second
        
        for i in range(warmup_ops):
            try:
                await self._warmup_operation()
                await asyncio.sleep(0.1)
            except Exception as e:
                self.logger.debug(f"Warmup operation failed: {e}")
    
    async def _warmup_operation(self):
        """Single warmup operation - override in subclasses."""
        await asyncio.sleep(0.01)  # Default no-op
    
    def _validate_performance_requirements(self, report: Dict[str, Any]):
        """Validate that performance requirements are met."""
        metrics = report['performance_metrics']
        
        # Memory validation
        if metrics['memory_peak_mb'] > self.config.max_memory_mb:
            raise AssertionError(
                f"Memory usage {metrics['memory_peak_mb']:.1f}MB exceeds limit {self.config.max_memory_mb}MB"
            )
        
        # CPU validation
        if metrics['cpu_peak_percent'] > self.config.max_cpu_percent:
            raise AssertionError(
                f"CPU usage {metrics['cpu_peak_percent']:.1f}% exceeds limit {self.config.max_cpu_percent}%"
            )
        
        # Response time validation
        operation_stats = report['operation_stats']
        for op_name, stats in operation_stats.items():
            if stats['max_duration_ms'] > self.config.max_response_time_ms:
                raise AssertionError(
                    f"Operation {op_name} max response time {stats['max_duration_ms']:.1f}ms exceeds limit {self.config.max_response_time_ms}ms"
                )
    
    def save_test_data(self, filename: str, data: Any):
        """Save test data to file."""
        filepath = self.test_data_dir / filename
        
        if isinstance(data, (dict, list)):
            with open(filepath, 'w') as f:
                json.dump(data, f, indent=2, default=str)
        else:
            with open(filepath, 'w') as f:
                f.write(str(data))
    
    def load_test_data(self, filename: str) -> Any:
        """Load test data from file."""
        filepath = self.test_data_dir / filename
        
        if not filepath.exists():
            return None
        
        try:
            with open(filepath, 'r') as f:
                return json.load(f)
        except json.JSONDecodeError:
            with open(filepath, 'r') as f:
                return f.read()


class LoadTestRunner:
    """Runs load tests with different patterns."""
    
    def __init__(self, config: TestConfiguration):
        self.config = config
        self.logger = logging.getLogger(self.__class__.__name__)
    
    async def run_constant_load(self, test_func: Callable, duration_seconds: int = None) -> List[Dict[str, Any]]:
        """Run test with constant load pattern."""
        duration = duration_seconds or self.config.duration_seconds
        rps = self.config.requests_per_second
        
        self.logger.info(f"Starting constant load test: {rps} RPS for {duration}s")
        
        results = []
        start_time = time.time()
        request_interval = 1.0 / rps
        
        while time.time() - start_time < duration:
            request_start = time.time()
            
            try:
                result = await test_func()
                success = True
                error = None
            except Exception as e:
                result = None
                success = False
                error = str(e)
            
            request_duration = (time.time() - request_start) * 1000
            
            results.append({
                'timestamp': request_start,
                'duration_ms': request_duration,
                'success': success,
                'error': error,
                'result': result
            })
            
            # Maintain request rate
            elapsed = time.time() - request_start
            sleep_time = max(0, request_interval - elapsed)
            if sleep_time > 0:
                await asyncio.sleep(sleep_time)
        
        return results
    
    async def run_burst_load(self, test_func: Callable, burst_size: int = None) -> List[Dict[str, Any]]:
        """Run test with burst load pattern."""
        burst_size = burst_size or self.config.batch_size
        
        self.logger.info(f"Starting burst load test: {burst_size} concurrent requests")
        
        async def single_request():
            start_time = time.time()
            try:
                result = await test_func()
                success = True
                error = None
            except Exception as e:
                result = None
                success = False
                error = str(e)
            
            return {
                'timestamp': start_time,
                'duration_ms': (time.time() - start_time) * 1000,
                'success': success,
                'error': error,
                'result': result
            }
        
        tasks = [single_request() for _ in range(burst_size)]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        # Handle exceptions in gather
        processed_results = []
        for result in results:
            if isinstance(result, Exception):
                processed_results.append({
                    'timestamp': time.time(),
                    'duration_ms': 0,
                    'success': False,
                    'error': str(result),
                    'result': None
                })
            else:
                processed_results.append(result)
        
        return processed_results
    
    async def run_ramp_load(self, test_func: Callable, max_rps: int = None, ramp_duration: int = None) -> List[Dict[str, Any]]:
        """Run test with ramping load pattern."""
        max_rps = max_rps or self.config.requests_per_second
        ramp_duration = ramp_duration or self.config.duration_seconds
        
        self.logger.info(f"Starting ramp load test: 0 to {max_rps} RPS over {ramp_duration}s")
        
        results = []
        start_time = time.time()
        
        while time.time() - start_time < ramp_duration:
            elapsed = time.time() - start_time
            current_rps = (elapsed / ramp_duration) * max_rps
            
            if current_rps > 0:
                request_interval = 1.0 / current_rps
                
                request_start = time.time()
                
                try:
                    result = await test_func()
                    success = True
                    error = None
                except Exception as e:
                    result = None
                    success = False
                    error = str(e)
                
                request_duration = (time.time() - request_start) * 1000
                
                results.append({
                    'timestamp': request_start,
                    'duration_ms': request_duration,
                    'success': success,
                    'error': error,
                    'result': result,
                    'current_rps': current_rps
                })
                
                # Maintain current request rate
                elapsed_request = time.time() - request_start
                sleep_time = max(0, request_interval - elapsed_request)
                if sleep_time > 0:
                    await asyncio.sleep(sleep_time)
            else:
                await asyncio.sleep(0.1)
        
        return results


@asynccontextmanager
async def http_client_session(timeout: int = 30) -> aiohttp.ClientSession:
    """Create HTTP client session with optimal settings."""
    connector = aiohttp.TCPConnector(
        limit=100,
        limit_per_host=30,
        ttl_dns_cache=300,
        use_dns_cache=True,
        keepalive_timeout=30,
        enable_cleanup_closed=True
    )
    
    timeout_config = aiohttp.ClientTimeout(total=timeout)
    
    async with aiohttp.ClientSession(
        connector=connector,
        timeout=timeout_config
    ) as session:
        yield session


# Test utilities
def generate_test_data(size: int, pattern: str = "random") -> List[Dict[str, Any]]:
    """Generate test data for performance tests."""
    import random
    import string
    
    data = []
    
    for i in range(size):
        if pattern == "random":
            data.append({
                'id': i,
                'name': ''.join(random.choices(string.ascii_letters, k=10)),
                'value': random.randint(1, 1000),
                'timestamp': datetime.utcnow().isoformat(),
                'data': ''.join(random.choices(string.ascii_letters + string.digits, k=100))
            })
        elif pattern == "sequential":
            data.append({
                'id': i,
                'name': f'item_{i:06d}',
                'value': i,
                'timestamp': datetime.utcnow().isoformat(),
                'data': f'data_{i}' * 20
            })
    
    return data


def measure_time(func: Callable) -> Callable:
    """Decorator to measure function execution time."""
    def wrapper(*args, **kwargs):
        start = time.time()
        try:
            result = func(*args, **kwargs)
            return result
        finally:
            duration = (time.time() - start) * 1000
            logging.info(f"{func.__name__} took {duration:.2f}ms")
    
    return wrapper


def measure_async_time(func: Callable) -> Callable:
    """Decorator to measure async function execution time."""
    async def wrapper(*args, **kwargs):
        start = time.time()
        try:
            result = await func(*args, **kwargs)
            return result
        finally:
            duration = (time.time() - start) * 1000
            logging.info(f"{func.__name__} took {duration:.2f}ms")
    
    return wrapper


# Export main classes
__all__ = [
    'PerformanceMetrics',
    'TestConfiguration',
    'MetricsCollector',
    'PerformanceTestBase',
    'LoadTestRunner',
    'http_client_session',
    'generate_test_data',
    'measure_time',
    'measure_async_time'
]