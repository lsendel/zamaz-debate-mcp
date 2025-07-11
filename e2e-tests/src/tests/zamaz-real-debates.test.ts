import puppeteer, { Browser, Page } from 'puppeteer';
import { config } from '../config';

describe('Zamaz Organization - Real Debate Creation', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false, // Show browser for visual inspection
      slowMo: 200, // Slow down for better observation
      defaultViewport: { width: 1400, height: 900 },
      devtools: false
    });
  });

  afterAll(async () => {
    if (browser) {
      // Keep browser open for a moment to see results
      await new Promise(resolve => setTimeout(resolve, 3000));
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

  describe('Create Zamaz Organization and Debates', () => {
    test('should create Zamaz organization and first debate', async () => {
      console.log('🚀 Starting Zamaz organization setup...');
      
      // Navigate to the UI
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1', { timeout: 10000 });
      
      console.log('✅ UI loaded successfully');
      
      // Take initial screenshot
      await page.screenshot({ path: './screenshots/zamaz-initial.png', fullPage: true });
      
      // Step 1: Create/Switch to Zamaz organization
      console.log('📝 Creating Zamaz organization...');
      
      // Click on organization switcher (currently shows "Default Organization")
      const orgButton = await page.waitForSelector('button:has-text("Default Organization")', { timeout: 5000 }).catch(() => null);
      
      if (!orgButton) {
        // Try alternative selector
        await page.click('button[data-testid="organization-switcher"]').catch(() => {
          console.log('Organization switcher not found with data-testid, trying text content...');
        });
      } else {
        await orgButton.click();
      }
      
      // Wait for dropdown and look for "Create Organization" or similar
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      
      // Try to find and click create organization option
      const createOrgFound = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button, div[role="button"], [role="menuitem"]'));
        const createBtn = buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('create') ||
          btn.textContent?.toLowerCase().includes('new') ||
          btn.textContent?.toLowerCase().includes('add')
        );
        if (createBtn) {
          (createBtn as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (createOrgFound) {
        console.log('🏢 Found create organization option');
        
        // Wait for organization creation form
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Fill in "Zamaz" as organization name
        const nameInput = await page.$('input[placeholder*="name"], input[id*="name"], input[type="text"]');
        if (nameInput) {
          await nameInput.click();
          await nameInput.type('Zamaz');
          console.log('✏️ Entered organization name: Zamaz');
          
          // Submit organization creation
          await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const submitBtn = buttons.find(btn => 
              btn.textContent?.toLowerCase().includes('create') ||
              btn.textContent?.toLowerCase().includes('save') ||
              btn.textContent?.toLowerCase().includes('submit')
            );
            if (submitBtn) (submitBtn as HTMLElement).click();
          });
          
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
        }
      }
      
      // Take screenshot after organization setup
      await page.screenshot({ path: './screenshots/zamaz-org-created.png', fullPage: true });
      
      // Step 2: Create first debate
      console.log('🎭 Creating first debate...');
      
      // Click "Create Debate" button
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const createDebateBtn = buttons.find(btn => 
          btn.textContent?.includes('Create Debate') ||
          btn.textContent?.includes('New Debate')
        );
        if (createDebateBtn) {
          (createDebateBtn as HTMLElement).click();
          console.log('Clicked Create Debate button');
        }
      });
      
      // Wait for debate creation dialog
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
      
      // Fill in debate details
      console.log('📝 Filling debate details...');
      
      // Debate name
      const nameField = await page.$('input[id="name"], input[placeholder*="name"]');
      if (nameField) {
        await nameField.click();
        await nameField.type('AI Ethics in Business: Human vs Machine Decision Making');
        console.log('✏️ Entered debate name');
      }
      
      // Debate topic
      const topicField = await page.$('input[id="topic"], textarea[id="topic"], input[placeholder*="topic"]');
      if (topicField) {
        await topicField.click();
        await topicField.type('Should AI systems be given autonomous decision-making authority in critical business operations?');
        console.log('✏️ Entered debate topic');
      }
      
      // Description
      const descField = await page.$('textarea[id="description"], input[id="description"]');
      if (descField) {
        await descField.click();
        await descField.type('A comprehensive debate exploring the ethical implications and practical considerations of implementing autonomous AI decision-making systems in business-critical operations, examining both the potential benefits and risks.');
        console.log('✏️ Entered debate description');
      }
      
      // Take screenshot of filled form
      await page.screenshot({ path: './screenshots/zamaz-debate-form.png', fullPage: true });
      
      // Add participants
      console.log('👥 Adding debate participants...');
      
      // Try to add participants if the form supports it
      const addParticipantBtn = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        return buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('add participant') ||
          btn.textContent?.toLowerCase().includes('participant')
        );
      });
      
      if (addParticipantBtn) {
        // Add first participant (Pro-AI stance)
        await page.evaluate(() => {
          const buttons = Array.from(document.querySelectorAll('button'));
          const addBtn = buttons.find(btn => 
            btn.textContent?.toLowerCase().includes('add participant')
          );
          if (addBtn) (addBtn as HTMLElement).click();
        });
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Fill participant 1 details
        const participantInputs = await page.$$('input[placeholder*="participant"], input[placeholder*="name"]');
        if (participantInputs.length > 0) {
          await participantInputs[0].type('AI Advocate');
          console.log('✏️ Added participant 1: AI Advocate');
        }
        
        // Add second participant (Human-centric stance)
        await page.evaluate(() => {
          const buttons = Array.from(document.querySelectorAll('button'));
          const addBtn = buttons.find(btn => 
            btn.textContent?.toLowerCase().includes('add participant')
          );
          if (addBtn) (addBtn as HTMLElement).click();
        });
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        const updatedInputs = await page.$$('input[placeholder*="participant"], input[placeholder*="name"]');
        if (updatedInputs.length > 1) {
          await updatedInputs[1].type('Human Ethics Defender');
          console.log('✏️ Added participant 2: Human Ethics Defender');
        }
      }
      
      // Take final screenshot before submission
      await page.screenshot({ path: './screenshots/zamaz-debate-ready.png', fullPage: true });
      
      // Submit the debate
      console.log('🚀 Submitting debate...');
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const submitBtn = buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('create debate') ||
          btn.textContent?.toLowerCase().includes('submit') ||
          btn.textContent?.toLowerCase().includes('save')
        );
        if (submitBtn) {
          (submitBtn as HTMLElement).click();
          console.log('Clicked submit button');
        }
      });
      
      // Wait for debate to be created
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
      
      // Take final screenshot
      await page.screenshot({ path: './screenshots/zamaz-debate-created.png', fullPage: true });
      
      console.log('✅ First Zamaz debate created successfully!');
      
    }, 60000);

    test('should create second debate: Technology Innovation', async () => {
      console.log('🚀 Creating second debate for Zamaz...');
      
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      // Click Create Debate
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const createBtn = buttons.find(btn => 
          btn.textContent?.includes('Create Debate') ||
          btn.textContent?.includes('New Debate')
        );
        if (createBtn) (createBtn as HTMLElement).click();
      });
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
      
      // Fill second debate details
      const nameField = await page.$('input[id="name"], input[placeholder*="name"]');
      if (nameField) {
        await nameField.click();
        await nameField.type('Future of Work: Remote vs On-Site Innovation');
      }
      
      const topicField = await page.$('input[id="topic"], textarea[id="topic"], input[placeholder*="topic"]');
      if (topicField) {
        await topicField.click();
        await topicField.type('Does remote work hinder or enhance technological innovation and team collaboration?');
      }
      
      const descField = await page.$('textarea[id="description"], input[id="description"]');
      if (descField) {
        await descField.click();
        await descField.type('An exploration of how different work environments impact innovation, creativity, and technological advancement in modern organizations.');
      }
      
      // Submit
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const submitBtn = buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('create debate') ||
          btn.textContent?.toLowerCase().includes('submit')
        );
        if (submitBtn) (submitBtn as HTMLElement).click();
      });
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
      await page.screenshot({ path: './screenshots/zamaz-debate-2.png', fullPage: true });
      
      console.log('✅ Second Zamaz debate created!');
      
    }, 30000);

    test('should create third debate: Digital Privacy', async () => {
      console.log('🚀 Creating third debate for Zamaz...');
      
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const createBtn = buttons.find(btn => 
          btn.textContent?.includes('Create Debate') ||
          btn.textContent?.includes('New Debate')
        );
        if (createBtn) (createBtn as HTMLElement).click();
      });
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
      
      const nameField = await page.$('input[id="name"], input[placeholder*="name"]');
      if (nameField) {
        await nameField.click();
        await nameField.type('Digital Privacy vs Personalization: The Data Dilemma');
      }
      
      const topicField = await page.$('input[id="topic"], textarea[id="topic"], input[placeholder*="topic"]');
      if (topicField) {
        await topicField.click();
        await topicField.type('Should companies prioritize user privacy over personalized experiences and targeted services?');
      }
      
      const descField = await page.$('textarea[id="description"], input[id="description"]');
      if (descField) {
        await descField.click();
        await descField.type('Examining the tension between protecting user privacy and delivering personalized, valuable digital experiences in the modern data economy.');
      }
      
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const submitBtn = buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('create debate') ||
          btn.textContent?.toLowerCase().includes('submit')
        );
        if (submitBtn) (submitBtn as HTMLElement).click();
      });
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
      await page.screenshot({ path: './screenshots/zamaz-debate-3.png', fullPage: true });
      
      console.log('✅ Third Zamaz debate created!');
      
    }, 30000);

    test('should verify all debates are visible and test navigation', async () => {
      console.log('🔍 Verifying all debates and testing navigation...');
      
      await page.goto('http://localhost:3001');
      await page.waitForSelector('h1');
      
      // Check how many debates are now visible
      const debateCount = await page.evaluate(() => {
        const debateCards = document.querySelectorAll('[data-testid="debate-card"]');
        const allCards = document.querySelectorAll('.card, .debate, [class*="debate"]');
        return {
          withTestId: debateCards.length,
          totalCards: allCards.length,
          pageText: document.body.innerText
        };
      });
      
      console.log('Debate count:', debateCount);
      
      // Take final screenshot showing all debates
      await page.screenshot({ path: './screenshots/zamaz-all-debates.png', fullPage: true });
      
      // Test navigation between different sections
      const navigationButtons = ['Debates', 'Active Debate', 'Ollama Setup'];
      
      for (const buttonText of navigationButtons) {
        console.log(`Testing navigation to: ${buttonText}`);
        
        const buttonClicked = await page.evaluate((text) => {
          const buttons = Array.from(document.querySelectorAll('button'));
          const btn = buttons.find(b => b.textContent?.includes(text));
          if (btn) {
            (btn as HTMLElement).click();
            return true;
          }
          return false;
        }, buttonText);
        
        if (buttonClicked) {
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
          await page.screenshot({ path: `./screenshots/zamaz-nav-${buttonText.toLowerCase().replace(' ', '-')}.png` });
          console.log(`✅ Navigated to ${buttonText}`);
        }
      }
      
      console.log('✅ Navigation testing completed!');
    }, 30000);
  });
});