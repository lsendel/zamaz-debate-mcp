import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Below normal load
    { duration: '5m', target: 100 },   // Normal load
    { duration: '2m', target: 200 },   // Around breaking point
    { duration: '5m', target: 200 },   // At breaking point
    { duration: '2m', target: 300 },   // Beyond breaking point
    { duration: '5m', target: 300 },   // Beyond breaking point
    { duration: '10m', target: 0 },    // Recovery stage
  ],
  thresholds: {
    http_req_duration: ['p(99)<5000'], // 99% of requests must complete below 5s
    errors: ['rate<0.1'],              // Error rate must be below 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'test-token';

export default function () {
  // Simple health check under stress
  const healthResponse = http.get(`${BASE_URL}/health`, {
    tags: { name: 'HealthCheck' },
    timeout: '5s'
  });

  const healthCheck = check(healthResponse, {
    'health check ok': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 1000,
  });

  if (!healthCheck) {
    errorRate.add(1);
  }

  // Try to create a debate under stress
  const debatePayload = JSON.stringify({
    title: `Stress Test Debate ${Date.now()}`,
    topic: 'System under stress',
    organizationId: 'stress-test-org',
    participants: [
      {
        name: 'Bot1',
        position: 'PRO',
        aiProvider: 'CLAUDE',
        model: 'claude-3-opus-20240229'
      },
      {
        name: 'Bot2',
        position: 'CON',
        aiProvider: 'OPENAI',
        model: 'gpt-4'
      }
    ],
    config: {
      maxRounds: 1,
      responseTimeout: 5000,
      maxResponseLength: 100
    }
  });

  const createResponse = http.post(`${BASE_URL}/api/debate/create`, debatePayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${JWT_TOKEN}`
    },
    tags: { name: 'CreateDebateUnderStress' },
    timeout: '10s'
  });

  const createCheck = check(createResponse, {
    'debate created or rate limited': (r) => [200, 429, 503].includes(r.status),
    'not timeout': (r) => r.timings.duration < 10000,
  });

  if (!createCheck || createResponse.status >= 500) {
    errorRate.add(1);
  }

  // Check rate limiting is working
  if (createResponse.status === 429) {
    check(createResponse, {
      'has rate limit headers': (r) => r.headers['X-RateLimit-Limit'] !== undefined,
    });
  }

  sleep(Math.random() * 2);
}