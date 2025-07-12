import { test, expect } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';

/**
 * Comprehensive Debate Testing Suite
 * 
 * Goal: Test the core functionality of the AI debate system with real LLM integrations
 * Focus: Claude 3.5 Sonnet vs Gemini 2.5 Pro debates
 * 
 * Test Objectives:
 * 1. Create debates between two different LLM providers
 * 2. Verify debate turns are generated with meaningful content
 * 3. Track debate progression and history
 * 4. Verify database persistence
 * 5. Test debate quality and coherence
 * 6. Capture comprehensive evidence of all operations
 */

test.describe('Comprehensive AI Debate System Tests', () => {
  const testRunId = uuidv4();
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  
  // Test configuration
  const DEFAULT_DEBATE_CONFIG = {
    llm1: {
      provider: 'claude',
      model: 'claude-3-5-sonnet-20241022',
      name: 'Claude Sonnet 4 Advocate'
    },
    llm2: {
      provider: 'gemini',
      model: 'gemini-2.5-pro',
      name: 'Gemini 2.5 Pro Challenger'
    }
  };

  test.beforeEach(async ({ page }) => {
    console.log(`[Test Run ${testRunId}] Starting new test`);
    
    // Set up request logging for evidence collection
    page.on('request', request => {
      if (request.url().includes('/api/')) {
        console.log(`[API Request] ${request.method()} ${request.url()}`);
      }
    });
    
    page.on('response', response => {
      if (response.url().includes('/api/') && response.status() !== 200) {
        console.log(`[API Response] ${response.status()} ${response.url()}`);
      }
    });
    
    // Navigate to the application
    await page.goto(baseURL);
    await page.waitForLoadState('networkidle');
    
    // Verify services are healthy
    await test.step('Verify all services are healthy', async () => {
      const healthChecks = [
        { service: 'LLM Service', url: 'http://localhost:5002/health' },
        { service: 'Debate Service', url: 'http://localhost:5013/health' }
      ];
      
      for (const check of healthChecks) {
        const response = await page.request.get(check.url);
        expect(response.ok(), `${check.service} should be healthy`).toBeTruthy();
        const health = await response.json();
        console.log(`[Health Check] ${check.service}: ${JSON.stringify(health)}`);
      }
    });
  });

  test('Complete debate flow: Problem-solving discussion between Claude and Gemini', async ({ page }) => {
    test.setTimeout(300000); // 5 minutes for complete debate
    
    const debateTopic = 'How can we design a sustainable city transportation system for 2030?';
    const debateName = `Transportation Debate ${new Date().toISOString()}`;
    
    await test.step('Create a new debate', async () => {
      console.log(`[Debate Creation] Topic: ${debateTopic}`);
      
      // Click create debate button
      await page.getByRole('button', { name: /create.*debate/i }).click();
      await page.waitForSelector('[role="dialog"]', { state: 'visible' });
      
      // Fill debate details
      await page.fill('[name="name"]', debateName);
      await page.fill('[name="topic"]', debateTopic);
      await page.fill('[name="description"]', 
        'A comprehensive discussion between AI models on designing sustainable urban transportation');
      
      // Set debate rules
      await page.fill('[name="maxRounds"]', '5');
      await page.fill('[name="maxTurnsPerRound"]', '2');
      await page.fill('[name="turnTimeLimit"]', '120'); // 2 minutes per turn
      
      // Configure first participant (Claude 3.5 Sonnet)
      await test.step('Configure Claude 3.5 Sonnet participant', async () => {
        await page.fill('[data-testid="participant-0-name"]', DEFAULT_DEBATE_CONFIG.llm1.name);
        await page.fill('[data-testid="participant-0-position"]', 
          'Advocate for integrated public transit and cycling infrastructure');
        
        // Select provider
        await page.selectOption('[data-testid="participant-0-provider"]', DEFAULT_DEBATE_CONFIG.llm1.provider);
        await page.waitForTimeout(500); // Wait for models to load
        
        // Select model
        await page.selectOption('[data-testid="participant-0-model"]', DEFAULT_DEBATE_CONFIG.llm1.model);
        
        // Set parameters
        await page.fill('[data-testid="participant-0-temperature"]', '0.7');
        await page.fill('[data-testid="participant-0-maxTokens"]', '2000');
        
        // Take screenshot of configuration
        await page.screenshot({ 
          path: `test_probe/evidence/screenshots/claude-config-${testRunId}.png`,
          fullPage: true 
        });
      });
      
      // Configure second participant (Gemini 2.5 Pro)
      await test.step('Configure Gemini 2.5 Pro participant', async () => {
        // Add second participant
        await page.getByRole('button', { name: /add.*participant/i }).click();
        
        await page.fill('[data-testid="participant-1-name"]', DEFAULT_DEBATE_CONFIG.llm2.name);
        await page.fill('[data-testid="participant-1-position"]', 
          'Proponent of autonomous vehicles and smart traffic management');
        
        // Select provider
        await page.selectOption('[data-testid="participant-1-provider"]', DEFAULT_DEBATE_CONFIG.llm2.provider);
        await page.waitForTimeout(500); // Wait for models to load
        
        // Select model
        await page.selectOption('[data-testid="participant-1-model"]', DEFAULT_DEBATE_CONFIG.llm2.model);
        
        // Set parameters
        await page.fill('[data-testid="participant-1-temperature"]', '0.8');
        await page.fill('[data-testid="participant-1-maxTokens"]', '2000');
        
        // Take screenshot of configuration
        await page.screenshot({ 
          path: `test_probe/evidence/screenshots/gemini-config-${testRunId}.png`,
          fullPage: true 
        });
      });
      
      // Submit debate creation
      await page.getByRole('button', { name: /create/i }).last().click();
      
      // Wait for navigation to debate view
      await page.waitForURL(/\/debate\/[a-zA-Z0-9-]+/, { timeout: 10000 });
      console.log(`[Debate Created] URL: ${page.url()}`);
    });
    
    // Extract debate ID from URL
    const debateId = page.url().split('/').pop();
    console.log(`[Debate ID] ${debateId}`);
    
    await test.step('Start the debate', async () => {
      // Wait for debate page to load
      await page.waitForSelector('[data-testid="debate-status"]', { state: 'visible' });
      
      // Verify initial status
      const initialStatus = await page.textContent('[data-testid="debate-status"]');
      expect(initialStatus?.toLowerCase()).toBe('draft');
      
      // Start the debate
      await page.getByRole('button', { name: /start.*debate/i }).click();
      
      // Wait for status to change to active
      await page.waitForFunction(
        () => {
          const statusEl = document.querySelector('[data-testid="debate-status"]');
          return statusEl?.textContent?.toLowerCase() === 'active';
        },
        { timeout: 10000 }
      );
      
      console.log('[Debate Started] Status: active');
    });
    
    // Track debate turns
    const debateTranscript = {
      debateId,
      topic: debateTopic,
      participants: [DEFAULT_DEBATE_CONFIG.llm1, DEFAULT_DEBATE_CONFIG.llm2],
      turns: []
    };
    
    await test.step('Monitor debate progression', async () => {
      let turnCount = 0;
      const maxTurns = 10; // 5 rounds * 2 turns per round
      
      while (turnCount < maxTurns) {
        // Wait for new turn
        await page.waitForFunction(
          (currentCount) => {
            const turns = document.querySelectorAll('[data-testid^="debate-turn-"]');
            return turns.length > currentCount;
          },
          turnCount,
          { timeout: 120000 } // 2 minute timeout per turn
        );
        
        turnCount++;
        
        // Extract turn content
        const turnElement = await page.locator(`[data-testid="debate-turn-${turnCount}"]`);
        const speaker = await turnElement.locator('[data-testid="turn-speaker"]').textContent();
        const content = await turnElement.locator('[data-testid="turn-content"]').textContent();
        const timestamp = await turnElement.locator('[data-testid="turn-timestamp"]').textContent();
        
        const turnData = {
          turnNumber: turnCount,
          speaker: speaker || 'Unknown',
          content: content || '',
          timestamp: timestamp || new Date().toISOString(),
          contentLength: content?.length || 0
        };
        
        debateTranscript.turns.push(turnData);
        
        console.log(`[Turn ${turnCount}] Speaker: ${speaker}, Length: ${turnData.contentLength} chars`);
        
        // Take screenshot every 2 turns
        if (turnCount % 2 === 0) {
          await page.screenshot({ 
            path: `test_probe/evidence/screenshots/debate-turn-${turnCount}-${testRunId}.png`,
            fullPage: true 
          });
        }
        
        // Check debate status
        const currentStatus = await page.textContent('[data-testid="debate-status"]');
        if (currentStatus?.toLowerCase() === 'completed') {
          console.log('[Debate Completed] Final turn count:', turnCount);
          break;
        }
      }
      
      // Save debate transcript
      const fs = require('fs');
      fs.writeFileSync(
        `test_probe/evidence/debate-transcripts/transcript-${debateId}.json`,
        JSON.stringify(debateTranscript, null, 2)
      );
    });
    
    await test.step('Analyze debate quality', async () => {
      // Verify turns have substantial content
      const substantialTurns = debateTranscript.turns.filter(turn => turn.contentLength > 100);
      expect(substantialTurns.length).toBeGreaterThan(debateTranscript.turns.length * 0.8);
      
      // Verify both participants contributed
      const claudeTurns = debateTranscript.turns.filter(turn => 
        turn.speaker.includes('Claude'));
      const geminiTurns = debateTranscript.turns.filter(turn => 
        turn.speaker.includes('Gemini'));
      
      expect(claudeTurns.length).toBeGreaterThan(0);
      expect(geminiTurns.length).toBeGreaterThan(0);
      expect(Math.abs(claudeTurns.length - geminiTurns.length)).toBeLessThanOrEqual(1);
      
      // Verify topic relevance (check if transportation keywords appear)
      const transportKeywords = ['transport', 'city', 'urban', 'sustainable', 'traffic', 
                                'public', 'vehicle', 'infrastructure', 'mobility'];
      const relevantTurns = debateTranscript.turns.filter(turn => 
        transportKeywords.some(keyword => 
          turn.content.toLowerCase().includes(keyword)
        )
      );
      
      expect(relevantTurns.length).toBeGreaterThan(debateTranscript.turns.length * 0.7);
      
      console.log('[Quality Analysis]');
      console.log(`- Total turns: ${debateTranscript.turns.length}`);
      console.log(`- Substantial turns: ${substantialTurns.length}`);
      console.log(`- Claude turns: ${claudeTurns.length}`);
      console.log(`- Gemini turns: ${geminiTurns.length}`);
      console.log(`- Topic-relevant turns: ${relevantTurns.length}`);
    });
    
    await test.step('Verify debate persistence in database', async () => {
      // Refresh the page
      await page.reload();
      await page.waitForLoadState('networkidle');
      
      // Navigate back to home
      await page.goto(baseURL);
      
      // Search for the debate
      await page.fill('[data-testid="debate-search"]', debateName);
      await page.waitForTimeout(1000); // Debounce delay
      
      // Verify debate appears in list
      const debateCard = page.locator('[data-testid="debate-card"]', { hasText: debateName });
      await expect(debateCard).toBeVisible();
      
      // Click on the debate
      await debateCard.click();
      await page.waitForURL(/\/debate\/[a-zA-Z0-9-]+/);
      
      // Verify all turns are loaded
      const loadedTurns = await page.locator('[data-testid^="debate-turn-"]').count();
      expect(loadedTurns).toBe(debateTranscript.turns.length);
      
      console.log('[Database Verification] All turns persisted and retrieved successfully');
    });
    
    await test.step('Generate debate summary report', async () => {
      const summary = {
        testRunId,
        debateId,
        topic: debateTopic,
        participants: DEFAULT_DEBATE_CONFIG,
        metrics: {
          totalTurns: debateTranscript.turns.length,
          averageTurnLength: Math.round(
            debateTranscript.turns.reduce((sum, turn) => sum + turn.contentLength, 0) / 
            debateTranscript.turns.length
          ),
          debateDuration: 'Calculated from timestamps',
          topicRelevance: `${Math.round(
            debateTranscript.turns.filter(turn => 
              turn.content.toLowerCase().includes('transport')
            ).length / debateTranscript.turns.length * 100
          )}%`
        },
        proposedSolutions: extractProposedSolutions(debateTranscript),
        areasOfAgreement: findAreasOfAgreement(debateTranscript),
        areasOfDisagreement: findAreasOfDisagreement(debateTranscript),
        timestamp: new Date().toISOString()
      };
      
      // Save summary
      const fs = require('fs');
      fs.writeFileSync(
        `test_probe/evidence/debate-transcripts/summary-${debateId}.json`,
        JSON.stringify(summary, null, 2)
      );
      
      console.log('[Debate Summary Generated]');
      console.log(JSON.stringify(summary.metrics, null, 2));
    });
  });
  
  test('Debate with custom problem: AI Ethics and Regulation', async ({ page }) => {
    test.setTimeout(300000); // 5 minutes
    
    const problemStatement = `Should AI systems be required to pass ethical certification before deployment? 
    Consider aspects like bias testing, transparency requirements, and accountability measures.`;
    
    await test.step('Create ethics debate', async () => {
      await page.getByRole('button', { name: /create.*debate/i }).click();
      
      await page.fill('[name="name"]', `Ethics Debate ${Date.now()}`);
      await page.fill('[name="topic"]', 'AI Ethics Certification Requirements');
      await page.fill('[name="description"]', problemStatement);
      
      // Configure for shorter debate
      await page.fill('[name="maxRounds"]', '3');
      await page.fill('[name="maxTurnsPerRound"]', '2');
      
      // Set up participants with different perspectives
      await page.fill('[data-testid="participant-0-name"]', 'Ethics Advocate (Claude)');
      await page.fill('[data-testid="participant-0-position"]', 
        'Strong proponent of mandatory ethical certification');
      await page.selectOption('[data-testid="participant-0-provider"]', 'claude');
      await page.selectOption('[data-testid="participant-0-model"]', 'claude-3-5-sonnet-20241022');
      
      await page.getByRole('button', { name: /add.*participant/i }).click();
      
      await page.fill('[data-testid="participant-1-name"]', 'Innovation Focus (Gemini)');
      await page.fill('[data-testid="participant-1-position"]', 
        'Concerned about regulatory burden on innovation');
      await page.selectOption('[data-testid="participant-1-provider"]', 'gemini');
      await page.selectOption('[data-testid="participant-1-model"]', 'gemini-2.5-pro');
      
      await page.getByRole('button', { name: /create/i }).last().click();
      await page.waitForURL(/\/debate\//);
    });
    
    await test.step('Run and analyze ethics debate', async () => {
      await page.getByRole('button', { name: /start.*debate/i }).click();
      
      // Wait for completion
      await page.waitForFunction(
        () => {
          const status = document.querySelector('[data-testid="debate-status"]');
          return status?.textContent?.toLowerCase() === 'completed';
        },
        { timeout: 240000 } // 4 minutes
      );
      
      // Extract key points
      const turns = await page.locator('[data-testid^="debate-turn-"]').allTextContents();
      
      const ethicsKeywords = ['bias', 'fairness', 'transparency', 'accountability', 
                             'certification', 'regulation', 'ethics', 'safety'];
      const keywordFrequency = {};
      
      turns.forEach(turn => {
        ethicsKeywords.forEach(keyword => {
          const count = (turn.toLowerCase().match(new RegExp(keyword, 'g')) || []).length;
          keywordFrequency[keyword] = (keywordFrequency[keyword] || 0) + count;
        });
      });
      
      console.log('[Ethics Debate Analysis]');
      console.log('Keyword frequency:', keywordFrequency);
      
      // Verify substantive discussion
      expect(Object.values(keywordFrequency).reduce((a, b) => a + b, 0)).toBeGreaterThan(20);
    });
  });
  
  test('Performance and reliability testing', async ({ page }) => {
    await test.step('Test concurrent debate creation', async () => {
      const debatePromises = [];
      
      for (let i = 0; i < 3; i++) {
        debatePromises.push(
          page.evaluate(async (index) => {
            const response = await fetch('/api/debate/debates', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                name: `Performance Test ${index}`,
                topic: 'Quick test topic',
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
              })
            });
            return response.ok;
          }, i)
        );
      }
      
      const results = await Promise.all(debatePromises);
      expect(results.every(r => r === true)).toBeTruthy();
      
      console.log('[Performance Test] Created 3 debates concurrently');
    });
    
    await test.step('Test error handling', async () => {
      // Test with invalid model
      await page.getByRole('button', { name: /create.*debate/i }).click();
      await page.fill('[name="name"]', 'Error Test Debate');
      await page.fill('[name="topic"]', 'Testing error handling');
      
      // Try to create without participants
      await page.getByRole('button', { name: /create/i }).last().click();
      
      // Should show error
      await expect(page.locator('.error-message')).toBeVisible();
    });
  });
});

