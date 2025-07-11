import puppeteer, { Browser, Page } from 'puppeteer';
import { config } from '../config';
import { ApiHelpers } from '../helpers/api-helpers';
import { HomePage } from '../pages/home-page';
import { CreateDebateDialog } from '../pages/create-debate-dialog';
import { DebateViewPage } from '../pages/debate-view-page';

describe('Multi-tenant Organization Tests', () => {
  let browser: Browser;
  let page: Page;
  let homePage: HomePage;

  beforeAll(async () => {
    // Wait for services to be ready
    await ApiHelpers.waitForAllServices();
    
    // Launch browser
    browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
  });

  afterAll(async () => {
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

  describe('Organization Switching', () => {
    test('should create and switch between organizations', async () => {
      // Navigate to home
      await homePage.navigate();
      
      // Check if organization switcher is visible
      await page.waitForSelector('[data-testid="organization-switcher"]', { timeout: 10000 });
      
      // Click organization switcher
      await page.click('[data-testid="organization-switcher"]');
      
      // Click "Create Organization"
      await page.waitForSelector('text/Create Organization');
      await page.click('text/Create Organization');
      
      // Fill in organization details
      await page.waitForSelector('input#org-name');
      await page.type('input#org-name', 'Test Organization Alpha');
      
      // Submit
      await page.click('text/Create Organization');
      
      // Verify organization was created and switched
      await page.waitForFunction(
        () => document.querySelector('[data-testid="organization-switcher"]')?.textContent?.includes('Test Organization Alpha'),
        { timeout: 5000 }
      );
      
      // Create a debate in this organization
      const createDialog = new CreateDebateDialog(page);
      await homePage.clickCreateDebate();
      await createDialog.fillDebateName('Alpha Organization Debate');
      await createDialog.fillTopic('Topic for Alpha Org');
      await createDialog.submit();
      
      // Wait for debate to be created
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      
      // Switch back to default organization
      await page.click('[data-testid="organization-switcher"]');
      await page.click('text/Default Organization');
      
      // Verify debates are different
      const debates = await homePage.getDebateCards();
      const alphaDebate = debates.find(d => d.title === 'Alpha Organization Debate');
      expect(alphaDebate).toBeUndefined();
    }, 30000);

    test('should maintain separate debate contexts per organization', async () => {
      // Create debates in two different organizations
      await homePage.navigate();
      
      // Organization 1
      const org1Debate = {
        name: 'Org1 AI Ethics Debate',
        topic: 'Should AI be regulated?'
      };
      
      // Organization 2  
      await page.click('[data-testid="organization-switcher"]');
      await page.click('text/Create Organization');
      await page.type('input#org-name', 'Research Team Beta');
      await page.click('text/Create Organization');
      
      const org2Debate = {
        name: 'Org2 Climate Debate',
        topic: 'Carbon tax effectiveness'
      };
      
      // Create debate in Org2
      const createDialog = new CreateDebateDialog(page);
      await homePage.clickCreateDebate();
      await createDialog.fillDebateName(org2Debate.name);
      await createDialog.fillTopic(org2Debate.topic);
      await createDialog.submit();
      
      // Verify only Org2 debates are visible
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      const debates = await homePage.getDebateCards();
      expect(debates.some(d => d.title === org2Debate.name)).toBeTruthy();
      expect(debates.some(d => d.title === org1Debate.name)).toBeFalsy();
    }, 30000);
  });

  describe('Implementation Tracking', () => {
    test('should track implementations per debate', async () => {
      await homePage.navigate();
      
      // Create a debate
      const createDialog = new CreateDebateDialog(page);
      await homePage.clickCreateDebate();
      await createDialog.fillDebateName('Implementation Test Debate');
      await createDialog.fillTopic('Testing implementation tracking');
      await createDialog.submit();
      
      // Open the debate
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      await homePage.clickDebateCard('Implementation Test Debate');
      
      // Wait for debate view
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      
      // Check for implementation tracker
      await page.waitForSelector('[data-testid="implementation-tracker"]', { timeout: 5000 });
      
      // Click add implementation
      await page.click('text/Add Implementation');
      
      // Fill implementation details
      await page.waitForSelector('textarea#description');
      await page.type('textarea#description', 'Implemented debate logic based on AI discussion');
      await page.type('input#commit', 'abc123def456');
      await page.type('input#branch', 'feature/ai-debate');
      await page.type('input#repository', 'company/debate-system');
      
      // Select status
      await page.click('text/In Progress');
      
      // Submit
      await page.click('text/Track Implementation');
      
      // Verify implementation was added
      await page.waitForFunction(
        () => window.document.body.textContent?.includes('Implemented debate logic based on AI discussion'),
        { timeout: 5000 }
      );
      
      // Verify commit hash is shown
      expect(await page.$eval('text/abc123d', el => el.textContent)).toBeTruthy();
    }, 30000);

    test('should show implementation history with commit details', async () => {
      await homePage.navigate();
      
      // Navigate to existing debate with implementations
      const debates = await homePage.getDebateCards();
      if (debates.length > 0) {
        await homePage.clickDebateCard(debates[0].title);
        
        const debateView = new DebateViewPage(page);
        await debateView.isLoaded();
        
        // Check implementation stats if any exist
        const hasImplementations = await page.$('[data-testid="implementation-stats"]');
        if (hasImplementations) {
          const stats = await page.$eval('[data-testid="implementation-stats"]', el => el.textContent);
          expect(stats).toMatch(/\d+ Commits/);
        }
      }
    }, 20000);
  });

  describe('Organization History', () => {
    test('should track organization actions in history', async () => {
      await homePage.navigate();
      
      // Open organization menu
      await page.click('[data-testid="organization-switcher"]');
      
      // Click View History
      await page.click('text/View History');
      
      // Wait for history dialog/page
      await page.waitForSelector('[data-testid="organization-history"]', { timeout: 5000 });
      
      // Verify history entries exist
      const historyEntries = await page.$$('[data-testid="history-entry"]');
      expect(historyEntries.length).toBeGreaterThan(0);
      
      // Check for organization switch events
      const hasOrgSwitch = await page.evaluate(() => 
        Array.from(window.document.querySelectorAll('[data-testid="history-entry"]'))
          .some((el: Element) => el.textContent?.includes('organization_switched'))
      );
      expect(hasOrgSwitch).toBeTruthy();
    }, 20000);
  });
});