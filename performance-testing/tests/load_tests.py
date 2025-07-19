"""
Concurrent processing and load testing suite.

This module provides comprehensive load testing capabilities including:
- Stress testing with high concurrency
- Volume testing with large datasets
- Spike testing with sudden load increases
- Endurance testing for long-running operations
- Scalability testing across different loads
"""

import asyncio
import random
import time
from collections.abc import Callable
from dataclasses import dataclass, field
from threading import Event, Lock, Semaphore
from typing import Any

import aiohttp

from ..framework.base import (
    PerformanceTestBase,
    TestConfiguration,
    generate_test_data,
)


@dataclass
class LoadTestScenario:
    """Configuration for a load test scenario."""

    name: str
    description: str

    # Load parameters
    initial_users: int = 1
    max_users: int = 100
    ramp_up_duration: int = 60  # seconds
    steady_state_duration: int = 300  # seconds
    ramp_down_duration: int = 60  # seconds

    # Request parameters
    requests_per_user: int = 100
    think_time_min: float = 0.1  # seconds
    think_time_max: float = 2.0  # seconds

    # Failure conditions
    max_error_rate: float = 0.05  # 5%
    max_response_time: float = 5.0  # seconds
    max_memory_mb: int = 1024
    max_cpu_percent: float = 80.0

    # Test data
    test_data_size: int = 1000
    data_distribution: str = "uniform"  # uniform, normal, exponential

    # Custom parameters
    custom_params: dict[str, Any] = field(default_factory=dict)


