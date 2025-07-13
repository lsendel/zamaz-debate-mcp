# GEMINI.md - MCP Debate Service

This document provides a concise overview of the `mcp-debate` service.

## Service Purpose

The `mcp-debate` service orchestrates debates between AI participants. It manages the flow of the debate, from creation to completion, and integrates with other services to generate responses and maintain context.

## Core Features

- **Debate Orchestration**: Manages the entire lifecycle of a debate, including creation, turn management, and status updates.
- **Participant Management**: Supports adding and configuring multiple AI participants for a debate.
- **Real-time Updates**: Provides real-time status updates on the progress of a debate (partially implemented via WebSockets).
- **Concurrency Control**: Includes mechanisms for rate limiting and locking to ensure data consistency.

## Technical Stack

- **Language**: Python
- **Storage**: In-memory (planned to be replaced with PostgreSQL)

## Integration

The `mcp-debate` service integrates with several other services to perform its functions:

- **`mcp-context`**: To manage the conversation history of the debate.
- **`mcp-llm`**: To generate responses for the AI participants.
- **`mcp-rag`** (optional): To provide additional context to the LLM.
- **`mcp-template`**: To generate prompts for the AI participants.

## Debate Flow

1. **Create Debate**: A new debate is created with a topic, rules, and a set of participants.
2. **Start Debate**: The debate is started, and the turn-based orchestration begins.
3. **Generate Turns**: For each turn, the service retrieves the context, generates a response using the LLM service, and adds the new turn to the debate.
4. **Complete Debate**: Once the debate has reached its conclusion, it is marked as complete.
