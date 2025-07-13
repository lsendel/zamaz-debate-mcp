# GEMINI.md - MCP Template Service

This document provides a concise overview of the `mcp-template` service.

## Service Purpose

The `mcp-template` service is responsible for managing and rendering Jinja2 templates. It is used to generate dynamic content, such as debate prompts, response formats, and system messages.

## Core Features

- **Template Management**: CRUD operations for templates.
- **Jinja2 Rendering**: Renders templates using the Jinja2 templating engine in a sandboxed environment.
- **Template Categories**: Organizes templates into categories, such as `debate`, `prompt`, and `response`.
- **Variable Validation**: Validates the variables provided when rendering a template.
- **System Templates**: Provides a set of default system templates that can be used out of the box.

## Technical Stack

- **Language**: Python
- **Database**: PostgreSQL
- **Templating Engine**: Jinja2

## Integration

The `mcp-template` service is used by other services, such as the `mcp-debate` service, to generate dynamic content. For example, the `mcp-debate` service can use it to generate the opening statement for a debate, or to create a prompt for an AI participant.

## Security

The service uses a sandboxed environment for rendering user-provided templates, which prevents the execution of arbitrary code. It also uses auto-escaping to prevent XSS attacks.
