# Documentation Guidelines

## Overview

This document outlines the guidelines and processes for maintaining high-quality documentation in the Zamaz Debate MCP project. Following these guidelines ensures consistency, accuracy, and professionalism in all project documentation.

## General Documentation Principles

1. **Clarity**: Write in clear, concise language that is easy to understand.
2. **Accuracy**: Ensure all information is factually correct and up-to-date.
3. **Completeness**: Cover all necessary aspects of the topic without omitting important details.
4. **Consistency**: Maintain consistent formatting, terminology, and style throughout.
5. **Professionalism**: Avoid personal notes, comments, or informal language in official documentation.

## Formatting Standards

### Markdown Files

1. **Headers**: Use appropriate header levels (# for main title, ## for sections, ### for subsections).
2. **Lists**: Use - for unordered lists and 1. for ordered lists.
3. **Code Blocks**: Use triple backticks (```) with language specification for code blocks.
4. **Emphasis**: Use **bold** for emphasis and *italics* for secondary emphasis.
5. **Links**: Use [text](URL) format for links.

### Repository Documentation

1. **Repository Structure**: Maintain a consistent format for describing directories:
   ```
   - **directory-name** - Brief description of purpose
   ```

2. **Component Descriptions**: Use consistent formatting for component descriptions:
   ```
   **Component Name**: Description of component
   ```

3. **Code Examples**: Always include language specification with code blocks:
   ```markdown
   ```bash
   # Command example
   ```
   ```

## Documentation Review Process

### Pre-Commit Review

Before committing documentation changes:

1. **Self-Review**: Review your changes for:
   - Spelling and grammar errors
   - Formatting consistency
   - Removal of personal notes or comments
   - Accuracy of technical information

2. **Linting**: Run documentation linting tools if available:
   ```bash
   # Example command for markdown linting
   npm run lint:docs
   ```

### Peer Review Process

All significant documentation changes should undergo peer review:

1. **Create Pull Request**: Submit documentation changes via pull request.
2. **Assign Reviewers**: Assign at least one team member familiar with the subject matter.
3. **Review Checklist**: Reviewers should check for:
   - Technical accuracy
   - Formatting consistency
   - Clarity and readability
   - Absence of personal notes or comments
   - Completeness of information

4. **Approval**: Documentation changes require approval before merging.

## Automated Checks

Implement automated checks in the CI/CD pipeline:

1. **Markdown Linting**: Use tools like markdownlint to check formatting.
2. **Spell Checking**: Implement spell checking with technical dictionary.
3. **Link Validation**: Verify that all links are valid.
4. **Personal Note Detection**: Implement checks for common patterns of personal notes (e.g., "TODO", "NOTE:", "see my").

## Documentation Maintenance

1. **Regular Reviews**: Schedule quarterly reviews of key documentation.
2. **Version Updates**: Update documentation when dependencies or versions change.
3. **Deprecation Notices**: Clearly mark deprecated features or documentation.
4. **Feedback Loop**: Implement a process for users to provide feedback on documentation.

## Pre-Commit Hook for Documentation

Add a pre-commit hook to catch common documentation issues:

```bash
#!/bin/bash
# Pre-commit hook for documentation quality

# Check for personal notes in documentation files
if git diff --cached --name-only | grep -E '\.(md|txt)$' | xargs grep -l -E 'TODO|FIXME|NOTE:|see my|my note' > /dev/null; then
  echo "Error: Found personal notes in documentation files."
  echo "Please remove personal notes before committing."
  git diff --cached --name-only | grep -E '\.(md|txt)$' | xargs grep -n -E 'TODO|FIXME|NOTE:|see my|my note'
  exit 1
fi

# Run markdown linting if available
if command -v markdownlint &> /dev/null; then
  git diff --cached --name-only | grep -E '\.md$' | xargs markdownlint
  if [ $? -ne 0 ]; then
    echo "Error: Markdown linting failed."
    echo "Please fix the issues before committing."
    exit 1
  fi
fi

exit 0
```

## Implementation Steps

1. Add this document to the project repository.
2. Set up automated linting for documentation files.
3. Implement the pre-commit hook for documentation quality.
4. Add documentation review to the pull request template.
5. Train team members on documentation standards.

By following these guidelines and processes, we can maintain high-quality, professional documentation throughout the project lifecycle.