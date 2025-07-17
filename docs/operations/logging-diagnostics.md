# Logging and Diagnostics Guide

## Overview

This guide provides comprehensive information on the logging and diagnostics system implemented in the MCP (Model Context Protocol) project. The system includes structured logging, distributed tracing, performance monitoring, audit logging, and debugging utilities for both Java services and Python components.

The system provides comprehensive logging and diagnostics capabilities for troubleshooting, monitoring, and performance analysis across all services.

## Architecture

### Components

#### Java Services (Primary)
1. **Structured Logging** - JSON-formatted logs with correlation IDs using Logback
2. **Distributed Tracing** - OpenTelemetry-based request tracing across services
3. **Performance Monitoring** - Real-time performance metrics and profiling
4. **Audit Logging** - Comprehensive security and compliance event logging
5. **Diagnostic Endpoints** - HTTP API for real-time system inspection
6. **ELK Stack Integration** - Elasticsearch, Logstash, Kibana for log aggregation
7. **Alerting System** - ElastAlert for automated monitoring and notifications
8. **Debug Utilities** - Advanced debugging and troubleshooting tools

#### Python Components (GitHub Integration)
1. **Structured Logging** - JSON-formatted logs with correlation IDs
2. **Log Aggregation** - Loki for centralized log storage
3. **Diagnostic Collection** - Request tracing and profiling
4. **Diagnostic Endpoints** - HTTP API for troubleshooting

### Flow

#### Java Services Flow
```
Java Application → StructuredLogger → Logback → JSON logs
                                                    ↓
                                              Filebeat → Logstash → Elasticsearch
                                                    ↓
OpenTelemetry → Distributed Tracing → Jaeger/OTLP
                                                    ↓
PerformanceLogger → Metrics → Prometheus → Grafana
                                                    ↓
AuditLogger → Security Events → Elasticsearch → Kibana
                                                    ↓
DiagnosticController → Real-time Inspection → HTTP API
```

#### Python Components Flow
```
Application → Structured Logger → stdout/file
                                     ↓
                                 Promtail → Loki → Grafana
                                     ↓
                              Diagnostic Collector
                                     ↓
                              Redis (short-term)
                              PostgreSQL (long-term)
```

---

# Java Services Logging and Diagnostics

## Table of Contents

