#!/usr/bin/env python3
"""
Demo script for the comprehensive documentation analysis system.
This script demonstrates the key features with a simple example.
"""

import os
import sys
import tempfile
import json
from pathlib import Path

# Add the current directory to the Python path
sys.path.insert(0, os.path.dirname(__file__))

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

response = requests.get('https://api.example.com/users')
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
    
    (java_dir / "UserController.java").write_text('''
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
''')
    
    return sample_dir

def run_demo():
    """Run the documentation analysis demo."""
    print("🚀 Documentation Analysis System Demo")
    print("=" * 50)
    
    # Create sample documentation
    print("Creating sample documentation...")
    sample_dir = create_sample_docs()
    print(f"Sample docs created in: {sample_dir}")
    
    # Initialize the analysis system
    print("\nInitializing analysis system...")
    system = DocumentationAnalysisSystem()
    
    # Run analysis
    print("Running documentation analysis...")
    results = system.analyze_project(str(sample_dir))
    
    # Display results
    print("\n📊 Analysis Results")
    print("-" * 30)
    
    metrics = results.get('metrics', {})
    if hasattr(metrics, 'total_files'):
        print(f"Total Files: {metrics.total_files}")
        print(f"Total Words: {metrics.total_words:,}")
        print(f"Quality Score: {metrics.quality_score:.1f}/100")
        print(f"Coverage: {metrics.coverage_percentage:.1f}%")
        print(f"Issues Found: {len(results.get('issues', []))}")
        
        # Show format distribution
        formats = metrics.formats
        if formats:
            print(f"\n📄 Format Distribution:")
            for fmt, count in formats.items():
                print(f"  {fmt.title()}: {count} files")
    else:
        print("No metrics available")
    
    # Show top issues
    issues = results.get('issues', [])
    if issues:
        print(f"\n⚠️  Top Issues:")
        for issue in issues[:3]:
            if hasattr(issue, 'severity'):
                print(f"  - {issue.severity.upper()}: {issue.message}")
                if hasattr(issue, 'file_path') and issue.file_path:
                    print(f"    File: {issue.file_path}")
            else:
                print(f"  - {issue}")
    
    # Show recommendations
    recommendations = results.get('recommendations', {})
    if recommendations:
        print(f"\n💡 Recommendations:")
        for category in ['high_priority', 'medium_priority']:
            if recommendations.get(category):
                for rec in recommendations[category][:2]:
                    if isinstance(rec, dict):
                        print(f"  - {rec['title']}")
                        print(f"    {rec['description']}")
                    else:
                        print(f"  - {rec}")
    
    # Demonstrate search functionality
    print(f"\n🔍 Search Demo")
    print("-" * 20)
    
    search_queries = ["API", "installation", "users"]
    for query in search_queries:
        results_search = system.search_documentation(query, top_k=3)
        print(f"Search for '{query}':")
        if results_search:
            for i, (doc, score) in enumerate(results_search, 1):
                print(f"  {i}. {doc.title} (Score: {score:.3f})")
        else:
            print(f"  No results found")
        print()
    
    # Show documentation files analyzed
    doc_files = results.get('documentation_files', [])
    if doc_files:
        print(f"\n📚 Documentation Files Analyzed:")
        for doc in doc_files:
            if isinstance(doc, dict):
                print(f"  - {doc.get('title', 'Untitled')} ({doc.get('format', 'unknown')})")
                print(f"    Words: {doc.get('word_count', 0)}, Quality: {doc.get('quality_score', 0):.1f}")
            else:
                print(f"  - {doc}")
    
    # Show code files analyzed
    code_files = results.get('code_files', [])
    if code_files:
        print(f"\n💻 Code Files Analyzed:")
        for code in code_files:
            if isinstance(code, dict):
                print(f"  - {code.get('path', 'unknown')} ({code.get('language', 'unknown')})")
                print(f"    Coverage: {code.get('documentation_coverage', 0):.1f}%")
            else:
                print(f"  - {code}")
    
    print(f"\n✅ Demo completed successfully!")
    print(f"Sample documentation created in: {sample_dir}")
    print(f"You can explore the files and run the full analysis manually.")
    
    # Clean up
    import shutil
    try:
        shutil.rmtree(sample_dir)
        print(f"Cleaned up sample directory.")
    except:
        print(f"Note: Sample directory not cleaned up: {sample_dir}")

if __name__ == "__main__":
    run_demo()