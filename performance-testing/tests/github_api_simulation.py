"""
GitHub API rate limiting simulation for testing.

This module simulates GitHub API interactions with rate limiting to test
how the system handles API constraints and develops resilience strategies.
"""

import asyncio
import logging
import secrets
import time
from dataclasses import dataclass, field
from enum import Enum
from typing import Any

from ..framework.base import PerformanceTestBase, TestConfiguration, measure_async_time


class RateLimitType(Enum):
    """GitHub API rate limit types."""

    CORE = "core"  # 5000 requests per hour
    SEARCH = "search"  # 30 requests per minute
    GRAPHQL = "graphql"  # 5000 points per hour
    INTEGRATION_MANIFEST = "integration_manifest"  # 5000 requests per hour
    SOURCE_IMPORT = "source_import"  # 100 requests per hour
    CODE_SCANNING_UPLOAD = "code_scanning_upload"  # 1000 requests per hour


@dataclass
class RateLimitConfig:
    """Configuration for GitHub API rate limiting."""

    # Core API limits
    core_limit: int = 5000
    core_window_seconds: int = 3600

    # Search API limits
    search_limit: int = 30
    search_window_seconds: int = 60

    # GraphQL API limits
    graphql_limit: int = 5000
    graphql_window_seconds: int = 3600

    # Secondary rate limiting
    secondary_limit: int = 100
    secondary_window_seconds: int = 60

    # Abuse detection
    abuse_detection_enabled: bool = True
    abuse_threshold: int = 10
    abuse_window_seconds: int = 1

    # Response delays
    base_delay_ms: int = 100
    rate_limit_delay_ms: int = 1000

    # Custom limits for testing
    custom_limits: dict[str, tuple[int, int]] = field(default_factory=dict)


