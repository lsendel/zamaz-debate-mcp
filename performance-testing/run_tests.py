#!/usr/bin/env python3
"""
Comprehensive performance test runner.

This script provides a unified interface for running all performance tests
with various configuration options and reporting capabilities.
"""

import argparse
import asyncio
import json
import logging
import sys
from datetime import datetime
from pathlib import Path
from typing import Any

# Add performance testing modules to path
sys.path.insert(0, str(Path(__file__).parent))

from framework.base import TestConfiguration
from framework.reporting import PerformanceReporter, TestResult
from jmeter.jmeter_integration import JMeterExecutor, JMeterTestGenerator, JMeterTestPlan

from docker.docker_integration import DockerPerformanceTestRunner, DockerTestConfig
from monitoring.resource_monitor import ResourceMonitor, ResourceThresholds

# Test imports
from tests.component_benchmarks import (
    DatabaseBenchmark,
    DebateAPIBenchmark,
    LLMIntegrationBenchmark,
    MemoryIntensiveBenchmark,
)
from tests.database_load_tests import DatabaseLoadConfig, DatabaseLoadTest as DBLoadTest
from tests.github_api_simulation import GitHubAPIRateLimitTest, RateLimitConfig
from tests.load_tests import HTTPLoadTest, LoadTestScenario, SpikeLoadTest


