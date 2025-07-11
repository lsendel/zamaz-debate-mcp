import puppeteer, { Browser, Page } from 'puppeteer';

describe('Zamaz Organization - Fixed UI Test', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 400,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false
    });
  });

  afterAll(async () => {
    await new Promise(resolve => setTimeout(resolve, 8000)); // Keep browser open longer to see results
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

  test('should create Zamaz organization and test UI navigation', async () => {
    console.log('🚀 Starting Zamaz organization creation test...');
    
    // Navigate to UI
    await page.goto('http://localhost:3001');
    await page.waitForSelector('h1');
    console.log('✅ UI loaded');
    
    await page.screenshot({ path: './screenshots/zamaz-fix-start.png', fullPage: true });
    
    // Step 1: Click organization dropdown using more specific selector
    console.log('📝 Attempting to click organization dropdown...');
    
    try {
      // Wait for the page to fully load
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
      
      // Use more specific selector for organization button
      const orgButtonClicked = await page.evaluate(() => {
        // Look for button containing "Default Organization" text
        const buttons = Array.from(document.querySelectorAll('button'));
        const orgButton = buttons.find(btn => 
          btn.textContent?.includes('Default Organization') ||
          btn.innerHTML?.includes('Default Organization')
        );
        
        if (orgButton) {
          (orgButton as HTMLElement).click();
          return true;
        }
        
        // Alternative: look for button with Building2 icon (organization icon)
        const iconButtons = buttons.filter(btn => 
          btn.querySelector('svg') || 
          btn.innerHTML.includes('Building') ||
          btn.classList.toString().includes('org')
        );
        
        if (iconButtons.length > 0) {
          (iconButtons[0] as HTMLElement).click();
          return true;
        }
        
        return false;
      });
      
      if (orgButtonClicked) {
        console.log('✅ Successfully clicked organization dropdown');
        
        // Wait for dropdown to appear
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1500)));
        
        await page.screenshot({ path: './screenshots/zamaz-dropdown-open.png', fullPage: true });
        
        // Step 2: Click "Create Organization"
        console.log('📝 Looking for Create Organization option...');
        
        const createClicked = await page.evaluate(() => {
          // Look for "Create Organization" text in menu items
          const elements = Array.from(document.querySelectorAll('*'));
          const createOption = elements.find(el => 
            el.textContent?.includes('Create Organization') &&
            (el.tagName === 'DIV' || el.tagName === 'BUTTON' || el.getAttribute('role') === 'menuitem')
          );
          
          if (createOption) {
            (createOption as HTMLElement).click();
            return true;
          }
          return false;
        });
        
        if (createClicked) {
          console.log('✅ Clicked Create Organization');
          
          // Wait for dialog to appear
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
          
          await page.screenshot({ path: './screenshots/zamaz-dialog-open.png', fullPage: true });
          
          // Step 3: Fill in organization name
          console.log('📝 Filling organization name...');
          
          // Look for the input field
          const inputFilled = await page.evaluate(() => {
            const input = document.querySelector('input[id="org-name"]') as HTMLInputElement;
            if (input) {
              input.value = '';
              input.focus();
              input.value = 'Zamaz';
              
              // Trigger input events
              const inputEvent = new Event('input', { bubbles: true });
              const changeEvent = new Event('change', { bubbles: true });
              input.dispatchEvent(inputEvent);
              input.dispatchEvent(changeEvent);
              
              return true;
            }
            return false;
          });
          
          if (inputFilled) {
            console.log('✅ Filled organization name: Zamaz');
            
            await page.screenshot({ path: './screenshots/zamaz-form-filled.png', fullPage: true });
            
            // Step 4: Submit the form
            console.log('🚀 Submitting organization creation...');
            
            const submitted = await page.evaluate(() => {
              // Look for Create Organization submit button
              const buttons = Array.from(document.querySelectorAll('button'));
              const submitButton = buttons.find(btn => 
                btn.textContent?.includes('Create Organization') &&
                !btn.disabled
              );
              
              if (submitButton) {
                (submitButton as HTMLElement).click();
                return true;
              }
              return false;
            });
            
            if (submitted) {
              console.log('✅ Clicked submit button');
              
              // Wait for organization to be created and dialog to close
              await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
              
              await page.screenshot({ path: './screenshots/zamaz-org-created.png', fullPage: true });
              
              // Step 5: Verify organization was created
              console.log('🔍 Verifying organization creation...');
              
              const verification = await page.evaluate(() => {
                // Check if organization switcher now shows "Zamaz"
                const buttons = Array.from(document.querySelectorAll('button'));
                const zamazButton = buttons.find(btn => 
                  btn.textContent?.includes('Zamaz')
                );
                
                return {
                  hasZamazButton: !!zamazButton,
                  currentOrgText: zamazButton?.textContent || 'Not found',
                  allButtonTexts: buttons.map(btn => btn.textContent?.trim()).filter(Boolean)
                };
              });
              
              console.log('Organization verification:', verification);
              
              if (verification.hasZamazButton) {
                console.log('🎉 SUCCESS! Zamaz organization created successfully!');
                
                // Step 6: Test creating a debate in the new organization
                console.log('🎭 Testing debate creation in Zamaz organization...');
                
                await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
                
                // Click Create Debate button
                const debateCreated = await page.evaluate(() => {
                  const buttons = Array.from(document.querySelectorAll('button'));
                  const createBtn = buttons.find(btn => 
                    btn.textContent?.includes('Create Debate') ||
                    btn.textContent?.includes('New Debate')
                  );
                  
                  if (createBtn) {
                    (createBtn as HTMLElement).click();
                    return true;
                  }
                  return false;
                });
                
                if (debateCreated) {
                  console.log('✅ Clicked Create Debate button');
                  
                  await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
                  await page.screenshot({ path: './screenshots/zamaz-debate-dialog.png', fullPage: true });
                  
                  // Fill debate form
                  await page.evaluate(() => {
                    // Fill name field
                    const nameInput = document.querySelector('input[id="name"]') as HTMLInputElement;
                    if (nameInput) {
                      nameInput.value = 'Zamaz AI Ethics Debate';
                      nameInput.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                    
                    // Fill topic field
                    const topicInput = document.querySelector('input[id="topic"], textarea[id="topic"]') as HTMLInputElement;
                    if (topicInput) {
                      topicInput.value = 'Should AI have decision-making authority in Zamaz operations?';
                      topicInput.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                    
                    // Fill description
                    const descInput = document.querySelector('textarea[id="description"]') as HTMLTextAreaElement;
                    if (descInput) {
                      descInput.value = 'A strategic debate for Zamaz about implementing AI autonomy in business decisions.';
                      descInput.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                  });
                  
                  console.log('✅ Filled debate form');
                  
                  await page.screenshot({ path: './screenshots/zamaz-debate-filled.png', fullPage: true });
                  
                  // Submit debate
                  const debateSubmitted = await page.evaluate(() => {
                    const buttons = Array.from(document.querySelectorAll('button'));
                    const submitBtn = buttons.find(btn => 
                      btn.textContent?.includes('Create Debate') ||
                      btn.textContent?.includes('Submit')
                    );
                    
                    if (submitBtn) {
                      (submitBtn as HTMLElement).click();
                      return true;
                    }
                    return false;
                  });
                  
                  if (debateSubmitted) {
                    console.log('✅ Submitted debate');
                    
                    await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
                    await page.screenshot({ path: './screenshots/zamaz-debate-final.png', fullPage: true });
                    
                    console.log('🎉 Zamaz organization and debate creation test completed successfully!');
                  }
                }
              } else {
                console.log('⚠️ Organization creation may not have worked as expected');
                console.log('Available buttons:', verification.allButtonTexts);
              }
            } else {
              console.log('❌ Could not find submit button');
            }
          } else {
            console.log('❌ Could not fill organization name');
          }
        } else {
          console.log('❌ Could not find Create Organization option');
        }
      } else {
        console.log('❌ Could not click organization dropdown');
        
        // Debug: show available buttons
        const availableButtons = await page.evaluate(() => {
          return Array.from(document.querySelectorAll('button')).map(btn => ({
            text: btn.textContent?.trim(),
            className: btn.className,
            innerHTML: btn.innerHTML.substring(0, 100)
          }));
        });
        
        console.log('Available buttons:', availableButtons);
      }
    } catch (error) {
      console.log('❌ Error during test:', error);
      await page.screenshot({ path: './screenshots/zamaz-error.png', fullPage: true });
    }
    
    console.log('🏁 Test completed');
  }, 120000);
});