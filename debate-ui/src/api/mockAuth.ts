// Mock authentication for development when backend is not running
export const MOCK_CREDENTIALS = {
  username: 'demo',
  password: 'demo123'
};

export const MOCK_USER = {
  id: 'user-123',
  username: 'demo',
  email: 'demo@zamaz.com',
  organizationId: 'org-123',
  role: 'admin',
  createdAt: new Date().toISOString()
};

export const MOCK_ORGANIZATION = {
  id: 'org-123',
  name: 'Demo Organization',
  description: 'Development testing organization',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString()
};

export const MOCK_AUTH_RESPONSE = {
  token: 'mock-jwt-token-' + Date.now(),
  user: MOCK_USER,
  organization: MOCK_ORGANIZATION
};