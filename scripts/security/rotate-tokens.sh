#!/bin/bash

# Token Rotation Helper Script
# This script helps rotate tokens and update configurations

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname """"$SCRIPT_DIR"""")")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Token Rotation Helper${NC}"
echo "======================="
echo

# Function to check if .env file exists
check_env_file() {
    if [ ! -f """"$PROJECT_ROOT"""/.env" ]; then
        echo -e "${YELLOW}Warning: .env file not found${NC}"
        echo "Creating .env from .env.example..."
        cp """"$PROJECT_ROOT"""/.env.example" """"$PROJECT_ROOT"""/.env"
        echo -e "${GREEN}Created .env file. Please update with your actual values.${NC}"
        return 1
    fi
    return 0
}

# Function to update token in .env file
update_token() {
    local token_name=$1
    local new_value=$2
    
    if grep -q "^${token_name}=" """"$PROJECT_ROOT"""/.env"; then
        # Update existing token
        if [[ """"$OSTYPE"""" == "darwin"* ]]; then
            # macOS
            sed -i '' "s|^${token_name}=.*|${token_name}=${new_value}|" """"$PROJECT_ROOT"""/.env"
        else
            # Linux
            sed -i "s|^${token_name}=.*|${token_name}=${new_value}|" """"$PROJECT_ROOT"""/.env"
        fi
        echo -e "${GREEN}Updated ${token_name}${NC}"
    else
        # Add new token
        echo "${token_name}=${new_value}" >> """"$PROJECT_ROOT"""/.env"
        echo -e "${GREEN}Added ${token_name}${NC}"
    fi
}

# Main menu
main_menu() {
    echo "What would you like to do?"
    echo "1. Rotate SonarCloud token"
    echo "2. Rotate database passwords"
    echo "3. Rotate API keys"
    echo "4. Generate secure passwords"
    echo "5. Check current configuration"
    echo "6. Exit"
    echo
    read -p "Select option (1-6): " choice
    
    case """$choice""" in
        1) rotate_sonar_token ;;
        2) rotate_db_passwords ;;
        3) rotate_api_keys ;;
        4) generate_passwords ;;
        5) check_configuration ;;
        6) exit 0 ;;
        *) echo -e "${RED}Invalid option${NC}"; main_menu ;;
    esac
}

# Rotate SonarCloud token
rotate_sonar_token() {
    echo
    echo -e "${BLUE}Rotating SonarCloud Token${NC}"
    echo "------------------------"
    echo
    echo "Steps to rotate SonarCloud token:"
    echo "1. Go to https://sonarcloud.io/account/security"
    echo "2. Revoke the existing token"
    echo "3. Generate a new token with 'Execute Analysis' permission"
    echo "4. Copy the new token"
    echo
    read -p "Enter new SonarCloud token: " new_token
    
    if [ -n """"$new_token"""" ]; then
        update_token "SONAR_TOKEN" """"$new_token""""
        echo -e "${GREEN}SonarCloud token updated successfully!${NC}"
        
        # Update GitHub secrets reminder
        echo
        echo -e "${YELLOW}Don't forget to update GitHub Secrets:${NC}"
        echo "1. Go to your repository settings"
        echo "2. Navigate to Secrets and variables > Actions"
        echo "3. Update SONAR_TOKEN with the new value"
    fi
    
    echo
    read -p "Press Enter to continue..."
    main_menu
}

# Rotate database passwords
rotate_db_passwords() {
    echo
    echo -e "${BLUE}Rotating Database Passwords${NC}"
    echo "--------------------------"
    echo
    
    # Generate secure password
    new_password=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    
    echo "Generated new password: ${new_password}"
    echo
    read -p "Use this password? (y/n): " confirm
    
    if [[ """"$confirm"""" == "y" ]]; then
        update_token "POSTGRES_PASSWORD" """"$new_password""""
        update_token "DB_PASSWORD" """"$new_password""""
        echo -e "${GREEN}Database passwords updated!${NC}"
        echo
        echo -e "${YELLOW}Important: You need to update the database with the new password${NC}"
        echo "Run: docker-compose down && docker-compose up -d"
    else
        read -p "Enter custom password: " custom_password
        update_token "POSTGRES_PASSWORD" """"$custom_password""""
        update_token "DB_PASSWORD" """"$custom_password""""
    fi
    
    echo
    read -p "Press Enter to continue..."
    main_menu
}

# Rotate API keys
rotate_api_keys() {
    echo
    echo -e "${BLUE}Rotating API Keys${NC}"
    echo "-----------------"
    echo
    echo "Select API key to rotate:"
    echo "1. OpenAI API Key"
    echo "2. Claude API Key"
    echo "3. Gemini API Key"
    echo "4. GitHub Token"
    echo "5. Back to main menu"
    echo
    read -p "Select option (1-5): " api_choice
    
    case """$api_choice""" in
        1) 
            read -p "Enter new OpenAI API key: " key
            update_token "OPENAI_API_KEY" """"$key""""
            ;;
        2) 
            read -p "Enter new Claude API key: " key
            update_token "CLAUDE_API_KEY" """"$key""""
            ;;
        3) 
            read -p "Enter new Gemini API key: " key
            update_token "GEMINI_API_KEY" """"$key""""
            ;;
        4) 
            read -p "Enter new GitHub token: " key
            update_token "GITHUB_TOKEN" """"$key""""
            ;;
        5) main_menu ;;
        *) echo -e "${RED}Invalid option${NC}" ;;
    esac
    
    echo
    read -p "Press Enter to continue..."
    main_menu
}

# Generate secure passwords
generate_passwords() {
    echo
    echo -e "${BLUE}Generate Secure Passwords${NC}"
    echo "------------------------"
    echo
    echo "Generated passwords:"
    echo
    echo "Database password: $(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)"
    echo "JWT Secret: $(openssl rand -base64 64 | tr -d "=+/")"
    echo "Session Secret: $(openssl rand -base64 48 | tr -d "=+/")"
    echo "Grafana Admin: $(openssl rand -base64 24 | tr -d "=+/" | cut -c1-20)"
    echo
    read -p "Press Enter to continue..."
    main_menu
}

# Check current configuration
check_configuration() {
    echo
    echo -e "${BLUE}Current Configuration Check${NC}"
    echo "--------------------------"
    echo
    
    if [ -f """"$PROJECT_ROOT"""/.env" ]; then
        echo "Checking for missing values in .env:"
        echo
        
        # Check for placeholder values
        if grep -q "your_.*_here\|demo-key" """"$PROJECT_ROOT"""/.env"; then
            echo -e "${RED}Found placeholder values:${NC}"
            grep -n "your_.*_here\|demo-key" """"$PROJECT_ROOT"""/.env" | sed 's/:/ -> /'
        else
            echo -e "${GREEN}No placeholder values found${NC}"
        fi
        
        echo
        echo "Checking for empty values:"
        if grep -E "^[A-Z_]+=\s*$" """"$PROJECT_ROOT"""/.env"; then
            echo -e "${RED}Found empty values:${NC}"
            grep -n -E "^[A-Z_]+=\s*$" """"$PROJECT_ROOT"""/.env" | sed 's/:/ -> /'
        else
            echo -e "${GREEN}No empty values found${NC}"
        fi
    else
        echo -e "${RED}.env file not found!${NC}"
    fi
    
    echo
    read -p "Press Enter to continue..."
    main_menu
}

# Check if .env exists
if ! check_env_file; then
    echo
    echo -e "${YELLOW}Please update the .env file with your actual values${NC}"
    echo
fi

# Start main menu
main_menu