# Design Document

## Overview

This document outlines the technical design for enhancing the Zamaz Debate MCP Services with advanced agentic flows. The system will implement multiple reasoning and decision-making patterns for AI agents, including Internal Monologue, Self-Critique Loop, Multi-Agent Red-Team, Tool-Calling Verification, and Retrieval-Augmented Generation with Re-ranking. The design follows hexagonal architecture principles and integrates seamlessly with the existing debate system.

## Architecture

### Hexagonal Architecture Implementation

The agentic flows system will follow the established hexagonal architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Debate UI     │  │   GraphQL API   │  │  REST API   │ │
│  │   Components    │  │   Endpoint      │  │  Endpoints  │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Agentic Flow  │  │   Debate        │  │  Analytics  │ │
│  │   Application   │  │   Application   │  │ Application │ │
│  │   Services      │  │   Services      │  │  Services   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Agentic Flow  │  │   Debate        │  │  Analytics  │ │
│  │   Domain        │  │   Domain        │  │   Domain    │ │
│  │   Services      │  │   Services      │  │  Services   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────┐ │
│  │   Database  │  │  External   │  │   Event     │  │ MCP │ │
│  │  Adapters   │  │   Tools     │  │   Bus       │  │ LLM │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────┘ │
└─────────────────────────────────────────────────────────────┘
```### Mod
ule Structure

The agentic flows system will be organized into the following modules:

#### Domain Modules
- **agentic-flow-domain**: Core domain entities, value objects, and services for agentic flows
- **debate-domain**: Existing debate domain entities and services (extended for agentic flows)
- **analytics-domain**: Domain entities and services for agentic flow analytics

#### Application Modules
- **agentic-flow-application**: Application services for managing agentic flows
- **debate-application**: Extended debate application services
- **analytics-application**: Analytics application services for agentic flows

#### Infrastructure Modules
- **agentic-flow-infrastructure**: Database adapters and external tool integrations
- **debate-infrastructure**: Extended debate infrastructure
- **analytics-infrastructure**: Analytics infrastructure for agentic flows

#### Presentation Modules
- **agentic-flow-ui**: UI components for configuring and visualizing agentic flows
- **agentic-flow-api**: GraphQL and REST API endpoints for agentic flows

## Components and Interfaces

### Core Domain Entities

#### Agentic Flow Domain
```java
// Domain Entity
public class AgenticFlow {
    private AgenticFlowId id;
    private AgenticFlowType type;
    private Map<String, Object> configuration;
    private AgenticFlowStatus status;
    private OrganizationId organizationId;
}

// Flow Type Enumeration
public enum AgenticFlowType {
    INTERNAL_MONOLOGUE,
    SELF_CRITIQUE_LOOP,
    MULTI_AGENT_RED_TEAM,
    TOOL_CALLING_VERIFICATION,
    RAG_WITH_RERANKING,
    CONFIDENCE_SCORING,
    CONSTITUTIONAL_PROMPTING,
    ENSEMBLE_VOTING,
    POST_PROCESSING_RULES,
    TREE_OF_THOUGHTS,
    STEP_BACK_PROMPTING,
    PROMPT_CHAINING
}

// Domain Service
public interface AgenticFlowDomainService {
    AgenticFlowResult processPrompt(AgenticFlow flow, PromptContext context);
    AgenticFlowConfiguration createFlowConfiguration(AgenticFlowType type, Map<String, Object> params);
    boolean validateFlowConfiguration(AgenticFlow flow);
}

// Repository Port
public interface AgenticFlowRepository {
    void save(AgenticFlow flow);
    Optional<AgenticFlow> findById(AgenticFlowId id);
    List<AgenticFlow> findByOrganization(OrganizationId orgId);
    List<AgenticFlow> findByType(AgenticFlowType type);
}
```#
### Debate Domain Extensions
```java
// Extended Debate Entity
public class Debate {
    // Existing fields
    private DebateId id;
    private String title;
    private List<Participant> participants;
    private DebateStatus status;
    
    // New fields for agentic flows
    private List<AgenticFlowConfiguration> agenticFlowConfigurations;
    private boolean showReasoningProcess;
    private Map<ParticipantId, List<AgenticFlowType>> participantFlows;
}

// Participant Extension
public class Participant {
    // Existing fields
    private ParticipantId id;
    private String name;
    private ParticipantType type;
    
    // New fields for agentic flows
    private List<AgenticFlowType> enabledFlows;
    private Map<AgenticFlowType, AgenticFlowConfiguration> flowConfigurations;
}

// Domain Service Extension
public interface DebateDomainService {
    // Existing methods
    Debate createDebate(CreateDebateCommand command);
    DebateResponse addParticipant(DebateId id, AddParticipantCommand command);
    
    // New methods for agentic flows
    DebateResponse configureAgenticFlows(DebateId id, ConfigureAgenticFlowsCommand command);
    DebateResponse processResponseWithAgenticFlows(DebateId id, ParticipantId participantId, String prompt);
}
```

#### Analytics Domain
```java
// Analytics Entity
public class AgenticFlowAnalytics {
    private AnalyticsId id;
    private AgenticFlowId flowId;
    private AgenticFlowType flowType;
    private DebateId debateId;
    private ParticipantId participantId;
    private Instant timestamp;
    private Duration processingTime;
    private int confidenceScore;
    private boolean flowChangedResponse;
    private Map<String, Object> metrics;
}

// Domain Service
public interface AnalyticsDomainService {
    void recordAgenticFlowExecution(AgenticFlowExecutionEvent event);
    AnalyticsReport generateFlowPerformanceReport(AnalyticsQuery query);
    List<AgenticFlowRecommendation> recommendFlowsForDebate(DebateContext context);
}

// Repository Port
public interface AnalyticsRepository {
    void saveAnalytics(AgenticFlowAnalytics analytics);
    List<AgenticFlowAnalytics> queryAnalytics(AnalyticsQuery query);
    Map<AgenticFlowType, PerformanceMetrics> getAggregatePerformance(OrganizationId orgId);
}
```### 
Application Services

#### Agentic Flow Application Service
```java
@Service
public class AgenticFlowApplicationService {
    private final AgenticFlowDomainService agenticFlowDomainService;
    private final AgenticFlowRepository agenticFlowRepository;
    private final LlmServiceClient llmServiceClient;
    private final AnalyticsApplicationService analyticsService;
    
    public AgenticFlowResponse createAgenticFlow(CreateAgenticFlowRequest request) {
        // Create and persist agentic flow configuration
        AgenticFlowConfiguration config = agenticFlowDomainService.createFlowConfiguration(
            request.getType(), request.getParameters());
        AgenticFlow flow = new AgenticFlow(request.getType(), config, request.getOrganizationId());
        agenticFlowRepository.save(flow);
        return AgenticFlowMapper.toResponse(flow);
    }
    
    public AgenticFlowExecutionResponse executeFlow(ExecuteAgenticFlowRequest request) {
        // Execute agentic flow with provided prompt
        AgenticFlow flow = agenticFlowRepository.findById(request.getFlowId())
            .orElseThrow(() -> new FlowNotFoundException(request.getFlowId()));
        
        PromptContext context = PromptContextMapper.fromRequest(request);
        AgenticFlowResult result = agenticFlowDomainService.processPrompt(flow, context);
        
        // Record analytics
        analyticsService.recordFlowExecution(flow, context, result);
        
        return AgenticFlowExecutionMapper.toResponse(result);
    }
}
```

#### Debate Application Service Extension
```java
@Service
public class DebateApplicationService {
    // Existing fields
    private final DebateDomainService debateDomainService;
    private final DebateRepository debateRepository;
    
    // New fields for agentic flows
    private final AgenticFlowApplicationService agenticFlowService;
    
    // Existing methods
    // ...
    
