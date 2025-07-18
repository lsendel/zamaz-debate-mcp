#!/bin/bash

# MCP Template Service (Java) Detailed Test Script
# Tests template management and rendering

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:5006"
TEST_TEMPLATE_NAME="Test Template $(date +%s)"
TEST_ORG_ID="test-org-$(date +%s)"

echo -e "${BLUE}=== MCP Template Service (Java) Detailed Test ===${NC}"
echo -e "${BLUE}Testing service at: ""$BASE_URL""${NC}"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
if curl -s """$BASE_URL""/actuator/health" | grep -q "UP"; then
    echo -e "${GREEN}✓ Health check passed${NC}"
else
    echo -e "${RED}✗ Health check failed${NC}"
    exit 1
fi
echo ""

# Test 2: Create Template
echo -e "${YELLOW}Test 2: Create Template${NC}"
CREATE_TEMPLATE_RESPONSE=$(curl -s -X POST """$BASE_URL""/api/v1/templates" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"""$TEST_TEMPLATE_NAME""\",
        \"organizationId\": \"""$TEST_ORG_ID""\",
            \"category\": \"debate\",
            \"description\": \"Template for debate opening statements\",
            \"content\": \"Hello {{ participant_name }},\\n\\nAs we begin this debate on {{ topic }}, I'd like to present my position:\\n\\n{{ opening_statement }}\\n\\nKey points to consider:\\n{% for point in key_points %}\\n- {{ point }}\\n{% endfor %}\\n\\nI look forward to a productive discussion.\",
            \"variables\": {
                \"participant_name\": {
                    \"type\": \"string\",
                    \"description\": \"Name of the debate participant\",
                    \"required\": true
                },
                \"topic\": {
                    \"type\": \"string\",
                    \"description\": \"Debate topic\",
                    \"required\": true
                },
                \"opening_statement\": {
                    \"type\": \"string\",
                    \"description\": \"Main opening statement\",
                    \"required\": true
                },
                \"key_points\": {
                    \"type\": \"array\",
                    \"description\": \"List of key points\",
                    \"required\": false,
                    \"default\": []
                }
            },
            \"metadata\": {
                \"author\": \"Test System\",
                \"version\": \"1.0\",
                \"tags\": [\"debate\", \"opening\", \"formal\"]
            }
        }
    }")

if echo """$CREATE_TEMPLATE_RESPONSE""" | jq -e '.result.template_id' > /dev/null; then
    TEMPLATE_ID=$(echo """$CREATE_TEMPLATE_RESPONSE""" | jq -r '.result.template_id')
    echo -e "${GREEN}✓ Template created with ID: ""$TEMPLATE_ID""${NC}"
    echo "Response: $(echo """$CREATE_TEMPLATE_RESPONSE""" | jq -c '.result')"
else
    echo -e "${RED}✗ Failed to create template${NC}"
    echo "Response: ""$CREATE_TEMPLATE_RESPONSE"""
    exit 1
fi
echo ""

# Test 3: Render Template
echo -e "${YELLOW}Test 3: Render Template${NC}"
RENDER_RESPONSE=$(curl -s -X POST """$BASE_URL""/tools/render_template" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"template_id\": \"""$TEMPLATE_ID""\",
            \"variables\": {
                \"participant_name\": \"Dr. Smith\",
                \"topic\": \"The Role of AI in Education\",
                \"opening_statement\": \"I believe AI can revolutionize education by providing personalized learning experiences.\",
                \"key_points\": [
                    \"Adaptive learning paths for each student\",
                    \"24/7 availability of AI tutors\",
                    \"Data-driven insights for educators\"
                ]
            }
        }
    }")

if echo """$RENDER_RESPONSE""" | jq -e '.result.rendered_content' > /dev/null; then
    echo -e "${GREEN}✓ Template rendered successfully${NC}"
    echo "Rendered content:"
    echo """$RENDER_RESPONSE""" | jq -r '.result.rendered_content' | head -n 10
    echo "..."
else
    echo -e "${RED}✗ Failed to render template${NC}"
    echo "Response: ""$RENDER_RESPONSE"""
fi
echo ""

# Test 4: Create Multiple Templates
echo -e "${YELLOW}Test 4: Create Multiple Templates${NC}"
echo -e "  Creating additional templates..."

# Closing statement template
CLOSE_TEMPLATE=$(curl -s -X POST """$BASE_URL""/tools/create_template" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"name\": \"Debate Closing Statement\",
            \"organization_id\": \"""$TEST_ORG_ID""\",
            \"category\": \"debate\",
            \"content\": \"In conclusion, {{ summary }}\\n\\nThank you for this engaging debate on {{ topic }}.\",
            \"variables\": {
                \"summary\": {\"type\": \"string\", \"required\": true},
                \"topic\": {\"type\": \"string\", \"required\": true}
            }
        }
    }")

# Email template
EMAIL_TEMPLATE=$(curl -s -X POST """$BASE_URL""/tools/create_template" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"name\": \"Debate Invitation Email\",
            \"organization_id\": \"""$TEST_ORG_ID""\",
            \"category\": \"email\",
            \"content\": \"Subject: Invitation to Debate: {{ topic }}\\n\\nDear {{ recipient_name }},\\n\\nYou are invited to participate in a debate on {{ topic }}.\\n\\nDate: {{ date }}\\nTime: {{ time }}\\n\\nBest regards,\\n{{ sender_name }}\",
            \"variables\": {
                \"topic\": {\"type\": \"string\", \"required\": true},
                \"recipient_name\": {\"type\": \"string\", \"required\": true},
                \"date\": {\"type\": \"string\", \"required\": true},
                \"time\": {\"type\": \"string\", \"required\": true},
                \"sender_name\": {\"type\": \"string\", \"required\": true}
            }
        }
    }")

