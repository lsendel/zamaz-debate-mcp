# Docker Image Building and Registry Management Guide

This guide provides comprehensive information about the Docker image building and registry management system implemented for the Zamaz Debate MCP Services project.

## Overview

The Docker image building and registry management system automates the process of building, scanning, signing, and publishing Docker images for all microservices in the project. It integrates with the CI/CD pipeline to ensure that only secure and properly tested images are deployed to production.

## Image Building Strategy

### Multi-Stage Builds

All service Dockerfiles use multi-stage builds to minimize image size and reduce attack surface:

1. **Builder Stage**: Uses `maven:3.9-eclipse-temurin-21-alpine` to compile and package the Java application
2. **Runtime Stage**: Uses `eclipse-temurin:21-jre-alpine` as a minimal base image for running the application

### Security Hardening

All images include the following security features:

- Non-root user execution
- Minimal base images
- Security updates applied during build
- Proper permission management
- Health checks for container monitoring
- JVM optimization for containerized environments

### Image Tagging Strategy

Images are tagged using the following convention:

- `{registry}/{organization}/{service}:{version}`
- `{registry}/{organization}/{service}:{commit-sha}`
- `{registry}/{organization}/{service}:latest` (only for main branch builds)

Where:
- `{registry}`: Container registry (e.g., `ghcr.io`)
- `{organization}`: GitHub organization or username
- `{service}`: Service name (e.g., `mcp-organization`)
- `{version}`: Semantic version (e.g., `1.0.0`)
- `{commit-sha}`: Git commit SHA (e.g., `abc123`)

## Registry Management

### Container Registry

The project uses GitHub Container Registry (GHCR) as the primary container registry. Images are automatically pushed to the registry by the CI/CD pipeline when builds pass all quality gates.

### Access Control

- **Read Access**: Public for open-source images, restricted to organization members for private images
- **Write Access**: Limited to CI/CD pipeline and authorized developers
- **Admin Access**: Limited to repository administrators

### Image Lifecycle Management

- **Retention Policy**: Images older than 90 days are automatically removed, except for release versions
- **Vulnerability Scanning**: All images are scanned for vulnerabilities before being pushed to the registry
- **Image Signing**: Images are signed using Cosign to ensure authenticity and integrity

## CI/CD Integration

### Workflow Integration

The Docker image building and registry management system is integrated with the CI/CD pipeline through the following workflows:

1. **CI Workflow (`ci.yml`)**: Builds and tests the application code
2. **Docker Build Workflow (`docker-build.yml`)**: Builds, scans, and pushes Docker images

### Build Triggers

Docker images are built automatically when:

- Code is pushed to the `main` or `develop` branches
- Java code changes are detected in any branch
- Docker-related files are modified
- A manual build is triggered using the workflow dispatch

## Security Scanning

### Vulnerability Scanning

All Docker images are scanned for vulnerabilities using Trivy. The scanning process:

1. Checks for vulnerabilities in the base image
2. Scans application dependencies
3. Identifies misconfigurations and security issues
4. Generates detailed reports in multiple formats (table, JSON, SARIF)

### Security Policies

The following security policies are enforced:

- **Critical Vulnerabilities**: Block image promotion to production
- **High Vulnerabilities**: Require review and approval before promotion
- **Medium/Low Vulnerabilities**: Tracked for future remediation

## Usage Guide

### Building Images Locally

To build Docker images locally, use the `docker-registry-manager.sh` script:

```bash
# Build a specific service
./scripts/docker-registry-manager.sh build mcp-organization

# Build multiple services
./scripts/docker-registry-manager.sh build mcp-organization mcp-llm

# Build all services
./scripts/docker-registry-manager.sh --all build

# Build with a specific version
./scripts/docker-registry-manager.sh --version 1.0.0 build mcp-organization
```

### Pushing Images to Registry

To push Docker images to the registry:

```bash
# Push a specific service
./scripts/docker-registry-manager.sh push mcp-organization

# Push all services
./scripts/docker-registry-manager.sh --all push
```

### Scanning Images for Vulnerabilities

To scan Docker images for vulnerabilities:

```bash
# Scan a specific service
./scripts/docker-image-scan.sh mcp-organization

# Scan all services
./scripts/docker-image-scan.sh --all

# Generate JSON report
./scripts/docker-image-scan.sh --format json mcp-organization

# Scan with custom severity levels
./scripts/docker-image-scan.sh --severity CRITICAL,HIGH,MEDIUM mcp-organization
```

### Signing Images

To sign Docker images using Cosign:

```bash
# Sign a specific service
./scripts/docker-registry-manager.sh sign mcp-organization

# Sign all services
./scripts/docker-registry-manager.sh --all sign
```

## Best Practices

### Image Building

- Keep base images updated with security patches
- Minimize image layers to reduce size and attack surface
- Use specific version tags instead of `latest`
- Include proper metadata labels for traceability
- Implement health checks for all services

### Security

- Regularly scan images for vulnerabilities
- Update dependencies to address security issues
- Use non-root users in containers
- Implement least privilege principle
- Sign images to ensure authenticity

### Registry Management

- Implement retention policies to manage storage
- Use access controls to restrict image access
- Regularly audit registry access and usage
- Document image tagging and versioning strategy
- Implement image promotion workflows

## Troubleshooting

### Common Issues

#### Image Build Failures

- **Issue**: Docker build fails with "no space left on device"
  - **Solution**: Clean up unused images and volumes with `docker system prune -a`

- **Issue**: Maven dependency download fails during build
  - **Solution**: Check network connectivity and Maven repository access

#### Registry Access Issues

- **Issue**: Authentication failure when pushing to registry
  - **Solution**: Verify credentials and permissions

- **Issue**: Image not found when pulling
  - **Solution**: Check image name, tag, and registry path

#### Vulnerability Scanning Issues

- **Issue**: Trivy not installed or not found
  - **Solution**: Install Trivy using the instructions in the script

- **Issue**: Scan fails with timeout
  - **Solution**: Increase timeout or scan with fewer severity levels

## References

- [Docker Multi-Stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/latest/)
- [Cosign Documentation](https://docs.sigstore.dev/cosign/overview/)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)