class ConcurrentLoadTest(PerformanceTestBase):
    """Base class for concurrent load tests."""

    def __init__(self, config: TestConfiguration, scenario: LoadTestScenario):
        super().__init__(config)
        self.scenario = scenario
        self.active_users = 0
        self.user_lock = Lock()
        self.stop_event = Event()
        self.results = []
        self.results_lock = Lock()

        # Rate limiting
        self.rate_limiter = Semaphore(scenario.max_users)

        # Test data
        self.test_data = []
        self.data_index = 0
        self.data_lock = Lock()

    async def setup_test(self):
        """Setup load test environment."""
        self.logger.info(f"Setting up load test: {self.scenario.name}")

        # Generate test data
        self.test_data = generate_test_data(self.scenario.test_data_size, self.scenario.data_distribution)

        # Custom setup
        await self.setup_load_test()

    async def run_test(self):
        """Execute the complete load test scenario."""
        self.logger.info(f"Starting load test: {self.scenario.name}")

        # Phase 1: Ramp up
        await self._ramp_up_phase()

        # Phase 2: Steady state
        await self._steady_state_phase()

        # Phase 3: Ramp down
        await self._ramp_down_phase()

        # Analyze results
        await self._analyze_results()

    async def cleanup_test(self):
        """Cleanup load test environment."""
        self.stop_event.set()
        await self.cleanup_load_test()

    async def setup_load_test(self):
        """Custom setup for specific load test - override in subclasses."""
        pass

    async def cleanup_load_test(self):
        """Custom cleanup for specific load test - override in subclasses."""
        pass

    async def user_session(self, user_id: int) -> dict[str, Any]:
        """Simulate a user session - override in subclasses."""
        return {"user_id": user_id, "operations": 0, "errors": 0}

    async def _ramp_up_phase(self):
        """Gradually increase load to target level."""
        self.logger.info(f"Ramp up phase: {self.scenario.ramp_up_duration}s")

        time.time()
        user_spawn_interval = self.scenario.ramp_up_duration / self.scenario.max_users

        tasks = []
        for user_id in range(self.scenario.max_users):
            # Calculate when this user should start
            start_delay = user_id * user_spawn_interval

            # Create user task
            task = asyncio.create_task(self._user_lifecycle(user_id, start_delay))
            tasks.append(task)

        # Wait for ramp up to complete
        await asyncio.sleep(self.scenario.ramp_up_duration)

        self.logger.info(f"Ramp up completed: {self.active_users} active users")

    async def _steady_state_phase(self):
        """Maintain steady load."""
        self.logger.info(f"Steady state phase: {self.scenario.steady_state_duration}s")

        time.time()

        # Monitor system during steady state
        monitoring_task = asyncio.create_task(self._monitor_steady_state())

        # Wait for steady state to complete
        await asyncio.sleep(self.scenario.steady_state_duration)

        # Stop monitoring
        monitoring_task.cancel()

        self.logger.info("Steady state completed")

    async def _ramp_down_phase(self):
        """Gradually decrease load."""
        self.logger.info(f"Ramp down phase: {self.scenario.ramp_down_duration}s")

        # Signal users to stop
        self.stop_event.set()

        # Wait for ramp down to complete
        await asyncio.sleep(self.scenario.ramp_down_duration)

        self.logger.info("Ramp down completed")

    async def _user_lifecycle(self, user_id: int, start_delay: float):
        """Manage the lifecycle of a single user."""
        # Wait for user's start time
        await asyncio.sleep(start_delay)

        # Acquire rate limit
        await self.rate_limiter.acquire()

        try:
            with self.user_lock:
                self.active_users += 1

            # Run user session
            session_result = await self.user_session(user_id)

            # Record results
            with self.results_lock:
                self.results.append({"user_id": user_id, "start_time": time.time(), "session_result": session_result})

        finally:
            self.rate_limiter.release()

            with self.user_lock:
                self.active_users -= 1

    async def _monitor_steady_state(self):
        """Monitor system during steady state phase."""
        while not self.stop_event.is_set():
            try:
                # Check system resources
                current_memory = self.metrics._get_memory_usage_mb()
                current_cpu = self.metrics._get_cpu_usage_percent()

                # Record metrics
                self.metrics.record_operation(
                    "steady_state_monitor",
                    0,
                    True,
                    {
                        "active_users": self.active_users,
                        "memory_mb": current_memory,
                        "cpu_percent": current_cpu,
                        "timestamp": time.time(),
                    },
                )

                # Check failure conditions
                if current_memory > self.scenario.max_memory_mb:
                    self.logger.warning(
                        f"Memory usage {current_memory:.1f}MB exceeds limit {self.scenario.max_memory_mb}MB"
                    )

                if current_cpu > self.scenario.max_cpu_percent:
                    self.logger.warning(f"CPU usage {current_cpu:.1f}% exceeds limit {self.scenario.max_cpu_percent}%")

                await asyncio.sleep(self.config.metrics_interval_seconds)

            except asyncio.CancelledError:
                break
            except Exception as e:
                self.logger.error(f"Error in steady state monitoring: {e}")

    async def _analyze_results(self):
        """Analyze load test results."""
        if not self.results:
            return

        # Calculate statistics
        total_users = len(self.results)
        total_operations = sum(r["session_result"].get("operations", 0) for r in self.results)
        total_errors = sum(r["session_result"].get("errors", 0) for r in self.results)

        error_rate = total_errors / total_operations if total_operations > 0 else 0

        # Record final metrics
        self.metrics.record_operation(
            "load_test_summary",
            0,
            True,
            {
                "scenario": self.scenario.name,
                "total_users": total_users,
                "total_operations": total_operations,
                "total_errors": total_errors,
                "error_rate": error_rate,
                "max_concurrent_users": self.scenario.max_users,
            },
        )

        # Validate against scenario limits
        if error_rate > self.scenario.max_error_rate:
            self.logger.error(f"Error rate {error_rate:.3f} exceeds limit {self.scenario.max_error_rate:.3f}")

        self.logger.info(
            f"Load test completed: {total_users} users, {total_operations} operations, {error_rate:.3f} error rate"
        )

    def get_next_test_data(self) -> dict[str, Any]:
        """Get next test data item in round-robin fashion."""
        with self.data_lock:
            if not self.test_data:
                return {}

            data = self.test_data[self.data_index]
            self.data_index = (self.data_index + 1) % len(self.test_data)
            return data

    async def think_time(self):
        """Simulate user think time."""
        think_duration = random.uniform(self.scenario.think_time_min, self.scenario.think_time_max)  # noqa: S311 (using random for test simulation)
        await asyncio.sleep(think_duration)


