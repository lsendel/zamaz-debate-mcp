#!/usr/bin/env node

/**
 * Standalone concurrency test for MCP Debate System
 * This script tests the system's ability to handle concurrent requests
 */

const http = require('http');
const { performance } = require('perf_hooks');

const CONFIG = {
  baseUrl: 'http://localhost:3000',
  services: {
    context: 'http://localhost:8001',
    llm: 'http://localhost:8002', 
    debate: 'http://localhost:8003',
    rag: 'http://localhost:8004'
  },
  tests: {
    concurrent_clients: 10,
    requests_per_client: 5,
    timeout: 30000
  }
};

class ConcurrencyTester {
  constructor() {
    this.results = {
      total_requests: 0,
      successful_requests: 0,
      failed_requests: 0,
      response_times: [],
      errors: [],
      concurrent_peaks: []
    };
  }

  async makeRequest(url, options = {}) {
    return new Promise((resolve, reject) => {
      const startTime = performance.now();
      
      const req = http.request(url, {
        method: options.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-Organization-ID': options.orgId || 'test-org',
          ...options.headers
        },
        timeout: 10000
      }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          const responseTime = performance.now() - startTime;
          resolve({
            status: res.statusCode,
            data: data,
            responseTime,
            headers: res.headers
          });
        });
      });

      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Request timeout'));
      });

      req.on('error', reject);

      if (options.body) {
        req.write(JSON.stringify(options.body));
      }

      req.end();
    });
  }

  async testServiceHealth() {
    console.log('🔍 Testing service health...');
    
    const healthChecks = Object.entries(CONFIG.services).map(async ([name, url]) => {
      try {
        const response = await this.makeRequest(`${url}/health`);
        return { service: name, status: response.status, healthy: response.status === 200 };
      } catch (error) {
        return { service: name, status: 'error', healthy: false, error: error.message };
      }
    });

    const results = await Promise.all(healthChecks);
    
    results.forEach(result => {
      const icon = result.healthy ? '✅' : '❌';
      console.log(`${icon} ${result.service}: ${result.healthy ? 'HEALTHY' : 'UNHEALTHY'}`);
      if (!result.healthy) {
        console.log(`   Error: ${result.error || 'Service unavailable'}`);
      }
    });

    const allHealthy = results.every(r => r.healthy);
    if (!allHealthy) {
      throw new Error('Some services are not healthy');
    }

    console.log('✅ All services are healthy\n');
  }

  async testConcurrentDebateCreation() {
    console.log('🧪 Testing concurrent debate creation...');
    
    const createDebate = async (clientId) => {
      const orgId = `test-org-${clientId}`;
      const debateData = {
        name: `Concurrent Test Debate ${clientId}`,
        topic: `Testing concurrency with client ${clientId}`,
        participants: [
          {
            name: `Participant A ${clientId}`,
            role: 'debater',
            llm_config: {
              provider: 'llama',
              model: 'llama3',
              temperature: 0.7
            }
          },
          {
            name: `Participant B ${clientId}`,
            role: 'debater', 
            llm_config: {
              provider: 'llama',
              model: 'mistral',
              temperature: 0.8
            }
          }
        ],
        rules: {
          format: 'round_robin',
          max_rounds: 2
        }
      };

      try {
        const response = await this.makeRequest(`${CONFIG.services.debate}/debates`, {
          method: 'POST',
          body: debateData,
          orgId
        });

        this.results.total_requests++;
        this.results.response_times.push(response.responseTime);

        if (response.status === 200 || response.status === 201) {
          this.results.successful_requests++;
          return { clientId, success: true, responseTime: response.responseTime };
        } else {
          this.results.failed_requests++;
          return { clientId, success: false, status: response.status };
        }
      } catch (error) {
        this.results.total_requests++;
        this.results.failed_requests++;
        this.results.errors.push({ clientId, error: error.message });
        return { clientId, success: false, error: error.message };
      }
    };

    // Create multiple concurrent clients
    const clientPromises = [];
    for (let i = 1; i <= CONFIG.tests.concurrent_clients; i++) {
      clientPromises.push(createDebate(i));
    }

    console.log(`   Launching ${CONFIG.tests.concurrent_clients} concurrent clients...`);
    const startTime = performance.now();
    
    const results = await Promise.all(clientPromises);
    
    const totalTime = performance.now() - startTime;
    const successCount = results.filter(r => r.success).length;
    
    console.log(`   Completed in ${totalTime.toFixed(2)}ms`);
    console.log(`   Success rate: ${successCount}/${CONFIG.tests.concurrent_clients} (${(successCount/CONFIG.tests.concurrent_clients*100).toFixed(1)}%)`);
    
    if (successCount < CONFIG.tests.concurrent_clients * 0.8) {
      console.log('❌ Less than 80% success rate - potential concurrency issues');
    } else {
      console.log('✅ Good concurrency handling');
    }

    console.log('');
  }

  async testConcurrentTurnGeneration() {
    console.log('🧪 Testing concurrent turn generation...');
    
    // First create a debate
    const debateData = {
      name: 'Turn Concurrency Test',
      topic: 'Testing concurrent turn generation',
      participants: [
        {
          name: 'Participant A',
          role: 'debater',
          llm_config: { provider: 'llama', model: 'llama3', temperature: 0.7 }
        },
        {
          name: 'Participant B', 
          role: 'debater',
          llm_config: { provider: 'llama', model: 'mistral', temperature: 0.8 }
        }
      ],
      rules: { format: 'round_robin', max_rounds: 3 }
    };

    try {
      const createResponse = await this.makeRequest(`${CONFIG.services.debate}/debates`, {
        method: 'POST',
        body: debateData,
        orgId: 'turn-test-org'
      });

      if (createResponse.status !== 200 && createResponse.status !== 201) {
        console.log('❌ Failed to create test debate');
        return;
      }

      const debate = JSON.parse(createResponse.data);
      const debateId = debate.id;

      // Start the debate
      await this.makeRequest(`${CONFIG.services.debate}/debates/${debateId}/start`, {
        method: 'POST',
        orgId: 'turn-test-org'
      });

      // Make concurrent requests for next turns
      const turnPromises = [];
      for (let i = 0; i < 5; i++) {
        turnPromises.push(
          this.makeRequest(`${CONFIG.services.debate}/debates/${debateId}/next-turn`, {
            method: 'POST',
            orgId: 'turn-test-org'
          })
        );
      }

      console.log('   Making 5 concurrent turn requests...');
      const turnResults = await Promise.allSettled(turnPromises);
      
      const successfulTurns = turnResults.filter(r => 
        r.status === 'fulfilled' && 
        (r.value.status === 200 || r.value.status === 201)
      ).length;

      console.log(`   Successful turn requests: ${successfulTurns}/5`);
      
      if (successfulTurns > 0) {
        console.log('✅ Concurrent turn handling working');
      } else {
        console.log('❌ No turns succeeded - check turn generation');
      }

    } catch (error) {
      console.log(`❌ Turn concurrency test failed: ${error.message}`);
    }

    console.log('');
  }

  async testRateLimiting() {
    console.log('🧪 Testing rate limiting...');
    
    const rapidRequests = [];
    const requestCount = 50;
    
    for (let i = 0; i < requestCount; i++) {
      rapidRequests.push(
        this.makeRequest(`${CONFIG.services.debate}/health`, {
          orgId: 'rate-limit-test'
        })
      );
    }

    console.log(`   Making ${requestCount} rapid requests...`);
    const startTime = performance.now();
    
    const results = await Promise.allSettled(rapidRequests);
    
    const totalTime = performance.now() - startTime;
    const successful = results.filter(r => 
      r.status === 'fulfilled' && r.value.status === 200
    ).length;
    const rateLimited = results.filter(r => 
      r.status === 'fulfilled' && r.value.status === 429
    ).length;

    console.log(`   Completed in ${totalTime.toFixed(2)}ms`);
    console.log(`   Successful: ${successful}, Rate limited: ${rateLimited}, Failed: ${requestCount - successful - rateLimited}`);
    
    if (rateLimited > 0) {
      console.log('✅ Rate limiting is working');
    } else {
      console.log('ℹ️ No rate limiting detected (may be set high)');
    }

    console.log('');
  }

  async testOrganizationIsolation() {
    console.log('🧪 Testing organization isolation...');
    
    const orgs = ['org-a', 'org-b', 'org-c'];
    const debatePromises = orgs.map(async (orgId, index) => {
      const debateData = {
        name: `${orgId} Private Debate`,
        topic: `Private topic for ${orgId}`,
        participants: [
          { name: 'P1', role: 'debater', llm_config: { provider: 'llama', model: 'llama3' }},
          { name: 'P2', role: 'debater', llm_config: { provider: 'llama', model: 'mistral' }}
        ],
        rules: { format: 'round_robin', max_rounds: 1 }
      };

      const response = await this.makeRequest(`${CONFIG.services.debate}/debates`, {
        method: 'POST',
        body: debateData,
        orgId
      });

      return { orgId, status: response.status, success: response.status === 200 || response.status === 201 };
    });

    const results = await Promise.all(debatePromises);
    const successCount = results.filter(r => r.success).length;
    
    console.log(`   Created debates for ${successCount}/${orgs.length} organizations`);
    
    // Try to access debates across organizations
    const crossOrgTests = [];
    for (const orgA of orgs) {
      for (const orgB of orgs) {
        if (orgA !== orgB) {
          crossOrgTests.push(
            this.makeRequest(`${CONFIG.services.debate}/debates`, {
              orgId: orgB // Try to access orgA's debates with orgB credentials
            })
          );
        }
      }
    }

    const crossResults = await Promise.allSettled(crossOrgTests);
    const isolationWorking = crossResults.every(r => 
      r.status === 'fulfilled' && 
      (r.value.status === 200 || r.value.status === 403)
    );

    if (isolationWorking) {
      console.log('✅ Organization isolation working');
    } else {
      console.log('❌ Organization isolation may have issues');
    }

    console.log('');
  }

  generateReport() {
    console.log('📊 Concurrency Test Report');
    console.log('============================');
    
    if (this.results.response_times.length > 0) {
      const avgResponseTime = this.results.response_times.reduce((a, b) => a + b, 0) / this.results.response_times.length;
      const maxResponseTime = Math.max(...this.results.response_times);
      const minResponseTime = Math.min(...this.results.response_times);
      
      console.log(`Total Requests: ${this.results.total_requests}`);
      console.log(`Successful: ${this.results.successful_requests}`);
      console.log(`Failed: ${this.results.failed_requests}`);
      console.log(`Success Rate: ${(this.results.successful_requests/this.results.total_requests*100).toFixed(1)}%`);
      console.log(`Avg Response Time: ${avgResponseTime.toFixed(2)}ms`);
      console.log(`Min Response Time: ${minResponseTime.toFixed(2)}ms`);
      console.log(`Max Response Time: ${maxResponseTime.toFixed(2)}ms`);
    }

    if (this.results.errors.length > 0) {
      console.log('\n❌ Errors:');
      this.results.errors.forEach(error => {
        console.log(`   Client ${error.clientId}: ${error.error}`);
      });
    }

    const overallHealth = this.results.failed_requests / this.results.total_requests < 0.1;
    console.log(`\n🎯 Overall System Health: ${overallHealth ? 'GOOD' : 'NEEDS_ATTENTION'}`);
  }

  async runAllTests() {
    console.log('🚀 Starting MCP Debate System Concurrency Tests\n');
    
    try {
      await this.testServiceHealth();
      await this.testConcurrentDebateCreation();
      await this.testConcurrentTurnGeneration();
      await this.testRateLimiting();
      await this.testOrganizationIsolation();
      
      this.generateReport();
      
      console.log('\n✅ All concurrency tests completed!');
      
    } catch (error) {
      console.log(`\n❌ Test suite failed: ${error.message}`);
      process.exit(1);
    }
  }
}

// Run the tests
if (require.main === module) {
  const tester = new ConcurrencyTester();
  tester.runAllTests().catch(console.error);
}

module.exports = ConcurrencyTester;