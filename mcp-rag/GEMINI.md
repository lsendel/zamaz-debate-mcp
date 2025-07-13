# GEMINI.md - MCP RAG Service

This document provides a concise overview of the `mcp-rag` service.

## Service Purpose

The `mcp-rag` service provides Retrieval-Augmented Generation (RAG) capabilities. It allows other services to upload documents, search for relevant information, and augment the context of a conversation with that information.

## Core Features

- **Document Management**: Upload, parse, and process documents in various formats.
- **Vector Embeddings**: Generate vector embeddings for document chunks to enable semantic search.
- **Semantic Search**: Search for relevant information in the document repository using natural language queries.
- **Context Augmentation**: Augment the context of a conversation with information retrieved from the document repository.

## Technical Stack

- **Language**: Python
- **Vector Database**: Qdrant
- **Database**: PostgreSQL
- **Embeddings**: OpenAI's `text-embedding-ada-002`

## Document Processing Pipeline

1. **Upload Document**: A document is uploaded with its content and metadata.
2. **Parse and Chunk**: The document is parsed and split into smaller chunks.
3. **Generate Embeddings**: Vector embeddings are generated for each chunk.
4. **Store in Qdrant**: The chunks and their embeddings are stored in the Qdrant vector database.

## Integration

The `mcp-rag` service can be used by any other service that needs to incorporate information from a document repository into its workflow. For example, the `mcp-debate` service could use it to provide evidence to the AI participants in a debate.
