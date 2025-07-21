# Agentic Flows API Documentation

## Overview

The Agentic Flows API provides advanced reasoning capabilities for AI participants in debates. It supports 12 different flow types, each designed to enhance AI responses through structured reasoning patterns.

## Authentication

All API endpoints require JWT authentication. Include the bearer token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Base URL

```
https://api.zamaz-debate.com/api/v1
```

## Flow Types

| Flow Type | Description | Use Case |
|-----------|-------------|----------|
| `INTERNAL_MONOLOGUE` | Chain-of-thought reasoning | Complex problem solving |
| `SELF_CRITIQUE_LOOP` | Generate-Critique-Revise pattern | Response refinement |
| `MULTI_AGENT_RED_TEAM` | Multiple perspectives evaluation | Comprehensive analysis |
| `TOOL_CALLING_VERIFICATION` | External tool integration | Fact verification |
| `RAG_WITH_RERANKING` | Document retrieval with LLM ranking | Knowledge-based responses |
| `CONFIDENCE_SCORING` | Response confidence assessment | Quality control |
| `CONSTITUTIONAL_PROMPTING` | Principle-based constraints | Ethical alignment |
| `ENSEMBLE_VOTING` | Multiple response generation | Consistency improvement |
| `POST_PROCESSING_RULES` | Deterministic validation | Format compliance |
| `TREE_OF_THOUGHTS` | Multi-path exploration | Strategic planning |
| `STEP_BACK_PROMPTING` | Abstract reasoning | Generalization |
| `PROMPT_CHAINING` | Sequential prompt processing | Complex workflows |

## Endpoints

### 1. Create Agentic Flow

Create a new agentic flow configuration for a debate.

**Endpoint:** `POST /debates/{debateId}/agentic-flows`

**Request Body:**
```json
{
  "name": "Deep Reasoning Flow",
  "flowType": "TREE_OF_THOUGHTS",
  "description": "Advanced reasoning for complex ethical questions",
  "configuration": {
    "maxDepth": 3,
    "branchingFactor": 3,
    "evaluationCriteria": "logical_coherence"
  },
  "participantIds": ["participant-1", "participant-2"]
}
```

