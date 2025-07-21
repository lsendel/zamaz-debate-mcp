const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

async function captureAdminLLMPresets() {
  console.log('📸 Capturing Admin LLM Presets Section...');
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: ['--start-maximized', '--no-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  try {
    // Step 1: Login
    console.log('→ Step 1: Logging in...');
    await page.goto('http://localhost:3003/login', { waitUntil: 'networkidle0' });
    
    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');
    await page.click('button[type="submit"]');
    
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    console.log('✓ Logged in successfully');
    
    // Step 2: Navigate to Organization Management
    console.log('→ Step 2: Navigating to Organization Management...');
    
    // Look for admin/organization link in navigation
    const orgLink = await page.$('a[href*="organization"], a:contains("Organization"), .ant-menu-item:contains("Organization")');
    if (orgLink) {
      await orgLink.click();
      await page.waitForNavigation({ waitUntil: 'networkidle0' });
    } else {
      // Try direct navigation
      await page.goto('http://localhost:3003/organization', { waitUntil: 'networkidle0' });
    }
    
    await page.screenshot({ path: './admin-organization-page.png', fullPage: true });
    console.log('✓ Organization Management page captured');
    
    // Step 3: Find and click LLM Presets tab
    console.log('→ Step 3: Looking for LLM Presets tab...');
    
    // Wait for tabs to load
    await page.waitForSelector('.ant-tabs', { timeout: 5000 });
    
    // Find LLM Presets tab
    const llmPresetTab = await page.evaluate(() => {
      const tabs = document.querySelectorAll('.ant-tabs-tab');
      for (let tab of tabs) {
        if (tab.textContent.includes('LLM Presets')) {
          tab.click();
          return true;
        }
      }
      return false;
    });
    
    if (llmPresetTab) {
      console.log('✓ Found and clicked LLM Presets tab');
      await new Promise(resolve => setTimeout(resolve, 2000)); // Wait for content to load
      
      await page.screenshot({ path: './admin-llm-presets-tab.png', fullPage: true });
      console.log('✓ LLM Presets tab content captured');
      
      // Capture specific sections
      // Check if preset cards are visible
      const hasPresetCards = await page.$('.ant-card');
      if (hasPresetCards) {
        await page.screenshot({ path: './admin-llm-presets-detail.png', fullPage: true });
        console.log('✓ LLM Preset details captured');
      }
      
    } else {
      console.log('❌ LLM Presets tab not found');
      
      // Capture what tabs are available
      const availableTabs = await page.evaluate(() => {
        return Array.from(document.querySelectorAll('.ant-tabs-tab')).map(tab => tab.textContent);
      });
      console.log('Available tabs:', availableTabs);
    }
    
    // Step 4: Check page content
    const pageContent = await page.evaluate(() => {
      return {
        title: document.querySelector('h1')?.textContent || 'No title',
        tabs: Array.from(document.querySelectorAll('.ant-tabs-tab')).map(tab => tab.textContent),
        hasLLMContent: document.body.textContent.includes('LLM') || document.body.textContent.includes('Preset'),
        bodyPreview: document.body.textContent.substring(0, 500)
      };
    });
    
    console.log('\n📊 Page Analysis:');
    console.log('Title:', pageContent.title);
    console.log('Tabs found:', pageContent.tabs.join(', '));
    console.log('Has LLM content:', pageContent.hasLLMContent);
    console.log('Content preview:', pageContent.bodyPreview.substring(0, 200) + '...');
    
    console.log('\n✅ Screenshots saved:');
    console.log('  - admin-organization-page.png');
    console.log('  - admin-llm-presets-tab.png (if tab found)');
    console.log('  - admin-llm-presets-detail.png (if content found)');
    
    // Keep browser open for manual inspection
    console.log('\n⏸️ Browser staying open for manual inspection.');
    console.log('You can navigate to the LLM Presets tab manually if needed.');
    console.log('Press Ctrl+C to close.');
    
    await new Promise(() => {}); // Keep browser open indefinitely
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: './admin-error-state.png', fullPage: true });
  }
}

captureAdminLLMPresets().catch(console.error);