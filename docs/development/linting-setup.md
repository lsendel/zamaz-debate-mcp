# Linting Setup and Usage Guide

This guide covers the comprehensive linting system implemented for the Zamaz
Debate MCP Services project.

## Overview

The project uses a multi-layered linting approach covering:

- **Java**: Checkstyle, SpotBugs, PMD
- **TypeScript/React**: ESLint, Prettier
- **Configuration Files**: YAML, JSON, Docker
- **Documentation**: Markdown, Link checking

## Quick Start

### 1. Install Required Tools

```bash
# Install linting tools
make lint-setup

# Or install manually:
npm install -g markdownlint-cli markdown-link-check
pip install yamllint
brew install hadolint  # macOS
```

### 2. Run Linting

```bash
# Lint everything
make lint-all

# Lint specific components
make lint-java
make lint-frontend
make lint-config
make lint-docs

# Auto-fix issues where possible
make lint-fix

# Generate comprehensive report
make lint-report
```

## IDE Integration

### VS Code Setup

The project includes comprehensive VS Code configuration:

- **Extensions**: Automatically suggests required extensions
- **Settings**: Pre-configured linting and formatting rules
- **Tasks**: Quick access to linting commands via Ctrl+Shift+P
- **Launch Configurations**: Debug configurations for all services

#### Key Features

- **Format on Save**: Automatically formats code when saving
- **Real-time Linting**: Shows issues as you type
- **Auto-fix**: Fixes many issues automatically
- **Problem Panel**: Centralized view of all linting issues

#### VS Code Commands

- `Ctrl+Shift+P` → "Tasks: Run Task" → Select linting task
- `Ctrl+Shift+P` → "ESLint: Fix all auto-fixable Problems"
- `Shift+Alt+F` → Format current file

### IntelliJ IDEA Setup

For IntelliJ IDEA users:

1. **Checkstyle Plugin**:

   - Install Checkstyle-IDEA plugin
   - Configure: Settings → Tools → Checkstyle
   - Add configuration file: `.linting/java/checkstyle.xml`

2. **SpotBugs Plugin**:

   - Install SpotBugs plugin
   - Configure exclusion file: `.linting/java/spotbugs-exclude.xml`

3. **ESLint Integration**:
   - Enable ESLint in Settings → Languages & Frameworks → JavaScript → Code
     Quality Tools
   - Set configuration file: `.linting/frontend/.eslintrc.js`

## Configuration Files

### Java Linting

#### Checkstyle (`.linting/java/checkstyle.xml`)

- Code style and formatting rules
- Microservice-specific suppressions
- Spring Boot compatibility

#### SpotBugs (`.linting/java/spotbugs-exclude.xml`)

- Bug pattern detection
- Lombok compatibility
- Spring framework exclusions

#### PMD (`.linting/java/pmd.xml`)

- Code quality rules
- Performance checks
- Best practices enforcement

### Frontend Linting

#### ESLint (`.linting/frontend/.eslintrc.js`)

- TypeScript/React rules
- Security checks
- Accessibility validation
- Import organization

#### Prettier (`.linting/frontend/.prettierrc`)

- Code formatting
- Consistent style
- Multi-language support

### Configuration File Linting

#### YAML (`.linting/config/yaml-lint.yml`)

- Syntax validation
- Formatting rules
- Docker Compose compatibility

#### JSON Schema (`.linting/config/json-schema.json`)

- Structure validation
- Type checking
- Common file formats

### Documentation Linting

#### Markdownlint (`.linting/docs/markdownlint.json`)

- Markdown formatting
- Consistency rules
- Link validation

## Command Reference

### Make Commands

```bash
# Core linting commands
make lint-all              # Run all linting checks
make lint-java             # Lint Java code only
make lint-frontend         # Lint React TypeScript code
make lint-config           # Lint configuration files
make lint-docs             # Lint documentation

# Utility commands
make lint-fix              # Auto-fix issues where possible
make lint-report           # Generate comprehensive report
make lint-setup            # Install linting tools
make lint-service-<name>   # Lint specific service

# Examples
make lint-service-mcp-llm  # Lint only the LLM service
```

### Maven Commands

