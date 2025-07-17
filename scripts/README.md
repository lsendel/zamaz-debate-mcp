# 📁 Scripts Directory Structure

This directory contains all project-wide scripts organized by purpose and scope.

## 🗂️ **Directory Organization**

### **📊 `/development/`** - Development & Setup
Development environment setup, configuration, and utility scripts.

| Script | Purpose | Usage |
|--------|---------|-------|
| `quick-start.sh` | Quick service startup | `./scripts/development/quick-start.sh` |
| `start-services.sh` | Full service orchestration | `./scripts/development/start-services.sh` |
| `run-with-env.sh` | Environment-aware execution | `./scripts/development/run-with-env.sh` |
| `setup-pre-commit.sh` | Git hooks setup | `./scripts/development/setup-pre-commit.sh` |
| `validate-setup.sh` | Environment validation | `./scripts/development/validate-setup.sh` |
| `check-env-vars.sh` | Environment variable checker | `./scripts/development/check-env-vars.sh` |
| `fix-*.sh` | Development fixes | Various fix utilities |

### **🧪 `/testing/`** - Testing & Validation
Comprehensive testing scripts for different layers and components.

| Category | Scripts | Purpose |
|----------|---------|---------|
| **Unit/Integration** | `test-*.sh` | Individual service testing |
| **Performance** | `performance/run-load-test.sh` | Load and performance testing |
| **Integration** | `integration/test-*.sh` | Cross-service integration tests |
| **Smoke Tests** | `smoke-tests.sh` | Basic health validation |
| **Utilities** | `wait-for-*.sh` | Test coordination utilities |

### **🔒 `/security/`** - Security Testing & Scanning
Security analysis, vulnerability scanning, and penetration testing.

| Script | Purpose | Frequency |
|--------|---------|-----------|
| `security-penetration-test.sh` | Comprehensive penetration testing | Weekly |
| `api-security-scan.sh` | API endpoint security analysis | Daily |
| `iac-security-scan.sh` | Infrastructure as Code scanning | On change |
| `security-monitoring.sh` | Real-time security monitoring | Continuous |
| `security-benchmark.sh` | Security compliance benchmarking | Monthly |
| `check-hardcoded-values.sh` | Hardcoded secrets detection | Pre-commit |

### **🚀 `/deployment/`** - Deployment & Infrastructure
Container management, deployment automation, and infrastructure scripts.

| Script | Purpose | Environment |
|--------|---------|-------------|
| `docker-cleanup.sh` | Container cleanup | All |
| `docker-registry-manager.sh` | Registry management | CI/CD |
| `start-mcp-services.sh` | Service orchestration | Production |
| `docker-image-scan.sh` | Container security scanning | All |

### **📈 `/monitoring/`** - Quality & Monitoring
Code quality analysis, reporting, and continuous monitoring.

| Category | Scripts | Output |
|----------|---------|--------|
| **SonarCloud** | `generate-*-sonarcloud-report.sh` | Quality reports |
| **Quality Gates** | `sonar-quality-check.sh` | Pass/fail status |
| **Metrics** | `sonarcloud-simple-report.sh` | Basic metrics |
| **Configuration** | `sonarcloud-env-exports.sh` | Environment setup |

### **🔧 `/maintenance/`** - Maintenance & Utilities
System maintenance, cleanup, and administrative utilities.

| Script | Purpose | Schedule |
|--------|---------|----------|
| `fix-blocker-issues.sh` | Automated issue fixes | As needed |
| `add-to-claude-code.sh` | Claude integration setup | One-time |
| `mcp-full-client.sh` | Complete client interaction | Testing |

## 🎯 **Service-Specific Scripts**

Service-specific scripts remain in their respective service directories:

```
mcp-controller/scripts/
├── debate-demo-curl.sh          # Debate demonstration
├── complete-debate-demo.sh      # Full debate workflow
├── mcp-debate-client.sh         # Client interaction
└── test-debate-flow.sh          # Debate flow testing

mcp-llm/scripts/
├── llm-demo-curl.sh            # LLM service demo
├── simple-llm-example.sh       # Basic examples
├── sky-color-*.sh              # Example implementations
└── test-llm.sh                 # LLM testing
```

## 📋 **Script Execution Guidelines**

### **Prerequisites**
```bash
# Ensure scripts are executable
chmod +x scripts/**/*.sh

# Set up environment
source scripts/development/check-env-vars.sh
```

### **Common Workflows**

#### **🚀 Development Setup**
```bash
# Quick start for development
./scripts/development/quick-start.sh

# Validate environment
./scripts/development/validate-setup.sh
```

#### **🧪 Testing Pipeline**
```bash
# Run all tests
./scripts/testing/run-all-tests.sh

# Security validation
./scripts/security/security-test-suite.sh

# Performance testing
./scripts/testing/performance/run-load-test.sh
```

#### **📊 Quality Monitoring**
```bash
# Generate quality report
./scripts/monitoring/generate-actionable-sonarcloud-report.sh

# Check quality gate
./scripts/monitoring/sonar-quality-check.sh
```

#### **🔒 Security Scanning**
```bash
# Full security scan
./scripts/security/security-penetration-test.sh

# API security check
./scripts/security/api-security-scan.sh
```

## 🔄 **Continuous Integration**

Scripts are designed for CI/CD integration:

```yaml
# Example GitHub Actions usage
- name: Run Security Scan
  run: ./scripts/security/security-test-suite.sh

- name: Quality Gate Check  
  run: ./scripts/monitoring/sonar-quality-check.sh

- name: Deploy Services
  run: ./scripts/deployment/start-mcp-services.sh
```

## 📚 **Documentation**

Each script includes:
- **Header documentation** explaining purpose and usage
- **Parameter validation** with clear error messages  
- **Exit codes** for CI/CD integration
- **Logging** with timestamps and severity levels

## 🤝 **Contributing**

When adding new scripts:

1. **Choose appropriate directory** based on script purpose
2. **Follow naming conventions**: `verb-noun.sh` format
3. **Include proper documentation** and error handling
4. **Make executable**: `chmod +x script-name.sh`
5. **Update this README** with script description

---

**📁 Total Scripts Organized**: 60+ scripts across all categories  
**🎯 Organization Benefits**: Better discoverability, maintainability, and CI/CD integration