import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const recoveryTime = new Trend('recovery_time');

export const options = {
  stages: [
    { duration: '10s', target: 10 },    // Warm up
    { duration: '1m', target: 10 },     // Stay at 10 users
    { duration: '10s', target: 1000 },  // Spike to 1000 users
    { duration: '3m', target: 1000 },   // Stay at 1000 users
    { duration: '10s', target: 10 },    // Scale down to 10 users
    { duration: '3m', target: 10 },     // Recovery period
    { duration: '10s', target: 0 },     // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(99)<10000'], // 99% of requests must complete below 10s during spike
    errors: ['rate<0.2'],               // Error rate must be below 20% during spike
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'test-token';

let spikeStarted = false;
let spikeEnded = false;
let normalResponseTime = 0;

export default function () {
  const startTime = Date.now();
  
  // Monitor when spike starts and ends
  const currentVUs = __VU;
  if (currentVUs > 500 && !spikeStarted) {
    spikeStarted = true;
    console.log('Spike started');
  }
  if (currentVUs < 50 && spikeStarted && !spikeEnded) {
    spikeEnded = true;
    console.log('Spike ended, monitoring recovery');
  }

  // Simple request to test system behavior
  const response = http.get(`${BASE_URL}/api/debate/list?page=0&size=10`, {
    headers: {
      'Authorization': `Bearer ${JWT_TOKEN}`
    },
    tags: { name: 'ListDebatesDuringSpike' },
    timeout: '15s'
  });

  const responseOk = check(response, {
    'status is 200 or 503': (r) => [200, 503].includes(r.status),
    'has response': (r) => r.body !== null,
  });

  if (!responseOk || response.status >= 500) {
    errorRate.add(1);
  }

  // Track recovery time after spike
  if (spikeEnded && response.status === 200) {
    const responseTime = response.timings.duration;
    if (normalResponseTime === 0 && responseTime < 1000) {
      normalResponseTime = Date.now() - startTime;
      recoveryTime.add(normalResponseTime);
      console.log(`System recovered to normal response times after ${normalResponseTime}ms`);
    }
  }

  // During spike, shorter sleep to maintain pressure
  if (currentVUs > 500) {
    sleep(0.1);
  } else {
    sleep(1);
  }
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'spike-test-results.json': JSON.stringify(data),
  };
}