class GitHubAPISimulator:
    """Simulates GitHub API with rate limiting."""

    def __init__(self, config: RateLimitConfig):
        self.config = config
        self.logger = logging.getLogger(self.__class__.__name__)

        # Rate limit tracking
        self.rate_limits = {
            RateLimitType.CORE: {
                "remaining": config.core_limit,
                "reset_time": time.time() + config.core_window_seconds,
            },
            RateLimitType.SEARCH: {
                "remaining": config.search_limit,
                "reset_time": time.time() + config.search_window_seconds,
            },
            RateLimitType.GRAPHQL: {
                "remaining": config.graphql_limit,
                "reset_time": time.time() + config.graphql_window_seconds,
            },
        }

        # Request tracking for abuse detection
        self.request_history = []

        # Response templates
        self.response_templates = self._setup_response_templates()

    def _setup_response_templates(self) -> dict[str, Any]:
        """Setup response templates for different endpoints."""
        return {
            "user": {
                "login": "testuser",
                "id": 12345,
                "avatar_url": "https://github.com/images/error/testuser_happy.gif",
                "type": "User",
                "name": "Test User",
                "company": "Test Company",
                "blog": "https://testblog.com",
                "location": "Test Location",
                "email": "test@example.com",
                "public_repos": 10,
                "public_gists": 5,
                "followers": 100,
                "following": 50,
                "created_at": "2020-01-01T00:00:00Z",
                "updated_at": "2023-01-01T00:00:00Z",
            },
            "repository": {
                "id": 67890,
                "name": "test-repo",
                "full_name": "testuser/test-repo",
                "owner": {"login": "testuser", "id": 12345},
                "private": False,
                "html_url": "https://github.com/testuser/test-repo",
                "description": "Test repository",
                "fork": False,
                "created_at": "2020-01-01T00:00:00Z",
                "updated_at": "2023-01-01T00:00:00Z",
                "pushed_at": "2023-01-01T00:00:00Z",
                "size": 1024,
                "stargazers_count": 10,
                "watchers_count": 5,
                "language": "Python",
                "forks_count": 3,
                "open_issues_count": 2,
                "default_branch": "main",
            },
            "pull_request": {
                "id": 11111,
                "number": 1,
                "state": "open",
                "title": "Test PR",
                "body": "This is a test pull request",
                "user": {"login": "testuser", "id": 12345},
                "created_at": "2023-01-01T00:00:00Z",
                "updated_at": "2023-01-01T00:00:00Z",
                "head": {"ref": "feature-branch", "sha": "abc123"},
                "base": {"ref": "main", "sha": "def456"},
                "mergeable": True,
                "merged": False,
                "additions": 100,
                "deletions": 50,
                "changed_files": 5,
            },
            "issue": {
                "id": 22222,
                "number": 1,
                "state": "open",
                "title": "Test Issue",
                "body": "This is a test issue",
                "user": {"login": "testuser", "id": 12345},
                "assignee": None,
                "milestone": None,
                "labels": [],
                "created_at": "2023-01-01T00:00:00Z",
                "updated_at": "2023-01-01T00:00:00Z",
                "closed_at": None,
                "comments": 0,
            },
            "commit": {
                "sha": "abc123def456",
                "commit": {
                    "author": {"name": "Test User", "email": "test@example.com", "date": "2023-01-01T00:00:00Z"},
                    "committer": {"name": "Test User", "email": "test@example.com", "date": "2023-01-01T00:00:00Z"},
                    "message": "Test commit message",
                },
                "author": {"login": "testuser", "id": 12345},
                "committer": {"login": "testuser", "id": 12345},
                "parents": [{"sha": "parent123"}],
            },
        }

    async def simulate_api_call(
        self,
        endpoint: str,
        method: str = "GET",
        rate_limit_type: RateLimitType = RateLimitType.CORE,
        payload: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        """Simulate a GitHub API call with rate limiting."""
        start_time = time.time()

        # Check for abuse detection
        if self.config.abuse_detection_enabled:
            await self._check_abuse_detection()

        # Check rate limits
        rate_limit_result = await self._check_rate_limit(rate_limit_type)

        if not rate_limit_result["allowed"]:
            # Rate limit exceeded
            return {
                "success": False,
                "status_code": 429,
                "error": "Rate limit exceeded",
                "rate_limit_info": rate_limit_result,
                "retry_after": rate_limit_result["reset_time"] - time.time(),
                "response_time_ms": (time.time() - start_time) * 1000,
            }

        # Simulate API processing delay
        await self._simulate_processing_delay(endpoint, method)

        # Generate response
        response_data = self._generate_response(endpoint, method, payload)

        # Update rate limits
        self._update_rate_limits(rate_limit_type)

        # Record request
        self._record_request(endpoint, method, rate_limit_type)

        return {
            "success": True,
            "status_code": 200,
            "data": response_data,
            "rate_limit_info": rate_limit_result,
            "response_time_ms": (time.time() - start_time) * 1000,
        }

    async def _check_abuse_detection(self):
        """Check for abuse detection patterns."""
        current_time = time.time()

        # Clean old requests
        self.request_history = [
            req for req in self.request_history if current_time - req["timestamp"] < self.config.abuse_window_seconds
        ]

        # Check request frequency
        if len(self.request_history) >= self.config.abuse_threshold:
            # Simulate abuse detection delay
            await asyncio.sleep(self.config.rate_limit_delay_ms / 1000)

    async def _check_rate_limit(self, rate_limit_type: RateLimitType) -> dict[str, Any]:
        """Check if rate limit allows the request."""
        current_time = time.time()
        rate_limit_info = self.rate_limits[rate_limit_type]

        # Reset rate limit if window expired
        if current_time >= rate_limit_info["reset_time"]:
            self._reset_rate_limit(rate_limit_type)
            rate_limit_info = self.rate_limits[rate_limit_type]

        allowed = rate_limit_info["remaining"] > 0

        return {
            "allowed": allowed,
            "limit": self._get_rate_limit_config(rate_limit_type)[0],
            "remaining": rate_limit_info["remaining"],
            "reset_time": rate_limit_info["reset_time"],
            "reset_in_seconds": max(0, rate_limit_info["reset_time"] - current_time),
        }

    def _get_rate_limit_config(self, rate_limit_type: RateLimitType) -> tuple[int, int]:
        """Get rate limit configuration for type."""
        if rate_limit_type == RateLimitType.CORE:
            return (self.config.core_limit, self.config.core_window_seconds)
        elif rate_limit_type == RateLimitType.SEARCH:
            return (self.config.search_limit, self.config.search_window_seconds)
        elif rate_limit_type == RateLimitType.GRAPHQL:
            return (self.config.graphql_limit, self.config.graphql_window_seconds)
        else:
            return (self.config.core_limit, self.config.core_window_seconds)

    def _reset_rate_limit(self, rate_limit_type: RateLimitType):
        """Reset rate limit for type."""
        limit, window = self._get_rate_limit_config(rate_limit_type)
        self.rate_limits[rate_limit_type] = {"remaining": limit, "reset_time": time.time() + window}

    def _update_rate_limits(self, rate_limit_type: RateLimitType):
        """Update rate limit after successful request."""
        self.rate_limits[rate_limit_type]["remaining"] -= 1

    def _record_request(self, endpoint: str, method: str, rate_limit_type: RateLimitType):
        """Record request for abuse detection."""
        self.request_history.append(
            {"timestamp": time.time(), "endpoint": endpoint, "method": method, "rate_limit_type": rate_limit_type.value}
        )

    async def _simulate_processing_delay(self, endpoint: str, method: str):
        """Simulate API processing delay."""
        base_delay = self.config.base_delay_ms / 1000

        # Add endpoint-specific delays
        if "search" in endpoint:
            base_delay *= 2  # Search endpoints are slower
        elif method in ["POST", "PUT", "PATCH"]:
            base_delay *= 1.5  # Write operations are slower

        # Add random variation
        delay = base_delay * random.uniform(0.5, 1.5)  # noqa: S311 (using random for test simulation)

        await asyncio.sleep(delay)

    def _generate_response(self, endpoint: str, _method: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        """Generate response data for endpoint."""
        # Determine response type based on endpoint
        if "/user" in endpoint:
            return self.response_templates["user"].copy()
        elif "/repos" in endpoint and "/pulls" in endpoint:
            return self.response_templates["pull_request"].copy()
        elif "/repos" in endpoint and "/issues" in endpoint:
            return self.response_templates["issue"].copy()
        elif "/repos" in endpoint and "/commits" in endpoint:
            return self.response_templates["commit"].copy()
        elif "/repos" in endpoint:
            return self.response_templates["repository"].copy()
        elif "/search" in endpoint:
            return {
                "total_count": secrets.randbelow(1, 1000),
                "incomplete_results": False,
                "items": [self.response_templates["repository"].copy() for _ in range(10)],
            }
        else:
            return {"message": "Success", "data": payload or {}}

    def get_rate_limit_status(self) -> dict[str, Any]:
        """Get current rate limit status."""
        current_time = time.time()

        status = {}
        for rate_limit_type, info in self.rate_limits.items():
            limit, window = self._get_rate_limit_config(rate_limit_type)

            status[rate_limit_type.value] = {
                "limit": limit,
                "remaining": info["remaining"],
                "reset_time": info["reset_time"],
                "reset_in_seconds": max(0, info["reset_time"] - current_time),
            }

        return status


class GitHubAPIRateLimitTest(PerformanceTestBase):
    """Test GitHub API rate limiting behavior."""

    def __init__(self, config: TestConfiguration, rate_limit_config: RateLimitConfig):
        super().__init__(config)
        self.rate_limit_config = rate_limit_config
        self.api_simulator = GitHubAPISimulator(rate_limit_config)
        self.test_scenarios = []

    async def setup_test(self):
        """Setup rate limiting test."""
        self.logger.info("Setting up GitHub API rate limiting test")

        # Define test scenarios
        self.test_scenarios = [
            {
                "name": "normal_usage",
                "description": "Normal API usage pattern",
                "requests_per_minute": 60,
                "duration_minutes": 5,
                "endpoints": [
                    ("/user", "GET", RateLimitType.CORE),
                    ("/repos/owner/repo", "GET", RateLimitType.CORE),
                    ("/repos/owner/repo/pulls", "GET", RateLimitType.CORE),
                    ("/repos/owner/repo/issues", "GET", RateLimitType.CORE),
                ],
            },
            {
                "name": "burst_usage",
                "description": "Burst API usage pattern",
                "requests_per_minute": 300,
                "duration_minutes": 2,
                "endpoints": [
                    ("/repos/owner/repo/pulls", "GET", RateLimitType.CORE),
                    ("/repos/owner/repo/commits", "GET", RateLimitType.CORE),
                ],
            },
            {
                "name": "search_heavy",
                "description": "Search API heavy usage",
                "requests_per_minute": 30,
                "duration_minutes": 3,
                "endpoints": [
                    ("/search/repositories", "GET", RateLimitType.SEARCH),
                    ("/search/issues", "GET", RateLimitType.SEARCH),
                    ("/search/code", "GET", RateLimitType.SEARCH),
                ],
            },
            {
                "name": "mixed_usage",
                "description": "Mixed API usage across different rate limits",
                "requests_per_minute": 150,
                "duration_minutes": 10,
                "endpoints": [
                    ("/user", "GET", RateLimitType.CORE),
                    ("/repos/owner/repo", "GET", RateLimitType.CORE),
                    ("/search/repositories", "GET", RateLimitType.SEARCH),
                    ("/repos/owner/repo/pulls", "POST", RateLimitType.CORE),
                    ("/repos/owner/repo/issues", "POST", RateLimitType.CORE),
                ],
            },
        ]

    async def run_test(self):
        """Run rate limiting tests."""
        self.logger.info("Running GitHub API rate limiting tests")

        for scenario in self.test_scenarios:
            await self._run_scenario(scenario)

            # Reset rate limits between scenarios
            await self._reset_rate_limits()

            # Wait between scenarios
            await asyncio.sleep(5)

    async def cleanup_test(self):
        """Cleanup rate limiting test."""
        pass

    async def _run_scenario(self, scenario: dict[str, Any]):
        """Run a single test scenario."""
        self.logger.info(f"Running scenario: {scenario['name']}")

        scenario_start = time.time()
        requests_per_minute = scenario["requests_per_minute"]
        duration_minutes = scenario["duration_minutes"]
        endpoints = scenario["endpoints"]

        # Calculate request interval
        request_interval = 60.0 / requests_per_minute

        # Track scenario results
        scenario_results = {
            "scenario": scenario["name"],
            "requests_sent": 0,
            "requests_successful": 0,
            "requests_rate_limited": 0,
            "total_response_time": 0,
            "rate_limit_delays": [],
        }

        # Run scenario
        end_time = scenario_start + (duration_minutes * 60)

        while time.time() < end_time:
            request_start = time.time()

            # Select random endpoint
            endpoint, method, rate_limit_type = secrets.choice(endpoints)

            # Make API call
            result = await self.api_simulator.simulate_api_call(endpoint, method, rate_limit_type)

            # Record results
            scenario_results["requests_sent"] += 1
            scenario_results["total_response_time"] += result["response_time_ms"]

            if result["success"]:
                scenario_results["requests_successful"] += 1
                self.metrics.record_operation(
                    f"api_request_{scenario['name']}",
                    result["response_time_ms"],
                    True,
                    {"endpoint": endpoint, "method": method, "rate_limit_type": rate_limit_type.value},
                )
            else:
                scenario_results["requests_rate_limited"] += 1
                if "retry_after" in result:
                    scenario_results["rate_limit_delays"].append(result["retry_after"])

                self.metrics.record_operation(
                    f"api_request_{scenario['name']}",
                    result["response_time_ms"],
                    False,
                    {
                        "endpoint": endpoint,
                        "method": method,
                        "rate_limit_type": rate_limit_type.value,
                        "error": result.get("error", "Unknown error"),
                    },
                )

            # Wait for next request
            request_duration = time.time() - request_start
            sleep_time = max(0, request_interval - request_duration)
            if sleep_time > 0:
                await asyncio.sleep(sleep_time)

        # Calculate scenario statistics
        if scenario_results["requests_sent"] > 0:
            scenario_results["success_rate"] = (
                scenario_results["requests_successful"] / scenario_results["requests_sent"]
            )
            scenario_results["rate_limit_rate"] = (
                scenario_results["requests_rate_limited"] / scenario_results["requests_sent"]
            )
            scenario_results["avg_response_time"] = (
                scenario_results["total_response_time"] / scenario_results["requests_sent"]
            )

        # Record scenario summary
        self.metrics.record_operation(
            f"scenario_summary_{scenario['name']}", (time.time() - scenario_start) * 1000, True, scenario_results
        )

        self.logger.info(
            f"Scenario {scenario['name']} completed: "
            f"{scenario_results['requests_successful']}/{scenario_results['requests_sent']} successful, "
            f"{scenario_results['success_rate']:.2%} success rate"
        )

    async def _reset_rate_limits(self):
        """Reset all rate limits."""
        for rate_limit_type in self.api_simulator.rate_limits:
            self.api_simulator._reset_rate_limit(rate_limit_type)

    @measure_async_time
    async def test_rate_limit_recovery(self):
        """Test how system recovers from rate limit exhaustion."""
        self.logger.info("Testing rate limit recovery")

        # Exhaust core rate limit
        while True:
            result = await self.api_simulator.simulate_api_call("/user", "GET", RateLimitType.CORE)

            if not result["success"]:
                break

        # Record time when rate limit was hit
        rate_limit_hit_time = time.time()

        # Try requests periodically until rate limit resets
        recovery_attempts = []

        while True:
            await asyncio.sleep(10)  # Wait 10 seconds between attempts

            result = await self.api_simulator.simulate_api_call("/user", "GET", RateLimitType.CORE)

            recovery_attempts.append(
                {
                    "timestamp": time.time(),
                    "success": result["success"],
                    "remaining": result.get("rate_limit_info", {}).get("remaining", 0),
                }
            )

            if result["success"]:
                # Rate limit recovered
                recovery_time = time.time() - rate_limit_hit_time

                self.metrics.record_operation(
                    "rate_limit_recovery",
                    recovery_time * 1000,
                    True,
                    {"recovery_time_seconds": recovery_time, "recovery_attempts": len(recovery_attempts)},
                )

                self.logger.info(f"Rate limit recovered after {recovery_time:.1f} seconds")
                break

            # Safety check to prevent infinite loop
            if len(recovery_attempts) > 100:
                self.logger.error("Rate limit recovery test timed out")
                break

    @measure_async_time
    async def test_concurrent_rate_limiting(self):
        """Test rate limiting with concurrent requests."""
        self.logger.info("Testing concurrent rate limiting")

        async def concurrent_requester(worker_id: int, requests_count: int):
            """Make concurrent requests."""
            results = []

            for i in range(requests_count):
                result = await self.api_simulator.simulate_api_call(
                    f"/repos/owner/repo-{worker_id}", "GET", RateLimitType.CORE
                )

                results.append(
                    {
                        "worker_id": worker_id,
                        "request_id": i,
                        "success": result["success"],
                        "response_time": result["response_time_ms"],
                    }
                )

                # Small delay between requests
                await asyncio.sleep(0.1)

            return results

        # Run concurrent workers
        workers = []
        for worker_id in range(10):
            worker = asyncio.create_task(concurrent_requester(worker_id, 50))
            workers.append(worker)

        # Wait for all workers to complete
        all_results = await asyncio.gather(*workers)

        # Analyze results
        total_requests = sum(len(results) for results in all_results)
        successful_requests = sum(sum(1 for r in results if r["success"]) for results in all_results)

        self.metrics.record_operation(
            "concurrent_rate_limiting",
            0,
            True,
            {
                "total_requests": total_requests,
                "successful_requests": successful_requests,
                "success_rate": successful_requests / total_requests if total_requests > 0 else 0,
                "workers": len(workers),
            },
        )

        self.logger.info(
            f"Concurrent rate limiting test completed: "
            f"{successful_requests}/{total_requests} successful "
            f"({successful_requests / total_requests:.2%})"
        )


# Export main classes
__all__ = ["GitHubAPIRateLimitTest", "GitHubAPISimulator", "RateLimitConfig", "RateLimitType"]
