# CI/CD Pipeline Guide

## Overview

This document provides a comprehensive guide to the enhanced CI/CD pipeline for the Zamaz Debate MCP Services project.

## Pipeline Architecture

### Workflow Triggers
- **Push events**: `main`, `develop`, `feature/**`, `hotfix/**`, `release/**`
- **Pull requests**: to `main` and `develop` branches
- **Manual dispatch**: with optional parameters for testing scenarios

### Pipeline Stages

#### 1. Java Build & Test (15-20 minutes)
- **Parallel compilation** using Maven `-T 2C` flag
- **Unit tests** with JaCoCo coverage analysis
- **Integration tests** using TestContainers
- **Coverage threshold**: 80% instruction coverage, 70% branch coverage
- **Artifacts**: JAR files, test reports, coverage reports

#### 2. Code Quality Analysis (10-15 minutes)
- **Checkstyle**: Code style enforcement
- **SpotBugs**: Static analysis for bug detection
- **SonarQube**: Comprehensive code quality analysis (when configured)
- **Quality gates**: Non-blocking warnings, blocking on critical issues

#### 3. Frontend Build & Test (5-10 minutes)
- **React TypeScript** compilation and bundling
- **ESLint**: Code linting and style checks
- **Jest**: Unit tests with coverage reporting
- **Build optimization**: Production-ready bundle generation

#### 4. Security Scanning (10-15 minutes)
- **OWASP Dependency Check**: Vulnerability scanning for dependencies
- **Semgrep**: Static application security testing (SAST)
- **TruffleHog**: Secret detection in codebase
- **Container scanning**: Docker image vulnerability assessment

#### 5. Build Summary & Quality Gates
- **Comprehensive reporting**: Pipeline metrics and artifact inventory
- **Quality gate assessment**: Pass/fail determination based on all stages
- **Automated notifications**: Issue creation for critical failures

## Maven Profiles

### Available Profiles

#### `ci` Profile
- Optimized for CI/CD environments
- Enhanced parallel test execution
- Memory-optimized JVM settings
- Skips unnecessary documentation generation

#### `code-quality` Profile
- Enables all quality analysis tools
- Checkstyle, SpotBugs, and JaCoCo execution
- Generates comprehensive quality reports

#### `security` Profile
- OWASP Dependency Check with configurable CVSS threshold
- Enhanced security scanning configuration
- Multiple report formats (XML, HTML, JSON)

#### `fast` Profile
- Skips tests and quality checks
- For emergency builds or quick compilation verification
- Not recommended for production deployments

#### `release` Profile
- Generates source and Javadoc JARs
- Full documentation generation
- Optimized for release artifacts

## Configuration Files

### Maven Configuration
- **`.mvn/maven.config`**: Default Maven execution parameters
- **`.mvn/jvm.config`**: JVM optimization for build performance
- **`.mvn/settings.xml`**: CI-optimized Maven settings

### Quality Configuration
- **`checkstyle.xml`**: Code style rules and enforcement
- **`pom.xml`**: Comprehensive plugin configuration with quality thresholds

## Usage Examples

### Local Development
```bash
# Full build with all quality checks
mvn clean verify -Pci,code-quality

# Fast build for development
mvn clean compile -Pfast

# Security scan only
mvn clean verify -Psecurity
```

### CI/CD Pipeline
```bash
# Validate build setup
./scripts/validate-build.sh

# Run specific validation
./scripts/validate-build.sh quality
./scripts/validate-build.sh security
```

### Manual Workflow Dispatch
Use GitHub Actions UI to trigger builds with custom parameters:
- **Skip tests**: For emergency builds
- **Force security scan**: Run security checks on any branch
- **Build images**: Force Docker image building

## Quality Thresholds

### Test Coverage
- **Instruction Coverage**: 80% minimum
- **Branch Coverage**: 70% minimum
- **Exclusions**: DTOs, configuration classes, main application classes

### Security
- **CVSS Threshold**: 7.0 (configurable via environment variable)
- **Dependency Vulnerabilities**: Fail on high/critical issues
- **Secret Detection**: Zero tolerance for exposed secrets

### Code Quality
- **Checkstyle**: Warning level, non-blocking
- **SpotBugs**: Low threshold, comprehensive analysis
- **Line Length**: 120 characters maximum
- **Method Length**: 50 lines maximum

## Performance Optimizations

### Build Performance
- **Parallel execution**: `-T 2C` for optimal CPU utilization
- **Dependency caching**: GitHub Actions cache for Maven dependencies
- **JVM tuning**: G1GC with optimized heap settings
- **Incremental builds**: Skip unchanged modules when possible

### Test Performance
- **Parallel test execution**: Methods-level parallelization
- **Test containers**: Optimized database setup for integration tests
- **Memory allocation**: Separate heap settings for test execution
- **Retry mechanism**: Automatic retry for flaky tests

## Monitoring and Alerts

### Success Metrics
- **Build success rate**: Target >95%
- **Average build time**: Target <25 minutes
- **Test execution time**: Target <10 minutes
- **Quality gate pass rate**: Target >90%

### Failure Handling
- **Automatic issue creation**: For main/develop branch failures
- **Slack notifications**: Real-time alerts for critical issues
- **Email alerts**: Digest reports for quality trends
- **Dashboard updates**: GitHub status checks and PR comments

## Troubleshooting

### Common Issues

#### Build Timeouts
- Check Maven memory settings in `.mvn/jvm.config`
- Verify parallel execution configuration
- Review test execution times and optimize slow tests

#### Quality Gate Failures
- Review Checkstyle and SpotBugs reports
- Check test coverage reports for uncovered code
- Verify security scan results for vulnerabilities

#### Dependency Issues
- Clear Maven cache: `mvn dependency:purge-local-repository`
- Update dependency versions in parent POM
- Check for conflicting transitive dependencies

### Debug Commands
```bash
# Verbose Maven execution
mvn clean verify -X -Pci

# Generate dependency tree
mvn dependency:tree

# Analyze build performance
mvn clean verify -Pci --debug
```

## Best Practices

### Development Workflow
1. **Feature branches**: Always create feature branches for new development
2. **Pull requests**: Use PR reviews for code quality assurance
3. **Local testing**: Run quality checks locally before pushing
4. **Incremental commits**: Make small, focused commits for easier review

### CI/CD Optimization
1. **Cache utilization**: Leverage GitHub Actions caching for dependencies
2. **Parallel execution**: Use appropriate thread counts for your environment
3. **Quality gates**: Set realistic but strict quality thresholds
4. **Security scanning**: Regular dependency updates and vulnerability monitoring

### Maintenance
1. **Regular updates**: Keep Maven plugins and dependencies current
2. **Performance monitoring**: Track build times and optimize bottlenecks
3. **Quality metrics**: Monitor code coverage and quality trends
4. **Security patches**: Apply security updates promptly

## Integration Points

### External Services
- **SonarQube/SonarCloud**: Code quality analysis and reporting
- **GitHub**: Source control, PR reviews, and status checks
- **Container Registry**: Docker image storage and vulnerability scanning
- **Notification Services**: Slack, email, and dashboard integrations

### Future Enhancements
- **Deployment automation**: Staging and production deployment workflows
- **Performance testing**: Load testing integration
- **Multi-environment support**: Environment-specific configuration management
- **Advanced security**: Runtime security monitoring and compliance reporting