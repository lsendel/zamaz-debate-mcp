import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics for monitoring system stability over time
const errorRate = new Rate('errors');
const memoryLeakIndicator = new Trend('response_time_degradation');
const successfulTransactions = new Counter('successful_transactions');
const failedTransactions = new Counter('failed_transactions');

export const options = {
  stages: [
    { duration: '5m', target: 50 },    // Ramp up to 50 users
    { duration: '4h', target: 50 },    // Stay at 50 users for 4 hours
    { duration: '5m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],     // 95% of requests under 2s
    errors: ['rate<0.02'],                 // Error rate under 2%
    http_req_failed: ['rate<0.02'],        // HTTP failure rate under 2%
    response_time_degradation: ['avg<100'], // Average degradation under 100ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'test-token';

// Track baseline response times
let baselineResponseTimes = {};
let isBaselineSet = false;
const BASELINE_DURATION = 300000; // 5 minutes in ms

// Helper function to get test endpoints
function getTestEndpoints() {
  return [
    { name: 'ListOrganizations', url: '/api/organization/list?page=0&size=20', method: 'GET' },
    { name: 'ListDebates', url: '/api/debate/list?page=0&size=20', method: 'GET' },
    { name: 'HealthCheck', url: '/health', method: 'GET' },
    { name: 'SearchDebates', url: '/api/debate/search?topic=test&page=0&size=10', method: 'GET' },
  ];
}

// Helper function to handle baseline tracking
function handleBaseline(endpoint, response, testStartTime) {
  if (testStartTime - __ITER * 1000 < BASELINE_DURATION) {
    trackBaselineResponse(endpoint, response);
  } else {
    checkPerformanceDegradation(endpoint, response);
  }
}

// Helper function to track baseline response times
function trackBaselineResponse(endpoint, response) {
  if (!baselineResponseTimes[endpoint.name]) {
    baselineResponseTimes[endpoint.name] = [];
  }
  baselineResponseTimes[endpoint.name].push(response.timings.duration);
}

// Helper function to check performance degradation
function checkPerformanceDegradation(endpoint, response) {
  if (!isBaselineSet) {
    calculateBaselineAverages();
  }
  
  const baseline = baselineResponseTimes[endpoint.name] || 1000;
  const degradation = response.timings.duration - baseline;
  if (degradation > 0) {
    memoryLeakIndicator.add(degradation);
  }
}

// Helper function to calculate baseline averages
function calculateBaselineAverages() {
  for (const [key, times] of Object.entries(baselineResponseTimes)) {
    const avg = times.reduce((a, b) => a + b, 0) / times.length;
    baselineResponseTimes[key] = avg;
  }
  isBaselineSet = true;
  console.log('Baseline response times set:', baselineResponseTimes);
}

export default function () {
  const testStartTime = Date.now();
  const endpoints = getTestEndpoints();
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  
  const params = {
    headers: {
      'Authorization': `Bearer ${JWT_TOKEN}`,
      'Content-Type': 'application/json',
    },
    tags: { name: endpoint.name },
    timeout: '10s',
  };

  const response = http[endpoint.method.toLowerCase()](
    `${BASE_URL}${endpoint.url}`,
    params
  );

  const checks = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 2000,
    'has body': (r) => r.body && r.body.length > 0,
  });

  if (checks) {
    successfulTransactions.add(1);
    handleBaseline(endpoint, response, testStartTime);
  } else {
    failedTransactions.add(1);
    errorRate.add(1);
  }

  // Simulate realistic user behavior with variable think time
  sleep(Math.random() * 5 + 2); // 2-7 seconds between requests

  // Every 100 iterations, perform a more complex operation
  if (__ITER % 100 === 0) {
    // Create a debate to test resource cleanup
    const debatePayload = JSON.stringify({
      title: `Soak Test Debate ${Date.now()}`,
      topic: 'Long running stability test',
      organizationId: 'soak-test-org',
      participants: [
        {
          name: 'StabilityBot1',
          position: 'PRO',
          aiProvider: 'CLAUDE',
          model: 'claude-3-opus-20240229'
        },
        {
          name: 'StabilityBot2',
          position: 'CON',
          aiProvider: 'OPENAI',
          model: 'gpt-4'
        }
      ],
      config: {
        maxRounds: 2,
        responseTimeout: 10000,
        maxResponseLength: 200
      }
    });

    const createResponse = http.post(
      `${BASE_URL}/api/debate/create`,
      debatePayload,
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${JWT_TOKEN}`
        },
        tags: { name: 'CreateDebateSoakTest' },
        timeout: '20s'
      }
    );

    check(createResponse, {
      'debate created': (r) => [200, 429].includes(r.status), // 429 is acceptable (rate limit)
    });
  }
}

export function handleSummary(data) {
  // Calculate performance degradation over time
  const degradation = data.metrics.response_time_degradation;
  const avgDegradation = degradation ? degradation.values.avg : 0;
  
  const summary = {
    testDuration: data.state.testRunDurationMs,
    totalRequests: data.metrics.http_reqs.values.count,
    successfulTransactions: successfulTransactions.values.count,
    failedTransactions: failedTransactions.values.count,
    errorRate: data.metrics.errors.values.rate,
    averageResponseTime: data.metrics.http_req_duration.values.avg,
    p95ResponseTime: data.metrics.http_req_duration.values['p(95)'],
    performanceDegradation: avgDegradation,
    possibleMemoryLeak: avgDegradation > 100,
  };

  return {
    'soak-test-results.json': JSON.stringify(summary, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}