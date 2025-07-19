#!/bin/bash

# Script to compare configurations across environments
# Usage: ./diff-environments.sh <env1> <env2>

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check arguments
if [ $# -ne 2 ]; then
    echo "Usage: $0 <env1> <env2>"
    echo "Example: $0 dev prod"
    echo "Available environments: dev, staging, prod"
    exit 1
fi

ENV1="$1"
ENV2="$2"

echo "Environment Configuration Comparison"
echo "==================================="
echo "Comparing: ${BLUE}$ENV1${NC} vs ${BLUE}$ENV2${NC}"
echo

# Check if environment files exist
if [ ! -f "$SCRIPT_DIR/application-$ENV1.yml" ]; then
    echo -e "${RED}Error: application-$ENV1.yml not found${NC}"
    exit 1
fi

if [ ! -f "$SCRIPT_DIR/application-$ENV2.yml" ]; then
    echo -e "${RED}Error: application-$ENV2.yml not found${NC}"
    exit 1
fi

# Function to extract and sort properties
extract_properties() {
    local file="$1"
    # Convert YAML to property format and sort
    grep -v "^#" "$file" | grep -v "^$" | sed 's/^  */ /' | sort
}

# Compare global environment files
echo "1. Comparing global environment configurations..."
echo "   File: application-$ENV1.yml vs application-$ENV2.yml"
echo

TEMP1=$(mktemp)
TEMP2=$(mktemp)

extract_properties "$SCRIPT_DIR/application-$ENV1.yml" > "$TEMP1"
extract_properties "$SCRIPT_DIR/application-$ENV2.yml" > "$TEMP2"

if diff -u "$TEMP1" "$TEMP2" > /dev/null; then
    echo -e "   ${GREEN}✓ No differences found${NC}"
else
    echo -e "   ${YELLOW}! Differences found:${NC}"
    diff -u "$TEMP1" "$TEMP2" | grep -E "^[+-]" | grep -v "^[+-]{3}" | while read -r line; do
        if [[ $line == +* ]]; then
            echo -e "   ${GREEN}$line${NC}"
        elif [[ $line == -* ]]; then
            echo -e "   ${RED}$line${NC}"
        fi
    done
fi

echo

# Compare service-specific configurations
echo "2. Comparing service-specific configurations..."

for service in mcp-organization mcp-llm mcp-controller mcp-rag mcp-template mcp-context; do
    FILE1="$SCRIPT_DIR/$service-$ENV1.yml"
    FILE2="$SCRIPT_DIR/$service-$ENV2.yml"
    
    # Skip if neither file exists
    if [ ! -f "$FILE1" ] && [ ! -f "$FILE2" ]; then
        continue
    fi
    
    echo "   Service: $service"
    
    if [ ! -f "$FILE1" ]; then
        echo -e "   ${YELLOW}! No $ENV1 configuration${NC}"
    elif [ ! -f "$FILE2" ]; then
        echo -e "   ${YELLOW}! No $ENV2 configuration${NC}"
    else
        extract_properties "$FILE1" > "$TEMP1"
        extract_properties "$FILE2" > "$TEMP2"
        
        if diff -u "$TEMP1" "$TEMP2" > /dev/null; then
            echo -e "   ${GREEN}✓ No differences${NC}"
        else
            echo -e "   ${YELLOW}! Differences found${NC}"
            # Show a summary of differences
            ADDED=$(diff "$TEMP1" "$TEMP2" | grep "^>" | wc -l)
            REMOVED=$(diff "$TEMP1" "$TEMP2" | grep "^<" | wc -l)
            echo "     Added in $ENV2: $ADDED properties"
            echo "     Removed from $ENV1: $REMOVED properties"
        fi
    fi
    echo
done

# Security comparison
echo "3. Security configuration comparison..."

# Check for encryption usage
ENCRYPTED1=$(grep -r "{cipher}" "$SCRIPT_DIR" --include="*-$ENV1.yml" | wc -l)
ENCRYPTED2=$(grep -r "{cipher}" "$SCRIPT_DIR" --include="*-$ENV2.yml" | wc -l)

echo "   Encrypted values in $ENV1: $ENCRYPTED1"
echo "   Encrypted values in $ENV2: $ENCRYPTED2"

if [ "$ENV2" = "prod" ] && [ "$ENCRYPTED2" -lt "$ENCRYPTED1" ]; then
    echo -e "   ${YELLOW}! Warning: Production has fewer encrypted values than $ENV1${NC}"
fi

echo

# Environment variable usage comparison
echo "4. Environment variable usage comparison..."

ENVVARS1=$(grep -rE '\$\{[A-Z_]+(:.*)?}' "$SCRIPT_DIR" --include="*-$ENV1.yml" | wc -l)
ENVVARS2=$(grep -rE '\$\{[A-Z_]+(:.*)?}' "$SCRIPT_DIR" --include="*-$ENV2.yml" | wc -l)

echo "   Environment variables in $ENV1: $ENVVARS1"
echo "   Environment variables in $ENV2: $ENVVARS2"

echo

# Resource allocation comparison
echo "5. Resource allocation comparison..."

# Extract and compare pool sizes, memory limits, etc.
for property in "max-pool-size" "maximum-pool-size" "max-threads" "connection-timeout"; do
    echo "   Property: $property"
    
    VALUE1=$(grep -h "$property:" "$SCRIPT_DIR"/*-$ENV1.yml 2>/dev/null | awk '{print $2}' | sort -u)
    VALUE2=$(grep -h "$property:" "$SCRIPT_DIR"/*-$ENV2.yml 2>/dev/null | awk '{print $2}' | sort -u)
    
    if [ -n "$VALUE1" ] || [ -n "$VALUE2" ]; then
        echo "     $ENV1: ${VALUE1:-not set}"
        echo "     $ENV2: ${VALUE2:-not set}"
    fi
done

# Cleanup
rm -f "$TEMP1" "$TEMP2"

echo
echo "==================================="
echo -e "${GREEN}Comparison complete${NC}"

# Summary recommendations
echo
echo "Recommendations:"

if [ "$ENV2" = "prod" ]; then
    echo "- Ensure all sensitive values in production are encrypted"
    echo "- Verify resource allocations are appropriate for production load"
    echo "- Check that debug/development features are disabled"
fi

if [ "$ENCRYPTED2" -lt "$ENCRYPTED1" ]; then
    echo "- Consider encrypting more values in $ENV2"
fi

echo "- Review all differences to ensure they are intentional"