1. [Structured Logging](#structured-logging-java)
2. [Distributed Tracing](#distributed-tracing-java)
3. [Performance Monitoring](#performance-monitoring-java)
4. [Audit Logging](#audit-logging-java)
5. [Diagnostic Endpoints](#diagnostic-endpoints-java)
6. [ELK Stack Integration](#elk-stack-integration-java)
7. [Alerting and Monitoring](#alerting-and-monitoring-java)
8. [Debugging Utilities](#debugging-utilities-java)
9. [Troubleshooting Guide](#troubleshooting-guide-java)
10. [Best Practices](#best-practices-java)

## Structured Logging {#structured-logging-java}

### Overview
The MCP Java services use structured logging with JSON format for consistent, searchable log entries across all services.

### Key Components
- **StructuredLogger**: Central logging component with context enrichment
- **LogContext**: Context object for adding metadata to log entries
- **Correlation ID**: Unique identifier for tracking requests across services

### Usage Examples

```java
// Basic structured logging
LogContext context = LogContext.forRequest("12345", "create_debate");
structuredLogger.info("DebateService", "Debate created successfully", context);

// Adding metadata
LogContext context = LogContext.builder()
    .userId("user123")
    .organizationId("org456")
    .operation("create_debate")
    .build()
    .addMetadata("debateId", "debate789")
    .addMetadata("participants", 4);

structuredLogger.info("DebateService", "Debate created with participants", context);

// Error logging with exception
try {
    createDebate(request);
} catch (Exception e) {
    LogContext errorContext = LogContext.forError(e)
        .operation("create_debate")
        .userId(userId)
        .organizationId(organizationId);
    structuredLogger.error("DebateService", "Failed to create debate", errorContext);
}
```

### Log Levels and Usage
- **ERROR**: System errors, exceptions, security violations
- **WARN**: Performance issues, deprecated API usage, configuration warnings
- **INFO**: Business events, API requests, system state changes
- **DEBUG**: Detailed execution flow, variable values, internal state
- **TRACE**: Extremely detailed debugging information

### Correlation ID Tracking
```java
// Correlation IDs are automatically generated and tracked
LogContext context = LogContext.forRequest("operation", "create_debate");
// Correlation ID is automatically included

// Create child context for nested operations
LogContext childContext = context.createChild("validate_input");
// Maintains same correlation ID with new span
```

## Distributed Tracing {#distributed-tracing-java}

### Overview
Distributed tracing provides visibility into request flow across multiple services using OpenTelemetry.

### Key Features
- **Trace ID**: Unique identifier for end-to-end request flow
- **Span ID**: Identifier for individual operations within a trace
- **Context Propagation**: Automatic context passing between services
- **Custom Span Processor**: Enriches spans with business context

### Configuration
```yaml
mcp:
  tracing:
    enabled: true
    sampling:
      strategy: "parent_based"
      ratio: 0.1
    exporters:
      otlp:
        enabled: true
        endpoint: "http://otel-collector:4317"
      jaeger:
        enabled: false
        endpoint: "http://jaeger:14250"
      prometheus:
        enabled: true
        port: 9464
```

### Usage Examples

```java
// Automatic tracing with Spring WebMVC
@RestController
public class DebateController {
    
    @PostMapping("/debates")
    public ResponseEntity<Debate> createDebate(@RequestBody CreateDebateRequest request) {
        // Span is automatically created for this endpoint
        // Custom attributes are added by CustomSpanProcessor
        return debateService.createDebate(request);
    }
}

// Manual span creation
@Service
public class DebateService {
    
    private final Tracer tracer;
    
    public Debate createDebate(CreateDebateRequest request) {
        Span span = tracer.spanBuilder("create_debate")
            .setAttribute("debate.type", request.getType())
            .setAttribute("user.id", request.getUserId())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Business logic here
            Debate debate = processDebate(request);
            span.setStatus(StatusCode.OK);
            return debate;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Baggage for Context Propagation
```java
// Set baggage for propagation
Baggage baggage = Baggage.builder()
    .put("organization.id", organizationId)
    .put("user.id", userId)
    .put("session.id", sessionId)
    .build();

try (Scope scope = baggage.makeCurrent()) {
    // Make service calls - baggage will be propagated
    llmService.generateResponse(prompt);
}
```

## Performance Monitoring {#performance-monitoring-java}

### Overview
Performance monitoring tracks operation durations, memory usage, and system health metrics.

### Key Components
- **PerformanceLogger**: Tracks operation lifecycle and performance metrics
- **Custom Metrics**: Business-specific performance indicators
- **Threshold Alerts**: Automated alerts for performance degradation

### Usage Examples

```java
@Service
public class DebateService {
    
    private final PerformanceLogger performanceLogger;
    
    public Debate createDebate(CreateDebateRequest request) {
        // Track operation performance
        String operationId = performanceLogger.startOperation("create_debate", "DebateService");
        
        try {
            // Business logic
            Debate debate = processDebate(request);
            performanceLogger.endOperation(operationId, "success", null);
            return debate;
        } catch (Exception e) {
            performanceLogger.endOperation(operationId, "failure", e);
            throw e;
        }
    }
}
```

### Performance Thresholds
- **Fast**: < 1 second
- **Moderate**: 1-5 seconds  
- **Slow**: 5-10 seconds
- **Very Slow**: > 10 seconds

### System Metrics
```java
// Log system metrics periodically
@Scheduled(fixedRate = 60000) // Every minute
public void logSystemMetrics() {
    performanceLogger.logSystemMetrics();
}

// Get active operations summary
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void logActiveOperations() {
    performanceLogger.logActiveOperationsSummary();
}
```

## Audit Logging {#audit-logging-java}

### Overview
Comprehensive audit logging for security, compliance, and business event tracking.

### Event Types
- **Authentication**: Login, logout, password changes
- **Authorization**: Permission grants/denials, role changes
- **Data Access**: CRUD operations, data exports
- **Security**: Violations, suspicious activities
- **Compliance**: GDPR events, data retention actions
- **Business**: Debate creation, organization changes

### Usage Examples

```java
@Service
public class AuthenticationService {
    
    private final SecurityAuditLogger auditLogger;
    
    public AuthenticationResult authenticate(String username, String password) {
        try {
            // Authenticate user
            User user = performAuthentication(username, password);
            
            // Log successful authentication
            AuditEvent authEvent = AuditEvent.builder()
                .eventType(AuditEventType.AUTHENTICATION_SUCCESS)
                .description("User authenticated successfully")
                .actor(AuditEvent.AuditActor.builder()
                    .userId(user.getId())
                    .username(username)
                    .organizationId(user.getOrganizationId())
                    .build())
                .context(AuditEvent.AuditContext.builder()
                    .application("mcp-gateway")
                    .environment("production")
                    .authenticationRequired(true)
                    .build())
                .build();
            
            auditLogger.logAuditEvent(authEvent);
            
            return AuthenticationResult.success(user);
            
        } catch (AuthenticationException e) {
            // Log failed authentication
            AuditEvent failureEvent = AuditEvent.builder()
                .eventType(AuditEventType.AUTHENTICATION_FAILURE)
                .outcome(AuditEvent.AuditOutcome.FAILURE)
                .description("Authentication failed: " + e.getMessage())
                .actor(AuditEvent.AuditActor.builder()
                    .username(username)
                    .build())
                .build()
                .addDetail("reason", e.getMessage())
                .addTag("security_violation");
            
            auditLogger.logAuditEvent(failureEvent);
            throw e;
        }
    }
}
```

### Business Event Logging
```java
@Service
public class DebateService {
    
    private final SecurityAuditLogger auditLogger;
    
    public Debate createDebate(CreateDebateRequest request) {
        Debate debate = processDebate(request);
        
        // Log business event
        auditLogger.logBusinessEvent(
            AuditEventType.DEBATE_CREATED,
            request.getUserId(),
            request.getOrganizationId(),
            "debate",
            debate.getId(),
            "Debate created: " + debate.getTitle(),
            Map.of(
                "debateType", debate.getType(),
                "participantCount", debate.getParticipants().size(),
                "isPublic", debate.isPublic()
            )
        );
        
        return debate;
    }
}
```

### Compliance Features
- **GDPR Support**: Data access, export, deletion tracking
- **Risk Scoring**: Automatic risk assessment for events
- **Retention Policies**: Automated log retention management
- **Anonymization**: PII removal for long-term storage

## Diagnostic Endpoints {#diagnostic-endpoints-java}

### Available Endpoints

#### Health Check
```bash
curl http://localhost:8080/diagnostics/health
```

Response:
```json
{
  "timestamp": "2024-01-17T10:30:45.123Z",
  "status": "UP",
  "checks": {
    "DatabaseHealthIndicator": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1",
        "connectionPool": {
          "active": 5,
          "idle": 10,
          "max": 20
        }
      }
    }
  },
  "system": {
    "availableProcessors": 8,
    "freeMemory": 536870912,
    "totalMemory": 1073741824,
    "maxMemory": 2147483648
  },
  "jvm": {
    "uptime": 3600000,
    "startTime": 1705491045123,
    "version": "17.0.2",
    "vendor": "Eclipse Adoptium"
  },
  "threads": {
    "count": 42,
    "daemon": 38,
    "peak": 45,
    "totalStarted": 150
  },
  "memory": {
    "heap": {
      "used": 536870912,
      "committed": 1073741824,
      "max": 2147483648
    },
    "nonHeap": {
      "used": 134217728,
      "committed": 268435456,
      "max": -1
    }
  }
}
```

#### JVM Metrics
```bash
curl http://localhost:8080/diagnostics/jvm
```

#### Thread Dump
```bash
curl http://localhost:8080/diagnostics/threads
```

#### Performance Analysis
```bash
curl http://localhost:8080/diagnostics/performance
```

### Diagnostic Service Features
- **Memory Analysis**: Identify memory leaks and usage patterns
- **Thread Analysis**: Detect deadlocks and thread issues
- **Performance Bottlenecks**: Identify slow operations
- **System Health**: Overall system status assessment

## ELK Stack Integration {#elk-stack-integration-java}

### Architecture
- **Elasticsearch**: Log storage and search
- **Logstash**: Log processing and enrichment
- **Kibana**: Visualization and dashboards
- **Filebeat**: Log collection and shipping

### Starting ELK Stack
```bash
# Start ELK stack
docker-compose -f docker/elk/docker-compose.elk.yml up -d

# Check status
docker-compose -f docker/elk/docker-compose.elk.yml ps
```

### Log Processing Pipeline
1. **Java Applications** → Generate JSON logs
2. **Filebeat** → Collect and ship logs
3. **Logstash** → Parse, enrich, and route logs
4. **Elasticsearch** → Store and index logs
5. **Kibana** → Visualize and analyze logs

### Log Indices
- **mcp-logs-**: General application logs
- **mcp-performance-**: Performance metrics
- **mcp-security-**: Security and audit events
- **mcp-errors-**: Error logs and exceptions
- **mcp-business-**: Business events

### Search Examples
```bash
# Find all authentication failures
GET /mcp-security-*/_search
{
  "query": {
    "term": {
      "eventType": "AUTHENTICATION_FAILURE"
    }
  }
}

# Find slow operations
GET /mcp-performance-*/_search
{
  "query": {
    "range": {
      "duration": {
        "gte": 5000
      }
    }
  }
}

# Find errors for specific organization
GET /mcp-logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        {"term": {"level": "ERROR"}},
        {"term": {"organizationId": "org123"}}
      ]
    }
  }
}
```

## Alerting and Monitoring {#alerting-and-monitoring-java}

### Alert Types
- **Security Violations**: Immediate alerts for security events
- **Authentication Failures**: Multiple failed login attempts
- **Performance Degradation**: Slow response times
- **Error Rate Spikes**: Increased error rates
- **System Health**: Memory/CPU issues
- **Compliance Events**: GDPR and audit events

### Alert Configuration Files
Located in `docker/elk/elastalert/rules/`:
- `security-violations.yml`: Security event alerts
- `authentication-failures.yml`: Authentication failure alerts
- `performance-degradation.yml`: Performance issue alerts
- `error-rate-spike.yml`: Error rate spike alerts
- `system-health.yml`: System health alerts
- `compliance-violations.yml`: Compliance event alerts

### Alert Channels
- **Email**: Critical alerts to operations team
- **Slack**: Real-time notifications
- **PagerDuty**: Escalation for critical issues
- **Webhooks**: Integration with external systems

### Starting ElastAlert
```bash
# Start ElastAlert with ELK stack
docker-compose -f docker/elk/docker-compose.elk.yml up -d elastalert

# Check alert rules
docker exec mcp-elastalert elastalert-test-rule --config /opt/elastalert/config.yml /opt/elastalert/rules/security-violations.yml
```

## Debugging Utilities {#debugging-utilities-java}

### Debug Sessions
Track complex operations with detailed event logging:

```java
@Service
public class DebugExampleService {
    
    private final DebuggingUtilities debuggingUtilities;
    
    public void complexOperation() {
        // Start debug session
        String sessionId = debuggingUtilities.startDebugSession("complex_operation", "ExampleService");
        
        try {
            // Add debug events
            debuggingUtilities.addDebugEvent(sessionId, "validation", "Input validation started", 
                Map.of("inputSize", 100));
            
            // Add metadata
            debuggingUtilities.addSessionMetadata(sessionId, "userId", "user123");
            
            // Perform operation
            performComplexLogic();
            
            debuggingUtilities.addDebugEvent(sessionId, "completion", "Operation completed successfully", 
                Map.of("result", "success"));
            
        } finally {
            // End session and get summary
            Map<String, Object> summary = debuggingUtilities.endDebugSession(sessionId);
            log.info("Debug session summary: {}", summary);
        }
    }
}
```

### System Analysis
```java
// Generate comprehensive system health report
Map<String, Object> healthReport = debuggingUtilities.generateSystemHealthReport();

// Analyze memory usage
Map<String, Object> memoryAnalysis = debuggingUtilities.analyzeMemoryUsage();

// Generate thread dump
Map<String, Object> threadDump = debuggingUtilities.generateThreadDump();
```

### Performance Profiling
```java
// Get active debug sessions
Map<String, Object> activeSessions = debuggingUtilities.getActiveDebugSessions();

// Check system metrics
debuggingUtilities.logSystemMetrics();
```

## Troubleshooting Guide {#troubleshooting-guide-java}

### Common Issues and Solutions

#### High Memory Usage
**Symptoms**: Memory usage > 90%, OutOfMemoryError
**Investigation**:
```bash
# Check memory metrics
curl http://localhost:8080/diagnostics/jvm | jq '.memory'

# Analyze memory usage
curl http://localhost:8080/diagnostics/memory-analysis
```
**Solutions**:
- Increase heap size: `-Xmx4g`
- Analyze for memory leaks using debug utilities
- Review object lifecycle management

#### Slow Performance
**Symptoms**: Response times > 5 seconds, timeouts
**Investigation**:
```bash
# Check performance metrics
curl http://localhost:8080/diagnostics/performance

# Search for slow operations in Elasticsearch
curl -X GET "localhost:9200/mcp-performance-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "range": {
      "duration": {
        "gte": 5000
      }
    }
  }
}'
```
**Solutions**:
- Optimize database queries
- Add caching layers
- Review algorithm efficiency
- Scale horizontally

#### Authentication Failures
**Symptoms**: Multiple failed login attempts
**Investigation**:
```bash
# Search for authentication failures
curl -X GET "localhost:9200/mcp-security-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {
      "eventType": "AUTHENTICATION_FAILURE"
    }
  }
}'
```
**Solutions**:
- Review authentication configuration
- Investigate potential brute force attacks
- Check user credential validity

### Diagnostic Workflow

1. **Check System Health**: `curl http://localhost:8080/diagnostics/health`
2. **Review Recent Logs**: Search in Kibana for recent events
3. **Analyze Performance**: Use performance endpoints and metrics
4. **Check Trace Flow**: Follow correlation IDs across services
5. **Generate Debug Reports**: Use debugging utilities for detailed analysis
6. **Review Audit Logs**: Check for security or compliance issues