class HTTPLoadTest(ConcurrentLoadTest):
    """Load test for HTTP APIs."""

    def __init__(
        self, config: TestConfiguration, scenario: LoadTestScenario, api_base_url: str, endpoints: list[dict[str, Any]]
    ):
        super().__init__(config, scenario)
        self.api_base_url = api_base_url
        self.endpoints = endpoints
        self.session = None

    async def setup_load_test(self):
        """Setup HTTP load test."""
        self.session = aiohttp.ClientSession()

    async def cleanup_load_test(self):
        """Cleanup HTTP load test."""
        if self.session:
            await self.session.close()

    async def user_session(self, user_id: int) -> dict[str, Any]:
        """Simulate HTTP user session."""
        operations = 0
        errors = 0
        response_times = []

        for _request_num in range(self.scenario.requests_per_user):
            if self.stop_event.is_set():
                break

            # Select endpoint
            endpoint = random.choice(self.endpoints)  # noqa: S311 (using random for test simulation)

            # Get test data
            test_data = self.get_next_test_data()

            # Make request
            start_time = time.time()

            try:
                async with self.session.request(
                    endpoint["method"],
                    f"{self.api_base_url}{endpoint['path']}",
                    json=test_data if endpoint["method"] in ["POST", "PUT"] else None,
                    params=test_data if endpoint["method"] == "GET" else None,
                ) as response:
                    await response.text()  # Consume response
                    response_time = (time.time() - start_time) * 1000
                    response_times.append(response_time)

                    if response.status < 400:
                        operations += 1
                    else:
                        errors += 1

                    # Record individual request
                    self.metrics.record_operation(
                        f"http_request_{endpoint['method'].lower()}",
                        response_time,
                        response.status < 400,
                        {"user_id": user_id, "endpoint": endpoint["path"], "status_code": response.status},
                    )

            except Exception as e:
                errors += 1
                response_time = (time.time() - start_time) * 1000
                self.metrics.record_operation(
                    f"http_request_{endpoint['method'].lower()}",
                    response_time,
                    False,
                    {"user_id": user_id, "endpoint": endpoint["path"], "error": str(e)},
                )

            # Think time
            await self.think_time()

        return {
            "operations": operations,
            "errors": errors,
            "avg_response_time": sum(response_times) / len(response_times) if response_times else 0,
            "max_response_time": max(response_times) if response_times else 0,
        }


