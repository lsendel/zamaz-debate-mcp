# Linting Troubleshooting Guide

## Table of Contents

1. [Quick Diagnosis](#quick-diagnosis)
2. [Common Issues](#common-issues)
3. [Cache Problems](#cache-problems)
4. [Git Integration Issues](#git-integration-issues)
5. [Performance Problems](#performance-problems)
6. [Configuration Issues](#configuration-issues)
7. [CI/CD Pipeline Issues](#cicd-pipeline-issues)
8. [IDE Integration Problems](#ide-integration-problems)
9. [Error Reference](#error-reference)
10. [Advanced Debugging](#advanced-debugging)

## Quick Diagnosis

### Health Check Command

```bash
# Run comprehensive health check
lint --health-check

# Quick status check
lint --status

# Verify all components
lint --verify-setup
```

### Common First Steps

1. **Check git repository status**: `git status`
2. **Verify configuration**: `lint --show-config`
3. **Clear cache if suspicious**: `lint --clear-cache`
4. **Test with verbose output**: `lint --verbose`
5. **Check permissions**: `ls -la .linting/`

## Common Issues

### Issue: "No changed files detected"

**Symptoms**: Incremental linting reports no files to process despite having changes.

**Possible Causes**:
- Working in wrong directory
- Git repository not initialized
- Changes are ignored by .gitignore
- Commit range is invalid

**Solutions**:

```bash
# 1. Verify you're in the correct directory
pwd
ls -la .git

# 2. Check git status
git status

# 3. Verify commit range
git log --oneline -10

# 4. Test with working directory changes
lint --incremental --working-dir

# 5. Use explicit commit range
lint --incremental --from-commit HEAD~1 --to-commit HEAD
```

### Issue: "Linting cache is corrupted"

**Symptoms**: Inconsistent results, unexpected cache misses, errors reading cache.

**Solutions**:

```bash
# 1. Clear and rebuild cache
lint --clear-cache
lint --warm-cache

# 2. Verify cache directory permissions
ls -la .linting/cache/
chmod -R 755 .linting/cache/

# 3. Check disk space
df -h .

# 4. Manual cache cleanup
rm -rf .linting/cache/*
mkdir -p .linting/cache
```

### Issue: "OutOfMemoryError during linting"

**Symptoms**: Java heap space errors, application crashes during large file processing.

**Solutions**:

```bash
# 1. Increase heap size
export JAVA_OPTS="-Xmx4g -Xms1g"

# 2. Reduce parallel threads
lint --incremental --threads 2

# 3. Process files in batches
lint --incremental --batch-size 100

# 4. Exclude large files
lint --exclude "**/*.min.js" --exclude "**/*.bundle.*"

# 5. Use streaming mode
lint --incremental --stream-processing
```

### Issue: "Permission denied on cache directory"

**Symptoms**: Cannot write to cache, file system permission errors.

**Solutions**:

```bash
# 1. Fix permissions
sudo chown -R $(whoami):$(whoami) .linting/
chmod -R 755 .linting/

# 2. Use alternative cache location
lint --cache-dir /tmp/lint-cache

# 3. Disable cache temporarily
lint --no-cache

# 4. Check SELinux/AppArmor restrictions (Linux)
sudo setsebool -P httpd_can_network_connect 1
```

## Cache Problems

### Cache Performance Issues

**Symptoms**: Low cache hit rate, slow cache operations.

**Diagnosis**:
```bash
# Check cache statistics
lint --cache-stats --verbose

# Analyze cache distribution
lint --cache-analysis

# Monitor cache performance
lint --cache-monitor --duration 60s
```

**Solutions**:

```bash
# 1. Optimize cache size
lint --cache-config --max-size 20000

# 2. Clean old entries
lint --cache-cleanup --max-age 7d

# 3. Reorganize cache
lint --cache-defrag

# 4. Use SSD for cache storage
ln -s /ssd/cache .linting/cache
```

### Cache Corruption Detection

**Check for corruption**:
```bash
# Verify cache integrity
lint --cache-verify

# Check for orphaned entries
lint --cache-check --repair

# Validate file hashes
lint --cache-validate --fix-invalid
```

**Recovery steps**:
```bash
# 1. Backup current cache
cp -r .linting/cache .linting/cache.backup

# 2. Attempt repair
lint --cache-repair

# 3. If repair fails, rebuild
lint --clear-cache
lint --warm-cache --verbose

# 4. Restore partial data if needed
lint --cache-import .linting/cache.backup --selective
```

## Git Integration Issues

### Issue: "Git command failed with exit code 128"

**Symptoms**: Git operations fail, repository access errors.

**Common Causes**:
- Not in a git repository
- Insufficient permissions
- Corrupted git repository
- Invalid commit references

**Solutions**:

```bash
# 1. Verify git repository
git rev-parse --git-dir

# 2. Check git configuration
git config --list

# 3. Repair repository if needed
git fsck --full

# 4. Reinitialize if corrupted
git init

# 5. Set proper git user
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### Issue: "Invalid commit range"

**Symptoms**: Error parsing commit range, no commits in range.

**Solutions**:

```bash
# 1. Verify commits exist
git log --oneline HEAD~5..HEAD

# 2. Use valid commit syntax
lint --from-commit $(git rev-parse HEAD~1) --to-commit $(git rev-parse HEAD)

# 3. Check branch references
git show-ref

# 4. Use absolute commit hashes
git rev-parse HEAD~1
git rev-parse HEAD
```

### Issue: "Shallow repository limitations"

**Symptoms**: Error in CI/CD with "shallow repository" or "insufficient history".

**Solutions**:

```yaml
# GitHub Actions - fetch full history
- uses: actions/checkout@v4
  with:
    fetch-depth: 0

# GitLab CI - unshallow repository
before_script:
  - git fetch --unshallow

# Manual fix
git fetch --unshallow
```

## Performance Problems

### Issue: "Incremental linting is slower than expected"

**Diagnosis**:

```bash
# 1. Profile linting performance
lint --incremental --profile --output profile.json

# 2. Check system resources
htop
iostat -x 1

# 3. Analyze bottlenecks
lint --incremental --trace --timing
```

**Solutions**:

```bash
# 1. Optimize exclusion patterns
lint --exclude "**/node_modules/**" --exclude "**/target/**"

# 2. Increase parallelism
lint --parallel --threads $(nproc)

# 3. Use faster storage for cache
mv .linting/cache /dev/shm/lint-cache
ln -s /dev/shm/lint-cache .linting/cache

# 4. Optimize JVM settings
export JAVA_OPTS="-server -Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### Issue: "High memory usage during linting"

**Monitoring**:
```bash
# Monitor memory usage
watch -n 1 'ps aux | grep lint | head -5'

# JVM memory analysis
jstat -gc -t $(pgrep -f lint) 1s
```

**Optimization**:
```bash
# 1. Tune JVM garbage collection
export JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m"

# 2. Process files in smaller batches
lint --batch-size 50 --stream-processing

# 3. Limit concurrent file processing
lint --max-concurrent-files 100

# 4. Use memory-mapped files for cache
lint --cache-type mmap
```

## Configuration Issues

### Issue: "Configuration file not found"

**Symptoms**: Default configuration used, custom rules ignored.

**Solutions**:

```bash
# 1. Verify configuration file location
ls -la .linting/global.yml

# 2. Check configuration syntax
lint --config-validate .linting/global.yml

# 3. Use explicit configuration path
lint --config $(pwd)/.linting/global.yml

# 4. Generate default configuration
lint --generate-config .linting/
```

### Issue: "Conflicting configuration rules"

**Symptoms**: Unexpected linting behavior, rules not applied correctly.

**Diagnosis**:
```bash
# Show effective configuration
lint --show-effective-config

# Validate configuration hierarchy
lint --config-hierarchy

# Check rule conflicts
lint --config-validate --check-conflicts
```

**Solutions**:
```bash
# 1. Override specific rules
lint --override "checkstyle.maxLineLength=120"

# 2. Use configuration profiles
lint --profile strict

# 3. Exclude conflicting configurations
lint --exclude-config .linting/legacy/
```

### Issue: "Custom linting rules not loading"

**Symptoms**: Custom rules ignored, plugins not found.

**Solutions**:

```bash
# 1. Verify plugin classpath
lint --list-plugins

# 2. Check plugin configuration
cat .linting/plugins.yml

# 3. Reload plugins
lint --reload-plugins

# 4. Debug plugin loading
lint --debug-plugins --verbose
```

## CI/CD Pipeline Issues

### Issue: "Linting fails in CI but works locally"

**Common Causes**:
- Different Java versions
- Missing dependencies
- File system permissions
- Environment differences

**Solutions**:

```yaml
# GitHub Actions debugging
- name: Debug environment
  run: |
    java -version
    mvn --version
    git --version
    pwd
    ls -la
    env | grep -E "(JAVA|PATH|HOME)"

- name: Setup identical environment
  run: |
    # Use exact same versions as local
    export JAVA_HOME=${{ env.JAVA_HOME }}
    export PATH=$JAVA_HOME/bin:$PATH
```

### Issue: "Cache not persisting between CI runs"

**Solutions**:

```yaml
# GitHub Actions - proper cache configuration
- name: Cache linting results
  uses: actions/cache@v3
  with:
    path: .linting/cache
    key: ${{ runner.os }}-lint-${{ hashFiles('**/*.java', '**/*.ts') }}
    restore-keys: |
      ${{ runner.os }}-lint-

# GitLab CI - artifacts and cache
cache:
  paths:
    - .linting/cache/
  policy: pull-push

artifacts:
  paths:
    - .linting/cache/
  expire_in: 1 week
```

### Issue: "PR comments not working"

**Symptoms**: No linting results posted to PR, GitHub integration fails.

**Solutions**:

```yaml
# Verify permissions
permissions:
  contents: read
  pull-requests: write
  issues: write

# Debug GitHub API
- name: Debug GitHub context
  run: |
    echo "Event: ${{ github.event_name }}"
    echo "PR Number: ${{ github.event.number }}"
    echo "Base SHA: ${{ github.event.pull_request.base.sha }}"
    echo "Head SHA: ${{ github.event.pull_request.head.sha }}"

# Test API access
- name: Test GitHub API
  run: |
    curl -H "Authorization: token ${{ secrets.GITHUB_TOKEN }}" \\
         "https://api.github.com/repos/${{ github.repository }}/pulls/${{ github.event.number }}"
```

## IDE Integration Problems

### IntelliJ IDEA Issues

**Issue: "Plugin not recognized"**

**Solutions**:
```bash
# 1. Verify plugin installation
ls ~/.IntelliJIdea*/config/plugins/

# 2. Check plugin compatibility
# Help → About → Check plugin compatibility

# 3. Reinstall plugin
# File → Settings → Plugins → Uninstall → Install
```

**Issue: "Linting not running automatically"**

**Solutions**:
```xml
<!-- .idea/workspace.xml -->
<component name="PropertiesComponent">
  <property name="mcp.linting.autorun" value="true" />
  <property name="mcp.linting.incremental" value="true" />
</component>
```

### VS Code Issues

**Issue: "Extension not working"**

**Solutions**:
```json
// .vscode/settings.json
{
  "mcp.linting.enabled": true,
  "mcp.linting.incremental": true,
  "mcp.linting.autorun": true,
  "mcp.linting.configPath": ".linting/global.yml"
}
```

**Debug extension**:
```bash
# Check extension logs
# View → Output → MCP Linting

# Reset extension
# Ctrl+Shift+P → "Developer: Reset Extension Host"
```

## Error Reference

### Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| E001 | Configuration file not found | Check `.linting/global.yml` exists |
| E002 | Git repository not found | Run `git init` or check directory |
| E003 | Invalid commit range | Verify commit hashes exist |
| E004 | Cache corruption detected | Run `lint --clear-cache` |
| E005 | Insufficient permissions | Fix file permissions |
| E006 | Out of memory | Increase heap size |
| E007 | Plugin loading failed | Check plugin configuration |
| E008 | Network connectivity | Check internet connection |
| E009 | Timeout exceeded | Increase timeout values |
| E010 | Invalid file format | Check file encoding/format |

### Warning Codes

| Code | Description | Action |
|------|-------------|--------|
| W001 | Low cache hit rate | Consider cache optimization |
| W002 | Large number of excluded files | Review exclusion patterns |
| W003 | Deprecated configuration | Update configuration |
| W004 | Performance degradation | Enable profiling |
| W005 | High memory usage | Monitor memory consumption |

### Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Linting issues found (warnings/errors) |
| 2 | Configuration error |
| 3 | Runtime error |
| 4 | System error |
| 5 | Permission error |

## Advanced Debugging

### Enable Debug Logging

```bash
# Enable all debug logging
export LINT_DEBUG=true
export LINT_LOG_LEVEL=DEBUG

# Specific component debugging
export LINT_DEBUG_CACHE=true
export LINT_DEBUG_GIT=true
export LINT_DEBUG_ENGINE=true

# Log to file
lint --incremental --log-file debug.log --log-level DEBUG
```

### Performance Profiling

```bash
# CPU profiling
lint --profile cpu --profile-output cpu-profile.json

# Memory profiling
lint --profile memory --profile-output memory-profile.json

# I/O profiling
lint --profile io --profile-output io-profile.json

# Combined profiling
lint --profile all --profile-output combined-profile.json
```

### Network Debugging

```bash
# Enable network logging
export LINT_NETWORK_DEBUG=true

# Trace HTTP requests
export JAVA_OPTS="-Dhttp.agent.debug=true"

# Monitor network usage
netstat -i
iftop -i eth0
```

### JVM Debugging

```bash
# Enable JVM debugging
export JAVA_OPTS="-agentlib:hprof=cpu=samples,heap=sites,depth=10"

# Memory dump on OutOfMemoryError
export JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/"

# GC logging
export JAVA_OPTS="-XX:+PrintGC -XX:+PrintGCDetails -Xloggc:gc.log"

# Remote debugging
export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Creating Reproduction Cases

```bash
# Create minimal reproduction
mkdir -p /tmp/lint-repro
cd /tmp/lint-repro
git init
echo "public class Test {}" > Test.java
git add .
git commit -m "Initial"
echo "public class Test { }" > Test.java  # Add space

# Run with full debugging
lint --incremental --working-dir --debug --verbose --trace

# Package for bug report
tar -czf lint-repro.tar.gz .
```

### Getting Help

When reporting issues, include:

1. **Environment information**:
   ```bash
   lint --version
   java -version
   git --version
   uname -a
   ```

2. **Configuration**:
   ```bash
   lint --show-config > config.txt
   ```

3. **Debug output**:
   ```bash
   lint --debug --verbose > debug.log 2>&1
   ```

4. **Reproduction steps**
5. **Expected vs actual behavior**

**Support Channels**:
- GitHub Issues: `https://github.com/zamaz/mcp-linting/issues`
- Documentation: `https://docs.zamaz.com/mcp-linting`
- Community Forum: `https://community.zamaz.com/linting`

---

This troubleshooting guide should help resolve most common issues with the incremental linting system. For complex problems, enable debug logging and follow the reproduction case guidelines when seeking support.