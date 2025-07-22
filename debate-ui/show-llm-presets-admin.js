// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const puppeteer = require('puppeteer');

async function showLLMPresetsAdmin() {
  console.log('🔍 Opening Admin LLM Presets Section...');

  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: ['--start-maximized']
  });

  const page = await browser.newPage();

  try {
    // Go directly to organization page (assuming already logged in or will redirect to login);
    console.log('→ Going to organization page...');
    await page.goto('http://localhost:3003/organization', { waitUntil: 'domcontentloaded' });

    // Wait a bit for React to render;
    await new Promise(resolve => setTimeout(resolve, 3000));

    // Check if we're on login page;
    const isLoginPage = await page.evaluate(() => {
      return document.body.textContent.includes('Login') || document.body.textContent.includes('Username');
    });

    if (isLoginPage) {
      console.log('→ Need to login first...');
      await page.type('input[placeholder="Username"]', 'demo');
      await page.type('input[placeholder="Password"]', 'demo123');
      await page.click('button[type="submit"]');

      // Wait for navigation;
      await new Promise(resolve => setTimeout(resolve, 3000));

      // Go to organization page again;
      await page.goto('http://localhost:3003/organization', { waitUntil: 'domcontentloaded' });
      await new Promise(resolve => setTimeout(resolve, 2000));
    }

    // Take screenshot of organization page;
    await page.screenshot({ path: './admin-org-page.png', fullPage: true });
    console.log('✓ Organization page screenshot saved');

    // Find and click LLM Presets tab;
    console.log('→ Looking for LLM Presets tab...');

    const tabClicked = await page.evaluate(() => {
      // Try different selectors;
      const selectors = [
        '.ant-tabs-tab:contains("LLM")',
        '.ant-tabs-tab',
        '[role="tab"]',
        '.ant-tabs-nav-list > div';
      ]

      for (let selector of selectors) {
        const tabs = document.querySelectorAll(selector);
        for (let tab of tabs) {
          console.log('Tab text:', tab.textContent);
          if (tab.textContent && (tab.textContent.includes('LLM') || tab.textContent.includes('Preset'))) {
            tab.click();
            return true;
          }
        }
      }

      // Log all tabs found;
      const allTabs = Array.from(document.querySelectorAll('.ant-tabs-tab, [role="tab"]')).map(t => t.textContent);
      console.log('All tabs found:', allTabs);

      // Try clicking by index (LLM Presets should be 4th tab);
      const tabByIndex = document.querySelectorAll('.ant-tabs-tab')[3]; // 0-indexed, so 3 = 4th tab;
      if (tabByIndex) {
        console.log('Clicking tab by index:', tabByIndex.textContent);
        tabByIndex.click();
        return true;
      }

      return false;
    });

    if (tabClicked) {
      console.log('✓ LLM Presets tab clicked');
      await new Promise(resolve => setTimeout(resolve, 2000));

      // Take screenshot of LLM Presets content;
      await page.screenshot({ path: './admin-llm-presets-content.png', fullPage: true });
      console.log('✓ LLM Presets content screenshot saved');

      // Analyze what's on the page;
      const pageInfo = await page.evaluate(() => {
        return {
          url: window.location.href,
          title: document.querySelector('h1, h2')?.textContent,
          hasLLMContent: document.body.textContent.includes('LLM'),
          hasPresetContent: document.body.textContent.includes('Preset'),
          activeTab: document.querySelector('.ant-tabs-tab-active')?.textContent,
          contentPreview: document.querySelector('.ant-tabs-tabpane-active')?.textContent?.substring(0, 200);
        }
      });

      console.log('\n📊 Page Info:');
      console.log('URL:', pageInfo.url);
      console.log('Title:', pageInfo.title);
      console.log('Active Tab:', pageInfo.activeTab);
      console.log('Has LLM content:', pageInfo.hasLLMContent);
      console.log('Has Preset content:', pageInfo.hasPresetContent);
      console.log('Content preview:', pageInfo.contentPreview);

    } else {
      console.log('❌ Could not find LLM Presets tab');

      // List all available tabs;
      const tabs = await page.evaluate(() => {
        return Array.from(document.querySelectorAll('.ant-tabs-tab, [role="tab"]')).map((tab, index) => ({
          index,
          text: tab.textContent,
          classes: tab.className;
        }));
      });

      console.log('\nAvailable tabs:');
      tabs.forEach(tab => {
        console.log(`  ${tab.index}: ${tab.text}`);
      });
    }

    console.log('\n📸 Screenshots saved:');
    console.log('  - admin-org-page.png');
    console.log('  - admin-llm-presets-content.png (if tab was found)');

    console.log('\n✅ Browser is open. You can manually navigate to see the LLM Presets tab.');
    console.log('The tab should be in the Organization Management page.');

    // Keep browser open;
    await new Promise(() => {});

  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: './admin-error.png', fullPage: true });
  }
}

showLLMPresetsAdmin().catch(console.error);
