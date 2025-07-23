const { performance } = require('perf_hooks');
const cluster = require('cluster');
const os = require('os');
const path = require('path');

// Load test configuration
const config = {
  duration: 60000, // 1 minute
  concurrentWorkflows: 50,
  workflowsPerSecond: 10,
  scenarios: [
    { name: 'CI Pipeline', weight: 0.4, severity: 'medium' },
    { name: 'Security Scan', weight: 0.2, severity: 'high' },
    { name: 'Deploy Staging', weight: 0.2, severity: 'medium' },
    { name: 'Test Suite', weight: 0.1, severity: 'low' },
    { name: 'Deploy Production', weight: 0.1, severity: 'critical' }
  ]
};

// Performance metrics collector
class MetricsCollector {
  constructor() {
    this.metrics = {
      totalRequests: 0,
      successfulRequests: 0,
      failedRequests: 0,
      issuesCreated: 0,
      issuesUpdated: 0,
      notificationsSent: 0,
      apiRateLimitHits: 0,
      responseTimes: [],
      errorTypes: {},
      throughput: []
    };
    this.startTime = Date.now();
  }
  
  recordRequest(duration, success, type = 'unknown') {
    this.metrics.totalRequests++;
    this.metrics.responseTimes.push(duration);
    
    if (success) {
      this.metrics.successfulRequests++;
      if (type === 'issue-create') this.metrics.issuesCreated++;
      if (type === 'issue-update') this.metrics.issuesUpdated++;
      if (type === 'notification') this.metrics.notificationsSent++;
    } else {
      this.metrics.failedRequests++;
    }
  }
  
  recordError(errorType) {
    this.metrics.errorTypes[errorType] = (this.metrics.errorTypes[errorType] || 0) + 1;
  }
  
  recordRateLimitHit() {
    this.metrics.apiRateLimitHits++;
  }
  
  calculateStats() {
    const duration = (Date.now() - this.startTime) / 1000; // seconds
    const responseTimes = this.metrics.responseTimes;
    
    return {
      duration: duration,
      totalRequests: this.metrics.totalRequests,
      successRate: (this.metrics.successfulRequests / this.metrics.totalRequests * 100).toFixed(2) + '%',
      requestsPerSecond: (this.metrics.totalRequests / duration).toFixed(2),
      avgResponseTime: this.calculateAverage(responseTimes).toFixed(2) + 'ms',
      minResponseTime: Math.min(...responseTimes).toFixed(2) + 'ms',
      maxResponseTime: Math.max(...responseTimes).toFixed(2) + 'ms',
      p95ResponseTime: this.calculatePercentile(responseTimes, 95).toFixed(2) + 'ms',
      p99ResponseTime: this.calculatePercentile(responseTimes, 99).toFixed(2) + 'ms',
      issuesCreated: this.metrics.issuesCreated,
      issuesUpdated: this.metrics.issuesUpdated,
      notificationsSent: this.metrics.notificationsSent,
      rateLimitHits: this.metrics.apiRateLimitHits,
      errors: this.metrics.errorTypes
    };
  }
  
  calculateAverage(arr) {
    return arr.reduce((a, b) => a + b, 0) / arr.length;
  }
  
  calculatePercentile(arr, percentile) {
    const sorted = [...arr].sort((a, b) => a - b);
    const index = Math.ceil((percentile / 100) * sorted.length) - 1;
    return sorted[index];
  }
}

// Workflow failure simulator
class WorkflowFailureSimulator {
  constructor(metricsCollector) {
    this.metrics = metricsCollector;
    this.mockAPIs = this.setupMockAPIs();
  }
  
  setupMockAPIs() {
    return {
      github: {
        searchIssues: async () => {
          await this.simulateAPICall(50, 150);
          // Simulate 30% duplicate rate
          return Math.random() < 0.3 ? [{ number: Math.floor(Math.random() * 1000) }] : [];
        },
        createIssue: async () => {
          await this.simulateAPICall(100, 300);
          if (Math.random() < 0.02) { // 2% failure rate
            throw new Error('API_RATE_LIMIT');
          }
          return { number: Math.floor(Math.random() * 10000) };
        },
        updateIssue: async () => {
          await this.simulateAPICall(80, 200);
          return { success: true };
        }
      },
      slack: {
        sendNotification: async () => {
          await this.simulateAPICall(50, 100);
          return { ok: true };
        }
      },
      email: {
        sendEmail: async () => {
          await this.simulateAPICall(100, 500);
          return { messageId: 'test-' + Date.now() };
        }
      }
    };
  }
  
  async simulateAPICall(minDelay, maxDelay) {
    const delay = Math.random() * (maxDelay - minDelay) + minDelay;
    await new Promise(resolve => setTimeout(resolve, delay));
  }
  
