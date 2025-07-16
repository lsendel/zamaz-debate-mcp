# Product Overview

## Zamaz Debate MCP Services

A multi-service Model Context Protocol (MCP) system for managing AI-powered debates with multi-tenant support.

### Core Purpose
- Enable structured debates between multiple AI participants
- Provide unified access to various LLM providers (Claude, OpenAI, Gemini, Llama)
- Support multi-tenant organizations with proper isolation
- Offer context management and retrieval augmented generation (RAG)

### Key Services
1. **mcp-organization** (Port 5005): Multi-tenant organization management with JWT auth
2. **mcp-llm** (Port 5002): LLM provider gateway supporting multiple AI providers
3. **mcp-controller** (Port 5013): Debate orchestration and workflow management
4. **mcp-rag** (Port 5004): Retrieval Augmented Generation service
5. **mcp-template** (Port 5006): Template management service
6. **mcp-context** (Port 5001): Context management service

### Frontend
- React TypeScript application with Material-UI
- Redux for state management
- Real-time updates via WebSocket

### Target Users
- Developers building AI debate systems
- Organizations needing structured AI conversations
- Researchers studying AI interaction patterns