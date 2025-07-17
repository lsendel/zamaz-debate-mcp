# Context Sharing Strategy for Dual MCP Architecture

## Challenge
How to effectively share conversation context between mcp-debate (orchestrator) and mcp-llm (LLM provider) while maintaining:
- Stateless LLM service
- Efficient token usage
- Context continuity
- Performance

## Recommended Approach: Stateless LLM + Stateful Debate

### Architecture Pattern

```
┌─────────────────────────────────────────────────────┐
│                   mcp-debate                         │
│  ┌─────────────────────────────────────────────┐   │
│  │          Context Manager                     │   │
│  │  - Stores full conversation history          │   │
│  │  - Manages context windows per participant   │   │
│  │  - Implements sliding window algorithm       │   │
│  │  - Handles context compression/summarization │   │
│  └─────────────────────────────────────────────┘   │
│                      │                              │
│                      ▼                              │
│  ┌─────────────────────────────────────────────┐   │
│  │          Context Optimizer                   │   │
│  │  - Selects relevant context for each turn   │   │
│  │  - Applies participant-specific prompts     │   │
│  │  - Ensures token limits are respected       │   │
│  └─────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────┘
                       │
                       │ Sends optimized context
                       │ with each request
                       ▼
┌─────────────────────────────────────────────────────┐
│                   mcp-llm                           │
│  ┌─────────────────────────────────────────────┐   │
│  │          Stateless Processing                │   │
│  │  - Receives complete context each request    │   │
│  │  - No conversation state stored              │   │
│  │  - Pure function: context → response         │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. Context Structure in mcp-debate

```python
class DebateContext:
    debate_id: str
    messages: List[Message]  # Full history
    participants: Dict[str, ParticipantContext]
    metadata: Dict[str, Any]
    
class ParticipantContext:
    participant_id: str
    system_prompt: str
    conversation_window: List[Message]  # Optimized subset
    token_usage: TokenUsage
    summary: Optional[str]  # For long conversations
```

### 2. Context Optimization Strategy

```python
class ContextOptimizer:
    def prepare_context_for_llm(
        self,
        debate_context: DebateContext,
        participant: Participant,
        target_model: ModelInfo
    ) -> List[Message]:
        """
        Prepares optimized context for LLM call
        """
        # 1. Start with system prompt
        messages = [Message(role="system", content=participant.system_prompt)]
        
        # 2. Add debate rules/format if needed
        if debate_context.metadata.get("include_rules"):
            messages.append(Message(
                role="system", 
                content=f"Debate rules: {debate_context.metadata['rules']}"
            ))
        
        # 3. Add conversation history using sliding window
        window_size = self._calculate_window_size(
            target_model.context_window,
            participant.reserved_tokens
        )
        
        # 4. Include summary of older messages if needed
        if len(debate_context.messages) > window_size:
            summary = self._get_or_create_summary(
                debate_context.messages[:-window_size]
            )
            messages.append(Message(
                role="system",
                content=f"Previous discussion summary: {summary}"
            ))
        
        # 5. Add recent messages within window
        recent_messages = debate_context.messages[-window_size:]
        messages.extend(recent_messages)
        
        return messages
```

### 3. Request Flow

```python
# In mcp-debate when orchestrating a turn
async def execute_participant_turn(debate_id: str, participant_id: str):
    # 1. Get full debate context
    debate_context = await context_manager.get_context(debate_id)
    
    # 2. Optimize context for this participant
    optimized_messages = context_optimizer.prepare_context_for_llm(
        debate_context,
        participant,
        target_model
    )
    
    # 3. Call mcp-llm with optimized context
    response = await llm_client.complete(
        provider=participant.llm_provider,
        model=participant.model,
        messages=optimized_messages,  # Complete context sent
        temperature=participant.temperature
    )
    
    # 4. Add response to debate context
    await context_manager.add_message(
        debate_id,
        Message(
            role="assistant",
            content=response.content,
            name=participant.name
        )
    )
```

### 4. Advanced Context Features

#### A. Semantic Context Selection
```python
class SemanticContextSelector:
    """Select most relevant messages based on semantic similarity"""
    
    async def select_relevant_messages(
        self,
        all_messages: List[Message],
        current_topic: str,
        max_messages: int
    ) -> List[Message]:
        # Use embeddings to find most relevant past messages
        embeddings = await self.embed_messages(all_messages)
        topic_embedding = await self.embed_text(current_topic)
        
        similarities = self.calculate_similarities(embeddings, topic_embedding)
        top_indices = np.argsort(similarities)[-max_messages:]
        
        return [all_messages[i] for i in top_indices]
```

#### B. Dynamic Context Compression
```python
class ContextCompressor:
    """Compress context while preserving key information"""
    
    async def compress_context(
        self,
        messages: List[Message],
        target_tokens: int
    ) -> List[Message]:
        # 1. Identify key messages (debate positions, conclusions)
        key_messages = self.identify_key_messages(messages)
        
        # 2. Summarize non-key messages
        other_messages = [m for m in messages if m not in key_messages]
        summary = await self.summarize_messages(other_messages)
        
        # 3. Reconstruct compressed context
        compressed = [
            Message(role="system", content=f"Summary: {summary}"),
            *key_messages
        ]
        
        return compressed
```

### 5. Context Sharing Benefits

1. **Stateless LLM Service**
   - Can scale horizontally
   - No session management
   - Simple caching strategy

2. **Flexible Context Management**
   - Different strategies per participant
   - Dynamic window sizing
   - Semantic relevance filtering

3. **Token Optimization**
   - Only send necessary context
   - Compress older messages
   - Respect model limits

4. **Participant Isolation**
   - Each participant can have different context views
   - Custom system prompts maintained
   - Independent token budgets

### 6. Alternative Approaches Considered

#### Option A: Shared Context Store (Redis)
- Pro: Both services access same store
- Con: Coupling, complex cache invalidation

#### Option B: Context IDs with Lazy Loading
- Pro: Minimal data transfer
- Con: Multiple round trips, complex state management

#### Option C: Event-Driven Context Sync
- Pro: Real-time updates
- Con: Complex event handling, potential inconsistencies

## Recommendation

The **Stateless LLM + Stateful Debate** approach is recommended because:

1. **Simplicity**: Clear separation of concerns
2. **Scalability**: LLM service remains stateless
3. **Flexibility**: Debate service controls context optimization
4. **Performance**: Single request per LLM call
5. **Maintainability**: Easy to debug and modify

This approach treats the LLM service as a pure function that transforms context into responses, while the debate service handles all the complex context management, making the system both powerful and maintainable.