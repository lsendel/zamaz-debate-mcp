# Performance Testing Framework

A comprehensive performance and load testing framework for the MCP Debate System that provides production-level validation of system performance, scalability, and reliability.

## Features

### 🚀 Core Capabilities

- **Component Benchmarking**: Individual component performance testing
- **Load Testing**: Concurrent user simulation and stress testing
- **Resource Monitoring**: Real-time memory, CPU, and I/O monitoring
- **Database Performance**: Connection pooling and query optimization testing
- **API Load Testing**: HTTP endpoint performance validation
- **GitHub API Simulation**: Rate limiting and resilience testing
- **JMeter Integration**: Advanced HTTP load testing
- **Docker Support**: Containerized test execution
- **CI/CD Integration**: GitHub Actions and pipeline support

### 📊 Advanced Features

- **Memory Leak Detection**: Automated memory leak identification
- **Deadlock Testing**: Database deadlock simulation and recovery
- **Rate Limiting**: API rate limiting behavior validation
- **Spike Testing**: Sudden load increase handling
- **Endurance Testing**: Long-running stability validation
- **Performance Trending**: Historical performance analysis
- **Alert System**: Real-time performance threshold monitoring

## Quick Start

### Installation

```bash
# Install dependencies
pip install -r requirements.txt

# Install optional dependencies for full functionality
pip install docker jmeter matplotlib seaborn
```

### Basic Usage

```bash
# Run all tests with default configuration
python run_tests.py

# Run specific test suite
python run_tests.py --suite load --duration 300 --users 50

# Run in Docker (recommended for isolation)
python run_tests.py --docker --suite all

# Generate detailed reports
python run_tests.py --verbose --output-dir ./reports
```

### Configuration

Create a `test_config.json` file:

```json
{
  "test_duration_seconds": 300,
  "concurrent_users": 50,
  "database_url": "postgresql://user:pass@localhost:5432/testdb",
  "api_base_url": "http://localhost:8080",
  "enable_resource_monitoring": true,
  "memory_limit_mb": 1024,
  "cpu_limit_percent": 80.0,
  "test_suites": {
    "component_benchmarks": true,
    "load_tests": true,
    "github_api_simulation": true,
    "database_load_tests": true,
    "jmeter_tests": false
  }
}
```

## Test Suites

### 1. Component Benchmarks

Tests individual system components for performance characteristics:

```python
from tests.component_benchmarks import DatabaseBenchmark, MemoryIntensiveBenchmark

# Database operations benchmarking
db_benchmark = DatabaseBenchmark(config, db_url)
await db_benchmark.execute_test()

# Memory usage patterns
memory_benchmark = MemoryIntensiveBenchmark(config)
await memory_benchmark.execute_test()
```

**Features:**
- Database query performance
- API endpoint response times
- Memory allocation patterns
- LLM integration performance

### 2. Load Testing

Simulates concurrent user load with various patterns:

```python
from tests.load_tests import HTTPLoadTest, LoadTestScenario

# HTTP load testing
scenario = LoadTestScenario(
    name='api_load_test',
    max_users=100,
    duration_seconds=300,
    ramp_up_duration=60
)

load_test = HTTPLoadTest(config, scenario, api_base_url, endpoints)
await load_test.execute_test()
```

**Load Patterns:**
- Constant load
- Ramp-up/ramp-down
- Spike testing
- Burst patterns

### 3. GitHub API Simulation

Tests API resilience and rate limiting:

```python
from tests.github_api_simulation import GitHubAPIRateLimitTest

# Rate limiting simulation
rate_limit_test = GitHubAPIRateLimitTest(config, rate_limit_config)
await rate_limit_test.execute_test()
```

**Scenarios:**
- Normal API usage
- Rate limit exhaustion
- Abuse detection
- Recovery testing

### 4. Database Load Testing

Comprehensive database performance validation:

```python
from tests.database_load_tests import DatabaseLoadTest

# Database under load
db_load_test = DatabaseLoadTest(config, db_config)
await db_load_test.execute_test()
```

**Tests:**
- Connection pool stress
- Transaction throughput
- Concurrent reads/writes
- Deadlock handling
- Index performance

### 5. JMeter Integration

Advanced HTTP load testing with JMeter:

