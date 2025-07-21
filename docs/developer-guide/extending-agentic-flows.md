# Developer Guide: Extending Agentic Flows

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Adding New Flow Types](#adding-new-flow-types)
3. [Creating Custom Tools](#creating-custom-tools)
4. [Implementing Flow Processors](#implementing-flow-processors)
5. [UI Component Development](#ui-component-development)
6. [Testing Strategies](#testing-strategies)
7. [Performance Optimization](#performance-optimization)
8. [Security Considerations](#security-considerations)

## Architecture Overview

The Agentic Flows system follows a hexagonal architecture pattern:

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                      │
│         (React Components, GraphQL)             │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│              Application Layer                   │
│    (Application Services, Use Cases)            │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│               Domain Layer                       │
│   (Entities, Value Objects, Domain Services)    │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│           Infrastructure Layer                   │
│    (Repositories, External Services, DB)        │
└─────────────────────────────────────────────────┘
```

### Key Components

1. **Domain Entities**
   - `AgenticFlow`: Core flow configuration
   - `AgenticFlowResult`: Execution results
   - `AgenticFlowType`: Enumeration of flow types

2. **Application Services**
   - `AgenticFlowApplicationService`: Flow management
   - `AgenticFlowExecutor`: Flow execution orchestration
   - `AgenticFlowAnalyticsService`: Analytics collection

3. **Infrastructure Adapters**
   - `PostgresAgenticFlowRepository`: Persistence
   - `McpLlmServiceAdapter`: LLM integration
   - `RedisFlowCacheAdapter`: Caching layer

## Adding New Flow Types

### Step 1: Define the Flow Type

Add to `AgenticFlowType.java`:

```java
public enum AgenticFlowType {
    // Existing types...
    CUSTOM_REASONING("Custom Reasoning", 
        "Implements custom reasoning pattern",
        Arrays.asList("reasoningSteps", "evaluationCriteria"));
    
    private final String displayName;
    private final String description;
    private final List<String> requiredParameters;
    
    // Constructor and getters...
}
```

### Step 2: Create Domain Model

Create `CustomReasoningFlowResult.java`:

```java
@Value
@Builder
public class CustomReasoningFlowResult implements FlowSpecificResult {
    String finalAnswer;
    List<ReasoningStep> reasoningSteps;
    Map<String, Double> evaluationScores;
    Double overallConfidence;
    
    @Value
    @Builder
    public static class ReasoningStep {
        int stepNumber;
        String description;
        String output;
        Double confidence;
        Duration duration;
    }
}
```

### Step 3: Implement Flow Processor

Create `CustomReasoningFlowProcessor.java`:

```java
@Component
@Slf4j
public class CustomReasoningFlowProcessor implements AgenticFlowProcessor {
    
    private final LlmServicePort llmService;
    
    @Override
    public AgenticFlowType getSupportedType() {
        return AgenticFlowType.CUSTOM_REASONING;
    }
    
    @Override
    public CompletableFuture<AgenticFlowResult> process(
            AgenticFlow flow, 
            String prompt, 
            ExecutionContext context) {
        
        Map<String, Object> config = flow.getConfiguration();
        List<String> reasoningSteps = (List<String>) config.get("reasoningSteps");
        
        return CompletableFuture.supplyAsync(() -> {
            List<ReasoningStep> steps = new ArrayList<>();
            StringBuilder reasoning = new StringBuilder();
            
            // Execute each reasoning step
            for (int i = 0; i < reasoningSteps.size(); i++) {
                String stepPrompt = buildStepPrompt(
                    prompt, 
                    reasoningSteps.get(i), 
                    steps
                );
                
                LlmResponse response = llmService.generateResponse(
                    LlmRequest.builder()
                        .prompt(stepPrompt)
                        .temperature(0.7)
                        .maxTokens(500)
                        .build()
                ).join();
                
                ReasoningStep step = ReasoningStep.builder()
                    .stepNumber(i + 1)
                    .description(reasoningSteps.get(i))
                    .output(response.getContent())
                    .confidence(response.getConfidence())
                    .build();
                
                steps.add(step);
                reasoning.append(formatStep(step));
            }
            
            // Generate final answer
            String finalPrompt = buildFinalPrompt(prompt, steps);
            LlmResponse finalResponse = llmService.generateResponse(
                LlmRequest.builder()
                    .prompt(finalPrompt)
                    .temperature(0.5)
                    .build()
            ).join();
            
            return AgenticFlowResult.builder()
                .flowId(flow.getId())
                .flowType(AgenticFlowType.CUSTOM_REASONING)
                .finalAnswer(finalResponse.getContent())
                .reasoning(reasoning.toString())
                .confidence(calculateOverallConfidence(steps, finalResponse))
                .metadata(Map.of(
                    "steps", steps,
                    "evaluationScores", evaluateResults(steps)
                ))
                .build();
        });
    }
}
```

### Step 4: Register Flow Processor

Update `FlowProcessorRegistry.java`:

```java
@Component
public class FlowProcessorRegistry {
    private final Map<AgenticFlowType, AgenticFlowProcessor> processors;
    
    @Autowired
    public FlowProcessorRegistry(List<AgenticFlowProcessor> processorList) {
        this.processors = processorList.stream()
            .collect(Collectors.toMap(
                AgenticFlowProcessor::getSupportedType,
                Function.identity()
            ));
    }
    
    public AgenticFlowProcessor getProcessor(AgenticFlowType type) {
        AgenticFlowProcessor processor = processors.get(type);
        if (processor == null) {
            throw new UnsupportedFlowTypeException(
                "No processor found for flow type: " + type
            );
        }
        return processor;
    }
}
```

### Step 5: Add UI Support

Create configuration component:

```typescript
// CustomReasoningConfig.tsx
interface CustomReasoningConfigProps {
  value: CustomReasoningConfiguration;
  onChange: (config: CustomReasoningConfiguration) => void;
}

export const CustomReasoningConfig: React.FC<CustomReasoningConfigProps> = ({
  value,
  onChange
}) => {
  const [steps, setSteps] = useState(value.reasoningSteps || []);
  
  const addStep = () => {
    setSteps([...steps, '']);
    onChange({ ...value, reasoningSteps: [...steps, ''] });
  };
  
  const updateStep = (index: number, step: string) => {
    const newSteps = [...steps];
    newSteps[index] = step;
    setSteps(newSteps);
    onChange({ ...value, reasoningSteps: newSteps });
  };
  
  return (
    <div>
      <h4>Reasoning Steps</h4>
      {steps.map((step, index) => (
        <Form.Item key={index} label={`Step ${index + 1}`}>
          <Input.TextArea
            value={step}
            onChange={(e) => updateStep(index, e.target.value)}
            placeholder="Describe reasoning step..."
          />
        </Form.Item>
      ))}
      <Button onClick={addStep} icon={<PlusOutlined />}>
        Add Step
      </Button>
      
      <Form.Item label="Evaluation Criteria">
        <Select
          mode="multiple"
          value={value.evaluationCriteria}
          onChange={(criteria) => 
            onChange({ ...value, evaluationCriteria: criteria })
          }
        >
          <Option value="logical_consistency">Logical Consistency</Option>
          <Option value="factual_accuracy">Factual Accuracy</Option>
          <Option value="completeness">Completeness</Option>
          <Option value="clarity">Clarity</Option>
        </Select>
      </Form.Item>
    </div>
  );
};
```

## Creating Custom Tools

### Step 1: Define Tool Interface

```java
public interface ExternalTool {
    String getName();
    String getDescription();
    List<ToolParameter> getParameters();
    CompletableFuture<ToolResult> execute(Map<String, Object> parameters);
}

@Value
@Builder
public class ToolParameter {
    String name;
    String type;
    String description;
    boolean required;
    Object defaultValue;
}

@Value
@Builder
public class ToolResult {
    boolean success;
    String output;
    Map<String, Object> metadata;
    String error;
}
```

### Step 2: Implement Custom Tool

```java
@Component
@Slf4j
public class DatabaseQueryTool implements ExternalTool {
    
    private final DataSource dataSource;
    
    @Override
    public String getName() {
        return "database_query";
    }
    
    @Override
    public String getDescription() {
        return "Execute read-only SQL queries against the knowledge base";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.builder()
                .name("query")
                .type("string")
                .description("SQL SELECT query")
                .required(true)
                .build(),
            ToolParameter.builder()
                .name("limit")
                .type("integer")
                .description("Maximum rows to return")
                .required(false)
                .defaultValue(10)
                .build()
        );
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            String query = (String) parameters.get("query");
            Integer limit = (Integer) parameters.getOrDefault("limit", 10);
            
            // Validate query is read-only
            if (!isReadOnlyQuery(query)) {
                return ToolResult.builder()
                    .success(false)
                    .error("Only SELECT queries are allowed")
                    .build();
            }
            
            try (Connection conn = dataSource.getConnection()) {
                // Add safety limit
                String safeQuery = query + " LIMIT " + limit;
                
                try (PreparedStatement stmt = conn.prepareStatement(safeQuery);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData metadata = rs.getMetaData();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= metadata.getColumnCount(); i++) {
                            row.put(metadata.getColumnName(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                    
                    return ToolResult.builder()
                        .success(true)
                        .output(formatResults(results))
                        .metadata(Map.of(
                            "rowCount", results.size(),
                            "executionTime", System.currentTimeMillis()
                        ))
                        .build();
                }
            } catch (SQLException e) {
                log.error("Database query failed", e);
                return ToolResult.builder()
                    .success(false)
                    .error("Query execution failed: " + e.getMessage())
                    .build();
            }
        });
    }
    
    private boolean isReadOnlyQuery(String query) {
        String normalized = query.trim().toUpperCase();
        return normalized.startsWith("SELECT") && 
               !normalized.contains("INSERT") &&
               !normalized.contains("UPDATE") &&
               !normalized.contains("DELETE") &&
               !normalized.contains("DROP") &&
               !normalized.contains("ALTER");
    }
}
```

### Step 3: Register Tool

```java
@Configuration
public class ToolConfiguration {
    
    @Bean
    public ToolRegistry toolRegistry(List<ExternalTool> tools) {
        Map<String, ExternalTool> toolMap = tools.stream()
            .collect(Collectors.toMap(
                ExternalTool::getName,
                Function.identity()
            ));
        
        return new ToolRegistry(toolMap);
    }
}
```

## UI Component Development

### Creating Flow Visualization Components

```typescript
// FlowVisualization.tsx
interface FlowVisualizationProps {
  result: AgenticFlowResult;
  interactive?: boolean;
}

export const FlowVisualization: React.FC<FlowVisualizationProps> = ({
  result,
  interactive = true
}) => {
  const renderVisualization = () => {
    switch (result.flowType) {
      case 'TREE_OF_THOUGHTS':
        return <TreeVisualization data={result.thoughtTree} />;
      
      case 'MULTI_AGENT_RED_TEAM':
        return <MultiAgentDebate perspectives={result.perspectives} />;
      
      case 'CUSTOM_REASONING':
        return <ReasoningSteps steps={result.metadata.steps} />;
      
      default:
        return <DefaultVisualization result={result} />;
    }
  };
  
  return (
    <Card className="flow-visualization">
      <div className="visualization-header">
        <h3>{getFlowDisplayName(result.flowType)}</h3>
        <ConfidenceIndicator value={result.confidence} />
      </div>
      
      <div className="visualization-content">
        {renderVisualization()}
      </div>
      
      {interactive && (
        <div className="visualization-actions">
          <Button onClick={() => exportVisualization(result)}>
            Export
          </Button>
          <Button onClick={() => shareVisualization(result)}>
            Share
          </Button>
        </div>
      )}
    </Card>
  );
};

// Tree visualization component
const TreeVisualization: React.FC<{ data: ThoughtTree }> = ({ data }) => {
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  
  const treeData = useMemo(() => 
    convertToD3Format(data), [data]
  );
  
  return (
    <div className="tree-visualization">
      <ResponsiveTree
        data={treeData}
        onNodeClick={(node) => setSelectedNode(node.id)}
        nodeSize={20}
        labelOffset={12}
        // D3 tree configuration
      />
      
      {selectedNode && (
        <NodeDetails 
          node={findNode(data, selectedNode)}
          onClose={() => setSelectedNode(null)}
        />
      )}
    </div>
  );
};
```

### Creating Custom Hooks

```typescript
// useAgenticFlow.ts
export const useAgenticFlow = (flowId: string) => {
  const [flow, setFlow] = useState<AgenticFlow | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  
  const execute = useCallback(async (prompt: string, context?: ExecutionContext) => {
    try {
      const result = await debateClient.executeFlow(flowId, {
        prompt,
        context
      });
      return result;
    } catch (err) {
      setError(err as Error);
      throw err;
    }
  }, [flowId]);
  
  const updateConfiguration = useCallback(async (config: FlowConfiguration) => {
    try {
      const updated = await debateClient.updateFlow(flowId, { 
        configuration: config 
      });
      setFlow(updated);
      return updated;
    } catch (err) {
      setError(err as Error);
      throw err;
    }
  }, [flowId]);
  
  useEffect(() => {
    const loadFlow = async () => {
      try {
        setLoading(true);
        const flowData = await debateClient.getFlow(flowId);
        setFlow(flowData);
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };
    
    loadFlow();
  }, [flowId]);
  
  return {
    flow,
    loading,
    error,
    execute,
    updateConfiguration
  };
};
```

## Testing Strategies

### Unit Testing Flow Processors

```java
@ExtendWith(MockitoExtension.class)
class CustomReasoningFlowProcessorTest {
    
    @Mock
    private LlmServicePort llmService;
    
    @InjectMocks
    private CustomReasoningFlowProcessor processor;
    
    @Test
    void shouldProcessCustomReasoningFlow() {
        // Given
        AgenticFlow flow = createTestFlow();
        String prompt = "Analyze the impact of AI on society";
        
        when(llmService.generateResponse(any()))
            .thenReturn(CompletableFuture.completedFuture(
                LlmResponse.builder()
                    .content("Step output")
                    .confidence(0.85)
                    .build()
            ));
        
        // When
        CompletableFuture<AgenticFlowResult> future = 
            processor.process(flow, prompt, new ExecutionContext());
        
        AgenticFlowResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFlowType()).isEqualTo(AgenticFlowType.CUSTOM_REASONING);
        assertThat(result.getConfidence()).isGreaterThan(0.0);
        
        verify(llmService, times(4)).generateResponse(any()); // 3 steps + final
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class AgenticFlowIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateAndExecuteFlow() throws Exception {
        // Create flow
        String createResponse = mockMvc.perform(post("/api/v1/debates/{debateId}/agentic-flows", "debate-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Test Flow",
                        "flowType": "CUSTOM_REASONING",
                        "configuration": {
                            "reasoningSteps": ["Step 1", "Step 2", "Step 3"]
                        }
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        String flowId = JsonPath.read(createResponse, "$.id");
        
        // Execute flow
        mockMvc.perform(post("/api/v1/agentic-flows/{flowId}/execute", flowId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "prompt": "Test prompt",
                        "context": {
                            "debateId": "debate-123"
                        }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.confidence").isNumber());
    }
}
```

### Performance Testing

```java
@Test
void shouldHandleConcurrentFlowExecutions() {
    int concurrentExecutions = 50;
    CountDownLatch latch = new CountDownLatch(concurrentExecutions);
    List<CompletableFuture<AgenticFlowResult>> futures = new ArrayList<>();
    
    for (int i = 0; i < concurrentExecutions; i++) {
        CompletableFuture<AgenticFlowResult> future = 
            flowExecutor.execute(flow, "Test prompt " + i)
                .whenComplete((result, error) -> latch.countDown());
        
        futures.add(future);
    }
    
    // Wait for all executions
    assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
    
    // Verify all completed successfully
    List<AgenticFlowResult> results = futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    
    assertThat(results).hasSize(concurrentExecutions);
    assertThat(results).allMatch(r -> r.getStatus() == FlowStatus.SUCCESS);
}
```

## Performance Optimization

### Caching Strategy

```java
@Component
public class FlowResultCache {
    private final RedisTemplate<String, AgenticFlowResult> redisTemplate;
    private final Duration defaultTtl = Duration.ofMinutes(15);
    
    public Optional<AgenticFlowResult> get(String cacheKey) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(cacheKey));
    }
    
    public void put(String cacheKey, AgenticFlowResult result) {
        redisTemplate.opsForValue().set(cacheKey, result, defaultTtl);
    }
    
    public String generateCacheKey(AgenticFlow flow, String prompt) {
        return String.format("flow:%s:prompt:%s", 
            flow.getId(), 
            DigestUtils.sha256Hex(prompt)
        );
    }
}
```

### Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    @Bean
    public TaskExecutor flowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("flow-executor-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### Database Optimization

```sql
-- Indexes for flow queries
CREATE INDEX idx_agentic_flows_org_status 
ON agentic_flows(organization_id, status);

CREATE INDEX idx_agentic_flows_debate_type 
ON agentic_flows(debate_id, flow_type);

-- Indexes for analytics
CREATE INDEX idx_flow_executions_flow_timestamp 
ON flow_executions(flow_id, timestamp DESC);

CREATE INDEX idx_flow_executions_org_type_timestamp 
ON flow_executions(organization_id, flow_type, timestamp DESC);

-- Partial index for active flows
CREATE INDEX idx_active_flows 
ON agentic_flows(organization_id, flow_type) 
WHERE status = 'ACTIVE';
```

## Security Considerations

### Input Validation

```java
@Component
public class FlowSecurityValidator {
    
    public void validateFlowConfiguration(
            AgenticFlowType type, 
            Map<String, Object> configuration) {
        
        // Type-specific validation
        switch (type) {
            case TOOL_CALLING_VERIFICATION:
                validateToolCallingConfig(configuration);
                break;
            case CONSTITUTIONAL_PROMPTING:
                validateConstitutionalConfig(configuration);
                break;
            // Other types...
        }
        
        // Common validation
        validateNoScriptInjection(configuration);
        validateConfigurationSize(configuration);
    }
    
    private void validateToolCallingConfig(Map<String, Object> config) {
        List<String> allowedTools = (List<String>) config.get("allowedTools");
        
        if (allowedTools == null || allowedTools.isEmpty()) {
            throw new ValidationException("Allowed tools must be specified");
        }
        
        // Validate against whitelist
        Set<String> validTools = Set.of(
            "web_search", "calculator", "database_query"
        );
        
        for (String tool : allowedTools) {
            if (!validTools.contains(tool)) {
                throw new ValidationException("Invalid tool: " + tool);
            }
        }
    }
    
    private void validateNoScriptInjection(Map<String, Object> config) {
        // Recursively check all string values
        checkForScriptInjection(config);
    }
}
```

### Rate Limiting

```java
@Component
public class FlowRateLimiter {
    private final RateLimiterRegistry rateLimiterRegistry;
    
    public void checkRateLimit(String userId, AgenticFlowType flowType) {
        String limiterName = String.format("flow:%s:%s", userId, flowType);
        
        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter(limiterName, () -> createConfig(flowType));
        
        if (!rateLimiter.tryAcquire()) {
            throw new RateLimitExceededException(
                "Rate limit exceeded for flow type: " + flowType
            );
        }
    }
    
    private RateLimiterConfig createConfig(AgenticFlowType flowType) {
        // Different limits for different flow types
        int limitPerMinute = switch (flowType) {
            case TOOL_CALLING_VERIFICATION -> 10;
            case TREE_OF_THOUGHTS -> 5;
            case MULTI_AGENT_RED_TEAM -> 8;
            default -> 20;
        };
        
        return RateLimiterConfig.custom()
            .limitForPeriod(limitPerMinute)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
    }
}
```

### Audit Logging

```java
@Aspect
@Component
@Slf4j
public class FlowAuditAspect {
    
    @Around("@annotation(Auditable)")
    public Object auditFlowOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        AuditEvent event = AuditEvent.builder()
            .timestamp(Instant.now())
            .userId(userId)
            .operation(operation)
            .resourceType("AGENTIC_FLOW")
            .build();
        
        try {
            Object result = joinPoint.proceed();
            event.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            event.setStatus("FAILURE");
            event.setError(e.getMessage());
            throw e;
        } finally {
            auditService.log(event);
        }
    }
}
```

## Best Practices

1. **Flow Design**
   - Keep flows focused on a single reasoning pattern
   - Provide clear configuration options
   - Include sensible defaults

2. **Error Handling**
   - Implement graceful degradation
   - Provide meaningful error messages
   - Log failures for debugging

3. **Performance**
   - Cache expensive operations
   - Use async processing where possible
   - Monitor execution times

4. **Security**
   - Validate all inputs
   - Implement proper authorization
   - Audit sensitive operations

5. **Testing**
   - Unit test flow processors
   - Integration test API endpoints
   - Performance test under load

6. **Documentation**
   - Document configuration options
   - Provide usage examples
   - Explain reasoning patterns