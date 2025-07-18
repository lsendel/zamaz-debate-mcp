#!/bin/bash
# Script to set Java 21 for this project

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="""$JAVA_HOME""/bin:""$PATH"""

echo "Java version set to:"
java -version

# Make this permanent for the shell session
echo "JAVA_HOME is set to: ""$JAVA_HOME"""