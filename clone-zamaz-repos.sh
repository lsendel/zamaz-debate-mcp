#!/bin/bash

# Script to clone zamaz-mcp repositories
# Run this script from /Users/lsendel/IdeaProjects directory

echo "Cloning zamaz-mcp repositories..."

# Clone zamaz-mcp-gateway
echo "Cloning zamaz-mcp-gateway..."
git clone https://github.com/lsendel/zamaz-mcp-gateway.git

# Clone zamaz-mcp-security
echo "Cloning zamaz-mcp-security..."
git clone https://github.com/lsendel/zamaz-mcp-security.git

# Clone zamaz-mcp-organization
echo "Cloning zamaz-mcp-organization..."
git clone https://github.com/lsendel/zamaz-mcp-organization.git

# Clone zamaz-mcp-llm
echo "Cloning zamaz-mcp-llm..."
git clone https://github.com/lsendel/zamaz-mcp-llm.git

echo "All repositories cloned successfully!"
echo "You should now have:"
echo "  - /Users/lsendel/IdeaProjects/zamaz-mcp-gateway"
echo "  - /Users/lsendel/IdeaProjects/zamaz-mcp-security"
echo "  - /Users/lsendel/IdeaProjects/zamaz-mcp-organization"
echo "  - /Users/lsendel/IdeaProjects/zamaz-mcp-llm"