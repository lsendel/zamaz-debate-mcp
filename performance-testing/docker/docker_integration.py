"""
Docker integration and CI/CD pipeline support for performance testing.

This module provides tools for running performance tests in Docker containers,
managing test environments, and integrating with CI/CD pipelines.
"""

import asyncio
import json
import logging
import shutil
import subprocess
import tempfile
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

import docker
from docker.models.containers import Container


@dataclass
class DockerTestConfig:
    """Configuration for Docker-based performance testing."""

    # Docker settings
    base_image: str = "python:3.11-slim"
    network_name: str = "performance-test-network"

    # Test environment
    test_services: list[dict[str, Any]] = field(
        default_factory=lambda: [
            {
                "name": "postgres",
                "image": "postgres:15",
                "environment": {"POSTGRES_DB": "testdb", "POSTGRES_USER": "testuser", "POSTGRES_PASSWORD": "testpass"},
                "ports": {"5432/tcp": 5432},
                "volumes": {},
            },
            {"name": "redis", "image": "redis:7-alpine", "ports": {"6379/tcp": 6379}, "volumes": {}},
        ]
    )

    # Resource limits
    memory_limit: str = "2g"
    cpu_limit: str = "2"

    # Test execution
    test_timeout: int = 3600  # 1 hour
    cleanup_after_test: bool = True

    # Monitoring
    enable_monitoring: bool = True
    monitoring_interval: int = 5

    # Artifacts
    artifacts_dir: str = "/tmp/performance-test-artifacts"

    # Custom settings
    custom_settings: dict[str, Any] = field(default_factory=dict)


