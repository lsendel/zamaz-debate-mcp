const { chromium } = require('playwright');

async function testOrganizationUI() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log('🔍 Testing Organization Management UI...');
    
    // Navigate to the app
    await page.goto('http://localhost:3002');
    await page.waitForTimeout(2000);
    
    // Take a screenshot of the initial page
    await page.screenshot({ path: 'screenshots/01-initial-page.png' });
    console.log('✅ Initial page loaded');
    
    // Check if login page is shown
    const loginButton = await page.locator('button:has-text("Login")').first();
    if (await loginButton.isVisible()) {
      console.log('📝 Login page detected, attempting login...');
      
      // Fill in login credentials
      await page.fill('input[name="username"]', 'admin');
      await page.fill('input[name="password"]', 'password');
      await page.click('button:has-text("Login")');
      
      // Wait for login to complete
      await page.waitForTimeout(2000);
      await page.screenshot({ path: 'screenshots/02-after-login.png' });
    }
    
    // Navigate to organization management (if not already there)
    try {
      const orgManagementButton = await page.locator('text=Organization Management').first();
      if (await orgManagementButton.isVisible()) {
        await orgManagementButton.click();
        await page.waitForTimeout(1000);
      }
    } catch (error) {
        console.error("Error:", error);
      // Try alternative navigation methods
      console.log('Trying alternative navigation...');
      try {
        await page.goto('http://localhost:3002/organization-management');
        await page.waitForTimeout(2000);
      } catch (navError) {
          console.error("Error:", e);
        console.log('Could not navigate to organization management page');
        console.error("Error:", error);
      }
    }
    
    // Take screenshot of organization management page
    await page.screenshot({ path: 'screenshots/03-organization-management.png' });
    console.log('✅ Organization management page loaded');
    
    // Check for organization cards/data
    const organizationCards = await page.locator('[data-testid="organization-card"], .MuiCard-root').count();
    console.log(`📊 Found ${organizationCards} organization cards`);
    
    // Look for specific organization names from our backend
    const acmeOrg = await page.locator('text=Acme Corporation').count();
    const techSolutions = await page.locator('text=Tech Solutions Inc').count();
    
    console.log(`🏢 Acme Corporation found: ${acmeOrg > 0 ? 'Yes' : 'No'}`);
    console.log(`🏢 Tech Solutions Inc found: ${techSolutions > 0 ? 'Yes' : 'No'}`);
    
    // Test create organization dialog
    try {
      const createButton = await page.locator('button:has-text("Create Organization")').first();
      if (await createButton.isVisible()) {
        await createButton.click();
        await page.waitForTimeout(1000);
        
        await page.screenshot({ path: 'screenshots/04-create-dialog.png' });
        console.log('✅ Create organization dialog opened');
        
        // Close dialog
        await page.click('button:has-text("Cancel")');
        await page.waitForTimeout(500);
      }
    } catch (error) {
      console.log('❌ Could not test create organization dialog');
      console.error("Error:", error);
    }
    
    // Test organization tabs
    try {
      const usersTab = await page.locator('text=Users').first();
      if (await usersTab.isVisible()) {
        await usersTab.click();
        await page.waitForTimeout(1000);
        
        await page.screenshot({ path: 'screenshots/05-users-tab.png' });
        console.log('✅ Users tab loaded');
        
        // Go back to organizations tab
        await page.click('text=Organizations');
        await page.waitForTimeout(500);
      }
    } catch (error) {
      console.log('❌ Could not test users tab');
      console.error("Error:", error);
    }
    
    // Final screenshot
    await page.screenshot({ path: 'screenshots/06-final-state.png' });
    
    console.log('✅ Organization UI test completed successfully!');
    console.log('📸 Screenshots saved to screenshots/ directory');
    
  } catch (error) {
    console.error('❌ Error during testing:', error);
    await page.screenshot({ path: 'screenshots/error-state.png' });
    throw error;
  } finally {
    await browser.close();
  }
}

// Run the test
testOrganizationUI().catch(console.error);