**Response:**
```json
{
  "id": "flow-123",
  "debateId": "debate-456",
  "name": "Deep Reasoning Flow",
  "flowType": "TREE_OF_THOUGHTS",
  "description": "Advanced reasoning for complex ethical questions",
  "configuration": {
    "maxDepth": 3,
    "branchingFactor": 3,
    "evaluationCriteria": "logical_coherence"
  },
  "participantIds": ["participant-1", "participant-2"],
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### 2. List Agentic Flows

Get all agentic flows for a debate.

**Endpoint:** `GET /debates/{debateId}/agentic-flows`

**Query Parameters:**
- `status` (optional): Filter by status (ACTIVE, INACTIVE)
- `flowType` (optional): Filter by flow type

**Response:**
```json
{
  "flows": [
    {
      "id": "flow-123",
      "name": "Deep Reasoning Flow",
      "flowType": "TREE_OF_THOUGHTS",
      "status": "ACTIVE",
      "participantIds": ["participant-1"]
    },
    {
      "id": "flow-124",
      "name": "Fact Checker",
      "flowType": "TOOL_CALLING_VERIFICATION",
      "status": "ACTIVE",
      "participantIds": ["participant-2"]
    }
  ],
  "total": 2
}
```

### 3. Update Agentic Flow

Update an existing flow configuration.

**Endpoint:** `PUT /agentic-flows/{flowId}`

**Request Body:**
```json
{
  "configuration": {
    "maxDepth": 4,
    "branchingFactor": 4,
    "evaluationCriteria": "comprehensive_analysis"
  },
  "participantIds": ["participant-1", "participant-2", "participant-3"]
}
```

### 4. Execute Agentic Flow

Execute a flow for a specific prompt.

**Endpoint:** `POST /agentic-flows/{flowId}/execute`

**Request Body:**
```json
{
  "prompt": "What are the ethical implications of artificial general intelligence?",
  "context": {
    "debateId": "debate-456",
    "participantId": "participant-1",
    "round": 3
  }
}
```

**Response:**
```json
{
  "executionId": "exec-789",
  "flowId": "flow-123",
  "flowType": "TREE_OF_THOUGHTS",
  "status": "SUCCESS",
  "finalAnswer": "The ethical implications of AGI are multifaceted...",
  "thoughtTree": {
    "root": "AGI Ethics",
    "branches": [
      {
        "thought": "Existential Risk",
        "score": 0.9,
        "children": [
          {
            "thought": "Control Problem",
            "score": 0.85
          },
          {
            "thought": "Alignment Challenge",
            "score": 0.88
          }
        ]
      }
    ]
  },
  "confidence": 88.5,
  "executionTime": 4250,
  "timestamp": "2024-01-15T10:35:00Z"
}
```

### 5. Get Flow Execution Results

Retrieve results for all flow executions in a debate.

**Endpoint:** `GET /debates/{debateId}/flow-results`

**Response:**
```json
{
  "results": {
    "message-1": {
      "flowId": "flow-123",
      "flowType": "INTERNAL_MONOLOGUE",
      "finalAnswer": "Based on my analysis...",
      "reasoning": "Step 1: Consider the problem\nStep 2: Apply logic",
      "confidence": 92.0,
      "executionTime": 1800
    },
    "message-2": {
      "flowId": "flow-124",
      "flowType": "TOOL_CALLING_VERIFICATION",
      "finalAnswer": "The verified facts show...",
      "toolCalls": [
        {
          "tool": "web_search",
          "input": "climate change statistics 2024",
          "output": "Latest IPCC report shows..."
        }
      ],
      "confidence": 95.0,
      "executionTime": 2500
    }
  }
}
```

### 6. Get Flow Analytics

Retrieve analytics for flow executions.

**Endpoint:** `GET /analytics/agentic-flows`

**Query Parameters:**
- `organizationId`: Organization ID
- `startDate`: Start date (ISO 8601)
- `endDate`: End date (ISO 8601)
- `flowType` (optional): Filter by flow type

**Response:**
```json
{
  "summary": {
    "totalExecutions": 1250,
    "averageConfidence": 86.5,
    "averageExecutionTime": 2850,
    "successRate": 0.94
  },
  "byFlowType": [
    {
      "flowType": "INTERNAL_MONOLOGUE",
      "executionCount": 350,
      "averageConfidence": 88.0,
      "averageExecutionTime": 1500,
      "successRate": 0.96
    }
  ],
  "confidenceTrends": [
    {
      "date": "2024-01-01",
      "averageConfidence": 84.0
    },
    {
      "date": "2024-01-02",
      "averageConfidence": 85.5
    }
  ]
}
```

### 7. Get Flow Recommendations

Get AI-powered flow recommendations for a debate.

**Endpoint:** `POST /recommendations/agentic-flows`

**Request Body:**
```json
{
  "debateContext": {
    "topic": "AI Safety and Ethics",
    "format": "OXFORD",
    "topicCategories": ["technology", "ethics", "philosophy"],
    "requiresFactChecking": true,
    "isHighStakes": true
  },
  "participantContext": {
    "modelProvider": "OpenAI",
    "modelName": "gpt-4-turbo",
    "role": "EXPERT"
  }
}
```

**Response:**
```json
{
  "recommendations": [
    {
      "flowType": "MULTI_AGENT_RED_TEAM",
      "score": 0.95,
      "reasons": [
        "High-stakes ethical debate benefits from multiple perspectives",
        "Complex topic requires comprehensive analysis"
      ],
      "expectedBenefits": [
        "More balanced arguments",
        "Better identification of edge cases"
      ]
    },
    {
      "flowType": "TOOL_CALLING_VERIFICATION",
      "score": 0.88,
      "reasons": [
        "Topic requires fact-checking",
        "Technical claims need verification"
      ]
    }
  ],
  "reasoning": "For a high-stakes debate on AI safety, multi-perspective analysis and fact verification are crucial."
}
```

## GraphQL API

The Agentic Flows API is also available via GraphQL.

**Endpoint:** `POST /graphql`

### Query Examples

**Get Flow with Execution History:**
```graphql
query GetFlowWithHistory($flowId: ID!) {
  agenticFlow(id: $flowId) {
    id
    name
    flowType
    configuration
    executions(limit: 10) {
      id
      status
      confidence
      executionTime
      timestamp
    }
  }
}
```

**Get Debate Flows with Analytics:**
```graphql
query GetDebateFlows($debateId: ID!) {
  debate(id: $debateId) {
    id
    topic
    agenticFlows {
      id
      name
      flowType
      analytics {
        totalExecutions
        averageConfidence
        successRate
      }
    }
  }
}
```

### Mutation Examples

**Create Flow with Configuration:**
```graphql
mutation CreateFlow($input: CreateAgenticFlowInput!) {
  createAgenticFlow(input: $input) {
    id
    name
    flowType
    configuration
    status
  }
}
```

### Subscription Examples

**Subscribe to Flow Execution Updates:**
```graphql
subscription FlowExecutionUpdates($debateId: ID!) {
  flowExecutionUpdated(debateId: $debateId) {
    executionId
    flowId
    status
    progress
    currentStep
  }
}
```

## Configuration Examples

### Internal Monologue
```json
{
  "prefix": "Let me think through this step by step:",
  "showReasoning": true,
  "temperature": 0.7
}
```

### Self-Critique Loop
```json
{
  "maxIterations": 3,
  "critiquePrompt": "Identify weaknesses in the following response:",
  "revisionPrompt": "Improve the response addressing these critiques:",
  "improvementThreshold": 0.2
}
```

### Multi-Agent Red-Team
```json
{
  "agents": {
    "architect": {
      "prompt": "Evaluate from a design perspective:",
      "weight": 0.33
    },
    "skeptic": {
      "prompt": "Find potential flaws and edge cases:",
      "weight": 0.33
    },
    "judge": {
      "prompt": "Provide balanced final assessment:",
      "weight": 0.34
    }
  }
}
```

### Tool-Calling Verification
```json
{
  "allowedTools": ["web_search", "calculator", "code_executor"],
  "verificationPrompt": "Verify the following claims using available tools:",
  "maxToolCalls": 5,
  "timeout": 30000
}
```

### RAG with Re-ranking
```json
{
  "initialRetrievalCount": 10,
  "rerankingPrompt": "Rank these documents by relevance to the query:",
  "finalDocumentCount": 3,
  "includeConfidenceScores": true
}
```

## Error Responses

### 400 Bad Request
```json
{
  "error": "INVALID_FLOW_TYPE",
  "message": "Flow type 'INVALID_TYPE' is not supported",
  "supportedTypes": ["INTERNAL_MONOLOGUE", "SELF_CRITIQUE_LOOP", ...]
}
```

### 401 Unauthorized
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired JWT token"
}
```

