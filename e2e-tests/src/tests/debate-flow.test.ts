import puppeteer, { Browser, Page } from 'puppeteer';
import { config } from '../config';
import { ApiHelpers } from '../helpers/api-helpers';
import { HomePage } from '../pages/home-page';
import { CreateDebateDialog } from '../pages/create-debate-dialog';
import { DebateViewPage } from '../pages/debate-view-page';
import { 
  setupPage,
  ensureServicesReady,
  waitForDebounce,
  retryOperation,
  takeScreenshot,
  waitForSelector,
  clickWithRetry,
  waitForText
} from '../helpers/test-utils';

describe('Debate System E2E Tests', () => {
  let browser: Browser;
  let page: Page;
  let homePage: HomePage;

  beforeAll(async () => {
    // Ensure all services are ready with proper health checks
    await ensureServicesReady();
    
    // Create test organization with retry logic
    await retryOperation(
      () => ApiHelpers.createTestOrganization(),
      { retries: 3, delay: 2000 }
    );
    
    // Launch browser with optimized settings
    browser = await puppeteer.launch({
      ...config.PUPPETEER_OPTIONS,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu',
        '--disable-web-security',
        '--disable-features=IsolateOrigins',
        '--disable-site-isolation-trials'
      ]
    });
  }, 120000);

  afterAll(async () => {
    // Cleanup
    await ApiHelpers.cleanupTestData();
    
    if (browser) {
      await browser.close();
    }
  });

  beforeEach(async () => {
    page = await setupPage(browser);
    homePage = new HomePage(page);
    
    // Clear any modals or overlays
    await page.evaluate(() => {
      document.querySelectorAll('[role="dialog"]').forEach(el => el.remove());
      document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
    });
  });

  afterEach(async () => {
    if (page && !page.isClosed()) {
      // Take screenshot on test failure
      const testName = expect.getState().currentTestName;
      if (expect.getState().isNot) {
        await takeScreenshot(page, `failed-${testName?.replace(/[^a-z0-9]/gi, '-')}`);
      }
      await page.close();
    }
  });

  describe('Homepage', () => {
    test('should load successfully', async () => {
      await retryOperation(async () => {
        await homePage.navigate();
        await waitForDebounce(1000);
        
        // Wait for main elements
        await waitForSelector(page, 'h1', { timeout: 10000 });
        await waitForText(page, 'AI Debate System', { timeout: 10000 });
        
        const title = await homePage.getTitle();
        expect(title).toContain('AI Debate System');
        
        // Verify main elements are loaded
        expect(await homePage.isLoaded()).toBeTruthy();
      }, { retries: 3, delay: 2000 });
    }, 30000);

    test('should display debate statistics', async () => {
      await homePage.navigate();
      
      const debateCount = await homePage.getDebateCount();
      expect(debateCount).toBeGreaterThanOrEqual(0);
      
      const turnCount = await homePage.getTurnCount();
      expect(turnCount).toBeGreaterThanOrEqual(0);
    }, 20000);
  });

  describe('Create Debate', () => {
    test('should create a new debate with multiple participants', async () => {
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(page);
      expect(await createDialog.isOpen()).toBeTruthy();
      
      // Fill debate details
      await createDialog.fillDebateName('Test AI Ethics Debate');
      await createDialog.fillTopic('Should AI development be regulated?');
      await createDialog.fillDescription('A comprehensive debate about AI regulation');
      
      // Configure rules
      await createDialog.setMaxRounds(3);
      
      // Configure participants
      await createDialog.fillParticipant(0, {
        name: 'Pro-Regulation Advocate',
        position: 'Supporting AI regulation',
        provider: 'claude',
        model: 'claude-3-5-sonnet-20241022',
        temperature: 0.7,
        role: 'debater'
      });
      
      await createDialog.fillParticipant(1, {
        name: 'Innovation Defender',
        position: 'Against excessive regulation',
        provider: 'gemini',
        model: 'gemini-2.5-pro',
        temperature: 0.8,
        role: 'debater'
      });
      
      // Submit with proper wait
      await createDialog.submit();
      
      // Wait for navigation or confirmation
      await waitForDebounce(2000);
      
      // Verify debate was created with retry logic
      await retryOperation(async () => {
        const debates = await homePage.getDebateCards();
        const createdDebate = debates.find(d => d.title === 'Test AI Ethics Debate');
        expect(createdDebate).toBeDefined();
        expect(createdDebate?.status).toBe('draft');
      }, { retries: 5, delay: 1000 });
    }, 60000);

    test('should validate required fields', async () => {
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(page);
      
      // Try to submit without filling required fields
      await createDialog.submit();
      
      // Check for validation error
      const error = await createDialog.getValidationError();
      expect(error).toContain('Please fill in all required fields');
    }, 20000);
  });

  describe('Debate View', () => {
    let testDebateId: string;

    beforeEach(async () => {
      // Create a test debate for viewing
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(page);
      await createDialog.fillDebateName('View Test Debate');
      await createDialog.fillTopic('Testing debate view functionality');
      
      // Add participants with retry logic
      await retryOperation(async () => {
        await createDialog.fillParticipant(0, {
          name: 'Test Bot 1',
          position: 'Position A',
          provider: 'claude',
          model: 'claude-3-5-sonnet-20241022',
          temperature: 0.7,
          role: 'debater'
        });
        
        await createDialog.fillParticipant(1, {
          name: 'Test Bot 2',
          position: 'Position B',
          provider: 'openai',
          model: 'gpt-4o',
          temperature: 0.7,
          role: 'debater'
        });
      });
      
      await createDialog.submit();
      await waitForDebounce(2000);
    });

    test('should display debate details correctly', async () => {
      await homePage.clickDebateCard('View Test Debate');
      
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      
      const title = await debateView.getDebateTitle();
      expect(title).toBe('View Test Debate');
      
      const topic = await debateView.getDebateTopic();
      expect(topic).toBe('Testing debate view functionality');
      
      const status = await debateView.getDebateStatus();
      expect(status).toBe('draft');
    }, 20000);

    test('should show participants with their configurations', async () => {
      await homePage.clickDebateCard('View Test Debate');
      
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      
      const participants = await debateView.getParticipants();
      expect(participants.length).toBeGreaterThanOrEqual(2);
      
      // Check participant details
      participants.forEach(p => {
        expect(p.name).toBeTruthy();
        expect(p.role).toBeTruthy();
        expect(p.provider).toBeTruthy();
        expect(p.model).toBeTruthy();
      });
    }, 20000);

    test('should start and run a debate', async () => {
      await homePage.clickDebateCard('View Test Debate');
      
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      
      // Start the debate with retry logic
      await retryOperation(async () => {
        await debateView.startDebate();
        await waitForDebounce(2000);
        
        // Wait for status to change with proper polling
        await page.waitForFunction(
          () => {
            const statusElement = document.querySelector('[data-testid="debate-status"]');
            return statusElement?.textContent?.toLowerCase() === 'active';
          },
          { timeout: 10000, polling: 500 }
        );
      });
      
      const status = await debateView.getDebateStatus();
      expect(status).toBe('active');
      
      // Wait for first turn with extended timeout
      await retryOperation(
        () => debateView.waitForTurn(1, 45000),
        { retries: 3, delay: 5000 }
      );
      
      const turns = await debateView.getTurns();
      expect(turns.length).toBeGreaterThan(0);
      expect(turns[0].content).toBeTruthy();
      
      // Pause the debate
      await debateView.pauseDebate();
      await waitForDebounce(2000);
      
      // Verify pause status
      await retryOperation(async () => {
        const pausedStatus = await debateView.getDebateStatus();
        expect(pausedStatus).toBe('paused');
      });
    }, 90000);
  });

  describe('Live Debate Functionality', () => {
    test('should handle real-time turn updates', async () => {
      // Create and start a debate
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(page);
      await createDialog.fillDebateName('Live Debate Test');
      await createDialog.fillTopic('Real-time debate testing');
      await createDialog.setMaxRounds(2);
      await createDialog.submit();
      
      await waitForDebounce(2000);
      
      // Click debate card with retry
      await retryOperation(
        () => homePage.clickDebateCard('Live Debate Test'),
        { retries: 3, delay: 1000 }
      );
      
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      
      // Start debate
      await debateView.startDebate();
      
      // Monitor turn progression with proper error handling
      let previousTurnCount = 0;
      for (let i = 0; i < 4; i++) {
        try {
          await retryOperation(
            async () => {
              await debateView.waitForTurn(i + 1, 45000);
              const turnCount = await debateView.getTurnCount();
              expect(turnCount).toBeGreaterThan(previousTurnCount);
              previousTurnCount = turnCount;
            },
            { retries: 3, delay: 5000 }
          );
          
          // Check if participant is speaking
          const participants = await debateView.getParticipants();
          const speakingParticipant = participants.find(p => p.isSpeaking);
          if (i < 3) { // Not on last turn
            expect(speakingParticipant).toBeDefined();
          }
          
          // Add delay between turn checks
          await waitForDebounce(2000);
        } catch (error) {
          await takeScreenshot(page, `turn-progression-error-${i}`);
          throw error;
        }
      }
      
      // Wait for debate to complete with extended timeout
      await retryOperation(
        () => debateView.waitForDebateCompletion(90000),
        { retries: 2, delay: 10000 }
      );
      
      const finalStatus = await debateView.getDebateStatus();
      expect(finalStatus).toBe('completed');
    }, 180000);
  });

  describe('MCP Integration', () => {
    test('should verify MCP services are accessible', async () => {
      const services = [
        { name: 'context', port: 5001 },
        { name: 'llm', port: 5002 },
        { name: 'debate', port: 5013 },
        { name: 'rag', port: 5004 }
      ];
      
      for (const service of services) {
        await retryOperation(
          async () => {
            const isConnected = await ApiHelpers.verifyMCPConnection(
              service.name,
              `http://localhost:${service.port}`
            );
            expect(isConnected).toBeTruthy();
          },
          { retries: 5, delay: 2000 }
        );
      }
    }, 60000);
  });
});