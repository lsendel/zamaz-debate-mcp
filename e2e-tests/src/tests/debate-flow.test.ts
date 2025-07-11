import puppeteer, { Browser, Page } from 'puppeteer';
import { config } from '../config';
import { ApiHelpers } from '../helpers/api-helpers';
import { HomePage } from '../pages/home-page';
import { CreateDebateDialog } from '../pages/create-debate-dialog';
import { DebateViewPage } from '../pages/debate-view-page';

describe('Debate System E2E Tests', () => {
  let browser: Browser;
  let page: Page;
  let homePage: HomePage;

  beforeAll(async () => {
    // Wait for all services to be ready
    await ApiHelpers.waitForAllServices();
    
    // Create test organization
    await ApiHelpers.createTestOrganization();
    
    // Launch browser
    browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
  });

  afterAll(async () => {
    // Cleanup
    await ApiHelpers.cleanupTestData();
    
    if (browser) {
      await browser.close();
    }
  });

  beforeEach(async () => {
    page = await browser.newPage();
    homePage = new HomePage(page);
  });

  afterEach(async () => {
    if (page) {
      await page.close();
    }
  });

  describe('Homepage', () => {
    test('should load successfully', async () => {
      await homePage.navigate();
      
      const title = await homePage.getTitle();
      expect(title).toContain('AI Debate System');
      
      // Check if main elements are visible
      expect(await homePage.isLoaded()).toBeTruthy();
    }, 20000);

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
        provider: 'llama',
        model: 'llama3',
        temperature: 0.7,
        role: 'debater'
      });
      
      await createDialog.fillParticipant(1, {
        name: 'Innovation Defender',
        position: 'Against excessive regulation',
        provider: 'llama',
        model: 'mistral',
        temperature: 0.8,
        role: 'debater'
      });
      
      // Submit
      await createDialog.submit();
      
      // Verify debate was created
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
      const debates = await homePage.getDebateCards();
      const createdDebate = debates.find(d => d.title === 'Test AI Ethics Debate');
      expect(createdDebate).toBeDefined();
      expect(createdDebate?.status).toBe('draft');
    }, 30000);

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
      await createDialog.submit();
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
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
      
      // Start the debate
      await debateView.startDebate();
      
      // Wait for status to change
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      const status = await debateView.getDebateStatus();
      expect(status).toBe('active');
      
      // Wait for first turn
      await debateView.waitForTurn(1, 30000);
      
      const turns = await debateView.getTurns();
      expect(turns.length).toBeGreaterThan(0);
      expect(turns[0].content).toBeTruthy();
      
      // Pause the debate
      await debateView.pauseDebate();
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      
      const pausedStatus = await debateView.getDebateStatus();
      expect(pausedStatus).toBe('paused');
    }, 60000);
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
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      await homePage.clickDebateCard('Live Debate Test');
      
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      
      // Start debate
      await debateView.startDebate();
      
      // Monitor turn progression
      let previousTurnCount = 0;
      for (let i = 0; i < 4; i++) {
        await debateView.waitForTurn(i + 1, 30000);
        const turnCount = await debateView.getTurnCount();
        expect(turnCount).toBeGreaterThan(previousTurnCount);
        previousTurnCount = turnCount;
        
        // Check if participant is speaking
        const participants = await debateView.getParticipants();
        const speakingParticipant = participants.find(p => p.isSpeaking);
        if (i < 3) { // Not on last turn
          expect(speakingParticipant).toBeDefined();
        }
      }
      
      // Wait for debate to complete
      await debateView.waitForDebateCompletion(60000);
      const finalStatus = await debateView.getDebateStatus();
      expect(finalStatus).toBe('completed');
    }, 120000);
  });

  describe('MCP Integration', () => {
    test('should verify MCP services are accessible', async () => {
      const services = ['context', 'llm', 'debate', 'rag'];
      
      for (const service of services) {
        const isConnected = await ApiHelpers.verifyMCPConnection(
          service,
          `http://localhost:${8001 + services.indexOf(service)}`
        );
        expect(isConnected).toBeTruthy();
      }
    }, 20000);
  });
});