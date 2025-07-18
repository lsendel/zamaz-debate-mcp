# Linting Troubleshooting Guide

This guide provides solutions for common linting issues you might encounter in
the Zamaz Debate MCP Services project.

## General Issues

### Linting Takes Too Long

**Problem**: Running `make lint` takes a long time to complete.

**Solutions**:

1. Use incremental linting which only checks changed files:

   ```bash
   make lint
   ```

2. Lint only specific file types:

   ```bash
   make lint-java    # Java files only
   make lint-frontend # TypeScript/React files only
   ```

3. Check if your cache is working properly:
   ```bash
   node .linting/scripts/cache-manager.js clean
   ```

### Pre-commit Hooks Blocking Commits

**Problem**: Pre-commit hooks are blocking commits with linting errors.

**Solutions**:

1. Fix the linting errors:

   ```bash
   make lint-fix
   ```

2. If you need to bypass the hooks temporarily (not recommended):

   ```bash
   git commit --no-verify -m "Your commit message"
   ```

3. If the hooks are taking too long, optimize them:
   ```bash
   # Edit .pre-commit-config.yaml to include only necessary hooks
   ```

## Java Linting Issues

### Checkstyle Violations

**Problem**: Checkstyle reports violations in Java code.

**Solutions**:

1. View detailed errors:

   ```bash
   mvn checkstyle:check
   ```

2. Common fixes:

   - **Missing Javadoc**: Add documentation comments to classes and methods
   - **Line Length**: Break long lines into multiple lines
   - **Indentation**: Use 4 spaces for indentation
   - **Naming Conventions**: Follow camelCase for variables/methods, PascalCase
     for classes

3. Auto-fix some issues:
   ```bash
   # Some IDEs can auto-format according to checkstyle rules
   # In IntelliJ, use Code > Reformat Code
   ```

### SpotBugs Issues

**Problem**: SpotBugs reports potential bugs in Java code.

**Solutions**:

1. View detailed errors:

   ```bash
   mvn spotbugs:check
   ```

2. Common fixes:

   - **Null Pointer**: Add null checks before using objects
   - **Resource Leaks**: Use try-with-resources for closeable resources
   - **Unchecked Exceptions**: Add proper exception handling

3. If a warning is a false positive, add it to the exclusion file:
   ```xml
   <!-- In .linting/java/spotbugs-exclude.xml -->
   <Match>
     <Class name="com.example.YourClass" />
     <Method name="yourMethod" />
     <Bug pattern="SPECIFIC_BUG_PATTERN" />
   </Match>
   ```

### PMD Violations

**Problem**: PMD reports code quality issues in Java code.

**Solutions**:

1. View detailed errors:

   ```bash
   mvn pmd:check
   ```

2. Common fixes:
   - **Unused Variables**: Remove or use declared variables
   - **Complex Methods**: Break down methods with high cyclomatic complexity
   - **Empty Blocks**: Add comments or code to empty catch blocks

## TypeScript/React Linting Issues

### ESLint Errors

**Problem**: ESLint reports errors in TypeScript/React code.

**Solutions**:

1. View detailed errors:

   ```bash
   cd debate-ui && npx eslint src
   ```

2. Auto-fix issues:

   ```bash
   cd debate-ui && npx eslint --fix src
   ```

3. Common fixes:

   - **Unused Variables**: Remove or use declared variables
   - **Missing Types**: Add proper TypeScript types
   - **React Hooks Rules**: Follow React hooks rules (dependencies array, etc.)

4. If a rule needs to be disabled for a specific line:
   ```typescript
   // eslint-disable-next-line react-hooks/exhaustive-deps
   useEffect(() => {
     // Your code
   }, []);
   ```

### Prettier Formatting Issues

**Problem**: Prettier reports formatting issues in TypeScript/React code.

**Solutions**:

1. Format files automatically:

   ```bash
   cd debate-ui && npx prettier --write src
   ```

2. Configure your editor to format on save using Prettier

## Configuration File Linting Issues

### YAML Syntax Errors

**Problem**: YAML linting reports syntax errors.

**Solutions**:

1. View detailed errors:

   ```bash
   yamllint -c .linting/config/yaml-lint.yml your-file.yml
   ```

2. Common fixes:
   - **Indentation**: Use consistent indentation (usually 2 spaces)
   - **Quotes**: Use quotes for strings with special characters
   - **Trailing Spaces**: Remove trailing spaces