## Best Practices {#best-practices-java}

### Logging Best Practices
1. **Use structured logging**: Always use LogContext with metadata
2. **Include correlation IDs**: Track requests across services
3. **Appropriate log levels**: Use correct severity levels
4. **Sensitive data**: Never log passwords, tokens, or PII
5. **Performance impact**: Consider logging overhead in hot paths

### Monitoring Best Practices
1. **Set meaningful alerts**: Avoid alert fatigue
2. **Use appropriate thresholds**: Based on SLA requirements
3. **Regular review**: Update alerts based on system changes
4. **Documentation**: Maintain runbooks for common issues
5. **Testing**: Verify alert functionality regularly

### Security Best Practices
1. **Audit all access**: Log all authentication/authorization events
2. **Risk assessment**: Implement risk scoring for events
3. **Compliance tracking**: Monitor GDPR and regulatory requirements
4. **Incident response**: Have clear procedures for security events
5. **Regular reviews**: Audit security logs regularly

### Configuration Examples

#### Logback Configuration
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### Application Properties
```properties
# Logging configuration
logging.level.com.zamaz.mcp=INFO
logging.level.com.zamaz.mcp.security=DEBUG

# Tracing configuration
mcp.tracing.enabled=true
mcp.tracing.sampling.strategy=parent_based
mcp.tracing.sampling.ratio=0.1

# Performance monitoring
mcp.performance.enabled=true
mcp.performance.slow-threshold=5000
mcp.performance.very-slow-threshold=10000

# Audit logging
mcp.audit.enabled=true
mcp.audit.risk-scoring=true
mcp.audit.retention-days=2555
```

