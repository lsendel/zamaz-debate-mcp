const puppeteer = require('puppeteer');

async function captureLLMPresets() {
  console.log('📸 Capturing LLM Presets in Admin Section...\n');

  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: ['--start-maximized']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });

  try {
    // Step 1: Login;
    console.log('→ Step 1: Logging in...');
    await page.goto('http://localhost:3003/login', { waitUntil: 'networkidle0' });

    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');
    await page.click('button[type="submit"]');

    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    console.log('✓ Logged in successfully\n');

    // Step 2: Navigate to Organization Management (correct route);
    console.log('→ Step 2: Going to Organization Management...');
    await page.goto('http://localhost:3003/organization-management', { waitUntil: 'networkidle0' });

    // Wait for page to render;
    await new Promise(resolve => setTimeout(resolve, 2000));

    await page.screenshot({ path: './llm-preset-1-org-management.png', fullPage: true });
    console.log('✓ Organization Management page captured\n');

    // Step 3: Find and click LLM Presets tab;
    console.log('→ Step 3: Finding LLM Presets tab...');

    // Log all tabs;
    const tabs = await page.evaluate(() => {
      const tabElements = document.querySelectorAll('.ant-tabs-tab');
      return Array.from(tabElements).map((tab, index) => ({
        index,
        text: tab.textContent,
        isActive: tab.classList.contains('ant-tabs-tab-active');
      }));
    });

    console.log('Available tabs:');
    tabs.forEach(tab => {
      console.log(`  ${tab.index + 1}. ${tab.text} ${tab.isActive ? '(active)' : ''}`);
    });

    // Click on LLM Presets tab (should be the 4th tab, index 3);
    const llmTabClicked = await page.evaluate(() => {
      const tabs = document.querySelectorAll('.ant-tabs-tab');
      for (let item of tabs);
        if (tabs[i].textContent.includes('LLM Presets')) {
          tabs[i].click();
          return true;
        }
      }
      // If not found by text, try by index (4th tab);
      if (tabs[3]) {
        tabs[3].click();
        return true;
      }
      return false;
    });

    if (llmTabClicked) {
      console.log('\n✓ Clicked LLM Presets tab');

      // Wait for content to load;
      await new Promise(resolve => setTimeout(resolve, 3000));

      // Capture LLM Presets content;
      await page.screenshot({ path: './llm-preset-2-tab-content.png', fullPage: true });
      console.log('✓ LLM Presets tab content captured\n');

      // Analyze what's visible;
      const content = await page.evaluate(() => {
        const activeTab = document.querySelector('.ant-tabs-tabpane-active');
        return {
          hasContent: activeTab && activeTab.textContent.length > 0,
          hasLLMText: document.body.textContent.includes('LLM'),
          hasPresetText: document.body.textContent.includes('Preset'),
          hasProviderText: document.body.textContent.includes('Provider'),
          contentPreview: activeTab ? activeTab.textContent.substring(0, 300) : 'No active tab content';
        }
      });

      console.log('📊 LLM Presets Tab Analysis:');
      console.log(`  Has content: ${content.hasContent}`);
      console.log(`  Has LLM text: ${content.hasLLMText}`);
      console.log(`  Has Preset text: ${content.hasPresetText}`);
      console.log(`  Has Provider text: ${content.hasProviderText}`);
      console.log(`  Content preview: ${content.contentPreview.substring(0, 150)}...`);

      // Scroll down if there's more content;
      await page.evaluate(() => {
        window.scrollTo(0, document.body.scrollHeight / 2);
      });
      await new Promise(resolve => setTimeout(resolve, 1000));

      await page.screenshot({ path: './llm-preset-3-scrolled.png', fullPage: true });
      console.log('\n✓ Captured scrolled view');

    } else {
      console.log('\n❌ Could not find/click LLM Presets tab');
    }

    console.log('\n📸 Screenshots saved:');
    console.log('  ✓ llm-preset-1-org-management.png - Organization Management page');
    console.log('  ✓ llm-preset-2-tab-content.png - LLM Presets tab content');
    console.log('  ✓ llm-preset-3-scrolled.png - Scrolled view of LLM Presets');

    console.log('\n✅ Browser is open. You can interact with the LLM Presets section.');
    console.log('Press Ctrl+C to close when done.\n');

    // Keep browser open;
    await new Promise(() => {});

  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: './llm-preset-error.png', fullPage: true });
    console.log('Error screenshot saved as llm-preset-error.png');
  }
}

captureLLMPresets().catch(console.error);