    // New methods for agentic flows
    public DebateResponse configureDebateAgenticFlows(ConfigureDebateFlowsRequest request) {
        Debate debate = debateRepository.findById(request.getDebateId())
            .orElseThrow(() -> new DebateNotFoundException(request.getDebateId()));
        
        ConfigureAgenticFlowsCommand command = ConfigureAgenticFlowsMapper.toCommand(request);
        DebateResponse response = debateDomainService.configureAgenticFlows(debate.getId(), command);
        
        return response;
    }
    
    public ParticipantResponseDto processParticipantResponse(ProcessResponseRequest request) {
        // Process participant response with configured agentic flows
        Debate debate = debateRepository.findById(request.getDebateId())
            .orElseThrow(() -> new DebateNotFoundException(request.getDebateId()));
        
        ParticipantResponse response = debateDomainService.processResponseWithAgenticFlows(
            debate.getId(), request.getParticipantId(), request.getPrompt());
        
        return ParticipantResponseMapper.toDto(response);
    }
}
```#### 
Analytics Application Service
```java
@Service
public class AnalyticsApplicationService {
    private final AnalyticsDomainService analyticsDomainService;
    private final AnalyticsRepository analyticsRepository;
    
    public void recordFlowExecution(AgenticFlow flow, PromptContext context, AgenticFlowResult result) {
        AgenticFlowExecutionEvent event = AgenticFlowExecutionEvent.builder()
            .flowId(flow.getId())
            .flowType(flow.getType())
            .debateId(context.getDebateId())
            .participantId(context.getParticipantId())
            .timestamp(Instant.now())
            .processingTime(result.getProcessingTime())
            .confidenceScore(result.getConfidenceScore())
            .flowChangedResponse(result.isResponseChanged())
            .metrics(result.getMetrics())
            .build();
        
        analyticsDomainService.recordAgenticFlowExecution(event);
    }
    
    public AnalyticsReportDto generatePerformanceReport(AnalyticsReportRequest request) {
        AnalyticsQuery query = AnalyticsQueryMapper.fromRequest(request);
        AnalyticsReport report = analyticsDomainService.generateFlowPerformanceReport(query);
        return AnalyticsReportMapper.toDto(report);
    }
    
    public List<AgenticFlowRecommendationDto> getFlowRecommendations(DebateContextDto contextDto) {
        DebateContext context = DebateContextMapper.fromDto(contextDto);
        List<AgenticFlowRecommendation> recommendations = 
            analyticsDomainService.recommendFlowsForDebate(context);
        return recommendations.stream()
            .map(AgenticFlowRecommendationMapper::toDto)
            .collect(Collectors.toList());
    }
}
```

### Infrastructure Adapters

#### Database Adapters
```java
@Repository
public class PostgresAgenticFlowRepository implements AgenticFlowRepository {
    private final JdbcTemplate jdbcTemplate;
    private final AgenticFlowRowMapper rowMapper;
    
    @Override
    public void save(AgenticFlow flow) {
        String sql = "INSERT INTO agentic_flows (id, type, configuration, status, organization_id) " +
                    "VALUES (?, ?, ?::jsonb, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET " +
                    "type = EXCLUDED.type, configuration = EXCLUDED.configuration, " +
                    "status = EXCLUDED.status, organization_id = EXCLUDED.organization_id";
        
        jdbcTemplate.update(sql, 
            flow.getId().getValue(),
            flow.getType().name(),
            objectMapper.writeValueAsString(flow.getConfiguration()),
            flow.getStatus().name(),
            flow.getOrganizationId().getValue());
    }
    
    @Override
    public Optional<AgenticFlow> findById(AgenticFlowId id) {
        try {
            String sql = "SELECT * FROM agentic_flows WHERE id = ?";
            AgenticFlow flow = jdbcTemplate.queryForObject(sql, rowMapper, id.getValue());
            return Optional.ofNullable(flow);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    // Other repository methods
}
```#### E
xternal Tool Adapters
```java
@Service
public class WebSearchToolAdapter implements ExternalToolPort {
    private final WebSearchClient webSearchClient;
    
    @Override
    public ToolResponse executeToolCall(ToolCall toolCall) {
        if (!"web_search".equals(toolCall.getTool())) {
            throw new UnsupportedToolException(toolCall.getTool());
        }
        
        String query = toolCall.getParameters().get("query").toString();
        List<SearchResult> results = webSearchClient.search(query);
        
        return ToolResponse.builder()
            .toolName("web_search")
            .results(results)
            .timestamp(Instant.now())
            .build();
    }
}

@Service
public class CalculatorToolAdapter implements ExternalToolPort {
    @Override
    public ToolResponse executeToolCall(ToolCall toolCall) {
        if (!"calculator".equals(toolCall.getTool())) {
            throw new UnsupportedToolException(toolCall.getTool());
        }
        
        String expression = toolCall.getParameters().get("expression").toString();
        Object result = evaluateExpression(expression);
        
        return ToolResponse.builder()
            .toolName("calculator")
            .result(result)
            .timestamp(Instant.now())
            .build();
    }
    
    private Object evaluateExpression(String expression) {
        // Use a safe expression evaluator library
        // This is a simplified example
        return ScriptEngineManager.getEngineByName("JavaScript").eval(expression);
    }
}
```

#### LLM Service Integration
```java
@Service
public class McpLlmServiceAdapter implements LlmServicePort {
    private final McpLlmClient llmClient;
    
    @Override
    public LlmResponse generateWithInternalMonologue(String prompt, Map<String, Object> parameters) {
        String enhancedPrompt = "Take a deep breath, think step by step, and show your work.\n\n" + prompt;
        
        LlmRequest request = LlmRequest.builder()
            .prompt(enhancedPrompt)
            .parameters(parameters)
            .responseFormat(ResponseFormat.INTERNAL_MONOLOGUE)
            .build();
        
        return llmClient.generate(request);
    }
    
    @Override
    public LlmResponse generateWithSelfCritique(String prompt, Map<String, Object> parameters, int iterations) {
        // Initial generation
        LlmRequest initialRequest = LlmRequest.builder()
            .prompt(prompt)
            .parameters(parameters)
            .build();
        
        LlmResponse initialResponse = llmClient.generate(initialRequest);
        
        // Self-critique iterations
        LlmResponse currentResponse = initialResponse;
        List<CritiqueIteration> iterations = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            // Generate critique
            String critiquePrompt = "Please critique the following response, identifying any errors, " +
                "unstated assumptions, or logical fallacies:\n\n" + currentResponse.getText();
            
            LlmRequest critiqueRequest = LlmRequest.builder()
                .prompt(critiquePrompt)
                .parameters(parameters)
                .build();
            
            LlmResponse critiqueResponse = llmClient.generate(critiqueRequest);
            
            // Generate revised response
            String revisionPrompt = "Please provide a revised response addressing the following critique:\n\n" +
                "Original response: " + currentResponse.getText() + "\n\n" +
                "Critique: " + critiqueResponse.getText() + "\n\n" +
                "Revised response:";
            
            LlmRequest revisionRequest = LlmRequest.builder()
                .prompt(revisionPrompt)
                .parameters(parameters)
                .build();
            
            currentResponse = llmClient.generate(revisionRequest);
            
            iterations.add(new CritiqueIteration(critiqueResponse.getText(), currentResponse.getText()));
        }
        
        return LlmResponse.builder()
            .text(currentResponse.getText())
            .iterations(iterations)
            .build();
    }
    
    // Other LLM service methods for different agentic flows
}
```### 
API Layer

#### GraphQL API
```java
@Controller
public class AgenticFlowGraphQLController {
    private final AgenticFlowApplicationService agenticFlowService;
    
    @QueryMapping
    public List<AgenticFlowDto> agenticFlows(@Argument String organizationId) {
        return agenticFlowService.getFlowsByOrganization(organizationId);
    }
    
    @QueryMapping
    public AgenticFlowDto agenticFlow(@Argument String id) {
        return agenticFlowService.getFlowById(id);
    }
    
