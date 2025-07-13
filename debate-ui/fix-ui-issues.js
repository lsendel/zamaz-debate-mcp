// Script to fix UI issues by resetting local storage and verifying state

async function fixUIIssues() {
  console.log('🔧 Starting UI fixes...\n');
  
  // 1. Clear problematic localStorage data
  console.log('1. Clearing localStorage...');
  localStorage.clear();
  
  // 2. Set default organization
  console.log('2. Setting default organization...');
  const defaultOrg = {
    id: 'default-org',
    name: 'Default Organization',
    createdAt: new Date().toISOString(),
    debateCount: 0
  };
  
  localStorage.setItem('organizations', JSON.stringify([defaultOrg]));
  localStorage.setItem('currentOrganizationId', defaultOrg.id);
  
  // 3. Test API endpoints
  console.log('\n3. Testing API endpoints...');
  
  try {
    // Test debate API
    const debateRes = await fetch('/api/debate/resources?uri=debate://debates');
    const debates = await debateRes.json();
    console.log(`✅ Debate API: ${debates.debates?.length || 0} debates found`);
    
    // Test LLM API
    const llmRes = await fetch('/api/llm/providers');
    const providers = await llmRes.json();
    console.log(`✅ LLM API: ${providers.providers?.length || 0} providers found`);
    
    // Test health API
    const healthRes = await fetch('/api/llm/health');
    const health = await healthRes.json();
    console.log(`✅ Health API: Status = ${health.status}`);
  } catch (error) {
    console.error('❌ API test failed:', error);
  }
  
  // 4. Check UI state
  console.log('\n4. Checking UI state...');
  
  // Check if org switcher is visible
  const orgSwitcher = document.querySelector('[aria-expanded]');
  console.log(`Organization Switcher: ${orgSwitcher ? '✅ Found' : '❌ Not found'}`);
  
  // Check if debates are loading
  const loadingSpinner = document.querySelector('[class*="animate-spin"]');
  console.log(`Loading Spinner: ${loadingSpinner ? '⚠️  Still loading' : '✅ Not loading'}`);
  
  // Check debate count
  const debateCards = document.querySelectorAll('[class*="CardTitle"]');
  console.log(`Debate Cards: ${debateCards.length} found`);
  
  // 5. Reload the page
  console.log('\n5. Reloading page in 2 seconds...');
  console.log('\n✅ Fixes applied! The page will reload automatically.');
  
  setTimeout(() => {
    window.location.reload();
  }, 2000);
}

// Instructions for use
console.log(`
=== UI Fix Script ===

To fix the UI issues, run this in the browser console:

1. Open the app at http://localhost:3001
2. Open browser DevTools (F12)
3. Go to the Console tab
4. Copy and paste this entire script
5. The page will reload automatically

Alternatively, run the fixUIIssues() function directly.
`);

// Export for manual execution
window.fixUIIssues = fixUIIssues;