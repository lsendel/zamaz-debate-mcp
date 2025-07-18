# Java Build Best Practices and Issue Prevention

## Overview

This document outlines best practices for maintaining a healthy Java build environment and preventing common build issues in the MCP project.

## Fixed Issues Summary

### ✅ Critical Issues Resolved (2025-01-18)

1. **Circular Dependency Fixed** (`mcp-security` ↔ `mcp-organization`)
   - **Problem**: mcp-security depended on mcp-organization for UserRepository, while mcp-organization depended on mcp-security for authentication
   - **Solution**: Created `UserLookupService` interface in mcp-common, implemented in mcp-organization
   - **Location**: `mcp-common/src/main/java/com/zamaz/mcp/common/security/UserLookupService.java`

2. **Missing Modules Added to Parent POM**
   - **Added**: mcp-gateway, mcp-testing, mcp-pattern-recognition, github-integration, load-tests, performance-tests/gatling
   - **Location**: `pom.xml` lines 91-96

3. **OWASP Dependency Misconfiguration Fixed**
   - **Problem**: OWASP dependency-check-maven was declared as dependency instead of plugin only
   - **Solution**: Removed from dependencies section in mcp-testing
   - **Location**: `mcp-testing/pom.xml`

4. **Version Conflicts Resolved**
   - **Problem**: Testcontainers (1.19.3 vs 1.20.4), REST Assured (5.3.2 vs 5.5.0) version mismatches
   - **Solution**: Removed version overrides to use parent POM versions
   - **Location**: `mcp-testing/pom.xml`

5. **MapStruct Java 21 Compatibility**
   - **Problem**: MapStruct dependency commented out due to compatibility concerns
   - **Solution**: Re-enabled with proper annotation processor configuration
   - **Location**: `mcp-common/pom.xml` line 172-175

## Best Practices

### 1. Dependency Management

#### ✅ DO:
- **Use parent POM for version management**: All dependency versions should be declared in `dependencyManagement` section of parent POM
- **Avoid version overrides**: Let child modules inherit versions from parent
- **Use BOM imports**: Import Spring Boot, Spring Cloud, and other BOMs for consistent versions
- **Regular dependency updates**: Schedule quarterly dependency updates

#### ❌ DON'T:
- **Declare plugin dependencies**: Never add Maven plugins to the `dependencies` section
- **Override parent versions**: Avoid `<version>` tags in child module dependencies unless absolutely necessary
- **Mix dependency scopes**: Be consistent with `test`, `runtime`, `provided` scopes

#### Example:
```xml
<!-- Parent POM - GOOD -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.20.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Child POM - GOOD -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<!-- Child POM - BAD -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version> <!-- Don't override parent version -->
    <scope>test</scope>
</dependency>
```

### 2. Module Architecture

#### ✅ DO:
- **Break circular dependencies**: Use interfaces in common modules to break cycles
- **Layer dependencies properly**: Common → Security → Organization → Business Logic
- **Declare all modules**: Ensure parent POM includes all actual modules
- **Use consistent naming**: Follow module naming conventions

#### ❌ DON'T:
- **Create circular dependencies**: Avoid A depends on B, B depends on A patterns
- **Skip module declarations**: Every module directory with pom.xml must be declared in parent
- **Mix architectural layers**: Don't let lower layers depend on higher layers

#### Dependency Order:
```
mcp-common (base utilities)
    ↓
mcp-security (authentication interfaces)
    ↓  
mcp-organization (user management + security implementation)
    ↓
mcp-debate-engine, mcp-llm, mcp-rag (business logic)
```

### 3. Security Configuration

#### ✅ DO:
- **Use environment variables**: Never hardcode credentials in POMs
- **Regular security scans**: Run OWASP dependency check in CI/CD
- **Pin plugin versions**: Always specify plugin versions for reproducible builds
- **Fail on high CVEs**: Set `failBuildOnCVSS=7` for production builds

#### Example Security Scan:
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.4</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <skipTestScope>false</skipTestScope>
    </configuration>
</plugin>
```

### 4. Build Automation

#### GitHub Actions Workflow
- **Automatic validation**: Build validation runs on every PR
- **Dependency scanning**: Nightly security scans
- **Multi-Java testing**: Test against Java 21
- **Parallel builds**: Use `-T 1C` for faster compilation

#### Pre-commit Hooks
```bash
# Install pre-commit hooks
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit
```

### 5. Code Quality

#### ✅ DO:
- **Enable all quality checks**: Checkstyle, SpotBugs, JaCoCo coverage
- **Set coverage thresholds**: Minimum 80% instruction coverage, 70% branch coverage
- **Use consistent formatting**: Configure IDE with project code style
- **Document public APIs**: Use JavaDoc for all public methods

#### Profiles for Different Scenarios:
```bash
# Fast build (skip tests and quality checks)
mvn clean package -Pfast

# Full quality check
mvn clean verify -Pcode-quality

# CI build (optimized for CI environment)
mvn clean verify -Pci

# Security scan
mvn clean verify -Psecurity
```

## Issue Prevention Checklist

### Before Committing:
- [ ] Run `mvn clean compile` successfully
- [ ] No circular dependency warnings in build output  
- [ ] All tests pass with `mvn test`
- [ ] Security scan passes with no high-severity vulnerabilities
- [ ] New modules added to parent POM `<modules>` section

### Before Releasing:
- [ ] Full build passes: `mvn clean verify -Pcode-quality`
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version numbers consistent across all modules
- [ ] Docker images build successfully

### Monthly Maintenance:
- [ ] Review and update dependency versions
- [ ] Check for new security vulnerabilities
- [ ] Review and clean up unused dependencies
- [ ] Update documentation and best practices

## Troubleshooting Common Issues

### "Could not find or load main class #"
- **Cause**: Shell environment issue with Maven wrapper
- **Solution**: Use direct `mvn` command or fix shell configuration

### "Circular dependency detected"
- **Cause**: Module A depends on module B, and B depends on A
- **Solution**: Create interface in common module, implement in one of the modules

### "Version conflicts"
- **Cause**: Different modules specify different versions of same dependency
- **Solution**: Remove version declarations from child POMs, use parent POM management

### "Plugin not found"
- **Cause**: Plugin declared as dependency instead of in plugins section
- **Solution**: Move to `<build><plugins>` section

### "Module not found during build"
- **Cause**: Module directory exists but not declared in parent POM
- **Solution**: Add `<module>module-name</module>` to parent POM

## Monitoring and Metrics

### Build Health Indicators:
- **Build duration**: Target < 5 minutes for full build
- **Test coverage**: Maintain > 80% instruction coverage
- **Security vulnerabilities**: Zero high-severity CVEs
- **Code quality**: Zero critical code smells

### Automated Alerts:
- Build failures on main branch
- Security vulnerabilities in dependencies  
- Coverage drops below threshold
- Long-running builds (> 10 minutes)

## Tools Integration

### IDE Setup:
- **IntelliJ IDEA**: Import Maven projects, enable annotation processing
- **VS Code**: Use Java Extension Pack, configure Maven integration
- **Eclipse**: Use M2Eclipse plugin, configure project facets

### CI/CD Integration:
- **GitHub Actions**: Automated build validation workflow
- **Docker**: Multi-stage builds for optimized images
- **SonarQube**: Code quality and security analysis

## Update Log

- **2025-01-18**: Initial build issue resolution and best practices documentation
- Fixed circular dependency between mcp-security and mcp-organization
- Added missing modules to parent POM
- Resolved all version conflicts
- Created automated validation workflow