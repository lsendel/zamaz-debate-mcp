# Agentic Flows User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Flow Types Overview](#flow-types-overview)
4. [Configuring Flows](#configuring-flows)
5. [Best Practices](#best-practices)
6. [Troubleshooting](#troubleshooting)
7. [FAQ](#faq)

## Introduction

Agentic Flows enhance AI participants in debates by providing structured reasoning patterns. Each flow type implements a specific cognitive strategy to improve response quality, accuracy, and reliability.

### Why Use Agentic Flows?

- **Improved Reasoning**: Structure AI thinking for better outcomes
- **Transparency**: See how AI arrives at conclusions
- **Quality Control**: Built-in verification and confidence scoring
- **Customization**: Tailor flows to specific debate topics
- **Analytics**: Track performance and optimize over time

## Getting Started

### Step 1: Access Flow Configuration

1. Navigate to your debate
2. Click on "Configure Flows" button
3. The Agentic Flow Configuration panel will open

![Flow Configuration Panel](./images/flow-config-panel.png)

### Step 2: Create Your First Flow

1. Click "Create Flow"
2. Enter a descriptive name (e.g., "Deep Analysis for Ethics")
3. Select a flow type based on your needs
4. Configure flow-specific parameters
5. Assign to participants
6. Click "Save"

### Step 3: Monitor Flow Execution

During the debate, you'll see:
- Confidence scores for each response
- Execution time indicators
- "View Details" buttons to explore reasoning

## Flow Types Overview

### 1. Internal Monologue 🧠
**Best for**: General reasoning and problem-solving

Shows the AI's step-by-step thinking process, making reasoning transparent.

**Example Configuration**:
```json
{
  "prefix": "Let me think through this carefully:",
  "showReasoning": true
}
```

**When to use**:
- Complex analytical questions
- Multi-step problem solving
- When transparency is important

### 2. Self-Critique Loop 🔄
**Best for**: Refining and improving responses

The AI generates a response, critiques it, and iteratively improves it.

**Example Configuration**:
```json
{
  "maxIterations": 3,
  "improvementThreshold": 0.2
}
```

**When to use**:
- High-stakes responses
- When quality matters more than speed
- For nuanced or sensitive topics

### 3. Multi-Agent Red-Team 👥
**Best for**: Comprehensive analysis from multiple viewpoints

Three AI personas debate internally before reaching a conclusion:
- **Architect**: Constructive viewpoint
- **Skeptic**: Critical analysis
- **Judge**: Balanced final assessment

**When to use**:
- Controversial topics
- Strategic decisions
- When balanced perspective is crucial

### 4. Tool-Calling Verification 🔧
**Best for**: Fact-checking and verification

Allows AI to use external tools like web search and calculators.

**Example Configuration**:
```json
{
  "allowedTools": ["web_search", "calculator"],
  "maxToolCalls": 5
}
```

**When to use**:
- Factual debates
- Scientific discussions
- Current events topics

### 5. RAG with Re-ranking 📚
**Best for**: Knowledge-intensive responses

Retrieves relevant documents and uses AI to re-rank them by relevance.

**Example Configuration**:
```json
{
  "initialRetrievalCount": 10,
  "finalDocumentCount": 3
}
```

**When to use**:
- Research-based debates
- Historical discussions
- Technical topics

### 6. Confidence Scoring 📊
**Best for**: Quality assurance

Evaluates response confidence and triggers improvement if below threshold.

**Example Configuration**:
```json
{
  "confidenceThreshold": 0.8,
  "improvementStrategy": "regenerate"
}
```

**When to use**:
- Critical debates
- When accuracy is paramount
- Quality control

### 7. Constitutional Prompting 📜
**Best for**: Ensuring ethical alignment

Enforces principles and guidelines in responses.

**Example Configuration**:
```json
{
  "principles": [
    "Be truthful and accurate",
    "Avoid harmful content",
    "Respect all viewpoints"
  ]
}
```

**When to use**:
- Ethical debates
- Sensitive topics
- Corporate compliance needs

### 8. Ensemble Voting 🗳️
**Best for**: Consistency and reliability

Generates multiple responses and selects the best through voting.

**Example Configuration**:
```json
{
  "sampleCount": 5,
  "temperatureVariation": 0.2
}
```

**When to use**:
- When consistency matters
- Reducing randomness
- Important conclusions

### 9. Tree of Thoughts 🌳
**Best for**: Strategic planning and exploration

Explores multiple reasoning paths before selecting the best.

**Example Configuration**:
```json
{
  "maxDepth": 3,
  "branchingFactor": 3,
  "evaluationMetric": "coherence"
}
```

**When to use**:
- Strategic debates
- Complex planning
- Multi-option scenarios

## Configuring Flows

### Basic Configuration

1. **Flow Name**: Choose descriptive names
   - ✅ "Fact Verification for Climate Debate"
   - ❌ "Flow 1"

2. **Description**: Explain the purpose
   - Helps team members understand usage
   - Documents your reasoning

3. **Status**: Active/Inactive
   - Toggle flows on/off without deletion
   - Test different configurations

### Advanced Configuration

#### Parameter Tuning

Each flow type has specific parameters:

**Internal Monologue**:
- `prefix`: Reasoning prompt prefix
- `temperature`: Creativity level (0.0-1.0)

**Self-Critique Loop**:
- `maxIterations`: Number of refinement cycles (1-5)
- `critiquePrompt`: Custom critique instructions
- `improvementThreshold`: Minimum improvement required

**Multi-Agent Red-Team**:
- `agentPrompts`: Customize each agent's perspective
- `weights`: Adjust influence of each agent

#### Participant Assignment

1. **Global Assignment**: Applies to all AI participants
2. **Specific Assignment**: Target individual participants
3. **Role-Based**: Assign based on participant roles

### Flow Templates

Save time with pre-configured templates:

1. **Fact-Checking Debate**
   - Tool-Calling Verification
   - RAG with Re-ranking
   - Confidence Scoring

2. **Philosophical Discussion**
   - Internal Monologue
   - Multi-Agent Red-Team
   - Step-Back Prompting

3. **Technical Analysis**
   - Tree of Thoughts
   - Tool-Calling Verification
   - Post-Processing Rules

## Best Practices

### 1. Flow Selection Strategy

**Match Flow to Topic**:
- Factual topics → Tool-Calling, RAG
- Ethical topics → Constitutional, Multi-Agent
- Complex analysis → Tree of Thoughts, Internal Monologue

**Consider Debate Format**:
- Oxford style → Self-Critique for opening statements
- Panel discussion → Multi-Agent for perspectives
- Q&A → Confidence Scoring for accuracy

### 2. Performance Optimization

**Balance Quality vs Speed**:
- High-quality flows take more time
- Use simpler flows for rapid exchanges
- Reserve complex flows for key arguments

**Resource Management**:
- Limit iterations in Self-Critique (2-3 max)
- Control tree depth in Tree of Thoughts
- Set reasonable tool-call limits

### 3. Monitoring and Analytics

**Track Key Metrics**:
- Average confidence scores
- Execution times
- Success rates
- Participant performance

**Iterate Based on Data**:
- Adjust configurations based on results
- A/B test different flows
- Use recommendations engine

### 4. Combining Flows

**Effective Combinations**:
1. **Research + Verification**:
   - RAG for knowledge retrieval
   - Tool-Calling for fact-checking

2. **Analysis + Quality**:
   - Tree of Thoughts for exploration
   - Confidence Scoring for validation

3. **Ethics + Perspectives**:
   - Constitutional for guidelines
   - Multi-Agent for balance

## Troubleshooting

### Common Issues

**1. Low Confidence Scores**
- **Problem**: Consistently low confidence
- **Solutions**:
  - Switch to different flow type
  - Adjust confidence threshold
  - Check prompt clarity

**2. Slow Execution**
- **Problem**: Flows taking too long
- **Solutions**:
  - Reduce iteration counts
  - Simplify tree depth
  - Use caching for repeated queries

**3. Tool Calling Failures**
- **Problem**: External tools not working
- **Solutions**:
  - Verify tool permissions
  - Check API limits
  - Review tool configuration

**4. Inconsistent Results**
- **Problem**: High variance in outputs
- **Solutions**:
  - Use Ensemble Voting
  - Lower temperature settings
  - Add Post-Processing Rules

### Error Messages

**"Flow execution timeout"**
- Increase timeout in configuration
- Reduce complexity parameters

**"Insufficient permissions"**
- Check organization settings
- Verify user role permissions

**"Rate limit exceeded"**
- Implement flow queuing
- Upgrade plan if needed

## FAQ

**Q: Can I use multiple flows on the same participant?**
A: Yes, but only one flow executes per response. The system selects the most appropriate based on context.

**Q: How do flows affect API usage?**
A: Complex flows use more API calls. Monitor usage in analytics dashboard.

**Q: Can I create custom flow types?**
A: Currently, custom flows aren't supported, but you can extensively configure existing types.

**Q: Do flows work with all AI models?**
A: Flows work with all supported models, but performance varies. Larger models generally perform better with complex flows.

**Q: How do I know which flow was used for a response?**
A: Check the flow indicator icon next to each response. Click "View Details" for full execution information.

**Q: Can flows be shared between debates?**
A: Flow configurations can be saved as templates and reused across debates within your organization.

**Q: What happens if a flow fails?**
A: The system falls back to standard response generation. Failed executions are logged for debugging.

**Q: How do confidence scores work?**
A: Confidence is calculated based on flow-specific metrics. Higher scores indicate more reliable responses.

**Q: Can I export flow analytics?**
A: Yes, analytics can be exported as CSV or JSON from the analytics dashboard.

**Q: Is there a limit to flow configurations?**
A: Free tier: 5 flows per debate. Pro: 20 flows. Enterprise: Unlimited.

## Getting Help

- **Documentation**: [docs.zamaz-debate.com/agentic-flows](https://docs.zamaz-debate.com/agentic-flows)
- **Support**: support@zamaz-debate.com
- **Community**: [community.zamaz-debate.com](https://community.zamaz-debate.com)
- **Video Tutorials**: [YouTube Channel](https://youtube.com/zamaz-debate)

## Next Steps

1. Start with simple flows (Internal Monologue)
2. Experiment with different configurations
3. Monitor analytics and optimize
4. Gradually adopt complex flows
5. Share learnings with your team

Remember: The best flow configuration depends on your specific use case. Experiment, measure, and iterate!