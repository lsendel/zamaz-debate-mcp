#!/bin/bash

# Fix Lombok annotation processing in all Java services

echo "Fixing Lombok configuration in all Java services..."

# List of Java services
services=(
    "mcp-organization-j"
    "mcp-llm-j"
    "mcp-controller-j"
    "mcp-rag-j"
    "mcp-template-j"
)

for service in "${services[@]}"; do
    echo "Updating $service/pom.xml..."
    
    # Check if maven-compiler-plugin is already configured
    if grep -q "maven-compiler-plugin" "$service/pom.xml"; then
        echo "  maven-compiler-plugin already configured in $service"
    else
        # Add maven-compiler-plugin configuration before the closing </plugins> tag
        sed -i.bak '/<\/plugins>/i\
            <plugin>\
                <groupId>org.apache.maven.plugins</groupId>\
                <artifactId>maven-compiler-plugin</artifactId>\
                <configuration>\
                    <source>17</source>\
                    <target>17</target>\
                    <annotationProcessorPaths>\
                        <path>\
                            <groupId>org.projectlombok</groupId>\
                            <artifactId>lombok</artifactId>\
                            <version>${lombok.version}</version>\
                        </path>\
                    </annotationProcessorPaths>\
                </configuration>\
            </plugin>' "$service/pom.xml"
        
        echo "  ✓ Added maven-compiler-plugin configuration"
    fi
done

echo "Lombok configuration fixed!"