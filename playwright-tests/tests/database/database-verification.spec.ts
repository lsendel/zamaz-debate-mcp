import { test, expect } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';

/**
 * Database Verification Tests
 * 
 * Goal: Verify data persistence and integrity in the database
 * Focus: Debate history, turn storage, participant data
 * 
 * Test Coverage:
 * 1. Debate creation and persistence
 * 2. Turn history tracking
 * 3. Participant information storage
 * 4. Status transitions logging
 * 5. Data retrieval and consistency
 * 6. Database performance with large datasets
 */

test.describe('Database Verification Tests', () => {
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  const debateServiceURL = 'http://localhost:5013';
  
  // Test data containers
  const createdDebateIds: string[] = [];
  const testRunId = uuidv4();

  test.afterAll(async () => {
    // Cleanup test debates if needed
    console.log(`[Database Tests] Created ${createdDebateIds.length} test debates`);
  });

  test.describe('Debate Persistence', () => {
    test('should persist debate with all metadata', async ({ page }) => {
      const debateName = `DB Test ${testRunId} - ${Date.now()}`;
      const debateTopic = 'Testing database persistence of debate metadata';
      const debateDescription = 'This debate tests if all metadata fields are properly stored and retrieved from the database.';
      
      // Create debate via API
      const createResponse = await page.request.post(`${debateServiceURL}/debates`, {
        data: {
          name: debateName,
          topic: debateTopic,
          description: debateDescription,
          rules: {
            maxRounds: 3,
            maxTurnsPerRound: 2,
            turnTimeLimit: 120,
            votingEnabled: true,
            audienceParticipation: false
          },
          participants: [
            {
              name: 'Data Advocate',
              position: 'Pro structured data',
              provider: 'claude',
              model: 'claude-3-5-sonnet-20241022',
              temperature: 0.7,
              maxTokens: 1000,
              role: 'debater'
            },
            {
              name: 'Flexibility Champion',
              position: 'Pro flexible schemas',
              provider: 'gemini',
              model: 'gemini-2.5-pro',
              temperature: 0.8,
              maxTokens: 1000,
              role: 'debater'
            }
          ],
          metadata: {
            testRunId,
            createdBy: 'database-test',
            tags: ['test', 'database', 'verification']
          }
        }
      });
      
      expect(createResponse.ok()).toBeTruthy();
      const createdDebate = await createResponse.json();
      expect(createdDebate).toHaveProperty('id');
      createdDebateIds.push(createdDebate.id);
      
      // Retrieve the debate
      const getResponse = await page.request.get(`${debateServiceURL}/debates/${createdDebate.id}`);
      expect(getResponse.ok()).toBeTruthy();
      const retrievedDebate = await getResponse.json();
      
      // Verify all fields were persisted
      expect(retrievedDebate.name).toBe(debateName);
      expect(retrievedDebate.topic).toBe(debateTopic);
      expect(retrievedDebate.description).toBe(debateDescription);
      expect(retrievedDebate.status).toBe('draft');
      
      // Verify rules
      expect(retrievedDebate.rules).toMatchObject({
        maxRounds: 3,
        maxTurnsPerRound: 2,
        turnTimeLimit: 120,
        votingEnabled: true,
        audienceParticipation: false
      });
      
      // Verify participants
      expect(retrievedDebate.participants).toHaveLength(2);
      expect(retrievedDebate.participants[0]).toMatchObject({
        name: 'Data Advocate',
        position: 'Pro structured data',
        provider: 'claude',
        model: 'claude-3-5-sonnet-20241022'
      });
      
      // Verify metadata
      expect(retrievedDebate.metadata).toMatchObject({
        testRunId,
        createdBy: 'database-test'
      });
      
      // Verify timestamps
      expect(retrievedDebate.createdAt).toBeTruthy();
      expect(retrievedDebate.updatedAt).toBeTruthy();
      
      console.log(`[Database] Debate ${createdDebate.id} persisted successfully`);
    });

    test('should persist debate status transitions', async ({ page }) => {
      // Create a debate
      const createResponse = await page.request.post(`${debateServiceURL}/debates`, {
        data: {
          name: `Status Transition Test ${Date.now()}`,
          topic: 'Testing status persistence',
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
      
      const debate = await createResponse.json();
      createdDebateIds.push(debate.id);
      
      // Track status transitions
      const statusTransitions = [];
      
      // Start debate (draft -> active)
      await page.request.post(`${debateServiceURL}/debates/${debate.id}/start`);
      let status = await getDebateStatus(page, debate.id);
      statusTransitions.push({ from: 'draft', to: status, timestamp: new Date() });
      expect(status).toBe('active');
      
      // Pause debate (active -> paused)
      await page.request.post(`${debateServiceURL}/debates/${debate.id}/pause`);
      status = await getDebateStatus(page, debate.id);
      statusTransitions.push({ from: 'active', to: status, timestamp: new Date() });
      expect(status).toBe('paused');
      
      // Resume debate (paused -> active)
      await page.request.post(`${debateServiceURL}/debates/${debate.id}/resume`);
      status = await getDebateStatus(page, debate.id);
      statusTransitions.push({ from: 'paused', to: status, timestamp: new Date() });
      expect(status).toBe('active');
      
      // Complete debate (active -> completed)
      await page.request.post(`${debateServiceURL}/debates/${debate.id}/complete`);
      status = await getDebateStatus(page, debate.id);
      statusTransitions.push({ from: 'active', to: status, timestamp: new Date() });
      expect(status).toBe('completed');
      
      console.log('[Database] Status transitions:', statusTransitions);
      
      // Verify status history if available
      const debateData = await page.request.get(`${debateServiceURL}/debates/${debate.id}`);
      const fullDebate = await debateData.json();
      if (fullDebate.statusHistory) {
        expect(fullDebate.statusHistory.length).toBeGreaterThanOrEqual(4);
      }
    });
  });

  test.describe('Turn History Tracking', () => {
    test('should persist all debate turns with metadata', async ({ page }) => {
      await page.goto(baseURL);
      
      // Create and start a debate
      const debateName = `Turn Tracking Test ${Date.now()}`;
      await page.click('button:has-text("Create Debate")');
      await page.fill('input[name="name"]', debateName);
      await page.fill('input[name="topic"]', 'Testing turn persistence in database');
      await page.fill('input[name="maxRounds"]', '2');
      await page.fill('input[name="maxTurnsPerRound"]', '2');
      
      // Configure participants
      await page.selectOption('[data-testid="participant-0-provider"]', 'claude');
      await page.selectOption('[data-testid="participant-0-model"]', 'claude-3-5-sonnet-20241022');
      await page.fill('[data-testid="participant-0-name"]', 'Turn Tracker 1');
      
      await page.selectOption('[data-testid="participant-1-provider"]', 'gemini');
      await page.selectOption('[data-testid="participant-1-model"]', 'gemini-2.5-pro');
      await page.fill('[data-testid="participant-1-name"]', 'Turn Tracker 2');
      
      await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
      await page.waitForURL(/\/debate\//);
      
      // Extract debate ID
      const debateId = page.url().split('/').pop();
      createdDebateIds.push(debateId);
      
      // Start debate and wait for turns
      await page.click('button:has-text("Start Debate")');
      
      // Wait for 4 turns (2 rounds × 2 turns)
      for (let i = 1; i <= 4; i++) {
        await page.waitForSelector(`[data-testid="debate-turn-${i}"]`, { timeout: 120000 });
        console.log(`[Turn Tracking] Turn ${i} completed`);
      }
      
      // Retrieve debate data via API
      const response = await page.request.get(`${debateServiceURL}/debates/${debateId}`);
      const debateData = await response.json();
      
      // Verify turns are persisted
      expect(debateData.turns).toBeDefined();
      expect(debateData.turns.length).toBe(4);
      
      // Verify each turn has required fields
      debateData.turns.forEach((turn, index) => {
        expect(turn).toMatchObject({
          turnNumber: index + 1,
          participantId: expect.any(String),
          participantName: expect.stringMatching(/Turn Tracker [12]/),
          content: expect.any(String),
          timestamp: expect.any(String),
          round: Math.ceil((index + 1) / 2),
          metadata: expect.objectContaining({
            provider: expect.stringMatching(/claude|gemini/),
            model: expect.any(String),
            tokensUsed: expect.any(Number)
          })
        });
        
        // Content should be substantial
        expect(turn.content.length).toBeGreaterThan(50);
      });
      
      // Save turn data for evidence
      const fs = require('fs');
      fs.writeFileSync(
        `test_probe/evidence/database-snapshots/turns-${debateId}.json`,
        JSON.stringify(debateData.turns, null, 2)
      );
      
      console.log(`[Database] Verified ${debateData.turns.length} turns persisted for debate ${debateId}`);
    });

    test('should maintain turn order and threading', async ({ page }) => {
      // Create debate via API for controlled testing
      const createResponse = await page.request.post(`${debateServiceURL}/debates`, {
        data: {
          name: `Turn Threading Test ${Date.now()}`,
          topic: 'Testing turn order and references',
          participants: [
            {
              name: 'Thread Starter',
              provider: 'claude',
              model: 'claude-3-5-sonnet-20241022',
              role: 'debater'
            },
            {
              name: 'Thread Responder',
              provider: 'gemini',
              model: 'gemini-2.5-pro',
              role: 'debater'
            }
          ]
        }
      });
      
      const debate = await createResponse.json();
      createdDebateIds.push(debate.id);
      
      // Start debate
      await page.request.post(`${debateServiceURL}/debates/${debate.id}/start`);
      
      // Add turns with references
      const turns = [];
      for (let i = 0; i < 4; i++) {
        const participantIndex = i % 2;
        const participantName = participantIndex === 0 ? 'Thread Starter' : 'Thread Responder';
        
        const turnResponse = await page.request.post(`${debateServiceURL}/debates/${debate.id}/turns`, {
          data: {
            participantName,
            content: `Turn ${i + 1}: ${participantName} responding to previous points...`,
            referencedTurns: i > 0 ? [turns[i - 1].id] : [],
            metadata: {
              testTurn: true,
              sequence: i + 1
            }
          }
        });
        
        const turn = await turnResponse.json();
        turns.push(turn);
      }
      
      // Verify turn order
      const debateResponse = await page.request.get(`${debateServiceURL}/debates/${debate.id}`);
      const fullDebate = await debateResponse.json();
      
      expect(fullDebate.turns).toHaveLength(4);
      fullDebate.turns.forEach((turn, index) => {
        expect(turn.turnNumber).toBe(index + 1);
        if (index > 0) {
          expect(turn.referencedTurns).toContain(turns[index - 1].id);
        }
      });
      
      console.log('[Database] Turn threading verified');
    });
  });

  test.describe('Data Integrity', () => {
    test('should handle concurrent updates correctly', async ({ page }) => {
      // Create a debate
      const createResponse = await page.request.post(`${debateServiceURL}/debates`, {
        data: {
          name: `Concurrency Test ${Date.now()}`,
          topic: 'Testing concurrent database updates',
          participants: [
            { name: 'Bot 1', provider: 'claude', model: 'claude-3-5-sonnet-20241022', role: 'debater' },
            { name: 'Bot 2', provider: 'gemini', model: 'gemini-2.5-pro', role: 'debater' }
          ]
        }
      });
      
      const debate = await createResponse.json();
      createdDebateIds.push(debate.id);
      
      // Start debate
      await page.request.post(`${debateServiceURL}/debates/${debate.id}/start`);
      
      // Simulate concurrent turn additions
      const concurrentTurns = [];
      for (let i = 0; i < 5; i++) {
        concurrentTurns.push(
          page.request.post(`${debateServiceURL}/debates/${debate.id}/turns`, {
            data: {
              participantName: i % 2 === 0 ? 'Bot 1' : 'Bot 2',
              content: `Concurrent turn ${i + 1}`,
              metadata: { concurrentTest: true, index: i }
            }
          })
        );
      }
      
      // Wait for all turns to complete
      const results = await Promise.allSettled(concurrentTurns);
      
      // Check results
      const successful = results.filter(r => r.status === 'fulfilled').length;
      const failed = results.filter(r => r.status === 'rejected').length;
      
      console.log(`[Concurrency] ${successful} successful, ${failed} failed`);
      
      // Verify final state
      const finalResponse = await page.request.get(`${debateServiceURL}/debates/${debate.id}`);
      const finalDebate = await finalResponse.json();
      
      // All turns should be present and ordered correctly
      expect(finalDebate.turns.length).toBe(successful);
      
      // Turn numbers should be sequential
      const turnNumbers = finalDebate.turns.map(t => t.turnNumber).sort((a, b) => a - b);
      for (let i = 0; i < turnNumbers.length - 1; i++) {
        expect(turnNumbers[i + 1] - turnNumbers[i]).toBe(1);
      }
    });

    test('should validate data constraints', async ({ page }) => {
      // Test various constraint violations
      const invalidDebates = [
        {
          name: 'Missing participants',
          data: {
            name: 'Invalid Debate',
            topic: 'Testing constraints',
            participants: []
          }
        },
        {
          name: 'Invalid participant data',
          data: {
            name: 'Invalid Debate 2',
            topic: 'Testing constraints',
            participants: [
              { name: 'Bot 1' }, // Missing required fields
              { name: 'Bot 2', provider: 'invalid-provider', model: 'invalid-model' }
            ]
          }
        },
        {
          name: 'Duplicate participant names',
          data: {
            name: 'Invalid Debate 3',
            topic: 'Testing constraints',
            participants: [
              { name: 'Same Name', provider: 'claude', model: 'claude-3-5-sonnet-20241022', role: 'debater' },
              { name: 'Same Name', provider: 'gemini', model: 'gemini-2.5-pro', role: 'debater' }
            ]
          }
        }
      ];
      
      for (const testCase of invalidDebates) {
        const response = await page.request.post(`${debateServiceURL}/debates`, {
          data: testCase.data
        });
        
        expect(response.status()).toBe(400);
        console.log(`[Validation] ${testCase.name}: Correctly rejected with status ${response.status()}`);
      }
    });
  });

  test.describe('Query Performance', () => {
    test('should efficiently retrieve debates with filters', async ({ page }) => {
      // Create debates with different statuses
      const statuses = ['draft', 'active', 'completed'];
      const debatesByStatus = {};
      
      for (const status of statuses) {
        debatesByStatus[status] = [];
        
        // Create 3 debates per status
        for (let i = 0; i < 3; i++) {
          const response = await page.request.post(`${debateServiceURL}/debates`, {
            data: {
              name: `${status} Debate ${i + 1}`,
              topic: `Testing ${status} queries`,
              participants: [
                { name: 'Bot 1', provider: 'claude', model: 'claude-3-5-sonnet-20241022', role: 'debater' },
                { name: 'Bot 2', provider: 'gemini', model: 'gemini-2.5-pro', role: 'debater' }
              ],
              metadata: { testStatus: status }
            }
          });
          
          const debate = await response.json();
          createdDebateIds.push(debate.id);
          debatesByStatus[status].push(debate.id);
          
          // Set appropriate status
          if (status === 'active') {
            await page.request.post(`${debateServiceURL}/debates/${debate.id}/start`);
          } else if (status === 'completed') {
            await page.request.post(`${debateServiceURL}/debates/${debate.id}/start`);
            await page.request.post(`${debateServiceURL}/debates/${debate.id}/complete`);
          }
        }
      }
      
      // Test filtered queries
      for (const status of statuses) {
        const startTime = Date.now();
        const response = await page.request.get(`${debateServiceURL}/debates?status=${status}`);
        const queryTime = Date.now() - startTime;
        
        expect(response.ok()).toBeTruthy();
        const debates = await response.json();
        
        // Should return debates with correct status
        const filteredDebates = debates.debates.filter(d => 
          debatesByStatus[status].includes(d.id)
        );
        
        expect(filteredDebates.length).toBeGreaterThanOrEqual(3);
        console.log(`[Query Performance] ${status} debates: ${filteredDebates.length} results in ${queryTime}ms`);
        
        // Query should be fast
        expect(queryTime).toBeLessThan(1000); // Less than 1 second
      }
    });

    test('should handle pagination efficiently', async ({ page }) => {
      // Test pagination
      const pageSize = 10;
      const response = await page.request.get(
        `${debateServiceURL}/debates?page=1&pageSize=${pageSize}`
      );
      
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      
      expect(data).toHaveProperty('debates');
      expect(data).toHaveProperty('pagination');
      expect(data.debates.length).toBeLessThanOrEqual(pageSize);
      
      if (data.pagination.totalPages > 1) {
        // Test second page
        const page2Response = await page.request.get(
          `${debateServiceURL}/debates?page=2&pageSize=${pageSize}`
        );
        
        const page2Data = await page2Response.json();
        expect(page2Data.debates.length).toBeGreaterThan(0);
        
        // Ensure no duplicate IDs between pages
        const page1Ids = data.debates.map(d => d.id);
        const page2Ids = page2Data.debates.map(d => d.id);
        const intersection = page1Ids.filter(id => page2Ids.includes(id));
        expect(intersection).toHaveLength(0);
      }
    });
  });

  // Helper function
  async function getDebateStatus(page: any, debateId: string): Promise<string> {
    const response = await page.request.get(`${debateServiceURL}/debates/${debateId}`);
    const debate = await response.json();
    return debate.status;
  }
});