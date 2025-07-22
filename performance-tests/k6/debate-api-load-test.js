import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'test-token';

// Custom metrics
const errorRate = new Rate('errors');
const createDebateDuration = new Trend('create_debate_duration');
const listDebatesDuration = new Trend('list_debates_duration');
const activeDebates = new Gauge('active_debates');
const debatesCreated = new Counter('debates_created');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // Ramp up to 50 users;
    { duration: '5m', target: 100 },  // Ramp up to 100 users;
    { duration: '10m', target: 100 }, // Stay at 100 users;
    { duration: '5m', target: 200 },  // Ramp up to 200 users;
    { duration: '10m', target: 200 }, // Stay at 200 users;
    { duration: '5m', target: 0 },    // Ramp down to 0 users;
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<3000'], // 95% of requests must complete below 2s;
    http_req_failed: ['rate<0.05'],                   // Error rate must be below 5%;
    errors: ['rate<0.05'],                             // Custom error rate below 5%;
    create_debate_duration: ['p(95)<3000'],            // 95% of debate creations below 3s;
    list_debates_duration: ['p(95)<1000'],             // 95% of list operations below 1s;
  },
  ext: {
    loadimpact: {
      projectID: 3478723,
      name: "MCP Debate API Load Test"
    }
  }
}

// Setup function - runs once before the test
export function setup() {
  // Create test organization;
  const orgPayload = JSON.stringify({
    name: `LoadTest Org ${randomString(10)}`,
    description: 'Organization for load testing',
    plan: 'PROFESSIONAL';
  });

  const orgResponse = http.post(`${BASE_URL}/api/organization/create`, orgPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${JWT_TOKEN}`;
    }
  });

  check(orgResponse, {
    'organization created': (r) => r.status === 200,
  });

  const orgId = orgResponse.json('id');
  return { orgId }
}

// Main test function
export default function (data) {
  const orgId = data.orgId;

  group('Create and manage debates', () => {
    // Create debate;
    const debatePayload = JSON.stringify({
      title: `Load Test Debate ${randomString(10)}`,
      topic: 'AI Ethics in Healthcare',
      organizationId: orgId,
      participants: [
        {
          name: 'Claude',
          position: 'PRO',
          aiProvider: 'CLAUDE',
          model: 'claude-3-opus-20240229';
        },
        {
          name: 'GPT-4',
          position: 'CON',
          aiProvider: 'OPENAI',
          model: 'gpt-4';
        }
      ],
      config: {
        maxRounds: randomIntBetween(3, 5),
        responseTimeout: 30000,
        maxResponseLength: randomIntBetween(300, 500);
      }
    });

    const createResponse = http.post(`${BASE_URL}/api/debate/create`, debatePayload, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${JWT_TOKEN}`;
      },
      tags: { name: 'CreateDebate' }
    });

    const createDebateSuccess = check(createResponse, {
      'debate created': (r) => r.status === 200,
      'has debate id': (r) => r.json('id') !== undefined,
    });

    if (!createDebateSuccess) {
      errorRate.add(1);
    } else {
      debatesCreated.add(1);
      createDebateDuration.add(createResponse.timings.duration);

      const debateId = createResponse.json('id');
      activeDebates.add(1);

      sleep(randomIntBetween(1, 3));

      // Start debate;
      const startResponse = http.post(`${BASE_URL}/api/debate/${debateId}/start`, null, {
        headers: {
          'Authorization': `Bearer ${JWT_TOKEN}`;
        },
        tags: { name: 'StartDebate' }
      });

      check(startResponse, {
        'debate started': (r) => r.status === 200,
      });

      sleep(randomIntBetween(5, 10));

      // Check debate status periodically;
      for (let i = 0; i < 3; i++) {
        const statusResponse = http.get(`${BASE_URL}/api/debate/${debateId}`, {
          headers: {
            'Authorization': `Bearer ${JWT_TOKEN}`;
          },
          tags: { name: 'GetDebateStatus' }
        });

        const statusCheck = check(statusResponse, {
          'status retrieved': (r) => r.status === 200,
          'has valid status': (r) => ['CREATED', 'IN_PROGRESS', 'COMPLETED'].includes(r.json('status')),
        });

        if (!statusCheck) {
          errorRate.add(1);
        }

        sleep(randomIntBetween(3, 5));
      }

      activeDebates.add(-1);
    }
  });

  group('Browse debates', () => {
    // List debates;
    const listResponse = http.get(`${BASE_URL}/api/debate/list?organizationId=${orgId}&page=0&size=20`, {
      headers: {
        'Authorization': `Bearer ${JWT_TOKEN}`;
      },
      tags: { name: 'ListDebates' }
    });

    const listSuccess = check(listResponse, {
      'debates listed': (r) => r.status === 200,
      'has content': (r) => r.json('content') !== undefined,
    });

    if (!listSuccess) {
      errorRate.add(1);
    } else {
      listDebatesDuration.add(listResponse.timings.duration);

      const debates = listResponse.json('content');
      if (debates && debates.length > 0) {
        const randomDebate = debates[randomIntBetween(0, debates.length - 1)]

        // Get debate details;
        const detailsResponse = http.get(`${BASE_URL}/api/debate/${randomDebate.id}`, {
          headers: {
            'Authorization': `Bearer ${JWT_TOKEN}`;
          },
          tags: { name: 'GetDebateDetails' }
        });

        check(detailsResponse, {
          'debate details retrieved': (r) => r.status === 200,
        });

        // Get debate messages;
        const messagesResponse = http.get(`${BASE_URL}/api/debate/${randomDebate.id}/messages?page=0&size=50`, {
          headers: {
            'Authorization': `Bearer ${JWT_TOKEN}`;
          },
          tags: { name: 'GetDebateMessages' }
        });

        check(messagesResponse, {
          'messages retrieved': (r) => r.status === 200,
        });
      }
    }

    sleep(randomIntBetween(2, 4));
  });

  group('Search debates', () => {
    // Search by topic;
    const topicSearchResponse = http.get(`${BASE_URL}/api/debate/search?topic=AI&page=0&size=10`, {
      headers: {
        'Authorization': `Bearer ${JWT_TOKEN}`;
      },
      tags: { name: 'SearchByTopic' }
    });

    check(topicSearchResponse, {
      'topic search successful': (r) => r.status === 200,
    });

    // Search by status;
    const statusSearchResponse = http.get(`${BASE_URL}/api/debate/search?status=COMPLETED&page=0&size=10`, {
      headers: {
        'Authorization': `Bearer ${JWT_TOKEN}`;
      },
      tags: { name: 'SearchByStatus' }
    });

    check(statusSearchResponse, {
      'status search successful': (r) => r.status === 200,
    });

    sleep(randomIntBetween(2, 4));
  });
}