---

# Python Components (GitHub Integration)

## Structured Logging

### Usage

```python
from scripts.core.structured_logging import get_structured_logger, LogContext

logger = get_structured_logger(__name__)

# Basic logging
logger.info("Processing PR", pr_number=123, repo="example/repo")

# Error logging with exception
try:
    process_pr()
except Exception as e:
    logger.error("Failed to process PR", error=e, pr_number=123)

# Timing operations
with LogContext(logger, "pr_analysis", pr_number=123) as ctx:
    analyze_pr()
    # Automatically logs start, end, and duration
```

### Log Format

All logs are output as JSON with these fields:

```json
{
  "timestamp": "2024-01-17T10:30:45.123Z",
  "level": "INFO",
  "logger": "scripts.services.webhook_service",
  "message": "Processing webhook",
  "correlation_id": "abc-123-def",
  "service": "github-integration",
  "pr_number": 123,
  "repo": "example/repo",
  "duration_ms": 234.5
}
```

### Correlation IDs

Correlation IDs track requests across services:

```python
from scripts.core.structured_logging import set_correlation_id, get_correlation_id

# Set correlation ID (usually done by middleware)
set_correlation_id("unique-request-id")

# All subsequent logs include this ID
logger.info("Processing started")  # Includes correlation_id

# Pass to other services
headers = {"X-Correlation-ID": get_correlation_id()}
```

