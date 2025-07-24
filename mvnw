#!/bin/bash

# Cross-platform Maven wrapper script
set -e

# Function to find Maven installation
find_maven() {
    # Check if mvn is already in PATH (CI environments usually have this)
    if command -v mvn >/dev/null 2>&1; then
        echo "$(command -v mvn)"
        return 0
    fi
    
    # Try common Maven installation paths
    local maven_paths=(
        "/opt/homebrew/Cellar/maven/*/libexec/bin/mvn"  # macOS Homebrew
        "/usr/local/Cellar/maven/*/libexec/bin/mvn"     # macOS Homebrew (Intel)
        "/usr/share/maven/bin/mvn"                      # Ubuntu/Debian
        "/opt/maven/bin/mvn"                            # Generic Linux
        "/usr/local/maven/bin/mvn"                      # Generic Linux
    )
    
    for path in "${maven_paths[@]}"; do
        # Handle glob patterns
        local expanded_paths=(${path})
        for expanded_path in "${expanded_paths[@]}"; do
            if [[ -x "$expanded_path" ]]; then
                echo "$expanded_path"
                return 0
            fi
        done
    done
    
    echo "Error: Maven not found. Please install Maven or add it to PATH." >&2
    exit 1
}

# Function to find Java installation  
find_java() {
    # Use JAVA_HOME if set and valid
    if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then
        return 0
    fi
    
    # Check if java is in PATH
    if command -v java >/dev/null 2>&1; then
        # Try to find JAVA_HOME from java command
        local java_path=$(command -v java)
        local java_home=$(dirname $(dirname "$java_path"))
        
        if [[ -x "$java_home/bin/java" ]]; then
            export JAVA_HOME="$java_home"
            return 0
        fi
    fi
    
    # Try common Java installation paths
    local java_paths=(
        "/Users/$(whoami)/.sdkman/candidates/java/current"  # SDKMAN on macOS
        "/home/$(whoami)/.sdkman/candidates/java/current"   # SDKMAN on Linux
        "/usr/lib/jvm/java-21-openjdk"                      # Ubuntu OpenJDK 21
        "/usr/lib/jvm/java-21-openjdk-amd64"               # Ubuntu OpenJDK 21 (AMD64)
        "/usr/lib/jvm/default-java"                        # Ubuntu default
        "/Library/Java/JavaVirtualMachines/*/Contents/Home" # macOS Oracle/OpenJDK
    )
    
    for path in "${java_paths[@]}"; do
        # Handle glob patterns
        local expanded_paths=(${path})
        for expanded_path in "${expanded_paths[@]}"; do
            if [[ -x "$expanded_path/bin/java" ]]; then
                export JAVA_HOME="$expanded_path"
                return 0
            fi
        done
    done
    
    echo "Warning: JAVA_HOME not set and Java installation not found in common locations." >&2
    echo "Using java from PATH if available." >&2
}

# Main execution
main() {
    echo "Maven Wrapper - Cross-platform script"
    
    # Find and set Java
    find_java
    if [[ -n "$JAVA_HOME" ]]; then
        echo "Using JAVA_HOME: $JAVA_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
    
    # Find Maven
    local maven_cmd=$(find_maven)
    echo "Using Maven: $maven_cmd"
    
    # Execute Maven with all arguments
    exec "$maven_cmd" "$@"
}

# Run main function with all arguments
main "$@"