### JSON Validation Errors

**Problem**: JSON linting reports validation errors.

**Solutions**:

1. View detailed errors:

   ```bash
   jsonlint -c .linting/config/json-schema.json your-file.json
   ```

2. Common fixes:
   - **Missing Commas**: Add commas between items in arrays and objects
   - **Trailing Commas**: Remove trailing commas
   - **Quotes**: Use double quotes for keys and string values

### Dockerfile Linting Issues

**Problem**: Dockerfile linting reports issues.

**Solutions**:

1. View detailed errors:

   ```bash
   hadolint -c .linting/config/dockerfile-rules.yml Dockerfile
   ```

2. Common fixes:
   - **Use Specific Tags**: Use specific version tags instead of `latest`
   - **Combine RUN Commands**: Combine multiple RUN commands with `&&`
   - **Remove Unnecessary Packages**: Clean up after package installation

## Documentation Linting Issues

### Markdown Formatting Issues

**Problem**: Markdownlint reports formatting issues in Markdown files.

**Solutions**:

1. View detailed errors:

   ```bash
   npx markdownlint --config .linting/docs/markdownlint.json your-file.md
   ```

2. Auto-fix issues:

   ```bash
   npx markdownlint --fix --config .linting/docs/markdownlint.json your-file.md
   ```

3. Common fixes:
   - **Heading Levels**: Don't skip heading levels (e.g., h1 to h3)
   - **Line Length**: Keep lines under 100 characters
   - **List Indentation**: Use consistent indentation for lists

### Broken Links

**Problem**: Link checker reports broken links in documentation.

**Solutions**:

1. View detailed errors:

   ```bash
   npx markdown-link-check --config .linting/docs/link-check.json your-file.md
   ```

2. Common fixes:
   - **Relative Links**: Ensure relative links point to existing files
   - **Anchors**: Check that anchor links (#section-name) match actual headings
   - **External Links**: Verify external URLs are correct and accessible

## CI/CD Integration Issues

### GitHub Actions Workflow Failures

**Problem**: Linting fails in GitHub Actions but passes locally.

**Solutions**:

1. Check the workflow logs for specific errors

2. Ensure all dependencies are installed in the workflow:

   ```yaml
   # In .github/workflows/incremental-lint.yml
   - name: Install dependencies
     run: |
       npm install -g jsonlint yamllint markdownlint-cli markdown-link-check
       pip install hadolint
   ```

3. Run the same commands locally that are run in CI:
   ```bash
   .linting/scripts/incremental-lint.sh --verbose --commit-range="HEAD~1..HEAD"
   ```

### PR Comments Not Working

**Problem**: Linting results are not being posted as PR comments.

**Solutions**:

1. Check that the GitHub token has the necessary permissions

2. Verify the workflow file has the correct action:
   ```yaml
   - name: Comment PR with linting results
     uses: actions/github-script@v6
     with:
       github-token: ${{ secrets.GITHUB_TOKEN }}
       # Rest of the configuration
   ```

## Advanced Troubleshooting

### Debugging Incremental Linting

If incremental linting is not working as expected:

1. Run with verbose output:

   ```bash
   .linting/scripts/incremental-lint.sh --verbose
   ```

2. Check the cache contents:

   ```bash
   ls -la .linting/cache/
   ```

3. Reset the cache:
   ```bash
   rm -rf .linting/cache/
   mkdir -p .linting/cache/
   ```

### Custom Linting Rules

If you need to customize linting rules for a specific service:

1. Create service-specific configuration:

   ```bash
   mkdir -p .linting/services/your-service/
   cp .linting/java/checkstyle.xml .linting/services/your-service/
   # Edit the copied file to customize rules
   ```

2. Test the custom configuration:
   ```bash
   java -jar checkstyle.jar -c .linting/services/your-service/checkstyle.xml your-file.java
   ```

### Performance Optimization

If linting is still slow after implementing incremental linting:

1. Profile the linting process:

   ```bash
   time .linting/scripts/incremental-lint.sh --verbose
   ```

2. Consider excluding large generated files:

   ```bash
   # Add patterns to .lintignore or similar
   ```

3. Run linting in parallel where possible:
   ```bash
   # Modify scripts to use parallel processing
   ```
