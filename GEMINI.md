# GEMINI.md - Project Overview

This document provides a high-level overview of the multi-service MCP (Model Context Protocol) system for managing debates.

## System Architecture

The system is composed of the following services:

- **`debate-ui`**: A web-based user interface for managing and participating in debates.
- **`mcp-organization`**: Manages organizations and projects in a multi-tenant environment.
- **`mcp-context`**: Manages conversation contexts and message histories.
- **`mcp-debate`**: Orchestrates debates between AI participants.
- **`mcp-llm`**: Provides a unified interface for interacting with multiple LLM providers.
- **`mcp-rag`**: Provides Retrieval-Augmented Generation (RAG) capabilities.
- **`mcp-template`**: Manages and renders Jinja2 templates.

These services communicate with each other using the MCP protocol.

## Technology Stack

- **Backend**: Python, MCP SDK, PostgreSQL, Redis, Qdrant
- **Frontend**: Node.js, React, Next.js
- **Containerization**: Docker, Docker Compose

## Development

To get started with development, you can use the following commands:

- `make setup`: Sets up the development environment.
- `make start`: Starts all the backend services.
- `make ui`: Starts the UI development server.

For more details on the available commands, please refer to the `Makefile`.

## Key Features

- **Multi-tenancy**: The system is designed to support multiple organizations with complete data isolation.
- **Real-time Debates**: The UI provides real-time updates on the progress of a debate.
- **LLM Agnostic**: The system can be integrated with a variety of LLM providers.
- **Retrieval-Augmented Generation**: The system can use a document repository to provide evidence to the AI participants in a debate.
- **Templating**: The system uses templates to generate dynamic content, such as debate prompts and system messages.