class PerformanceTestSuite:
    """Main performance test suite runner."""

    def __init__(self, config_file: str | None = None):
        self.config_file = config_file
        self.logger = logging.getLogger(self.__class__.__name__)
        self.test_results = []
        self.resource_monitor = None

        # Load configuration
        self.config = self._load_config()

        # Setup logging
        self._setup_logging()

        # Initialize resource monitoring
        if self.config.get("enable_resource_monitoring", True):
            thresholds = ResourceThresholds(
                memory_critical_mb=self.config.get("memory_limit_mb", 1024),
                cpu_critical_percent=self.config.get("cpu_limit_percent", 80.0),
            )
            self.resource_monitor = ResourceMonitor(thresholds)

    def _load_config(self) -> dict[str, Any]:
        """Load test configuration."""
        default_config = {
            "test_duration_seconds": 300,
            "concurrent_users": 50,
            "warmup_seconds": 30,
            "cooldown_seconds": 30,
            "memory_limit_mb": 1024,
            "cpu_limit_percent": 80.0,
            "enable_resource_monitoring": True,
            "enable_detailed_logging": False,
            "output_dir": "performance_reports",
            "database_url": "postgresql://testuser:testpass@localhost:5432/testdb",
            "api_base_url": "http://localhost:8080",
            "llm_api_url": "http://localhost:8081",
            "enable_docker": False,
            "docker_cleanup": True,
            "test_suites": {
                "component_benchmarks": True,
                "load_tests": True,
                "github_api_simulation": True,
                "database_load_tests": True,
                "jmeter_tests": False,
            },
        }

        if self.config_file and Path(self.config_file).exists():
            with open(self.config_file) as f:
                user_config = json.load(f)
                default_config.update(user_config)

        return default_config

    def _setup_logging(self):
        """Setup logging configuration."""
        log_level = logging.DEBUG if self.config.get("enable_detailed_logging", False) else logging.INFO

        logging.basicConfig(
            level=log_level,
            format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            handlers=[
                logging.StreamHandler(),
                logging.FileHandler(Path(self.config["output_dir"]) / "performance_tests.log"),
            ],
        )

    async def run_all_tests(self) -> list[TestResult]:
        """Run all enabled test suites."""
        self.logger.info("Starting comprehensive performance test suite")

        # Create output directory
        output_dir = Path(self.config["output_dir"])
        output_dir.mkdir(exist_ok=True)

        # Start resource monitoring
        if self.resource_monitor:
            self.resource_monitor.start_monitoring()

        try:
            # Run test suites
            if self.config["test_suites"]["component_benchmarks"]:
                await self._run_component_benchmarks()

            if self.config["test_suites"]["load_tests"]:
                await self._run_load_tests()

            if self.config["test_suites"]["github_api_simulation"]:
                await self._run_github_api_simulation()

            if self.config["test_suites"]["database_load_tests"]:
                await self._run_database_load_tests()

            if self.config["test_suites"]["jmeter_tests"]:
                await self._run_jmeter_tests()

            # Generate comprehensive report
            await self._generate_comprehensive_report()

            return self.test_results

        finally:
            # Stop resource monitoring
            if self.resource_monitor:
                self.resource_monitor.stop_monitoring()

                # Generate monitoring report
                self.resource_monitor.generate_monitoring_report(output_dir / "monitoring")

    async def _run_component_benchmarks(self):
        """Run component benchmark tests."""
        self.logger.info("Running component benchmark tests")

        test_config = TestConfiguration(
            duration_seconds=self.config["test_duration_seconds"],
            warmup_seconds=self.config["warmup_seconds"],
            cooldown_seconds=self.config["cooldown_seconds"],
            concurrent_users=self.config["concurrent_users"],
            max_memory_mb=self.config["memory_limit_mb"],
            max_cpu_percent=self.config["cpu_limit_percent"],
        )

        # Database benchmark
        try:
            db_benchmark = DatabaseBenchmark(test_config, self.config["database_url"])
            result = await db_benchmark.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="DatabaseBenchmark",
                    test_type="component_benchmark",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "component_benchmarks"},
                )
            )

        except Exception as e:
            self.logger.error(f"Database benchmark failed: {e}")
            self._add_failed_test_result("DatabaseBenchmark", "component_benchmark", e)

        # API benchmark
        try:
            api_benchmark = DebateAPIBenchmark(test_config, self.config["api_base_url"])
            result = await api_benchmark.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="DebateAPIBenchmark",
                    test_type="component_benchmark",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "component_benchmarks"},
                )
            )

        except Exception as e:
            self.logger.error(f"API benchmark failed: {e}")
            self._add_failed_test_result("DebateAPIBenchmark", "component_benchmark", e)

        # LLM integration benchmark
        try:
            llm_benchmark = LLMIntegrationBenchmark(test_config, self.config["llm_api_url"])
            result = await llm_benchmark.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="LLMIntegrationBenchmark",
                    test_type="component_benchmark",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "component_benchmarks"},
                )
            )

        except Exception as e:
            self.logger.error(f"LLM benchmark failed: {e}")
            self._add_failed_test_result("LLMIntegrationBenchmark", "component_benchmark", e)

        # Memory intensive benchmark
        try:
            memory_benchmark = MemoryIntensiveBenchmark(test_config)
            result = await memory_benchmark.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="MemoryIntensiveBenchmark",
                    test_type="component_benchmark",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "component_benchmarks"},
                )
            )

        except Exception as e:
            self.logger.error(f"Memory benchmark failed: {e}")
            self._add_failed_test_result("MemoryIntensiveBenchmark", "component_benchmark", e)

    async def _run_load_tests(self):
        """Run load test scenarios."""
        self.logger.info("Running load tests")

        test_config = TestConfiguration(
            duration_seconds=self.config["test_duration_seconds"],
            concurrent_users=self.config["concurrent_users"],
            requests_per_second=self.config.get("requests_per_second", 100.0),
        )

        # HTTP load test
        try:
            scenario = LoadTestScenario(
                name="http_load_test",
                description="HTTP API load testing",
                max_users=self.config["concurrent_users"],
                steady_state_duration=self.config["test_duration_seconds"],
                requests_per_user=100,
            )

            endpoints = [
                {"method": "GET", "path": "/api/organizations"},
                {"method": "GET", "path": "/api/debates"},
                {"method": "POST", "path": "/api/debates"},
                {"method": "GET", "path": "/api/debates/1"},
            ]

            http_load_test = HTTPLoadTest(test_config, scenario, self.config["api_base_url"], endpoints)
            result = await http_load_test.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="HTTPLoadTest",
                    test_type="load_test",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "load_tests"},
                )
            )

        except Exception as e:
            self.logger.error(f"HTTP load test failed: {e}")
            self._add_failed_test_result("HTTPLoadTest", "load_test", e)

        # Spike test
        try:

            async def dummy_function():
                await asyncio.sleep(0.01)

            spike_test = SpikeLoadTest(test_config, dummy_function)
            result = await spike_test.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="SpikeLoadTest",
                    test_type="load_test",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "load_tests"},
                )
            )

        except Exception as e:
            self.logger.error(f"Spike test failed: {e}")
            self._add_failed_test_result("SpikeLoadTest", "load_test", e)

    async def _run_github_api_simulation(self):
        """Run GitHub API simulation tests."""
        self.logger.info("Running GitHub API simulation tests")

        test_config = TestConfiguration(
            duration_seconds=self.config["test_duration_seconds"], concurrent_users=self.config["concurrent_users"]
        )

        rate_limit_config = RateLimitConfig(core_limit=5000, search_limit=30, abuse_detection_enabled=True)

        try:
            github_test = GitHubAPIRateLimitTest(test_config, rate_limit_config)
            result = await github_test.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="GitHubAPIRateLimitTest",
                    test_type="simulation_test",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "github_api_simulation"},
                )
            )

        except Exception as e:
            self.logger.error(f"GitHub API simulation failed: {e}")
            self._add_failed_test_result("GitHubAPIRateLimitTest", "simulation_test", e)

    async def _run_database_load_tests(self):
        """Run database load tests."""
        self.logger.info("Running database load tests")

        test_config = TestConfiguration(
            duration_seconds=self.config["test_duration_seconds"], concurrent_users=self.config["concurrent_users"]
        )

        db_config = DatabaseLoadConfig(
            db_url=self.config["database_url"],
            concurrent_users=self.config["concurrent_users"],
            test_duration_minutes=self.config["test_duration_seconds"] // 60,
            initial_data_size=1000,
        )

        try:
            db_load_test = DBLoadTest(test_config, db_config)
            result = await db_load_test.execute_test()

            self.test_results.append(
                TestResult(
                    test_name="DatabaseLoadTest",
                    test_type="database_load_test",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["performance_metrics"]["duration_ms"] / 1000,
                    success=True,
                    metrics=result["performance_metrics"],
                    operation_stats=result["operation_stats"],
                    error_summary=result["error_summary"],
                    configuration=test_config.__dict__,
                    metadata={"test_suite": "database_load_tests"},
                )
            )

        except Exception as e:
            self.logger.error(f"Database load test failed: {e}")
            self._add_failed_test_result("DatabaseLoadTest", "database_load_test", e)

    async def _run_jmeter_tests(self):
        """Run JMeter tests."""
        self.logger.info("Running JMeter tests")

        # Create JMeter test plan
        test_plan = JMeterTestPlan(
            name="MCP System Load Test",
            description="JMeter load test for MCP system",
            base_url=self.config["api_base_url"],
            duration=self.config["test_duration_seconds"],
            thread_groups=[
                {
                    "name": "API Load Test",
                    "threads": self.config["concurrent_users"],
                    "ramp_time": 60,
                    "duration": self.config["test_duration_seconds"],
                    "requests": [
                        {"name": "Get Organizations", "method": "GET", "path": "/api/organizations"},
                        {"name": "Get Debates", "method": "GET", "path": "/api/debates"},
                        {
                            "name": "Create Debate",
                            "method": "POST",
                            "path": "/api/debates",
                            "body": '{"title": "Test Debate"}',
                        },
                    ],
                }
            ],
        )

        try:
            # Generate test plan
            generator = JMeterTestGenerator()
            test_plan_path = Path(self.config["output_dir"]) / "jmeter_test_plan.jmx"
            generator.generate_test_plan(test_plan, str(test_plan_path))

            # Execute test
            executor = JMeterExecutor()
            result = executor.execute_test(str(test_plan_path), str(Path(self.config["output_dir"]) / "jmeter_results"))

            self.test_results.append(
                TestResult(
                    test_name="JMeterLoadTest",
                    test_type="jmeter_test",
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    duration_seconds=result["execution_time"],
                    success=result["success"],
                    metrics={"execution_time": result["execution_time"]},
                    operation_stats={},
                    error_summary={"error": result.get("error", "")},
                    configuration=test_plan.__dict__,
                    metadata={"test_suite": "jmeter_tests"},
                )
            )

        except Exception as e:
            self.logger.error(f"JMeter test failed: {e}")
            self._add_failed_test_result("JMeterLoadTest", "jmeter_test", e)

    async def _generate_comprehensive_report(self):
        """Generate comprehensive performance report."""
        self.logger.info("Generating comprehensive performance report")

        output_dir = Path(self.config["output_dir"])
        reporter = PerformanceReporter(str(output_dir))

        # Generate HTML report
        html_report = reporter.generate_html_report(self.test_results, "comprehensive_performance_report")

        # Generate JSON report
        json_report = reporter.generate_json_report(self.test_results, "comprehensive_performance_data")

        # Generate PDF report
        pdf_report = reporter.generate_pdf_report(self.test_results, "comprehensive_performance_report")

        self.logger.info("Reports generated:")
        self.logger.info(f"  HTML: {html_report}")
        self.logger.info(f"  JSON: {json_report}")
        self.logger.info(f"  PDF: {pdf_report}")

        # Generate summary
        successful_tests = [r for r in self.test_results if r.success]
        failed_tests = [r for r in self.test_results if not r.success]

        summary = {
            "total_tests": len(self.test_results),
            "successful_tests": len(successful_tests),
            "failed_tests": len(failed_tests),
            "success_rate": len(successful_tests) / len(self.test_results) if self.test_results else 0,
            "total_duration": sum(r.duration_seconds for r in self.test_results),
            "test_suites": list({r.metadata.get("test_suite", "unknown") for r in self.test_results}),
        }

        self.logger.info(f"Test Summary: {summary}")

        # Save summary
        with open(output_dir / "test_summary.json", "w") as f:
            json.dump(summary, f, indent=2)

    def _add_failed_test_result(self, test_name: str, test_type: str, error: Exception):
        """Add failed test result."""
        self.test_results.append(
            TestResult(
                test_name=test_name,
                test_type=test_type,
                start_time=datetime.now(),
                end_time=datetime.now(),
                duration_seconds=0,
                success=False,
                metrics={},
                operation_stats={},
                error_summary={"error": str(error)},
                configuration={},
                metadata={"failed": True},
            )
        )


