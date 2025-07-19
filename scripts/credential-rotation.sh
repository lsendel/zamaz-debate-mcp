#!/bin/bash

# Credential rotation script for MCP services
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration
ENVIRONMENT="${1:-development}"
ROTATION_TYPE="${2:-all}"  # all, database, jwt, api-keys
DRY_RUN="${DRY_RUN:-false}"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -t|--type)
            ROTATION_TYPE="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN="true"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -e, --environment    Environment (development, staging, production)"
            echo "  -t, --type          Rotation type (all, database, jwt, api-keys)"
            echo "  -d, --dry-run       Show what would be rotated without making changes"
            echo "  -h, --help          Show this help message"
            exit 0
            ;;
    esac
done

echo -e "${GREEN}Credential Rotation Tool${NC}"
echo "Environment: $ENVIRONMENT"
echo "Rotation Type: $ROTATION_TYPE"
echo "Dry Run: $DRY_RUN"
echo ""

# Function to generate secure password
generate_password() {
    local length=${1:-32}
    openssl rand -base64 $length | tr -d "=+/" | cut -c1-$length
}

# Function to generate secure key
generate_key() {
    local length=${1:-64}
    openssl rand -hex $length
}

# Function to encrypt value
encrypt_value() {
    local value=$1
    local key=${CONFIG_ENCRYPTION_KEY:-$(cat "$PROJECT_ROOT/.encryption-key" 2>/dev/null || echo "")}
    
    if [ -z "$key" ]; then
        echo -e "${RED}Error: Encryption key not found${NC}"
        exit 1
    fi
    
    # Call the encrypt-value.sh script
    "$PROJECT_ROOT/config-repo/encrypt-value.sh" "$value"
}

# Function to update configuration file
update_config() {
    local file=$1
    local key=$2
    local old_value=$3
    local new_value=$4
    
    if [ "$DRY_RUN" = "true" ]; then
        echo -e "${YELLOW}Would update $file:${NC}"
        echo "  Key: $key"
        echo "  Old: [REDACTED]"
        echo "  New: [REDACTED]"
    else
        # Backup original file
        cp "$file" "$file.backup"
        
        # Update the value
        sed -i.tmp "s|$key: $old_value|$key: $new_value|g" "$file"
        rm -f "$file.tmp"
        
        echo -e "${GREEN}Updated $file${NC}"
    fi
}

# Function to rotate database passwords
rotate_database_passwords() {
    echo -e "${GREEN}Rotating database passwords...${NC}"
    
    local config_files=(
        "$PROJECT_ROOT/config-repo/application.yml"
        "$PROJECT_ROOT/config-repo/application-$ENVIRONMENT.yml"
        "$PROJECT_ROOT/config-repo/mcp-organization.yml"
        "$PROJECT_ROOT/config-repo/mcp-controller.yml"
        "$PROJECT_ROOT/config-repo/mcp-rag.yml"
        "$PROJECT_ROOT/config-repo/mcp-template.yml"
    )
    
    for file in "${config_files[@]}"; do
        if [ -f "$file" ]; then
            # Find database password entries
            if grep -q "password: {cipher}" "$file"; then
                local new_password=$(generate_password)
                local encrypted_password=$(encrypt_value "$new_password")
                
                # Update the configuration
                update_config "$file" "password" "{cipher}.*" "$encrypted_password"
                
                # Store the new password securely
                if [ "$DRY_RUN" = "false" ]; then
                    echo "$new_password" | kubectl create secret generic db-password-$ENVIRONMENT \
                        --from-literal=password="$new_password" \
                        --dry-run=client -o yaml | kubectl apply -f -
                fi
            fi
        fi
    done
}

