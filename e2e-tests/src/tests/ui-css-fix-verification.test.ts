import puppeteer, { Browser, Page } from 'puppeteer';

describe('UI CSS Fix Verification - Organization Dropdown', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 100,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false,
      timeout: 30000
    });
  });

  afterAll(async () => {
    // Keep browser open for manual verification
    await new Promise(resolve => setTimeout(resolve, 5000));
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

  test('should verify organization dropdown CSS fix and text readability', async () => {
    console.log('🎨 Starting CSS fix verification test...');
    
    // Step 1: Navigate to UI
    await page.goto('http://localhost:3002');
    await page.waitForSelector('h1', { timeout: 10000 });
    console.log('✅ UI loaded successfully');
    
    await page.screenshot({ path: './screenshots/css-test-start.png', fullPage: true });
    
    // Step 2: Find and analyze organization dropdown
    console.log('🔍 Analyzing organization dropdown...');
    
    const dropdownExists = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      );
      return !!orgButton;
    });
    
    expect(dropdownExists).toBe(true);
    console.log('✅ Organization dropdown found');
    
    // Step 3: Test normal state styling
    const normalStateStyles = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      
      if (!orgButton) return null;
      
      const computedStyle = window.getComputedStyle(orgButton);
      return {
        backgroundColor: computedStyle.backgroundColor,
        color: computedStyle.color,
        borderColor: computedStyle.borderColor,
        textContent: orgButton.textContent?.trim()
      };
    });
    
    console.log('📊 Normal state styles:', normalStateStyles);
    expect(normalStateStyles).not.toBeNull();
    
    // Step 4: Test hover state
    await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      if (orgButton) {
        orgButton.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
      }
    });
    await new Promise(resolve => setTimeout(resolve, 500));
    
    const hoverStateStyles = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      
      if (!orgButton) return null;
      
      const computedStyle = window.getComputedStyle(orgButton);
      return {
        backgroundColor: computedStyle.backgroundColor,
        color: computedStyle.color,
        borderColor: computedStyle.borderColor
      };
    });
    
    console.log('📊 Hover state styles:', hoverStateStyles);
    await page.screenshot({ path: './screenshots/css-test-hover.png', fullPage: true });
    
    // Step 5: Test focus/click state
    await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      if (orgButton) {
        orgButton.click();
      }
    });
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    const focusStateStyles = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      
      if (!orgButton) return null;
      
      const computedStyle = window.getComputedStyle(orgButton);
      return {
        backgroundColor: computedStyle.backgroundColor,
        color: computedStyle.color,
        borderColor: computedStyle.borderColor,
        hasDataStateOpen: orgButton.hasAttribute('data-state') && orgButton.getAttribute('data-state') === 'open'
      };
    });
    
    console.log('📊 Focus/open state styles:', focusStateStyles);
    await page.screenshot({ path: './screenshots/css-test-focus-open.png', fullPage: true });
    
    // Step 6: Check dropdown menu visibility
    const dropdownMenuVisible = await page.evaluate(() => {
      const dropdownContent = document.querySelector('[role="menu"], .dropdown-menu, [data-radix-popper-content-wrapper]');
      if (!dropdownContent) return false;
      
      const style = window.getComputedStyle(dropdownContent as Element);
      return style.display !== 'none' && style.opacity !== '0' && style.visibility !== 'hidden';
    });
    
    console.log('📋 Dropdown menu visible:', dropdownMenuVisible);
    
    // Step 7: Test text contrast and readability
    const contrastAnalysis = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const orgButton = buttons.find(btn => 
        btn.textContent?.includes('Organization') ||
        btn.textContent?.includes('Default')
      ) as HTMLElement;
      
      if (!orgButton) return null;
      
      const computedStyle = window.getComputedStyle(orgButton);
      const textSpan = orgButton.querySelector('span');
      
      // Helper function to parse RGB
      const parseRgb = (rgbString: string) => {
        const match = rgbString.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
        if (!match) return null;
        return {
          r: parseInt(match[1]),
          g: parseInt(match[2]),
          b: parseInt(match[3])
        };
      };
      
      // Calculate luminance for contrast ratio
      const getLuminance = (rgb: {r: number, g: number, b: number}) => {
        const rsRGB = rgb.r / 255;
        const gsRGB = rgb.g / 255;
        const bsRGB = rgb.b / 255;
        
        const r = rsRGB <= 0.03928 ? rsRGB / 12.92 : Math.pow((rsRGB + 0.055) / 1.055, 2.4);
        const g = gsRGB <= 0.03928 ? gsRGB / 12.92 : Math.pow((gsRGB + 0.055) / 1.055, 2.4);
        const b = bsRGB <= 0.03928 ? bsRGB / 12.92 : Math.pow((bsRGB + 0.055) / 1.055, 2.4);
        
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
      };
      
      const textColor = parseRgb(computedStyle.color);
      const bgColor = parseRgb(computedStyle.backgroundColor);
      
      if (!textColor || !bgColor) {
        return {
          readable: false,
          reason: 'Could not parse colors',
          textColor: computedStyle.color,
          backgroundColor: computedStyle.backgroundColor
        };
      }
      
      const textLuminance = getLuminance(textColor);
      const bgLuminance = getLuminance(bgColor);
      
      const contrastRatio = (Math.max(textLuminance, bgLuminance) + 0.05) / (Math.min(textLuminance, bgLuminance) + 0.05);
      
      return {
        readable: contrastRatio >= 4.5, // WCAG AA standard
        contrastRatio: contrastRatio,
        textColor: computedStyle.color,
        backgroundColor: computedStyle.backgroundColor,
        textContent: orgButton.textContent?.trim(),
        hasVisibleText: !!orgButton.textContent?.trim()
      };
    });
    
    console.log('🔍 Contrast analysis:', contrastAnalysis);
    
    // Step 8: Assertions
    expect(contrastAnalysis).not.toBeNull();
    expect(contrastAnalysis?.hasVisibleText).toBe(true);
    expect(contrastAnalysis?.textContent).toBeTruthy();
    
    // The main fix - text should be readable (contrast ratio >= 4.5)
    if (contrastAnalysis?.contrastRatio && contrastAnalysis.contrastRatio < 4.5) {
      console.warn(`⚠️ Low contrast ratio: ${contrastAnalysis.contrastRatio.toFixed(2)}`);
      console.warn(`Text: ${contrastAnalysis.textColor}, Background: ${contrastAnalysis.backgroundColor}`);
    } else {
      console.log(`✅ Good contrast ratio: ${contrastAnalysis?.contrastRatio?.toFixed(2)}`);
    }
    
    // Step 9: Test organization creation flow
    console.log('🏢 Testing organization creation...');
    
    // Check if dropdown is open, if not click it
    if (!dropdownMenuVisible) {
      await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const orgButton = buttons.find(btn => 
          btn.textContent?.includes('Organization') ||
          btn.textContent?.includes('Default')
        ) as HTMLElement;
        if (orgButton) {
          orgButton.click();
        }
      });
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    // Look for Create Organization option
    const createOrgFound = await page.evaluate(() => {
      const elements = Array.from(document.querySelectorAll('*'));
      return elements.some(el => 
        el.textContent?.includes('Create Organization')
      );
    });
    
    console.log('🆕 Create Organization option found:', createOrgFound);
    
    if (createOrgFound) {
      // Try to click Create Organization
      try {
        await page.evaluate(() => {
          const elements = Array.from(document.querySelectorAll('*'));
          const createOption = elements.find(el => 
            el.textContent?.includes('Create Organization')
          ) as HTMLElement;
          if (createOption) {
            createOption.click();
          }
        });
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Check if dialog opened
        const dialogOpen = await page.evaluate(() => {
          return document.querySelector('[role="dialog"], .dialog, [data-radix-dialog-content]') !== null;
        });
        
        console.log('📝 Organization creation dialog opened:', dialogOpen);
        
        if (dialogOpen) {
          await page.screenshot({ path: './screenshots/css-test-create-org-dialog.png', fullPage: true });
          
          // Close dialog
          await page.keyboard.press('Escape');
          await new Promise(resolve => setTimeout(resolve, 500));
        }
      } catch (error) {
        console.log('ℹ️ Could not test organization creation (dialog interaction)');
      }
    }
    
    // Final screenshot
    await page.screenshot({ path: './screenshots/css-test-final.png', fullPage: true });
    
    console.log('🎉 CSS fix verification completed!');
    console.log('📸 Screenshots saved to ./screenshots/css-test-*.png');
    
    // Summary
    const summary = {
      dropdownFound: dropdownExists,
      menuVisible: dropdownMenuVisible,
      textReadable: contrastAnalysis?.readable || false,
      contrastRatio: contrastAnalysis?.contrastRatio || 0,
      createOrgAvailable: createOrgFound
    };
    
    console.log('📋 Test Summary:', summary);
    
    // Main assertion - the CSS fix should make text readable
    expect(summary.textReadable).toBe(true);
    
  }, 60000); // 1 minute timeout

  test('should test responsive design and mobile view', async () => {
    console.log('📱 Testing responsive design...');
    
    // Test mobile viewport
    await page.setViewport({ width: 375, height: 667 }); // iPhone size
    await page.goto('http://localhost:3002');
    await page.waitForSelector('h1');
    
    await page.screenshot({ path: './screenshots/css-test-mobile.png', fullPage: true });
    
    // Test tablet viewport  
    await page.setViewport({ width: 768, height: 1024 }); // iPad size
    await page.reload();
    await page.waitForSelector('h1');
    
    await page.screenshot({ path: './screenshots/css-test-tablet.png', fullPage: true });
    
    // Test desktop viewport
    await page.setViewport({ width: 1920, height: 1080 }); // Desktop size
    await page.reload();
    await page.waitForSelector('h1');
    
    await page.screenshot({ path: './screenshots/css-test-desktop.png', fullPage: true });
    
    console.log('✅ Responsive design test completed');
  }, 30000);
});