async def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(description="Run comprehensive performance tests")
    parser.add_argument("--config", "-c", help="Configuration file path")
    parser.add_argument("--output-dir", "-o", help="Output directory", default="performance_reports")
    parser.add_argument("--duration", "-d", type=int, help="Test duration in seconds", default=300)
    parser.add_argument("--users", "-u", type=int, help="Concurrent users", default=50)
    parser.add_argument("--verbose", "-v", action="store_true", help="Enable verbose logging")
    parser.add_argument(
        "--suite",
        "-s",
        choices=["component", "load", "github", "database", "jmeter", "all"],
        default="all",
        help="Test suite to run",
    )
    parser.add_argument("--docker", action="store_true", help="Run tests in Docker containers")

    args = parser.parse_args()

    # Create configuration
    config = {
        "output_dir": args.output_dir,
        "test_duration_seconds": args.duration,
        "concurrent_users": args.users,
        "enable_detailed_logging": args.verbose,
        "enable_docker": args.docker,
        "test_suites": {
            "component_benchmarks": args.suite in ["component", "all"],
            "load_tests": args.suite in ["load", "all"],
            "github_api_simulation": args.suite in ["github", "all"],
            "database_load_tests": args.suite in ["database", "all"],
            "jmeter_tests": args.suite in ["jmeter", "all"],
        },
    }

    # Save temporary config file
    if args.config:
        config_path = args.config
    else:
        config_path = Path(args.output_dir) / "test_config.json"
        config_path.parent.mkdir(exist_ok=True)
        with open(config_path, "w") as f:
            json.dump(config, f, indent=2)

    # Run tests
    if args.docker:
        # Run in Docker
        docker_config = DockerTestConfig(
            cleanup_after_test=True,
            test_timeout=args.duration + 600,  # Add 10 minutes buffer
        )

        docker_runner = DockerPerformanceTestRunner(docker_config)

        try:
            await docker_runner.setup_test_environment()

            # Run each test suite in Docker
            for suite_name, enabled in config["test_suites"].items():
                if enabled:
                    await docker_runner.run_performance_tests(suite_name, config)

        finally:
            await docker_runner.cleanup_test_environment()

    else:
        # Run locally
        test_suite = PerformanceTestSuite(config_path)
        results = await test_suite.run_all_tests()

        # Print summary
        successful_tests = [r for r in results if r.success]

        # Exit with appropriate code
        if len(successful_tests) == len(results):
            sys.exit(0)
        else:
            sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
