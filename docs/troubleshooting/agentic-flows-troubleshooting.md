# Agentic Flows Troubleshooting Guide

## Common Issues and Solutions

### 1. Flow Execution Failures

#### Symptom: "Flow execution timeout" error
**Possible Causes:**
- Flow configuration too complex
- LLM service slow/overloaded
- Network connectivity issues

**Solutions:**
1. **Reduce complexity:**
   ```json
   {
     "maxDepth": 2,        // Reduce from 3
     "branchingFactor": 2, // Reduce from 3
     "timeout": 60000      // Increase timeout
   }
   ```

2. **Check LLM service status:**
   ```bash
   curl -X GET http://localhost:5002/api/v1/health
   ```

3. **Monitor execution logs:**
   ```bash
   tail -f logs/agentic-flows.log | grep "execution_id"
   ```

#### Symptom: "Insufficient permissions" error
**Possible Causes:**
- User lacks flow execution permissions
- Organization limits exceeded
- JWT token expired

**Solutions:**
1. **Verify user permissions:**
   ```sql
   SELECT p.* FROM user_permissions p
   JOIN users u ON p.user_id = u.id
   WHERE u.email = 'user@example.com'
   AND p.resource_type = 'AGENTIC_FLOW';
   ```

2. **Check organization limits:**
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:5005/api/v1/organizations/{orgId}/limits
   ```

3. **Refresh JWT token:**
   ```javascript
   const refreshToken = async () => {
     const response = await fetch('/api/v1/auth/refresh', {
       method: 'POST',
       headers: {
         'Authorization': `Bearer ${refreshToken}`
       }
     });
     const { accessToken } = await response.json();
     localStorage.setItem('accessToken', accessToken);
   };
   ```

### 2. Low Confidence Scores

#### Symptom: Consistently low confidence scores (< 60%)
**Possible Causes:**
- Inappropriate flow type for task
- Poor prompt quality
- Model limitations

**Solutions:**
1. **Try different flow types:**
   - Complex reasoning → Tree of Thoughts
   - Factual queries → Tool-Calling Verification
   - Ethical questions → Constitutional Prompting

2. **Improve prompt clarity:**
   ```javascript
   // Poor prompt
   "What about AI?"
   
   // Better prompt
   "Analyze the potential benefits and risks of artificial intelligence in healthcare, considering patient privacy and diagnostic accuracy."
   ```

3. **Use flow combinations:**
   ```json
   {
     "primary": "TREE_OF_THOUGHTS",
     "validation": "CONFIDENCE_SCORING",
     "fallback": "ENSEMBLE_VOTING"
   }
   ```

### 3. Tool-Calling Issues

#### Symptom: "Tool execution failed" errors
**Possible Causes:**
- External service unavailable
- API rate limits
- Invalid tool parameters

**Solutions:**
1. **Check tool availability:**
   ```bash
   # Test web search tool
   curl -X POST http://localhost:5004/api/v1/tools/web_search/test
   
   # Test calculator tool
   curl -X POST http://localhost:5004/api/v1/tools/calculator/test
   ```

2. **Monitor rate limits:**
   ```javascript
   // Check response headers
   const checkRateLimits = (response) => {
     console.log('Rate Limit:', response.headers.get('X-RateLimit-Limit'));
     console.log('Remaining:', response.headers.get('X-RateLimit-Remaining'));
     console.log('Reset:', new Date(response.headers.get('X-RateLimit-Reset') * 1000));
   };
   ```

3. **Validate tool configuration:**
   ```json
   {
     "allowedTools": ["web_search", "calculator"],
     "toolTimeout": 15000,
     "maxRetries": 2,
     "fallbackBehavior": "continue_without_tool"
   }
   ```

### 4. Performance Issues

#### Symptom: Slow flow execution (> 10 seconds)
**Possible Causes:**
- Complex flow configurations
- Database query performance
- Caching not enabled

**Solutions:**
1. **Enable caching:**
   ```yaml
   agentic-flows:
     cache:
       enabled: true
       ttl: 900 # 15 minutes
       redis:
         host: localhost
         port: 6379
   ```

2. **Optimize database queries:**
   ```sql
   -- Add missing indexes
   CREATE INDEX CONCURRENTLY idx_flow_executions_created_at 
   ON flow_executions(created_at DESC);
   
   -- Analyze query performance
   EXPLAIN ANALYZE
   SELECT * FROM agentic_flows 
   WHERE organization_id = ? AND status = 'ACTIVE';
   ```

3. **Use async execution:**
   ```javascript
   // Instead of synchronous execution
   const result = await executeFlow(flowId, prompt);
   
   // Use async with callback
   const executionId = await startFlowExecution(flowId, prompt);
   subscribeToUpdates(executionId, (update) => {
     if (update.status === 'COMPLETED') {
       handleResult(update.result);
     }
   });
   ```

### 5. Configuration Errors

#### Symptom: "Invalid configuration" validation errors
**Possible Causes:**
- Missing required parameters
- Type mismatches
- Invalid parameter values

**Solutions:**
1. **Validate configuration schema:**
   ```javascript
   const validateFlowConfig = (flowType, config) => {
     const schema = getFlowSchema(flowType);
     const { error } = schema.validate(config);
     if (error) {
       console.error('Validation error:', error.details);
     }
   };
   ```

2. **Use configuration templates:**
   ```javascript
   const flowTemplates = {
     INTERNAL_MONOLOGUE: {
       prefix: "Let me think step by step:",
       temperature: 0.7,
       maxTokens: 1000
     },
     SELF_CRITIQUE_LOOP: {
       maxIterations: 3,
       improvementThreshold: 0.2,
       critiquePrompt: "Identify areas for improvement:"
     }
   };
   ```

### 6. UI Display Issues

#### Symptom: Flow results not displaying properly
**Possible Causes:**
- Missing result data
- Component rendering errors
- WebSocket connection issues

**Solutions:**
1. **Check data completeness:**
   ```javascript
   const validateFlowResult = (result) => {
     const required = ['flowId', 'flowType', 'status', 'timestamp'];
     const missing = required.filter(field => !result[field]);
     if (missing.length > 0) {
       console.error('Missing fields:', missing);
     }
   };
   ```

2. **Debug component rendering:**
   ```javascript
   // Add error boundaries
   class FlowResultErrorBoundary extends React.Component {
     componentDidCatch(error, errorInfo) {
       console.error('Flow result render error:', error, errorInfo);
       // Log to monitoring service
     }
     
     render() {
       if (this.state.hasError) {
         return <div>Unable to display flow result</div>;
       }
       return this.props.children;
     }
   }
   ```

3. **Monitor WebSocket connection:**
   ```javascript
   const socket = io('/flow-updates');
   
   socket.on('connect', () => {
     console.log('WebSocket connected');
   });
   
   socket.on('disconnect', (reason) => {
     console.error('WebSocket disconnected:', reason);
     // Implement reconnection logic
   });
   ```

## Debugging Tools

### 1. Flow Execution Tracer
```bash
# Enable detailed tracing
export AGENTIC_FLOW_TRACE=true
export AGENTIC_FLOW_TRACE_LEVEL=DEBUG

