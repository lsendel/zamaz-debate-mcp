# Debate System - CURL Examples

## Complete Debate Flow: "Should I use Airtable and DAG in the system?"

### 1. Create Organization
```bash
curl -X POST http://localhost:5005/api/organizations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tech Architecture Debates",
    "description": "Debates about technology choices"
  }'

# Response:
# {"id":"org-123","name":"Tech Architecture Debates","description":"Debates about technology choices"}
```

### 2. Create Debate
```bash
curl -X POST http://localhost:5013/api/debates \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Should I use Airtable and DAG in the system?",
    "description": "Evaluating Airtable as database and DAG for workflows",
    "format": "OXFORD",
    "organizationId": "org-123",
    "maxRounds": 3,
    "maxResponseLength": 500,
    "participantNames": ["Pro-Airtable", "Anti-Airtable"]
  }'

# Response:
# {
#   "id": "debate-456",
#   "topic": "Should I use Airtable and DAG in the system?",
#   "status": "IN_PROGRESS",
#   "currentRound": 1,
#   "participants": [
#     {"id": "part-1", "name": "Pro-Airtable", "type": "AI"},
#     {"id": "part-2", "name": "Anti-Airtable", "type": "AI"}
#   ]
# }
```

### 3. Submit Turns for Round 1 (Opening Statements)

**Pro-Airtable Opening:**
```bash
curl -X POST http://localhost:5013/api/debates/debate-456/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "part-1",
    "content": "Airtable offers rapid development with built-in UI, perfect for MVPs. DAGs provide clear workflow orchestration. Together, they enable fast iteration while maintaining system clarity."
  }'
```

**Anti-Airtable Opening:**
```bash
curl -X POST http://localhost:5013/api/debates/debate-456/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "part-2",
    "content": "Airtable has severe API limits and lacks database features. DAGs overcomplicate workflows. PostgreSQL with simple async workers provides better performance and flexibility."
  }'
```

### 4. Submit Turns for Round 2 (Rebuttals)

**Pro-Airtable Rebuttal:**
```bash
curl -X POST http://localhost:5013/api/debates/debate-456/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "part-1",
    "content": "API limits are generous for most startups. The visual interface reduces dev time by 70%. DAGs provide clarity, not complexity. Ship features now, optimize later."
  }'
```

**Anti-Airtable Rebuttal:**
```bash
curl -X POST http://localhost:5013/api/debates/debate-456/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "part-2",
    "content": "5 requests/second kills production apps. You need sub-millisecond queries, not 200ms API calls. Migration from Airtable is painful. Start with proper foundations."
  }'
```

### 5. Submit Turns for Round 3 (Closing Arguments)

**Pro-Airtable Closing:**
```bash
curl -X POST http://localhost:5013/api/debates/debate-456/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "part-1",
    "content": "Consider total cost: Airtable includes versioning, forms, automations. DAGs make behavior predictable. For startups, shipping quickly beats premature optimization."
  }'
```

**Anti-Airtable Closing:**
```bash
curl -X POST http://localhost:5013/api/debates/debate-456/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "part-2",
    "content": "Technical debt compounds. You will need transactions, complex queries, replication. DAGs lock you into patterns. Build on PostgreSQL + Redis. Thank yourself later."
  }'
```

### 6. Get Complete Debate Results
```bash
curl http://localhost:5013/api/debates/debate-456

# Response:
# {
#   "id": "debate-456",
#   "topic": "Should I use Airtable and DAG in the system?",
#   "status": "COMPLETED",
#   "format": "OXFORD",
#   "currentRound": 3,
#   "maxRounds": 3,
#   "participants": [
#     {
#       "id": "part-1",
#       "name": "Pro-Airtable",
#       "type": "AI",
#       "score": 0
#     },
#     {
#       "id": "part-2", 
#       "name": "Anti-Airtable",
#       "type": "AI",
#       "score": 0
#     }
#   ],
#   "createdAt": "2024-01-14T10:00:00Z",
#   "updatedAt": "2024-01-14T10:30:00Z"
# }
```

### 7. Get All Turns (Full Transcript)
```bash
curl http://localhost:5013/api/debates/debate-456/turns

# Response:
# [
#   {
#     "id": "turn-1",
#     "debateId": "debate-456",
#     "round": 1,
#     "participant": {"id": "part-1", "name": "Pro-Airtable"},
#     "content": "Airtable offers rapid development...",
#     "createdAt": "2024-01-14T10:05:00Z"
#   },
#   {
#     "id": "turn-2",
#     "debateId": "debate-456",
#     "round": 1,
#     "participant": {"id": "part-2", "name": "Anti-Airtable"},
#     "content": "Airtable has severe API limits...",
#     "createdAt": "2024-01-14T10:10:00Z"
#   },
#   // ... all 6 turns
# ]
```

### 8. Get AI Analysis (Optional)
```bash
curl -X POST http://localhost:5002/api/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "prompt": "Analyze this debate about Airtable vs traditional DB. Who made stronger arguments?",
    "context": "Include the debate transcript here",
    "maxTokens": 300
  }'
```

## Summary View Commands

### List All Debates
```bash
curl "http://localhost:5013/api/debates?organizationId=org-123"
```

### Get Debate Statistics
```bash
curl "http://localhost:5013/api/debates/debate-456/stats"
```

### Export Debate as Markdown
```bash
curl "http://localhost:5013/api/debates/debate-456/export?format=markdown" > debate-transcript.md
```

## The Verdict Structure

Based on the debate, here's how to interpret results:

**Pro-Airtable Key Points:**
- ✅ Rapid development (70% faster)
- ✅ Built-in UI and collaboration
- ✅ No database administration
- ✅ Clear workflow orchestration with DAGs
- ✅ Lower operational overhead

**Anti-Airtable Key Points:**
- ❌ API rate limits (5 req/sec)
- ❌ Poor query performance (200ms vs <1ms)
- ❌ Vendor lock-in
- ❌ Limited database features
- ❌ Migration difficulties

**Recommendation Decision Tree:**
```
If (MVP or Prototype) && (User Count < 1000) && (Simple Queries)
  → Use Airtable + DAG
  
ElseIf (Scaling Product) && (Complex Queries) && (Performance Critical)
  → Use PostgreSQL + Redis + Async Workers
  
Else
  → Start with Airtable, plan migration path
```

## Testing the Complete Flow

Run all commands in sequence:
```bash
# 1. Create org
ORG_ID=$(curl -s -X POST http://localhost:5005/api/organizations -H "Content-Type: application/json" -d '{"name":"Test Org"}' | jq -r .id)

# 2. Create debate  
DEBATE_ID=$(curl -s -X POST http://localhost:5013/api/debates -H "Content-Type: application/json" -d '{"topic":"Airtable vs PostgreSQL","format":"OXFORD","organizationId":"'$ORG_ID'","participantNames":["Pro","Con"]}' | jq -r .id)

# 3. Get participants
PARTICIPANTS=$(curl -s http://localhost:5013/api/debates/$DEBATE_ID | jq -r '.participants')

# 4. Submit all turns and view results
# ... (use the commands above)
```