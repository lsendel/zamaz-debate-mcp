"""
Performance and load tests for Kiro GitHub Integration.

This module tests performance characteristics including:
- PR processing times
- Concurrent request handling
- Memory usage under load
- Large repository handling
"""

import asyncio
import time
import pytest
import psutil
import gc
from datetime import datetime
from typing import List, Dict, Any
from concurrent.futures import ThreadPoolExecutor
from unittest.mock import Mock, AsyncMock

from ...scripts.core.logging import configure_logging, get_logger
from ...scripts.analyzers.base_analyzer import BaseAnalyzer, AnalyzerContext
from ...scripts.analyzers.security_analyzer import SecurityPatternStrategy
from ...scripts.analyzers.style_analyzer import StyleGuideStrategy, ComplexityStrategy


# Configure logging
configure_logging(level='INFO', format_type='human')
logger = get_logger(__name__)


class PerformanceMetrics:
    """Collect and analyze performance metrics."""
    
    def __init__(self):
        self.start_time = None
        self.end_time = None
        self.memory_start = None
        self.memory_peak = None
        self.operations = []
        
    def start(self):
        """Start performance measurement."""
        self.start_time = time.time()
        self.memory_start = self._get_memory_usage()
        gc.collect()  # Clean baseline
        
    def end(self):
        """End performance measurement."""
        self.end_time = time.time()
        self.memory_peak = self._get_memory_usage()
        
    def record_operation(self, name: str, duration_ms: float, metadata: Dict = None):
        """Record an operation's performance."""
        self.operations.append({
            'name': name,
            'duration_ms': duration_ms,
            'timestamp': datetime.utcnow(),
            'metadata': metadata or {}
        })
        
    def get_summary(self) -> Dict[str, Any]:
        """Get performance summary."""
        if not self.start_time or not self.end_time:
            return {}
            
        total_duration = (self.end_time - self.start_time) * 1000
        memory_used = self.memory_peak - self.memory_start if self.memory_start and self.memory_peak else 0
        
        operation_stats = {}
        for op in self.operations:
            name = op['name']
            if name not in operation_stats:
                operation_stats[name] = {
                    'count': 0,
                    'total_ms': 0,
                    'min_ms': float('inf'),
                    'max_ms': 0
                }
            
            stats = operation_stats[name]
            stats['count'] += 1
            stats['total_ms'] += op['duration_ms']
            stats['min_ms'] = min(stats['min_ms'], op['duration_ms'])
            stats['max_ms'] = max(stats['max_ms'], op['duration_ms'])
        
        # Calculate averages
        for stats in operation_stats.values():
            stats['avg_ms'] = stats['total_ms'] / stats['count'] if stats['count'] > 0 else 0
            
        return {
            'total_duration_ms': total_duration,
            'memory_used_mb': memory_used / (1024 * 1024),
            'operations': operation_stats,
            'total_operations': len(self.operations)
        }
        
    def _get_memory_usage(self) -> int:
        """Get current memory usage in bytes."""
        process = psutil.Process()
        return process.memory_info().rss


@pytest.fixture
def performance_metrics():
    """Create performance metrics collector."""
    return PerformanceMetrics()