# View trace logs
tail -f logs/flow-trace.log | jq '.'
```

### 2. Performance Profiler
```javascript
// Browser console
performance.mark('flow-execution-start');
await executeFlow(flowId, prompt);
performance.mark('flow-execution-end');
performance.measure('flow-execution', 'flow-execution-start', 'flow-execution-end');

const measure = performance.getEntriesByName('flow-execution')[0];
console.log(`Flow execution took: ${measure.duration}ms`);
```

### 3. Database Query Monitor
```sql
-- Enable query logging
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_duration = on;
SELECT pg_reload_conf();

-- View slow queries
SELECT query, total_time, mean_time, calls
FROM pg_stat_statements
WHERE query LIKE '%agentic_flows%'
ORDER BY mean_time DESC
LIMIT 10;
```

## Health Checks

### System Health Check Script
```bash
#!/bin/bash
# check-agentic-flows-health.sh

echo "Checking Agentic Flows System Health..."

# Check services
services=("mcp-controller:5013" "mcp-llm:5002" "mcp-rag:5004")
for service in "${services[@]}"; do
  IFS=':' read -r name port <<< "$service"
  if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/health" | grep -q "200"; then
    echo "✓ $name is healthy"
  else
    echo "✗ $name is unhealthy"
  fi
done

# Check database
psql -h localhost -U postgres -d debate_db -c "SELECT COUNT(*) FROM agentic_flows;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✓ Database is accessible"
else
  echo "✗ Database is not accessible"
