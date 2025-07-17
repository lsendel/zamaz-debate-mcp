# Contributing Guidelines

Thank you for your interest in contributing to the Zamaz Debate MCP project! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Workflow](#development-workflow)
4. [Pull Request Process](#pull-request-process)
5. [Coding Standards](#coding-standards)
6. [Testing Guidelines](#testing-guidelines)
7. [Documentation Guidelines](#documentation-guidelines)
8. [Issue Reporting](#issue-reporting)
9. [Feature Requests](#feature-requests)
10. [Community](#community)

## Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. We expect all participants to adhere to the following principles:

- Be respectful and considerate of others
- Be open to different viewpoints and experiences
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

Unacceptable behavior includes:

- Harassment or discrimination of any kind
- Offensive comments or personal attacks
- Trolling, insulting/derogatory comments, or personal/political attacks
- Public or private harassment
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate in a professional setting

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 21
- Maven 3.8+
- Docker and Docker Compose
- Git
- IDE of your choice (IntelliJ IDEA recommended)

### Setup

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/yourusername/zamaz-debate-mcp.git
   cd zamaz-debate-mcp
   ```
3. Add the original repository as an upstream remote:
   ```bash
   git remote add upstream https://github.com/originalusername/zamaz-debate-mcp.git
   ```
4. Create a `.env` file from the template:
   ```bash
   cp .env.example .env
   # Edit .env with your API keys
   ```
5. Build the project:
   ```bash
   mvn clean install
   ```
6. Start the services:
   ```bash
   docker-compose up -d
   ```

## Development Workflow

1. **Keep your fork updated**:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   git push origin main
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**:
   - Write code that follows our [coding standards](#coding-standards)
   - Add or update tests as necessary
   - Add or update documentation as necessary

4. **Commit your changes**:
   ```bash
   git add .
   git commit -m "Add feature X"
   ```

5. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a pull request** from your branch to the upstream `main` branch

## Pull Request Process

1. **Create a pull request** from your feature branch to the upstream `main` branch
2. **Fill out the pull request template** with all required information
3. **Ensure all checks pass**:
   - CI/CD pipeline
   - Code review
   - Documentation review
4. **Address review feedback** and make necessary changes
5. **Update your branch** if needed:
   ```bash
   git fetch upstream
   git merge upstream/main
   git push origin feature/your-feature-name
   ```
6. **Once approved**, a maintainer will merge your pull request

### Pull Request Template

```markdown
## Description
[Provide a brief description of the changes in this pull request]

## Related Issue
[Link to the related issue(s)]

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Refactoring
- [ ] Other (please describe)

## Checklist
- [ ] I have read the CONTRIBUTING document
- [ ] My code follows the coding standards of this project
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] I have updated the documentation accordingly
- [ ] My changes generate no new warnings
- [ ] All new and existing tests passed
```

## Coding Standards

Please follow the [coding standards](./coding-standards.md) for this project. Key points include:

- Follow the Google Java Style Guide with specified modifications
- Use meaningful names for variables, methods, and classes
- Write clear and concise comments
- Keep methods and classes focused on a single responsibility
- Use appropriate design patterns
- Follow REST API best practices

## Testing Guidelines

Please follow the [testing guidelines](./testing.md) for this project. Key points include:

- Write unit tests for all new code
- Maintain high test coverage (aim for > 80%)
- Test both happy paths and error cases
- Use descriptive test names
- Keep tests independent and idempotent
- Mock external dependencies in unit tests

## Documentation Guidelines

Good documentation is essential for the project's usability and maintainability. Please follow these guidelines:

1. **Code Documentation**:
   - Document all public APIs with clear descriptions
   - Include parameter and return value descriptions
   - Document exceptions that may be thrown
   - Add examples for complex functionality

2. **README and Wiki**:
   - Update README.md when adding new features or changing functionality
   - Keep the wiki up-to-date with architectural changes
   - Document configuration options and environment variables

3. **Service Documentation**:
   - Each service should have its own documentation in its `/docs` directory
   - Document API endpoints, request/response formats, and error codes
   - Include usage examples

4. **Documentation Structure**:
   - Follow the established documentation structure
   - Place documentation in the appropriate location
   - Link related documentation together

## Issue Reporting

If you find a bug or have a suggestion for improvement, please create an issue on GitHub:

1. Check if the issue already exists
2. Use the issue template to create a new issue
3. Provide as much detail as possible:
   - Steps to reproduce the issue
   - Expected behavior
   - Actual behavior
   - Screenshots or logs if applicable
   - Environment details (OS, browser, etc.)

### Issue Template

```markdown
## Description
[A clear and concise description of the issue]

## Steps to Reproduce
1. [First step]
2. [Second step]
3. [And so on...]

## Expected Behavior
[What you expected to happen]

## Actual Behavior
[What actually happened]

## Screenshots
[If applicable, add screenshots to help explain your problem]

## Environment
- OS: [e.g., Windows 10, macOS 12.0]
- Browser: [e.g., Chrome 96, Firefox 95]
- Version: [e.g., 1.0.0]

## Additional Context
[Add any other context about the problem here]
```

## Feature Requests

We welcome feature requests! To submit a feature request:

1. Check if the feature has already been requested or implemented
2. Use the feature request template to create a new issue
3. Provide as much detail as possible:
   - Clear description of the feature
   - Use cases and benefits
   - Potential implementation approach
   - Any relevant examples or references

### Feature Request Template

```markdown
## Feature Description
[A clear and concise description of the feature]

## Use Cases
[Describe the use cases for this feature]

## Benefits
[Explain the benefits of implementing this feature]

## Proposed Implementation
[If you have ideas about how to implement this feature, describe them here]

## Alternatives Considered
[Describe any alternative solutions or features you've considered]

## Additional Context
[Add any other context, examples, or references about the feature request here]
```

## Community

### Communication Channels

- **GitHub Issues**: For bug reports, feature requests, and discussions
- **Pull Requests**: For code contributions and reviews
- **Wiki**: For documentation and knowledge sharing
- **Slack**: For real-time communication and questions

### Roles and Responsibilities

- **Contributors**: Anyone who submits a pull request, reports an issue, or participates in discussions
- **Maintainers**: Project team members with write access to the repository
- **Administrators**: Project leaders with administrative access to the repository

### Recognition

We value all contributions to the project, including:

- Code contributions
- Documentation improvements
- Bug reports
- Feature suggestions
- Code reviews
- Answering questions

Contributors will be recognized in the project's README and release notes.

## License

By contributing to this project, you agree that your contributions will be licensed under the project's license.

## Questions?

If you have any questions about contributing, please reach out to the project maintainers.
