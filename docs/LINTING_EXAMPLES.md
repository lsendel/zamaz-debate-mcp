# Linting Examples and Use Cases

## Table of Contents

1. [Basic Usage Examples](#basic-usage-examples)
2. [Development Workflow Examples](#development-workflow-examples)
3. [CI/CD Integration Examples](#cicd-integration-examples)
4. [Advanced Configuration Examples](#advanced-configuration-examples)
5. [Custom Linter Examples](#custom-linter-examples)
6. [Performance Optimization Examples](#performance-optimization-examples)
7. [Troubleshooting Examples](#troubleshooting-examples)

## Basic Usage Examples

### Example 1: First-time Setup

```bash
# 1. Initialize linting in your project
cd /path/to/your/project
mkdir -p .linting

# 2. Copy base configuration
cp -r /path/to/zamaz-mcp/.linting/* .linting/

# 3. Customize for your project
cat > .linting/global.yml << EOF
thresholds:
  maxErrors: 0
  maxWarnings: 5
  maxInfo: 20

performance:
  parallelExecution: true
  maxThreads: 4

files:
  excludePatterns:
    - "**/target/**"
    - "**/node_modules/**"
    - "**/*.generated.*"
EOF

# 4. Initial full lint (populates cache)
lint --project . --warm-cache

# 5. Test incremental linting
echo "// Test change" >> src/main/java/Example.java
lint --incremental --working-dir
```

### Example 2: Daily Development Workflow

```bash
# Morning: Check what needs linting after pulling latest
git pull origin main
lint --incremental --from-commit HEAD@{1} --to-commit HEAD

# During development: Lint your changes
lint --incremental --working-dir

# Before commit: Full check of changed files
git add .
lint --incremental --staged

# Quick fix common issues
lint --incremental --working-dir --auto-fix

# Final check before push
lint --incremental --from-commit origin/main --to-commit HEAD
```

### Example 3: Code Review Preparation

```bash
# Check all changes in your feature branch
lint --incremental --from-commit main --to-commit feature-branch

# Generate detailed report for review
lint --incremental --from-commit main --to-commit HEAD \\
     --format html --output code-review-report.html

# Quick summary for commit message
lint --incremental --from-commit HEAD~1 --to-commit HEAD \\
     --format summary
```

## Development Workflow Examples

### Example 4: Feature Development Cycle

```bash
#!/bin/bash
# feature-development.sh

# 1. Start feature branch
git checkout -b feature/new-api-endpoint
git push -u origin feature/new-api-endpoint

# 2. Pre-development cache warm-up
lint --warm-cache --service mcp-controller

# 3. Development loop
while [ "$development_active" = true ]; do
  # Make changes
  vim src/main/java/com/example/NewEndpoint.java
  
  # Quick lint check
  lint --incremental --working-dir --files src/main/java/com/example/NewEndpoint.java
  
  # Auto-fix if possible
  if [ $? -ne 0 ]; then
    lint --incremental --working-dir --auto-fix --files src/main/java/com/example/NewEndpoint.java
  fi
  
  # Continue development...
done

# 4. Pre-commit comprehensive check
git add .
lint --incremental --staged --format json --output pre-commit-lint.json

# 5. Commit if clean
if [ $(jq '.errors' pre-commit-lint.json) -eq 0 ]; then
  git commit -m "Add new API endpoint with linting checks"
else
  echo "Fix linting issues before committing"
  jq '.issues[] | select(.severity == "ERROR")' pre-commit-lint.json
fi
```

### Example 5: Multi-Service Development

```bash
#!/bin/bash
# multi-service-lint.sh

# Lint multiple services in parallel
services=("mcp-controller" "mcp-llm" "mcp-organization" "mcp-rag")

for service in "${services[@]}"; do
  (
    echo "Linting $service..."
    lint --service "$service" --incremental --working-dir \\
         --format json --output "lint-$service.json" &
  )
done

# Wait for all to complete
wait

# Aggregate results
echo "# Multi-Service Linting Report" > multi-service-report.md
echo "Generated: $(date)" >> multi-service-report.md
echo "" >> multi-service-report.md

for service in "${services[@]}"; do
  echo "## $service" >> multi-service-report.md
  
  if [ -f "lint-$service.json" ]; then
    errors=$(jq '.errors' "lint-$service.json")
    warnings=$(jq '.warnings' "lint-$service.json")
    echo "- Errors: $errors" >> multi-service-report.md
    echo "- Warnings: $warnings" >> multi-service-report.md
    
    if [ "$errors" -gt 0 ]; then
      echo "- Issues:" >> multi-service-report.md
      jq -r '.issues[] | select(.severity == "ERROR") | "  - " + .message + " (" + .file + ":" + (.line|tostring) + ")"' "lint-$service.json" >> multi-service-report.md
    fi
  fi
  echo "" >> multi-service-report.md
done

echo "Report generated: multi-service-report.md"
```

## CI/CD Integration Examples

### Example 6: GitHub Actions PR Workflow

```yaml
# .github/workflows/pr-incremental-lint.yml
name: PR Incremental Linting

on:
  pull_request:
    branches: [ main, develop ]
    types: [ opened, synchronize, reopened ]

jobs:
  incremental-lint:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'

      - name: Install linting tools
        run: |
          npm install -g eslint prettier markdownlint-cli
          pip install yamllint

      - name: Cache linting results
        uses: actions/cache@v3
        with:
          path: .linting/cache
          key: ${{ runner.os }}-lint-${{ hashFiles('**/*.java', '**/*.ts', '**/*.yml') }}
          restore-keys: |
            ${{ runner.os }}-lint-

      - name: Determine changed files
        id: changes
        run: |
          echo "base_sha=${{ github.event.pull_request.base.sha }}" >> $GITHUB_OUTPUT
          echo "head_sha=${{ github.event.pull_request.head.sha }}" >> $GITHUB_OUTPUT
          echo "commit_range=${{ github.event.pull_request.base.sha }}..${{ github.event.pull_request.head.sha }}" >> $GITHUB_OUTPUT

      - name: Run incremental linting
        id: lint
        run: |
          # Run incremental linting
          lint --incremental \\
               --from-commit ${{ steps.changes.outputs.base_sha }} \\
               --to-commit ${{ steps.changes.outputs.head_sha }} \\
               --format json \\
               --output lint-results.json \\
               --auto-fix

          # Check if auto-fixes were made
          if git diff --quiet; then
            echo "auto_fixes=false" >> $GITHUB_OUTPUT
          else
            echo "auto_fixes=true" >> $GITHUB_OUTPUT
          fi

      - name: Commit auto-fixes
        if: steps.lint.outputs.auto_fixes == 'true'
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action Auto-fix"
          git add .
          git commit -m "🤖 Auto-fix linting issues" || exit 0
          git push

      - name: Generate PR comment
        if: always()
        run: |
          mkdir -p pr-comments
          
          cat > pr-comments/lint-summary.md << 'EOF'
          ## 🔍 Incremental Linting Results
          
          **Commit Range**: `${{ steps.changes.outputs.commit_range }}`
          **Generated**: $(date -u)
          
          EOF
          
          if [ -f lint-results.json ]; then
            errors=$(jq '.errors' lint-results.json)
            warnings=$(jq '.warnings' lint-results.json)
            files_processed=$(jq '.filesProcessed' lint-results.json)
            
            echo "### Summary" >> pr-comments/lint-summary.md
            echo "- 📁 Files processed: $files_processed" >> pr-comments/lint-summary.md
            echo "- ❌ Errors: $errors" >> pr-comments/lint-summary.md
            echo "- ⚠️ Warnings: $warnings" >> pr-comments/lint-summary.md
            
            if [ "$errors" -gt 0 ]; then
              echo "" >> pr-comments/lint-summary.md
              echo "### ❌ Errors" >> pr-comments/lint-summary.md
              jq -r '.issues[] | select(.severity == "ERROR") | "- **" + .rule + "**: " + .message + "\\n  - File: `" + .file + ":" + (.line|tostring) + "`"' lint-results.json >> pr-comments/lint-summary.md
            fi
            
            if [ "$warnings" -gt 0 ]; then
              echo "" >> pr-comments/lint-summary.md
              echo "### ⚠️ Warnings" >> pr-comments/lint-summary.md
              jq -r '.issues[] | select(.severity == "WARNING") | "- **" + .rule + "**: " + .message + "\\n  - File: `" + .file + ":" + (.line|tostring) + "`"' lint-results.json >> pr-comments/lint-summary.md
            fi
          else
            echo "❌ No linting results found." >> pr-comments/lint-summary.md
          fi
          
          if [ "${{ steps.lint.outputs.auto_fixes }}" = "true" ]; then
            echo "" >> pr-comments/lint-summary.md
            echo "### 🤖 Auto-fixes Applied" >> pr-comments/lint-summary.md
            echo "Some issues were automatically fixed and committed." >> pr-comments/lint-summary.md
          fi

      - name: Comment PR
        uses: actions/github-script@v6
        if: always()
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          script: |
            const fs = require('fs');
            const path = 'pr-comments/lint-summary.md';
            
            if (fs.existsSync(path)) {
              const body = fs.readFileSync(path, 'utf8');
              
              // Look for existing comment
              const comments = await github.rest.issues.listComments({
                issue_number: context.issue.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
              });
              
              const existingComment = comments.data.find(
                comment => comment.body.includes('🔍 Incremental Linting Results')
              );
              
              if (existingComment) {
                // Update existing comment
                await github.rest.issues.updateComment({
                  comment_id: existingComment.id,
                  owner: context.repo.owner,
                  repo: context.repo.repo,
                  body: body
                });
              } else {
                // Create new comment
                await github.rest.issues.createComment({
                  issue_number: context.issue.number,
                  owner: context.repo.owner,
                  repo: context.repo.repo,
                  body: body
                });
              }
            }

      - name: Upload detailed results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: lint-results-${{ github.event.pull_request.number }}
          path: |
            lint-results.json
            pr-comments/

      - name: Fail on errors
        if: always()
        run: |
          if [ -f lint-results.json ]; then
            errors=$(jq '.errors' lint-results.json)
            if [ "$errors" -gt 0 ]; then
              echo "❌ Linting failed with $errors errors"
              exit 1
            fi
          fi
```

### Example 7: Jenkins Pipeline with Quality Gates

```groovy
// Jenkinsfile
pipeline {
    agent any
    
    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21'
        PATH = "${JAVA_HOME}/bin:${PATH}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup Cache') {
            steps {
                script {
                    // Restore cache from previous builds
                    if (fileExists('.linting/cache.tar.gz')) {
                        sh 'tar -xzf .linting/cache.tar.gz -C .linting/ || true'
                    }
                }
            }
        }
        
        stage('Incremental Lint') {
            parallel {
                stage('Java Services') {
                    steps {
                        script {
                            def commitRange = "${env.CHANGE_TARGET}..${env.GIT_COMMIT}"
                            if (env.CHANGE_TARGET == null) {
                                commitRange = "HEAD~1..HEAD"
                            }
                            
                            sh """
                                lint --incremental \\
                                     --commit-range "${commitRange}" \\
                                     --include-pattern "**/*.java" \\
                                     --format json \\
                                     --output java-lint-results.json \\
                                     --parallel --threads 4
                            """
                        }
                    }
                }
                
                stage('TypeScript Frontend') {
                    steps {
                        script {
                            def commitRange = "${env.CHANGE_TARGET}..${env.GIT_COMMIT}"
                            if (env.CHANGE_TARGET == null) {
                                commitRange = "HEAD~1..HEAD"
                            }
                            
                            sh """
                                lint --incremental \\
                                     --commit-range "${commitRange}" \\
                                     --include-pattern "debate-ui/**/*.{ts,tsx,js,jsx}" \\
                                     --format json \\
                                     --output frontend-lint-results.json
                            """
                        }
                    }
                }
                
                stage('Configuration Files') {
                    steps {
                        script {
                            def commitRange = "${env.CHANGE_TARGET}..${env.GIT_COMMIT}"
                            if (env.CHANGE_TARGET == null) {
                                commitRange = "HEAD~1..HEAD"
                            }
                            
                            sh """
                                lint --incremental \\
                                     --commit-range "${commitRange}" \\
                                     --include-pattern "**/*.{yml,yaml,json}" \\
                                     --format json \\
                                     --output config-lint-results.json
                            """
                        }
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    // Combine results
                    sh '''
                        jq -s '
                            {
                                errors: (map(.errors) | add),
                                warnings: (map(.warnings) | add),
                                filesProcessed: (map(.filesProcessed) | add),
                                issues: (map(.issues) | add)
                            }' java-lint-results.json frontend-lint-results.json config-lint-results.json > combined-results.json
                    '''
                    
                    // Read combined results
                    def results = readJSON file: 'combined-results.json'
                    
                    // Apply quality gates
                    def qualityGates = [
                        [name: 'No Errors', condition: { results.errors == 0 }, severity: 'error'],
                        [name: 'Max 10 Warnings', condition: { results.warnings <= 10 }, severity: 'warning'],
                        [name: 'Max 100 Files', condition: { results.filesProcessed <= 100 }, severity: 'info']
                    ]
                    
                    def failures = []
                    def warnings = []
                    
                    qualityGates.each { gate ->
                        if (!gate.condition()) {
                            if (gate.severity == 'error') {
                                failures.add(gate.name)
                            } else if (gate.severity == 'warning') {
                                warnings.add(gate.name)
                            }
                        }
                    }
                    
                    // Generate report
                    writeFile file: 'quality-gate-report.md', text: """
# Quality Gate Report

## Summary
- Errors: ${results.errors}
- Warnings: ${results.warnings}
- Files Processed: ${results.filesProcessed}

## Quality Gate Results
${qualityGates.collect { gate ->
    def status = gate.condition() ? '✅ PASS' : '❌ FAIL'
    "- ${status}: ${gate.name}"
}.join('\\n')}

## Details
${failures.empty ? '' : "### Failures\\n" + failures.collect { "- ${it}" }.join('\\n')}
${warnings.empty ? '' : "### Warnings\\n" + warnings.collect { "- ${it}" }.join('\\n')}
                    """
                    
                    // Fail build if quality gate failures
                    if (!failures.empty) {
                        error("Quality gate failed: ${failures.join(', ')}")
                    }
                    
                    // Set build status
                    if (!warnings.empty) {
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('Cache Results') {
            steps {
                sh 'tar -czf .linting/cache.tar.gz .linting/cache/ || true'
            }
        }
    }
    
    post {
        always {
            // Archive results
            archiveArtifacts artifacts: '*-lint-results.json,combined-results.json,quality-gate-report.md', allowEmptyArchive: true
            
            // Publish HTML report
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.linting/reports',
                reportFiles: '*.html',
                reportName: 'Linting Report'
            ])
            
            // Post to Slack/Teams
            script {
                if (env.CHANGE_ID) {
                    def results = readJSON file: 'combined-results.json'
                    def message = """
Linting Results for PR #${env.CHANGE_ID}:
• Errors: ${results.errors}
• Warnings: ${results.warnings}
• Files: ${results.filesProcessed}
${results.errors > 0 ? '❌ Quality gate failed' : '✅ Quality gate passed'}
                    """
                    
                    // slackSend channel: '#development', message: message
                }
            }
        }
        
        success {
            echo '✅ All linting checks passed!'
        }
        
        failure {
            echo '❌ Linting checks failed!'
        }
        
        unstable {
            echo '⚠️ Linting checks passed with warnings!'
        }
    }
}
```

## Advanced Configuration Examples

### Example 8: Multi-Environment Configuration

```yaml
# .linting/environments/development.yml
extends: ../global.yml

thresholds:
  maxErrors: 5      # More lenient for development
  maxWarnings: 20
  maxInfo: 100

performance:
  parallelExecution: true
  maxThreads: 8     # Use more threads for developer machines

autoFix:
  enabled: true     # Auto-fix in development
  backup: true
  safeMode: true

cache:
  maxSize: 5000     # Smaller cache for local development
  maxAge: "3d"
```

```yaml
# .linting/environments/staging.yml
extends: ../global.yml

thresholds:
  maxErrors: 2      # Stricter than development
  maxWarnings: 10
  maxInfo: 50

performance:
  parallelExecution: true
  maxThreads: 4

autoFix:
  enabled: false    # No auto-fix in staging

reports:
  formats:
    - "json"
    - "junit"
    - "html"
```

```yaml
# .linting/environments/production.yml
extends: ../global.yml

thresholds:
  maxErrors: 0      # Zero tolerance in production
  maxWarnings: 0
  maxInfo: 0

performance:
  parallelExecution: true
  maxThreads: 2     # Conservative resource usage

autoFix:
  enabled: false

qualityGates:
  enabled: true
  failOnThresholdExceeded: true
  
monitoring:
  metricsEnabled: true
  alerting: true
```

Usage with environments:
```bash
# Development
lint --config .linting/environments/development.yml --incremental

# Staging
lint --config .linting/environments/staging.yml --incremental

# Production
lint --config .linting/environments/production.yml --project .
```

### Example 9: Service-Specific Configuration

```yaml
# .linting/services/mcp-controller.yml
extends: ../global.yml

files:
  includePatterns:
    - "mcp-controller/**/*.java"
    - "mcp-common/**/*.java"
  excludePatterns:
    - "**/test/**"
    - "**/*Test.java"

rules:
  java:
    checkstyle:
      - "LineLength.max=120"
      - "MethodLength.max=50"
    spotbugs:
      - "excludeFilterFile=.linting/java/controller-spotbugs-exclude.xml"
    pmd:
      - "rulesets/java/quickstart.xml"

thresholds:
  maxErrors: 0
  maxWarnings: 3    # Controller is critical, minimal warnings

qualityGates:
  - name: "Controller Security"
    rules: ["security.*"]
    maxViolations: 0
  - name: "Performance"
    rules: ["performance.*"]
    maxViolations: 2
```

```yaml
# .linting/services/debate-ui.yml
extends: ../global.yml

files:
  includePatterns:
    - "debate-ui/src/**/*.{ts,tsx,js,jsx}"
  excludePatterns:
    - "debate-ui/node_modules/**"
    - "debate-ui/build/**"
    - "**/*.test.{ts,tsx,js,jsx}"

rules:
  typescript:
    eslint:
      - "@typescript-eslint/no-unused-vars=error"
      - "react/prop-types=off"
    prettier:
      - "semi=true"
      - "singleQuote=true"

thresholds:
  maxErrors: 0
  maxWarnings: 10   # Frontend can have more styling warnings

qualityGates:
  - name: "React Best Practices"
    rules: ["react.*"]
    maxViolations: 5
  - name: "Security"
    rules: ["security.*"]
    maxViolations: 0
```

Usage:
```bash
# Lint specific service
lint --service mcp-controller --incremental
lint --service debate-ui --incremental

# Lint multiple services
lint --service mcp-controller,mcp-llm --incremental

# Override service config
lint --service mcp-controller --override "thresholds.maxWarnings=0"
```

## Custom Linter Examples

### Example 10: Custom Security Linter

```java
// CustomSecurityLinter.java
@Component
public class CustomSecurityLinter implements LinterPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomSecurityLinter.class);
    
    private final List<SecurityRule> securityRules = Arrays.asList(
        new HardcodedSecretRule(),
        new SqlInjectionRule(),
        new XssVulnerabilityRule(),
        new InsecureRandomRule()
    );
    
    @Override
    public boolean supports(String fileExtension) {
        return "java".equals(fileExtension) || "ts".equals(fileExtension) || "js".equals(fileExtension);
    }
    
    @Override
    public List<LintingIssue> lint(Path file, LintingContext context) {
        List<LintingIssue> issues = new ArrayList<>();
        
        try {
            String content = Files.readString(file);
            List<String> lines = content.lines().collect(Collectors.toList());
            
            for (SecurityRule rule : securityRules) {
                if (rule.isApplicable(file, content)) {
                    issues.addAll(rule.check(file, lines, context));
                }
            }
            
        } catch (IOException e) {
            logger.error("Error reading file {}: {}", file, e.getMessage());
        }
        
        return issues;
    }
    
    @Override
    public String getName() {
        return "custom-security";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    // Security rules implementation
    private static class HardcodedSecretRule implements SecurityRule {
        private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(password|secret|key|token)\\s*[=:]\\s*[\"']([^\"']{8,})[\"']",
            Pattern.CASE_INSENSITIVE
        );
        
        @Override
        public boolean isApplicable(Path file, String content) {
            return content.contains("password") || content.contains("secret") || 
                   content.contains("key") || content.contains("token");
        }
        
        @Override
        public List<LintingIssue> check(Path file, List<String> lines, LintingContext context) {
            List<LintingIssue> issues = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher matcher = SECRET_PATTERN.matcher(line);
                
                if (matcher.find()) {
                    issues.add(createSecurityIssue(
                        file, i + 1, matcher.start(), matcher.end(),
                        "Hardcoded secret detected",
                        "Move secrets to environment variables or secure configuration",
                        "HARDCODED_SECRET"
                    ));
                }
            }
            
            return issues;
        }
    }
    
    private static LintingIssue createSecurityIssue(Path file, int line, int startColumn, 
            int endColumn, String message, String suggestion, String ruleId) {
        return new LintingIssue.Builder()
            .file(file.toString())
            .line(line)
            .column(startColumn)
            .endColumn(endColumn)
            .severity(LintingSeverity.ERROR)
            .message(message)
            .rule(ruleId)
            .category("security")
            .suggestion(suggestion)
            .build();
    }
}
```

Configuration for custom linter:
```yaml
# .linting/custom-linters.yml
linters:
  - name: "custom-security"
    enabled: true
    class: "com.zamaz.mcp.linting.CustomSecurityLinter"
    configuration:
      strictMode: true
      excludeTestFiles: true
      rules:
        hardcodedSecrets: "error"
        sqlInjection: "error"
        xssVulnerability: "warning"
        insecureRandom: "info"
```

## Performance Optimization Examples

### Example 11: Large Codebase Optimization

```bash
#!/bin/bash
# optimize-large-codebase.sh

PROJECT_ROOT="/path/to/large/project"
cd "$PROJECT_ROOT"

echo "Optimizing linting for large codebase..."

# 1. Create optimized exclusion patterns
cat > .linting/large-project-excludes.yml << EOF
files:
  excludePatterns:
    # Build artifacts
    - "**/target/**"
    - "**/build/**"
    - "**/dist/**"
    - "**/.gradle/**"
    
    # Dependencies
    - "**/node_modules/**"
    - "**/vendor/**"
    - "**/.m2/**"
    
    # Generated code
    - "**/*.generated.*"
    - "**/generated/**"
    - "**/*_pb.java"
    - "**/*_pb.ts"
    
    # Test resources
    - "**/test-data/**"
    - "**/test-resources/**"
    - "**/*.test.data"
    
    # IDE files
    - "**/.idea/**"
    - "**/.vscode/**"
    - "**/*.iml"
    
    # Large media files
    - "**/*.{png,jpg,jpeg,gif,pdf,zip,tar,gz}"
    
    # Minified files
    - "**/*.min.{js,css}"
    - "**/*.bundle.{js,css}"
EOF

# 2. Configure optimal cache settings
cat > .linting/cache-optimization.yml << EOF
cache:
  maxSize: 100000           # Large cache for big projects
  maxAge: "14d"            # Keep cache longer
  compressionEnabled: true  # Compress cache entries
  cleanupInterval: "4h"    # Clean up more frequently
  
  # Use faster storage for cache
  location: "/dev/shm/lint-cache"  # RAM disk on Linux
  
  # Optimize for SSD
  ioOptimization: true
  batchWrites: true
EOF

# 3. Create performance-optimized configuration
cat > .linting/performance.yml << EOF
extends: 
  - global.yml
  - large-project-excludes.yml
  - cache-optimization.yml

performance:
  parallelExecution: true
  maxThreads: 16           # Use all available cores
  
  # Batch processing for efficiency
  batchSize: 200
  streamProcessing: true
  
  # Memory optimization
  maxMemoryUsage: "8g"
  gcOptimization: true
  
  # I/O optimization
  ioThreads: 4
  readBufferSize: "64k"
  writeBufferSize: "64k"

# Only run essential linters for performance
linters:
  java:
    - checkstyle
    - spotbugs         # Skip PMD for speed
  typescript:
    - eslint           # Skip prettier in incremental mode
  yaml:
    - yamllint
EOF

# 4. Setup RAM disk for cache (Linux)
if [ "$(uname)" = "Linux" ]; then
  sudo mkdir -p /dev/shm/lint-cache
  sudo chown $(whoami):$(whoami) /dev/shm/lint-cache
  chmod 755 /dev/shm/lint-cache
fi

# 5. Warm cache with parallel processing
echo "Warming cache with parallel processing..."
lint --config .linting/performance.yml \\
     --warm-cache \\
     --parallel \\
     --threads 16 \\
     --verbose

# 6. Create optimized incremental lint script
cat > scripts/fast-incremental-lint.sh << 'EOF'
#!/bin/bash
set -e

# Check if we're in CI/CD or development
if [ -n "$CI" ]; then
  THREADS=4
  MEMORY="2g"
else
  THREADS=$(nproc)
  MEMORY="8g"
fi

# Set JVM options for performance
export JAVA_OPTS="-Xmx$MEMORY -XX:+UseG1GC -XX:+UseStringDeduplication -server"

# Run optimized incremental linting
lint --config .linting/performance.yml \\
     --incremental \\
     --parallel \\
     --threads $THREADS \\
     --format json \\
     --output lint-results.json \\
     "$@"

echo "Fast incremental linting completed"
EOF

chmod +x scripts/fast-incremental-lint.sh

echo "Optimization complete!"
echo "Usage: ./scripts/fast-incremental-lint.sh --working-dir"
```

### Example 12: CI/CD Performance Optimization

```yaml
# .github/workflows/optimized-lint.yml
name: Optimized Incremental Linting

on:
  pull_request:
    branches: [ main ]

jobs:
  fast-incremental-lint:
    runs-on: ubuntu-latest
    
    # Use faster runners for large projects
    # runs-on: self-hosted-large
    
    steps:
      - name: Checkout with minimal history
        uses: actions/checkout@v4
        with:
          fetch-depth: 50  # Minimal history for speed
          
      - name: Setup Java with custom options
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'
          # Custom JVM options for performance
          java-package: 'jdk'
          
      - name: Configure build cache
        uses: actions/cache@v3
        with:
          path: |
            ~/.m2/repository
            ~/.gradle/caches
            .linting/cache
          key: ${{ runner.os }}-build-${{ hashFiles('**/*.java', '**/*.ts', '**/pom.xml', '**/package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-build-
            
      - name: Setup RAM disk for cache
        run: |
          sudo mkdir -p /dev/shm/lint-cache
          sudo chown runner:runner /dev/shm/lint-cache
          ln -s /dev/shm/lint-cache .linting/cache-fast
          
      - name: Optimize system for linting
        run: |
          # Increase file descriptor limits
          ulimit -n 65536
          
          # Set JVM options for CI
          echo "JAVA_OPTS=-Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -server" >> $GITHUB_ENV
          
          # Optimize git for large repos
          git config core.preloadindex true
          git config core.fscache true
          git config gc.auto 0
          
      - name: Smart file change detection
        id: changes
        run: |
          # Get changed files by type for targeted linting
          base_sha="${{ github.event.pull_request.base.sha }}"
          head_sha="${{ github.event.pull_request.head.sha }}"
          
          # Count changes by type
          java_changes=$(git diff --name-only $base_sha..$head_sha | grep -c '\.java$' || echo 0)
          ts_changes=$(git diff --name-only $base_sha..$head_sha | grep -c '\.(ts|tsx|js|jsx)$' || echo 0)
          config_changes=$(git diff --name-only $base_sha..$head_sha | grep -c '\.(yml|yaml|json)$' || echo 0)
          
          echo "java_changes=$java_changes" >> $GITHUB_OUTPUT
          echo "ts_changes=$ts_changes" >> $GITHUB_OUTPUT
          echo "config_changes=$config_changes" >> $GITHUB_OUTPUT
          echo "total_changes=$((java_changes + ts_changes + config_changes))" >> $GITHUB_OUTPUT
          
          # Determine strategy based on change volume
          if [ $((java_changes + ts_changes + config_changes)) -gt 100 ]; then
            echo "strategy=parallel-batch" >> $GITHUB_OUTPUT
          else
            echo "strategy=parallel-single" >> $GITHUB_OUTPUT
          fi
          
      - name: Parallel linting (high volume)
        if: steps.changes.outputs.strategy == 'parallel-batch'
        run: |
          # Process different file types in parallel jobs
          (
            if [ "${{ steps.changes.outputs.java_changes }}" -gt 0 ]; then
              lint --incremental \\
                   --from-commit ${{ github.event.pull_request.base.sha }} \\
                   --to-commit ${{ github.event.pull_request.head.sha }} \\
                   --include-pattern "**/*.java" \\
                   --cache-dir .linting/cache-fast \\
                   --parallel --threads 4 \\
                   --format json --output java-results.json
            fi
          ) &
          
          (
            if [ "${{ steps.changes.outputs.ts_changes }}" -gt 0 ]; then
              lint --incremental \\
                   --from-commit ${{ github.event.pull_request.base.sha }} \\
                   --to-commit ${{ github.event.pull_request.head.sha }} \\
                   --include-pattern "**/*.{ts,tsx,js,jsx}" \\
                   --cache-dir .linting/cache-fast \\
                   --parallel --threads 2 \\
                   --format json --output ts-results.json
            fi
          ) &
          
          # Wait for all background jobs
          wait
          
          # Combine results
          jq -s 'reduce .[] as $item ({}; 
            {
              errors: (.errors // 0) + ($item.errors // 0),
              warnings: (.warnings // 0) + ($item.warnings // 0),
              filesProcessed: (.filesProcessed // 0) + ($item.filesProcessed // 0),
              issues: (.issues // []) + ($item.issues // [])
            })' java-results.json ts-results.json > combined-results.json
            
      - name: Standard parallel linting (low volume)
        if: steps.changes.outputs.strategy == 'parallel-single'
        run: |
          lint --incremental \\
               --from-commit ${{ github.event.pull_request.base.sha }} \\
               --to-commit ${{ github.event.pull_request.head.sha }} \\
               --cache-dir .linting/cache-fast \\
               --parallel --threads 6 \\
               --format json --output combined-results.json
               
      - name: Performance metrics
        if: always()
        run: |
          echo "### 🚀 Performance Metrics" >> performance-report.md
          echo "- Total files changed: ${{ steps.changes.outputs.total_changes }}" >> performance-report.md
          echo "- Java files: ${{ steps.changes.outputs.java_changes }}" >> performance-report.md
          echo "- TypeScript files: ${{ steps.changes.outputs.ts_changes }}" >> performance-report.md
          echo "- Config files: ${{ steps.changes.outputs.config_changes }}" >> performance-report.md
          echo "- Strategy used: ${{ steps.changes.outputs.strategy }}" >> performance-report.md
          
          if [ -f combined-results.json ]; then
            duration=$(jq -r '.durationMs // 0' combined-results.json)
            echo "- Linting duration: ${duration}ms" >> performance-report.md
            
            # Calculate performance score
            files_per_second=$(echo "scale=2; ${{ steps.changes.outputs.total_changes }} / ($duration / 1000)" | bc -l)
            echo "- Processing speed: ${files_per_second} files/second" >> performance-report.md
          fi
```

This comprehensive examples guide demonstrates real-world usage patterns, advanced configurations, and performance optimizations for the incremental linting system. Each example includes practical code that can be adapted to specific project needs.