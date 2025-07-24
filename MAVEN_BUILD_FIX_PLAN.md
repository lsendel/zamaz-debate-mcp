# Maven Build Issues Fix Plan

## Problem Analysis
The error "Could not find or load main class" with "ClassNotFoundException" indicates several Maven build issues:

1. **Missing Module**: `mcp-config-server` is declared in parent POM but directory doesn't exist
2. **Compilation Issues**: Some modules may have missing dependencies or compilation errors  
3. **Main Class Issues**: Spring Boot applications may be missing proper main classes
4. **Dependency Resolution**: Inter-module dependencies may not be resolved correctly

## Step-by-Step Fix Plan

### Phase 1: Diagnosis
Run the diagnostic script to identify specific issues:
```bash
./scripts/diagnose-maven-issues.sh
```

### Phase 2: Comprehensive Fix
Run the comprehensive fix script:
```bash
./scripts/fix-maven-build-comprehensive.sh
```

### Phase 3: Validation
Run the validation script to ensure everything works:
```bash
./scripts/validation/validate-maven-build.sh
```

## Manual Steps (if automated scripts fail)

### Step 1: Fix Missing Module
The `mcp-config-server` module is declared in the parent POM but doesn't exist:

```bash
# Remove the missing module from parent POM temporarily
sed -i.bak 's|<module>mcp-config-server</module>|<!-- <module>mcp-config-server</module> -->|g' pom.xml
```

### Step 2: Clean Everything
```bash
# Clean all Maven artifacts
find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
rm -rf ~/.m2/repository/com/zamaz/mcp
mvn clean --batch-mode --no-transfer-progress
```

### Step 3: Build Core Modules First
```bash
# Build common modules first (dependency order)
cd mcp-common && mvn clean install -DskipTests --batch-mode --no-transfer-progress
cd ../mcp-security && mvn clean install -DskipTests --batch-mode --no-transfer-progress
cd ..
```

### Step 4: Build Remaining Modules
```bash
# Try full project build
mvn clean compile -T 2C --batch-mode --no-transfer-progress
```

### Step 5: Create Missing Config Server (Optional)
If you need the config server module:
```bash
mkdir -p mcp-config-server/src/main/java/com/zamaz/mcp/config
# Create basic Spring Boot application (see comprehensive script for details)
```

## Expected Outcomes

### Success Indicators
- ✅ All modules compile without errors
- ✅ Spring Boot applications can be packaged
- ✅ No "ClassNotFoundException" errors
- ✅ Maven dependency resolution works

### Common Issues and Solutions

#### Issue: "Could not find or load main class"
**Solution**: Ensure Spring Boot modules have proper main classes in the correct package structure

#### Issue: "ClassNotFoundException" 
**Solution**: Check that all dependencies are properly resolved and modules are built in correct order

#### Issue: Missing dependencies
**Solution**: Run `mvn dependency:resolve` and check for any missing artifacts

#### Issue: Compilation errors
**Solution**: Check Java version (must be 21+) and ensure all annotation processors are configured

## Verification Commands

After running the fix:
```bash
# Test compilation
mvn clean compile -T 2C --batch-mode --no-transfer-progress

# Test packaging of a Spring Boot app
cd mcp-organization && mvn clean package -DskipTests --batch-mode --no-transfer-progress

# Test running a Spring Boot app
java -jar target/mcp-organization-1.0.0.jar --spring.profiles.active=test
```

## Rollback Plan

If the fixes cause issues:
```bash
# Restore original POM
git checkout pom.xml

# Clean everything
mvn clean
rm -rf ~/.m2/repository/com/zamaz/mcp

# Start over with manual approach
```

## Next Steps

1. Run the diagnostic script first to understand the specific issues
2. Execute the comprehensive fix script
3. Validate the results
4. If issues persist, follow the manual steps
5. Consider removing unused/legacy modules from the parent POM to simplify the build