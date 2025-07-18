#!/bin/bash
# Incremental linting script for the Zamaz Debate MCP Services
# This script detects changed files and runs appropriate linters only on those files

set -e

# Default values
COMMIT_RANGE=""
CACHE_DIR=".linting/cache"
VERBOSE=false
FORCE_ALL=false
AUTO_FIX=false
INCLUDE_PATTERN=""

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
  key="$1"
  case ""$key"" in
    --commit-range)
      COMMIT_RANGE="$2"
      shift
      shift
      ;;
    --cache-dir)
      CACHE_DIR="$2"
      shift
      shift
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --force-all)
      FORCE_ALL=true
      shift
      ;;
    --auto-fix)
      AUTO_FIX=true
      shift
      ;;
    --include-pattern)
      INCLUDE_PATTERN="$2"
      shift
      shift
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Create cache directory if it doesn't exist
mkdir -p """$CACHE_DIR"""

# Function to log verbose messages
log() {
  if [ """$VERBOSE""" = true ]; then
    echo "$@"
  fi
}

# Function to get changed files
get_changed_files() {
  local file_pattern=$1
  local commit_range=$2

  if [ -z """$commit_range""" ]; then
    # If no commit range is provided, use unstaged and staged changes
    git diff --name-only --diff-filter=ACMRT HEAD -- """$file_pattern"""
    git diff --name-only --diff-filter=ACMRT --staged -- """$file_pattern"""
  else
    # If commit range is provided, use it
    git diff --name-only --diff-filter=ACMRT """$commit_range""" -- """$file_pattern"""
  fi
}

