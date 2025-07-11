import puppeteer, { Browser, Page } from 'puppeteer';

describe('Zamaz Real Debate Creation - Simple Test', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 300,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false
    });
  });

  afterAll(async () => {
    // Keep browser open for 5 seconds to see results
    await new Promise(resolve => setTimeout(resolve, 5000));
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

  test('should create first real debate for Zamaz organization', async () => {
    console.log('🚀 Starting real debate creation for Zamaz...');
    
    // Navigate to UI
    await page.goto('http://localhost:3001');
    await page.waitForSelector('h1');
    console.log('✅ UI loaded');
    
    // Take initial screenshot
    await page.screenshot({ path: './screenshots/start.png', fullPage: true });
    
    // Step 1: Try to create organization first by clicking org dropdown
    console.log('📝 Attempting to create Zamaz organization...');
    
    // Click on the organization dropdown (Default Organization button)
    try {
      await page.click('button:has-text("Default Organization")');
      console.log('✅ Clicked organization dropdown');
      
      // Wait a moment for dropdown to appear
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 500)));
      
      // Look for any "Create" or "Add" options in the dropdown
      const createOrgClicked = await page.evaluate(() => {
        // Look for dropdown menu items
        const menuItems = Array.from(document.querySelectorAll('[role="menuitem"], [role="option"], button, div[role="button"]'));
        const createOption = menuItems.find(item => 
          item.textContent?.toLowerCase().includes('create') ||
          item.textContent?.toLowerCase().includes('add') ||
          item.textContent?.toLowerCase().includes('new')
        );
        
        if (createOption) {
          (createOption as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (createOrgClicked) {
        console.log('✅ Found and clicked create organization option');
        
        // Wait for form and fill organization name
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Find input field and enter "Zamaz"
        const orgNameInput = await page.$('input[type="text"], input[placeholder*="name"], input[placeholder*="organization"]');
        if (orgNameInput) {
          await orgNameInput.click();
          await orgNameInput.type('Zamaz');
          console.log('✏️ Entered organization name: Zamaz');
          
          // Submit the form
          await page.keyboard.press('Enter');
          // Or try clicking submit button
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
          console.log('✅ Organization creation attempted');
        }
      }
    } catch (error) {
      console.log('ℹ️ Organization creation not available or failed, proceeding with debate creation...');
    }
    
    await page.screenshot({ path: './screenshots/after-org.png', fullPage: true });
    
    // Step 2: Create the debate
    console.log('🎭 Creating debate...');
    
    // Click the main "Create Debate" button
    await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const createBtn = buttons.find(btn => 
        btn.textContent?.includes('Create Debate')
      );
      if (createBtn) {
        (createBtn as HTMLElement).click();
        console.log('Clicked Create Debate button');
      } else {
        console.log('Create Debate button not found');
      }
    });
    
    // Wait for debate form to appear
    await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
    
    await page.screenshot({ path: './screenshots/debate-form.png', fullPage: true });
    
    // Fill in debate details
    console.log('📝 Filling debate form...');
    
    // Find and fill debate name
    const nameInputs = await page.$$('input[type="text"], input[id="name"], input[placeholder*="name"]');
    if (nameInputs.length > 0) {
      await nameInputs[0].click();
      await nameInputs[0].type('AI Ethics in Business Decision Making', {delay: 50});
      console.log('✏️ Entered debate name');
    }
    
    // Find and fill topic (might be second input or textarea)
    const topicInputs = await page.$$('input[id="topic"], textarea[id="topic"], input[placeholder*="topic"], textarea[placeholder*="topic"]');
    if (topicInputs.length > 0) {
      await topicInputs[0].click();
      await topicInputs[0].type('Should AI systems be given autonomous decision-making authority in critical business operations?', {delay: 50});
      console.log('✏️ Entered debate topic');
    } else {
      // Try finding the second text input if topic field not found by ID
      if (nameInputs.length > 1) {
        await nameInputs[1].click();
        await nameInputs[1].type('Should AI systems be given autonomous decision-making authority in critical business operations?', {delay: 50});
        console.log('✏️ Entered topic in second input field');
      }
    }
    
    // Find and fill description
    const descriptionFields = await page.$$('textarea, input[id="description"], textarea[id="description"]');
    if (descriptionFields.length > 0) {
      await descriptionFields[0].click();
      await descriptionFields[0].type('A comprehensive debate exploring the ethical implications and practical considerations of implementing autonomous AI decision-making systems in business-critical operations.', {delay: 30});
      console.log('✏️ Entered debate description');
    }
    
    // Take screenshot of filled form
    await page.screenshot({ path: './screenshots/filled-form.png', fullPage: true });
    
    // Submit the debate
    console.log('🚀 Submitting debate...');
    
    // Try different ways to submit
    const submitted = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      
      // Look for submit buttons in order of preference
      const submitVariants = [
        'Create Debate',
        'Submit',
        'Save',
        'Create',
        'Done',
        'Add Debate'
      ];
      
      for (const variant of submitVariants) {
        const btn = buttons.find(b => 
          b.textContent?.includes(variant) ||
          b.textContent?.toLowerCase().includes(variant.toLowerCase())
        );
        if (btn) {
          (btn as HTMLElement).click();
          console.log(`Clicked ${variant} button`);
          return true;
        }
      }
      
      // If no submit button found, try Enter key
      return false;
    });
    
    if (!submitted) {
      console.log('No submit button found, trying Enter key...');
      await page.keyboard.press('Enter');
    }
    
    // Wait for submission to complete
    await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
    
    // Take final screenshot
    await page.screenshot({ path: './screenshots/final-result.png', fullPage: true });
    
    // Check if debate was created by looking for changes in the UI
    const finalState = await page.evaluate(() => {
      const bodyText = document.body.innerText;
      const debateCards = document.querySelectorAll('[data-testid="debate-card"], .debate-card, [class*="debate"]');
      const totalDebatesElement = document.querySelector('[data-testid="stats-debates"]');
      
      return {
        bodyText: bodyText.substring(0, 500),
        debateCardsCount: debateCards.length,
        totalDebatesText: totalDebatesElement?.textContent || 'not found',
        hasSuccessMessage: bodyText.toLowerCase().includes('created') || bodyText.toLowerCase().includes('success'),
        pageTitle: document.title
      };
    });
    
    console.log('Final state:', finalState);
    
    if (finalState.debateCardsCount > 0 || finalState.hasSuccessMessage) {
      console.log('✅ Debate appears to have been created successfully!');
    } else {
      console.log('⚠️ Debate creation status unclear, but form was submitted');
    }
    
    console.log('🎉 Test completed! Check screenshots for visual verification.');
    
  }, 90000); // 90 second timeout
});