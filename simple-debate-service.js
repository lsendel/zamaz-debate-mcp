#!/usr/bin/env node

const express = require('express');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// In-memory data store for debates
let debates = [
  {
    id: "debate-001",
    title: "AI Ethics in Healthcare",
    topic: "Should AI make medical decisions?",
    status: "COMPLETED",
    format: "OXFORD",
    participants: ["claude-3-opus", "gpt-4"],
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
    organizationId: "org-001",
    createdBy: "user-001"
  },
  {
    id: "debate-002", 
    title: "Climate Change Solutions",
    topic: "Is nuclear energy the answer?",
    status: "IN_PROGRESS",
    format: "LINCOLN_DOUGLAS",
    participants: ["claude-3-sonnet", "gemini-pro"],
    createdAt: "2024-01-02T00:00:00Z",
    updatedAt: "2024-01-02T00:00:00Z",
    organizationId: "org-001",
    createdBy: "user-002"
  },
  {
    id: "debate-003",
    title: "Future of Work",
    topic: "Will automation replace human jobs?",
    status: "CREATED",
    format: "OXFORD",
    participants: ["gpt-3.5-turbo", "claude-3-opus"],
    createdAt: "2024-01-03T00:00:00Z",
    updatedAt: "2024-01-03T00:00:00Z",
    organizationId: "org-002",
    createdBy: "user-003"
  }
];

// Get all debates
app.get('/api/v1/debates', (req, res) => {
  const { organizationId, status, format } = req.query;
  let filteredDebates = debates;
  
  if (organizationId) {
    filteredDebates = filteredDebates.filter(d => d.organizationId === organizationId);
  }
  
  if (status) {
    filteredDebates = filteredDebates.filter(d => d.status === status);
  }
  
  if (format) {
    filteredDebates = filteredDebates.filter(d => d.format === format);
  }
  
  res.json(filteredDebates);
});

// Get debate by ID
app.get('/api/v1/debates/:id', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  res.json(debate);
});

// Create new debate
app.post('/api/v1/debates', (req, res) => {
  const { title, topic, format = 'OXFORD', participants = [], organizationId, createdBy } = req.body;
  
  if (!title || !topic) {
    return res.status(400).json({ error: 'Title and topic are required' });
  }
  
  const newDebate = {
    id: `debate-${Date.now()}`,
    title,
    topic,
    status: 'CREATED',
    format,
    participants: Array.isArray(participants) ? participants : [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    organizationId: organizationId || 'org-001',
    createdBy: createdBy || 'user-001'
  };
  
  debates.push(newDebate);
  res.status(201).json(newDebate);
});

// Update debate
app.put('/api/v1/debates/:id', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const { title, topic, status, format, participants } = req.body;
  
  if (title) debate.title = title;
  if (topic) debate.topic = topic;
  if (status) debate.status = status;
  if (format) debate.format = format;
  if (participants && Array.isArray(participants)) debate.participants = participants;
  
  debate.updatedAt = new Date().toISOString();
  
  res.json(debate);
});

// Delete debate
app.delete('/api/v1/debates/:id', (req, res) => {
  const index = debates.findIndex(d => d.id === req.params.id);
  if (index === -1) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  debates.splice(index, 1);
  res.status(204).send();
});

// Start debate
app.post('/api/v1/debates/:id/start', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  if (debate.status !== 'CREATED') {
    return res.status(400).json({ error: 'Debate can only be started from CREATED status' });
  }
  
  debate.status = 'IN_PROGRESS';
  debate.updatedAt = new Date().toISOString();
  
  res.json(debate);
});

// Complete debate
app.post('/api/v1/debates/:id/complete', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  if (debate.status !== 'IN_PROGRESS') {
    return res.status(400).json({ error: 'Debate can only be completed from IN_PROGRESS status' });
  }
  
  debate.status = 'COMPLETED';
  debate.updatedAt = new Date().toISOString();
  
  res.json(debate);
});

// Get debate formats
app.get('/api/v1/debate-formats', (req, res) => {
  res.json([
    {
      id: 'OXFORD',
      name: 'Oxford Style',
      description: 'Traditional Oxford-style debate with opening statements, rebuttals, and closing arguments'
    },
    {
      id: 'LINCOLN_DOUGLAS',
      name: 'Lincoln-Douglas',
      description: 'One-on-one debate format focusing on philosophical and ethical issues'
    },
    {
      id: 'PARLIAMENTARY',
      name: 'Parliamentary',
      description: 'Team-based debate format with government and opposition sides'
    },
    {
      id: 'STRUCTURED',
      name: 'Structured',
      description: 'Highly structured format with specific time limits and rounds'
    }
  ]);
});

// Get debate statistics
app.get('/api/v1/debates/stats', (req, res) => {
  const { organizationId } = req.query;
  let filteredDebates = debates;
  
  if (organizationId) {
    filteredDebates = filteredDebates.filter(d => d.organizationId === organizationId);
  }
  
  const stats = {
    total: filteredDebates.length,
    byStatus: {
      CREATED: filteredDebates.filter(d => d.status === 'CREATED').length,
      IN_PROGRESS: filteredDebates.filter(d => d.status === 'IN_PROGRESS').length,
      COMPLETED: filteredDebates.filter(d => d.status === 'COMPLETED').length
    },
    byFormat: {
      OXFORD: filteredDebates.filter(d => d.format === 'OXFORD').length,
      LINCOLN_DOUGLAS: filteredDebates.filter(d => d.format === 'LINCOLN_DOUGLAS').length,
      PARLIAMENTARY: filteredDebates.filter(d => d.format === 'PARLIAMENTARY').length,
      STRUCTURED: filteredDebates.filter(d => d.format === 'STRUCTURED').length
    }
  };
  
  res.json(stats);
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

const PORT = process.env.PORT || 5013;
app.listen(PORT, () => {
  console.log(`🚀 Simple Debate Service running on port ${PORT}`);
  console.log(`📋 Available endpoints:`);
  console.log(`   GET    /api/v1/debates - List all debates`);
  console.log(`   POST   /api/v1/debates - Create new debate`);
  console.log(`   GET    /api/v1/debates/:id - Get debate by ID`);
  console.log(`   PUT    /api/v1/debates/:id - Update debate`);
  console.log(`   DELETE /api/v1/debates/:id - Delete debate`);
  console.log(`   POST   /api/v1/debates/:id/start - Start debate`);
  console.log(`   POST   /api/v1/debates/:id/complete - Complete debate`);
  console.log(`   GET    /api/v1/debate-formats - Get available debate formats`);
  console.log(`   GET    /api/v1/debates/stats - Get debate statistics`);
  console.log(`   GET    /actuator/health - Health check`);
  console.log(`\n✅ Service is ready to accept requests!`);
  console.log(`\n📊 Available debates: ${debates.length}`);
});