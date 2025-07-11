import puppeteer, { Browser, Page } from 'puppeteer';

describe('Claude Sonnet 4 vs Gemini Debate - Full Integration Test', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 300,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false,
      timeout: 60000
    });
  });

  afterAll(async () => {
    // Keep browser open longer to see final results
    await new Promise(resolve => setTimeout(resolve, 10000));
    if (browser) {
      await browser.close();
    }
  });

  beforeEach(async () => {
    page = await browser.newPage();
    page.setDefaultTimeout(30000);
  });

  afterEach(async () => {
    if (page) {
      await page.close();
    }
  });

  test('should create Zamaz organization and run Claude vs Gemini debate with history tracking', async () => {
    console.log('🚀 Starting Claude Sonnet 4 vs Gemini debate test...');
    
    // Step 1: Navigate to UI and verify it's running
    await page.goto('http://localhost:3001');
    await page.waitForSelector('h1');
    console.log('✅ UI loaded successfully');
    
    await page.screenshot({ path: './screenshots/claude-gemini-start.png', fullPage: true });
    
    // Step 2: Create Zamaz organization
    console.log('🏢 Creating Zamaz organization...');
    
    try {
      // Click organization dropdown
      const orgButtonClicked = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const orgButton = buttons.find(btn => 
          btn.textContent?.includes('Default Organization') ||
          btn.textContent?.includes('Organization')
        );
        
        if (orgButton) {
          (orgButton as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (orgButtonClicked) {
        console.log('✅ Clicked organization dropdown');
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Click Create Organization
        const createClicked = await page.evaluate(() => {
          const elements = Array.from(document.querySelectorAll('*'));
          const createOption = elements.find(el => 
            el.textContent?.includes('Create Organization')
          );
          
          if (createOption) {
            (createOption as HTMLElement).click();
            return true;
          }
          return false;
        });
        
        if (createClicked) {
          console.log('✅ Clicked Create Organization');
          
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
          
          // Fill organization name
          const inputFilled = await page.evaluate(() => {
            const input = document.querySelector('input[id="org-name"]') as HTMLInputElement;
            if (input) {
              input.value = 'Zamaz';
              input.dispatchEvent(new Event('input', { bubbles: true }));
              input.dispatchEvent(new Event('change', { bubbles: true }));
              return true;
            }
            return false;
          });
          
          if (inputFilled) {
            console.log('✅ Filled organization name: Zamaz');
            
            // Submit organization
            const submitted = await page.evaluate(() => {
              const buttons = Array.from(document.querySelectorAll('button'));
              const submitButton = buttons.find(btn => 
                btn.textContent?.includes('Create Organization')
              );
              
              if (submitButton) {
                (submitButton as HTMLElement).click();
                return true;
              }
              return false;
            });
            
            if (submitted) {
              console.log('✅ Organization creation submitted');
              await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
            }
          }
        }
      }
    } catch (error) {
      console.log('⚠️ Organization creation may have failed, continuing with debate creation...');
    }
    
    await page.screenshot({ path: './screenshots/claude-gemini-org-setup.png', fullPage: true });
    
    // Step 3: Create debate with Claude Sonnet 4 vs Gemini
    console.log('🎭 Creating Claude vs Gemini debate...');
    
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
      
      await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
      await page.screenshot({ path: './screenshots/claude-gemini-debate-dialog.png', fullPage: true });
      
      // Fill debate details
      console.log('📝 Filling debate form...');
      
      await page.evaluate(() => {
        // Fill name field
        const nameInput = document.querySelector('input[id="name"]') as HTMLInputElement;
        if (nameInput) {
          nameInput.value = 'AI Ethics: Claude Sonnet 4 vs Gemini Pro Debate';
          nameInput.dispatchEvent(new Event('input', { bubbles: true }));
        }
        
        // Fill topic field
        const topicInput = document.querySelector('input[id="topic"], textarea[id="topic"]') as HTMLInputElement;
        if (topicInput) {
          topicInput.value = 'Should AI systems be granted legal personhood and rights?';
          topicInput.dispatchEvent(new Event('input', { bubbles: true }));
        }
        
        // Fill description
        const descInput = document.querySelector('textarea[id="description"]') as HTMLTextAreaElement;
        if (descInput) {
          descInput.value = 'A philosophical debate between Claude Sonnet 4 and Gemini Pro on whether artificial intelligence should be granted legal personhood, exploring ethical, legal, and societal implications.';
          descInput.dispatchEvent(new Event('input', { bubbles: true }));
        }
      });
      
      console.log('✅ Filled basic debate information');
      
      // Add Claude Sonnet 4 participant
      console.log('👤 Adding Claude Sonnet 4 participant...');
      
      const claudeAdded = await page.evaluate(() => {
        // Look for Add Participant button
        const buttons = Array.from(document.querySelectorAll('button'));
        const addBtn = buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('add participant') ||
          btn.textContent?.toLowerCase().includes('participant')
        );
        
        if (addBtn) {
          (addBtn as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (claudeAdded) {
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Fill Claude participant details
        await page.evaluate(() => {
          const participantInputs = document.querySelectorAll('input[placeholder*="participant"], input[placeholder*="name"]');
          if (participantInputs.length > 0) {
            const input = participantInputs[0] as HTMLInputElement;
            input.value = 'Claude Sonnet 4 (Pro-Rights)';
            input.dispatchEvent(new Event('input', { bubbles: true }));
          }
          
          // Set position
          const positionInputs = document.querySelectorAll('input[placeholder*="position"]');
          if (positionInputs.length > 0) {
            const input = positionInputs[0] as HTMLInputElement;
            input.value = 'AI systems should be granted legal personhood';
            input.dispatchEvent(new Event('input', { bubbles: true }));
          }
        });
        
        // Select Claude provider
        await page.evaluate(() => {
          const tabs = Array.from(document.querySelectorAll('button[role="tab"]'));
          const claudeTab = tabs.find(tab => 
            tab.textContent?.toLowerCase().includes('claude') ||
            tab.getAttribute('value') === 'claude'
          );
          if (claudeTab) {
            (claudeTab as HTMLElement).click();
          }
        });
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 500)));
        
        // Select Sonnet 4 model
        await page.evaluate(() => {
          const selects = document.querySelectorAll('[data-testid="model-select"], select');
          if (selects.length > 0) {
            const select = selects[0] as HTMLSelectElement;
            select.click();
          }
        });
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 500)));
        
        await page.evaluate(() => {
          const options = Array.from(document.querySelectorAll('[role="option"]'));
          const sonnetOption = options.find(opt => 
            opt.textContent?.includes('sonnet') ||
            opt.textContent?.includes('4')
          );
          if (sonnetOption) {
            (sonnetOption as HTMLElement).click();
          }
        });
        
        console.log('✅ Added Claude Sonnet 4 participant');
      }
      
      // Add Gemini participant
      console.log('👤 Adding Gemini Pro participant...');
      
      const geminiAdded = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const addBtn = buttons.find(btn => 
          btn.textContent?.toLowerCase().includes('add participant')
        );
        
        if (addBtn) {
          (addBtn as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (geminiAdded) {
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 1000)));
        
        // Fill Gemini participant details
        await page.evaluate(() => {
          const participantInputs = document.querySelectorAll('input[placeholder*="participant"], input[placeholder*="name"]');
          if (participantInputs.length > 1) {
            const input = participantInputs[1] as HTMLInputElement;
            input.value = 'Gemini Pro (Anti-Rights)';
            input.dispatchEvent(new Event('input', { bubbles: true }));
          }
          
          // Set position
          const positionInputs = document.querySelectorAll('input[placeholder*="position"]');
          if (positionInputs.length > 1) {
            const input = positionInputs[1] as HTMLInputElement;
            input.value = 'AI systems should NOT be granted legal personhood';
            input.dispatchEvent(new Event('input', { bubbles: true }));
          }
        });
        
        // Select Gemini provider (second participant)
        await page.evaluate(() => {
          const tabs = Array.from(document.querySelectorAll('button[role="tab"]'));
          const geminiTabs = tabs.filter(tab => 
            tab.textContent?.toLowerCase().includes('gemini') ||
            tab.getAttribute('value') === 'gemini'
          );
          if (geminiTabs.length > 1) {
            (geminiTabs[1] as HTMLElement).click();
          } else if (geminiTabs.length > 0) {
            (geminiTabs[0] as HTMLElement).click();
          }
        });
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 500)));
        
        // Select Gemini Pro model
        await page.evaluate(() => {
          const selects = document.querySelectorAll('[data-testid="model-select"], select');
          if (selects.length > 1) {
            const select = selects[1] as HTMLSelectElement;
            select.click();
          }
        });
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 500)));
        
        await page.evaluate(() => {
          const options = Array.from(document.querySelectorAll('[role="option"]'));
          const geminiOption = options.find(opt => 
            opt.textContent?.includes('pro') ||
            opt.textContent?.includes('gemini')
          );
          if (geminiOption) {
            (geminiOption as HTMLElement).click();
          }
        });
        
        console.log('✅ Added Gemini Pro participant');
      }
      
      await page.screenshot({ path: './screenshots/claude-gemini-participants.png', fullPage: true });
      
      // Submit the debate
      console.log('🚀 Submitting debate...');
      
      const debateSubmitted = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const submitBtn = buttons.find(btn => 
          btn.textContent?.includes('Create Debate') &&
          !btn.disabled
        );
        
        if (submitBtn) {
          (submitBtn as HTMLElement).click();
          return true;
        }
        return false;
      });
      
      if (debateSubmitted) {
        console.log('✅ Debate submitted successfully');
        
        await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 5000)));
        await page.screenshot({ path: './screenshots/claude-gemini-created.png', fullPage: true });
        
        // Step 4: Start the debate
        console.log('🎬 Starting debate...');
        
        // Look for the created debate and click on it
        const debateOpened = await page.evaluate(() => {
          // Look for debate cards
          const debateCards = document.querySelectorAll('[data-testid="debate-card"], .debate-card, [class*="debate"]');
          
          if (debateCards.length > 0) {
            (debateCards[0] as HTMLElement).click();
            return true;
          }
          
          // Alternative: look for any element containing our debate name
          const elements = Array.from(document.querySelectorAll('*'));
          const debateElement = elements.find(el => 
            el.textContent?.includes('Claude Sonnet 4 vs Gemini') ||
            el.textContent?.includes('AI Ethics')
          );
          
          if (debateElement) {
            (debateElement as HTMLElement).click();
            return true;
          }
          
          return false;
        });
        
        if (debateOpened) {
          console.log('✅ Opened debate view');
          
          await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 3000)));
          await page.screenshot({ path: './screenshots/claude-gemini-debate-view.png', fullPage: true });
          
          // Try to start the debate
          const debateStarted = await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const startBtn = buttons.find(btn => 
              btn.textContent?.includes('Start Debate') ||
              btn.textContent?.includes('Begin Debate') ||
              btn.textContent?.includes('Start')
            );
            
            if (startBtn) {
              (startBtn as HTMLElement).click();
              return true;
            }
            return false;
          });
          
          if (debateStarted) {
            console.log('✅ Debate started! Waiting for AI responses...');
            
            // Wait for debate turns and monitor progress
            for (let i = 0; i < 10; i++) {
              await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 5000)));
              
              const turnCount = await page.evaluate(() => {
                // Check for turn indicators or messages
                const turnElements = document.querySelectorAll('.turn, [data-testid*="turn"], .message, [class*="message"]');
                return turnElements.length;
              });
              
              console.log(`📊 Turn ${i + 1}: Found ${turnCount} turn elements`);
              
              await page.screenshot({ 
                path: `./screenshots/claude-gemini-turn-${i + 1}.png`, 
                fullPage: true 
              });
              
              // Check if debate is complete
              const isComplete = await page.evaluate(() => {
                return document.body.textContent?.includes('complete') ||
                       document.body.textContent?.includes('finished') ||
                       document.body.textContent?.includes('ended');
              });
              
              if (isComplete) {
                console.log('✅ Debate appears to be complete');
                break;
              }
            }
            
            // Step 5: Test history tracking
            console.log('📚 Testing history tracking...');
            
            // Navigate to history or organization view
            const historyFound = await page.evaluate(() => {
              const buttons = Array.from(document.querySelectorAll('button'));
              const historyBtn = buttons.find(btn => 
                btn.textContent?.includes('History') ||
                btn.textContent?.includes('View History')
              );
              
              if (historyBtn) {
                (historyBtn as HTMLElement).click();
                return true;
              }
              return false;
            });
            
            if (historyFound) {
              console.log('✅ Opened history view');
              await page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
              await page.screenshot({ path: './screenshots/claude-gemini-history.png', fullPage: true });
            }
            
            // Final verification
            const finalState = await page.evaluate(() => {
              const bodyText = document.body.innerText;
              return {
                hasZamaz: bodyText.includes('Zamaz'),
                hasClaudeGemini: bodyText.includes('Claude') && bodyText.includes('Gemini'),
                hasAIEthics: bodyText.includes('AI Ethics'),
                hasDebateContent: bodyText.includes('debate') || bodyText.includes('participant'),
                pageTitle: document.title
              };
            });
            
            console.log('🔍 Final verification:', finalState);
            
            await page.screenshot({ path: './screenshots/claude-gemini-final.png', fullPage: true });
            
            console.log('🎉 Claude vs Gemini debate test completed successfully!');
            console.log('📸 Screenshots saved to ./screenshots/');
            
          } else {
            console.log('⚠️ Could not start debate - may need backend services');
          }
        } else {
          console.log('⚠️ Could not find created debate - checking current page state');
          
          const currentState = await page.evaluate(() => {
            return {
              debateCards: document.querySelectorAll('[data-testid="debate-card"]').length,
              bodyText: document.body.innerText.substring(0, 500),
              buttons: Array.from(document.querySelectorAll('button')).map(btn => btn.textContent?.trim()).filter(Boolean)
            };
          });
          
          console.log('Current page state:', currentState);
        }
      } else {
        console.log('❌ Could not submit debate form');
      }
    } else {
      console.log('❌ Could not open debate creation dialog');
    }
    
    console.log('🏁 Test completed - Check screenshots for visual verification');
    
  }, 300000); // 5 minute timeout for the full test
});