class DockerPerformanceTestRunner:
    """Runs performance tests in Docker containers."""

    def __init__(self, config: DockerTestConfig):
        self.config = config
        self.logger = logging.getLogger(self.__class__.__name__)
        self.docker_client = docker.from_env()

        # Container management
        self.test_containers = {}
        self.test_network = None
        self.test_volumes = {}

        # Monitoring
        self.monitoring_active = False
        self.monitoring_data = []

    async def setup_test_environment(self):
        """Setup Docker test environment."""
        self.logger.info("Setting up Docker test environment")

        # Create network
        try:
            self.test_network = self.docker_client.networks.create(self.config.network_name, driver="bridge")
            self.logger.info(f"Created network: {self.config.network_name}")
        except docker.errors.APIError as e:
            if "already exists" in str(e):
                self.test_network = self.docker_client.networks.get(self.config.network_name)
                self.logger.info(f"Using existing network: {self.config.network_name}")
            else:
                raise

        # Start test services
        for service_config in self.config.test_services:
            await self._start_service(service_config)

        # Wait for services to be ready
        await self._wait_for_services()

    async def run_performance_tests(self, test_module: str, test_config: dict[str, Any]) -> dict[str, Any]:
        """Run performance tests in Docker container."""
        self.logger.info(f"Running performance tests: {test_module}")

        # Create test container
        test_container = await self._create_test_container(test_module, test_config)

        # Start monitoring
        if self.config.enable_monitoring:
            monitoring_task = asyncio.create_task(self._monitor_containers())

        try:
            # Run tests
            start_time = time.time()

            test_container.start()

            # Wait for test completion
            result = test_container.wait(timeout=self.config.test_timeout)

            duration = time.time() - start_time

            # Get test results
            test_results = await self._collect_test_results(test_container)

            # Get container logs
            logs = test_container.logs().decode("utf-8")

            success = result["StatusCode"] == 0

            return {
                "success": success,
                "duration_seconds": duration,
                "exit_code": result["StatusCode"],
                "test_results": test_results,
                "logs": logs,
                "monitoring_data": self.monitoring_data.copy() if self.config.enable_monitoring else [],
            }

        finally:
            # Stop monitoring
            if self.config.enable_monitoring:
                self.monitoring_active = False
                if "monitoring_task" in locals():
                    monitoring_task.cancel()

            # Cleanup test container
            if self.config.cleanup_after_test:
                test_container.remove(force=True)

    def cleanup_test_environment(self):
        """Cleanup Docker test environment."""
        self.logger.info("Cleaning up Docker test environment")

        # Stop and remove containers
        for container_name, container in self.test_containers.items():
            try:
                container.stop()
                container.remove()
                self.logger.info(f"Removed container: {container_name}")
            except Exception as e:
                self.logger.error(f"Error removing container {container_name}: {e}")

        # Remove volumes
        for volume_name, volume in self.test_volumes.items():
            try:
                volume.remove()
                self.logger.info(f"Removed volume: {volume_name}")
            except Exception as e:
                self.logger.error(f"Error removing volume {volume_name}: {e}")

        # Remove network
        if self.test_network:
            try:
                self.test_network.remove()
                self.logger.info(f"Removed network: {self.config.network_name}")
            except Exception as e:
                self.logger.error(f"Error removing network: {e}")

    def _start_service(self, service_config: dict[str, Any]):
        """Start a test service container."""
        service_name = service_config["name"]

        self.logger.info(f"Starting service: {service_name}")

        # Create volumes for service
        volumes = {}
        for volume_config in service_config.get("volumes", {}):
            volume_name = f"{service_name}-{volume_config}"
            try:
                volume = self.docker_client.volumes.create(volume_name)
                self.test_volumes[volume_name] = volume
                volumes[volume_name] = {"bind": volume_config, "mode": "rw"}
            except docker.errors.APIError as e:
                if "already exists" in str(e):
                    volume = self.docker_client.volumes.get(volume_name)
                    self.test_volumes[volume_name] = volume
                    volumes[volume_name] = {"bind": volume_config, "mode": "rw"}
                else:
                    raise

        # Start container
        container = self.docker_client.containers.run(
            service_config["image"],
            name=f"test-{service_name}",
            environment=service_config.get("environment", {}),
            ports=service_config.get("ports", {}),
            volumes=volumes,
            network=self.config.network_name,
            detach=True,
            remove=False,
            mem_limit=self.config.memory_limit,
            cpu_period=100000,
            cpu_quota=int(float(self.config.cpu_limit) * 100000),
        )

        self.test_containers[service_name] = container

        self.logger.info(f"Started service container: {service_name}")

    async def _wait_for_services(self):
        """Wait for all services to be ready."""
        self.logger.info("Waiting for services to be ready")

        # Service-specific health checks
        health_checks = {
            "postgres": self._check_postgres_health,
            "redis": self._check_redis_health,
            "rabbitmq": self._check_rabbitmq_health,
        }

        for service_name, container in self.test_containers.items():
            if service_name in health_checks:
                await health_checks[service_name](container)
            else:
                # Generic health check - wait for container to be running
                await self._wait_for_container_running(container)

        self.logger.info("All services are ready")

    async def _check_postgres_health(self, container: Container):
        """Check PostgreSQL health."""
        max_attempts = 30
        for _ in range(max_attempts):
            try:
                result = container.exec_run("pg_isready -h localhost -U testuser -d testdb", user="postgres")
                if result.exit_code == 0:
                    self.logger.info("PostgreSQL is ready")
                    return
            except Exception as e:
                self.logger.debug(f"PostgreSQL health check failed: {e}")

            await asyncio.sleep(2)

        raise RuntimeError("PostgreSQL did not become ready in time")

    async def _check_redis_health(self, container: Container):
        """Check Redis health."""
        max_attempts = 30
        for _ in range(max_attempts):
            try:
                result = container.exec_run("redis-cli ping")
                if result.exit_code == 0 and b"PONG" in result.output:
                    self.logger.info("Redis is ready")
                    return
            except Exception as e:
                self.logger.debug(f"Redis health check failed: {e}")

            await asyncio.sleep(2)

        raise RuntimeError("Redis did not become ready in time")

    async def _check_rabbitmq_health(self, container: Container):
        """Check RabbitMQ health."""
        max_attempts = 60
        for _ in range(max_attempts):
            try:
                result = container.exec_run("rabbitmq-diagnostics ping")
                if result.exit_code == 0:
                    self.logger.info("RabbitMQ is ready")
                    return
            except Exception as e:
                self.logger.debug(f"RabbitMQ health check failed: {e}")

            await asyncio.sleep(2)

        raise RuntimeError("RabbitMQ did not become ready in time")

    async def _wait_for_container_running(self, container: Container):
        """Wait for container to be running."""
        max_attempts = 30
        for _ in range(max_attempts):
            container.reload()
            if container.status == "running":
                self.logger.info(f"Container {container.name} is running")
                return

            await asyncio.sleep(1)

        raise RuntimeError(f"Container {container.name} did not start in time")

    async def _create_test_container(self, test_module: str, test_config: dict[str, Any]) -> Container:
        """Create test container."""
        # Create temporary directory for test files
        temp_dir = tempfile.mkdtemp(prefix="performance-test-")

        try:
            # Create test script
            test_script = self._generate_test_script(test_module, test_config)
            test_script_path = Path(temp_dir) / "run_tests.py"

            with open(test_script_path, "w") as f:
                f.write(test_script)

            # Create requirements.txt
            requirements = self._generate_requirements()
            requirements_path = Path(temp_dir) / "requirements.txt"

            with open(requirements_path, "w") as f:
                f.write(requirements)

            # Create Dockerfile
            dockerfile = self._generate_dockerfile()
            dockerfile_path = Path(temp_dir) / "Dockerfile"

            with open(dockerfile_path, "w") as f:
                f.write(dockerfile)

            # Build test image
            test_image_tag = f"performance-test:{int(time.time())}"

            self.logger.info(f"Building test image: {test_image_tag}")

            image, build_logs = self.docker_client.images.build(path=temp_dir, tag=test_image_tag, remove=True)

            # Create artifacts volume
            artifacts_volume = self.docker_client.volumes.create(f"test-artifacts-{int(time.time())}")

            # Create test container
            container = self.docker_client.containers.create(
                test_image_tag,
                name=f"performance-test-{int(time.time())}",
                network=self.config.network_name,
                volumes={artifacts_volume.name: {"bind": "/artifacts", "mode": "rw"}},
                environment={
                    "POSTGRES_HOST": "test-postgres",
                    "POSTGRES_DB": "testdb",
                    "POSTGRES_USER": "testuser",
                    "POSTGRES_PASSWORD": "testpass",
                    "REDIS_HOST": "test-redis",
                    "REDIS_PORT": "6379",
                },
                mem_limit=self.config.memory_limit,
                cpu_period=100000,
                cpu_quota=int(float(self.config.cpu_limit) * 100000),
            )

            self.test_volumes[f"artifacts-{container.id[:12]}"] = artifacts_volume

            return container

        finally:
            # Clean up temporary directory
            shutil.rmtree(temp_dir, ignore_errors=True)

    def _generate_test_script(self, test_module: str, test_config: dict[str, Any]) -> str:
        """Generate test execution script."""
        return f"""
import asyncio
import json
import logging
import os
import sys
from pathlib import Path

# Add performance testing modules to path
sys.path.insert(0, '/app')

from performance_testing.framework.base import TestConfiguration
from performance_testing.framework.reporting import PerformanceReporter
from performance_testing.tests.{test_module} import *

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

async def main():
    \"\"\"Run performance tests.\"\"\"
    try:
        # Test configuration
        config = TestConfiguration(**{json.dumps(test_config)})

        # Create test instances based on module
        test_classes = []

        if '{test_module}' == 'component_benchmarks':
            db_url = f"postgresql://{{os.getenv('POSTGRES_USER')}}:{{os.getenv('POSTGRES_PASSWORD')}}@{{os.getenv('POSTGRES_HOST')}}/{{os.getenv('POSTGRES_DB')}}"
            test_classes = [
                DatabaseBenchmark(config, db_url),
                MemoryIntensiveBenchmark(config)
            ]

        elif '{test_module}' == 'load_tests':
            # Load test scenarios
            scenarios = [
                LoadTestScenario(
                    name='basic_load',
                    max_users=50,
                    duration_seconds=300
                )
            ]
            for scenario in scenarios:
                test_classes.append(ConcurrentLoadTest(config, scenario))

        elif '{test_module}' == 'database_load_tests':
            db_url = f"postgresql://{{os.getenv('POSTGRES_USER')}}:{{os.getenv('POSTGRES_PASSWORD')}}@{{os.getenv('POSTGRES_HOST')}}/{{os.getenv('POSTGRES_DB')}}"
            db_config = DatabaseLoadConfig(
                db_url=db_url,
                concurrent_users=20,
                test_duration_minutes=5
            )
            test_classes.append(DatabaseLoadTest(config, db_config))

        # Run tests
        all_results = []

        for test_instance in test_classes:
            logger.info(f"Running test: {{test_instance.__class__.__name__}}")

            try:
                result = await test_instance.execute_test()
                all_results.append({{
                    'test_name': test_instance.__class__.__name__,
                    'success': True,
                    'result': result
                }})

            except Exception as e:
                logger.error(f"Test failed: {{test_instance.__class__.__name__}}: {{e}}")
                all_results.append({{
                    'test_name': test_instance.__class__.__name__,
                    'success': False,
                    'error': str(e)
                }})

        # Generate reports
        reporter = PerformanceReporter('/artifacts')

        # Save results
        with open('/artifacts/test_results.json', 'w') as f:
            json.dump(all_results, f, indent=2, default=str)

        logger.info("Tests completed successfully")

        # Exit with success if all tests passed
        if all(r['success'] for r in all_results):
            sys.exit(0)
        else:
            sys.exit(1)

    except Exception as e:
        logger.error(f"Test execution failed: {{e}}")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
"""

    def _generate_requirements(self) -> str:
        """Generate requirements.txt for test container."""
        return """
asyncio
asyncpg
aiohttp
matplotlib
numpy
pandas
psutil
psycopg2-binary
pytest
pytest-asyncio
seaborn
sqlalchemy
prometheus-client
jinja2
"""

    def _generate_dockerfile(self) -> str:
        """Generate Dockerfile for test container."""
        return f"""
FROM {self.config.base_image}

# Install system dependencies
RUN apt-get update && apt-get install -y \\
    postgresql-client \\
    redis-tools \\
    build-essential \\
    && rm -rf /var/lib/apt/lists/*

# Create working directory
WORKDIR /app

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy test scripts
COPY run_tests.py .

# Copy performance testing framework
COPY performance_testing ./performance_testing/

# Create artifacts directory
RUN mkdir -p /artifacts

# Run tests
CMD ["python", "run_tests.py"]
"""

    def _find_artifacts_volume(self, container: Container):
        """Find the artifacts volume for a container."""
        for volume_name, volume in self.test_volumes.items():
            if container.id[:12] in volume_name:
                return volume
        return None

    def _extract_artifacts_from_volume(self, artifacts_volume, temp_dir: str):
        """Extract artifacts from Docker volume to temporary directory."""
        temp_container = self.docker_client.containers.run(
            "alpine:latest",
            command="tar -czf /tmp/artifacts.tar.gz -C /artifacts .",
            volumes={artifacts_volume.name: {"bind": "/artifacts", "mode": "ro"}},
            detach=True,
            remove=False,
        )

        temp_container.wait()

        # Copy artifacts
        bits, _ = temp_container.get_archive("/tmp/artifacts.tar.gz")

        with open(Path(temp_dir) / "artifacts.tar.gz", "wb") as f:
            for chunk in bits:
                f.write(chunk)

        # Extract artifacts
        subprocess.run(["tar", "-xzf", str(Path(temp_dir) / "artifacts.tar.gz"), "-C", temp_dir], check=True)

        temp_container.remove()

    async def _collect_test_results(self, container: Container) -> dict[str, Any]:
        """Collect test results from container."""
        try:
            artifacts_volume = self._find_artifacts_volume(container)
            if not artifacts_volume:
                return {"error": "Artifacts volume not found"}

            temp_dir = tempfile.mkdtemp(prefix="test-results-")

            try:
                self._extract_artifacts_from_volume(artifacts_volume, temp_dir)

                # Load test results
                results_file = Path(temp_dir) / "test_results.json"
                if results_file.exists():
                    with open(results_file) as f:
                        return json.load(f)

            finally:
                shutil.rmtree(temp_dir, ignore_errors=True)

        except Exception as e:
            self.logger.error(f"Error collecting test results: {e}")
            return {"error": str(e)}

        return {"error": "No test results found"}

    def _calculate_cpu_percent(self, stats):
        """Calculate CPU usage percentage from container stats."""
        cpu_delta = stats["cpu_stats"]["cpu_usage"]["total_usage"] - stats["precpu_stats"]["cpu_usage"]["total_usage"]
        system_delta = stats["cpu_stats"]["system_cpu_usage"] - stats["precpu_stats"]["system_cpu_usage"]
        return (cpu_delta / system_delta) * len(stats["cpu_stats"]["cpu_usage"]["percpu_usage"]) * 100.0

    def _calculate_memory_metrics(self, stats):
        """Calculate memory usage metrics from container stats."""
        memory_usage = stats["memory_stats"]["usage"]
        memory_limit = stats["memory_stats"]["limit"]
        memory_percent = (memory_usage / memory_limit) * 100.0

        return {
            "usage_mb": memory_usage / (1024 * 1024),
            "limit_mb": memory_limit / (1024 * 1024),
            "percent": memory_percent,
        }

    def _get_network_stats(self, stats):
        """Extract network statistics from container stats."""
        if "networks" not in stats:
            return {"rx_bytes": 0, "tx_bytes": 0}

        return {"rx_bytes": stats["networks"]["eth0"]["rx_bytes"], "tx_bytes": stats["networks"]["eth0"]["tx_bytes"]}

    def _collect_container_stats(self, container_name, container):
        """Collect stats for a single container."""
        try:
            container.reload()
            stats = container.stats(stream=False)

            cpu_percent = self._calculate_cpu_percent(stats)
            memory_metrics = self._calculate_memory_metrics(stats)
            network_stats = self._get_network_stats(stats)

            return {
                "status": container.status,
                "cpu_percent": cpu_percent,
                "memory_usage_mb": memory_metrics["usage_mb"],
                "memory_limit_mb": memory_metrics["limit_mb"],
                "memory_percent": memory_metrics["percent"],
                "network_rx_bytes": network_stats["rx_bytes"],
                "network_tx_bytes": network_stats["tx_bytes"],
            }
        except Exception as e:
            self.logger.debug(f"Error monitoring container {container_name}: {e}")
            return None

    async def _monitor_containers(self):
        """Monitor container resources during test execution."""
        self.monitoring_active = True

        while self.monitoring_active:
            try:
                monitoring_sample = {"timestamp": time.time(), "containers": {}}

                # Collect stats for all containers
                for container_name, container in self.test_containers.items():
                    container_stats = self._collect_container_stats(container_name, container)
                    if container_stats:
                        monitoring_sample["containers"][container_name] = container_stats

                self.monitoring_data.append(monitoring_sample)
                await asyncio.sleep(self.config.monitoring_interval)

            except asyncio.CancelledError:
                break
            except Exception as e:
                self.logger.error(f"Error in container monitoring: {e}")

        self.monitoring_active = False


