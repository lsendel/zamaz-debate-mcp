import puppeteer, { Browser, Page } from 'puppeteer';
import { config } from '../config';

describe('UI Style and Navigation Check', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false, // Show browser for visual inspection
      slowMo: 100, // Slow down for better observation
      defaultViewport: { width: 1280, height: 720 },
      devtools: false
    });
  });

  afterAll(async () => {
    if (browser) {
      await browser.close();
    }
  });

  beforeEach(async () => {
    page = await browser.newPage();
  });

  afterEach(async () => {
    if (page) {
      await page.close();
    }
  });

  describe('Homepage UI and Styling', () => {
    test('should load homepage with professional styling', async () => {
      console.log('Navigating to UI...');
      await page.goto('http://localhost:3001');
      
      // Wait for page to load
      await page.waitForSelector('h1', { timeout: 10000 });
      
      // Check title
      const title = await page.$eval('h1', el => el.textContent);
      console.log('Page title:', title);
      expect(title).toContain('AI Debate System');
      
      // Take screenshot for visual inspection
      await page.screenshot({ 
        path: './screenshots/homepage.png',
        fullPage: true 
      });
      
      console.log('Screenshot saved to ./screenshots/homepage.png');
    }, 30000);

    test('should display navigation elements', async () => {
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      // Check for main navigation elements
      const elements = await page.evaluate(() => {
        return {
          hasCreateButton: !!document.querySelector('button:contains("Create"), button:contains("New")'),
          hasSearchInput: !!document.querySelector('input[placeholder*="search"], input[type="search"]'),
          hasOrgSwitcher: !!document.querySelector('[data-testid="organization-switcher"]'),
          hasStats: !!document.querySelector('[data-testid*="stats"]'),
          hasDebateCards: !!document.querySelector('[data-testid="debate-card"]')
        };
      });
      
      console.log('Navigation elements found:', elements);
      
      // Check for main UI components
      const buttons = await page.$$('button');
      console.log(`Found ${buttons.length} buttons`);
      
      const inputs = await page.$$('input');
      console.log(`Found ${inputs.length} input fields`);
    }, 15000);

    test('should test responsive design on different screen sizes', async () => {
      // Desktop view
      await page.setViewport({ width: 1920, height: 1080 });
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      await page.screenshot({ path: './screenshots/desktop-view.png' });
      
      // Tablet view
      await page.setViewport({ width: 768, height: 1024 });
      await page.reload();
      await page.waitForSelector('h1');
      await page.screenshot({ path: './screenshots/tablet-view.png' });
      
      // Mobile view
      await page.setViewport({ width: 375, height: 667 });
      await page.reload();
      await page.waitForSelector('h1');
      await page.screenshot({ path: './screenshots/mobile-view.png' });
      
      console.log('Responsive design screenshots saved');
    }, 20000);

    test('should check create debate dialog', async () => {
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      // Look for create debate button with different possible text variations
      const createButton = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        return buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('create') ||
          btn.textContent?.toLowerCase().includes('new') ||
          btn.textContent?.toLowerCase().includes('debate')
        );
      });
      
      if (createButton) {
        console.log('Found create debate button');
        await page.evaluate(() => {
          const buttons = Array.from(document.querySelectorAll('button'));
          const createBtn = buttons.find(btn => 
            btn.textContent?.toLowerCase().includes('create') ||
            btn.textContent?.toLowerCase().includes('new') ||
            btn.textContent?.toLowerCase().includes('debate')
          );
          if (createBtn) createBtn.click();
        });
        
        // Wait for dialog to appear
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Take screenshot of dialog
        await page.screenshot({ path: './screenshots/create-debate-dialog.png' });
        console.log('Create debate dialog screenshot saved');
      } else {
        console.log('Create debate button not found, checking page structure...');
        
        // Log page structure for debugging
        const pageText = await page.evaluate(() => document.body.innerText);
        console.log('Page content:', pageText.substring(0, 500));
        
        const allButtons = await page.evaluate(() => 
          Array.from(document.querySelectorAll('button')).map(btn => btn.textContent)
        );
        console.log('Available buttons:', allButtons);
      }
    }, 15000);

    test('should check organization switcher functionality', async () => {
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      // Look for organization switcher
      const orgSwitcher = await page.$('[data-testid="organization-switcher"]');
      
      if (orgSwitcher) {
        console.log('Found organization switcher');
        await orgSwitcher.click();
        
        // Wait for dropdown/menu
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        await page.screenshot({ path: './screenshots/org-switcher.png' });
        console.log('Organization switcher screenshot saved');
      } else {
        console.log('Organization switcher not found, checking for alternative selectors...');
        
        // Check for any elements that might be the org switcher
        const possibleSwitchers = await page.evaluate(() => {
          const elements = Array.from(document.querySelectorAll('*'));
          return elements.filter(el => 
            el.textContent?.toLowerCase().includes('organization') ||
            el.textContent?.toLowerCase().includes('company') ||
            el.className?.includes('org') ||
            el.id?.includes('org')
          ).map(el => ({
            tag: el.tagName,
            text: el.textContent?.substring(0, 50),
            className: el.className,
            id: el.id
          }));
        });
        
        console.log('Possible organization elements:', possibleSwitchers);
      }
    }, 15000);
  });

  describe('Navigation and Usability Issues', () => {
    test('should identify navigation difficulties', async () => {
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      // Analyze page structure for navigation issues
      const navAnalysis = await page.evaluate(() => {
        const analysis = {
          totalButtons: document.querySelectorAll('button').length,
          totalLinks: document.querySelectorAll('a').length,
          totalInputs: document.querySelectorAll('input').length,
          hasMainNav: !!document.querySelector('nav'),
          hasBreadcrumbs: !!document.querySelector('.breadcrumb, [data-testid*="breadcrumb"]'),
          hasSearchBar: !!document.querySelector('input[type="search"], input[placeholder*="search"]'),
          hasFilters: !!document.querySelector('.filter, [data-testid*="filter"]'),
          buttonTexts: Array.from(document.querySelectorAll('button')).map(btn => btn.textContent?.trim()).filter(Boolean),
          headings: Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6')).map(h => ({
            level: h.tagName,
            text: h.textContent?.trim()
          }))
        };
        
        return analysis;
      });
      
      console.log('Navigation Analysis:', JSON.stringify(navAnalysis, null, 2));
      
      // Identify potential usability issues
      const issues = [];
      
      if (navAnalysis.totalButtons === 0) {
        issues.push('No buttons found - may be difficult to interact');
      }
      
      if (!navAnalysis.hasMainNav) {
        issues.push('No main navigation found - users may get lost');
      }
      
      if (!navAnalysis.hasSearchBar && navAnalysis.totalButtons > 5) {
        issues.push('No search functionality - may be hard to find specific content');
      }
      
      if (navAnalysis.buttonTexts.length > 0) {
        const unclearButtons = navAnalysis.buttonTexts.filter(text => 
          text && text.length < 3 || !text?.match(/[a-zA-Z]/)
        );
        if (unclearButtons.length > 0) {
          issues.push(`Unclear button labels: ${unclearButtons.join(', ')}`);
        }
      }
      
      console.log('Potential Navigation Issues:', issues);
      
      // Take a screenshot with annotations
      await page.screenshot({ path: './screenshots/navigation-analysis.png' });
    }, 15000);

    test('should test keyboard navigation', async () => {
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      console.log('Testing keyboard navigation...');
      
      // Test tab navigation
      let focusedElements = [];
      
      for (let i = 0; i < 10; i++) {
        await page.keyboard.press('Tab');
        const focused = await page.evaluate(() => {
          const el = document.activeElement;
          return {
            tag: el?.tagName,
            text: el?.textContent?.substring(0, 30),
            className: el?.className,
            id: el?.id
          };
        });
        focusedElements.push(focused);
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 200)));
      }
      
      console.log('Tab navigation sequence:', focusedElements);
      
      // Check if important elements are focusable
      const focusableElements = await page.evaluate(() => {
        const focusable = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
        return Array.from(document.querySelectorAll(focusable)).length;
      });
      
      console.log(`Found ${focusableElements} focusable elements`);
    }, 15000);
  });
});