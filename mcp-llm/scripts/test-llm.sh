#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
LLM_SERVICE_URL="${LLM_SERVICE_URL}"
TEST_PROMPT="${1:-"Hello, can you explain what artificial intelligence is in simple terms?"}"

echo -e "${BLUE}=== LLM Service Test Tool ===${NC}"
echo -e "Service URL: ${LLM_SERVICE_URL}"
echo ""

# Function to test LLM endpoint
test_llm() {
    local provider=$1
    local model=$2
    local prompt=$3
    
    echo -e "${CYAN}Testing ${provider} - ${model}${NC}"
    
    # Create request payload
    local payload=$(cat <<EOF
{
    "provider": "${provider}",
    "model": "${model}",
    "messages": [
        {
            "role": "user",
            "content": "${prompt}"
        }
    ],
    "max_tokens": 150,
    "temperature": 0.7
}
EOF
)
    
    # Make request and measure time
    local start_time=$(date +%s.%N)
    
    response=$(curl -s -X POST "${LLM_SERVICE_URL}/completions" \
        -H "Content-Type: application/json" \
        -d "${payload}" \
        -w "\n{\"http_code\": %{http_code}, \"total_time\": %{time_total}}")
    
    local end_time=$(date +%s.%N)
    local duration=$(echo """"$end_time""" - """$start_time"""" | bc)
    
    # Extract HTTP code and response body
    http_code=$(echo """"$response"""" | tail -n1 | jq -r '.http_code')
    response_body=$(echo """"$response"""" | sed '"""$d"""')
    
    if [ """"$http_code"""" = "200" ]; then
        # Extract the response text
        response_text=$(echo """"$response_body"""" | jq -r '.choices[0].text' 2>/dev/null)
        token_usage=$(echo """"$response_body"""" | jq -r '.usage' 2>/dev/null)
        
        if [ -n """"$response_text"""" ] && [ """"$response_text"""" != "null" ]; then
            echo -e "${GREEN}✓ Success${NC}"
            echo -e "Response time: ${duration}s"
            echo -e "Response preview: ${response_text:0:100}..."
            
            if [ """"$token_usage"""" != "null" ]; then
                echo -e "Tokens used: $(echo """"$token_usage"""" | jq -r '.total_tokens // 0')"
            fi
        else
            echo -e "${RED}✗ Failed - Invalid response format${NC}"
            echo "Response: """$response_body""""
        fi
    else
        echo -e "${RED}✗ Failed - HTTP ${http_code}${NC}"
        error_detail=$(echo """"$response_body"""" | jq -r '.detail // .error // "Unknown error"' 2>/dev/null)
        echo "Error: """$error_detail""""
    fi
    
    echo ""
}

# Check if LLM service is healthy
echo -e "${BLUE}Checking LLM service health...${NC}"
health_response=$(curl -s "${LLM_SERVICE_URL}/health")
health_status=$(echo """"$health_response"""" | jq -r '.status' 2>/dev/null)

if [ """"$health_status"""" = "healthy" ]; then
    echo -e "${GREEN}✓ LLM service is healthy${NC}"
    
    # Show available providers
    echo -e "\n${BLUE}Available providers:${NC}"
    echo """"$health_response"""" | jq -r '.providers | to_entries[] | "\(.key): \(.value.available)"'
else
    echo -e "${RED}✗ LLM service is not healthy${NC}"
    echo "Response: """$health_response""""
    exit 1
fi

# Get available models
echo -e "\n${BLUE}Fetching available models...${NC}"
models_response=$(curl -s "${LLM_SERVICE_URL}/models")
models=$(echo """"$models_response"""" | jq -r '.models[]' 2>/dev/null)

if [ -z """"$models"""" ]; then
    echo -e "${RED}✗ No models available${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found $(echo """"$models_response"""" | jq '.models | length') models${NC}"

# Test each available provider
echo -e "\n${BLUE}=== Testing LLM Providers ===${NC}"
echo -e "Test prompt: \"${TEST_PROMPT}\""
echo ""

# Test Claude if available
if echo """"$health_response"""" | jq -e '.providers.claude.available' > /dev/null 2>&1; then
    test_llm "claude" "claude-3-5-sonnet-20241022" """"$TEST_PROMPT""""
fi

# Test OpenAI if available
if echo """"$health_response"""" | jq -e '.providers.openai.available' > /dev/null 2>&1; then
    test_llm "openai" "gpt-4o" """"$TEST_PROMPT""""
fi

# Test Gemini if available
if echo """"$health_response"""" | jq -e '.providers.gemini.available' > /dev/null 2>&1; then
    test_llm "gemini" "gemini-2.0-flash-exp" """"$TEST_PROMPT""""
fi

# Test local models (always available)
test_llm "llama" "mistral" """"$TEST_PROMPT""""

# Test debate creation with LLM
echo -e "${BLUE}=== Testing Debate Creation with LLM ===${NC}"

debate_payload=$(cat <<EOF
{
    "arguments": {
        "name": "LLM Test Debate",
        "topic": "Is AI beneficial for humanity?",
        "description": "Testing LLM integration",
        "participants": [
            {
                "name": "AI Optimist",
                "role": "debater",
                "position": "Pro-AI",
                "llm_config": {
                    "provider": "claude",
                    "model": "claude-3-5-sonnet-20241022",
                    "temperature": 0.7
                }
            },
            {
                "name": "AI Skeptic",
                "role": "debater",
                "position": "AI Concerns",
                "llm_config": {
                    "provider": "openai",
                    "model": "gpt-4o",
                    "temperature": 0.7
                }
            }
        ],
        "rules": {
            "format": "round_robin",
            "maxRounds": 2,
            "maxTurnLength": 200
        }
    }
}
EOF
)

debate_response=$(curl -s -X POST "${CONTROLLER_SERVICE_URL}/tools/create_debate" \
    -H "Content-Type: application/json" \
    -d "${debate_payload}")

debate_id=$(echo """"$debate_response"""" | jq -r '.debateId' 2>/dev/null)

if [ -n """"$debate_id"""" ] && [ """"$debate_id"""" != "null" ]; then
    echo -e "${GREEN}✓ Debate created successfully: ${debate_id}${NC}"
    
    # Start the debate
    echo -e "\n${BLUE}Starting debate...${NC}"
    start_response=$(curl -s -X POST "${CONTROLLER_SERVICE_URL}/tools/start_debate" \
        -H "Content-Type: application/json" \
        -d "{\"arguments\": {\"debate_id\": \"${debate_id}\"}}")
    
    if echo """"$start_response"""" | jq -e '.status == "started"' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Debate started${NC}"
        
        # Test LLM-generated turn
        echo -e "\n${BLUE}Testing LLM-generated debate turn...${NC}"
        turn_response=$(curl -s -X POST "${CONTROLLER_SERVICE_URL}/tools/add_turn" \
            -H "Content-Type: application/json" \
            -d "{\"arguments\": {\"debate_id\": \"${debate_id}\", \"participant_name\": \"AI Optimist\", \"content\": \"\"}}")
        
        if echo """"$turn_response"""" | jq -e '.status == "turn_added"' > /dev/null 2>&1; then
            echo -e "${GREEN}✓ LLM successfully generated debate turn${NC}"
        else
            echo -e "${RED}✗ Failed to generate turn${NC}"
            echo "Response: """$turn_response""""
        fi
    else
        echo -e "${RED}✗ Failed to start debate${NC}"
        echo "Response: """$start_response""""
    fi
else
    echo -e "${RED}✗ Failed to create debate${NC}"
    echo "Response: """$debate_response""""
fi

echo -e "\n${BLUE}=== LLM Test Complete ===${NC}"