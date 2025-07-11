import puppeteer, { Browser, Page } from 'puppeteer';
import { config } from '../config';
import { ApiHelpers } from '../helpers/api-helpers';
import { HomePage } from '../pages/home-page';
import { CreateDebateDialog } from '../pages/create-debate-dialog';
import { DebateViewPage } from '../pages/debate-view-page';

describe('MCP Concurrency Tests', () => {
  let browsers: Browser[] = [];
  
  beforeAll(async () => {
    await ApiHelpers.waitForAllServices();
  });

  afterAll(async () => {
    // Close all browsers
    await Promise.all(browsers.map(b => b.close()));
  });

  describe('Concurrent Client Access', () => {
    test('should handle multiple clients creating debates simultaneously', async () => {
      const clientCount = 5;
      const results: any[] = [];
      
      // Create multiple browser instances
      for (let i = 0; i < clientCount; i++) {
        const browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
        browsers.push(browser);
      }
      
      // Execute concurrent operations
      const promises = browsers.map(async (browser, index) => {
        const page = await browser.newPage();
        const homePage = new HomePage(page);
        
        try {
          await homePage.navigate();
          await homePage.clickCreateDebate();
          
          const createDialog = new CreateDebateDialog(page);
          await createDialog.fillDebateName(`Concurrent Debate ${index + 1}`);
          await createDialog.fillTopic(`Testing concurrency - Client ${index + 1}`);
          await createDialog.submit();
          
          // Wait for creation
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
          
          // Verify debate was created
          const debates = await homePage.getDebateCards();
          const created = debates.find(d => d.title === `Concurrent Debate ${index + 1}`);
          
          return {
            clientId: index + 1,
            success: !!created,
            debateName: created?.title
          };
        } catch (error) {
          return {
            clientId: index + 1,
            success: false,
            error: (error as Error).message
          };
        } finally {
          await page.close();
        }
      });
      
      results.push(...await Promise.all(promises));
      
      // Verify all clients succeeded
      results.forEach(result => {
        expect(result.success).toBeTruthy();
        expect(result.debateName).toBeDefined();
      });
      
      // Verify no duplicates or conflicts
      const debateNames = results.map(r => r.debateName);
      const uniqueNames = new Set(debateNames);
      expect(uniqueNames.size).toBe(clientCount);
    }, 60000);

    test('should handle concurrent turn generation in same debate', async () => {
      // First, create a debate with multiple participants
      const browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
      browsers.push(browser);
      const page = await browser.newPage();
      const homePage = new HomePage(page);
      
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(page);
      await createDialog.fillDebateName('Concurrent Turn Test');
      await createDialog.fillTopic('Testing concurrent turn generation');
      
      // Add 4 participants
      await createDialog.addParticipant();
      await createDialog.addParticipant();
      
      for (let i = 0; i < 4; i++) {
        await createDialog.fillParticipant(i, {
          name: `Participant ${i + 1}`,
          provider: 'llama',
          model: 'llama3',
          role: 'debater'
        });
      }
      
      await createDialog.submit();
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      
      // Open debate in multiple windows
      const debatePages: Page[] = [];
      for (let i = 0; i < 3; i++) {
        const newPage = await browser.newPage();
        const newHomePage = new HomePage(newPage);
        await newHomePage.navigate();
        await newHomePage.clickDebateCard('Concurrent Turn Test');
        debatePages.push(newPage);
      }
      
      // Start debate from first page
      const debateView = new DebateViewPage(debatePages[0]);
      await debateView.isLoaded();
      await debateView.startDebate();
      
      // Monitor turn updates across all pages
      const turnCounts = await Promise.all(
        debatePages.map(async (p, index) => {
          const view = new DebateViewPage(p);
          await view.waitForTurn(2, 30000);
          return {
            pageIndex: index,
            turnCount: await view.getTurnCount()
          };
        })
      );
      
      // All pages should show same turn count
      const firstCount = turnCounts[0].turnCount;
      turnCounts.forEach(tc => {
        expect(tc.turnCount).toBe(firstCount);
      });
      
      // Cleanup
      await Promise.all(debatePages.map(p => p.close()));
    }, 90000);
  });

  describe('MCP Service Stress Test', () => {
    test('should handle rapid sequential API calls', async () => {
      const browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
      browsers.push(browser);
      const page = await browser.newPage();
      
      // Make rapid API calls
      const callCount = 20;
      const results: any[] = [];
      
      for (let i = 0; i < callCount; i++) {
        try {
          const response = await page.evaluate(async () => {
            const res = await fetch('/api/debate/health');
            return {
              status: res.status,
              ok: res.ok
            };
          });
          results.push({ success: response.ok, index: i });
        } catch (error) {
          results.push({ success: false, index: i, error });
        }
      }
      
      // Verify all calls succeeded
      const successCount = results.filter(r => r.success).length;
      expect(successCount).toBe(callCount);
      
      await page.close();
    }, 30000);

    test('should handle concurrent WebSocket connections', async () => {
      const wsClients = 10;
      const connections: any[] = [];
      
      // Create multiple WebSocket connections
      const connectPromises = Array.from({ length: wsClients }, async (_, i) => {
        return new Promise((resolve, reject) => {
          const ws = new (globalThis as any).WebSocket(`ws://localhost:5003/ws`);
          
          ws.onopen = () => {
            connections.push(ws);
            resolve({ clientId: i, connected: true });
          };
          
          ws.onerror = (error: any) => {
            reject({ clientId: i, connected: false, error });
          };
          
          // Set timeout
          setTimeout(() => {
            if (ws.readyState !== 1) { // WebSocket.OPEN = 1
              ws.close();
              reject({ clientId: i, connected: false, error: 'Timeout' });
            }
          }, 5000);
        });
      });
      
      const results = await Promise.allSettled(connectPromises);
      
      // Count successful connections
      const successfulConnections = results.filter(
        r => r.status === 'fulfilled' && (r.value as any).connected
      ).length;
      
      expect(successfulConnections).toBeGreaterThanOrEqual(wsClients * 0.8); // Allow 80% success rate
      
      // Cleanup
      connections.forEach(ws => ws.close());
    }, 20000);
  });

  describe('Organization Isolation', () => {
    test('should maintain data isolation between organizations', async () => {
      const orgCount = 3;
      const orgBrowsers: { browser: Browser; orgId: string; debates: string[] }[] = [];
      
      // Create browsers for different organizations
      for (let i = 0; i < orgCount; i++) {
        const browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
        browsers.push(browser);
        orgBrowsers.push({
          browser,
          orgId: `test-org-${i}`,
          debates: []
        });
      }
      
      // Each org creates debates
      const promises = orgBrowsers.map(async ({ browser, orgId }, index) => {
        const page = await browser.newPage();
        
        // Set organization context
        await page.goto('about:blank');
        await page.evaluate((orgId) => {
          localStorage.setItem('currentOrganizationId', orgId);
        }, orgId);
        
        const homePage = new HomePage(page);
        await homePage.navigate();
        
        // Create debates specific to this org
        for (let j = 0; j < 2; j++) {
          await homePage.clickCreateDebate();
          const createDialog = new CreateDebateDialog(page);
          const debateName = `Org${index + 1} Debate ${j + 1}`;
          await createDialog.fillDebateName(debateName);
          await createDialog.fillTopic(`Topic for ${orgId}`);
          await createDialog.submit();
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
          orgBrowsers[index].debates.push(debateName);
        }
        
        // Verify only this org's debates are visible
        const visibleDebates = await homePage.getDebateCards();
        const debateTitles = visibleDebates.map(d => d.title);
        
        await page.close();
        
        return {
          orgId,
          visibleDebates: debateTitles,
          expectedDebates: orgBrowsers[index].debates
        };
      });
      
      const results = await Promise.all(promises);
      
      // Verify isolation
      results.forEach(result => {
        // Should see own debates
        result.expectedDebates.forEach(debate => {
          expect(result.visibleDebates).toContain(debate);
        });
        
        // Should NOT see other orgs' debates
        results.forEach(otherResult => {
          if (otherResult.orgId !== result.orgId) {
            otherResult.expectedDebates.forEach(debate => {
              expect(result.visibleDebates).not.toContain(debate);
            });
          }
        });
      });
    }, 90000);
  });

  describe('Race Condition Prevention', () => {
    test('should handle concurrent updates to same debate', async () => {
      const browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
      browsers.push(browser);
      
      // Create a debate
      const setupPage = await browser.newPage();
      const homePage = new HomePage(setupPage);
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(setupPage);
      await createDialog.fillDebateName('Race Condition Test');
      await createDialog.fillTopic('Testing concurrent updates');
      await createDialog.submit();
      await setupPage.close();
      
      // Open debate in multiple tabs
      const pages: Page[] = [];
      for (let i = 0; i < 3; i++) {
        const page = await browser.newPage();
        const hp = new HomePage(page);
        await hp.navigate();
        await hp.clickDebateCard('Race Condition Test');
        pages.push(page);
      }
      
      // Try to start debate from multiple pages simultaneously
      const startPromises = pages.map(async (page, index) => {
        const debateView = new DebateViewPage(page);
        await debateView.isLoaded();
        
        try {
          await debateView.startDebate();
          return { pageIndex: index, started: true };
        } catch (error) {
          return { pageIndex: index, started: false, error: (error as Error).message };
        }
      });
      
      const results = await Promise.all(startPromises);
      
      // Only one should succeed in starting
      const successCount = results.filter(r => r.started).length;
      expect(successCount).toBe(1);
      
      // Verify debate status is consistent across all pages
      await pages[0].waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
      const statuses = await Promise.all(
        pages.map(async page => {
          const view = new DebateViewPage(page);
          return await view.getDebateStatus();
        })
      );
      
      // All should show same status
      const firstStatus = statuses[0];
      statuses.forEach(status => {
        expect(status).toBe(firstStatus);
      });
      
      // Cleanup
      await Promise.all(pages.map(p => p.close()));
    }, 60000);

    test('should queue turn requests appropriately', async () => {
      const browser = await puppeteer.launch(config.PUPPETEER_OPTIONS);
      browsers.push(browser);
      const page = await browser.newPage();
      
      // Create and start a debate
      const homePage = new HomePage(page);
      await homePage.navigate();
      await homePage.clickCreateDebate();
      
      const createDialog = new CreateDebateDialog(page);
      await createDialog.fillDebateName('Turn Queue Test');
      await createDialog.fillTopic('Testing turn queueing');
      await createDialog.setMaxRounds(5);
      await createDialog.submit();
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      await homePage.clickDebateCard('Turn Queue Test');
      
      const debateView = new DebateViewPage(page);
      await debateView.isLoaded();
      await debateView.startDebate();
      
      // Make multiple skip turn requests
      const skipPromises = [];
      for (let i = 0; i < 3; i++) {
        skipPromises.push(
          debateView.skipTurn().catch(e => ({ error: e.message }))
        );
      }
      
      await Promise.all(skipPromises);
      
      // Wait for turns to process
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 5000)));
      
      // Check turn progression is sequential
      const turns = await debateView.getTurns();
      for (let i = 1; i < turns.length; i++) {
        expect(turns[i].turn).toBe(turns[i - 1].turn + 1);
      }
      
      await page.close();
    }, 45000);
  });
});