    @MutationMapping
    public AgenticFlowDto createAgenticFlow(@Argument CreateAgenticFlowInput input) {
        CreateAgenticFlowRequest request = CreateAgenticFlowMapper.fromInput(input);
        return agenticFlowService.createAgenticFlow(request);
    }
    
    @MutationMapping
    public AgenticFlowExecutionDto executeAgenticFlow(@Argument ExecuteAgenticFlowInput input) {
        ExecuteAgenticFlowRequest request = ExecuteAgenticFlowMapper.fromInput(input);
        return agenticFlowService.executeFlow(request);
    }
    
    @SubscriptionMapping
    public Flux<AgenticFlowExecutionEvent> agenticFlowExecutions(@Argument String debateId) {
        return agenticFlowService.subscribeToFlowExecutions(debateId);
    }
}

@Controller
public class DebateAgenticFlowGraphQLController {
    private final DebateApplicationService debateService;
    
    @MutationMapping
    public DebateDto configureDebateAgenticFlows(@Argument ConfigureDebateFlowsInput input) {
        ConfigureDebateFlowsRequest request = ConfigureDebateFlowsMapper.fromInput(input);
        return debateService.configureDebateAgenticFlows(request);
    }
    
    @MutationMapping
    public ParticipantResponseDto processParticipantResponse(@Argument ProcessResponseInput input) {
        ProcessResponseRequest request = ProcessResponseMapper.fromInput(input);
        return debateService.processParticipantResponse(request);
    }
}
```

#### REST API
```java
@RestController
@RequestMapping("/api/v1/agentic-flows")
public class AgenticFlowRestController {
    private final AgenticFlowApplicationService agenticFlowService;
    
    @GetMapping
    public List<AgenticFlowDto> getFlows(@RequestParam String organizationId) {
        return agenticFlowService.getFlowsByOrganization(organizationId);
    }
    
    @GetMapping("/{id}")
    public AgenticFlowDto getFlow(@PathVariable String id) {
        return agenticFlowService.getFlowById(id);
    }
    
    @PostMapping
    public AgenticFlowDto createFlow(@RequestBody CreateAgenticFlowRequest request) {
        return agenticFlowService.createAgenticFlow(request);
    }
    
    @PostMapping("/{id}/execute")
    public AgenticFlowExecutionDto executeFlow(
            @PathVariable String id, 
            @RequestBody ExecuteAgenticFlowRequest request) {
        request.setFlowId(id);
        return agenticFlowService.executeFlow(request);
    }
}

@RestController
@RequestMapping("/api/v1/debates/{debateId}/agentic-flows")
public class DebateAgenticFlowRestController {
    private final DebateApplicationService debateService;
    
    @PostMapping("/configure")
    public DebateDto configureFlows(
            @PathVariable String debateId,
            @RequestBody ConfigureDebateFlowsRequest request) {
        request.setDebateId(debateId);
        return debateService.configureDebateAgenticFlows(request);
    }
    
    @PostMapping("/participants/{participantId}/process")
    public ParticipantResponseDto processResponse(
            @PathVariable String debateId,
            @PathVariable String participantId,
            @RequestBody ProcessResponseRequest request) {
        request.setDebateId(debateId);
        request.setParticipantId(participantId);
        return debateService.processParticipantResponse(request);
    }
}
```#
# Data Models

### Database Schema

#### Agentic Flows Table
```sql
CREATE TABLE agentic_flows (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    organization_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agentic_flows_organization ON agentic_flows(organization_id);
CREATE INDEX idx_agentic_flows_type ON agentic_flows(type);
```

#### Debate Agentic Flow Configuration Table
```sql
CREATE TABLE debate_agentic_flows (
    id VARCHAR(36) PRIMARY KEY,
    debate_id VARCHAR(36) NOT NULL,
    flow_id VARCHAR(36) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (debate_id) REFERENCES debates(id) ON DELETE CASCADE,
    FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE
);

CREATE INDEX idx_debate_agentic_flows_debate ON debate_agentic_flows(debate_id);
```

#### Participant Agentic Flow Configuration Table
```sql
CREATE TABLE participant_agentic_flows (
    id VARCHAR(36) PRIMARY KEY,
    participant_id VARCHAR(36) NOT NULL,
    flow_id VARCHAR(36) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (participant_id) REFERENCES participants(id) ON DELETE CASCADE,
    FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE
);

CREATE INDEX idx_participant_agentic_flows_participant ON participant_agentic_flows(participant_id);
```

#### Agentic Flow Analytics Table
```sql
CREATE TABLE agentic_flow_analytics (
    id VARCHAR(36) PRIMARY KEY,
    flow_id VARCHAR(36) NOT NULL,
    flow_type VARCHAR(50) NOT NULL,
    debate_id VARCHAR(36) NOT NULL,
    participant_id VARCHAR(36) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    processing_time_ms INTEGER NOT NULL,
    confidence_score INTEGER,
    flow_changed_response BOOLEAN NOT NULL,
    metrics JSONB,
    FOREIGN KEY (flow_id) REFERENCES agentic_flows(id) ON DELETE CASCADE,
    FOREIGN KEY (debate_id) REFERENCES debates(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES participants(id) ON DELETE CASCADE
);

CREATE INDEX idx_agentic_flow_analytics_flow ON agentic_flow_analytics(flow_id);
CREATE INDEX idx_agentic_flow_analytics_debate ON agentic_flow_analytics(debate_id);
CREATE INDEX idx_agentic_flow_analytics_timestamp ON agentic_flow_analytics(timestamp);
```

### GraphQL Schema

```graphql
type AgenticFlow {
  id: ID!
  type: AgenticFlowType!
  configuration: JSONObject!
  status: AgenticFlowStatus!
  organizationId: ID!
  createdAt: DateTime!
  updatedAt: DateTime!
}

enum AgenticFlowType {
  INTERNAL_MONOLOGUE
  SELF_CRITIQUE_LOOP
  MULTI_AGENT_RED_TEAM
  TOOL_CALLING_VERIFICATION
  RAG_WITH_RERANKING
  CONFIDENCE_SCORING
  CONSTITUTIONAL_PROMPTING
  ENSEMBLE_VOTING
  POST_PROCESSING_RULES
  TREE_OF_THOUGHTS
  STEP_BACK_PROMPTING
  PROMPT_CHAINING
}

enum AgenticFlowStatus {
  ACTIVE
  INACTIVE
  DRAFT
}

type AgenticFlowExecution {
  id: ID!
  flowId: ID!
  flowType: AgenticFlowType!
  prompt: String!
  response: String!
  processingSteps: [ProcessingStep!]
  confidenceScore: Int
  processingTimeMs: Int!
  timestamp: DateTime!
}

type ProcessingStep {
  stepType: String!
  input: String!
  output: String!
  metadata: JSONObject
}

input CreateAgenticFlowInput {
  type: AgenticFlowType!
  configuration: JSONObject!
  organizationId: ID!
}

input ExecuteAgenticFlowInput {
  flowId: ID!
  prompt: String!
  context: JSONObject
  debateId: ID
  participantId: ID
}

input ConfigureDebateFlowsInput {
  debateId: ID!
  flowConfigurations: [DebateFlowConfigurationInput!]!
}

input DebateFlowConfigurationInput {
  flowId: ID!
  enabled: Boolean!
  configuration: JSONObject
}

input ProcessResponseInput {
  debateId: ID!
  participantId: ID!
  prompt: String!
}

type Query {
  agenticFlows(organizationId: ID!): [AgenticFlow!]!
  agenticFlow(id: ID!): AgenticFlow
  agenticFlowAnalytics(flowId: ID!, timeRange: TimeRangeInput): [AgenticFlowAnalytics!]!
}

type Mutation {
  createAgenticFlow(input: CreateAgenticFlowInput!): AgenticFlow!
  executeAgenticFlow(input: ExecuteAgenticFlowInput!): AgenticFlowExecution!
  configureDebateAgenticFlows(input: ConfigureDebateFlowsInput!): Debate!
  processParticipantResponse(input: ProcessResponseInput!): ParticipantResponse!
}

type Subscription {
  agenticFlowExecutions(debateId: ID!): AgenticFlowExecution!
}
```## Imple
mentation Details for Agentic Flows

### 1. Internal Monologue (Chain-of-Thought Prompting)

#### Implementation Approach
```java
@Service
public class InternalMonologueFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;
    
    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.INTERNAL_MONOLOGUE;
    }
    
    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration config, PromptContext context) {
        // Add chain-of-thought instructions to the prompt
        String enhancedPrompt = buildEnhancedPrompt(prompt, config);
        
        // Generate response with internal monologue
        LlmResponse response = llmService.generateWithInternalMonologue(enhancedPrompt, config.getParameters());
        
        // Extract reasoning and final answer
        String reasoning = extractReasoning(response.getText());
        String finalAnswer = extractFinalAnswer(response.getText());
        
        return AgenticFlowResult.builder()
            .originalPrompt(prompt)
            .enhancedPrompt(enhancedPrompt)
            .fullResponse(response.getText())
            .finalResponse(finalAnswer)
            .reasoning(reasoning)
            .processingTime(response.getProcessingTime())
            .build();
    }
    