# Function to rotate JWT secrets
rotate_jwt_secrets() {
    echo -e "${GREEN}Rotating JWT secrets...${NC}"
    
    local new_secret=$(generate_key)
    local encrypted_secret=$(encrypt_value "$new_secret")
    
    # Update all service configurations
    local config_files=(
        "$PROJECT_ROOT/config-repo/application.yml"
        "$PROJECT_ROOT/config-repo/application-$ENVIRONMENT.yml"
    )
    
    for file in "${config_files[@]}"; do
        if [ -f "$file" ] && grep -q "jwt:" "$file"; then
            update_config "$file" "secret" "{cipher}.*" "$encrypted_secret"
        fi
    done
    
    # Update Kubernetes secret
    if [ "$DRY_RUN" = "false" ]; then
        kubectl create secret generic jwt-secret-$ENVIRONMENT \
            --from-literal=secret="$new_secret" \
            --dry-run=client -o yaml | kubectl apply -f -
    fi
}

# Function to rotate API keys
rotate_api_keys() {
    echo -e "${GREEN}Rotating API keys...${NC}"
    
    local services=("openai" "anthropic" "google")
    
    for service in "${services[@]}"; do
        echo -e "${YELLOW}Note: $service API key must be rotated through their console${NC}"
        echo "1. Log in to $service console"
        echo "2. Generate new API key"
        echo "3. Update the encrypted value in configuration"
        echo ""
    done
}

# Function to validate rotation
validate_rotation() {
    echo -e "${GREEN}Validating credential rotation...${NC}"
    
    # Run security scan
    "$PROJECT_ROOT/scripts/security-scan.sh"
    
    # Check configuration server
    if command -v curl &> /dev/null; then
        if curl -f http://localhost:8888/actuator/health >/dev/null 2>&1; then
            echo -e "${GREEN}Config Server is healthy${NC}"
        else
            echo -e "${YELLOW}Warning: Config Server is not accessible${NC}"
        fi
    fi
}

# Function to create rotation report
create_rotation_report() {
    local report_file="$PROJECT_ROOT/credential-rotation-$(date +%Y%m%d-%H%M%S).log"
    
    cat > "$report_file" <<EOF
Credential Rotation Report
========================
Date: $(date)
Environment: $ENVIRONMENT
Type: $ROTATION_TYPE
Dry Run: $DRY_RUN

Actions Taken:
EOF
    
    if [ "$ROTATION_TYPE" = "all" ] || [ "$ROTATION_TYPE" = "database" ]; then
        echo "- Database passwords rotated" >> "$report_file"
    fi
    
    if [ "$ROTATION_TYPE" = "all" ] || [ "$ROTATION_TYPE" = "jwt" ]; then
        echo "- JWT secrets rotated" >> "$report_file"
    fi
    
    if [ "$ROTATION_TYPE" = "all" ] || [ "$ROTATION_TYPE" = "api-keys" ]; then
        echo "- API key rotation instructions provided" >> "$report_file"
    fi
    
    echo "" >> "$report_file"
    echo "Next Steps:" >> "$report_file"
    echo "1. Restart affected services" >> "$report_file"
    echo "2. Verify service connectivity" >> "$report_file"
    echo "3. Update any external integrations" >> "$report_file"
    
    echo -e "${GREEN}Report saved to: $report_file${NC}"
}

# Main rotation logic
case $ROTATION_TYPE in
    all)
        rotate_database_passwords
        rotate_jwt_secrets
        rotate_api_keys
        ;;
    database)
        rotate_database_passwords
        ;;
    jwt)
        rotate_jwt_secrets
        ;;
    api-keys)
        rotate_api_keys
        ;;
    *)
        echo -e "${RED}Unknown rotation type: $ROTATION_TYPE${NC}"
        exit 1
        ;;
esac

# Validate if not dry run
if [ "$DRY_RUN" = "false" ]; then
    validate_rotation
fi

# Create report
create_rotation_report

echo ""
echo -e "${GREEN}Credential rotation completed!${NC}"

if [ "$DRY_RUN" = "true" ]; then
    echo -e "${YELLOW}This was a dry run. No changes were made.${NC}"
    echo "To perform actual rotation, run without --dry-run flag"
fi