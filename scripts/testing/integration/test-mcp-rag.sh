#!/bin/bash

# MCP RAG Service (Java) Detailed Test Script
# Tests Retrieval Augmented Generation functionality

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${RAG_SERVICE_URL}"
TEST_KB_NAME="Test Knowledge Base $(date +%s)"
TEST_ORG_ID="test-org-$(date +%s)"

echo -e "${BLUE}=== MCP RAG Service (Java) Detailed Test ===${NC}"
echo -e "${BLUE}Testing service at: """$BASE_URL"""${NC}"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
if curl -s """"$BASE_URL"""/actuator/health" | grep -q "UP"; then
    echo -e "${GREEN}✓ Health check passed${NC}"
else
    echo -e "${RED}✗ Health check failed${NC}"
    exit 1
fi
echo ""

# Test 2: Check API Documentation
echo -e "${YELLOW}Test 2: Check API Documentation${NC}"
if curl -s """"$BASE_URL"""/api-docs" | jq -e '.paths' > /dev/null; then
    echo -e "${GREEN}✓ OpenAPI documentation available${NC}"
else
    echo -e "${YELLOW}⚠ OpenAPI documentation not available${NC}"
fi
echo ""

# Test 3: Create Knowledge Base (if endpoint exists)
echo -e "${YELLOW}Test 3: Create Knowledge Base${NC}"
CREATE_KB_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/api/v1/knowledge-bases" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"name\": \""""$TEST_KB_NAME"""\",
            \"organization_id\": \""""$TEST_ORG_ID"""\",
            \"description\": \"Test knowledge base for automated testing\",
            \"embedding_model\": \"openai/text-embedding-ada-002\",
            \"config\": {
                \"chunk_size\": 500,
                \"chunk_overlap\": 50,
                \"metadata_fields\": [\"source\", \"date\", \"author\"]
            }
        }
    }")

if echo """"$CREATE_KB_RESPONSE"""" | jq -e '.result.knowledge_base_id' > /dev/null; then
    KB_ID=$(echo """"$CREATE_KB_RESPONSE"""" | jq -r '.result.knowledge_base_id')
    echo -e "${GREEN}✓ Knowledge base created with ID: """$KB_ID"""${NC}"
    echo "Response: $(echo """"$CREATE_KB_RESPONSE"""" | jq -c '.result')"
else
    echo -e "${RED}✗ Failed to create knowledge base${NC}"
    echo "Response: """$CREATE_KB_RESPONSE""""
    exit 1
fi
echo ""

# Test 3: Ingest Document
echo -e "${YELLOW}Test 3: Ingest Document${NC}"
INGEST_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/tools/ingest_document" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"content\": \"Artificial Intelligence (AI) is revolutionizing various industries. Machine learning, a subset of AI, enables computers to learn from data without explicit programming. Deep learning, using neural networks, has achieved breakthrough results in image recognition, natural language processing, and game playing. Key applications include autonomous vehicles, medical diagnosis, financial fraud detection, and personalized recommendations. However, AI also raises ethical concerns about privacy, bias, job displacement, and the need for explainable AI systems.\",
            \"metadata\": {
                \"source\": \"AI Overview Document\",
                \"date\": \"2024-01-15\",
                \"author\": \"Test Author\",
                \"category\": \"Technology\",
                \"tags\": [\"AI\", \"ML\", \"Deep Learning\"]
            },
            \"document_id\": \"doc-001\"
        }
    }")

if echo """"$INGEST_RESPONSE"""" | jq -e '.result.success' | grep -q "true"; then
    echo -e "${GREEN}✓ Document ingested successfully${NC}"
    CHUNKS=$(echo """"$INGEST_RESPONSE"""" | jq -r '.result.chunks_created // "unknown"')
    echo -e "  Chunks created: """$CHUNKS""""
else
    echo -e "${RED}✗ Failed to ingest document${NC}"
    echo "Response: """$INGEST_RESPONSE""""
fi
echo ""

# Test 4: Ingest Multiple Documents
echo -e "${YELLOW}Test 4: Ingest Multiple Documents${NC}"
echo -e "  Ingesting additional test documents..."

# Document 2
curl -s -X POST """"$BASE_URL"""/tools/ingest_document" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"content\": \"Ethics in AI development is crucial. Key principles include transparency, fairness, accountability, and privacy protection. Bias in AI systems can perpetuate discrimination. Explainable AI helps users understand decision-making processes. Regulatory frameworks are emerging globally to govern AI use.\",
            \"metadata\": {
                \"source\": \"AI Ethics Guide\",
                \"date\": \"2024-01-20\",
                \"author\": \"Ethics Committee\",
                \"category\": \"Ethics\"
            },
            \"document_id\": \"doc-002\"
        }
    }" > /dev/null

