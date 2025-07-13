# GEMINI.md - MCP Context Service

This document provides a concise overview of the `mcp-context` service.

## Service Purpose

The `mcp-context` service is responsible for managing conversation contexts and message histories. It is a critical component for any application that needs to maintain a stateful conversation with an LLM.

## Core Features

- **Context Management**: CRUD operations for conversation contexts.
- **Message Management**: Add, retrieve, and manage messages within a context.
- **Context Windowing**: Strategies for managing the size of the context window (e.g., fixed size, token-based).
- **Token Counting**: Utilities for counting the number of tokens in a message or context.
- **Search**: Search for contexts based on various criteria.

## Technical Stack

- **Language**: Python
- **Database**: PostgreSQL
- **Caching**: Redis (planned)

## Integration

The `mcp-context` service is designed to be used by other services that need to manage conversation history. For example, a debate service would use it to store the history of a debate, and an LLM service would use it to retrieve the context needed to generate a response.

## Key Development Tasks

- **Creating a Context**: A context is created with a name, description, and a windowing strategy.
- **Adding Messages**: Messages are added to a context with a role, content, and metadata.
- **Retrieving Context**: The context can be retrieved with the windowing strategy applied, which ensures that the context size is within the desired limits.
