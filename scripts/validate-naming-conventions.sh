#!/bin/bash

# Naming Convention Validation Script
# This script validates that Java files follow the established naming conventions
# and checks database naming patterns in migration files

echo "🔍 Validating Naming Conventions..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Temporary files for counting
VIOLATIONS_FILE=$(mktemp)
WARNINGS_FILE=$(mktemp)

# Cleanup function
cleanup() {
    rm -f "$VIOLATIONS_FILE" "$WARNINGS_FILE"
}
trap cleanup EXIT

# Function to check class naming patterns
check_class_naming() {
    local file="$1"
    local class_name=$(basename "$file" .java)
    
    # Check Controller classes
    if [[ "$file" == *"/controller/"* ]]; then
        if [[ ! "$class_name" =~ Controller$ ]]; then
            echo -e "${RED}❌ Controller class '$class_name' should end with 'Controller'${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
    fi
    
    # Check Service classes (allow ServiceImpl suffix)
    if [[ "$file" == *"/service/"* ]]; then
        if [[ ! "$class_name" =~ (Service|ServiceImpl)$ ]]; then
            echo -e "${RED}❌ Service class '$class_name' should end with 'Service' or 'ServiceImpl'${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
    fi
    
    # Check Repository interfaces
    if [[ "$file" == *"/repository/"* ]]; then
        if [[ ! "$class_name" =~ Repository$ ]]; then
            echo -e "${RED}❌ Repository interface '$class_name' should end with 'Repository'${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
    fi
    
    # Check Exception classes
    if [[ "$file" == *"/exception/"* ]]; then
        if [[ ! "$class_name" =~ Exception$ ]]; then
            echo -e "${RED}❌ Exception class '$class_name' should end with 'Exception'${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
    fi
    
    # Check Config classes
    if [[ "$file" == *"/config/"* ]]; then
        if [[ ! "$class_name" =~ Config$ ]]; then
            echo -e "${RED}❌ Configuration class '$class_name' should end with 'Config'${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
    fi
    
    # Check DTO classes (allow Request/Response suffixes as alternatives)
    if [[ "$file" == *"/dto/"* ]]; then
        if [[ ! "$class_name" =~ (Dto|Request|Response)$ ]]; then
            echo -e "${RED}❌ DTO class '$class_name' should end with 'Dto', 'Request', or 'Response'${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
    fi
}

# Function to check method naming patterns
check_method_naming() {
    local file="$1"
    
    # Check for CRUD method patterns
    while IFS= read -r line; do
        # Check for create methods
        if [[ "$line" =~ public.*[[:space:]](create|add|register)[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper create method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for read methods
        if [[ "$line" =~ public.*[[:space:]](get|find|list|search)[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper read method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for update methods
        if [[ "$line" =~ public.*[[:space:]](update|modify|change)[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper update method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for delete methods
        if [[ "$line" =~ public.*[[:space:]](delete|remove|deactivate)[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper delete method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for boolean methods
        if [[ "$line" =~ public.*boolean[[:space:]](is|has|can|should|will)[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper boolean method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for validation methods
        if [[ "$line" =~ private.*[[:space:]]validate[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper validation method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for mapping methods
        if [[ "$line" =~ private.*[[:space:]](map|to|from)[A-Z] ]]; then
            echo -e "${GREEN}✅ Found proper mapping method pattern in $(basename "$file")${NC}"
        fi
        
        # Check for camelCase violations
        if [[ "$line" =~ public.*[[:space:]][a-z][a-zA-Z0-9]*_[a-zA-Z] ]]; then
            echo -e "${RED}❌ Method name with underscore found in $(basename "$file"): $line${NC}"
            echo "1" >> "$VIOLATIONS_FILE"
        fi
        
    done < "$file"
}

# Function to check database naming conventions in migration files
check_database_naming() {
    local file="$1"
    
    echo "🗄️  Checking database naming in: $(basename "$file")"
    
    # Check for table names (should be snake_case and plural)
    while IFS= read -r line; do
        # Check CREATE TABLE statements
        if [[ "$line" =~ CREATE[[:space:]]+TABLE[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*) ]]; then
            table_name="${BASH_REMATCH[1]}"
            if [[ ! "$table_name" =~ ^[a-z][a-z0-9_]*$ ]]; then
                echo -e "${RED}❌ Table name '$table_name' should use snake_case${NC}"
                echo "1" >> "$VIOLATIONS_FILE"
            elif [[ ! "$table_name" =~ s$ ]] && [[ ! "$table_name" =~ _audit$ ]] && [[ ! "$table_name" =~ _history$ ]]; then
                echo -e "${YELLOW}⚠️  Table name '$table_name' should be plural${NC}"
                echo "1" >> "$WARNINGS_FILE"
            else
                echo -e "${GREEN}✅ Table name '$table_name' follows conventions${NC}"
            fi
        fi
        
        # Check column names (should be snake_case)
        if [[ "$line" =~ [[:space:]]([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]+(VARCHAR|INTEGER|BOOLEAN|TIMESTAMP|UUID|TEXT|DECIMAL) ]]; then
            column_name="${BASH_REMATCH[1]}"
            if [[ ! "$column_name" =~ ^[a-z][a-z0-9_]*$ ]]; then
                echo -e "${RED}❌ Column name '$column_name' should use snake_case${NC}"
                echo "1" >> "$VIOLATIONS_FILE"
            else
                echo -e "${GREEN}✅ Column name '$column_name' follows conventions${NC}"
            fi
        fi
        
        # Check foreign key naming
        if [[ "$line" =~ REFERENCES[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*) ]]; then
            referenced_table="${BASH_REMATCH[1]}"
            if [[ "$line" =~ ([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]+UUID.*REFERENCES ]]; then
                fk_column="${BASH_REMATCH[1]}"
                expected_fk="${referenced_table%s}_id"  # Remove 's' and add '_id'
                if [[ "$fk_column" != "$expected_fk" ]] && [[ "$fk_column" != "id" ]]; then
                    echo -e "${YELLOW}⚠️  Foreign key '$fk_column' should be '${expected_fk}'${NC}"
                    echo "1" >> "$WARNINGS_FILE"
                fi
            fi
        fi
        
    done < "$file"
}

# Main validation function
validate_file() {
    local file="$1"
    
    echo "📁 Checking: $(basename "$file")"
    
    check_class_naming "$file"
    check_method_naming "$file"
}

# Find and validate Java files
echo -e "${BLUE}🔍 Scanning Java files in mcp-organization service...${NC}"

find mcp-organization/src/main/java -name "*.java" -type f | while read -r file; do
    validate_file "$file"
done

# Find and validate database migration files
echo -e "${BLUE}🗄️  Scanning database migration files...${NC}"

find . -path "*/db/migration/*.sql" -type f | while read -r file; do
    check_database_naming "$file"
done

# Count violations and warnings
TOTAL_VIOLATIONS=$(wc -l < "$VIOLATIONS_FILE" 2>/dev/null || echo "0")
TOTAL_WARNINGS=$(wc -l < "$WARNINGS_FILE" 2>/dev/null || echo "0")

echo ""
echo "📊 Validation Summary:"
echo -e "   ${RED}Violations: $TOTAL_VIOLATIONS${NC}"
echo -e "   ${YELLOW}Warnings: $TOTAL_WARNINGS${NC}"

if [ "$TOTAL_VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✅ All naming conventions are properly followed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Found $TOTAL_VIOLATIONS naming convention violations${NC}"
    exit 1
fi