# Security Scanning Integration Guide

This guide provides comprehensive information about the security scanning integration implemented for the Zamaz Debate MCP Services project.

## Overview

The security scanning integration provides automated security testing and vulnerability detection throughout the CI/CD pipeline. It includes multiple scanning tools and techniques to identify security issues early in the development process.

## Security Scanning Components

### 1. Dependency Vulnerability Scanning

Identifies vulnerabilities in project dependencies using:

- **OWASP Dependency Check**: Scans Java dependencies for known vulnerabilities
- **NPM Audit**: Scans JavaScript dependencies for known vulnerabilities

### 2. Static Application Security Testing (SAST)

Analyzes source code for security issues using:

- **Semgrep**: Pattern-based code analysis for security vulnerabilities
- **SonarQube**: Comprehensive code quality and security analysis
- **SpotBugs**: Java-specific bug and security issue detection

### 3. Secrets Detection

Identifies hardcoded secrets and credentials using:

- **TruffleHog**: Advanced secret detection with pattern matching
- **GitLeaks**: Git history scanning for leaked secrets

### 4. Container Security Scanning

Analyzes container images for vulnerabilities using:

- **Trivy**: Comprehensive container vulnerability scanner
- **Docker Scout**: Docker's built-in vulnerability scanner

### 5. Infrastructure as Code (IaC) Security Scanning

Analyzes infrastructure code for security issues using:

- **Checkov**: Multi-framework IaC security scanner
- **TFSec**: Terraform security scanner
- **Hadolint**: Dockerfile linter and security scanner
- **Kubesec**: Kubernetes manifest security scanner

### 6. Dynamic Application Security Testing (DAST)

Tests running applications for security vulnerabilities using:

- **OWASP ZAP**: Web application security scanner
- **API Security Testing**: Tests API endpoints for security issues

## CI/CD Integration

### GitHub Actions Workflow

The security scanning is integrated into the CI/CD pipeline through the `security-enhanced.yml` workflow, which:

1. Runs automatically on schedule (daily)
2. Runs on pushes to main, develop, and release branches
3. Runs on pull requests to main and develop branches
4. Can be triggered manually with customizable parameters

### Security Gates

The pipeline implements security gates that:

1. Block the pipeline on critical security issues
2. Generate comprehensive security reports
3. Create GitHub issues for identified vulnerabilities
4. Provide feedback in pull request comments

## Usage Guide

### Running Security Scans Locally

#### Dependency Vulnerability Scanning

```bash
# Run OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

# Run NPM Audit
cd debate-ui && npm audit
```

#### Static Application Security Testing (SAST)

```bash
# Run Semgrep
semgrep --config=p/security-audit --config=p/owasp-top-ten .

# Run SpotBugs
mvn spotbugs:check
```

#### Container Security Scanning

```bash
# Scan all services
./scripts/docker-image-scan.sh --all

# Scan specific service
./scripts/docker-image-scan.sh mcp-organization

# Scan with custom severity levels
./scripts/docker-image-scan.sh --severity CRITICAL,HIGH,MEDIUM mcp-organization
```

#### Infrastructure as Code (IaC) Security Scanning

```bash
# Scan all IaC files
./scripts/iac-security-scan.sh --type all

# Scan only Dockerfiles
./scripts/iac-security-scan.sh --type docker

# Scan Kubernetes manifests
./scripts/iac-security-scan.sh --type kubernetes
```

#### Dynamic Application Security Testing (DAST)

```bash
# Run ZAP baseline scan
./scripts/api-security-scan.sh --target http://localhost:8080

# Run ZAP full scan
./scripts/api-security-scan.sh --target http://localhost:8080 --mode full

# Run API scan with OpenAPI spec
./scripts/api-security-scan.sh --target http://localhost:8080 --spec api-spec.json --mode api
```

### Viewing Security Reports

Security reports are generated in multiple formats:

1. **HTML Reports**: Comprehensive reports with detailed findings
2. **JSON Reports**: Machine-readable reports for integration with other tools
3. **SARIF Reports**: GitHub-compatible reports for Security tab integration
4. **Markdown Reports**: Human-readable summary reports

Reports are available in the `security-reports` directory after running scans.

### Triggering Scans in CI/CD

To trigger security scans in the CI/CD pipeline:

1. **Automatic Scans**: Triggered by pushes to main, develop, and release branches
2. **Manual Scans**: Triggered using GitHub Actions workflow dispatch
3. **Scheduled Scans**: Run daily at 3 AM UTC

## Security Policy

The project follows a comprehensive security policy defined in `.github/security-policy.yml`, which includes:

1. **Vulnerability Management**: Severity thresholds and scanning tools
2. **Secret Detection**: Tools and notification settings
3. **Container Security**: Base image allowlist and required security features
4. **Infrastructure as Code Security**: Scanning tools and frameworks
5. **Compliance Requirements**: Standards and audit logging
6. **Security Testing**: SAST, DAST, and SCA configuration
7. **Incident Response**: Issue creation and notifications
8. **Security Metrics**: Tracked metrics and reporting frequency