# Function to check if file has changed since last lint
has_changed_since_last_lint() {
  local file=$1

  # Use the cache manager if available
  if command -v node &> /dev/null && [ -f ".linting/scripts/cache-manager.js" ]; then
    local result=$(node .linting/scripts/cache-manager.js check """$file""")
    if [[ """$result""" == *"has changed: true"* ]]; then
      return 0
    else
      return 1
    fi
  else
    # Fallback to simple hash-based caching
    local cache_file="""$CACHE_DIR""/$(echo """$file""" | tr '/' '_').hash"

    # Get current hash of the file
    local current_hash=$(git hash-object """$file""" 2>/dev/null || echo "file_not_found")

    # If file doesn't exist, return false
    if [ """$current_hash""" = "file_not_found" ]; then
      return 1
    fi

    # If cache file doesn't exist or hash is different, file has changed
    if [ ! -f """$cache_file""" ] || [ "$(cat """$cache_file""")" != """$current_hash""" ]; then
      echo """$current_hash""" > """$cache_file"""
      return 0
    fi

    return 1
  fi
}

# Function to update cache after linting
update_cache() {
  local file=$1

  # Use the cache manager if available
  if command -v node &> /dev/null && [ -f ".linting/scripts/cache-manager.js" ]; then
    node .linting/scripts/cache-manager.js update """$file"""
  fi
}

# Function to run Java linting on specific files
run_java_linting() {
  local files=("$@")

  if [ "${#files[@]}" -eq 0 ]; then
    log "No Java files to lint"
    return 0
  fi

  log "Running Java linting on ${#files[@]} files"

  # Create a temporary POM with only the files to lint
  local tmp_pom=$(mktemp)
  echo "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"
  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.zamaz.mcp</groupId>
  <artifactId>incremental-lint</artifactId>
  <version>1.0.0</version>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <configuration>
          <includeResources>false</includeResources>
          <includeTestResources>false</includeTestResources>
          <includes>" > """$tmp_pom"""

  # Add each file to the includes
  for file in "${files[@]}"; do
    echo "            <include>${file#*/}</include>" >> """$tmp_pom"""
  done

  echo "          </includes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>" >> """$tmp_pom"""

  # Run Maven with the temporary POM
  mvn -f """$tmp_pom""" checkstyle:check spotbugs:check pmd:check

  # Clean up
  rm """$tmp_pom"""

  # Update cache for each file
  for file in "${files[@]}"; do
    update_cache """$file"""
  done
}

# Function to run TypeScript/React linting on specific files
run_ts_linting() {
  local files=("$@")

  if [ "${#files[@]}" -eq 0 ]; then
    log "No TypeScript/React files to lint"
    return 0
  fi

  log "Running TypeScript/React linting on ${#files[@]} files"

  # Run ESLint on the files
  if [ """$AUTO_FIX""" = true ]; then
    cd debate-ui && npx eslint --fix "${files[@]}" && npx prettier --write "${files[@]}"
  else
    cd debate-ui && npx eslint "${files[@]}" && npx prettier --check "${files[@]}"
  fi

  # Update cache for each file
  for file in "${files[@]}"; do
    update_cache """$file"""
  done
}

# Function to run YAML/JSON linting on specific files
run_config_linting() {
  local files=("$@")

  if [ "${#files[@]}" -eq 0 ]; then
    log "No config files to lint"
    return 0
  fi

  log "Running config linting on ${#files[@]} files"

  # Run Python linting with Ruff - NEW for 2025
  local python_files=($(echo "${files[@]}" | tr ' ' '\n' | grep -E '\.pyi?$'))
  if [ "${#python_files[@]}" -gt 0 ]; then
    log "Running Ruff on ${#python_files[@]} Python files"
    if command -v ruff &> /dev/null; then
      ruff check "${python_files[@]}" --config pyproject.toml
    else
      log "WARNING: Ruff not installed, skipping Python linting"
    fi
  fi

  # Run Shell script linting with ShellCheck - NEW for 2025
  local shell_files=($(echo "${files[@]}" | tr ' ' '\n' | grep -E '\.(sh|bash)$'))
  if [ "${#shell_files[@]}" -gt 0 ]; then
    log "Running ShellCheck on ${#shell_files[@]} shell scripts"
    if command -v shellcheck &> /dev/null; then
      shellcheck --rcfile .shellcheckrc "${shell_files[@]}"
    else
      log "WARNING: ShellCheck not installed, skipping shell script linting"
    fi
  fi

  # Run yamllint on YAML files
  local yaml_files=($(echo "${files[@]}" | tr ' ' '\n' | grep -E '\.ya?ml$'))
  if [ "${#yaml_files[@]}" -gt 0 ]; then
    yamllint -c .linting/config/yaml-lint.yml "${yaml_files[@]}"
  fi

  # Run jsonlint on JSON files
  local json_files=($(echo "${files[@]}" | tr ' ' '\n' | grep -E '\.json$'))
  if [ "${#json_files[@]}" -gt 0 ]; then
    for file in "${json_files[@]}"; do
      jsonlint -c .linting/config/json-schema.json """$file"""
    done
  fi

  # Run hadolint on Dockerfiles
  local docker_files=($(echo "${files[@]}" | tr ' ' '\n' | grep -E 'Dockerfile'))
  if [ "${#docker_files[@]}" -gt 0 ]; then
    for file in "${docker_files[@]}"; do
      hadolint -c .linting/config/dockerfile-rules.yml """$file"""
    done
  fi

  # Update cache for each file
  for file in "${files[@]}"; do
    update_cache """$file"""
  done
}

# Function to run Markdown linting on specific files
run_md_linting() {
  local files=("$@")

  if [ "${#files[@]}" -eq 0 ]; then
    log "No Markdown files to lint"
    return 0
  fi

  log "Running Markdown linting on ${#files[@]} files"

  # Run markdownlint on the files
  npx markdownlint --config .linting/docs/markdownlint.json "${files[@]}"

  # Run link-check on the files
  npx markdown-link-check --config .linting/docs/link-check.json "${files[@]}"

  # Update cache for each file
  for file in "${files[@]}"; do
    update_cache """$file"""
  done
}

# Reset results before starting
if command -v node &> /dev/null && [ -f ".linting/scripts/cache-manager.js" ]; then
  node .linting/scripts/cache-manager.js reset
fi

# Main execution
if [ """$FORCE_ALL""" = true ]; then
  log "Forcing full lint on all files"
  make lint-all
  exit $?
fi

# If include pattern is provided, use it
if [ -n """$INCLUDE_PATTERN""" ]; then
  log "Using include pattern: ""$INCLUDE_PATTERN"""
  files=$(find . -type f -path """$INCLUDE_PATTERN""" | sort -u)

  # Filter files by type
  java_files=$(echo """$files""" | grep -E '\.java$' || echo "")
  ts_files=$(echo """$files""" | grep -E '\.(ts|tsx|js|jsx)$' || echo "")
  config_files=$(echo """$files""" | grep -E '\.(yml|yaml|json)$|Dockerfile' || echo "")
  md_files=$(echo """$files""" | grep -E '\.md$' || echo "")
else
  # Get changed files by type
  java_files=$(get_changed_files "*.java" """$COMMIT_RANGE""" | sort -u)
  ts_files=$(get_changed_files "*.ts *.tsx *.js *.jsx" """$COMMIT_RANGE""" | grep -E '^debate-ui/' | sort -u)
  config_files=$(get_changed_files "*.yml *.yaml *.json Dockerfile" """$COMMIT_RANGE""" | sort -u)
  md_files=$(get_changed_files "*.md" """$COMMIT_RANGE""" | sort -u)
fi

# Filter files that have changed since last lint
java_files_to_lint=()
for file in ""$java_files""; do
  if has_changed_since_last_lint """$file"""; then
    java_files_to_lint+=("""$file""")
  fi
done

ts_files_to_lint=()
for file in ""$ts_files""; do
  if has_changed_since_last_lint """$file"""; then
    ts_files_to_lint+=("""$file""")
  fi
done

config_files_to_lint=()
for file in ""$config_files""; do
  if has_changed_since_last_lint """$file"""; then
    config_files_to_lint+=("""$file""")
  fi
done

md_files_to_lint=()
for file in ""$md_files""; do
  if has_changed_since_last_lint """$file"""; then
    md_files_to_lint+=("""$file""")
  fi
done

# Run linting on changed files
run_java_linting "${java_files_to_lint[@]}"
run_ts_linting "${ts_files_to_lint[@]}"
run_config_linting "${config_files_to_lint[@]}"
run_md_linting "${md_files_to_lint[@]}"

# Clean old cache entries
if command -v node &> /dev/null && [ -f ".linting/scripts/cache-manager.js" ]; then
  node .linting/scripts/cache-manager.js clean
fi

log "Incremental linting completed successfully"
exit 0