class DatabaseLoadTest(ConcurrentLoadTest):
    """Load test for database operations."""

    def __init__(
        self, config: TestConfiguration, scenario: LoadTestScenario, db_url: str, operations: list[dict[str, Any]]
    ):
        super().__init__(config, scenario)
        self.db_url = db_url
        self.operations = operations
        self.connection_pool = None

    async def setup_load_test(self):
        """Setup database load test."""
        import asyncpg

        # Create connection pool
        self.connection_pool = await asyncpg.create_pool(self.db_url, min_size=5, max_size=self.scenario.max_users)

    async def cleanup_load_test(self):
        """Cleanup database load test."""
        if self.connection_pool:
            await self.connection_pool.close()

    async def user_session(self, user_id: int) -> dict[str, Any]:
        """Simulate database user session."""
        operations = 0
        errors = 0

        async with self.connection_pool.acquire() as conn:
            for _op_num in range(self.scenario.requests_per_user):
                if self.stop_event.is_set():
                    break

                # Select operation
                operation = random.choice(self.operations)  # noqa: S311 (using random for test simulation)

                # Get test data
                test_data = self.get_next_test_data()

                start_time = time.time()

                try:
                    if operation["type"] == "select":
                        await conn.fetch(operation["query"], *operation.get("params", []))
                    elif operation["type"] == "insert":
                        await conn.execute(operation["query"], *self._prepare_insert_params(test_data))
                    elif operation["type"] == "update":
                        await conn.execute(operation["query"], *self._prepare_update_params(test_data))
                    elif operation["type"] == "delete":
                        await conn.execute(operation["query"], *operation.get("params", []))

                    duration = (time.time() - start_time) * 1000
                    operations += 1

                    self.metrics.record_operation(
                        f"db_operation_{operation['type']}",
                        duration,
                        True,
                        {"user_id": user_id, "operation": operation["name"]},
                    )

                except Exception as e:
                    errors += 1
                    duration = (time.time() - start_time) * 1000

                    self.metrics.record_operation(
                        f"db_operation_{operation['type']}",
                        duration,
                        False,
                        {"user_id": user_id, "operation": operation["name"], "error": str(e)},
                    )

                # Think time
                await self.think_time()

        return {"operations": operations, "errors": errors}

    def _prepare_insert_params(self, test_data: dict[str, Any]) -> list[Any]:
        """Prepare parameters for insert operation."""
        return [
            test_data.get("name", f"user_data_{random.randint(1000, 9999)}"),  # noqa: S311 (using random for test simulation)
            test_data.get("value", random.randint(1, 1000)),  # noqa: S311 (using random for test simulation)
            test_data.get("data", f"test_data_{random.randint(1000, 9999)}"),  # noqa: S311 (using random for test simulation)
        ]

    def _prepare_update_params(self, test_data: dict[str, Any]) -> list[Any]:
        """Prepare parameters for update operation."""
        return [test_data.get("value", random.randint(1, 1000)), test_data.get("id", random.randint(1, 1000))]  # noqa: S311 (using random for test simulation)