    private String buildEnhancedPrompt(String prompt, AgenticFlowConfiguration config) {
        String prefix = config.getParameters().getOrDefault("prefix", 
            "Take a deep breath, think step by step, and show your work.").toString();
        return prefix + "\n\n" + prompt;
    }
    
    private String extractReasoning(String fullResponse) {
        // Extract reasoning part using regex or other text processing
        // This is a simplified example
        Pattern pattern = Pattern.compile("(?s).*?(?=Final Answer:)");
        Matcher matcher = pattern.matcher(fullResponse);
        return matcher.find() ? matcher.group().trim() : fullResponse;
    }
    
    private String extractFinalAnswer(String fullResponse) {
        // Extract final answer part
        Pattern pattern = Pattern.compile("(?s)Final Answer:(.*)");
        Matcher matcher = pattern.matcher(fullResponse);
        return matcher.find() ? matcher.group(1).trim() : fullResponse;
    }
}
```

### 2. Self-Critique Loop

#### Implementation Approach
```java
@Service
public class SelfCritiqueLoopFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;
    
    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.SELF_CRITIQUE_LOOP;
    }
    
    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration config, PromptContext context) {
        // Get configuration parameters
        int iterations = Integer.parseInt(config.getParameters().getOrDefault("iterations", "1").toString());
        
        // Generate initial response
        LlmResponse initialResponse = llmService.generate(prompt, config.getParameters());
        String currentResponse = initialResponse.getText();
        
        List<CritiqueIteration> critiques = new ArrayList<>();
        
        // Perform self-critique iterations
        for (int i = 0; i < iterations; i++) {
            // Generate critique
            String critiquePrompt = "Please critique the following response, identifying potential errors, " +
                "unstated assumptions, or logical fallacies:\n\n" + currentResponse;
            
            LlmResponse critiqueResponse = llmService.generate(critiquePrompt, config.getParameters());
            String critique = critiqueResponse.getText();
            
            // Generate revised response
            String revisionPrompt = "Please provide a revised response addressing the following critique:\n\n" +
                "Original response: " + currentResponse + "\n\n" +
                "Critique: " + critique + "\n\n" +
                "Revised response:";
            
            LlmResponse revisedResponse = llmService.generate(revisionPrompt, config.getParameters());
            String revisedText = revisedResponse.getText();
            
            // Store critique iteration
            critiques.add(new CritiqueIteration(critique, revisedText));
            
            // Update current response for next iteration
            currentResponse = revisedText;
        }
        
        return AgenticFlowResult.builder()
            .originalPrompt(prompt)
            .fullResponse(currentResponse)
            .finalResponse(currentResponse)
            .critiques(critiques)
            .processingTime(Duration.ofMillis(
                initialResponse.getProcessingTime().toMillis() + 
                critiques.stream().mapToLong(c -> c.getProcessingTime().toMillis()).sum()))
            .build();
    }
}
```###
 3. Multi-Agent Red-Team

#### Implementation Approach
```java
@Service
public class MultiAgentRedTeamFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;
    
    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.MULTI_AGENT_RED_TEAM;
    }
    
    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration config, PromptContext context) {
        // Get persona configurations
        Map<String, String> personas = (Map<String, String>) config.getParameters().getOrDefault("personas", 
            Map.of(
                "architect", "You are the Architect. Your role is to propose a solution to the problem.",
                "skeptic", "You are the Skeptic. Your role is to find flaws in the Architect's solution.",
                "judge", "You are the Judge. Your role is to evaluate both perspectives and make a final decision."
            ));
        
        // Generate Architect's proposal
        String architectPrompt = personas.get("architect") + "\n\n" + prompt;
        LlmResponse architectResponse = llmService.generate(architectPrompt, config.getParameters());
        String proposal = architectResponse.getText();
        
        // Generate Skeptic's critique
        String skepticPrompt = personas.get("skeptic") + "\n\nThe Architect has proposed:\n\n" + 
            proposal + "\n\nWhat are the flaws in this proposal?";
        LlmResponse skepticResponse = llmService.generate(skepticPrompt, config.getParameters());
        String critique = skepticResponse.getText();
        
        // Generate Judge's evaluation
        String judgePrompt = personas.get("judge") + "\n\nThe Architect has proposed:\n\n" + 
            proposal + "\n\nThe Skeptic has critiqued:\n\n" + critique + 
            "\n\nPlease evaluate both perspectives and make a final decision.";
        LlmResponse judgeResponse = llmService.generate(judgePrompt, config.getParameters());
        String judgment = judgeResponse.getText();
        
        // Build result with all perspectives
        Map<String, String> perspectives = new HashMap<>();
        perspectives.put("architect", proposal);
        perspectives.put("skeptic", critique);
        perspectives.put("judge", judgment);
        
        return AgenticFlowResult.builder()
            .originalPrompt(prompt)
            .fullResponse(judgment)
            .finalResponse(judgment)
            .perspectives(perspectives)
            .processingTime(Duration.ofMillis(
                architectResponse.getProcessingTime().toMillis() + 
                skepticResponse.getProcessingTime().toMillis() + 
                judgeResponse.getProcessingTime().toMillis()))
            .build();
    }
}
```

### 4. Tool-Calling Verification

#### Implementation Approach
```java
@Service
public class ToolCallingVerificationFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;
    private final Map<String, ExternalToolPort> toolAdapters;
    
    public ToolCallingVerificationFlowService(
            LlmServicePort llmService,
            List<ExternalToolPort> toolAdaptersList) {
        this.llmService = llmService;
        this.toolAdapters = toolAdaptersList.stream()
            .collect(Collectors.toMap(ExternalToolPort::getToolName, Function.identity()));
    }
    
    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.TOOL_CALLING_VERIFICATION;
    }
    
    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration config, PromptContext context) {
        // Get enabled tools
        List<String> enabledTools = (List<String>) config.getParameters().getOrDefault("enabledTools", 
            Arrays.asList("web_search", "calculator"));
        
        // Generate initial response with tool calls
        String toolPrompt = "You can use tools to verify information. " +
            "Available tools: " + String.join(", ", enabledTools) + ".\n\n" +
            "To use a tool, output a JSON object like this: " +
            "{ \"tool\": \"tool_name\", \"query\": \"your query\" }\n\n" + prompt;
        
        LlmResponse initialResponse = llmService.generate(toolPrompt, config.getParameters());
        String responseText = initialResponse.getText();
        
        // Extract tool calls
        List<ToolCall> toolCalls = extractToolCalls(responseText);
        List<ToolExecution> toolExecutions = new ArrayList<>();
        
        // Execute each tool call
        for (ToolCall toolCall : toolCalls) {
            if (enabledTools.contains(toolCall.getTool()) && toolAdapters.containsKey(toolCall.getTool())) {
                ExternalToolPort toolAdapter = toolAdapters.get(toolCall.getTool());
                ToolResponse toolResponse = toolAdapter.executeToolCall(toolCall);
                toolExecutions.add(new ToolExecution(toolCall, toolResponse));
            }
        }
        
        // If tools were used, generate a revised response
        String finalResponse = responseText;
        if (!toolExecutions.isEmpty()) {
            StringBuilder toolResultsBuilder = new StringBuilder();
            toolResultsBuilder.append("Here are the results of your tool calls:\n\n");
            
            for (ToolExecution execution : toolExecutions) {
                toolResultsBuilder.append("Tool: ").append(execution.getToolCall().getTool()).append("\n");
                toolResultsBuilder.append("Query: ").append(execution.getToolCall().getParameters().get("query")).append("\n");
                toolResultsBuilder.append("Result: ").append(execution.getToolResponse().getResult()).append("\n\n");
            }
            
            String revisionPrompt = "Based on your initial response and the following tool results, " +
                "please provide a revised, factually accurate response:\n\n" +
                "Initial response: " + responseText + "\n\n" +
                "Tool results:\n" + toolResultsBuilder.toString() + "\n\n" +
                "Revised response:";
            
            LlmResponse revisedResponse = llmService.generate(revisionPrompt, config.getParameters());
            finalResponse = revisedResponse.getText();
        }
        
        return AgenticFlowResult.builder()
            .originalPrompt(prompt)
            .enhancedPrompt(toolPrompt)
            .fullResponse(finalResponse)
            .finalResponse(finalResponse)
            .toolExecutions(toolExecutions)
            .build();
    }
    
    private List<ToolCall> extractToolCalls(String text) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        // Extract JSON tool calls using regex
        Pattern pattern = Pattern.compile("\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"query\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String tool = matcher.group(1);
            String query = matcher.group(2);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("query", query);
            
            toolCalls.add(new ToolCall(tool, parameters));
        }
        
        return toolCalls;
    }
}
```### 5. Ret
rieval-Augmented Generation (RAG) with Re-ranking

#### Implementation Approach
```java
@Service
public class RagWithRerankingFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;
    private final DocumentRepository documentRepository;
    
    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.RAG_WITH_RERANKING;
    }
    
    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration config, PromptContext context) {
        // Get configuration parameters
        int initialRetrievalCount = Integer.parseInt(
            config.getParameters().getOrDefault("initialRetrievalCount", "20").toString());
        int finalDocumentCount = Integer.parseInt(
            config.getParameters().getOrDefault("finalDocumentCount", "3").toString());
        
        // Step 1: Retrieve initial documents
        List<Document> initialDocuments = documentRepository.search(prompt, initialRetrievalCount);
        
        // Step 2: Re-rank documents
        String rerankPrompt = "I need to select the most relevant documents for answering this question:\n\n" +
            prompt + "\n\n" +
            "Please rank the following documents by relevance (most relevant first) and select the top " + 
            finalDocumentCount + " documents:\n\n";
        
        for (int i = 0; i < initialDocuments.size(); i++) {
            Document doc = initialDocuments.get(i);
            rerankPrompt += "Document " + (i + 1) + ":\n" + doc.getContent() + "\n\n";
        }
        
        rerankPrompt += "List the numbers of the top " + finalDocumentCount + " most relevant documents in order.";
        
        LlmResponse rerankResponse = llmService.generate(rerankPrompt, config.getParameters());
        List<Integer> topDocumentIndices = extractDocumentIndices(rerankResponse.getText(), finalDocumentCount);
        
        // Get the selected documents
        List<Document> selectedDocuments = topDocumentIndices.stream()
            .map(i -> initialDocuments.get(i - 1))
            .collect(Collectors.toList());
        
        // Step 3: Generate answer with selected documents
        StringBuilder contextBuilder = new StringBuilder();
        for (Document doc : selectedDocuments) {
            contextBuilder.append(doc.getContent()).append("\n\n");
        }
        
        String finalPrompt = "Based on the following information, please answer this question:\n\n" +
            "Question: " + prompt + "\n\n" +
            "Information:\n" + contextBuilder.toString() + "\n\n" +
            "Answer:";
        
        LlmResponse finalResponse = llmService.generate(finalPrompt, config.getParameters());
        
        return AgenticFlowResult.builder()
            .originalPrompt(prompt)
            .enhancedPrompt(finalPrompt)
            .fullResponse(finalResponse.getText())
            .finalResponse(finalResponse.getText())
            .retrievedDocuments(initialDocuments)
            .selectedDocuments(selectedDocuments)
            .processingTime(Duration.ofMillis(
                rerankResponse.getProcessingTime().toMillis() + 
                finalResponse.getProcessingTime().toMillis()))
            .build();
    }
    
    private List<Integer> extractDocumentIndices(String text, int count) {
        List<Integer> indices = new ArrayList<>();
        
        // Extract document numbers using regex
        Pattern pattern = Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find() && indices.size() < count) {
            try {
                int index = Integer.parseInt(matcher.group(1));
                if (index > 0) {
                    indices.add(index);
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric matches
            }
        }
        
        return indices;
    }
}
```

## UI Components

### Agentic Flow Configuration UI

```typescript
// React component for configuring agentic flows
import React, { useState, useEffect } from 'react';
import { 
  Box, Typography, Card, CardContent, FormControl, 
  InputLabel, Select, MenuItem, Switch, TextField, Button 
} from '@mui/material';
import { useQuery, useMutation } from '@apollo/client';
import { GET_AGENTIC_FLOWS, CONFIGURE_DEBATE_FLOWS } from '../graphql/queries';

