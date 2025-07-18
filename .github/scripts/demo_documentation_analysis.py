#!/usr/bin/env python3
"""
Demo script for the comprehensive documentation analysis system.
This script demonstrates the key features with a simple example.
"""

import os
import sys
import tempfile
from pathlib import Path

# Add the current directory to the Python path
sys.path.insert(0, str(Path(__file__).parent))

import contextlib

from analyzers.documentation_analyzer import DocumentationAnalysisSystem


def create_sample_docs():
    """Create sample documentation files for testing."""
    sample_dir = Path(tempfile.mkdtemp())

    # Create sample markdown documentation
    (sample_dir / "README.md").write_text("""
# Sample Project

This is a sample project for testing the documentation analysis system.

## Features

- Documentation parsing
- Quality analysis
- Coverage metrics
- Search functionality

## Getting Started

Follow these steps to get started:

1. Install dependencies
2. Run the analysis
3. Review results

## API Reference

### Authentication

All API endpoints require authentication using Bearer tokens.

### Endpoints

#### GET /api/users
Returns a list of users.

#### POST /api/users
Creates a new user.

## Examples

```python
import requests

response = requests.get('https://api.example.com/users', timeout=30)
print(response.json())
```

## Contributing

Please read our [contributing guidelines](CONTRIBUTING.md) before submitting pull requests.

## License

This project is licensed under the MIT License.
""")

    # Create API documentation
    (sample_dir / "API.md").write_text("""
# API Documentation

## Overview

This API provides user management functionality.

## Authentication

Use Bearer tokens in the Authorization header:

```
Authorization: Bearer YOUR_TOKEN_HERE
```

## Endpoints

### Users

#### List Users
- **GET** `/api/users`
- **Description**: Retrieve all users
- **Response**: Array of user objects

#### Create User
- **POST** `/api/users`
- **Description**: Create a new user
- **Body**: User object
- **Response**: Created user object

### Groups

#### List Groups
- **GET** `/api/groups`
- **Description**: Retrieve all groups

## Error Handling

The API returns standard HTTP status codes:

- 200: Success
- 400: Bad Request
- 401: Unauthorized
- 404: Not Found
- 500: Internal Server Error

## Rate Limiting

Requests are limited to 1000 per hour per API key.
""")

    # Create setup guide
    (sample_dir / "SETUP.md").write_text("""
# Setup Guide

## Prerequisites

- Python 3.8+
- Node.js 14+
- Docker (optional)

## Installation

1. Clone the repository
2. Install dependencies
3. Configure environment variables
4. Run the application

## Configuration

Create a `.env` file with:

```
DATABASE_URL=postgresql://localhost/myapp
API_KEY=your-api-key-here
```

## Development

Run in development mode:

```bash
npm run dev
```

## Testing

Run tests:

```bash
npm test
```

## Deployment

Deploy to production:

```bash
docker build -t myapp .
docker run -p 3000:3000 myapp
```
""")

    # Create incomplete documentation
    (sample_dir / "INCOMPLETE.md").write_text("""
# Incomplete Documentation

This document is incomplete.

TODO: Add more content here.
""")

    # Create sample Python code
    code_dir = sample_dir / "src"
    code_dir.mkdir()

    (code_dir / "main.py").write_text('''
"""
Main application module.

This module provides the main application logic.
"""

class UserService:
    """Service for managing users."""

    def __init__(self, database_url: str):
        """Initialize the user service.

        Args:
            database_url: Database connection URL
        """
        self.database_url = database_url

    def get_users(self):
        """Get all users.

        Returns:
            List of user objects
        """
        # Implementation here
        pass

    def create_user(self, user_data):
        """Create a new user.

        Args:
            user_data: User data dictionary

        Returns:
            Created user object
        """
        # Implementation here
        pass

def main():
    """Main application entry point."""
    service = UserService("postgresql://localhost/myapp")
    users = service.get_users()
    print(f"Found {len(users)} users")

if __name__ == "__main__":
    main()
''')

    # Create sample Java code
    java_dir = code_dir / "java"
    java_dir.mkdir()

    (java_dir / "UserController.java").write_text("""
package com.example.api;

import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for user management.
 *
 * Provides endpoints for CRUD operations on users.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * Constructor for UserController.
     *
     * @param userService The user service dependency
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get all users.
     *
     * @return List of users
     */
    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    /**
     * Create a new user.
     *
     * @param user The user to create
     * @return The created user
     */
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // This method has no documentation
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }
}
""")

    return sample_dir


def run_demo():
    """Run the documentation analysis demo."""

    # Create sample documentation
    sample_dir = create_sample_docs()

    # Initialize the analysis system
    system = DocumentationAnalysisSystem()

    # Run analysis
    results = system.analyze_project(str(sample_dir))

    # Display results

    metrics = results.get("metrics", {})
    if hasattr(metrics, "total_files"):
        # Show format distribution
        formats = metrics.formats
        if formats:
            for _fmt, _count in formats.items():
                pass
    else:
        pass

    # Show top issues
    issues = results.get("issues", [])
    if issues:
        for issue in issues[:3]:
            if hasattr(issue, "severity"):
                if hasattr(issue, "file_path") and issue.file_path:
                    pass
            else:
                pass

    # Show recommendations
    recommendations = results.get("recommendations", {})
    if recommendations:
        for category in ["high_priority", "medium_priority"]:
            if recommendations.get(category):
                for rec in recommendations[category][:2]:
                    if isinstance(rec, dict):
                        pass
                    else:
                        pass

    # Demonstrate search functionality

    search_queries = ["API", "installation", "users"]
    for query in search_queries:
        results_search = system.search_documentation(query, top_k=3)
        if results_search:
            for _i, (doc, _score) in enumerate(results_search, 1):
                pass
        else:
            pass

    # Show documentation files analyzed
    doc_files = results.get("documentation_files", [])
    if doc_files:
        for doc in doc_files:
            if isinstance(doc, dict):
                pass
            else:
                pass

    # Show code files analyzed
    code_files = results.get("code_files", [])
    if code_files:
        for code in code_files:
            if isinstance(code, dict):
                pass
            else:
                pass

    # Clean up
    import shutil

    with contextlib.suppress(Exception):
        shutil.rmtree(sample_dir)


if __name__ == "__main__":
    run_demo()
