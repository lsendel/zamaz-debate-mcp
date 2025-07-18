const express = require('express');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// LLM Providers data
const providers = [
  {
    name: 'claude',
    models: [
      'claude-3-opus-20240229',
      'claude-3-sonnet-20240229',
      'claude-3-haiku-20240307',
      'claude-2.1',
      'claude-2.0'
    ],
    features: {
      streaming: true,
      functionCalling: true,
      visionSupport: true,
      maxTokens: 200000
    },
    status: 'available'
  },
  {
    name: 'openai',
    models: [
      'gpt-4-turbo-preview',
      'gpt-4',
      'gpt-4-32k',
      'gpt-3.5-turbo',
      'gpt-3.5-turbo-16k'
    ],
    features: {
      streaming: true,
      functionCalling: true,
      visionSupport: true,
      maxTokens: 128000
    },
    status: 'available'
  },
  {
    name: 'gemini',
    models: [
      'gemini-pro',
      'gemini-pro-vision',
      'gemini-ultra'
    ],
    features: {
      streaming: true,
      functionCalling: true,
      visionSupport: true,
      maxTokens: 32000
    },
    status: 'available'
  },
  {
    name: 'ollama',
    models: [
      'llama2',
      'mistral',
      'mixtral',
      'codellama',
      'neural-chat'
    ],
    features: {
      streaming: true,
      functionCalling: false,
      visionSupport: false,
      maxTokens: 4096
    },
    status: 'available'
  }
];

// Routes
app.get('/resources', (req, res) => {
  const resources = providers.map(p => ({
    uri: `provider://${p.name}`,
    name: p.name,
    type: 'provider'
  }));
  res.json(resources);
});

app.get('/resources/:uri', (req, res) => {
  const providerName = req.params.uri.replace('provider://', '');
  const provider = providers.find(p => p.name === providerName);
  if (provider) {
    res.json(provider);
  } else {
    res.status(404).json({ error: 'Provider not found' });
  }
});

app.post('/tools/complete', (req, res) => {
  // Simple mock completion
  res.json({
    content: 'This is a mock response from the LLM provider.',
    model: req.body.model || 'mock-model',
    usage: {
      promptTokens: 10,
      completionTokens: 20,
      totalTokens: 30
    }
  });
});

app.get('/models', (req, res) => {
  const allModels = [];
  providers.forEach(provider => {
    provider.models.forEach(model => {
      allModels.push({
        provider: provider.name,
        model: model,
        description: `${provider.name} ${model}`,
        contextWindow: provider.features.maxTokens,
        pricing: { inputTokens: 0.01, outputTokens: 0.03 },
        capabilities: ['reasoning', 'coding', 'analysis']
      });
    });
  });
  res.json(allModels);
});

app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    providers: Object.fromEntries(providers.map(p => [p.name, p.status]))
  });
});

app.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP' });
});

// Organization API mock endpoints
app.post('/auth/login', (req, res) => {
  const { username, password } = req.body;
  if (username === 'demo' && password === 'demo123') {
    res.json({
      token: 'mock-jwt-token',
      user: {
        id: 'user-1',
        username: 'demo',
        email: 'demo@example.com',
        organizationId: 'org-123'
      }
    });
  } else {
    res.status(401).json({ error: 'Invalid credentials' });
  }
});

app.get('/organizations/:id', (req, res) => {
  res.json({
    id: req.params.id,
    name: 'Demo Organization',
    description: 'Testing organization',
    createdAt: new Date().toISOString()
  });
});

// Debate API mock endpoints
app.post('/tools/create_debate', (req, res) => {
  const debate = {
    id: 'debate-' + Date.now(),
    topic: req.body.topic,
    description: req.body.description,
    status: 'created',
    participants: req.body.participants.map((p, i) => ({
      id: 'participant-' + i,
      ...p
    })),
    rounds: [],
    maxRounds: req.body.maxRounds || 5,
    currentRound: 0,
    organizationId: 'org-123',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  res.json(debate);
});

app.get('/debates', (req, res) => {
  res.json([
    {
      id: 'debate-1',
      topic: 'The Impact of AI on Society',
      description: 'A comprehensive debate about AI impacts',
      status: 'completed',
      participants: [
        {
          id: 'p1',
          name: 'AI Optimist',
          llmProvider: 'claude',
          model: 'claude-3-opus-20240229'
        },
        {
          id: 'p2',
          name: 'AI Skeptic',
          llmProvider: 'openai',
          model: 'gpt-4'
        }
      ],
      rounds: [],
      maxRounds: 5,
      currentRound: 5,
      organizationId: 'org-123',
      createdAt: '2024-01-15T10:00:00Z',
      updatedAt: '2024-01-15T11:30:00Z'
    }
  ]);
});

// Start server
const PORT = process.env.PORT || 5002;
app.listen(PORT, () => {
  console.log(`Mock LLM server running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
  console.log(`Resources: http://localhost:${PORT}/resources`);
});