  async simulateWorkflowFailure(scenario) {
    const startTime = performance.now();
    
    try {
      // Simulate failure detection
      await this.simulateAPICall(10, 50);
      
      // Check for duplicates
      const duplicates = await this.mockAPIs.github.searchIssues();
      
      let result;
      if (duplicates.length > 0) {
        // Update existing issue
        result = await this.mockAPIs.github.updateIssue();
        this.metrics.recordRequest(performance.now() - startTime, true, 'issue-update');
      } else {
        // Create new issue
        result = await this.mockAPIs.github.createIssue();
        this.metrics.recordRequest(performance.now() - startTime, true, 'issue-create');
      }
      
      // Send notifications for high/critical severity
      if (scenario.severity === 'high' || scenario.severity === 'critical') {
        await Promise.all([
          this.mockAPIs.slack.sendNotification(),
          scenario.severity === 'critical' ? this.mockAPIs.email.sendEmail() : Promise.resolve()
        ]);
        this.metrics.recordRequest(performance.now() - startTime, true, 'notification');
      }
      
    } catch (error) {
      this.metrics.recordRequest(performance.now() - startTime, false);
      this.metrics.recordError(error.message);
      
      if (error.message === 'API_RATE_LIMIT') {
        this.metrics.recordRateLimitHit();
        // Implement exponential backoff
        await this.simulateAPICall(1000, 5000);
      }
    }
  }
  
  selectScenario() {
    const random = Math.random();
    let accumulated = 0;
    
    for (const scenario of config.scenarios) {
      accumulated += scenario.weight;
      if (random < accumulated) {
        return scenario;
      }
    }
    
    return config.scenarios[0];
  }
}

// Load test runner
async function runLoadTest() {
  console.log('=€ Starting Workflow Failure Handler Load Test');
  console.log(`Duration: ${config.duration / 1000}s`);
  console.log(`Target throughput: ${config.workflowsPerSecond} workflows/second`);
  console.log(`Concurrent workflows: ${config.concurrentWorkflows}`);
  console.log('-------------------------------------------\n');
  
  const metricsCollector = new MetricsCollector();
  const simulator = new WorkflowFailureSimulator(metricsCollector);
  
  const startTime = Date.now();
  const endTime = startTime + config.duration;
  
  let activeRequests = 0;
  const requestInterval = 1000 / config.workflowsPerSecond;
  
  // Progress reporting
  const progressInterval = setInterval(() => {
    const elapsed = Date.now() - startTime;
    const progress = (elapsed / config.duration * 100).toFixed(1);
    const currentStats = metricsCollector.calculateStats();
    
    process.stdout.write(`\rProgress: ${progress}% | Requests: ${currentStats.totalRequests} | RPS: ${currentStats.requestsPerSecond} | Active: ${activeRequests}`);
  }, 1000);
  
  // Request generator
  const requestGenerator = setInterval(async () => {
    if (Date.now() >= endTime) {
      clearInterval(requestGenerator);
      return;
    }
    
    if (activeRequests < config.concurrentWorkflows) {
      activeRequests++;
      const scenario = simulator.selectScenario();
      
      simulator.simulateWorkflowFailure(scenario).finally(() => {
        activeRequests--;
      });
    }
  }, requestInterval);
  
  // Wait for test completion
  await new Promise(resolve => {
    const checkCompletion = setInterval(() => {
      if (Date.now() >= endTime && activeRequests === 0) {
        clearInterval(checkCompletion);
        clearInterval(progressInterval);
        resolve();
      }
    }, 100);
  });
  
  console.log('\n\n=Ê Load Test Results');
  console.log('===================\n');
  
  const finalStats = metricsCollector.calculateStats();
  
  console.log('Summary:');
  console.log(`  Duration: ${finalStats.duration}s`);
  console.log(`  Total Requests: ${finalStats.totalRequests}`);
  console.log(`  Success Rate: ${finalStats.successRate}`);
  console.log(`  Throughput: ${finalStats.requestsPerSecond} req/s`);
  
  console.log('\nResponse Times:');
  console.log(`  Average: ${finalStats.avgResponseTime}`);
  console.log(`  Min: ${finalStats.minResponseTime}`);
  console.log(`  Max: ${finalStats.maxResponseTime}`);
  console.log(`  P95: ${finalStats.p95ResponseTime}`);
  console.log(`  P99: ${finalStats.p99ResponseTime}`);
  
  console.log('\nOperations:');
  console.log(`  Issues Created: ${finalStats.issuesCreated}`);
  console.log(`  Issues Updated: ${finalStats.issuesUpdated}`);
  console.log(`  Notifications Sent: ${finalStats.notificationsSent}`);
  
  console.log('\nErrors:');
  console.log(`  Rate Limit Hits: ${finalStats.rateLimitHits}`);
  if (Object.keys(finalStats.errors).length > 0) {
    console.log('  Error Types:');
    for (const [type, count] of Object.entries(finalStats.errors)) {
      console.log(`    ${type}: ${count}`);
    }
  }
  
  // Performance analysis
  console.log('\n<¯ Performance Analysis:');
  
  const targetRPS = config.workflowsPerSecond;
  const actualRPS = parseFloat(finalStats.requestsPerSecond);
  const rpsAchievement = (actualRPS / targetRPS * 100).toFixed(1);
  
  console.log(`  Target RPS Achievement: ${rpsAchievement}%`);
  
  if (actualRPS < targetRPS * 0.9) {
    console.log('     System could not maintain target throughput');
    console.log('  Recommendations:');
    console.log('    - Increase API rate limits');
    console.log('    - Optimize database queries');
    console.log('    - Add caching layer');
  } else {
    console.log('   System successfully handled target load');
  }
  
  if (finalStats.rateLimitHits > 0) {
    console.log(`     Hit API rate limits ${finalStats.rateLimitHits} times`);
    console.log('  Recommendations:');
    console.log('    - Implement request queuing');
    console.log('    - Add exponential backoff');
    console.log('    - Consider API quota increase');
  }
  
  const p99 = parseFloat(finalStats.p99ResponseTime);
  if (p99 > 5000) {
    console.log('     P99 response time exceeds 5 seconds');
    console.log('  Recommendations:');
    console.log('    - Profile slow operations');
    console.log('    - Add database indexes');
    console.log('    - Implement async processing');
  }
  
  return finalStats;
}