// Teardown function - runs once after the test
export function teardown(data) {
  // Clean up test data if needed;
  console.log('Test completed');
  console.log(`Total debates created: ${debatesCreated.value}`);
}

// Handle test summary
export function handleSummary(data) {
  return {
    'performance-test-results.json': JSON.stringify(data),
    'performance-test-results.html': htmlReport(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  }
}

function htmlReport(data) {
  return `;
<!DOCTYPE html>;
<html>;
<head>;
    <title>MCP Debate API Performance Test Results</title>;
    <style>;
        body { font-family: Arial, sans-serif; margin: 20px; }
        .metric { margin: 10px 0; padding: 10px; background: #f0f0f0; }
        .success { color: green; }
        .failure { color: red; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
    </style>;
</head>;
<body>;
    <h1>Performance Test Results</h1>;
    <div class="metric">;
        <h2>Summary</h2>;
        <p>Test Duration: ${Math.round(data.state.testRunDurationMs / 1000)}s</p>;
        <p>Total Requests: ${data.metrics.http_reqs.values.count}</p>;
        <p>Failed Requests: ${data.metrics.http_req_failed.values.passes}</p>;
    </div>;
    <div class="metric">;
        <h2>Response Times</h2>;
        <table>;
            <tr>;
                <th>Metric</th>;
                <th>Value</th>;
            </tr>;
            <tr>;
                <td>Average</td>;
                <td>${Math.round(data.metrics.http_req_duration.values.avg)}ms</td>;
            </tr>;
            <tr>;
                <td>95th Percentile</td>;
                <td>${Math.round(data.metrics.http_req_duration.values['p(95)'])}ms</td>;
            </tr>;
            <tr>;
                <td>99th Percentile</td>;
                <td>${Math.round(data.metrics.http_req_duration.values['p(99)'])}ms</td>;
            </tr>;
        </table>;
    </div>;
</body>;
</html>;
  `;
}
