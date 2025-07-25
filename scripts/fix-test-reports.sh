#!/bin/bash

# Script to diagnose and fix test report generation issues

echo "=== Test Report Diagnostic Script ==="
echo

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "ERROR: Not in project root directory (no pom.xml found)"
    exit 1
fi

# Function to check if a module has tests
check_module_tests() {
    local module=$1
    if [ -d "$module/src/test/java" ]; then
        local test_count=$(find "$module/src/test/java" -name "*Test.java" -o -name "*Tests.java" | wc -l)
        echo "Module $module has $test_count test files"
        return 0
    else
        echo "Module $module has no test directory"
        return 1
    fi
}

# Function to ensure JaCoCo is configured for a module
ensure_jacoco_config() {
    local module=$1
    local pom_file="$module/pom.xml"
    
    if [ ! -f "$pom_file" ]; then
        return 1
    fi
    
    # Check if JaCoCo plugin is already configured
    if grep -q "jacoco-maven-plugin" "$pom_file"; then
        echo "JaCoCo already configured in $module"
    else
        echo "WARNING: JaCoCo not configured in $module"
    fi
}

echo "1. Checking Maven modules for test configuration..."
echo

# Get all modules from parent pom
modules=$(grep -oP '(?<=<module>)[^<]+' pom.xml)

for module in $modules; do
    if [ -d "$module" ]; then
        check_module_tests "$module"
        ensure_jacoco_config "$module"
        echo
    fi
done

echo "2. Running test compilation to verify setup..."
echo

# Try to compile tests
mvn clean test-compile -B -q

if [ $? -eq 0 ]; then
    echo "✓ Test compilation successful"
else
    echo "✗ Test compilation failed"
fi

echo
echo "3. Running a single module test with verbose output..."
echo

# Find a module with tests
test_module=""
for module in $modules; do
    if [ -d "$module/src/test/java" ] && [ -f "$module/pom.xml" ]; then
        test_module=$module
        break
    fi
done

if [ -n "$test_module" ]; then
    echo "Testing module: $test_module"
    cd "$test_module"
    mvn clean test -Djacoco.skip=false -B
    
    # Check for generated reports
    echo
    echo "4. Checking for generated reports in $test_module..."
    
    if [ -d "target/surefire-reports" ]; then
        echo "✓ Surefire reports directory exists"
        ls -la target/surefire-reports/ | head -5
    else
        echo "✗ No surefire-reports directory found"
    fi
    
    if [ -d "target/site/jacoco" ]; then
        echo "✓ JaCoCo reports directory exists"
        ls -la target/site/jacoco/ | head -5
    else
        echo "✗ No JaCoCo reports directory found"
    fi
    
    cd ..
else
    echo "No suitable test module found"
fi

echo
echo "5. Recommendations:"
echo "==================="

# Check if tests are being skipped
if grep -q "maven.test.skip.*true" pom.xml; then
    echo "- WARNING: Tests might be skipped by default. Check maven.test.skip property"
fi

echo "- Ensure all modules inherit JaCoCo configuration from parent pom"
echo "- Use 'mvn clean test -Pci -Djacoco.skip=false' to run tests with coverage"
echo "- Check that argLine property includes @{argLine} placeholder for JaCoCo"
echo "- Verify test files follow naming convention (*Test.java or *Tests.java)"

echo
echo "6. Suggested CI workflow fix:"
echo "============================="
echo "Add explicit JaCoCo execution binding in parent pom.xml:"
echo
cat << 'EOF'
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <executions>
        <execution>
            <id>default-prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>default-report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
EOF

echo
echo "Script completed."