## Diagnostic Collection

### Starting Diagnostics

```python
from scripts.diagnostics.diagnostic_collector import DiagnosticCollector

collector = DiagnosticCollector(database, cache)

# Start diagnostic session
context = collector.start_diagnostic("pr_processing")

# Add traces
context.add_trace("webhook_received", {"pr_number": 123})
context.add_trace("analysis_started", {"analyzers": ["security", "style"]})

# Handle errors
try:
    process()
except Exception as e:
    context.add_error(e, {"step": "processing"})

# End session
collector.end_diagnostic(context.correlation_id)
```

### Using Context Manager

```python
with collector.diagnostic_context("pr_processing") as ctx:
    ctx.add_trace("step1", {"data": "value"})
    # Automatic error handling and timing
```

## Diagnostic Endpoints

### Health Check

```bash
# Detailed health check
curl http://localhost:5000/diagnostics/health/detailed

{
  "status": "healthy",
  "components": {
    "database": {"status": "healthy", "latency_ms": 2.3},
    "cache": {"status": "healthy", "latency_ms": 0.8},
    "github_api": {
      "status": "healthy",
      "rate_limit": {"remaining": 4823, "limit": 5000}
    }
  },
  "resources": {
    "cpu_percent": 23.4,
    "memory_mb": 512.3,
    "threads": 10
  }
}
```

