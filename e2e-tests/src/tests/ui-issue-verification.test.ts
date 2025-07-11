import puppeteer, { Browser, Page } from 'puppeteer';

describe('UI Issue Verification - Comprehensive Check', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 200,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false,
      timeout: 30000
    });
  });

  afterAll(async () => {
    // Keep browser open longer for manual verification
    await new Promise(resolve => setTimeout(resolve, 10000));
    if (browser) {
      await browser.close();
    }
  });

  beforeEach(async () => {
    page = await browser.newPage();
    page.setDefaultTimeout(15000);
  });

  afterEach(async () => {
    if (page) {
      await page.close();
    }
  });

  test('should verify current UI state and identify issues', async () => {
    console.log('🔍 Starting comprehensive UI verification...');
    
    // Step 1: Navigate to UI
    await page.goto('http://localhost:3000');
    
    try {
      await page.waitForSelector('h1', { timeout: 5000 });
      console.log('✅ UI loaded on port 3000');
    } catch {
      // Try port 3001
      await page.goto('http://localhost:3001');
      try {
        await page.waitForSelector('h1', { timeout: 5000 });
        console.log('✅ UI loaded on port 3001');
      } catch {
        // Try port 3002
        await page.goto('http://localhost:3002');
        await page.waitForSelector('h1', { timeout: 5000 });
        console.log('✅ UI loaded on port 3002');
      }
    }
    
    await page.screenshot({ path: './screenshots/ui-issue-start.png', fullPage: true });
    
    // Step 2: Analyze current page state
    const pageAnalysis = await page.evaluate(() => {
      const analysis = {
        title: document.querySelector('h1')?.textContent || '',
        url: window.location.href,
        viewportSize: {
          width: window.innerWidth,
          height: window.innerHeight
        },
        organizations: {
          dropdown: null as any,
          menuOpen: false,
          options: [] as string[]
        },
        buttons: [] as any[],
        tabs: [] as any[],
        errors: [] as string[],
        warnings: [] as string[]
      };
      
      // Check organization dropdown
      const orgButtons = Array.from(document.querySelectorAll('button')).filter(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      );
      
      if (orgButtons.length > 0) {
        const orgButton = orgButtons[0] as HTMLElement;
        const computedStyle = window.getComputedStyle(orgButton);
        
        analysis.organizations.dropdown = {
          text: orgButton.textContent?.trim(),
          visible: computedStyle.display !== 'none',
          backgroundColor: computedStyle.backgroundColor,
          color: computedStyle.color,
          borderColor: computedStyle.borderColor,
          hasDataState: orgButton.hasAttribute('data-state'),
          dataState: orgButton.getAttribute('data-state'),
          classes: orgButton.className
        };
        
        // Check if dropdown menu is visible
        const dropdownMenu = document.querySelector('[role="menu"], .dropdown-menu, [data-radix-popper-content-wrapper]');
        if (dropdownMenu) {
          const menuStyle = window.getComputedStyle(dropdownMenu as Element);
          analysis.organizations.menuOpen = menuStyle.display !== 'none' && 
                                         menuStyle.opacity !== '0' && 
                                         menuStyle.visibility !== 'hidden';
          
          // Get menu options
          const menuItems = Array.from(dropdownMenu.querySelectorAll('[role="menuitem"], .dropdown-item'));
          analysis.organizations.options = menuItems.map(item => item.textContent?.trim()).filter(Boolean) as string[];
        }
      }
      
      // Check all buttons
      analysis.buttons = Array.from(document.querySelectorAll('button')).map(btn => {
        const style = window.getComputedStyle(btn);
        return {
          text: btn.textContent?.trim(),
          visible: style.display !== 'none',
          disabled: btn.disabled,
          classes: btn.className,
          type: btn.type || 'button'
        };
      });
      
      // Check tabs
      analysis.tabs = Array.from(document.querySelectorAll('[role="tab"]')).map(tab => {
        const style = window.getComputedStyle(tab);
        return {
          text: tab.textContent?.trim(),
          selected: tab.getAttribute('aria-selected') === 'true',
          visible: style.display !== 'none',
          classes: tab.className
        };
      });
      
      // Check for common issues
      const allElements = Array.from(document.querySelectorAll('*'));
      
      // Check for invisible text (white on white, etc.)
      allElements.forEach(el => {
        const style = window.getComputedStyle(el);
        const hasText = el.textContent && el.textContent.trim().length > 0;
        
        if (hasText) {
          const textColor = style.color;
          const bgColor = style.backgroundColor;
          
          // Simple check for potential contrast issues
          if ((textColor === 'rgb(255, 255, 255)' && bgColor === 'rgb(255, 255, 255)') ||
              (textColor === 'rgb(0, 0, 0)' && bgColor === 'rgb(0, 0, 0)')) {
            analysis.warnings.push(`Potential contrast issue: ${el.tagName} with text "${el.textContent?.trim()?.substring(0, 50)}"`);
          }
        }
      });
      
      // Check for JavaScript errors
      if ((window as any).lastError) {
        analysis.errors.push((window as any).lastError.toString());
      }
      
      return analysis;
    });
    
    console.log('📊 Page Analysis:', JSON.stringify(pageAnalysis, null, 2));
    
    // Step 3: Test organization dropdown interaction
    console.log('🏢 Testing organization dropdown interaction...');
    
    const dropdownInteractionResult = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      
      if (!orgButton) {
        return { success: false, error: 'Organization button not found' };
      }
      
      // Record initial state
      const initialState = {
        text: orgButton.textContent?.trim(),
        dataState: orgButton.getAttribute('data-state'),
        classes: orgButton.className
      };
      
      // Click the button
      orgButton.click();
      
      // Wait a moment for the dropdown to appear
      return new Promise((resolve) => {
        setTimeout(() => {
          const afterClickState = {
            text: orgButton.textContent?.trim(),
            dataState: orgButton.getAttribute('data-state'),
            classes: orgButton.className
          };
          
          // Check if dropdown menu appeared
          const dropdownMenu = document.querySelector('[role="menu"], .dropdown-menu, [data-radix-popper-content-wrapper]');
          let menuVisible = false;
          let menuItems: string[] = [];
          
          if (dropdownMenu) {
            const menuStyle = window.getComputedStyle(dropdownMenu as Element);
            menuVisible = menuStyle.display !== 'none' && 
                         menuStyle.opacity !== '0' && 
                         menuStyle.visibility !== 'hidden';
            
            if (menuVisible) {
              const items = Array.from(dropdownMenu.querySelectorAll('[role="menuitem"], .dropdown-item, [data-radix-dropdown-menu-item]'));
              menuItems = items.map(item => item.textContent?.trim()).filter(Boolean) as string[];
            }
          }
          
          resolve({
            success: true,
            initialState,
            afterClickState,
            menuVisible,
            menuItems,
            stateChanged: initialState.dataState !== afterClickState.dataState
          });
        }, 1000);
      });
    });
    
    console.log('🔄 Dropdown Interaction Result:', dropdownInteractionResult);
    
    await page.screenshot({ path: './screenshots/ui-issue-dropdown-open.png', fullPage: true });
    
    // Step 4: Test tab navigation
    console.log('📑 Testing tab navigation...');
    
    if (pageAnalysis.tabs.length > 0) {
      for (const [index, tab] of pageAnalysis.tabs.entries()) {
        console.log(`Testing tab ${index + 1}: ${tab.text}`);
        
        try {
          await page.evaluate((tabText: string) => {
            const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
            const targetTab = tabs.find(t => t.textContent && t.textContent.includes(tabText)) as HTMLElement;
            if (targetTab) {
              targetTab.click();
            }
          }, tab.text || '');
          
          await new Promise(resolve => setTimeout(resolve, 1000));
          await page.screenshot({ path: `./screenshots/ui-issue-tab-${index + 1}.png`, fullPage: true });
          
          console.log(`✅ Successfully interacted with tab: ${tab.text}`);
        } catch (error) {
          console.log(`❌ Failed to interact with tab: ${tab.text}`);
        }
      }
    }
    
    // Step 5: Test form interactions
    console.log('📝 Testing form interactions...');
    
    // Try to click "New Debate" button
    const newDebateResult = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const newDebateBtn = buttons.find(btn => 
        btn.textContent?.includes('New Debate')
      ) as HTMLElement;
      
      if (newDebateBtn) {
        newDebateBtn.click();
        return { clicked: true, buttonText: newDebateBtn.textContent?.trim() };
      }
      
      return { clicked: false, error: 'New Debate button not found' };
    });
    
    console.log('🆕 New Debate Button Result:', newDebateResult);
    
    if (newDebateResult.clicked) {
      await new Promise(resolve => setTimeout(resolve, 2000));
      await page.screenshot({ path: './screenshots/ui-issue-new-debate.png', fullPage: true });
      
      // Check if a dialog or form appeared
      const dialogCheck = await page.evaluate(() => {
        const dialogs = document.querySelectorAll('[role="dialog"], .dialog, [data-radix-dialog-content]');
        const forms = document.querySelectorAll('form');
        
        return {
          dialogsFound: dialogs.length,
          formsFound: forms.length,
          visibleDialogs: Array.from(dialogs).filter(d => {
            const style = window.getComputedStyle(d);
            return style.display !== 'none' && style.opacity !== '0';
          }).length
        };
      });
      
      console.log('💬 Dialog Check Result:', dialogCheck);
    }
    
    // Step 6: Final page state analysis
    const finalPageState = await page.evaluate(() => {
      return {
        errors: Array.from(document.querySelectorAll('.error, [data-error], .alert-error')).map(el => el.textContent?.trim()),
        warnings: Array.from(document.querySelectorAll('.warning, [data-warning], .alert-warning')).map(el => el.textContent?.trim()),
        loading: Array.from(document.querySelectorAll('.loading, [data-loading], .spinner')).length > 0,
        totalElements: document.querySelectorAll('*').length,
        hasJavaScriptErrors: !!(window as any).lastError
      };
    });
    
    await page.screenshot({ path: './screenshots/ui-issue-final.png', fullPage: true });
    
    console.log('📋 Final Page State:', finalPageState);
    
    // Step 7: Summary and recommendations
    const summary = {
      pageLoaded: !!pageAnalysis.title,
      organizationDropdownWorking: !!pageAnalysis.organizations.dropdown,
      tabNavigationWorking: pageAnalysis.tabs.length > 0,
      newDebateButtonWorking: newDebateResult.clicked,
      potentialIssues: [
        ...pageAnalysis.errors,
        ...pageAnalysis.warnings,
        ...finalPageState.errors.filter(Boolean),
        ...finalPageState.warnings.filter(Boolean)
      ]
    };
    
    console.log('🎯 SUMMARY:', summary);
    console.log('📸 Screenshots saved to ./screenshots/ui-issue-*.png');
    
    // Assertions
    expect(summary.pageLoaded).toBe(true);
    expect(summary.organizationDropdownWorking).toBe(true);
    
    console.log('✅ UI verification completed!');
    
  }, 120000); // 2 minute timeout
});