interface AgenticFlowConfigProps {
  debateId: string;
}

export const AgenticFlowConfig: React.FC<AgenticFlowConfigProps> = ({ debateId }) => {
  const [flowConfigurations, setFlowConfigurations] = useState<any[]>([]);
  
  const { loading, error, data } = useQuery(GET_AGENTIC_FLOWS, {
    variables: { organizationId: localStorage.getItem('organizationId') }
  });
  
  const [configureDebateFlows] = useMutation(CONFIGURE_DEBATE_FLOWS);
  
  useEffect(() => {
    if (data?.agenticFlows) {
      // Initialize flow configurations
      setFlowConfigurations(data.agenticFlows.map((flow: any) => ({
        flowId: flow.id,
        enabled: false,
        configuration: flow.configuration
      })));
    }
  }, [data]);
  
  const handleToggleFlow = (flowId: string, enabled: boolean) => {
    setFlowConfigurations(flowConfigurations.map(config => 
      config.flowId === flowId ? { ...config, enabled } : config
    ));
  };
  
  const handleConfigChange = (flowId: string, key: string, value: any) => {
    setFlowConfigurations(flowConfigurations.map(config => 
      config.flowId === flowId ? { 
        ...config, 
        configuration: { 
          ...config.configuration, 
          [key]: value 
        } 
      } : config
    ));
  };
  
  const handleSave = async () => {
    try {
      await configureDebateFlows({
        variables: {
          input: {
            debateId,
            flowConfigurations: flowConfigurations.filter(config => config.enabled)
          }
        }
      });
      
      // Show success message
    } catch (error) {
      // Handle error
      console.error('Error configuring flows:', error);
    }
  };
  
  if (loading) return <p>Loading...</p>;
  if (error) return <p>Error loading agentic flows</p>;
  
  return (
    <Box>
      <Typography variant="h5" gutterBottom>Configure Agentic Flows</Typography>
      
      {data?.agenticFlows.map((flow: any) => (
        <Card key={flow.id} sx={{ mb: 2 }}>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">{formatFlowType(flow.type)}</Typography>
              <Switch 
                checked={flowConfigurations.find(c => c.flowId === flow.id)?.enabled || false}
                onChange={(e) => handleToggleFlow(flow.id, e.target.checked)}
              />
            </Box>
            
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {getFlowDescription(flow.type)}
            </Typography>
            
            {flowConfigurations.find(c => c.flowId === flow.id)?.enabled && (
              <Box mt={2}>
                {renderFlowConfigOptions(flow, flowConfigurations, handleConfigChange)}
              </Box>
            )}
          </CardContent>
        </Card>
      ))}
      
      <Button 
        variant="contained" 
        color="primary" 
        onClick={handleSave}
        disabled={!flowConfigurations.some(c => c.enabled)}
      >
        Save Configuration
      </Button>
    </Box>
  );
};