// Distributed load test runner (using cluster)
async function runDistributedLoadTest() {
  const numCPUs = os.cpus().length;
  
  if (cluster.isMaster) {
    console.log(`=' Master ${process.pid} is running`);
    console.log(`=æ Forking ${numCPUs} workers for distributed load test\n`);
    
    const workerResults = [];
    
    // Fork workers
    for (let i = 0; i < numCPUs; i++) {
      const worker = cluster.fork();
      
      worker.on('message', (msg) => {
        if (msg.type === 'results') {
          workerResults.push(msg.data);
        }
      });
    }
    
    // Handle worker exit
    cluster.on('exit', (worker, code, signal) => {
      console.log(`Worker ${worker.process.pid} died`);
    });
    
    // Wait for all workers to complete
    await new Promise(resolve => {
      const checkInterval = setInterval(() => {
        if (workerResults.length === numCPUs) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);
    });
    
    // Aggregate results
    console.log('\n=Ê Aggregated Results from All Workers');
    console.log('=====================================\n');
    
    let totalRequests = 0;
    let totalSuccess = 0;
    let totalIssuesCreated = 0;
    let totalIssuesUpdated = 0;
    let totalNotifications = 0;
    let totalRateLimits = 0;
    
    for (const result of workerResults) {
      totalRequests += result.totalRequests;
      totalSuccess += result.successfulRequests;
      totalIssuesCreated += result.issuesCreated;
      totalIssuesUpdated += result.issuesUpdated;
      totalNotifications += result.notificationsSent;
      totalRateLimits += result.rateLimitHits;
    }
    
    console.log(`Total Requests: ${totalRequests}`);
    console.log(`Total Success Rate: ${(totalSuccess / totalRequests * 100).toFixed(2)}%`);
    console.log(`Aggregate Throughput: ${(totalRequests / (config.duration / 1000)).toFixed(2)} req/s`);
    console.log(`Total Issues Created: ${totalIssuesCreated}`);
    console.log(`Total Issues Updated: ${totalIssuesUpdated}`);
    console.log(`Total Notifications: ${totalNotifications}`);
    console.log(`Total Rate Limit Hits: ${totalRateLimits}`);
    
  } else {
    console.log(`=w Worker ${process.pid} started`);
    
    // Run load test in worker
    const results = await runLoadTest();
    
    // Send results back to master
    process.send({
      type: 'results',
      data: {
        totalRequests: results.totalRequests,
        successfulRequests: parseInt(results.successRate) * results.totalRequests / 100,
        issuesCreated: results.issuesCreated,
        issuesUpdated: results.issuesUpdated,
        notificationsSent: results.notificationsSent,
        rateLimitHits: results.rateLimitHits
      }
    });
    
    process.exit(0);
  }
}

// CLI interface
if (require.main === module) {
  const args = process.argv.slice(2);
  const distributed = args.includes('--distributed');
  
  if (distributed) {
    runDistributedLoadTest().catch(console.error);
  } else {
    runLoadTest().catch(console.error);
  }
}

module.exports = { runLoadTest, MetricsCollector, WorkflowFailureSimulator };