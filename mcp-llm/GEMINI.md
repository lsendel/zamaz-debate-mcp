# GEMINI.md - MCP LLM Service

This document provides a concise overview of the `mcp-llm` service.

## Service Purpose

The `mcp-llm` service provides a unified interface for interacting with multiple Large Language Model (LLM) providers. It abstracts the complexities of each provider's API, allowing other services to consume them through a consistent interface.

## Core Features

- **Multi-provider Support**: Supports a variety of LLM providers, including OpenAI, Anthropic, Google, and more.
- **Streaming Responses**: Can stream responses from the LLM, which is useful for real-time applications.
- **Token Counting**: Provides utilities for counting the number of tokens in a request.
- **Caching**: Caches responses from the LLM to reduce latency and cost.
- **Error Handling**: Implements retry logic and handles provider-specific errors.

## Supported Providers

The service supports a wide range of LLM providers, including:

- OpenAI (GPT models)
- Anthropic (Claude models)
- Google (Gemini models)
- Ollama (local models)
- X.AI (Grok)
- Alibaba (Qwen)

## Integration

Other services, such as the `mcp-debate` service, can use the `mcp-llm` service to generate text by sending a request that specifies the provider, model, and messages. The service will then handle the interaction with the LLM and return the response.

## Key Development Tasks

- **Adding a New Provider**: To add a new provider, a new provider class must be created that implements the `BaseLLMProvider` interface.
- **Implementing Streaming**: The `stream_generate` method must be implemented for each provider to support streaming responses.
- **Token Counting**: A token counting method should be implemented for each provider to ensure accurate token counting.
