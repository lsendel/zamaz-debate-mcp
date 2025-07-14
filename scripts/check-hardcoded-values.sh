#!/bin/bash

echo "Checking for hardcoded values in configuration files..."
echo ""

# Check for hardcoded ports
echo "=== Checking for hardcoded ports ==="
grep -n "port: [0-9]" mcp-*/src/main/resources/application.yml || echo "✅ No hardcoded ports found"
echo ""

# Check for hardcoded URLs
echo "=== Checking for hardcoded URLs ==="
grep -n "url: http" mcp-*/src/main/resources/application.yml | grep -v '$' || echo "✅ No hardcoded URLs found"
echo ""

# Check for hardcoded hosts
echo "=== Checking for hardcoded hosts ==="
grep -n "host: [a-z]" mcp-*/src/main/resources/application.yml | grep -v '$' || echo "✅ No hardcoded hosts found"
echo ""

# Check docker-compose files
echo "=== Checking docker-compose files for hardcoded values ==="
grep -E "(host|port|url).*:" docker-compose*.yml | grep -v '$' | grep -v '#' || echo "✅ No hardcoded values in docker-compose files"
echo ""

# Check for exposed secrets
echo "=== Checking for exposed secrets ==="
grep -E "(api[_-]?key|password|secret)" mcp-*/src/main/resources/application.yml | grep -v '$' || echo "✅ No exposed secrets in application.yml files"
echo ""

# Summary
echo "=== Summary ==="
echo "All configuration should use environment variables with defaults"
echo "Format: \${ENV_VAR:default-value}"
echo ""
echo "To see all environment variables used:"
grep -h '\${[A-Z_]*' mcp-*/src/main/resources/application.yml docker-compose*.yml | sed 's/.*\${\([^:}]*\).*/\1/' | sort | uniq