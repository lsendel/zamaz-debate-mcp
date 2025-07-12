import { test, expect } from '@playwright/test';

/**
 * LLM Integration Tests
 * 
 * Goal: Test LLM service integration and functionality
 * Focus: Real LLM API calls, model switching, response quality
 * 
 * Test Coverage:
 * 1. LLM service health and availability
 * 2. Model listing and availability
 * 3. Chat completion functionality
 * 4. Model switching during debates
 * 5. Token usage tracking
 * 6. Error handling and rate limiting
 */

test.describe('LLM Integration Tests', () => {
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  const llmServiceURL = 'http://localhost:5002';

  test.beforeEach(async ({ page }) => {
    // Verify LLM service is healthy
    const health = await page.request.get(`${llmServiceURL}/health`);
    expect(health.ok(), 'LLM service should be healthy').toBeTruthy();
  });

  test.describe('LLM Service Health', () => {
    test('should verify all configured LLM providers', async ({ page }) => {
      const response = await page.request.get(`${llmServiceURL}/models`);
      expect(response.ok()).toBeTruthy();
      
      const models = await response.json();
      console.log(`[LLM Service] Available models: ${models.length}`);
      
      // Group models by provider
      const modelsByProvider = models.reduce((acc, model) => {
        if (!acc[model.provider]) acc[model.provider] = [];
        acc[model.provider].push(model);
        return acc;
      }, {});
      
      // Log provider summary
      for (const [provider, providerModels] of Object.entries(modelsByProvider)) {
        console.log(`[${provider}] ${providerModels.length} models available`);
        providerModels.forEach(model => {
          console.log(`  - ${model.name} (${model.id})`);
        });
      }
      
      // Verify we have at least Claude and Gemini (as per user requirements)
      expect(modelsByProvider).toHaveProperty('claude');
      expect(modelsByProvider).toHaveProperty('gemini');
      
      // Verify specific models exist
      const claudeSonnet = models.find(m => m.id === 'claude-3-5-sonnet-20241022');
      expect(claudeSonnet).toBeDefined();
      expect(claudeSonnet.name).toBe('Claude 3.5 Sonnet');
      
      const geminiPro = models.find(m => m.id === 'gemini-2.5-pro');
      expect(geminiPro).toBeDefined();
      expect(geminiPro.name).toBe('Gemini 2.5 Pro');
    });

    test('should check individual provider endpoints', async ({ page }) => {
      const providers = ['claude', 'openai', 'gemini'];
      
      for (const provider of providers) {
        const response = await page.request.get(`${llmServiceURL}/models/${provider}`);
        
        if (response.ok()) {
          const models = await response.json();
          console.log(`[${provider}] Endpoint returned ${models.length} models`);
          expect(models.length).toBeGreaterThan(0);
        } else {
          console.log(`[${provider}] Not configured or unavailable`);
        }
      }
    });
  });

  test.describe('Chat Completion Tests', () => {
    test('should get completion from Claude 3.5 Sonnet', async ({ page }) => {
      const startTime = Date.now();
      
      const response = await page.request.post(`${llmServiceURL}/chat/completions`, {
        data: {
          model: 'claude-3-5-sonnet-20241022',
          messages: [
            {
              role: 'system',
              content: 'You are a helpful AI assistant participating in a debate test.'
            },
            {
              role: 'user',
              content: 'In one sentence, explain why automated testing is important for software quality.'
            }
          ],
          temperature: 0.7,
          max_tokens: 100
        }
      });
      
      const responseTime = Date.now() - startTime;
      expect(response.ok()).toBeTruthy();
      
      const completion = await response.json();
      console.log(`[Claude Response Time] ${responseTime}ms`);
      console.log(`[Claude Response] ${completion.choices[0].message.content}`);
      
      // Verify response structure
      expect(completion).toHaveProperty('id');
      expect(completion).toHaveProperty('model');
      expect(completion).toHaveProperty('choices');
      expect(completion.choices).toHaveLength(1);
      expect(completion.choices[0].message.role).toBe('assistant');
      expect(completion.choices[0].message.content).toBeTruthy();
      
      // Verify token usage is tracked
      expect(completion).toHaveProperty('usage');
      expect(completion.usage).toHaveProperty('prompt_tokens');
      expect(completion.usage).toHaveProperty('completion_tokens');
      expect(completion.usage).toHaveProperty('total_tokens');
      
      // Content should be relevant
      const content = completion.choices[0].message.content.toLowerCase();
      expect(content).toMatch(/test|quality|software|automated|automation/);
    });

    test('should get completion from Gemini 2.5 Pro', async ({ page }) => {
      const startTime = Date.now();
      
      const response = await page.request.post(`${llmServiceURL}/chat/completions`, {
        data: {
          model: 'gemini-2.5-pro',
          messages: [
            {
              role: 'user',
              content: 'What are the key benefits of AI in education? List 3 points briefly.'
            }
          ],
          temperature: 0.8,
          max_tokens: 150
        }
      });
      
      const responseTime = Date.now() - startTime;
      expect(response.ok()).toBeTruthy();
      
      const completion = await response.json();
      console.log(`[Gemini Response Time] ${responseTime}ms`);
      console.log(`[Gemini Response] ${completion.choices[0].message.content}`);
      
      // Verify response
      expect(completion.choices[0].message.content).toBeTruthy();
      expect(completion.choices[0].message.content.length).toBeGreaterThan(50);
      
      // Should mention education-related keywords
      const content = completion.choices[0].message.content.toLowerCase();
      expect(content).toMatch(/education|learning|student|teach/);
    });

    test('should handle streaming responses', async ({ page }) => {
      const response = await page.request.post(`${llmServiceURL}/chat/completions`, {
        data: {
          model: 'claude-3-5-sonnet-20241022',
          messages: [
            {
              role: 'user',
              content: 'Count from 1 to 5 slowly.'
            }
          ],
          stream: true,
          max_tokens: 50
        }
      });
      
      expect(response.ok()).toBeTruthy();
      
      const body = await response.body();
      const text = body.toString();
      
      // Streaming responses should contain multiple data chunks
      const chunks = text.split('\n').filter(line => line.startsWith('data: '));
      expect(chunks.length).toBeGreaterThan(1);
      
      console.log(`[Streaming] Received ${chunks.length} chunks`);
    });
  });

  test.describe('Model Switching in Debates', () => {
    test('should create debate with different LLM models', async ({ page }) => {
      await page.goto(baseURL);
      
      // Create a multi-model debate
      await page.click('button:has-text("Create Debate")');
      await page.fill('input[name="name"]', 'Multi-Model Test Debate');
      await page.fill('input[name="topic"]', 'Testing different LLM models in one debate');
      
      // Configure first participant with Claude
      await page.selectOption('[data-testid="participant-0-provider"]', 'claude');
      await page.waitForTimeout(500);
      await page.selectOption('[data-testid="participant-0-model"]', 'claude-3-5-sonnet-20241022');
      await page.fill('[data-testid="participant-0-name"]', 'Claude Debater');
      
      // Configure second participant with Gemini
      await page.selectOption('[data-testid="participant-1-provider"]', 'gemini');
      await page.waitForTimeout(500);
      await page.selectOption('[data-testid="participant-1-model"]', 'gemini-2.5-pro');
      await page.fill('[data-testid="participant-1-name"]', 'Gemini Debater');
      
      // Add third participant with OpenAI (if available)
      await page.click('button:has-text("Add Participant")');
      await page.selectOption('[data-testid="participant-2-provider"]', 'openai');
      await page.waitForTimeout(500);
      
      const openaiModels = await page.locator('[data-testid="participant-2-model"] option').count();
      if (openaiModels > 1) { // More than just the placeholder
        await page.selectOption('[data-testid="participant-2-model"]', 'gpt-4o');
        await page.fill('[data-testid="participant-2-name"]', 'GPT-4 Debater');
      } else {
        // Remove if OpenAI not available
        await page.click('[data-testid="remove-participant-2"]');
      }
      
      // Create the debate
      await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
      
      // Wait for navigation
      await page.waitForURL(/\/debate\//);
      
      // Start the debate
      await page.click('button:has-text("Start Debate")');
      
      // Wait for first turn from each model
      await page.waitForSelector('[data-testid="debate-turn-1"]', { timeout: 60000 });
      await page.waitForSelector('[data-testid="debate-turn-2"]', { timeout: 60000 });
      
      // Verify different models responded
      const turn1Speaker = await page.textContent('[data-testid="debate-turn-1"] [data-testid="turn-speaker"]');
      const turn2Speaker = await page.textContent('[data-testid="debate-turn-2"] [data-testid="turn-speaker"]');
      
      expect(turn1Speaker).not.toBe(turn2Speaker);
      console.log(`[Multi-Model Debate] Turn 1: ${turn1Speaker}, Turn 2: ${turn2Speaker}`);
    });
  });

  test.describe('Error Handling', () => {
    test('should handle invalid model gracefully', async ({ page }) => {
      const response = await page.request.post(`${llmServiceURL}/chat/completions`, {
        data: {
          model: 'invalid-model-xyz',
          messages: [{ role: 'user', content: 'Hello' }]
        }
      });
      
      expect(response.status()).toBe(400);
      const error = await response.json();
      expect(error).toHaveProperty('error');
      expect(error.error).toContain('Invalid model');
    });

    test('should handle rate limiting appropriately', async ({ page }) => {
      // Send multiple rapid requests
      const promises = [];
      for (let i = 0; i < 5; i++) {
        promises.push(
          page.request.post(`${llmServiceURL}/chat/completions`, {
            data: {
              model: 'claude-3-5-sonnet-20241022',
              messages: [{ role: 'user', content: `Test message ${i}` }],
              max_tokens: 10
            }
          })
        );
      }
      
      const responses = await Promise.all(promises);
      const statusCodes = responses.map(r => r.status());
      
      // Should handle all requests (with possible rate limiting)
      statusCodes.forEach((status, index) => {
        expect([200, 429]).toContain(status);
        if (status === 429) {
          console.log(`[Rate Limit] Request ${index} was rate limited`);
        }
      });
    });

    test('should validate message format', async ({ page }) => {
      const invalidRequests = [
        {
          name: 'Missing messages',
          data: { model: 'claude-3-5-sonnet-20241022' }
        },
        {
          name: 'Invalid message role',
          data: {
            model: 'claude-3-5-sonnet-20241022',
            messages: [{ role: 'invalid-role', content: 'Test' }]
          }
        },
        {
          name: 'Empty messages array',
          data: {
            model: 'claude-3-5-sonnet-20241022',
            messages: []
          }
        }
      ];
      
      for (const testCase of invalidRequests) {
        const response = await page.request.post(`${llmServiceURL}/chat/completions`, {
          data: testCase.data
        });
        
        expect(response.status()).toBe(400);
        console.log(`[Validation] ${testCase.name}: ${response.status()}`);
      }
    });
  });

  test.describe('Performance Metrics', () => {
    test('should track response times for different models', async ({ page }) => {
      const models = [
        { id: 'claude-3-5-sonnet-20241022', name: 'Claude 3.5 Sonnet' },
        { id: 'gemini-2.5-pro', name: 'Gemini 2.5 Pro' },
        { id: 'gpt-4o-mini', name: 'GPT-4o Mini' }
      ];
      
      const performanceMetrics = [];
      
      for (const model of models) {
        const startTime = Date.now();
        
        const response = await page.request.post(`${llmServiceURL}/chat/completions`, {
          data: {
            model: model.id,
            messages: [
              {
                role: 'user',
                content: 'What is 2+2? Answer in one word.'
              }
            ],
            max_tokens: 10
          }
        });
        
        const responseTime = Date.now() - startTime;
        
        if (response.ok()) {
          const completion = await response.json();
          performanceMetrics.push({
            model: model.name,
            responseTime,
            tokensUsed: completion.usage?.total_tokens || 0
          });
        }
      }
      
      // Log performance comparison
      console.log('\n[Performance Metrics]');
      performanceMetrics.forEach(metric => {
        console.log(`${metric.model}: ${metric.responseTime}ms (${metric.tokensUsed} tokens)`);
      });
      
      // Save metrics to evidence
      const fs = require('fs');
      fs.writeFileSync(
        `test_probe/evidence/performance-metrics/llm-response-times-${Date.now()}.json`,
        JSON.stringify(performanceMetrics, null, 2)
      );
    });
  });
});