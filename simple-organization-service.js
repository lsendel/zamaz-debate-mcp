#!/usr/bin/env node

const express = require('express');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// In-memory data store
let organizations = [
  {
    id: "org-001",
    name: "Acme Corporation",
    description: "Leading technology company",
    apiKey: "ak_test_abc123",
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z"
  },
  {
    id: "org-002", 
    name: "Tech Solutions Inc",
    description: "Software development and consulting",
    apiKey: "ak_test_def456",
    createdAt: "2024-01-02T00:00:00Z",
    updatedAt: "2024-01-02T00:00:00Z"
  },
  {
    id: "org-003",
    name: "Global Enterprises",
    description: "International business solutions",
    apiKey: null,
    createdAt: "2024-01-03T00:00:00Z",
    updatedAt: "2024-01-03T00:00:00Z"
  }
];

let users = [
  {
    id: "user-001",
    username: "admin",
    email: "admin@acme.com",
    organizationId: "org-001",
    role: "admin",
    createdAt: "2024-01-01T00:00:00Z"
  },
  {
    id: "user-002",
    username: "john.doe",
    email: "john@acme.com", 
    organizationId: "org-001",
    role: "member",
    createdAt: "2024-01-02T00:00:00Z"
  },
  {
    id: "user-003",
    username: "jane.smith",
    email: "jane@techsolutions.com",
    organizationId: "org-002", 
    role: "admin",
    createdAt: "2024-01-03T00:00:00Z"
  }
];

// Organization endpoints
app.get('/api/v1/organizations', (req, res) => {
  res.json(organizations);
});

app.get('/api/v1/organizations/:id', (req, res) => {
  const org = organizations.find(o => o.id === req.params.id);
  if (!org) {
    return res.status(404).json({ error: 'Organization not found' });
  }
  res.json(org);
});

app.post('/api/v1/organizations', (req, res) => {
  const { name, description } = req.body;
  
  if (!name) {
    return res.status(400).json({ error: 'Organization name is required' });
  }
  
  const newOrg = {
    id: `org-${Date.now()}`,
    name,
    description: description || '',
    apiKey: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  
  organizations.push(newOrg);
  res.status(201).json(newOrg);
});

app.put('/api/v1/organizations/:id', (req, res) => {
  const org = organizations.find(o => o.id === req.params.id);
  if (!org) {
    return res.status(404).json({ error: 'Organization not found' });
  }
  
  const { name, description } = req.body;
  if (name) org.name = name;
  if (description !== undefined) org.description = description;
  org.updatedAt = new Date().toISOString();
  
  res.json(org);
});

app.delete('/api/v1/organizations/:id', (req, res) => {
  const index = organizations.findIndex(o => o.id === req.params.id);
  if (index === -1) {
    return res.status(404).json({ error: 'Organization not found' });
  }
  
  organizations.splice(index, 1);
  res.status(204).send();
});

// API Key management
app.post('/api/v1/organizations/:id/api-keys', (req, res) => {
  const org = organizations.find(o => o.id === req.params.id);
  if (!org) {
    return res.status(404).json({ error: 'Organization not found' });
  }
  
  const apiKey = `ak_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  org.apiKey = apiKey;
  org.updatedAt = new Date().toISOString();
  
  res.json({ apiKey });
});

app.delete('/api/v1/organizations/:id/api-keys', (req, res) => {
  const org = organizations.find(o => o.id === req.params.id);
  if (!org) {
    return res.status(404).json({ error: 'Organization not found' });
  }
  
  org.apiKey = null;
  org.updatedAt = new Date().toISOString();
  
  res.status(204).send();
});

// User management endpoints
app.get('/api/v1/users', (req, res) => {
  const { organizationId } = req.query;
  let filteredUsers = users;
  
  if (organizationId) {
    filteredUsers = users.filter(u => u.organizationId === organizationId);
  }
  
  res.json(filteredUsers);
});

app.post('/api/v1/users', (req, res) => {
  const { username, email, password, role = 'member', organizationId } = req.body;
  
  if (!username || !email || !password) {
    return res.status(400).json({ error: 'Username, email, and password are required' });
  }
  
  // Check if user already exists
  const existingUser = users.find(u => u.username === username || u.email === email);
  if (existingUser) {
    return res.status(409).json({ error: 'User already exists' });
  }
  
  const newUser = {
    id: `user-${Date.now()}`,
    username,
    email,
    organizationId: organizationId || organizations[0]?.id,
    role,
    createdAt: new Date().toISOString()
  };
  
  users.push(newUser);
  res.status(201).json(newUser);
});

// Health check
app.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP' });
});

// Current organization (for compatibility)
app.get('/api/v1/current-organization', (req, res) => {
  res.json(organizations[0] || null);
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

const PORT = process.env.PORT || 5005;
app.listen(PORT, () => {
  console.log(`🚀 Simple Organization Service running on port ${PORT}`);
  console.log(`📋 Available endpoints:`);
  console.log(`   GET    /api/v1/organizations - List all organizations`);
  console.log(`   POST   /api/v1/organizations - Create new organization`);
  console.log(`   GET    /api/v1/organizations/:id - Get organization by ID`);
  console.log(`   PUT    /api/v1/organizations/:id - Update organization`);
  console.log(`   DELETE /api/v1/organizations/:id - Delete organization`);
  console.log(`   POST   /api/v1/organizations/:id/api-keys - Generate API key`);
  console.log(`   DELETE /api/v1/organizations/:id/api-keys - Revoke API key`);
  console.log(`   GET    /api/v1/users - List users`);
  console.log(`   POST   /api/v1/users - Create new user`);
  console.log(`   GET    /actuator/health - Health check`);
  console.log(`   GET    /api/v1/current-organization - Get current organization`);
  console.log(`\n✅ Service is ready to accept requests!`);
});