class TestPRProcessingPerformance:
    """Test PR processing performance."""
    
    @pytest.mark.asyncio
    async def test_single_pr_processing_time(self, performance_metrics):
        """Test processing time for a single PR."""
        logger.info("=== Testing Single PR Processing Time ===")
        
        performance_metrics.start()
        
        # Simulate PR processing steps
        steps = [
            ("fetch_pr_data", 50),
            ("fetch_pr_files", 150),
            ("analyze_files", 500),
            ("generate_review", 100),
            ("post_review", 200)
        ]
        
        for step_name, duration_ms in steps:
            start = time.time()
            await asyncio.sleep(duration_ms / 1000)  # Simulate work
            actual_duration = (time.time() - start) * 1000
            performance_metrics.record_operation(step_name, actual_duration)
        
        performance_metrics.end()
        summary = performance_metrics.get_summary()
        
        # Verify performance
        assert summary['total_duration_ms'] < 1500  # Should complete in < 1.5s
        assert summary['memory_used_mb'] < 50  # Should use < 50MB
        
        logger.info(f"Total processing time: {summary['total_duration_ms']:.2f}ms")
        logger.info(f"Memory used: {summary['memory_used_mb']:.2f}MB")
    
    @pytest.mark.asyncio
    async def test_concurrent_pr_processing(self, performance_metrics):
        """Test processing multiple PRs concurrently."""
        logger.info("=== Testing Concurrent PR Processing ===")
        
        performance_metrics.start()
        
        async def process_pr(pr_number: int):
            """Simulate PR processing."""
            start = time.time()
            
            # Simulate varying processing times
            base_time = 0.1
            variance = pr_number * 0.01
            await asyncio.sleep(base_time + variance)
            
            duration = (time.time() - start) * 1000
            performance_metrics.record_operation(
                "process_pr",
                duration,
                {"pr_number": pr_number}
            )
            
            return pr_number
        
        # Process 20 PRs concurrently
        pr_numbers = list(range(1, 21))
        tasks = [process_pr(pr) for pr in pr_numbers]
        
        start = time.time()
        results = await asyncio.gather(*tasks)
        total_time = (time.time() - start) * 1000
        
        performance_metrics.end()
        summary = performance_metrics.get_summary()
        
        # Verify concurrent processing
        assert len(results) == 20
        assert total_time < 500  # Should complete in < 500ms (not 20 * 100ms)
        assert summary['operations']['process_pr']['count'] == 20
        
        logger.info(f"Processed {len(results)} PRs in {total_time:.2f}ms")
        logger.info(f"Average time per PR: {summary['operations']['process_pr']['avg_ms']:.2f}ms")
    
    @pytest.mark.asyncio
    async def test_large_pr_performance(self, performance_metrics):
        """Test performance with large PRs."""
        logger.info("=== Testing Large PR Performance ===")
        
        performance_metrics.start()
        
        # Create large file content
        large_file_content = "\n".join([
            f"def function_{i}():"
            f"    # This is line {i}"
            f"    return {i}"
            for i in range(1000)  # 3000 lines
        ])
        
        # Analyze large file
        analyzer = BaseAnalyzer([
            SecurityPatternStrategy(),
            StyleGuideStrategy(),
            ComplexityStrategy()
        ])
        
        start = time.time()
        result = await analyzer.analyze(
            "large_file.py",
            large_file_content
        )
        analysis_time = (time.time() - start) * 1000
        
        performance_metrics.record_operation("analyze_large_file", analysis_time)
        performance_metrics.end()
        
        # Verify performance
        assert analysis_time < 5000  # Should complete in < 5s
        assert len(result.issues) > 0  # Should find some issues
        
        logger.info(f"Analyzed {len(large_file_content.splitlines())} lines in {analysis_time:.2f}ms")
        logger.info(f"Found {len(result.issues)} issues")


class TestAnalyzerPerformance:
    """Test code analyzer performance."""
    
    @pytest.mark.asyncio
    async def test_analyzer_strategy_performance(self, performance_metrics):
        """Test performance of individual analyzer strategies."""
        logger.info("=== Testing Analyzer Strategy Performance ===")
        
        # Test content
        test_code = """
import os
password = "admin123"  # Security issue

def complex_function(data):
    if data:
        for item in data:
            if item > 10:
                for sub in item:
                    if sub:
                        print(sub)
    # More complexity...
    
class VeryLongClassNameThatViolatesNamingConvention:
    pass
"""
        
        strategies = [
            ("Security", SecurityPatternStrategy()),
            ("Style", StyleGuideStrategy()),
            ("Complexity", ComplexityStrategy())
        ]
        
        context = AnalyzerContext(
            file_path="test.py",
            content=test_code,
            language="python"
        )
        
        performance_metrics.start()
        
        for name, strategy in strategies:
            start = time.time()
            issues = await strategy.analyze(context)
            duration = (time.time() - start) * 1000
            
            performance_metrics.record_operation(
                f"analyze_{name.lower()}",
                duration,
                {"issues_found": len(issues)}
            )
            
            logger.info(f"{name} analysis: {duration:.2f}ms, found {len(issues)} issues")
        
        performance_metrics.end()
        summary = performance_metrics.get_summary()
        
        # Each strategy should be fast
        for op_name, stats in summary['operations'].items():
            assert stats['avg_ms'] < 100  # Each should complete in < 100ms
    
    @pytest.mark.asyncio
    async def test_parallel_file_analysis(self, performance_metrics):
        """Test analyzing multiple files in parallel."""
        logger.info("=== Testing Parallel File Analysis ===")
        
        # Create test files
        test_files = []
        for i in range(50):
            test_files.append({
                "name": f"file_{i}.py",
                "content": f"""
def function_{i}():
    password = "secret{i}"  # Issue to find
    return {i}
"""
            })
        
        analyzer = BaseAnalyzer([SecurityPatternStrategy()])
        
        performance_metrics.start()
        
        # Analyze files in parallel
        async def analyze_file(file_data):
            start = time.time()
            result = await analyzer.analyze(file_data["name"], file_data["content"])
            duration = (time.time() - start) * 1000
            return duration, len(result.issues)
        
        tasks = [analyze_file(f) for f in test_files]
        results = await asyncio.gather(*tasks)
        
        performance_metrics.end()
        
        # Calculate statistics
        total_time = sum(r[0] for r in results)
        total_issues = sum(r[1] for r in results)
        avg_time = total_time / len(results)
        
        logger.info(f"Analyzed {len(test_files)} files")
        logger.info(f"Average time per file: {avg_time:.2f}ms")
        logger.info(f"Total issues found: {total_issues}")
        
        # Performance assertions
        assert avg_time < 50  # Average should be < 50ms per file
        assert total_issues > 0  # Should find issues