### 403 Forbidden
```json
{
  "error": "INSUFFICIENT_PERMISSIONS",
  "message": "User does not have permission to modify this flow"
}
```

### 404 Not Found
```json
{
  "error": "FLOW_NOT_FOUND",
  "message": "Agentic flow with ID 'flow-123' not found"
}
```

### 429 Too Many Requests
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please try again in 60 seconds",
  "retryAfter": 60
}
```

### 500 Internal Server Error
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "requestId": "req-12345"
}
```

## Rate Limiting

API rate limits vary by endpoint and organization tier:

| Endpoint | Free Tier | Pro Tier | Enterprise |
|----------|-----------|----------|------------|
| Create Flow | 10/hour | 100/hour | Unlimited |
| Execute Flow | 100/hour | 1000/hour | Unlimited |
| Get Analytics | 50/hour | 500/hour | Unlimited |

Rate limit headers:
- `X-RateLimit-Limit`: Maximum requests per window
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Unix timestamp when limit resets

## Webhooks

Configure webhooks to receive real-time updates:

### Flow Execution Completed
```json
{
  "event": "flow.execution.completed",
  "timestamp": "2024-01-15T10:35:00Z",
  "data": {
    "executionId": "exec-789",
    "flowId": "flow-123",
    "status": "SUCCESS",
    "confidence": 88.5,
    "executionTime": 4250
  }
}
```

