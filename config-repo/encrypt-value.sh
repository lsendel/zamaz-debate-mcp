#!/bin/bash

# Script to encrypt configuration values
# Usage: ./encrypt-value.sh <value-to-encrypt>

set -e

# Configuration
CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-http://localhost:8888}"
CONFIG_SERVER_USER="${CONFIG_SERVER_USER:-admin}"
CONFIG_SERVER_PASS="${CONFIG_SERVER_PASS:-admin}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check arguments
if [ $# -eq 0 ]; then
    echo "Usage: $0 <value-to-encrypt>"
    echo "Example: $0 'my-secret-password'"
    exit 1
fi

VALUE_TO_ENCRYPT="$1"

echo "Configuration Value Encryption"
echo "============================="
echo

# Check if Config Server is reachable
echo "Checking Config Server availability..."
if curl -s -f -u "$CONFIG_SERVER_USER:$CONFIG_SERVER_PASS" \
    "$CONFIG_SERVER_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Config Server is available${NC}"
else
    echo -e "${RED}✗ Config Server is not available at $CONFIG_SERVER_URL${NC}"
    echo -e "${YELLOW}Make sure the Config Server is running${NC}"
    exit 1
fi

echo

# Encrypt the value
echo "Encrypting value..."
RESPONSE=$(curl -s -X POST \
    -u "$CONFIG_SERVER_USER:$CONFIG_SERVER_PASS" \
    -H "Content-Type: text/plain" \
    -d "$VALUE_TO_ENCRYPT" \
    "$CONFIG_SERVER_URL/encrypt")

if [ $? -eq 0 ] && [ -n "$RESPONSE" ]; then
    echo -e "${GREEN}✓ Encryption successful${NC}"
    echo
    echo "Original value: $VALUE_TO_ENCRYPT"
    echo "Encrypted value: {cipher}$RESPONSE"
    echo
    echo "Use this in your configuration file:"
    echo -e "${YELLOW}property: '{cipher}$RESPONSE'${NC}"
    echo
    echo "Example:"
    echo "  spring:"
    echo "    datasource:"
    echo "      password: '{cipher}$RESPONSE'"
else
    echo -e "${RED}✗ Encryption failed${NC}"
    echo "Response: $RESPONSE"
    exit 1
fi

# Test decryption
echo
echo "Testing decryption..."
DECRYPTED=$(curl -s -X POST \
    -u "$CONFIG_SERVER_USER:$CONFIG_SERVER_PASS" \
    -H "Content-Type: text/plain" \
    -d "{cipher}$RESPONSE" \
    "$CONFIG_SERVER_URL/decrypt")

if [ "$DECRYPTED" = "$VALUE_TO_ENCRYPT" ]; then
    echo -e "${GREEN}✓ Decryption test passed${NC}"
else
    echo -e "${RED}✗ Decryption test failed${NC}"
    echo "Expected: $VALUE_TO_ENCRYPT"
    echo "Got: $DECRYPTED"
    exit 1
fi

echo
echo "============================="
echo -e "${GREEN}✓ Encryption completed successfully${NC}"