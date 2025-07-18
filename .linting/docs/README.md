# Project Linter Documentation

## Overview

The Project Linter system provides comprehensive code quality enforcement across
the entire Zamaz Debate MCP Services ecosystem. It integrates multiple linting
tools to ensure consistent code quality, security, and maintainability across
Java microservices, React frontend, configuration files, and documentation.

## Quick Start

### Running Linting

To run incremental linting (only on changed files):

```bash
# Run linting on all changed files
make lint

# Run linting with auto-fix where possible
make lint-fix

# Run linting on specific file types
make lint-java
make lint-frontend
make lint-config
make lint-docs

# Run linting on a specific commit range
make lint-commit-range
# When prompted, enter a commit range like HEAD~3..HEAD
```

### Pre-commit Hooks

The linting system is integrated with pre-commit hooks to automatically check
code quality before commits:

```bash
# Install pre-commit hooks (only needed once)
pre-commit install

# Run pre-commit hooks manually
pre-commit run --all-files
```

## Linting Configuration

### Global Configuration

Global linting rules are defined in `.linting/global.yml`. This file contains
settings that apply to all linting tools and services.

### Language-Specific Configuration

#### Java

Java linting uses the following tools:

- **Checkstyle**: Code style and formatting (`.linting/java/checkstyle.xml`)
- **SpotBugs**: Bug pattern detection (`.linting/java/spotbugs-exclude.xml`)
- **PMD**: Code quality and best practices (`.linting/java/pmd.xml`)

#### TypeScript/React

Frontend linting uses:

- **ESLint**: JavaScript/TypeScript code quality
  (`.linting/frontend/.eslintrc.js`)
- **Prettier**: Code formatting (`.linting/frontend/.prettierrc`)
- **TypeScript**: Type checking (`.linting/frontend/tsconfig.lint.json`)

#### Configuration Files

Configuration file linting includes:

- **YAML Lint**: YAML syntax and formatting (`.linting/config/yaml-lint.yml`)
- **JSON Schema**: JSON validation (`.linting/config/json-schema.json`)
- **Dockerfile**: Docker best practices (`.linting/config/dockerfile-rules.yml`)
- **Maven POM**: XML validation (`.linting/config/maven-pom-rules.xml`)

#### Documentation

Documentation linting includes:

- **Markdownlint**: Markdown formatting (`.linting/docs/markdownlint.json`)
- **Link Check**: Broken link detection (`.linting/docs/link-check.json`)

### Service-Specific Overrides

Each service can have its own linting configuration overrides in
`.linting/services/{service-name}/`. These configurations extend the global
rules but can be customized for specific service needs.

## IDE Integration

### VS Code

The project includes VS Code settings for seamless linting integration. Install
the recommended extensions:

1. Java: `ms-vscode.vscode-java-pack`
2. ESLint: `dbaeumer.vscode-eslint`
3. Prettier: `esbenp.prettier-vscode`
4. Markdownlint: `davidanson.vscode-markdownlint`

The workspace settings are configured to use the project's linting rules.

### IntelliJ IDEA

For IntelliJ IDEA:

1. Install the Checkstyle-IDEA plugin
2. Configure it to use `.linting/java/checkstyle.xml`
3. Install the SpotBugs plugin and configure it to use
   `.linting/java/spotbugs-exclude.xml`
4. Install the PMD plugin and configure it to use `.linting/java/pmd.xml`

## CI/CD Integration

The linting system is integrated with GitHub Actions for automated checks on
pull requests and pushes. The workflow is defined in
`.github/workflows/incremental-lint.yml`.

Key features:

- Runs linting on changed files only
- Comments on pull requests with linting results
- Fails the build if linting errors are found
- Uploads detailed reports as artifacts

## Troubleshooting

### Common Issues

#### Linting Fails with "Command Not Found"

Ensure all required linting tools are installed:

```bash
# Install Node.js dependencies
npm install -g jsonlint yamllint markdownlint-cli markdown-link-check

# Install Python dependencies
pip install hadolint
```

#### Pre-commit Hooks Not Running

If pre-commit hooks are not running automatically:

```bash
# Reinstall hooks
pre-commit uninstall
pre-commit install
```

#### Incremental Linting Not Working

If incremental linting is not detecting changed files:

```bash
# Clean the linting cache
node .linting/scripts/cache-manager.js clean

# Force a full lint
make lint --force-all
```

### Fixing Common Linting Violations

#### Java

1. **Checkstyle Violations**:

   - Run `mvn checkstyle:check` to see detailed errors
   - Use `java -jar checkstyle.jar -c .linting/java/checkstyle.xml <file>` for
     specific files

2. **SpotBugs Issues**:
   - Run `mvn spotbugs:check` for detailed reports
   - Check the SpotBugs documentation for specific error codes

#### TypeScript/React

1. **ESLint Errors**:

   - Run `npx eslint --fix <file>` to automatically fix issues
   - Check the ESLint documentation for specific rules

2. **Prettier Formatting**:
   - Run `npx prettier --write <file>` to format the file

#### Markdown

1. **Markdownlint Issues**:
   - Run `npx markdownlint --fix <file>` to automatically fix issues
   - Check the Markdownlint documentation for specific rules

## Contributing

### Adding New Linting Rules

To add new linting rules:

1. Identify the appropriate configuration file
2. Add or modify the rule
3. Test the change locally
4. Submit a pull request with the change

### Creating Service-Specific Rules

To create service-specific linting rules:

1. Create a directory for the service in `.linting/services/{service-name}/`
2. Copy the relevant configuration files from the global configuration
3. Modify the rules as needed
4. The service-specific rules will automatically override the global rules

## Advanced Usage

### Incremental Linting

The incremental linting system only lints files that have changed since the last
successful lint. This is done by:

1. Detecting changed files using git diff
2. Caching file hashes to track changes
3. Only running linters on files that have changed

To customize incremental linting:

```bash
# Run with specific commit range
.linting/scripts/incremental-lint.sh --commit-range=HEAD~3..HEAD

# Run with verbose output
.linting/scripts/incremental-lint.sh --verbose

# Force linting of all files
.linting/scripts/incremental-lint.sh --force-all

# Specify a custom cache directory
.linting/scripts/incremental-lint.sh --cache-dir=.custom-cache
```

### Cache Management

The linting cache can be managed using the cache manager:

```bash
# Clean old cache entries
node .linting/scripts/cache-manager.js clean

# Reset linting results
node .linting/scripts/cache-manager.js reset

# Check if a file has changed
node .linting/scripts/cache-manager.js check <file>

# Update cache for a file
node .linting/scripts/cache-manager.js update <file>
```
