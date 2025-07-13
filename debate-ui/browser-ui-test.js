// Browser-based UI test script
// Run this directly in the browser console at http://localhost:3001

async function runBrowserUITest() {
  console.clear();
  console.log('🚀 Starting Browser UI Test\n');
  
  const results = {
    passed: [],
    failed: [],
    warnings: []
  };
  
  // Helper function to test elements
  function testElement(selector, description, additionalCheck) {
    try {
      const element = document.querySelector(selector);
      if (!element) {
        throw new Error('Element not found');
      }
      
      if (additionalCheck && !additionalCheck(element)) {
        throw new Error('Additional check failed');
      }
      
      results.passed.push(description);
      console.log(`✅ ${description}`);
      return element;
    } catch (error) {
      results.failed.push({ test: description, error: error.message });
      console.log(`❌ ${description}: ${error.message}`);
      return null;
    }
  }
  
  // Helper to wait for element
  async function waitForElement(selector, timeout = 5000) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
      const element = document.querySelector(selector);
      if (element) return element;
      await new Promise(r => setTimeout(r, 100));
    }
    return null;
  }
  
  console.log('=== 1. LOCALSTORAGE CHECK ===\n');
  
  const orgId = localStorage.getItem('currentOrganizationId');
  const orgs = JSON.parse(localStorage.getItem('organizations') || '[]');
  
  console.log(`Organization ID: ${orgId || 'None'}`);
  console.log(`Organizations: ${orgs.length}`);
  console.log(`First org: ${orgs[0]?.name || 'None'}\n`);
  
  if (!orgId || orgs.length === 0) {
    console.log('⚠️  No organization set, creating default...');
    const defaultOrg = {
      id: 'default-org',
      name: 'Default Organization',
      createdAt: new Date().toISOString(),
      debateCount: 0
    };
    localStorage.setItem('organizations', JSON.stringify([defaultOrg]));
    localStorage.setItem('currentOrganizationId', defaultOrg.id);
    console.log('✅ Default organization created\n');
  }
  
  console.log('=== 2. HEADER COMPONENTS ===\n');
  
  testElement('h1', 'Main title', el => el.textContent.includes('AI Debate System'));
  testElement('[aria-expanded]', 'Organization switcher');
  testElement('button', 'Test LLM button', el => el.textContent.includes('Test LLM'));
  testElement('button', 'New Debate button', el => el.textContent.includes('New Debate'));
  
  // Check organization switcher text
  const orgSwitcher = document.querySelector('[aria-expanded]');
  if (orgSwitcher) {
    const orgText = orgSwitcher.querySelector('span')?.textContent;
    console.log(`Organization shown: "${orgText}"`);
    
    // Check if it's showing skeleton
    const hasSkeleton = !!document.querySelector('.animate-pulse');
    if (hasSkeleton) {
      console.log('⚠️  Organization switcher showing skeleton loader');
      results.warnings.push('Organization switcher showing skeleton');
    }
  }
  
  console.log('\n=== 3. QUICK ACTIONS & STATS ===\n');
  
  // Count cards
  const cards = document.querySelectorAll('[class*="Card"]');
  console.log(`Total cards found: ${cards.length}`);
  
  // Check stats
  const statsTexts = Array.from(document.querySelectorAll('p.text-3xl')).map(el => ({
    value: el.textContent,
    label: el.previousElementSibling?.textContent
  }));
  
  console.log('Stats found:');
  statsTexts.forEach(stat => {
    console.log(`  ${stat.label}: ${stat.value}`);
  });
  
  console.log('\n=== 4. TAB NAVIGATION ===\n');
  
  const tabs = ['debates', 'templates', 'library', 'active', 'ollama'];
  tabs.forEach(tab => {
    testElement(`[value="${tab}"]`, `Tab: ${tab}`);
  });
  
  console.log('\n=== 5. DEBATES DISPLAY ===\n');
  
  // Check current tab
  const activeTab = document.querySelector('[data-state="active"]');
  console.log(`Active tab: ${activeTab?.textContent || 'Unknown'}`);
  
  // Check debates display
  const loadingSpinner = document.querySelector('[class*="animate-spin"]');
  const noDebatesMsg = Array.from(document.querySelectorAll('h3')).find(h => 
    h.textContent?.includes('No debates yet')
  );
  const debateCards = document.querySelectorAll('[class*="CardTitle"]');
  
  console.log(`Loading spinner: ${loadingSpinner ? 'Yes' : 'No'}`);
  console.log(`No debates message: ${noDebatesMsg ? 'Yes' : 'No'}`);
  console.log(`Debate cards: ${debateCards.length}`);
  
  if (debateCards.length > 0) {
    console.log('Debate titles:');
    debateCards.forEach(card => {
      console.log(`  - ${card.textContent}`);
    });
  }
  
  console.log('\n=== 6. API STATUS ===\n');
  
  // Test APIs
  try {
    const debateRes = await fetch('/api/debate/resources?uri=debate://debates');
    const debates = await debateRes.json();
    console.log(`✅ Debate API: ${debates.debates?.length || 0} debates`);
    results.passed.push('Debate API');
  } catch (e) {
    console.log(`❌ Debate API: ${e.message}`);
    results.failed.push({ test: 'Debate API', error: e.message });
  }
  
  try {
    const llmRes = await fetch('/api/llm/providers');
    const providers = await llmRes.json();
    console.log(`✅ LLM API: ${providers.providers?.length || 0} providers`);
    results.passed.push('LLM API');
  } catch (e) {
    console.log(`❌ LLM API: ${e.message}`);
    results.failed.push({ test: 'LLM API', error: e.message });
  }
  
  console.log('\n=== 7. INTERACTIVE TESTS ===\n');
  
  // Test organization dropdown
  console.log('\n🔹 Testing organization dropdown...');
  const orgBtn = document.querySelector('[aria-expanded]');
  if (orgBtn && !document.querySelector('.animate-pulse')) {
    orgBtn.click();
    await new Promise(r => setTimeout(r, 500));
    
    const dropdown = document.querySelector('[class*="absolute"][class*="shadow-lg"]');
    if (dropdown) {
      console.log('✅ Dropdown opened');
      const orgOptions = dropdown.querySelectorAll('button');
      console.log(`  Organizations in dropdown: ${orgOptions.length - 2}`); // Minus Create and History buttons
      
      // Close dropdown
      document.body.click();
      await new Promise(r => setTimeout(r, 500));
    } else {
      console.log('❌ Dropdown did not open');
    }
  }
  
  // Test New Debate dialog
  console.log('\n🔹 Testing New Debate dialog...');
  const newDebateBtn = Array.from(document.querySelectorAll('button')).find(btn => 
    btn.textContent?.includes('New Debate') && !btn.disabled
  );
  
  if (newDebateBtn) {
    newDebateBtn.click();
    await new Promise(r => setTimeout(r, 1000));
    
    const dialog = document.querySelector('[role="dialog"]');
    if (dialog) {
      console.log('✅ Dialog opened');
      
      // Check form elements
      testElement('input[placeholder*="debate name"]', 'Debate name input');
      testElement('input[placeholder*="topic"]', 'Topic input');
      testElement('button', 'Add Participant button', el => el.textContent.includes('Add Participant'));
      
      // Close dialog
      const closeBtn = dialog.querySelector('button[aria-label*="Close"]');
      if (closeBtn) closeBtn.click();
      else document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      
      await new Promise(r => setTimeout(r, 500));
    } else {
      console.log('❌ Dialog did not open');
    }
  }
  
  console.log('\n=== TEST SUMMARY ===\n');
  
  const total = results.passed.length + results.failed.length;
  const passRate = total > 0 ? Math.round((results.passed.length / total) * 100) : 0;
  
  console.log(`Total tests: ${total}`);
  console.log(`Passed: ${results.passed.length}`);
  console.log(`Failed: ${results.failed.length}`);
  console.log(`Warnings: ${results.warnings.length}`);
  console.log(`Pass rate: ${passRate}%`);
  
  if (results.failed.length > 0) {
    console.log('\nFailed tests:');
    results.failed.forEach(f => {
      console.log(`  - ${f.test}: ${f.error}`);
    });
  }
  
  if (results.warnings.length > 0) {
    console.log('\nWarnings:');
    results.warnings.forEach(w => {
      console.log(`  - ${w}`);
    });
  }
  
  console.log('\n✅ Test complete!');
  
  // Return results for programmatic use
  return results;
}

// Auto-run the test
console.log('UI test loaded. Running automatically...\n');
runBrowserUITest().then(results => {
  window.testResults = results;
  console.log('\nTest results saved to window.testResults');
});