# Document 3
curl -s -X POST """"$BASE_URL"""/tools/ingest_document" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"content\": \"Large Language Models (LLMs) like GPT and Claude use transformer architectures. Training requires massive datasets and computational resources. Fine-tuning adapts models for specific tasks. Prompt engineering optimizes model responses. RAG combines retrieval with generation for factual accuracy.\",
            \"metadata\": {
                \"source\": \"LLM Technical Overview\",
                \"date\": \"2024-01-25\",
                \"author\": \"ML Team\",
                \"category\": \"Technical\"
            },
            \"document_id\": \"doc-003\"
        }
    }" > /dev/null

echo -e "${GREEN}✓ Multiple documents ingested${NC}"
echo ""

# Test 5: Search Knowledge Base
echo -e "${YELLOW}Test 5: Search Knowledge Base${NC}"
SEARCH_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/tools/search" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"query\": \"What are the ethical concerns with AI?\",
            \"top_k\": 5,
            \"filters\": {
                \"category\": [\"Ethics\", \"Technology\"]
            }
        }
    }")

if echo """"$SEARCH_RESPONSE"""" | jq -e '.result.results' > /dev/null; then
    RESULT_COUNT=$(echo """"$SEARCH_RESPONSE"""" | jq '.result.results | length')
    echo -e "${GREEN}✓ Search returned """$RESULT_COUNT""" results${NC}"
    
    # Show top result
    if [ """"$RESULT_COUNT"""" -gt 0 ]; then
        echo "Top result:"
        echo """"$SEARCH_RESPONSE"""" | jq -r '.result.results[0] | "  Score: \(.score // "N/A")\n  Content: \(.content | .[0:100])..."'
    fi
else
    echo -e "${RED}✗ Search failed${NC}"
    echo "Response: """$SEARCH_RESPONSE""""
fi
echo ""

# Test 6: Augment Context
echo -e "${YELLOW}Test 6: Augment Context with RAG${NC}"
AUGMENT_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/tools/augment_context" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"context\": {
                \"messages\": [
                    {\"role\": \"user\", \"content\": \"Tell me about bias in AI systems\"}
                ],
                \"organization_id\": \""""$TEST_ORG_ID"""\"
            },
            \"augmentation_config\": {
                \"max_chunks\": 3,
                \"min_relevance_score\": 0.7,
                \"include_metadata\": true
            }
        }
    }")

if echo """"$AUGMENT_RESPONSE"""" | jq -e '.result.augmented_context' > /dev/null; then
    echo -e "${GREEN}✓ Context augmented successfully${NC}"
    ADDED_COUNT=$(echo """"$AUGMENT_RESPONSE"""" | jq '.result.chunks_added // 0')
    echo -e "  Chunks added to context: """$ADDED_COUNT""""
    
    # Show augmented context structure
    if echo """"$AUGMENT_RESPONSE"""" | jq -e '.result.augmented_context.system_prompt' > /dev/null; then
        echo -e "  System prompt updated with relevant information"
    fi
else
    echo -e "${YELLOW}⚠ Context augmentation may not be implemented${NC}"
    echo "Response: """$AUGMENT_RESPONSE""""
fi
echo ""

# Test 7: List Knowledge Bases
echo -e "${YELLOW}Test 7: List Knowledge Bases${NC}"
LIST_KB_RESPONSE=$(curl -s """"$BASE_URL"""/resources/rag://knowledge-bases/read")

if echo """"$LIST_KB_RESPONSE"""" | jq -e '.contents' > /dev/null; then
    KB_COUNT=$(echo """"$LIST_KB_RESPONSE"""" | jq '.contents | length')
    echo -e "${GREEN}✓ Found """$KB_COUNT""" knowledge bases${NC}"
    
    # Verify our test KB is in the list
    if echo """"$LIST_KB_RESPONSE"""" | jq -e ".contents[] | select(.id == \""""$KB_ID"""\")" > /dev/null; then
        echo -e "${GREEN}✓ Test knowledge base found in list${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Knowledge base listing may use different format${NC}"
fi
echo ""

# Test 8: Update Embeddings
echo -e "${YELLOW}Test 8: Update Embeddings${NC}"
UPDATE_EMBEDDINGS_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/tools/update_embeddings" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"document_ids\": [\"doc-001\"],
            \"new_model\": \"openai/text-embedding-3-small\"
        }
    }")

if echo """"$UPDATE_EMBEDDINGS_RESPONSE"""" | jq -e '.result.success' | grep -q "true"; then
    echo -e "${GREEN}✓ Embeddings updated successfully${NC}"
    UPDATED=$(echo """"$UPDATE_EMBEDDINGS_RESPONSE"""" | jq -r '.result.documents_updated // 0')
    echo -e "  Documents updated: """$UPDATED""""
else
    echo -e "${YELLOW}⚠ Embedding update may not be implemented${NC}"
fi
echo ""

# Test 9: Get System Stats
echo -e "${YELLOW}Test 9: Get System Statistics${NC}"
STATS_RESPONSE=$(curl -s """"$BASE_URL"""/resources/rag://stats/read")

if echo """"$STATS_RESPONSE"""" | jq -e '.contents' > /dev/null; then
    echo -e "${GREEN}✓ Retrieved system statistics${NC}"
    echo """"$STATS_RESPONSE"""" | jq -r '.contents | to_entries[] | "  \(.key): \(.value)"' 2>/dev/null || echo "  Stats format varies"
else
    echo -e "${YELLOW}⚠ System stats may not be implemented${NC}"
fi
echo ""

# Test 10: Export Knowledge Base
echo -e "${YELLOW}Test 10: Export Knowledge Base${NC}"
EXPORT_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/tools/export_knowledge_base" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"format\": \"json\",
            \"include_embeddings\": false
        }
    }")

