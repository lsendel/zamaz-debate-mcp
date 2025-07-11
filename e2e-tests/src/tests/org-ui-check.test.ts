import puppeteer, { Browser, Page } from 'puppeteer';

describe('Organization UI Issue Check', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 500,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false
    });
  });

  afterAll(async () => {
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

  test('should test organization creation UI flow', async () => {
    console.log('🔍 Testing organization creation UI...');
    
    // Navigate to UI
    await page.goto('http://localhost:3001');
    await page.waitForSelector('h1');
    
    console.log('✅ UI loaded');
    await page.screenshot({ path: './screenshots/org-test-start.png', fullPage: true });
    
    // Click on organization dropdown
    console.log('📝 Clicking organization dropdown...');
    
    try {
      // Wait for the organization switcher button to be visible
      await page.waitForSelector('button:has-text("Default Organization")', { timeout: 5000 });
      
      // Click the organization dropdown
      await page.click('button:has-text("Default Organization")');
      console.log('✅ Clicked organization dropdown');
      
      // Wait for dropdown menu to appear
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
      
      // Take screenshot of dropdown
      await page.screenshot({ path: './screenshots/org-dropdown-open.png', fullPage: true });
      
      // Look for "Create Organization" option and click it
      const createOrgFound = await page.evaluate(() => {
        const menuItems = Array.from(document.querySelectorAll('[role="menuitem"], button, div[role="button"]'));
        const createOption = menuItems.find(item => 
          item.textContent?.includes('Create Organization')
        );
        
        if (createOption) {
          (createOption as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (createOrgFound) {
        console.log('✅ Found and clicked Create Organization');
        
        // Wait for dialog to appear
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1500)));
        
        // Take screenshot of dialog
        await page.screenshot({ path: './screenshots/org-dialog.png', fullPage: true });
        
        // Check if dialog is visible
        const dialogVisible = await page.evaluate(() => {
          const dialog = document.querySelector('[role="dialog"]');
          const title = document.querySelector('h2:contains("Create New Organization"), h1:contains("Create"), h3:contains("Create")');
          const input = document.querySelector('input[id="org-name"], input[placeholder*="name"]');
          
          return {
            hasDialog: !!dialog,
            hasTitle: !!title,
            hasInput: !!input,
            dialogVisible: dialog ? getComputedStyle(dialog).display !== 'none' : false,
            dialogHTML: dialog ? dialog.outerHTML.substring(0, 500) : 'No dialog found'
          };
        });
        
        console.log('Dialog state:', dialogVisible);
        
        if (dialogVisible.hasDialog && dialogVisible.hasInput) {
          console.log('✅ Dialog opened successfully');
          
          // Fill in organization name
          await page.type('input[id="org-name"]', 'Zamaz', {delay: 100});
          console.log('✏️ Entered "Zamaz" as organization name');
          
          // Take screenshot with filled form
          await page.screenshot({ path: './screenshots/org-form-filled.png', fullPage: true });
          
          // Click create button
          const created = await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const createBtn = buttons.find(btn => 
              btn.textContent?.includes('Create Organization')
            );
            
            if (createBtn) {
              (createBtn as HTMLElement).click();
              return true;
            }
            return false;
          });
          
          if (created) {
            console.log('✅ Clicked Create Organization button');
            
            // Wait for dialog to close and organization to be created
            await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
            
            // Take final screenshot
            await page.screenshot({ path: './screenshots/org-created.png', fullPage: true });
            
            // Check if organization was created successfully
            const finalState = await page.evaluate(() => {
              const orgButton = document.querySelector('button:has-text("Zamaz"), button:contains("Zamaz")');
              const buttonText = Array.from(document.querySelectorAll('button')).map(btn => btn.textContent?.trim()).filter(Boolean);
              
              return {
                hasZamazButton: !!orgButton,
                buttonTexts: buttonText,
                currentOrgText: orgButton?.textContent || 'Not found'
              };
            });
            
            console.log('Final organization state:', finalState);
            
            if (finalState.hasZamazButton) {
              console.log('🎉 Success! Zamaz organization created and active');
            } else {
              console.log('⚠️ Organization creation may have failed or not reflected in UI');
            }
          }
        } else {
          console.log('❌ Dialog not properly displayed');
          console.log('Dialog details:', dialogVisible);
        }
      } else {
        console.log('❌ Create Organization option not found in dropdown');
        
        // Debug: Show what's in the dropdown
        const dropdownContent = await page.evaluate(() => {
          const menuItems = Array.from(document.querySelectorAll('[role="menuitem"], button, div'));
          return menuItems.map(item => ({
            tag: item.tagName,
            text: item.textContent?.substring(0, 50),
            className: item.className,
            role: item.getAttribute('role')
          })).filter(item => item.text && item.text.trim().length > 0);
        });
        
        console.log('Dropdown content:', dropdownContent);
      }
    } catch (error) {
      console.log('❌ Error during organization test:', error);
      await page.screenshot({ path: './screenshots/org-error.png', fullPage: true });
    }
    
    console.log('🏁 Organization UI test completed');
    
  }, 60000);
});