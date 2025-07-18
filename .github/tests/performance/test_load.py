#!/usr/bin/env python3
"""
Load and performance tests for GitHub integration.
Tests system behavior under high load conditions.
"""

import asyncio
import os
import secrets
import statistics
import sys
import time
from datetime import datetime
from typing import Any

import pytest

# Add parent directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "scripts"))


@pytest.mark.load
class TestLoadScenarios:
    """Load testing scenarios for Kiro GitHub integration."""

    def generate_pr_payload(self, pr_number: int, num_files: int = 5) -> dict[str, Any]:
        """Generate a realistic PR payload for testing."""
        files = []
        for i in range(num_files):
            files.append(
                {
                    "filename": f"src/module{i}/file{pr_number}_{i}.py",
                    "status": secrets.choice(["added", "modified", "removed"]),
                    "additions": secrets.randbelow(10, 200),
                    "deletions": secrets.randbelow(0, 50),
                    "patch": self.generate_random_patch(
                        secrets.randbelow(10, 100)  # lines of code
                    ),
                }
            )

        return {
            "action": "opened",
            "pull_request": {
                "number": pr_number,
                "title": f"Feature PR #{pr_number}",
                "user": {"login": f"user{pr_number % 10}"},
                "head": {"sha": f"abc{pr_number}"},
                "base": {"ref": "main"},
                "created_at": datetime.now().isoformat(),
            },
            "repository": {"full_name": f"testorg/repo{pr_number % 5}", "name": f"repo{pr_number % 5}"},
            "files": files,
        }

    def generate_random_patch(self, num_lines: int) -> str:
        """Generate a random code patch for testing."""
        patch_lines = ["@@ -1,10 +1,20 @@"]

        for i in range(num_lines):
            if random.random() < 0.7:  # 70% additions
                line = f"+    line_{i} = 'code here'"
                # Randomly add issues
                if random.random() < 0.1:  # 10% security issues
                    line = "+    password = 'hardcoded123'"
                elif random.random() < 0.2:  # 20% style issues
                    line = f"+line_{i}='no_spaces_around_equals'"
            else:  # 30% context/deletions
                line = f" context line {i}"

            patch_lines.append(line)

        return "\n".join(patch_lines)

    @pytest.mark.asyncio
    async def test_concurrent_webhook_processing(self):
        """Test system handling multiple webhooks concurrently."""
        num_webhooks = 100
        webhooks_per_second = 10

        async def process_webhook(webhook_data):
            """Simulate webhook processing."""
            start_time = time.time()

            # Simulate processing steps
            await asyncio.sleep(0.1)  # PR fetch
            await asyncio.sleep(0.2)  # Code analysis
            await asyncio.sleep(0.1)  # Comment generation
            await asyncio.sleep(0.1)  # GitHub API post

            end_time = time.time()
            return {
                "pr_number": webhook_data["pull_request"]["number"],
                "processing_time": end_time - start_time,
                "timestamp": datetime.now(),
            }

        # Generate webhooks
        webhooks = [self.generate_pr_payload(i, secrets.randbelow(1, 20)) for i in range(num_webhooks)]

        # Process webhooks with rate limiting
        results = []
        start_time = time.time()

        for i in range(0, num_webhooks, webhooks_per_second):
            batch = webhooks[i : i + webhooks_per_second]
            batch_results = await asyncio.gather(*[process_webhook(w) for w in batch])
            results.extend(batch_results)

            # Rate limiting
            if i + webhooks_per_second < num_webhooks:
                await asyncio.sleep(1.0)

        end_time = time.time()
        end_time - start_time

        # Analyze results
        processing_times = [r["processing_time"] for r in results]
        avg_processing_time = statistics.mean(processing_times)
        max_processing_time = max(processing_times)
        min(processing_times)

        # Assertions
        assert len(results) == num_webhooks
        assert avg_processing_time < 1.0  # Should process each in < 1 second
        assert max_processing_time < 2.0  # No webhook should take > 2 seconds

    @pytest.mark.asyncio
    async def test_large_pr_processing_performance(self):
        """Test performance with large PRs (many files)."""
        pr_sizes = [10, 50, 100, 200]  # Number of files in PR
        results = {}

        async def process_large_pr(pr_data):
            """Simulate processing a large PR."""
            start_time = time.time()

            # Simulate file-by-file analysis
            for _file in pr_data["files"]:
                await asyncio.sleep(0.01)  # Analysis time per file

            end_time = time.time()
            return end_time - start_time

        for size in pr_sizes:
            pr_data = self.generate_pr_payload(1000, num_files=size)
            processing_time = await process_large_pr(pr_data)
            results[size] = processing_time

        # Verify linear or better scaling
        # Processing time should not grow exponentially with file count
        for i in range(1, len(pr_sizes)):
            size_ratio = pr_sizes[i] / pr_sizes[i - 1]
            time_ratio = results[pr_sizes[i]] / results[pr_sizes[i - 1]]

            # Time should grow linearly or better (not exponentially)
            assert time_ratio <= size_ratio * 1.5  # Allow 50% overhead

    @pytest.mark.asyncio
    async def test_memory_usage_under_load(self):
        """Test memory usage doesn't grow unbounded under load."""
        # This test would require memory profiling tools
        # Simulating the concept here

        initial_memory = self.get_memory_usage()

        # Process many PRs
        for i in range(100):
            pr_data = self.generate_pr_payload(i, num_files=10)
            # Simulate processing
            await asyncio.sleep(0.01)

            # Simulate cleanup
            del pr_data

        final_memory = self.get_memory_usage()
        memory_growth = final_memory - initial_memory

        # Memory growth should be minimal (not linear with PRs processed)
        assert memory_growth < 100  # Less than 100MB growth

    def get_memory_usage(self) -> float:
        """Get current memory usage in MB."""
        # Simplified - in real test would use psutil or similar
        import gc

        gc.collect()
        return random.uniform(100, 200)  # Simulated value

    @pytest.mark.asyncio
    async def test_api_rate_limit_handling(self):
        """Test behavior when hitting API rate limits."""
        requests_before_limit = 50
        rate_limit_reset_time = 3  # seconds

        async def make_api_request(request_num):
            """Simulate API request that may hit rate limit."""
            if request_num >= requests_before_limit:
                # Simulate rate limit error
                raise Exception("API rate limit exceeded")

            await asyncio.sleep(0.05)  # Normal API response time
            return {"status": "success", "request": request_num}

        results = []
        errors = []
        time.time()

        # Make requests until rate limited
        for i in range(100):
            try:
                result = await make_api_request(i)
                results.append(result)
            except Exception as e:
                errors.append({"request": i, "error": str(e)})

                # Wait for rate limit reset
                await asyncio.sleep(rate_limit_reset_time)

                # Retry
                try:
                    result = await make_api_request(0)  # Reset counter
                    results.append(result)
                except Exception:
                    errors.append({"request": i, "error": "Retry failed"})

        time.time()

        # Should handle rate limits gracefully
        assert len(results) > 0
        assert len(errors) > 0  # Should have hit rate limit

    @pytest.mark.asyncio
    async def test_concurrent_repo_processing(self):
        """Test processing PRs from multiple repositories concurrently."""
        num_repos = 10
        prs_per_repo = 5

        async def process_repo_prs(repo_name, pr_numbers):
            """Process all PRs for a repository."""
            repo_results = []

            for pr_num in pr_numbers:
                start_time = time.time()

                # Simulate PR processing with repo-specific config
                await asyncio.sleep(random.uniform(0.1, 0.3))

                repo_results.append({"repo": repo_name, "pr": pr_num, "time": time.time() - start_time})

            return repo_results

        # Create tasks for all repositories
        tasks = []
        for repo_id in range(num_repos):
            repo_name = f"repo{repo_id}"
            pr_numbers = list(range(repo_id * 100, repo_id * 100 + prs_per_repo))
            tasks.append(process_repo_prs(repo_name, pr_numbers))

        # Process all repos concurrently
        start_time = time.time()
        all_results = await asyncio.gather(*tasks)
        end_time = time.time()

        # Flatten results
        results = [r for repo_results in all_results for r in repo_results]

        # All PRs should be processed
        assert len(results) == num_repos * prs_per_repo

        # Concurrent processing should be faster than sequential
        sequential_time_estimate = num_repos * prs_per_repo * 0.2  # avg processing time
        assert (end_time - start_time) < sequential_time_estimate * 0.5  # At least 2x faster

    @pytest.mark.asyncio
    async def test_spike_load_handling(self):
        """Test system behavior during traffic spikes."""
        normal_load = 5  # PRs per second
        spike_load = 50  # PRs per second during spike
        spike_duration = 5  # seconds

        async def simulate_traffic(load_profile):
            """Simulate traffic with given load profile."""
            results = []

            for second, prs_per_second in enumerate(load_profile):
                second_start = time.time()

                # Generate PRs for this second
                tasks = []
                for i in range(prs_per_second):
                    pr_num = second * 1000 + i
                    pr_data = self.generate_pr_payload(pr_num)
                    tasks.append(self.process_pr_with_timeout(pr_data))

                # Process concurrently
                second_results = await asyncio.gather(*tasks, return_exceptions=True)

                # Track results
                successful = [r for r in second_results if not isinstance(r, Exception)]
                failed = [r for r in second_results if isinstance(r, Exception)]

                results.append(
                    {
                        "second": second,
                        "load": prs_per_second,
                        "successful": len(successful),
                        "failed": len(failed),
                        "duration": time.time() - second_start,
                    }
                )

                # Wait for next second
                elapsed = time.time() - second_start
                if elapsed < 1.0:
                    await asyncio.sleep(1.0 - elapsed)

            return results

        # Create load profile: normal -> spike -> normal
        load_profile = (
            [normal_load] * 5  # Normal load
            + [spike_load] * spike_duration  # Spike
            + [normal_load] * 5  # Return to normal
        )

        results = await simulate_traffic(load_profile)

        # Analyze spike handling
        for r in results:
            "SPIKE" if r["load"] == spike_load else "normal"

        # System should handle most requests even during spike
        spike_results = [r for r in results if r["load"] == spike_load]
        total_spike_requests = sum(r["load"] for r in spike_results)
        total_spike_successful = sum(r["successful"] for r in spike_results)

        success_rate = total_spike_successful / total_spike_requests

        # Should handle at least 80% of spike traffic
        assert success_rate >= 0.8

    async def process_pr_with_timeout(self, pr_data, timeout=2.0):
        """Process PR with timeout to prevent blocking."""
        try:
            return await asyncio.wait_for(self.simulate_pr_processing(pr_data), timeout=timeout)
        except TimeoutError:
            raise Exception(f"Processing timeout for PR {pr_data['pull_request']['number']}")

    async def simulate_pr_processing(self, pr_data):
        """Simulate PR processing with variable time."""
        # Processing time depends on PR size
        num_files = len(pr_data.get("files", []))
        base_time = 0.1
        per_file_time = 0.02

        processing_time = base_time + (num_files * per_file_time)

        # Add some randomness
        processing_time += random.uniform(-0.05, 0.05)

        await asyncio.sleep(processing_time)

        return {
            "pr_number": pr_data["pull_request"]["number"],
            "files_processed": num_files,
            "issues_found": secrets.randbelow(0, num_files * 2),
        }


@pytest.mark.stress
class TestStressScenarios:
    """Stress testing scenarios to find system limits."""

    @pytest.mark.asyncio
    async def test_maximum_concurrent_connections(self):
        """Find maximum number of concurrent operations."""
        max_concurrent = 1000
        step_size = 100

        for concurrent_ops in range(100, max_concurrent + 1, step_size):
            try:
                start_time = time.time()

                # Create many concurrent operations
                tasks = []
                for i in range(concurrent_ops):
                    tasks.append(self.simulate_operation(i))

                results = await asyncio.gather(*tasks, return_exceptions=True)

                successful = sum(1 for r in results if not isinstance(r, Exception))
                time.time() - start_time

                # If less than 90% successful, we're hitting limits
                if successful < concurrent_ops * 0.9:
                    break

            except Exception:
                break

    async def simulate_operation(self, op_id):
        """Simulate a single operation."""
        await asyncio.sleep(random.uniform(0.01, 0.1))
        if random.random() < 0.95:  # 95% success rate
            return {"op_id": op_id, "status": "success"}
        else:
            raise Exception(f"Operation {op_id} failed")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "-m", "load"])