// Helper functions for analysis
function extractProposedSolutions(transcript) {
  const solutions = [];
  const solutionKeywords = ['propose', 'suggest', 'solution', 'implement', 'could', 'should'];
  
  transcript.turns.forEach(turn => {
    const sentences = turn.content.split(/[.!?]+/);
    sentences.forEach(sentence => {
      if (solutionKeywords.some(keyword => sentence.toLowerCase().includes(keyword))) {
        solutions.push({
          speaker: turn.speaker,
          proposal: sentence.trim()
        });
      }
    });
  });
  
  return solutions.slice(0, 10); // Top 10 solutions
}

function findAreasOfAgreement(transcript) {
  const agreements = [];
  const agreementPhrases = ['agree', 'correct', 'indeed', 'absolutely', 'good point'];
  
  transcript.turns.forEach((turn, index) => {
    if (index > 0) {
      const previousSpeaker = transcript.turns[index - 1].speaker;
      if (previousSpeaker !== turn.speaker) {
        agreementPhrases.forEach(phrase => {
          if (turn.content.toLowerCase().includes(phrase)) {
            agreements.push({
              turn: turn.turnNumber,
              speaker: turn.speaker,
              context: turn.content.substring(0, 200) + '...'
            });
          }
        });
      }
    }
  });
  
  return agreements;
}

function findAreasOfDisagreement(transcript) {
  const disagreements = [];
  const disagreementPhrases = ['however', 'but', 'disagree', 'concern', 'problem with'];
  
  transcript.turns.forEach((turn, index) => {
    if (index > 0) {
      const previousSpeaker = transcript.turns[index - 1].speaker;
      if (previousSpeaker !== turn.speaker) {
        disagreementPhrases.forEach(phrase => {
          if (turn.content.toLowerCase().includes(phrase)) {
            disagreements.push({
              turn: turn.turnNumber,
              speaker: turn.speaker,
              context: turn.content.substring(0, 200) + '...'
            });
          }
        });
      }
    }
  });
  
  return disagreements;
}