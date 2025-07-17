#!/bin/bash

# Simple Maven wrapper script
export JAVA_HOME=/Users/lsendel/.sdkman/candidates/java/current
export MAVEN_HOME=/opt/homebrew/Cellar/maven/3.9.11/libexec
export PATH=$MAVEN_HOME/bin:$PATH

# Execute Maven with all arguments
exec "$MAVEN_HOME/bin/mvn" "$@"