class CIPipelineIntegration:
    """Integration with CI/CD pipelines."""

    def __init__(self, pipeline_type: str = "github"):
        self.pipeline_type = pipeline_type
        self.logger = logging.getLogger(self.__class__.__name__)

    def generate_github_workflow(self, test_configs: list[dict[str, Any]]) -> str:
        """Generate GitHub Actions workflow for performance tests."""
        workflow = {
            "name": "Performance Tests",
            "on": {
                "push": {"branches": ["main", "develop"]},
                "pull_request": {"branches": ["main"]},
                "schedule": [
                    {"cron": "0 2 * * *"}  # Run daily at 2 AM
                ],
            },
            "jobs": {
                "performance-tests": {
                    "runs-on": "ubuntu-latest",
                    "timeout-minutes": 120,
                    "steps": [
                        {"name": "Checkout code", "uses": "actions/checkout@v4"},
                        {"name": "Set up Docker Buildx", "uses": "docker/setup-buildx-action@v3"},
                        {"name": "Start test services", "run": "docker-compose -f docker-compose.test.yml up -d"},
                        {"name": "Wait for services", "run": "sleep 30"},
                    ],
                }
            },
        }

        # Add test steps
        for _i, test_config in enumerate(test_configs):
            workflow["jobs"]["performance-tests"]["steps"].append(
                {
                    "name": f"Run {test_config['name']} tests",
                    "run": f"python -m performance_testing.docker.run_tests {test_config['module']}",
                    "env": {"TEST_CONFIG": json.dumps(test_config)},
                }
            )

        # Add artifact upload
        workflow["jobs"]["performance-tests"]["steps"].extend(
            [
                {
                    "name": "Upload performance test results",
                    "uses": "actions/upload-artifact@v4",
                    "if": "always()",
                    "with": {"name": "performance-test-results", "path": "performance_reports/"},
                },
                {
                    "name": "Stop test services",
                    "run": "docker-compose -f docker-compose.test.yml down",
                    "if": "always()",
                },
            ]
        )

        return yaml.dump(workflow, default_flow_style=False)

    def generate_docker_compose_test(self, services: list[dict[str, Any]]) -> str:
        """Generate docker-compose.test.yml for test services."""
        compose = {"version": "3.8", "services": {}, "networks": {"test-network": {"driver": "bridge"}}}

        for service in services:
            compose["services"][service["name"]] = {
                "image": service["image"],
                "environment": service.get("environment", {}),
                "ports": [f"{host}:{container}" for container, host in service.get("ports", {}).items()],
                "networks": ["test-network"],
                "healthcheck": service.get(
                    "healthcheck",
                    {"test": ["CMD", "echo", "healthy"], "interval": "30s", "timeout": "10s", "retries": 3},
                ),
            }

        return yaml.dump(compose, default_flow_style=False)

    def generate_performance_report_comment(self, test_results: dict[str, Any]) -> str:
        """Generate GitHub PR comment with performance test results."""
        if not test_results.get("success", False):
            return f"""
## ❌ Performance Tests Failed

The performance tests failed with the following error:

```
{test_results.get("error", "Unknown error")}
```

**Duration:** {test_results.get("duration_seconds", 0):.1f} seconds
**Exit Code:** {test_results.get("exit_code", 1)}

Please check the test logs for more details.
"""

        results = test_results.get("test_results", [])
        successful_tests = [r for r in results if r.get("success", False)]
        failed_tests = [r for r in results if not r.get("success", False)]

        comment = f"""
## 📊 Performance Test Results

**Summary:** {len(successful_tests)}/{len(results)} tests passed
**Duration:** {test_results.get("duration_seconds", 0):.1f} seconds

"""

        if successful_tests:
            comment += "### ✅ Successful Tests\n\n"
            for test in successful_tests:
                comment += f"- **{test['test_name']}**: Passed\n"

        if failed_tests:
            comment += "\n### ❌ Failed Tests\n\n"
            for test in failed_tests:
                comment += f"- **{test['test_name']}**: {test.get('error', 'Unknown error')}\n"

        # Add monitoring data summary if available
        monitoring_data = test_results.get("monitoring_data", [])
        if monitoring_data:
            comment += "\n### 📈 Resource Usage\n\n"

            # Calculate average resource usage
            avg_cpu = (
                sum(
                    sum(container["cpu_percent"] for container in sample["containers"].values())
                    for sample in monitoring_data
                )
                / len(monitoring_data)
                if monitoring_data
                else 0
            )

            avg_memory = (
                sum(
                    sum(container["memory_usage_mb"] for container in sample["containers"].values())
                    for sample in monitoring_data
                )
                / len(monitoring_data)
                if monitoring_data
                else 0
            )

            comment += f"- **Average CPU Usage:** {avg_cpu:.1f}%\n"
            comment += f"- **Average Memory Usage:** {avg_memory:.1f} MB\n"

        return comment


# Export main classes
__all__ = ["CIPipelineIntegration", "DockerPerformanceTestRunner", "DockerTestConfig"]