// Helper functions for UI rendering
const formatFlowType = (type: string): string => {
  return type.split('_').map(word => 
    word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  ).join(' ');
};

const getFlowDescription = (type: string): string => {
  const descriptions: Record<string, string> = {
    INTERNAL_MONOLOGUE: 'Instructs the AI to "think out loud" by writing out reasoning step-by-step before providing the final answer.',
    SELF_CRITIQUE_LOOP: 'AI generates an answer, critiques its own output for flaws, and then revises it based on that critique.',
    MULTI_AGENT_RED_TEAM: 'Simulates a debate between different personas (Architect, Skeptic, Judge) within a single AI to challenge and defend an answer.',
    TOOL_CALLING_VERIFICATION: 'Empowers the AI to use external tools to verify facts and retrieve up-to-date information.',
    RAG_WITH_RERANKING: 'Enhanced RAG that first retrieves many documents, then re-ranks them for relevance before generating an answer.',
    // Add descriptions for other flow types
  };
  
  return descriptions[type] || 'No description available';
};

const renderFlowConfigOptions = (flow: any, configurations: any[], handleChange: Function) => {
  const config = configurations.find(c => c.flowId === flow.id);
  
  switch (flow.type) {
    case 'INTERNAL_MONOLOGUE':
      return (
        <TextField
          fullWidth
          label="Thinking Prompt"
          variant="outlined"
          defaultValue={config?.configuration?.prefix || "Take a deep breath, think step by step, and show your work."}
          onChange={(e) => handleChange(flow.id, 'prefix', e.target.value)}
          margin="normal"
        />
      );
      
    case 'SELF_CRITIQUE_LOOP':
      return (
        <FormControl fullWidth margin="normal">
          <InputLabel>Critique Iterations</InputLabel>
          <Select
            value={config?.configuration?.iterations || 1}
            onChange={(e) => handleChange(flow.id, 'iterations', e.target.value)}
            label="Critique Iterations"
          >
            <MenuItem value={1}>1 iteration</MenuItem>
            <MenuItem value={2}>2 iterations</MenuItem>
            <MenuItem value={3}>3 iterations</MenuItem>
          </Select>
        </FormControl>
      );
      
    // Add configuration options for other flow types
      
    default:
      return <Typography>No configuration options available</Typography>;
  }
};
```### Ag
entic Flow Visualization UI

```typescript
// React component for visualizing agentic flow results
import React, { useState } from 'react';
import { 
  Box, Typography, Paper, Tabs, Tab, Accordion, 
  AccordionSummary, AccordionDetails, Chip, Divider 
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

interface AgenticFlowResultProps {
  result: any;
  flowType: string;
}

export const AgenticFlowResult: React.FC<AgenticFlowResultProps> = ({ result, flowType }) => {
  const [activeTab, setActiveTab] = useState(0);
  
  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };
  
  const renderResultContent = () => {
    switch (flowType) {
      case 'INTERNAL_MONOLOGUE':
        return renderInternalMonologueResult();
      case 'SELF_CRITIQUE_LOOP':
        return renderSelfCritiqueResult();
      case 'MULTI_AGENT_RED_TEAM':
        return renderMultiAgentResult();
      case 'TOOL_CALLING_VERIFICATION':
        return renderToolCallingResult();
      case 'RAG_WITH_RERANKING':
        return renderRagWithRerankingResult();
      default:
        return (
          <Box p={2}>
            <Typography variant="body1">{result.finalResponse}</Typography>
          </Box>
        );
    }
  };
  
  const renderInternalMonologueResult = () => {
    return (
      <Box>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Final Answer" />
          <Tab label="Reasoning Process" />
        </Tabs>
        
        <Box p={2}>
          {activeTab === 0 ? (
            <Typography variant="body1">{result.finalResponse}</Typography>
          ) : (
            <Paper variant="outlined" sx={{ p: 2, backgroundColor: '#f5f5f5' }}>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {result.reasoning}
              </Typography>
            </Paper>
          )}
        </Box>
      </Box>
    );
  };
  
  const renderSelfCritiqueResult = () => {
    return (
      <Box>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Final Answer" />
          <Tab label="Critique Process" />
        </Tabs>
        
        <Box p={2}>
          {activeTab === 0 ? (
            <Typography variant="body1">{result.finalResponse}</Typography>
          ) : (
            <Box>
              {result.critiques.map((critique: any, index: number) => (
                <Accordion key={index}>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography>Iteration {index + 1}</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Typography variant="subtitle2" gutterBottom>Critique:</Typography>
                    <Paper variant="outlined" sx={{ p: 2, mb: 2, backgroundColor: '#fff4e5' }}>
                      <Typography variant="body2">{critique.critique}</Typography>
                    </Paper>
                    
                    <Typography variant="subtitle2" gutterBottom>Revised Response:</Typography>
                    <Paper variant="outlined" sx={{ p: 2, backgroundColor: '#e3f2fd' }}>
                      <Typography variant="body2">{critique.revisedResponse}</Typography>
                    </Paper>
                  </AccordionDetails>
                </Accordion>
              ))}
            </Box>
          )}
        </Box>
      </Box>
    );
  };
  
  const renderMultiAgentResult = () => {
    return (
      <Box>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Final Decision" />
          <Tab label="All Perspectives" />
        </Tabs>
        
        <Box p={2}>
          {activeTab === 0 ? (
            <Typography variant="body1">{result.finalResponse}</Typography>
          ) : (
            <Box>
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography sx={{ display: 'flex', alignItems: 'center' }}>
                    <Chip label="Architect" color="primary" size="small" sx={{ mr: 1 }} />
                    Proposal
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="body2">{result.perspectives.architect}</Typography>
                </AccordionDetails>
              </Accordion>
              
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography sx={{ display: 'flex', alignItems: 'center' }}>
                    <Chip label="Skeptic" color="secondary" size="small" sx={{ mr: 1 }} />
                    Critique
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="body2">{result.perspectives.skeptic}</Typography>
                </AccordionDetails>
              </Accordion>
              
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography sx={{ display: 'flex', alignItems: 'center' }}>
                    <Chip label="Judge" color="success" size="small" sx={{ mr: 1 }} />
                    Final Decision
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="body2">{result.perspectives.judge}</Typography>
                </AccordionDetails>
              </Accordion>
            </Box>
          )}
        </Box>
      </Box>
    );
  };
  
  const renderToolCallingResult = () => {
    return (
      <Box>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Final Answer" />
          <Tab label="Tool Calls" />
        </Tabs>
        
        <Box p={2}>
          {activeTab === 0 ? (
            <Typography variant="body1">{result.finalResponse}</Typography>
          ) : (
            <Box>
              {result.toolExecutions.length > 0 ? (
                result.toolExecutions.map((execution: any, index: number) => (
                  <Paper key={index} variant="outlined" sx={{ p: 2, mb: 2 }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Tool: <Chip label={execution.toolCall.tool} color="primary" size="small" />
                    </Typography>
                    
                    <Typography variant="body2" gutterBottom>
                      <strong>Query:</strong> {execution.toolCall.parameters.query}
                    </Typography>
                    
                    <Divider sx={{ my: 1 }} />
                    
                    <Typography variant="body2">
                      <strong>Result:</strong> {execution.toolResponse.result}
                    </Typography>
                  </Paper>
                ))
              ) : (
                <Typography>No tool calls were made</Typography>
              )}
            </Box>
          )}
        </Box>
      </Box>
    );
  };
  
  const renderRagWithRerankingResult = () => {
    return (
      <Box>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Final Answer" />
          <Tab label="Retrieved Documents" />
        </Tabs>
        
        <Box p={2}>
          {activeTab === 0 ? (
            <Typography variant="body1">{result.finalResponse}</Typography>
          ) : (
            <Box>
              <Typography variant="subtitle1" gutterBottom>
                Selected Documents ({result.selectedDocuments.length})
              </Typography>
              
              {result.selectedDocuments.map((doc: any, index: number) => (
                <Paper key={index} variant="outlined" sx={{ p: 2, mb: 2, backgroundColor: '#e8f5e9' }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Document {index + 1}: {doc.title}
                  </Typography>
                  <Typography variant="body2">{doc.content}</Typography>
                </Paper>
              ))}
              
              <Divider sx={{ my: 2 }} />
              
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography>
                    All Retrieved Documents ({result.retrievedDocuments.length})
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  {result.retrievedDocuments.map((doc: any, index: number) => (
                    <Paper 
                      key={index} 
                      variant="outlined" 
                      sx={{ 
                        p: 2, 
                        mb: 1, 
                        backgroundColor: result.selectedDocuments.some((d: any) => d.id === doc.id) 
                          ? '#e8f5e9' 
                          : '#ffffff'
                      }}
                    >
                      <Typography variant="subtitle2" gutterBottom>
                        Document {index + 1}: {doc.title}
                        {result.selectedDocuments.some((d: any) => d.id === doc.id) && (
                          <Chip label="Selected" color="success" size="small" sx={{ ml: 1 }} />
                        )}
                      </Typography>
                      <Typography variant="body2">{doc.content.substring(0, 200)}...</Typography>
                    </Paper>
                  ))}
                </AccordionDetails>
              </Accordion>
            </Box>
          )}
        </Box>
      </Box>
    );
  };
  
  return (
    <Box sx={{ border: '1px solid #e0e0e0', borderRadius: 1, overflow: 'hidden' }}>
      <Box sx={{ p: 1, backgroundColor: '#f5f5f5', display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="subtitle2">
          {formatFlowType(flowType)}
        </Typography>
        
        {result.confidenceScore !== undefined && (
          <Chip 
            label={`Confidence: ${result.confidenceScore}%`}
            color={getConfidenceColor(result.confidenceScore)}
            size="small"
          />
        )}
      </Box>
      
      {renderResultContent()}
    </Box>
  );
};

// Helper functions
const formatFlowType = (type: string): string => {
  return type.split('_').map(word => 
    word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  ).join(' ');
};

const getConfidenceColor = (score: number): "success" | "warning" | "error" => {
  if (score >= 70) return "success";
  if (score >= 40) return "warning";
  return "error";
};
```#
# Error Handling

### Domain-Level Error Handling
```java
// Domain Exceptions
public class AgenticFlowException extends DomainException {
    public AgenticFlowException(String message, AgenticFlowId flowId) {
        super(message);
        this.flowId = flowId;
    }
}

public class ToolExecutionException extends DomainException {
    public ToolExecutionException(String message, String toolName) {
        super(message);
        this.toolName = toolName;
    }
}

public class UnsupportedFlowTypeException extends DomainException {
    public UnsupportedFlowTypeException(AgenticFlowType flowType) {
        super("Unsupported agentic flow type: " + flowType);
        this.flowType = flowType;
    }
}
```

### Application-Level Error Handling
```java
@ControllerAdvice
public class AgenticFlowExceptionHandler {
    @ExceptionHandler(AgenticFlowException.class)
    public GraphQLError handleAgenticFlowException(AgenticFlowException ex) {
        return GraphQLError.newError()
            .message(ex.getMessage())
            .errorType(ErrorType.ExecutionAborted)
            .build();
    }
    
    @ExceptionHandler(ToolExecutionException.class)
    public GraphQLError handleToolExecutionException(ToolExecutionException ex) {
        return GraphQLError.newError()
            .message("Tool execution failed: " + ex.getMessage())
            .errorType(ErrorType.ExecutionAborted)
            .build();
    }
    
    @ExceptionHandler(UnsupportedFlowTypeException.class)
    public GraphQLError handleUnsupportedFlowTypeException(UnsupportedFlowTypeException ex) {
        return GraphQLError.newError()
            .message(ex.getMessage())
            .errorType(ErrorType.ValidationError)
            .build();
    }
}
```

### Infrastructure-Level Error Handling
- Circuit breaker pattern for LLM service calls
- Retry logic for external tool calls with exponential backoff
- Fallback mechanisms for failed agentic flows
- Graceful degradation for unavailable services

## Testing Strategy

### Domain Testing
```java
@ExtendWith(MockitoExtension.class)
class InternalMonologueFlowServiceTest {
    @Mock
    private LlmServicePort llmService;
    
    @InjectMocks
    private InternalMonologueFlowService flowService;
    
    @Test
    void shouldProcessPromptWithInternalMonologue() {
        // Given
        String prompt = "What is the capital of France?";
        AgenticFlowConfiguration config = new AgenticFlowConfiguration(Map.of(
            "prefix", "Take a deep breath, think step by step, and show your work."
        ));
        PromptContext context = new PromptContext();
        
        LlmResponse mockResponse = LlmResponse.builder()
            .text("Let me think step by step.\n1. France is a country in Europe.\n2. The capital of France is Paris.\n\nFinal Answer: Paris")
            .processingTime(Duration.ofMillis(500))
            .build();
        
        when(llmService.generateWithInternalMonologue(anyString(), anyMap())).thenReturn(mockResponse);
        
        // When
        AgenticFlowResult result = flowService.process(prompt, config, context);
        
        // Then
        assertNotNull(result);
        assertEquals("Paris", result.getFinalResponse());
        assertEquals("Let me think step by step.\n1. France is a country in Europe.\n2. The capital of France is Paris.", result.getReasoning());
        verify(llmService).generateWithInternalMonologue(anyString(), anyMap());
    }
}
```

### Application Testing
```java
@SpringBootTest
class AgenticFlowApplicationServiceIntegrationTest {
    @MockBean
    private LlmServicePort llmService;
    
    @MockBean
    private AgenticFlowRepository agenticFlowRepository;
    
    @Autowired
    private AgenticFlowApplicationService agenticFlowService;
    
    @Test
    void shouldExecuteAgenticFlow() {
        // Given
        AgenticFlowId flowId = new AgenticFlowId("test-flow-id");
        AgenticFlow flow = new AgenticFlow(
            flowId,
            AgenticFlowType.INTERNAL_MONOLOGUE,
            new AgenticFlowConfiguration(Map.of("prefix", "Think step by step")),
            AgenticFlowStatus.ACTIVE,
            new OrganizationId("test-org-id")
        );
        
        ExecuteAgenticFlowRequest request = new ExecuteAgenticFlowRequest();
        request.setFlowId(flowId.getValue());
        request.setPrompt("What is the capital of France?");
        
        LlmResponse mockResponse = LlmResponse.builder()
            .text("Let me think step by step.\n1. France is a country in Europe.\n2. The capital of France is Paris.\n\nFinal Answer: Paris")
            .processingTime(Duration.ofMillis(500))
            .build();
        
        when(agenticFlowRepository.findById(flowId)).thenReturn(Optional.of(flow));
        when(llmService.generateWithInternalMonologue(anyString(), anyMap())).thenReturn(mockResponse);
        
        // When
        AgenticFlowExecutionResponse response = agenticFlowService.executeFlow(request);
        
        // Then
        assertNotNull(response);
        assertEquals("Paris", response.getFinalResponse());
        assertEquals(AgenticFlowType.INTERNAL_MONOLOGUE, response.getFlowType());
    }
}
```

### UI Testing
```typescript
// React component testing
describe('AgenticFlowConfig', () => {
  test('should render agentic flow configuration options', () => {
    const mockFlows = [
      {
        id: 'flow1',
        type: 'INTERNAL_MONOLOGUE',
        configuration: { prefix: 'Think step by step' },
        status: 'ACTIVE',
        organizationId: 'org1'
      },
      {
        id: 'flow2',
        type: 'SELF_CRITIQUE_LOOP',
        configuration: { iterations: 2 },
        status: 'ACTIVE',
        organizationId: 'org1'
      }
    ];
    
    const mocks = [
      {
        request: {
          query: GET_AGENTIC_FLOWS,
          variables: { organizationId: 'org1' }
        },
        result: {
          data: {
            agenticFlows: mockFlows
          }
        }
      }
    ];
    
    render(
      <MockedProvider mocks={mocks} addTypename={false}>
        <AgenticFlowConfig debateId="debate1" />
      </MockedProvider>
    );
    
    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Internal Monologue')).toBeInTheDocument();
      expect(screen.getByText('Self Critique Loop')).toBeInTheDocument();
    });
    
    // Toggle a flow on
    fireEvent.click(screen.getAllByRole('checkbox')[0]);
    
    // Check that configuration options appear
    expect(screen.getByLabelText('Thinking Prompt')).toBeInTheDocument();
  });
});
```#
# Performance Considerations

### Optimization Strategies
- Asynchronous processing for resource-intensive flows (e.g., Ensemble Voting)
- Caching of common flow configurations and results
- Parallel execution of independent flow steps
- Streaming responses for long-running flows
- Optimized database queries with proper indexing
- Connection pooling for external service calls

### Scalability Approach
- Horizontal scaling of agentic flow processing services
- Load balancing across multiple LLM service instances
- Distributed caching for shared flow results
- Queue-based processing for high-volume scenarios
- Resource limits and throttling for fair usage

## Security Integration

### Authentication and Authorization
```java
@Configuration
@EnableWebSecurity
public class AgenticFlowSecurityConfig {
    @Bean
    public SecurityFilterChain agenticFlowFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/v1/agentic-flows/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/agentic-flows").hasRole("AGENTIC_FLOW_ADMIN")
                .requestMatchers("/api/v1/agentic-flows/*/execute").hasRole("AGENTIC_FLOW_USER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

### Data Protection
- Organization-level isolation for agentic flow configurations
- Encryption of sensitive configuration parameters
- Audit logging of all agentic flow operations
- Sanitization of tool call inputs and outputs
- Rate limiting for API endpoints

## Integration with Existing System

### Integration with mcp-llm Service
```java
@Service
public class McpLlmClientAdapter implements LlmServicePort {
    private final McpLlmClient llmClient;
    
    @Override
    public LlmResponse generate(String prompt, Map<String, Object> parameters) {
        // Convert to mcp-llm request format
        McpLlmRequest mcpRequest = McpLlmRequest.builder()
            .prompt(prompt)
            .model(parameters.getOrDefault("model", "claude-3-opus-20240229").toString())
            .temperature(Double.parseDouble(parameters.getOrDefault("temperature", "0.7").toString()))
            .maxTokens(Integer.parseInt(parameters.getOrDefault("maxTokens", "2000").toString()))
            .build();
        
        // Call mcp-llm service
        McpLlmResponse mcpResponse = llmClient.generateCompletion(mcpRequest);
        
        // Convert to domain model
        return LlmResponse.builder()
            .text(mcpResponse.getText())
            .processingTime(Duration.ofMillis(mcpResponse.getProcessingTimeMs()))
            .build();
    }
    
    // Implement other methods for specific agentic flows
}
```

### Integration with Debate UI
```typescript
// Integration with existing debate components
import React from 'react';
import { useQuery } from '@apollo/client';
import { GET_DEBATE_PARTICIPANT_FLOWS } from '../graphql/queries';
import { AgenticFlowResult } from './AgenticFlowResult';

interface ParticipantResponseProps {
  debateId: string;
  participantId: string;
  response: any;
}

export const EnhancedParticipantResponse: React.FC<ParticipantResponseProps> = ({ 
  debateId, 
  participantId, 
  response 
}) => {
  const { loading, error, data } = useQuery(GET_DEBATE_PARTICIPANT_FLOWS, {
    variables: { debateId, participantId }
  });
  
  if (loading) return <p>Loading...</p>;
  if (error) return <p>Error loading agentic flow data</p>;
  
  const hasAgenticFlowResult = response.agenticFlowResult && response.agenticFlowType;
  
  return (
    <div className="participant-response">
      {hasAgenticFlowResult ? (
        <AgenticFlowResult 
          result={response.agenticFlowResult} 
          flowType={response.agenticFlowType} 
        />
      ) : (
        <div className="standard-response">
          {response.text}
        </div>
      )}
    </div>
  );
};
```

## Analytics and Reporting

### Analytics Dashboard
```typescript
// Analytics dashboard component
import React, { useState } from 'react';
import { useQuery } from '@apollo/client';
import { 
  Box, Typography, Grid, Card, CardContent, 
  FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableHead, TableRow,
  Paper, CircularProgress
} from '@mui/material';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  PieChart, Pie, Cell
} from 'recharts';
import { GET_AGENTIC_FLOW_ANALYTICS } from '../graphql/queries';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D'];

export const AgenticFlowAnalytics: React.FC = () => {
  const [timeRange, setTimeRange] = useState('last7days');
  const [organizationId, setOrganizationId] = useState(localStorage.getItem('organizationId'));
  
  const { loading, error, data } = useQuery(GET_AGENTIC_FLOW_ANALYTICS, {
    variables: { 
      organizationId,
      timeRange
    }
  });
  
  if (loading) return <CircularProgress />;
  if (error) return <Typography color="error">Error loading analytics data</Typography>;
  
  const { 
    flowUsageStats,
    confidenceScoreDistribution,
    processingTimeStats,
    topPerformingFlows,
    flowChangeRateStats
  } = data.agenticFlowAnalytics;
  
  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Agentic Flow Analytics</Typography>
        
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel>Time Range</InputLabel>
          <Select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            label="Time Range"
          >
            <MenuItem value="today">Today</MenuItem>
            <MenuItem value="last7days">Last 7 Days</MenuItem>
            <MenuItem value="last30days">Last 30 Days</MenuItem>
            <MenuItem value="last90days">Last 90 Days</MenuItem>
          </Select>
        </FormControl>
      </Box>
      
      <Grid container spacing={3}>
        {/* Flow Usage Chart */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Flow Usage</Typography>
            <BarChart width={500} height={300} data={flowUsageStats}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="flowType" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="count" fill="#8884d8" />
            </BarChart>
          </Paper>
        </Grid>
        
        {/* Confidence Score Distribution */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Confidence Score Distribution</Typography>
            <PieChart width={500} height={300}>
              <Pie
                data={confidenceScoreDistribution}
                cx={250}
                cy={150}
                labelLine={false}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
              >
                {confidenceScoreDistribution.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </Paper>
        </Grid>
        
        {/* Top Performing Flows */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Top Performing Flows</Typography>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Flow Type</TableCell>
                  <TableCell>Avg. Confidence Score</TableCell>
                  <TableCell>Avg. Processing Time (ms)</TableCell>
                  <TableCell>Response Change Rate</TableCell>
                  <TableCell>Usage Count</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {topPerformingFlows.map((flow: any) => (
                  <TableRow key={flow.flowType}>
                    <TableCell>{formatFlowType(flow.flowType)}</TableCell>
                    <TableCell>{flow.avgConfidenceScore}</TableCell>
                    <TableCell>{flow.avgProcessingTimeMs}</TableCell>
                    <TableCell>{(flow.responseChangeRate * 100).toFixed(1)}%</TableCell>
                    <TableCell>{flow.usageCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

// Helper function
const formatFlowType = (type: string): string => {
  return type.split('_').map(word => 
    word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  ).join(' ');
};
```

This design provides a comprehensive foundation for implementing advanced agentic flows in the Zamaz Debate MCP Services, following hexagonal architecture principles and integrating seamlessly with the existing system.