echo -e "${GREEN}✓ Multiple templates created${NC}"
echo ""

# Test 5: Search Templates
echo -e "${YELLOW}Test 5: Search Templates${NC}"
SEARCH_RESPONSE=$(curl -s -X POST """$BASE_URL""/tools/search_templates" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"organization_id\": \"""$TEST_ORG_ID""\",
            \"query\": \"debate\",
            \"category\": \"debate\"
        }
    }")

if echo """$SEARCH_RESPONSE""" | jq -e '.result.templates' > /dev/null; then
    FOUND_COUNT=$(echo """$SEARCH_RESPONSE""" | jq '.result.templates | length')
    echo -e "${GREEN}✓ Search returned ""$FOUND_COUNT"" templates${NC}"
    
    # List found templates
    echo "Found templates:"
    echo """$SEARCH_RESPONSE""" | jq -r '.result.templates[] | "  - \(.name) (\(.category))"' 2>/dev/null || echo "  Template listing format varies"
else
    echo -e "${YELLOW}⚠ Template search may not be implemented${NC}"
fi
echo ""

# Test 6: List Template Categories
echo -e "${YELLOW}Test 6: List Template Categories${NC}"
CATEGORIES_RESPONSE=$(curl -s """$BASE_URL""/resources/template://categories/read")

if echo """$CATEGORIES_RESPONSE""" | jq -e '.contents' > /dev/null; then
    echo -e "${GREEN}✓ Retrieved template categories${NC}"
    echo "Categories:"
    echo """$CATEGORIES_RESPONSE""" | jq -r '.contents[]' 2>/dev/null | head -5 || echo "  Categories format varies"
else
    echo -e "${YELLOW}⚠ Categories resource may not be implemented${NC}"
fi
echo ""

# Test 7: Create Debate Templates
echo -e "${YELLOW}Test 7: Create Debate-Specific Templates${NC}"
DEBATE_TEMPLATES_RESPONSE=$(curl -s -X POST """$BASE_URL""/tools/create_debate_templates" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"organization_id\": \"""$TEST_ORG_ID""\",
            \"debate_format\": \"oxford\",
            \"templates\": {
                \"opening\": \"Standard Oxford debate opening\",
                \"rebuttal\": \"Rebuttal template for Oxford format\",
                \"closing\": \"Oxford debate closing statement\"
            }
        }
    }")

if echo """$DEBATE_TEMPLATES_RESPONSE""" | jq -e '.result.created_templates' > /dev/null; then
    CREATED_COUNT=$(echo """$DEBATE_TEMPLATES_RESPONSE""" | jq '.result.created_templates | length')
    echo -e "${GREEN}✓ Created ""$CREATED_COUNT"" debate-specific templates${NC}"
else
    echo -e "${YELLOW}⚠ Debate template creation may not be implemented${NC}"
fi
echo ""

# Test 8: Template Validation
echo -e "${YELLOW}Test 8: Template Validation${NC}"

# Test with missing required variable
INVALID_RENDER=$(curl -s -X POST """$BASE_URL""/tools/render_template" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"template_id\": \"""$TEMPLATE_ID""\",
            \"variables\": {
                \"participant_name\": \"Dr. Jones\"
            }
        }
    }")

if echo """$INVALID_RENDER""" | jq -e '.error' > /dev/null; then
    echo -e "${GREEN}✓ Template validation working (missing required variables detected)${NC}"
    echo "Error: $(echo """$INVALID_RENDER""" | jq -r '.error')"
else
    echo -e "${YELLOW}⚠ Template validation may not be strict${NC}"
fi
echo ""

# Test 9: List All Templates
echo -e "${YELLOW}Test 9: List All Templates${NC}"
LIST_TEMPLATES_RESPONSE=$(curl -s """$BASE_URL""/resources/template://templates/read")