class SpikeLoadTest(PerformanceTestBase):
    """Test system behavior under sudden load spikes."""

    def __init__(self, config: TestConfiguration, target_function: Callable, spike_multiplier: int = 10):
        super().__init__(config)
        self.target_function = target_function
        self.spike_multiplier = spike_multiplier
        self.baseline_load = 10
        self.spike_duration = 60  # seconds

    async def setup_test(self):
        """Setup spike test."""
        self.logger.info("Setting up spike load test")

    async def run_test(self):
        """Execute spike test."""
        self.logger.info("Starting spike load test")

        # Phase 1: Baseline load
        await self._baseline_phase()

        # Phase 2: Spike load
        await self._spike_phase()

        # Phase 3: Recovery
        await self._recovery_phase()

    async def cleanup_test(self):
        """Cleanup spike test."""
        pass

    async def _baseline_phase(self):
        """Establish baseline performance."""
        self.logger.info("Baseline phase: establishing normal load")

        tasks = []
        for i in range(self.baseline_load):
            task = asyncio.create_task(self._baseline_worker(i))
            tasks.append(task)

        # Run baseline for 30 seconds
        await asyncio.sleep(30)

        # Cancel baseline tasks
        for task in tasks:
            task.cancel()

        await asyncio.gather(*tasks, return_exceptions=True)

    async def _spike_phase(self):
        """Generate sudden load spike."""
        self.logger.info(f"Spike phase: {self.baseline_load * self.spike_multiplier} concurrent operations")

        spike_load = self.baseline_load * self.spike_multiplier

        # Create spike tasks
        spike_tasks = []
        for i in range(spike_load):
            task = asyncio.create_task(self._spike_worker(i))
            spike_tasks.append(task)

        # Measure spike impact
        time.time()

        # Wait for spike duration
        await asyncio.sleep(self.spike_duration)

        # Cancel spike tasks
        for task in spike_tasks:
            task.cancel()

        results = await asyncio.gather(*spike_tasks, return_exceptions=True)

        # Analyze spike results
        successful_operations = sum(1 for r in results if not isinstance(r, Exception))
        failed_operations = len(results) - successful_operations

        self.metrics.record_operation(
            "spike_test",
            self.spike_duration * 1000,
            True,
            {
                "spike_load": spike_load,
                "successful_operations": successful_operations,
                "failed_operations": failed_operations,
                "success_rate": successful_operations / len(results) if results else 0,
            },
        )

    async def _recovery_phase(self):
        """Measure system recovery after spike."""
        self.logger.info("Recovery phase: measuring system recovery")

        # Return to baseline load
        recovery_tasks = []
        for i in range(self.baseline_load):
            task = asyncio.create_task(self._recovery_worker(i))
            recovery_tasks.append(task)

        # Monitor recovery for 60 seconds
        await asyncio.sleep(60)

        # Cancel recovery tasks
        for task in recovery_tasks:
            task.cancel()

        await asyncio.gather(*recovery_tasks, return_exceptions=True)

    async def _baseline_worker(self, worker_id: int):
        """Worker for baseline phase."""
        while True:
            try:
                start_time = time.time()
                await self.target_function()
                duration = (time.time() - start_time) * 1000

                self.metrics.record_operation(
                    "baseline_operation", duration, True, {"worker_id": worker_id, "phase": "baseline"}
                )

                await asyncio.sleep(1)  # 1 second between operations

            except asyncio.CancelledError:
                break
            except Exception as e:
                self.metrics.record_operation(
                    "baseline_operation", 0, False, {"worker_id": worker_id, "phase": "baseline", "error": str(e)}
                )

    async def _spike_worker(self, worker_id: int):
        """Worker for spike phase."""
        try:
            start_time = time.time()
            await self.target_function()
            duration = (time.time() - start_time) * 1000

            self.metrics.record_operation("spike_operation", duration, True, {"worker_id": worker_id, "phase": "spike"})

        except asyncio.CancelledError:
            # Expected during shutdown, no need to log
            self.logger.debug(f"Spike worker {worker_id} cancelled")
        except Exception as e:
            self.metrics.record_operation(
                "spike_operation", 0, False, {"worker_id": worker_id, "phase": "spike", "error": str(e)}
            )

    async def _recovery_worker(self, worker_id: int):
        """Worker for recovery phase."""
        while True:
            try:
                start_time = time.time()
                await self.target_function()
                duration = (time.time() - start_time) * 1000

                self.metrics.record_operation(
                    "recovery_operation", duration, True, {"worker_id": worker_id, "phase": "recovery"}
                )

                await asyncio.sleep(1)  # 1 second between operations

            except asyncio.CancelledError:
                break
            except Exception as e:
                self.metrics.record_operation(
                    "recovery_operation", 0, False, {"worker_id": worker_id, "phase": "recovery", "error": str(e)}
                )