### Request Tracing

```bash
# Get trace for a correlation ID
curl http://localhost:5000/diagnostics/trace/abc-123-def

{
  "correlation_id": "abc-123-def",
  "trace": [...],
  "timeline": "0.00ms | webhook_received\n  2.34ms | analysis_started..."
}
```

### Performance Summary

```bash
# Get performance metrics
curl http://localhost:5000/diagnostics/performance/summary

{
  "operations": {
    "pr_processing": {
      "count": 1234,
      "avg_ms": 234.5,
      "p95_ms": 567.8,
      "p99_ms": 890.1
    }
  },
  "bottlenecks": [
    {"operation": "github_api_call", "p95_ms": 890.1}
  ]
}
```

### Error Analysis

```bash
# Get error summary
curl http://localhost:5000/diagnostics/errors/summary

{
  "total_errors": 42,
  "unique_patterns": 5,
  "top_errors": [
    ["RateLimitError:API rate limit exceeded", 20],
    ["TimeoutError:Request timeout", 15]
  ]
}
```

## Log Aggregation with Loki

### Starting Services

```bash
# Start with log aggregation
docker-compose up -d loki promtail

# View logs in Grafana
# 1. Open http://localhost:3000
# 2. Add Loki data source: http://loki:3100
# 3. Explore logs with LogQL
```

### LogQL Queries

```promql
# All logs for a correlation ID
{job="java_services"} |= "correlation_id=\"abc-123-def\""

# Errors in last hour
{job="java_services"} |= "level=\"ERROR\"" | json

# Slow operations
{job="java_services"} | json | duration_ms > 1000

# PR processing timeline
{job="java_services"} 
  |= "correlation_id=\"abc-123-def\"" 
  | json 
  | line_format "{{.timestamp}} {{.message}}"
```

### Grafana Dashboards

Pre-configured dashboards:
- **Service Logs** - Real-time log streaming
- **Error Analysis** - Error patterns and trends
- **Performance Logs** - Slow operations and bottlenecks
- **Correlation Traces** - Request flow visualization

## Troubleshooting Guide

### Common Issues

#### 1. Missing Correlation IDs

**Symptom**: Logs don't have correlation IDs
**Solution**: Ensure middleware is configured:

```python
from scripts.core.structured_logging import CorrelationIdMiddleware

app.add_middleware(CorrelationIdMiddleware)
```

#### 2. High Memory Usage

**Diagnosis**:
```bash
curl http://localhost:5000/diagnostics/system-state
```

**Solutions**:
- Clear old diagnostics: `DELETE /diagnostics/cleanup?older_than_hours=24`
- Check for memory leaks in active diagnostics
- Review cache TTL settings

#### 3. Slow Performance

**Diagnosis**:
```bash
# Start profiling
curl -X POST http://localhost:5000/diagnostics/profile/pr_processing?duration_seconds=60

# Get results
curl http://localhost:5000/diagnostics/abc-123-def
```

**Analysis**:
- Check bottlenecks in performance summary
- Review trace timelines for slow steps
- Analyze profiling data

