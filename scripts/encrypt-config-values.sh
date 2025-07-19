#!/bin/bash

# Script to encrypt sensitive values in configuration files
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_REPO="$PROJECT_ROOT/config-repo"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if Config Server is running
check_config_server() {
    if curl -s -f http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}Config Server is running${NC}"
        return 0
    else
        echo -e "${RED}Config Server is not running!${NC}"
        echo "Please start Config Server first:"
        echo "  cd infrastructure/docker-compose"
        echo "  docker-compose up -d mcp-config-server"
        exit 1
    fi
}

# Function to encrypt a value
encrypt_value() {
    local value=$1
    local encrypted=$(curl -s -X POST http://localhost:8888/encrypt -d "$value")
    echo "$encrypted"
}

# Function to process configuration file
process_config_file() {
    local file=$1
    local temp_file=$(mktemp)
    local changes_made=false
    
    echo -e "${GREEN}Processing $file...${NC}"
    
    # Patterns to identify sensitive values
    local patterns=(
        # Database passwords
        "password: *[^{].*[^}]$"
        # API keys
        "api-key: *[^{].*[^}]$"
        "apikey: *[^{].*[^}]$"
        # Secrets
        "secret: *[^{].*[^}]$"
        # Tokens
        "token: *[^{].*[^}]$"
    )
    
    # Read file line by line
    while IFS= read -r line; do
        local encrypted_line="$line"
        
        # Skip if already encrypted or using environment variable
        if [[ "$line" =~ \{cipher\} ]] || [[ "$line" =~ \$\{ ]]; then
            echo "$line" >> "$temp_file"
            continue
        fi
        
        # Check each pattern
        for pattern in "${patterns[@]}"; do
            if [[ "$line" =~ $pattern ]]; then
                # Extract key and value
                local key=$(echo "$line" | cut -d':' -f1)
                local value=$(echo "$line" | cut -d':' -f2- | xargs)
                
                # Skip if value is a placeholder
                if [[ "$value" =~ ^(password|secret|change-me|example|placeholder|test)$ ]]; then
                    echo "$line" >> "$temp_file"
                    continue
                fi
                
                # Skip environment variable references
                if [[ "$value" =~ ^\$\{ ]]; then
                    echo "$line" >> "$temp_file"
                    continue
                fi
                
                # Encrypt the value
                echo -e "${YELLOW}  Encrypting: $key${NC}"
                local encrypted=$(encrypt_value "$value")
                encrypted_line="$key: {cipher}$encrypted"
                changes_made=true
                break
            fi
        done
        
        echo "$encrypted_line" >> "$temp_file"
    done < "$file"
    
    # Replace original file if changes were made
    if [ "$changes_made" = true ]; then
        mv "$temp_file" "$file"
        echo -e "${GREEN}  Updated $file with encrypted values${NC}"
    else
        rm "$temp_file"
        echo -e "${YELLOW}  No unencrypted sensitive values found${NC}"
    fi
}

# Function to generate secure passwords
generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-24
}

# Function to update development passwords
update_dev_passwords() {
    echo -e "${GREEN}Generating secure passwords for development environment...${NC}"
    
    # Generate passwords
    local db_pass=$(generate_password)
    local redis_pass=$(generate_password)
    local jwt_secret=$(openssl rand -hex 32)
    
    # Encrypt passwords
    local db_pass_encrypted=$(encrypt_value "$db_pass")
    local redis_pass_encrypted=$(encrypt_value "$redis_pass")
    local jwt_secret_encrypted=$(encrypt_value "$jwt_secret")
    
    # Update development files
    for file in "$CONFIG_REPO"/*-development.yml; do
        if [ -f "$file" ]; then
            # Update database password
            sed -i.bak "s|password: *{cipher}encrypted_dev_password|password: {cipher}$db_pass_encrypted|g" "$file"
            
            # Add JWT secret if it's an auth-related service
            if [[ "$file" =~ (auth|gateway|organization) ]]; then
                if ! grep -q "jwt:" "$file"; then
                    echo "" >> "$file"
                    echo "# JWT Configuration" >> "$file"
                    echo "jwt:" >> "$file"
                    echo "  secret: {cipher}$jwt_secret_encrypted" >> "$file"
                fi
            fi
            
            rm -f "$file.bak"
        fi
    done
    
    # Save passwords for reference (in production, use a secure vault)
    cat > "$CONFIG_REPO/.dev-passwords.txt" <<EOF
# Development Passwords (DELETE AFTER NOTING)
Database Password: $db_pass
Redis Password: $redis_pass
JWT Secret: $jwt_secret

To decrypt a value:
curl -X POST http://localhost:8888/decrypt -d '{cipher}...'
EOF
    
    chmod 600 "$CONFIG_REPO/.dev-passwords.txt"
    echo -e "${YELLOW}Development passwords saved to $CONFIG_REPO/.dev-passwords.txt${NC}"
    echo -e "${RED}IMPORTANT: Note these passwords and delete the file!${NC}"
}

# Main script
echo -e "${GREEN}Configuration Encryption Tool${NC}"
echo ""

# Check Config Server
check_config_server

# Process all configuration files
echo -e "${GREEN}Scanning configuration files for sensitive values...${NC}"
echo ""

for file in "$CONFIG_REPO"/*.yml; do
    # Skip shared directory
    if [[ "$file" =~ /shared/ ]]; then
        continue
    fi
    
    # Process the file
    process_config_file "$file"
done

# Ask about generating development passwords
echo ""
read -p "Generate and encrypt secure passwords for development environment? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    update_dev_passwords
fi

echo ""
echo -e "${GREEN}Encryption complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Review the encrypted values in config files"
echo "2. Commit changes to Git repository"
echo "3. Test services with encrypted configuration"
echo ""
echo "To manually encrypt a value:"
echo "  curl -X POST http://localhost:8888/encrypt -d 'your-value'"
echo ""
echo "To decrypt a value (for verification):"
echo "  curl -X POST http://localhost:8888/decrypt -d '{cipher}...'"