class EnduranceTest(PerformanceTestBase):
    """Test system stability under prolonged load."""

    def __init__(self, config: TestConfiguration, target_function: Callable, test_duration_hours: int = 2):
        super().__init__(config)
        self.target_function = target_function
        self.test_duration_hours = test_duration_hours
        self.constant_load = 20
        self.memory_samples = []
        self.performance_samples = []

    async def setup_test(self):
        """Setup endurance test."""
        self.logger.info(f"Setting up endurance test: {self.test_duration_hours} hours")

    async def run_test(self):
        """Execute endurance test."""
        self.logger.info("Starting endurance test")

        # Start monitoring
        monitor_task = asyncio.create_task(self._monitor_system())

        # Start workers
        worker_tasks = []
        for i in range(self.constant_load):
            task = asyncio.create_task(self._endurance_worker(i))
            worker_tasks.append(task)

        # Run for specified duration
        await asyncio.sleep(self.test_duration_hours * 3600)

        # Stop monitoring
        monitor_task.cancel()

        # Stop workers
        for task in worker_tasks:
            task.cancel()

        await asyncio.gather(*worker_tasks, return_exceptions=True)

        # Analyze endurance results
        await self._analyze_endurance_results()

    async def cleanup_test(self):
        """Cleanup endurance test."""
        pass

    async def _endurance_worker(self, worker_id: int):
        """Worker for endurance test."""
        operations = 0
        errors = 0

        while True:
            try:
                start_time = time.time()
                await self.target_function()
                duration = (time.time() - start_time) * 1000

                operations += 1

                self.metrics.record_operation(
                    "endurance_operation", duration, True, {"worker_id": worker_id, "operation_count": operations}
                )

                # Variable think time to simulate realistic usage
                await asyncio.sleep(random.uniform(0.1, 2.0))  # noqa: S311 (using random for test simulation)

            except asyncio.CancelledError:
                break
            except Exception as e:
                errors += 1
                self.metrics.record_operation(
                    "endurance_operation",
                    0,
                    False,
                    {"worker_id": worker_id, "operation_count": operations, "error_count": errors, "error": str(e)},
                )

    async def _monitor_system(self):
        """Monitor system resources during endurance test."""
        while True:
            try:
                memory_usage = self.metrics._get_memory_usage_mb()
                cpu_usage = self.metrics._get_cpu_usage_percent()

                # Record samples
                self.memory_samples.append(
                    {"timestamp": time.time(), "memory_mb": memory_usage, "cpu_percent": cpu_usage}
                )

                # Check for memory leaks (growing memory usage)
                if len(self.memory_samples) > 60:  # Check every minute
                    recent_memory = [s["memory_mb"] for s in self.memory_samples[-60:]]
                    if self._detect_memory_leak(recent_memory):
                        self.logger.warning("Potential memory leak detected")

                await asyncio.sleep(60)  # Sample every minute

            except asyncio.CancelledError:
                break
            except Exception as e:
                self.logger.error(f"Error in endurance monitoring: {e}")

    def _detect_memory_leak(self, memory_samples: list[float]) -> bool:
        """Detect potential memory leak from samples."""
        if len(memory_samples) < 30:
            return False

        # Simple linear regression to detect trend
        x = list(range(len(memory_samples)))
        y = memory_samples

        # Calculate slope
        n = len(x)
        sum_x = sum(x)
        sum_y = sum(y)
        sum_xy = sum(x[i] * y[i] for i in range(n))
        sum_x2 = sum(x[i] ** 2 for i in range(n))

        slope = (n * sum_xy - sum_x * sum_y) / (n * sum_x2 - sum_x**2)

        # If memory is growing consistently (slope > 1MB per minute)
        return slope > 1.0

    async def _analyze_endurance_results(self):
        """Analyze endurance test results."""
        if not self.memory_samples:
            return

        # Calculate memory statistics
        memory_values = [s["memory_mb"] for s in self.memory_samples]
        cpu_values = [s["cpu_percent"] for s in self.memory_samples]

        memory_stats = {
            "min": min(memory_values),
            "max": max(memory_values),
            "avg": sum(memory_values) / len(memory_values),
            "growth": memory_values[-1] - memory_values[0] if len(memory_values) > 1 else 0,
        }

        cpu_stats = {"min": min(cpu_values), "max": max(cpu_values), "avg": sum(cpu_values) / len(cpu_values)}

        # Record endurance results
        self.metrics.record_operation(
            "endurance_test_summary",
            0,
            True,
            {
                "test_duration_hours": self.test_duration_hours,
                "memory_stats": memory_stats,
                "cpu_stats": cpu_stats,
                "samples_collected": len(self.memory_samples),
                "memory_leak_detected": self._detect_memory_leak(memory_values),
            },
        )

        self.logger.info(f"Endurance test completed: {memory_stats['growth']:.1f}MB memory growth")


# Export test classes
__all__ = [
    "ConcurrentLoadTest",
    "DatabaseLoadTest",
    "EnduranceTest",
    "HTTPLoadTest",
    "LoadTestScenario",
    "SpikeLoadTest",
]
