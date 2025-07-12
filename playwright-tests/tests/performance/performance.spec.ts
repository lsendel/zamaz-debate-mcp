import { test, expect } from '@playwright/test';

/**
 * Performance Tests
 * 
 * Goal: Measure and verify system performance under various loads
 * Focus: Response times, concurrent operations, resource usage
 * 
 * Test Coverage:
 * 1. Page load performance
 * 2. API response times
 * 3. Concurrent debate handling
 * 4. LLM response latency
 * 5. WebSocket message throughput
 * 6. Database query performance
 */

test.describe('Performance Tests', () => {
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  const performanceThresholds = {
    pageLoad: 3000, // 3 seconds
    apiResponse: 1000, // 1 second
    llmResponse: 30000, // 30 seconds
    debateCreation: 2000, // 2 seconds
    turnGeneration: 60000 // 60 seconds
  };

  test.beforeEach(async ({ page }) => {
    // Enable performance metrics collection
    await page.coverage.startJSCoverage();
  });

  test.afterEach(async ({ page }) => {
    // Stop coverage and collect metrics
    const coverage = await page.coverage.stopJSCoverage();
    console.log(`[Performance] JS Coverage: ${coverage.length} files`);
  });

  test.describe('Page Load Performance', () => {
    test('should load homepage within threshold', async ({ page }) => {
      const startTime = Date.now();
      
      await page.goto(baseURL);
      await page.waitForLoadState('networkidle');
      
      const loadTime = Date.now() - startTime;
      console.log(`[Page Load] Homepage: ${loadTime}ms`);
      
      expect(loadTime).toBeLessThan(performanceThresholds.pageLoad);
      
      // Measure key metrics
      const metrics = await page.evaluate(() => {
        const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        return {
          domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
          loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
          firstPaint: performance.getEntriesByName('first-paint')[0]?.startTime || 0,
          firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0
        };
      });
      
      console.log('[Performance Metrics]', metrics);
      
      // Save metrics
      const fs = require('fs');
      fs.writeFileSync(
        `test_probe/evidence/performance-metrics/page-load-${Date.now()}.json`,
        JSON.stringify({ loadTime, metrics, url: baseURL }, null, 2)
      );
    });

    test('should efficiently render debate list', async ({ page }) => {
      await page.goto(baseURL);
      
      const startTime = Date.now();
      await page.waitForSelector('[data-testid="debate-card"]', { timeout: 10000 });
      const renderTime = Date.now() - startTime;
      
      const debateCount = await page.locator('[data-testid="debate-card"]').count();
      console.log(`[Render Performance] ${debateCount} debates rendered in ${renderTime}ms`);
      
      // Should render efficiently even with many debates
      const timePerDebate = debateCount > 0 ? renderTime / debateCount : 0;
      expect(timePerDebate).toBeLessThan(100); // Less than 100ms per debate card
    });

    test('should handle smooth scrolling with many debates', async ({ page }) => {
      await page.goto(baseURL);
      await page.waitForSelector('[data-testid="debate-card"]');
      
      // Measure scroll performance
      const scrollMetrics = await page.evaluate(async () => {
        const metrics = [];
        const scrollHeight = document.documentElement.scrollHeight;
        const viewportHeight = window.innerHeight;
        const scrollSteps = 10;
        const stepSize = (scrollHeight - viewportHeight) / scrollSteps;
        
        for (let i = 0; i < scrollSteps; i++) {
          const startTime = performance.now();
          window.scrollTo(0, i * stepSize);
          await new Promise(resolve => requestAnimationFrame(resolve));
          const frameTime = performance.now() - startTime;
          metrics.push(frameTime);
        }
        
        return {
          avgFrameTime: metrics.reduce((a, b) => a + b, 0) / metrics.length,
          maxFrameTime: Math.max(...metrics),
          totalScrollHeight: scrollHeight
        };
      });
      
      console.log('[Scroll Performance]', scrollMetrics);
      
      // Should maintain 60fps (16.67ms per frame)
      expect(scrollMetrics.avgFrameTime).toBeLessThan(20);
      expect(scrollMetrics.maxFrameTime).toBeLessThan(50);
    });
  });

  test.describe('API Performance', () => {
    test('should handle concurrent API requests efficiently', async ({ page }) => {
      const endpoints = [
        '/api/debate/debates',
        '/api/llm/models',
        '/api/debate/stats'
      ];
      
      const requests = endpoints.map(endpoint => {
        const startTime = Date.now();
        return page.request.get(`${baseURL}${endpoint}`).then(response => ({
          endpoint,
          status: response.status(),
          duration: Date.now() - startTime
        }));
      });
      
      const results = await Promise.all(requests);
      
      results.forEach(result => {
        console.log(`[API Performance] ${result.endpoint}: ${result.duration}ms`);
        expect(result.duration).toBeLessThan(performanceThresholds.apiResponse);
      });
    });

    test('should efficiently create multiple debates', async ({ page }) => {
      const debateCount = 5;
      const createTimes = [];
      
      for (let i = 0; i < debateCount; i++) {
        const startTime = Date.now();
        
        const response = await page.request.post(`${baseURL}/api/debate/debates`, {
          data: {
            name: `Performance Test ${i + 1}`,
            topic: 'Testing debate creation performance',
            participants: [
              {
                name: 'Bot 1',
                provider: 'claude',
                model: 'claude-3-5-sonnet-20241022',
                role: 'debater'
              },
              {
                name: 'Bot 2',
                provider: 'gemini',
                model: 'gemini-2.5-pro',
                role: 'debater'
              }
            ]
          }
        });
        
        const createTime = Date.now() - startTime;
        createTimes.push(createTime);
        
        expect(response.ok()).toBeTruthy();
        expect(createTime).toBeLessThan(performanceThresholds.debateCreation);
      }
      
      const avgCreateTime = createTimes.reduce((a, b) => a + b, 0) / createTimes.length;
      console.log(`[Debate Creation] Average: ${avgCreateTime}ms, Max: ${Math.max(...createTimes)}ms`);
    });
  });

  test.describe('LLM Performance', () => {
    test('should measure LLM response times under load', async ({ page }) => {
      const models = [
        { provider: 'claude', model: 'claude-3-5-sonnet-20241022' },
        { provider: 'gemini', model: 'gemini-2.5-pro' }
      ];
      
      const concurrentRequests = 3;
      const results = [];
      
      for (const modelConfig of models) {
        const requests = [];
        
        for (let i = 0; i < concurrentRequests; i++) {
          const startTime = Date.now();
          requests.push(
            page.request.post('http://localhost:5002/chat/completions', {
              data: {
                model: modelConfig.model,
                messages: [
                  {
                    role: 'user',
                    content: `Performance test ${i + 1}: Explain in one sentence why performance testing is important.`
                  }
                ],
                max_tokens: 100,
                temperature: 0.7
              }
            }).then(response => ({
              ...modelConfig,
              requestIndex: i,
              duration: Date.now() - startTime,
              status: response.status()
            }))
          );
        }
        
        const modelResults = await Promise.all(requests);
        results.push(...modelResults);
      }
      
      // Analyze results
      const modelStats = {};
      results.forEach(result => {
        if (!modelStats[result.model]) {
          modelStats[result.model] = [];
        }
        modelStats[result.model].push(result.duration);
      });
      
      console.log('\n[LLM Performance Under Load]');
      Object.entries(modelStats).forEach(([model, times]) => {
        const avg = times.reduce((a, b) => a + b, 0) / times.length;
        console.log(`${model}:`);
        console.log(`  Average: ${avg}ms`);
        console.log(`  Min: ${Math.min(...times)}ms`);
        console.log(`  Max: ${Math.max(...times)}ms`);
      });
      
      // All requests should complete within threshold
      results.forEach(result => {
        expect(result.duration).toBeLessThan(performanceThresholds.llmResponse);
      });
    });
  });

  test.describe('Concurrent Operations', () => {
    test('should handle multiple simultaneous debates', async ({ page }) => {
      const simultaneousDebates = 3;
      
      // Create debates
      const debatePromises = [];
      for (let i = 0; i < simultaneousDebates; i++) {
        debatePromises.push(
          page.request.post(`${baseURL}/api/debate/debates`, {
            data: {
              name: `Concurrent Debate ${i + 1}`,
              topic: 'Testing concurrent debate handling',
              participants: [
                {
                  name: `Debater A${i + 1}`,
                  provider: 'claude',
                  model: 'claude-3-5-sonnet-20241022',
                  role: 'debater'
                },
                {
                  name: `Debater B${i + 1}`,
                  provider: 'gemini',
                  model: 'gemini-2.5-pro',
                  role: 'debater'
                }
              ]
            }
          })
        );
      }
      
      const debates = await Promise.all(debatePromises);
      const debateIds = await Promise.all(debates.map(r => r.json().then(d => d.id)));
      
      // Start all debates simultaneously
      const startTime = Date.now();
      const startPromises = debateIds.map(id => 
        page.request.post(`http://localhost:5013/debates/${id}/start`)
      );
      
      await Promise.all(startPromises);
      const startDuration = Date.now() - startTime;
      
      console.log(`[Concurrent Start] ${simultaneousDebates} debates started in ${startDuration}ms`);
      
      // Wait for first turn in each debate
      const turnPromises = debateIds.map(async (id, index) => {
        const turnStart = Date.now();
        
        // Poll for first turn
        let turn = null;
        for (let attempt = 0; attempt < 60; attempt++) {
          const response = await page.request.get(`http://localhost:5013/debates/${id}`);
          const debate = await response.json();
          
          if (debate.turns && debate.turns.length > 0) {
            turn = debate.turns[0];
            break;
          }
          
          await page.waitForTimeout(1000);
        }
        
        const turnDuration = Date.now() - turnStart;
        return {
          debateIndex: index,
          turnDuration,
          hastur: !!turn
        };
      });
      
      const turnResults = await Promise.all(turnPromises);
      
      console.log('\n[Concurrent Turn Generation]');
      turnResults.forEach(result => {
        console.log(`Debate ${result.debateIndex + 1}: ${result.turnDuration}ms`);
        expect(result.hastur).toBeTruthy();
      });
    });
  });

  test.describe('Memory and Resource Usage', () => {
    test('should not leak memory during extended operation', async ({ page }) => {
      // Navigate to main page
      await page.goto(baseURL);
      
      // Collect initial memory snapshot
      const initialMetrics = await page.evaluate(() => {
        if ('memory' in performance) {
          return {
            usedJSHeapSize: (performance as any).memory.usedJSHeapSize,
            totalJSHeapSize: (performance as any).memory.totalJSHeapSize
          };
        }
        return null;
      });
      
      if (!initialMetrics) {
        console.log('[Memory Test] Skipped - performance.memory not available');
        return;
      }
      
      // Perform repeated operations
      for (let i = 0; i < 10; i++) {
        // Create debate dialog
        await page.click('button:has-text("Create Debate")');
        await page.waitForSelector('[role="dialog"]');
        
        // Fill some fields
        await page.fill('input[name="name"]', `Memory Test ${i}`);
        await page.fill('input[name="topic"]', 'Testing memory usage');
        
        // Cancel
        await page.click('button:has-text("Cancel")');
        await page.waitForSelector('[role="dialog"]', { state: 'hidden' });
        
        // Small delay
        await page.waitForTimeout(100);
      }
      
      // Force garbage collection if available
      await page.evaluate(() => {
        if ('gc' in window) {
          (window as any).gc();
        }
      });
      
      // Collect final memory snapshot
      const finalMetrics = await page.evaluate(() => {
        if ('memory' in performance) {
          return {
            usedJSHeapSize: (performance as any).memory.usedJSHeapSize,
            totalJSHeapSize: (performance as any).memory.totalJSHeapSize
          };
        }
        return null;
      });
      
      if (finalMetrics) {
        const heapGrowth = finalMetrics.usedJSHeapSize - initialMetrics.usedJSHeapSize;
        const heapGrowthMB = heapGrowth / (1024 * 1024);
        
        console.log('[Memory Usage]');
        console.log(`  Initial: ${(initialMetrics.usedJSHeapSize / (1024 * 1024)).toFixed(2)}MB`);
        console.log(`  Final: ${(finalMetrics.usedJSHeapSize / (1024 * 1024)).toFixed(2)}MB`);
        console.log(`  Growth: ${heapGrowthMB.toFixed(2)}MB`);
        
        // Memory growth should be reasonable (less than 50MB)
        expect(heapGrowthMB).toBeLessThan(50);
      }
    });
  });

  test.describe('Performance Summary', () => {
    test('should generate performance report', async ({ page }) => {
      const report = {
        timestamp: new Date().toISOString(),
        thresholds: performanceThresholds,
        results: {
          pageLoad: 'See individual test results',
          apiPerformance: 'See individual test results',
          llmPerformance: 'See individual test results',
          concurrentOperations: 'See individual test results'
        },
        recommendations: [
          'Monitor LLM response times during peak usage',
          'Consider caching frequently accessed debate data',
          'Implement pagination for large debate lists',
          'Add request queuing for concurrent LLM calls'
        ]
      };
      
      // Save performance report
      const fs = require('fs');
      fs.writeFileSync(
        `test_probe/evidence/performance-metrics/performance-report-${Date.now()}.json`,
        JSON.stringify(report, null, 2)
      );
      
      console.log('\n=== PERFORMANCE TEST SUMMARY ===');
      console.log('Performance testing completed successfully');
      console.log('Check test_probe/evidence/performance-metrics/ for detailed results');
      console.log('================================\n');
    });
  });
});