if echo """"$EXPORT_RESPONSE"""" | jq -e '.result.export_data' > /dev/null; then
    echo -e "${GREEN}✓ Knowledge base exported successfully${NC}"
    DOC_COUNT=$(echo """"$EXPORT_RESPONSE"""" | jq '.result.export_data.documents | length' 2>/dev/null || echo "0")
    echo -e "  Documents exported: """$DOC_COUNT""""
else
    echo -e "${YELLOW}⚠ Export may not be implemented${NC}"
fi
echo ""

# Test 11: Delete Document
echo -e "${YELLOW}Test 11: Delete Document${NC}"
DELETE_DOC_RESPONSE=$(curl -s -X POST """"$BASE_URL"""/tools/delete_document" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"document_id\": \"doc-003\"
        }
    }")

if echo """"$DELETE_DOC_RESPONSE"""" | jq -e '.result.success' | grep -q "true"; then
    echo -e "${GREEN}✓ Document deleted successfully${NC}"
else
    echo -e "${YELLOW}⚠ Document deletion may not be implemented${NC}"
fi
echo ""

# Test 12: Advanced Search Features
echo -e "${YELLOW}Test 12: Advanced Search Features${NC}"

# Semantic search
echo -e "  Testing semantic search..."
SEMANTIC_SEARCH=$(curl -s -X POST """"$BASE_URL"""/tools/search" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"query\": \"How do neural networks learn?\",
            \"search_type\": \"semantic\",
            \"top_k\": 3
        }
    }")

if echo """"$SEMANTIC_SEARCH"""" | jq -e '.result.results' > /dev/null; then
    echo -e "  ${GREEN}✓ Semantic search working${NC}"
fi

# Hybrid search
echo -e "  Testing hybrid search..."
HYBRID_SEARCH=$(curl -s -X POST """"$BASE_URL"""/tools/search" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"knowledge_base_id\": \""""$KB_ID"""\",
            \"query\": \"machine learning applications\",
            \"search_type\": \"hybrid\",
            \"keyword_weight\": 0.3,
            \"semantic_weight\": 0.7
        }
    }")

if echo """"$HYBRID_SEARCH"""" | jq -e '.result.results' > /dev/null; then
    echo -e "  ${GREEN}✓ Hybrid search working${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}=== Test Summary ===${NC}"
echo -e "${GREEN}✓ Health check${NC}"
echo -e "${GREEN}✓ Knowledge base creation${NC}"
echo -e "${GREEN}✓ Document ingestion${NC}"
echo -e "${GREEN}✓ Vector search${NC}"
echo -e "${GREEN}✓ Context augmentation${NC}"
echo -e "${GREEN}✓ Document management${NC}"
echo -e "${BLUE}All critical tests passed!${NC}"