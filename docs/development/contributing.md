# Contributing to the MCP Debate System

Thank you for your interest in contributing to the MCP Debate System! To ensure a smooth collaboration, please follow these guidelines.

## 1. Code of Conduct
Please read and adhere to our [Code of Conduct](../SECURITY.md#code-of-conduct).

## 2. Reporting Issues
- Search existing issues to avoid duplicates.
- Open a new GitHub Issue if your problem or feature request is not covered.

## 3. Development Workflow

### 3.1 Branching
- Create a feature branch from `main` using the naming convention:
  ```
  feature/<github-issue-number>-short-description
  fix/<github-issue-number>-short-description
  ```

### 3.2 Commit Messages
We follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: add new feature X
fix: correct behavior of Y
docs: update documentation
chore: maintenance tasks (e.g., dependency upgrades)
```  
Include the issue number in the commit body, e.g., `Closes #123`.

### 3.3 Code Style & Linting
- Run pre-commit hooks before submitting:
  ```bash
  pre-commit run --all-files
  ```
- Java: Checkstyle and SpotBugs are configured via Maven.
- Python: Black and isort are configured in pre-commit.

### 3.4 Testing
- Unit tests:
  ```bash
  make test UNIT=true
  ```
- Integration tests (requires Docker services):
  ```bash
  make test-integration
  ```

### 3.5 Pull Requests
- Push your branch to GitHub and open a PR against `main`.
- Include a clear description and reference related issues.
- At least two approving reviews are required before merging.
- Ensure the CI pipeline passes (lint, tests, security scans).

## 4. Continuous Integration
All contributions must pass the CI pipeline:
- Code style checks
- Dependency and security scans
- Unit, integration, and end-to-end tests

## 5. Thank You
We appreciate your contributions and efforts to improve the MCP Debate System!
