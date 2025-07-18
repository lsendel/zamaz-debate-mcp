#!/usr/bin/env node

const express = require('express');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// In-memory data store for LLM providers and models
const providers = [
  {
    id: "openai",
    name: "OpenAI",
    apiEndpoint: "https://api.openai.com/v1",
    authType: "API_KEY",
    status: "ACTIVE",
    models: [
      {
        id: "gpt-4",
        name: "GPT-4",
        provider: "openai",
        type: "CHAT",
        maxTokens: 8192,
        costPerToken: 0.00003,
        capabilities: ["text-generation", "code-generation", "analysis"]
      },
      {
        id: "gpt-3.5-turbo",
        name: "GPT-3.5 Turbo",
        provider: "openai", 
        type: "CHAT",
        maxTokens: 4096,
        costPerToken: 0.000002,
        capabilities: ["text-generation", "conversation"]
      }
    ]
  },
  {
    id: "anthropic",
    name: "Anthropic",
    apiEndpoint: "https://api.anthropic.com/v1",
    authType: "API_KEY",
    status: "ACTIVE",
    models: [
      {
        id: "claude-3-opus",
        name: "Claude 3 Opus",
        provider: "anthropic",
        type: "CHAT",
        maxTokens: 200000,
        costPerToken: 0.000015,
        capabilities: ["text-generation", "analysis", "reasoning"]
      },
      {
        id: "claude-3-sonnet",
        name: "Claude 3 Sonnet", 
        provider: "anthropic",
        type: "CHAT",
        maxTokens: 200000,
        costPerToken: 0.000003,
        capabilities: ["text-generation", "analysis"]
      }
    ]
  },
  {
    id: "google",
    name: "Google AI",
    apiEndpoint: "https://generativelanguage.googleapis.com/v1",
    authType: "API_KEY",
    status: "ACTIVE",
    models: [
      {
        id: "gemini-pro",
        name: "Gemini Pro",
        provider: "google",
        type: "CHAT",
        maxTokens: 32768,
        costPerToken: 0.00000025,
        capabilities: ["text-generation", "multimodal", "reasoning"]
      }
    ]
  }
];

// Get all providers
app.get('/api/v1/providers', (req, res) => {
  res.json(providers);
});

// Get provider by ID
app.get('/api/v1/providers/:id', (req, res) => {
  const provider = providers.find(p => p.id === req.params.id);
  if (!provider) {
    return res.status(404).json({ error: 'Provider not found' });
  }
  res.json(provider);
});

// Get all models
app.get('/api/v1/models', (req, res) => {
  const allModels = providers.flatMap(p => p.models);
  res.json(allModels);
});

// Get models by provider
app.get('/api/v1/providers/:providerId/models', (req, res) => {
  const provider = providers.find(p => p.id === req.params.providerId);
  if (!provider) {
    return res.status(404).json({ error: 'Provider not found' });
  }
  res.json(provider.models);
});

// Get model by ID
app.get('/api/v1/models/:id', (req, res) => {
  const allModels = providers.flatMap(p => p.models);
  const model = allModels.find(m => m.id === req.params.id);
  if (!model) {
    return res.status(404).json({ error: 'Model not found' });
  }
  res.json(model);
});

// Simple chat completion endpoint (mock)
app.post('/api/v1/chat/completions', (req, res) => {
  const { model, messages, max_tokens = 100 } = req.body;
  
  if (!model || !messages || !Array.isArray(messages)) {
    return res.status(400).json({ error: 'Model and messages are required' });
  }
  
  // Find the model
  const allModels = providers.flatMap(p => p.models);
  const selectedModel = allModels.find(m => m.id === model);
  
  if (!selectedModel) {
    return res.status(404).json({ error: 'Model not found' });
  }
  
  // Mock response
  const response = {
    id: `chatcmpl-${Date.now()}`,
    object: "chat.completion",
    created: Math.floor(Date.now() / 1000),
    model: model,
    choices: [
      {
        index: 0,
        message: {
          role: "assistant",
          content: `This is a mock response from ${selectedModel.name}. The user asked: "${messages[messages.length - 1]?.content || 'Unknown'}". This is a simulated response for testing purposes.`
        },
        finish_reason: "stop"
      }
    ],
    usage: {
      prompt_tokens: 50,
      completion_tokens: 25,
      total_tokens: 75
    }
  };
  
  res.json(response);
});

// Provider health check
app.get('/api/v1/providers/:id/health', (req, res) => {
  const provider = providers.find(p => p.id === req.params.id);
  if (!provider) {
    return res.status(404).json({ error: 'Provider not found' });
  }
  
  res.json({
    provider: provider.id,
    status: provider.status,
    timestamp: new Date().toISOString(),
    latency: Math.floor(Math.random() * 100) + 20, // Mock latency
    available: true
  });
});

// Health check
app.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP' });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

const PORT = process.env.PORT || 5002;
app.listen(PORT, () => {
  console.log(`🚀 Simple LLM Service running on port ${PORT}`);
  console.log(`📋 Available endpoints:`);
  console.log(`   GET    /api/v1/providers - List all LLM providers`);
  console.log(`   GET    /api/v1/providers/:id - Get provider by ID`);
  console.log(`   GET    /api/v1/models - List all available models`);
  console.log(`   GET    /api/v1/providers/:providerId/models - Get models by provider`);
  console.log(`   GET    /api/v1/models/:id - Get model by ID`);
  console.log(`   POST   /api/v1/chat/completions - Chat completion (mock)`);
  console.log(`   GET    /api/v1/providers/:id/health - Check provider health`);
  console.log(`   GET    /actuator/health - Health check`);
  console.log(`\n✅ Service is ready to accept requests!`);
  console.log(`\n📊 Available providers: ${providers.length}`);
  console.log(`📊 Available models: ${providers.flatMap(p => p.models).length}`);
});