```python
from jmeter.jmeter_integration import JMeterTestPlan, JMeterExecutor

# Create JMeter test plan
test_plan = JMeterTestPlan(
    name='MCP Load Test',
    thread_groups=[{
        'name': 'API Test',
        'threads': 100,
        'duration': 300
    }]
)

# Execute test
executor = JMeterExecutor()
result = executor.execute_test(test_plan_path, output_dir)
```

## Resource Monitoring

### Real-time Monitoring

The framework provides comprehensive resource monitoring:

```python
from monitoring.resource_monitor import ResourceMonitor, ResourceThresholds

# Configure monitoring
thresholds = ResourceThresholds(
    memory_critical_mb=1024,
    cpu_critical_percent=80.0,
    memory_growth_rate_mb_per_min=10.0
)

monitor = ResourceMonitor(thresholds)
monitor.start_monitoring()

# Add custom alerts
monitor.add_alert_callback(lambda type, level, data: print(f"Alert: {type} {level}"))
```

### Memory Leak Detection

Automated detection of memory leaks:

```python
# Detect memory leaks
leak_analysis = monitor.detect_memory_leaks(window_minutes=10)

if leak_analysis['leak_detected']:
    print(f"Memory leak detected: {leak_analysis['growth_rate_mb_per_min']:.2f} MB/min")
```

### Performance Profiling

Detailed performance analysis:

```python
from monitoring.resource_monitor import MemoryProfiler

profiler = MemoryProfiler()

# Take snapshots
profiler.take_snapshot('before_test')
# ... run tests ...
profiler.take_snapshot('after_test')

# Compare snapshots
comparison = profiler.compare_snapshots('before_test', 'after_test')
```

## Docker Integration

### Running Tests in Docker

```bash
# Run all tests in Docker
python run_tests.py --docker

# Custom Docker configuration
python -c "
from docker.docker_integration import DockerPerformanceTestRunner, DockerTestConfig

config = DockerTestConfig(
    memory_limit='2g',
    cpu_limit='2',
    test_timeout=3600
)

runner = DockerPerformanceTestRunner(config)
# ... run tests ...
"
```

### Docker Compose for Test Services

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: testuser
      POSTGRES_PASSWORD: testpass
    ports:
      - "5432:5432"
    
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    
  performance-tests:
    build: .
    depends_on:
      - postgres
      - redis
    environment:
      DATABASE_URL: postgresql://testuser:testpass@postgres:5432/testdb
      REDIS_URL: redis://redis:6379
    volumes:
      - ./reports:/app/reports
```

## CI/CD Integration

### GitHub Actions

Generate GitHub Actions workflow:

```python
from docker.docker_integration import CIPipelineIntegration

ci_integration = CIPipelineIntegration('github')

# Generate workflow
workflow = ci_integration.generate_github_workflow([
    {
        'name': 'Component Benchmarks',
        'module': 'component_benchmarks',
        'duration': 300
    },
    {
        'name': 'Load Tests',
        'module': 'load_tests',
        'duration': 600
    }
])

# Save to .github/workflows/performance.yml
with open('.github/workflows/performance.yml', 'w') as f:
    f.write(workflow)
```

### Example Workflow

```yaml
name: Performance Tests
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  performance-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 120
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
    
    - name: Install dependencies
      run: |
        pip install -r performance-testing/requirements.txt
    
    - name: Start test services
      run: |
        docker-compose -f docker-compose.test.yml up -d
        sleep 30
    
    - name: Run performance tests
      run: |
        cd performance-testing
        python run_tests.py --suite all --duration 300
    
    - name: Upload results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: performance-results
        path: performance-testing/reports/
```

## Reporting

### HTML Reports

Interactive HTML reports with charts and detailed analysis:

```python
from framework.reporting import PerformanceReporter

reporter = PerformanceReporter('reports')

# Generate comprehensive report
html_report = reporter.generate_html_report(test_results)
pdf_report = reporter.generate_pdf_report(test_results)
json_report = reporter.generate_json_report(test_results)
```

### Report Features

- **Performance Trends**: Historical performance analysis
- **Resource Usage**: Memory, CPU, and I/O charts
- **Error Analysis**: Detailed error patterns and frequencies
- **Comparison Reports**: Baseline vs current performance
- **Prometheus Integration**: Metrics export for monitoring

### Sample Report Structure

```
performance_reports/
├── comprehensive_performance_report.html
├── comprehensive_performance_report.pdf
├── comprehensive_performance_data.json
├── test_summary.json
├── monitoring/
│   ├── memory_usage.png
│   ├── cpu_usage.png
│   └── monitoring_report.json
└── jmeter_results/
    ├── results.jtl
    └── dashboard/