### Debug Mode

Enable debug logging:

```python
# In code
logger = get_structured_logger(__name__)
logger.logger.setLevel(logging.DEBUG)

# Environment variable
export LOG_LEVEL=DEBUG
```

### Testing Components

```bash
# Test database connection
curl -X POST http://localhost:5000/diagnostics/test-connection/database

# Test cache
curl -X POST http://localhost:5000/diagnostics/test-connection/cache

# Test GitHub API
curl -X POST http://localhost:5000/diagnostics/test-connection/github
```

## Best Practices

### 1. Use Structured Logging

Always include relevant context:
```python
logger.info("Processing PR", 
           pr_number=pr.number,
           repo=pr.repository,
           author=pr.author,
           files_count=len(pr.files))
```

### 2. Track Important Operations

```python
with LogContext(logger, "expensive_operation", resource_id=123):
    # Operation is automatically timed and logged
    perform_operation()
```

### 3. Handle Errors Properly

```python
try:
    risky_operation()
except SpecificError as e:
    logger.error("Known error occurred", 
                error=e,
                recovery_action="retrying",
                retry_count=retry_count)
    # Handle recovery
except Exception as e:
    logger.error("Unexpected error", error=e)
    # Add to diagnostics
    context.add_error(e, {"operation": "risky_operation"})
    raise
```

### 4. Monitor Performance

```python
perf_logger = PerformanceLogger(logger)

perf_logger.start_timer("api_call")
result = make_api_call()
perf_logger.end_timer("api_call", endpoint="/repos/get")

# Record metrics
perf_logger.record_metric("queue_size", queue.size())
```

### 5. Clean Up Old Data

Set up regular cleanup:
```yaml
# In cron or scheduler
0 2 * * * curl -X DELETE http://localhost:5000/diagnostics/cleanup?older_than_hours=168
```

## Integration Examples

### FastAPI Integration

```python
from fastapi import FastAPI, Request
from scripts.core.structured_logging import (
    CorrelationIdMiddleware, 
    get_structured_logger,
    set_correlation_id
)

app = FastAPI()
app.add_middleware(CorrelationIdMiddleware)

logger = get_structured_logger(__name__)

@app.post("/webhook")
async def handle_webhook(request: Request):
    # Correlation ID is automatically set by middleware
    logger.info("Webhook received", 
               path=request.url.path,
               method=request.method)
    
    # Process webhook
    return {"status": "processed"}
```

### Background Task Integration

```python
async def process_pr_task(pr_data: dict, correlation_id: str):
    # Set correlation ID for background task
    set_correlation_id(correlation_id)
    
    logger = get_structured_logger(__name__)
    
    with collector.diagnostic_context("background_pr_processing") as ctx:
        logger.info("Starting background processing", pr_number=pr_data["number"])
        
        ctx.add_trace("task_started", {"pr_number": pr_data["number"]})
        
        # Process PR
        await process_pr(pr_data)
        
        ctx.add_trace("task_completed", {"status": "success"})
```

## Monitoring Alerts

Configure alerts based on logs:

```yaml
# Prometheus alert for high error rate in logs
- alert: HighErrorRate
  expr: |
    rate(log_entries_total{level="ERROR"}[5m]) > 10
  annotations:
    summary: "High error rate in logs"
    description: "More than 10 errors/sec in logs"

# Alert for missing correlation IDs
- alert: MissingCorrelationIds
  expr: |
    rate(log_entries_total{correlation_id=""}[5m]) / 
    rate(log_entries_total[5m]) > 0.1
  annotations:
    summary: "Many logs missing correlation IDs"
```

## Security Considerations

1. **Sensitive Data**: Never log passwords, tokens, or PII
2. **Log Retention**: Set appropriate retention policies
3. **Access Control**: Restrict access to diagnostic endpoints
4. **Rate Limiting**: Protect diagnostic endpoints from abuse

```python
# Example: Sanitize sensitive data
def sanitize_headers(headers: dict) -> dict:
    sensitive_headers = ['authorization', 'x-api-key', 'cookie']
    sanitized = {}
    for key, value in headers.items():
        if key.lower() in sensitive_headers:
            sanitized[key] = "***"
        else:
            sanitized[key] = value
    return sanitized

logger.info("Request received", headers=sanitize_headers(request.headers))
```