describe('UI Core Functionality Test', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,
      slowMo: 100,
      defaultViewport: { width: 1400, height: 900 },
      devtools: false
    });
  });

  afterAll(async () => {
    await new Promise(resolve => setTimeout(resolve, 3000));
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

  test('should test navigation and basic UI interactions', async () => {
    console.log('🧭 Testing navigation and UI interactions...');
    
    await page.goto('http://localhost:3002');
    await page.waitForSelector('h1');
    
    // Test main navigation elements
    const navigationElements = await page.evaluate(() => {
      const elements = {
        title: document.querySelector('h1')?.textContent || '',
        buttons: Array.from(document.querySelectorAll('button')).map(btn => btn.textContent?.trim()).filter((btn): btn is string => Boolean(btn)),
        tabs: Array.from(document.querySelectorAll('[role="tab"]')).map(tab => tab.textContent?.trim()).filter((tab): tab is string => Boolean(tab)),
        links: Array.from(document.querySelectorAll('a')).map(link => link.textContent?.trim()).filter((link): link is string => Boolean(link))
      };
      return elements;
    });
    
    console.log('🔍 Navigation elements found:', navigationElements);
    
    // Test tab navigation if available
    if (navigationElements.tabs.length > 0) {
      for (const tab of navigationElements.tabs.slice(0, 3)) { // Test first 3 tabs
        try {
          await page.evaluate((tabText: string) => {
            const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
            const targetTab = tabs.find(t => t.textContent && t.textContent.includes(tabText)) as HTMLElement;
            if (targetTab) {
              targetTab.click();
            }
          }, tab);
          await new Promise(resolve => setTimeout(resolve, 1000));
          console.log(`✅ Successfully clicked tab: ${tab}`);
        } catch (error) {
          console.log(`ℹ️ Could not click tab: ${tab}`);
        }
      }
    }
    
    await page.screenshot({ path: './screenshots/navigation-test.png', fullPage: true });
    
    expect(navigationElements.title).toBeTruthy();
    console.log('✅ Navigation test completed');
  }, 30000);
});