```bash
# Java linting via Maven
mvn checkstyle:check       # Run Checkstyle
mvn spotbugs:check         # Run SpotBugs
mvn pmd:check             # Run PMD
mvn clean compile -P code-quality  # Run all quality checks
```

### NPM Commands (Frontend)

```bash
cd debate-ui

# Linting commands
npm run lint              # Run ESLint
npm run lint:fix          # Fix ESLint issues
npm run lint:check        # Check without fixing
npm run format            # Format with Prettier
npm run format:check      # Check formatting
npm run type-check        # TypeScript validation
npm run lint-all          # Run all checks
```

## Quality Thresholds

The project enforces quality thresholds:

- **Errors**: 0 allowed (build fails)
- **Warnings**: Maximum 10 per service
- **Code Coverage**: Minimum 80%
- **Complexity**: Maximum 10 per method

## Auto-fixing

Many issues can be automatically fixed:

### Java

- Import organization
- Basic formatting (via IDE)

### Frontend

- ESLint auto-fixable rules
- Prettier formatting
- Import sorting

### Configuration

- YAML formatting
- JSON formatting

## Continuous Integration

Linting is integrated into the CI/CD pipeline:

```yaml
# GitHub Actions example
- name: Run Linting
  run: make lint-all

- name: Upload Linting Reports
  uses: actions/upload-artifact@v4
  with:
    name: linting-reports
    path: .linting/reports/
```

## Troubleshooting

### Common Issues

#### Java Linting Fails

```bash
# Check Maven configuration
mvn validate

# Run with verbose output
mvn checkstyle:check -X

# Check configuration files exist
ls -la .linting/java/
```

#### Frontend Linting Fails

```bash
# Install dependencies
cd debate-ui && npm install

# Check ESLint configuration
npx eslint --print-config src/App.tsx

# Run with debug output
DEBUG=eslint:* npm run lint
```

#### Configuration File Issues

```bash
# Check YAML syntax
yamllint --version
yamllint -c .linting/config/yaml-lint.yml .

# Validate JSON files
find . -name "*.json" -exec python -m json.tool {} \;
```

### Performance Issues

If linting is slow:

1. **Disable parallel execution**: `make lint-all PARALLEL=false`
2. **Exclude large directories**: Add patterns to `.linting/global.yml`
3. **Use incremental linting**: Only lint changed files

### IDE Integration Issues

#### VS Code

- Reload window: `Ctrl+Shift+P` → "Developer: Reload Window"
- Check extension status: View → Extensions
- Verify settings: `Ctrl+,` → Search for "eslint" or "checkstyle"

#### IntelliJ IDEA

- Invalidate caches: File → Invalidate Caches and Restart
- Check plugin status: Settings → Plugins
- Verify configuration: Settings → Editor → Inspections

## Best Practices

### Development Workflow

1. **Before Committing**:

   ```bash
   make lint-all
   make lint-fix  # If issues found
   ```

2. **During Development**:

   - Enable format-on-save in your IDE
   - Fix linting issues as they appear
   - Use auto-fix features liberally

3. **Code Reviews**:
   - Linting should pass before review
   - Focus review on logic, not style
   - Use linting reports for context

### Custom Rules

To add custom rules:

1. **Java**: Modify `.linting/java/checkstyle.xml`
2. **Frontend**: Update `.linting/frontend/.eslintrc.js`
3. **Documentation**: Edit `.linting/docs/markdownlint.json`

### Suppressing Rules

When necessary, suppress rules appropriately:

#### Java

```java
@SuppressWarnings("checkstyle:MethodLength")
public void longMethod() {
    // Implementation
}
```

#### TypeScript

```typescript
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const data: any = response.data;
```

#### Markdown

```markdown
<!-- markdownlint-disable MD013 -->

This is a very long line that exceeds the normal line length limit but is
necessary for this specific case.

<!-- markdownlint-enable MD013 -->
```

## Support

For issues with the linting system:

1. Check this documentation
2. Review configuration files in `.linting/`
3. Run `make lint-setup` to ensure tools are installed
4. Check the project's issue tracker
5. Consult tool-specific documentation:
   - [Checkstyle](https://checkstyle.sourceforge.io/)
   - [SpotBugs](https://spotbugs.github.io/)
   - [ESLint](https://eslint.org/)
   - [Prettier](https://prettier.io/)
