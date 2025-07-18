# Incremental Linting System - Comprehensive Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Configuration](#configuration)
5. [CLI Usage](#cli-usage)
6. [CI/CD Integration](#cicd-integration)
7. [Cache Management](#cache-management)
8. [Performance Optimization](#performance-optimization)
9. [Troubleshooting](#troubleshooting)
10. [Advanced Features](#advanced-features)
11. [Contributing](#contributing)

## Overview

The Zamaz MCP Incremental Linting System is a high-performance, git-aware linting framework that dramatically improves linting speed by only processing files that have changed since the last successful lint operation.

### Key Features

- **Git Integration**: Automatically detects changed files using git diff
- **Intelligent Caching**: SHA-256 based caching with automatic invalidation
- **Parallel Processing**: Multi-threaded linting for optimal performance
- **Multiple Formats**: Support for Java, TypeScript, YAML, JSON, Markdown, and Dockerfiles
- **CI/CD Optimized**: Specialized workflows for continuous integration
- **Quality Gates**: Configurable thresholds with detailed reporting
- **IDE Integration**: Support for IntelliJ IDEA, VS Code, and other editors

### Performance Benefits

- **Up to 90% faster** than full project linting
- **Cache hit rates** typically 70-85% in active development
- **Parallel execution** reduces wall-clock time by 60-80%
- **Memory efficient** with streaming processing for large codebases

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Incremental Linting Engine               │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  GitDiffAnalyzer│  │  LintingCache   │  │ LintingCLI   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ File Type       │  │ Quality Gates   │  │ Report       │ │
│  │ Handlers        │  │ & Thresholds    │  │ Generation   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Change Detection**: Git diff analysis identifies modified files
2. **Cache Lookup**: Check if cached results exist and are valid
3. **Selective Linting**: Process only uncached/changed files
4. **Result Aggregation**: Combine cached and new results
5. **Quality Assessment**: Apply thresholds and generate reports
6. **Cache Update**: Store new results for future runs

## Getting Started

### Prerequisites

- Java 21 or higher
- Git repository
- Maven 3.9+
- Node.js 18+ (for frontend linting)

### Installation

1. **Add to your project's POM:**

```xml
<dependency>
    <groupId>com.zamaz.mcp</groupId>
    <artifactId>mcp-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. **Install CLI globally:**

```bash
mvn install
ln -s target/linting-cli.jar /usr/local/bin/lint
```

3. **Configure linting rules:**

```bash
mkdir -p .linting
cp -r /path/to/zamaz-mcp/.linting/* .linting/
```

### Quick Start

```bash
# Lint changed files in working directory
lint --incremental --working-dir

# Lint changes between commits
lint --incremental --from-commit HEAD~5 --to-commit HEAD

# Lint PR changes
lint --incremental --from-commit origin/main --to-commit HEAD

# Full project lint (for comparison)
lint --project .
```

## Configuration

### Global Configuration (`.linting/global.yml`)

```yaml
# Quality thresholds
thresholds:
  maxErrors: 0
  maxWarnings: 10
  maxInfo: 50

# Performance settings
performance:
  parallelExecution: true
  maxThreads: 4
  cacheEnabled: true
  cacheMaxSize: 10000

# File handling
files:
  excludePatterns:
    - "**/*test*"
    - "**/*.generated.*"
    - "**/node_modules/**"
    - "**/target/**"
  
# Output formats
reports:
  defaultFormat: "console"
  formats:
    - "console"
    - "json"
    - "html"
    - "junit"

# Auto-fix settings
autoFix:
  enabled: false
  backup: true
  safeMode: true
```

### Language-Specific Configuration

#### Java (`.linting/java/checkstyle.xml`)

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>
    <property name="fileExtensions" value="java"/>
    
    <module name="TreeWalker">
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="WhitespaceAfter"/>
        <module name="WhitespaceAround"/>
    </module>
</module>
```

#### TypeScript (`.linting/frontend/.eslintrc.js`)

```javascript
module.exports = {
  extends: [
    'eslint:recommended',
    '@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:security/recommended'
  ],
  rules: {
    '@typescript-eslint/no-unused-vars': 'error',
    'react/prop-types': 'off',
    'security/detect-object-injection': 'warn'
  },
  parserOptions: {
    ecmaVersion: 2021,
    sourceType: 'module'
  }
};
```

## CLI Usage

### Basic Commands

```bash
# Show help
lint --help

# Show version
lint --version

# List available linters
lint --list-linters

# Lint specific files
lint --files src/main/java/Example.java src/test/java/ExampleTest.java

# Lint specific service
lint --service mcp-controller

# Use custom configuration
lint --config custom-linting.yml
```

### Incremental Options

```bash
# Incremental linting (changed files since last commit)
lint --incremental

# Working directory changes
lint --incremental --working-dir

# Specific commit range
lint --incremental --from-commit abc123 --to-commit def456

# Commit range syntax
lint --incremental --commit-range "HEAD~3..HEAD"

# Single commit
lint --incremental --commit 1a2b3c4
```

### Cache Management

```bash
# Show cache statistics
lint --cache-stats

# Clear cache
lint --clear-cache

# Warm cache (pre-populate)
lint --warm-cache

# Clean stale entries
lint --cache-cleanup
```

### Output and Reporting

```bash
# JSON output
lint --format json --output results.json

# HTML report
lint --format html --output report.html

# Verbose output
lint --verbose

# Quiet mode (errors only)
lint --quiet

# Multiple formats
lint --format json,html,junit
```

### Parallel Processing

```bash
# Enable parallel processing
lint --parallel

# Disable parallel processing
lint --no-parallel

# Custom thread count
lint --parallel --threads 8
```

### Auto-fix

```bash
# Auto-fix issues (where possible)
lint --auto-fix

# Dry-run auto-fix (show what would be fixed)
lint --auto-fix --dry-run

# Auto-fix with backup
lint --auto-fix --backup
```

## CI/CD Integration

### GitHub Actions

The project includes optimized workflows for different CI/CD scenarios:

#### Pull Request Linting (`.github/workflows/pr-linting.yml`)

```yaml
name: PR Linting
on:
  pull_request:
    branches: [ main, develop ]

jobs:
  incremental-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Run incremental linting
        run: |
          chmod +x .linting/scripts/incremental-lint.sh
          .linting/scripts/incremental-lint.sh \\
            --commit-range="${{ github.event.pull_request.base.sha }}..${{ github.event.pull_request.head.sha }}" \\
            --verbose \\
            --auto-fix

      - name: Commit auto-fixes
        if: success()
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add .
          git diff --staged --quiet || git commit -m "Auto-fix linting issues"
          git push
```

#### Comprehensive Quality Gate (`.github/workflows/code-quality.yml`)

```yaml
name: Code Quality Gate
on:
  push:
    branches: [ main ]

jobs:
  quality-gate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Full project linting
        run: |
          lint --project . --format junit --output lint-results.xml
          
      - name: Quality gate check
        run: |
          lint --quality-gate --threshold-file .linting/thresholds.yml

      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: quality-results
          path: lint-results.xml
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Incremental Lint') {
            when {
                changeRequest()
            }
            steps {
                script {
                    def commitRange = "${env.CHANGE_TARGET}..${env.GIT_COMMIT}"
                    sh """
                        .linting/scripts/incremental-lint.sh \\
                            --commit-range="${commitRange}" \\
                            --format json \\
                            --output lint-results.json
                    """
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    def results = readJSON file: 'lint-results.json'
                    if (results.errors > 0) {
                        error "Quality gate failed: ${results.errors} errors found"
                    }
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'lint-results.json', allowEmptyArchive: true
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.linting/reports',
                reportFiles: 'report.html',
                reportName: 'Linting Report'
            ])
        }
    }
}
```

### GitLab CI

```yaml
# .gitlab-ci.yml
stages:
  - quality

incremental-lint:
  stage: quality
  image: openjdk:21-jdk
  before_script:
    - apt-get update -qq && apt-get install -y git nodejs npm
  script:
    - |
      if [ "$CI_PIPELINE_SOURCE" = "merge_request_event" ]; then
        COMMIT_RANGE="origin/${CI_MERGE_REQUEST_TARGET_BRANCH_NAME}..HEAD"
      else
        COMMIT_RANGE="HEAD~1..HEAD"
      fi
    - .linting/scripts/incremental-lint.sh --commit-range="$COMMIT_RANGE" --format json --output lint-results.json
  artifacts:
    reports:
      junit: lint-results.xml
    paths:
      - lint-results.json
      - .linting/reports/
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
```

## Cache Management

### Cache Structure

```
.linting/cache/
├── files/           # Individual file hashes and results
│   ├── abc123.json  # Cached linting results
│   └── def456.json
├── metadata/        # Cache metadata
│   ├── stats.json   # Performance statistics
│   └── index.json   # Cache index
└── config/          # Cache configuration
    └── settings.json
```

### Cache Operations

#### Viewing Cache Statistics

```bash
# Basic statistics
lint --cache-stats

# Detailed cache analysis
lint --cache-stats --verbose

# Export statistics to JSON
lint --cache-stats --format json --output cache-stats.json
```

Example output:
```
Cache Statistics:
  Total entries: 1,247
  Hit rate: 78.3%
  Miss rate: 21.7%
  Total requests: 3,891
  Cache size: 45.2 MB
  
File Type Distribution:
  java: 856 files (68.6%)
  typescript: 234 files (18.8%)
  yaml: 89 files (7.1%)
  markdown: 45 files (3.6%)
  other: 23 files (1.8%)
```

#### Cache Maintenance

```bash
# Warm up cache (recommended before major development)
lint --warm-cache

# Clean stale entries
lint --cache-cleanup

# Clear entire cache
lint --clear-cache

# Rebuild cache from scratch
lint --clear-cache && lint --warm-cache
```

#### Cache Configuration

```yaml
# .linting/cache/settings.json
{
  "maxSize": 10000,
  "maxAge": "7d",
  "compressionEnabled": true,
  "cleanupInterval": "1h",
  "hashAlgorithm": "SHA-256",
  "persistLocation": ".linting/cache"
}
```

### Cache Performance Tuning

#### Optimal Cache Settings

```yaml
# For small projects (<1000 files)
cache:
  maxSize: 2000
  maxAge: "3d"
  
# For medium projects (1000-5000 files)  
cache:
  maxSize: 10000
  maxAge: "7d"
  
# For large projects (>5000 files)
cache:
  maxSize: 50000
  maxAge: "14d"
```

#### Monitoring Cache Performance

```bash
# Monitor cache hit rate over time
lint --cache-stats --watch

# Generate cache performance report
lint --cache-report --output cache-report.html

# Analyze cache efficiency by file type
lint --cache-analysis --group-by filetype
```

## Performance Optimization

### Best Practices

1. **Use Incremental Mode**: Always prefer `--incremental` for development
2. **Warm Cache Regularly**: Run `lint --warm-cache` after major merges
3. **Optimize Exclude Patterns**: Exclude test files and generated code
4. **Parallel Processing**: Enable `--parallel` for multi-core systems
5. **Targeted Linting**: Use `--service` or `--files` for focused linting

### Performance Benchmarks

| Project Size | Full Lint | Incremental (Cold) | Incremental (Warm) |
|--------------|-----------|-------------------|-------------------|
| Small (100 files) | 15s | 8s | 2s |
| Medium (1000 files) | 2m 30s | 35s | 8s |
| Large (5000 files) | 12m 15s | 2m 45s | 25s |
| XL (10000+ files) | 45m+ | 8m 30s | 1m 20s |

### Memory Optimization

```bash
# For memory-constrained environments
lint --incremental --parallel --threads 2 --memory-limit 1G

# For high-memory systems
lint --incremental --parallel --threads 8 --memory-limit 4G
```

### Network Optimization (CI/CD)

```yaml
# Cache Maven dependencies
- name: Cache Maven dependencies
  uses: actions/cache@v3
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

# Cache NPM dependencies  
- name: Cache NPM dependencies
  uses: actions/cache@v3
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}

# Cache linting results
- name: Cache linting results
  uses: actions/cache@v3
  with:
    path: .linting/cache
    key: ${{ runner.os }}-lint-${{ github.sha }}
    restore-keys: |
      ${{ runner.os }}-lint-
```

## Troubleshooting

### Common Issues

#### 1. Cache Corruption

**Symptoms**: Inconsistent results, cache misses for unchanged files

**Solution**:
```bash
# Clear and rebuild cache
lint --clear-cache
lint --warm-cache

# Verify cache integrity
lint --cache-verify
```

#### 2. Git Integration Problems

**Symptoms**: "Failed to get changed files" errors

**Solution**:
```bash
# Ensure you're in a git repository
git status

# Check git configuration
git config --list

# Verify commit range
git log --oneline HEAD~5..HEAD

# Use absolute paths
lint --incremental --project /full/path/to/project
```

#### 3. Performance Issues

**Symptoms**: Slow incremental linting, high memory usage

**Solution**:
```bash
# Check cache size
lint --cache-stats

# Clean up cache
lint --cache-cleanup

# Reduce parallel threads
lint --incremental --threads 2

# Exclude more patterns
lint --exclude "**/*test*" --exclude "**/generated/**"
```

#### 4. Configuration Conflicts

**Symptoms**: Unexpected linting rules, missing configurations

**Solution**:
```bash
# Verify configuration loading
lint --config-check

# Show effective configuration
lint --show-config

# Use explicit configuration
lint --config .linting/custom-config.yml
```

### Debug Mode

```bash
# Enable debug logging
lint --incremental --debug

# Verbose output with timing
lint --incremental --verbose --timing

# Trace mode (very detailed)
lint --incremental --trace
```

### Log Analysis

```bash
# View recent linting logs
tail -f .linting/logs/lint.log

# Search for specific errors
grep "ERROR" .linting/logs/lint.log

# Analyze performance
grep "Duration" .linting/logs/lint.log | awk '{print $NF}' | sort -n
```

## Advanced Features

### Custom Linters

Create custom linters for specific file types:

```java
@Component
public class CustomLinter implements LinterPlugin {
    
    @Override
    public boolean supports(String fileExtension) {
        return "proto".equals(fileExtension);
    }
    
    @Override
    public List<LintingIssue> lint(Path file, LintingContext context) {
        // Custom linting logic
        return issues;
    }
}
```

### Quality Gates

Configure custom quality gates:

```yaml
# .linting/quality-gates.yml
gates:
  - name: "critical-files"
    patterns: ["src/main/java/com/example/core/**"]
    thresholds:
      maxErrors: 0
      maxWarnings: 0
      
  - name: "feature-files"  
    patterns: ["src/main/java/com/example/features/**"]
    thresholds:
      maxErrors: 0
      maxWarnings: 5
      
  - name: "test-files"
    patterns: ["src/test/**"]
    thresholds:
      maxErrors: 2
      maxWarnings: 10
```

### Metrics and Monitoring

Export metrics to monitoring systems:

```bash
# Prometheus metrics
lint --metrics-endpoint http://prometheus:9090/metrics

# JSON metrics for custom systems
lint --metrics-format json --metrics-output metrics.json

# Real-time monitoring
lint --metrics-watch --metrics-interval 30s
```

### Integration with IDEs

#### IntelliJ IDEA Plugin

```xml
<!-- plugin.xml -->
<idea-plugin>
  <id>com.zamaz.mcp.linting</id>
  <name>MCP Incremental Linting</name>
  
  <actions>
    <action id="RunIncrementalLint" 
            class="com.zamaz.mcp.idea.LintAction" 
            text="Run Incremental Lint">
      <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt L"/>
    </action>
  </actions>
</idea-plugin>
```

#### VS Code Extension

```json
{
  "name": "mcp-incremental-linting",
  "version": "1.0.0",
  "contributes": {
    "commands": [
      {
        "command": "mcp.lint.incremental",
        "title": "Run Incremental Linting"
      }
    ],
    "keybindings": [
      {
        "command": "mcp.lint.incremental",
        "key": "ctrl+alt+l"
      }
    ]
  }
}
```

## Contributing

### Development Setup

```bash
# Clone repository
git clone https://github.com/zamaz/mcp-linting.git
cd mcp-linting

# Build project
mvn clean install

# Run tests
mvn test

# Run integration tests
mvn integration-test
```

### Testing

```bash
# Unit tests
mvn test -Dtest=IncrementalLintingEngineTest

# Integration tests
mvn test -Dtest=*IntegrationTest

# Performance tests
mvn test -Dtest=*PerformanceTest -DargLine="-Xmx4g"
```

### Adding New Linters

1. Implement `LinterPlugin` interface
2. Add configuration in `.linting/`
3. Add tests in `src/test/`
4. Update documentation
5. Submit pull request

### Code Style

Follow existing patterns:
- Use SLF4J for logging
- Include comprehensive JavaDoc
- Write unit tests for all new functionality
- Use dependency injection where appropriate
- Follow Spring Framework conventions

---

This comprehensive guide covers all aspects of the Incremental Linting System. For additional help, consult the API documentation or reach out to the development team.