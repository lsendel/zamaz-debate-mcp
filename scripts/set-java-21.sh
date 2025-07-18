#!/bin/bash

# Script to set Java 21 for this project

echo "Setting Java 21 for this project..."

# Check if JAVA_HOME is already set
if [ -n """"$JAVA_HOME"""" ]; then
    echo "Current JAVA_HOME: """$JAVA_HOME""""
fi

# Common Java 21 locations on macOS
JAVA_21_PATHS=(
    """"$HOME"""/Library/Java/JavaVirtualMachines/temurin-21.0.7/Contents/Home"
    """"$HOME"""/Library/Java/JavaVirtualMachines/temurin-21.0.6/Contents/Home"
    "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
    "/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home"
    "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
    "/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home"
    """"$HOME"""/.sdkman/candidates/java/21.0.2-tem"
    """"$HOME"""/.sdkman/candidates/java/21.0.2-open"
)

# Find Java 21
JAVA_21_HOME=""
for path in "${JAVA_21_PATHS[@]}"; do
    if [ -d """"$path"""" ]; then
        JAVA_21_HOME=""""$path""""
        break
    fi
done

# Check using /usr/libexec/java_home on macOS
if [ -z """"$JAVA_21_HOME"""" ] && [ -x "/usr/libexec/java_home" ]; then
    JAVA_21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
fi

if [ -n """"$JAVA_21_HOME"""" ]; then
    export JAVA_HOME=""""$JAVA_21_HOME""""
    export PATH=""""$JAVA_HOME"""/bin:"""$PATH""""
    echo "✅ Java 21 set successfully!"
    echo "JAVA_HOME: """$JAVA_HOME""""
    java -version
else
    echo "❌ Java 21 not found. Please install Java 21 first."
    echo ""
    echo "You can install it using:"
    echo "  brew install --cask temurin21"
    echo "or"
    echo "  sdk install java 21.0.2-tem"
    exit 1
fi