fi

# Check Redis
redis-cli ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✓ Redis is running"
else
  echo "✗ Redis is not running"
fi
```

### Monitoring Dashboard Queries

```sql
-- Flow execution statistics
SELECT 
  flow_type,
  COUNT(*) as total_executions,
  AVG(confidence) as avg_confidence,
  AVG(execution_time) as avg_execution_time,
  COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END)::float / COUNT(*) as success_rate
FROM flow_executions
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY flow_type
ORDER BY total_executions DESC;

-- Error analysis
SELECT 
  flow_type,
  error_message,
  COUNT(*) as error_count,
  MAX(created_at) as last_occurrence
FROM flow_executions
WHERE status = 'FAILED'
AND created_at > NOW() - INTERVAL '7 days'
GROUP BY flow_type, error_message
ORDER BY error_count DESC
LIMIT 20;

-- Performance trends
SELECT 
  DATE_TRUNC('hour', created_at) as hour,
  flow_type,
  AVG(execution_time) as avg_time,
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY execution_time) as p95_time
FROM flow_executions
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY hour, flow_type
ORDER BY hour DESC, flow_type;
```

## Emergency Procedures

### 1. Disable Problematic Flow
```bash
# Via API
curl -X PATCH http://localhost:5013/api/v1/agentic-flows/{flowId} \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'

# Via database (emergency)
psql -h localhost -U postgres -d debate_db -c \
  "UPDATE agentic_flows SET status = 'INACTIVE' WHERE id = 'flow-id';"
```

### 2. Clear Cache
```bash
# Clear all flow caches
redis-cli --scan --pattern "flow:*" | xargs redis-cli del

# Clear specific flow cache
redis-cli del "flow:${FLOW_ID}:*"
```

### 3. Reset Rate Limits
```bash
# Reset user rate limits
redis-cli --scan --pattern "rate_limit:*:${USER_ID}" | xargs redis-cli del

# Reset all rate limits (use with caution)
redis-cli --scan --pattern "rate_limit:*" | xargs redis-cli del
```

### 4. Emergency Rollback
```bash
# Rollback to previous version
kubectl rollout undo deployment/agentic-flows-processor

# Check rollback status
kubectl rollout status deployment/agentic-flows-processor
```

## Contact Support

If issues persist after trying these solutions:

1. **Collect diagnostic information:**
   ```bash
   ./collect-diagnostics.sh > diagnostics-$(date +%Y%m%d-%H%M%S).log
   ```

2. **Include in support ticket:**
   - Diagnostic log file
   - Flow configuration JSON
   - Error messages and stack traces
   - Steps to reproduce

3. **Contact channels:**
   - Email: support@zamaz-debate.com
   - Slack: #agentic-flows-support
   - Emergency: +1-800-ZAMAZ-911

## Preventive Measures

1. **Regular Health Checks**
   - Run health check script daily
   - Monitor dashboard metrics
   - Set up alerts for anomalies

2. **Configuration Reviews**
   - Validate configs before deployment
   - Use version control for configs
   - Test in staging environment

3. **Performance Monitoring**
   - Track execution times
   - Monitor resource usage
   - Identify bottlenecks early

4. **Security Audits**
   - Review access logs
   - Check for unusual patterns
   - Update dependencies regularly