## Best Practices

### Dependency Management

- Regularly update dependencies to address security vulnerabilities
- Use dependency locking to prevent unexpected updates
- Implement a dependency review process for new dependencies
- Monitor dependencies for security advisories

### Code Security

- Follow secure coding guidelines
- Use security-focused code reviews
- Implement input validation and output encoding
- Apply the principle of least privilege
- Use security annotations and frameworks

### Container Security

- Use minimal base images
- Keep base images updated
- Run containers as non-root users
- Implement proper secret management
- Use multi-stage builds to reduce attack surface

### Infrastructure as Code Security

- Follow security best practices for IaC
- Use security-hardened templates
- Implement least privilege principle
- Avoid hardcoding sensitive information
- Use version pinning for dependencies

### Secret Management

- Use environment variables for secrets
- Implement a secrets management solution (e.g., HashiCorp Vault)
- Rotate secrets regularly
- Avoid hardcoding secrets in source code
- Use secret scanning to detect leaked secrets

## Troubleshooting

### Common Issues

#### Dependency Check Failures

- **Issue**: Dependency check fails with connection errors
  - **Solution**: Check network connectivity and proxy settings
- **Issue**: False positives in dependency scanning
  - **Solution**: Use suppression files to ignore known false positives

#### SAST Scan Failures

- **Issue**: SAST scan times out
  - **Solution**: Increase timeout or scan specific directories
- **Issue**: Too many false positives
  - **Solution**: Adjust rule configurations or add exclusions

#### Secrets Detection Issues

- **Issue**: High number of false positives
  - **Solution**: Customize detection patterns or add exclusions
- **Issue**: Scan fails on large repositories
  - **Solution**: Increase memory allocation or scan incrementally

#### Container Scanning Issues

- **Issue**: Container build fails during scan
  - **Solution**: Fix Dockerfile issues before scanning
- **Issue**: Vulnerability remediation challenges
  - **Solution**: Use newer base images or multi-stage builds

#### IaC Scanning Issues

- **Issue**: Scan fails with parsing errors
  - **Solution**: Validate IaC files syntax before scanning
- **Issue**: Too many policy violations
  - **Solution**: Customize policies or add exceptions for specific cases

#### DAST Scanning Issues

- **Issue**: ZAP scan fails to connect to target
  - **Solution**: Ensure target application is running and accessible
- **Issue**: Scan takes too long
  - **Solution**: Limit scan scope or use baseline scan instead of full scan

## Integration with Development Workflow

### Pre-commit Hooks

Set up pre-commit hooks to run security checks before committing code:

```bash
# Install pre-commit
pip install pre-commit

# Add pre-commit configuration
cat > .pre-commit-config.yaml << EOF
repos:
-   repo: https://github.com/gitleaks/gitleaks
    rev: v8.16.3
    hooks:
    -   id: gitleaks
-   repo: https://github.com/returntocorp/semgrep
    rev: v1.18.0
    hooks:
    -   id: semgrep
        args: ['--config', 'p/security-audit', '--error']
EOF

# Install hooks
pre-commit install
```

### IDE Integration

Configure IDE plugins for real-time security feedback:

- **SonarLint**: Real-time code quality and security analysis
- **SpotBugs**: Java bug and security issue detection
- **Snyk**: Dependency vulnerability scanning
- **Checkov**: IaC security scanning

### Pull Request Checks

The CI/CD pipeline automatically runs security checks on pull requests:

1. SAST scans for code security issues
2. Dependency checks for vulnerable libraries
3. Secret detection for exposed credentials
4. Container scanning for Docker image vulnerabilities

## Security Metrics and Reporting

### Key Security Metrics

- **Mean Time to Remediate**: Average time to fix security issues
- **Vulnerability Density**: Number of vulnerabilities per 1000 lines of code
- **Security Debt Ratio**: Percentage of security issues vs total issues
- **Scan Coverage**: Percentage of code covered by security scans
- **Critical Vulnerability Count**: Number of critical vulnerabilities

### Reporting

- **Daily Scans**: Automated security scans run daily
- **Weekly Reports**: Comprehensive security reports generated weekly
- **Monthly Metrics**: Security metrics tracked and reported monthly
- **Compliance Reports**: Compliance status reports generated as needed

## References

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Semgrep Documentation](https://semgrep.dev/docs/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/latest/)
- [TruffleHog Documentation](https://github.com/trufflesecurity/trufflehog)
- [GitLeaks Documentation](https://github.com/zricethezav/gitleaks)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [OWASP ZAP Documentation](https://www.zaproxy.org/docs/)
- [Checkov Documentation](https://www.checkov.io/1.Welcome/Quick%20Start.html)
- [GitHub Security Features](https://docs.github.com/en/code-security)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)