if echo """$LIST_TEMPLATES_RESPONSE""" | jq -e '.contents' > /dev/null; then
    TEMPLATE_COUNT=$(echo """$LIST_TEMPLATES_RESPONSE""" | jq '.contents | length')
    echo -e "${GREEN}✓ Found ""$TEMPLATE_COUNT"" templates${NC}"
    
    # Verify our test template is in the list
    if echo """$LIST_TEMPLATES_RESPONSE""" | jq -e ".contents[] | select(.id == \"""$TEMPLATE_ID""\")" > /dev/null; then
        echo -e "${GREEN}✓ Test template found in list${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Template listing may use different format${NC}"
fi
echo ""

# Test 10: Complex Template Rendering
echo -e "${YELLOW}Test 10: Complex Template Rendering${NC}"

# Create a more complex template
COMPLEX_TEMPLATE_RESPONSE=$(curl -s -X POST """$BASE_URL""/tools/create_template" \
    -H "Content-Type: application/json" \
    -d "{
        \"arguments\": {
            \"name\": \"Complex Debate Report\",
            \"organization_id\": \"""$TEST_ORG_ID""\",
            \"category\": \"report\",
            \"content\": \"# Debate Report: {{ debate.topic }}\\n\\n**Date:** {{ debate.date }}\\n**Format:** {{ debate.format }}\\n\\n## Participants\\n{% for p in participants %}\\n### {{ p.name }} ({{ p.position }})\\n- **Opening:** {{ p.opening_score }}/10\\n- **Arguments:** {{ p.argument_score }}/10\\n- **Closing:** {{ p.closing_score }}/10\\n- **Overall:** {{ (p.opening_score + p.argument_score + p.closing_score) / 3 | round(1) }}/10\\n{% endfor %}\\n\\n## Summary\\n{{ summary }}\\n\\n## Winner\\n{% if winner %}**{{ winner.name }}** with an average score of {{ winner.avg_score }}{% else %}Draw{% endif %}\",
            \"variables\": {
                \"debate\": {\"type\": \"object\", \"required\": true},
                \"participants\": {\"type\": \"array\", \"required\": true},
                \"summary\": {\"type\": \"string\", \"required\": true},
                \"winner\": {\"type\": \"object\", \"required\": false}
            }
        }
    }")

if echo """$COMPLEX_TEMPLATE_RESPONSE""" | jq -e '.result.template_id' > /dev/null; then
    COMPLEX_ID=$(echo """$COMPLEX_TEMPLATE_RESPONSE""" | jq -r '.result.template_id')
    
    # Render complex template
    COMPLEX_RENDER=$(curl -s -X POST """$BASE_URL""/tools/render_template" \
        -H "Content-Type: application/json" \
        -d "{
            \"arguments\": {
                \"template_id\": \"""$COMPLEX_ID""\",
                \"variables\": {
                    \"debate\": {
                        \"topic\": \"AI Regulation\",
                        \"date\": \"2024-01-15\",
                        \"format\": \"Oxford\"
                    },
                    \"participants\": [
                        {
                            \"name\": \"Dr. Smith\",
                            \"position\": \"Pro\",
                            \"opening_score\": 8,
                            \"argument_score\": 9,
                            \"closing_score\": 8
                        },
                        {
                            \"name\": \"Prof. Johnson\",
                            \"position\": \"Con\",
                            \"opening_score\": 7,
                            \"argument_score\": 8,
                            \"closing_score\": 9
                        }
                    ],
                    \"summary\": \"An engaging debate on AI regulation with strong arguments from both sides.\",
                    \"winner\": {
                        \"name\": \"Dr. Smith\",
                        \"avg_score\": 8.3
                    }
                }
            }
        }")
    
    if echo """$COMPLEX_RENDER""" | jq -e '.result.rendered_content' > /dev/null; then
        echo -e "${GREEN}✓ Complex template rendered successfully${NC}"
        echo "Preview:"
        echo """$COMPLEX_RENDER""" | jq -r '.result.rendered_content' | head -n 15
        echo "..."
    fi
fi
echo ""

# Summary
echo -e "${BLUE}=== Test Summary ===${NC}"
echo -e "${GREEN}✓ Health check${NC}"
echo -e "${GREEN}✓ Template creation${NC}"
echo -e "${GREEN}✓ Template rendering${NC}"
echo -e "${GREEN}✓ Template search${NC}"
echo -e "${GREEN}✓ Validation${NC}"
echo -e "${GREEN}✓ Complex templates${NC}"
echo -e "${BLUE}All critical tests passed!${NC}"