```

## Performance Thresholds

### Default Thresholds

```python
# Resource thresholds
ResourceThresholds(
    memory_warning_mb=512.0,
    memory_critical_mb=1024.0,
    cpu_warning_percent=70.0,
    cpu_critical_percent=90.0,
    max_response_time_ms=5000,
    max_error_rate=0.05
)

# Database thresholds
DatabaseLoadConfig(
    max_query_time_ms=5000,
    max_transaction_time_ms=10000,
    max_deadlock_rate=0.01,
    connection_timeout=30
)
```

### Custom Thresholds

```python
# Custom performance validation
def validate_performance(results):
    for result in results:
        if result.metrics.get('avg_response_time', 0) > 1000:
            raise AssertionError(f"Response time too high: {result.metrics['avg_response_time']}ms")
        
        if result.metrics.get('error_rate', 0) > 0.01:
            raise AssertionError(f"Error rate too high: {result.metrics['error_rate']}")
```

## Advanced Usage

### Custom Test Development

Create custom performance tests:

```python
from framework.base import PerformanceTestBase

class CustomPerformanceTest(PerformanceTestBase):
    async def setup_test(self):
        # Custom setup
        pass
    
    async def run_test(self):
        # Custom test logic
        for i in range(1000):
            start_time = time.time()
            # ... test operation ...
            duration = (time.time() - start_time) * 1000
            
            self.metrics.record_operation(
                'custom_operation',
                duration,
                success=True,
                metadata={'iteration': i}
            )
    
    async def cleanup_test(self):
        # Custom cleanup
        pass
```

### Performance Monitoring Integration

```python
# Prometheus metrics export
metrics_data = monitor.export_prometheus_metrics()

# Grafana dashboard integration
monitor.export_to_grafana(
    datasource_url="http://grafana:3000",
    api_key="your-api-key"
)
```

### Load Test Scenarios

```python
# Complex load patterns
scenarios = [
    LoadTestScenario(
        name='morning_peak',
        initial_users=10,
        max_users=200,
        ramp_up_duration=300,
        steady_state_duration=1800
    ),
    LoadTestScenario(
        name='evening_peak',
        initial_users=50,
        max_users=500,
        ramp_up_duration=600,
        steady_state_duration=3600
    )
]
```

## Troubleshooting

### Common Issues

1. **Database Connection Issues**
   ```bash
   # Check database connectivity
   pg_isready -h localhost -p 5432 -U testuser
   
   # Verify connection string
   psql "postgresql://testuser:testpass@localhost:5432/testdb"
   ```

2. **Memory Issues**
   ```bash
   # Monitor memory usage
   python -c "from monitoring.resource_monitor import ResourceMonitor; monitor = ResourceMonitor(); print(monitor.get_current_sample())"
   ```

3. **Docker Issues**
   ```bash
   # Check Docker resources
   docker system df
   docker system prune -f
   
   # Monitor container resources
   docker stats
   ```

### Debug Mode

Enable detailed debugging:

```bash
# Verbose logging
python run_tests.py --verbose

# Debug specific component
PYTHONPATH=. python -c "
import logging
logging.basicConfig(level=logging.DEBUG)
from tests.component_benchmarks import DatabaseBenchmark
# ... debug code ...
"
```

## Contributing

### Development Setup

```bash
# Clone repository
git clone https://github.com/your-repo/zamaz-debate-mcp.git
cd zamaz-debate-mcp/performance-testing

# Install development dependencies
pip install -r requirements-dev.txt

# Run tests
pytest tests/

# Code formatting
black .
ruff check .
```

### Adding New Tests

1. Create test class inheriting from `PerformanceTestBase`
2. Implement required methods: `setup_test`, `run_test`, `cleanup_test`
3. Add metrics recording with `self.metrics.record_operation`
4. Include test in appropriate test suite module
5. Update documentation

### Performance Best Practices

- Always clean up resources in `cleanup_test`
- Use appropriate sample sizes for statistical significance
- Include warmup and cooldown periods
- Monitor system resources during tests
- Use realistic test data and scenarios
- Document performance expectations and thresholds

## License

This performance testing framework is part of the MCP Debate System and follows the same license terms.

## Support

For issues and questions:
- Create GitHub issues for bugs and feature requests
- Check existing documentation and examples
- Review logs for detailed error information
- Use debug mode for troubleshooting