class TestMemoryPerformance:
    """Test memory usage and leaks."""
    
    @pytest.mark.asyncio
    async def test_memory_usage_under_load(self, performance_metrics):
        """Test memory usage during sustained load."""
        logger.info("=== Testing Memory Usage Under Load ===")
        
        performance_metrics.start()
        initial_memory = performance_metrics._get_memory_usage()
        
        # Process many operations
        for batch in range(10):
            # Create and analyze files
            tasks = []
            for i in range(100):
                content = "x" * 10000  # 10KB per file
                task = self._analyze_content(f"file_{batch}_{i}.py", content)
                tasks.append(task)
            
            await asyncio.gather(*tasks)
            
            # Force garbage collection
            gc.collect()
            
            # Check memory
            current_memory = performance_metrics._get_memory_usage()
            memory_increase_mb = (current_memory - initial_memory) / (1024 * 1024)
            
            logger.info(f"Batch {batch + 1}: Memory increase: {memory_increase_mb:.2f}MB")
            
            # Memory should not grow indefinitely
            assert memory_increase_mb < 100  # Should stay under 100MB increase
        
        performance_metrics.end()
        summary = performance_metrics.get_summary()
        
        logger.info(f"Total memory used: {summary['memory_used_mb']:.2f}MB")
    
    async def _analyze_content(self, filename: str, content: str):
        """Helper to analyze content."""
        analyzer = BaseAnalyzer([SecurityPatternStrategy()])
        return await analyzer.analyze(filename, content)
    
    def test_memory_cleanup(self):
        """Test that memory is properly cleaned up."""
        logger.info("=== Testing Memory Cleanup ===")
        
        # Get baseline memory
        gc.collect()
        baseline_memory = psutil.Process().memory_info().rss
        
        # Create large objects
        large_data = []
        for i in range(100):
            large_data.append("x" * 100000)  # 100KB strings
        
        # Check memory increased
        memory_with_data = psutil.Process().memory_info().rss
        assert memory_with_data > baseline_memory
        
        # Clear references
        large_data = None
        gc.collect()
        
        # Check memory decreased
        final_memory = psutil.Process().memory_info().rss
        memory_diff_mb = (final_memory - baseline_memory) / (1024 * 1024)
        
        logger.info(f"Memory difference after cleanup: {memory_diff_mb:.2f}MB")
        assert memory_diff_mb < 10  # Should release most memory


class TestLoadTesting:
    """Load testing for the system."""
    
    @pytest.mark.asyncio
    async def test_webhook_burst_load(self, performance_metrics):
        """Test handling burst of webhooks."""
        logger.info("=== Testing Webhook Burst Load ===")
        
        performance_metrics.start()
        
        # Simulate webhook processing
        async def process_webhook(webhook_id: int):
            start = time.time()
            
            # Simulate webhook validation and queuing
            await asyncio.sleep(0.01)  # 10ms per webhook
            
            duration = (time.time() - start) * 1000
            return webhook_id, duration
        
        # Send 1000 webhooks in burst
        webhook_count = 1000
        tasks = [process_webhook(i) for i in range(webhook_count)]
        
        start = time.time()
        results = await asyncio.gather(*tasks)
        total_time = (time.time() - start) * 1000
        
        performance_metrics.end()
        
        # Calculate throughput
        throughput = webhook_count / (total_time / 1000)  # webhooks per second
        avg_latency = sum(r[1] for r in results) / len(results)
        
        logger.info(f"Processed {webhook_count} webhooks in {total_time:.2f}ms")
        logger.info(f"Throughput: {throughput:.2f} webhooks/second")
        logger.info(f"Average latency: {avg_latency:.2f}ms")
        
        # Performance requirements
        assert throughput > 100  # Should handle > 100 webhooks/second
        assert avg_latency < 50  # Average latency < 50ms
    
    @pytest.mark.asyncio
    async def test_sustained_load(self, performance_metrics):
        """Test system under sustained load."""
        logger.info("=== Testing Sustained Load ===")
        
        performance_metrics.start()
        
        # Run for 30 seconds with constant load
        duration_seconds = 30
        requests_per_second = 50
        
        async def request_generator():
            """Generate requests at constant rate."""
            interval = 1.0 / requests_per_second
            request_count = 0
            
            start_time = time.time()
            while time.time() - start_time < duration_seconds:
                await self._process_request(request_count)
                request_count += 1
                await asyncio.sleep(interval)
            
            return request_count
        
        total_requests = await request_generator()
        
        performance_metrics.end()
        summary = performance_metrics.get_summary()
        
        logger.info(f"Processed {total_requests} requests over {duration_seconds}s")
        logger.info(f"Memory usage: {summary['memory_used_mb']:.2f}MB")
        
        # System should remain stable
        assert summary['memory_used_mb'] < 200  # Memory should stay reasonable
        assert total_requests > duration_seconds * requests_per_second * 0.95  # 95% of target
    
    async def _process_request(self, request_id: int):
        """Simulate processing a request."""
        # Random work between 5-15ms
        work_time = 0.005 + (request_id % 10) * 0.001
        await asyncio.sleep(work_time)