### Flow Configuration Updated
```json
{
  "event": "flow.configuration.updated",
  "timestamp": "2024-01-15T10:40:00Z",
  "data": {
    "flowId": "flow-123",
    "changes": {
      "configuration.maxDepth": {
        "old": 3,
        "new": 4
      }
    }
  }
}
```

## SDK Examples

### JavaScript/TypeScript
```typescript
import { AgenticFlowsClient } from '@zamaz/agentic-flows-sdk';

const client = new AgenticFlowsClient({
  apiKey: process.env.ZAMAZ_API_KEY,
  baseUrl: 'https://api.zamaz-debate.com'
});

// Create a flow
const flow = await client.createFlow({
  debateId: 'debate-456',
  name: 'Advanced Reasoning',
  flowType: 'TREE_OF_THOUGHTS',
  configuration: {
    maxDepth: 3,
    branchingFactor: 3
  }
});

// Execute the flow
const result = await client.executeFlow(flow.id, {
  prompt: 'Analyze the implications of quantum computing',
  context: { round: 1 }
});

console.log(`Confidence: ${result.confidence}%`);
console.log(`Answer: ${result.finalAnswer}`);
```

### Python
```python
from zamaz_agentic_flows import AgenticFlowsClient

client = AgenticFlowsClient(
    api_key=os.environ['ZAMAZ_API_KEY'],
    base_url='https://api.zamaz-debate.com'
)

# Create a flow
flow = client.create_flow(
    debate_id='debate-456',
    name='Fact Verification',
    flow_type='TOOL_CALLING_VERIFICATION',
    configuration={
        'allowed_tools': ['web_search', 'calculator'],
        'max_tool_calls': 5
    }
)

# Execute the flow
result = client.execute_flow(
    flow_id=flow.id,
    prompt='Verify the current global temperature rise',
    context={'round': 2}
)

print(f"Confidence: {result.confidence}%")
for tool_call in result.tool_calls:
    print(f"Tool: {tool_call.tool} - Output: {tool_call.output}")
```

### Java
```java
import com.zamaz.agentic.AgenticFlowsClient;
import com.zamaz.agentic.models.*;

AgenticFlowsClient client = new AgenticFlowsClient.Builder()
    .apiKey(System.getenv("ZAMAZ_API_KEY"))
    .baseUrl("https://api.zamaz-debate.com")
    .build();

// Create a flow
AgenticFlow flow = client.createFlow(
    CreateFlowRequest.builder()
        .debateId("debate-456")
        .name("Self-Improvement Loop")
        .flowType(FlowType.SELF_CRITIQUE_LOOP)
        .configuration(Map.of(
            "maxIterations", 3,
            "improvementThreshold", 0.2
        ))
        .build()
);

// Execute the flow
FlowResult result = client.executeFlow(
    flow.getId(),
    ExecuteFlowRequest.builder()
        .prompt("Explain the theory of relativity")
        .context(Map.of("round", 3))
        .build()
);

System.out.printf("Final answer after %d iterations: %s%n", 
    result.getIterations().size(), 
    result.getFinalAnswer()
);
```

## Best Practices

1. **Flow Selection**
   - Use `INTERNAL_MONOLOGUE` for general reasoning tasks
   - Apply `TOOL_CALLING_VERIFICATION` when facts need verification
   - Choose `MULTI_AGENT_RED_TEAM` for comprehensive analysis
   - Implement `CONFIDENCE_SCORING` for quality control

2. **Configuration Optimization**
   - Keep iteration counts reasonable (2-3 for most flows)
   - Set appropriate timeouts for tool-calling flows
   - Use temperature settings that match the task

3. **Performance Considerations**
   - Cache flow configurations to reduce API calls
   - Use webhook subscriptions for real-time updates
   - Batch analytics queries when possible

4. **Error Handling**
   - Implement exponential backoff for rate limits
   - Have fallback flows for critical operations
   - Log execution IDs for debugging

5. **Security**
   - Rotate API keys regularly
   - Use organization-level permissions
   - Validate tool inputs in tool-calling flows