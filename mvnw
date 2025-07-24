#!/bin/bash

# Robust Maven wrapper script for CI/CD environments
set -e

# Auto-detect Java
if [ -z "$JAVA_HOME" ]; then
    # Try common Java locations
    if [ -d "/usr/lib/jvm/temurin-21-jdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk-amd64"
    elif [ -d "/usr/lib/jvm/temurin-17-jdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/temurin-17-jdk-amd64"
    elif [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
fi

# Verify Java is available
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "Error: Java not found. Please install Java or set JAVA_HOME" >&2
    exit 1
fi

# Try to find Maven
if [ -n "$MAVEN_HOME" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
    MAVEN_CMD="$MAVEN_HOME/bin/mvn"
elif command -v mvn >/dev/null 2>&1; then
    MAVEN_CMD="mvn"
elif [ -f "/usr/share/maven/bin/mvn" ]; then
    MAVEN_CMD="/usr/share/maven/bin/mvn"
elif [ -f "/opt/maven/bin/mvn" ]; then
    MAVEN_CMD="/opt/maven/bin/mvn"
else
    echo "Error: Maven not found. Installing Maven..." >&2
    # Download and install Maven if not found
    MAVEN_VERSION="3.9.6"
    MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    
    # Create temp directory
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    # Download and extract Maven
    curl -fsSL "$MAVEN_URL" | tar -xz
    
    # Set up Maven
    export MAVEN_HOME="$TEMP_DIR/apache-maven-${MAVEN_VERSION}"
    MAVEN_CMD="$MAVEN_HOME/bin/mvn"
    
    if [ ! -x "$MAVEN_CMD" ]; then
        echo "Failed to install Maven" >&2
        exit 1
    fi
fi

# Execute Maven with all arguments
exec "$MAVEN_CMD" "$@"