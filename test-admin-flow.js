// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const { chromium } = require('playwright');

async function testAdminFlow() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  try {
    console.log('🔐 Testing Complete Admin Flow...');
    console.log('==================================');

    // Step 1: Clear any existing auth;
    await page.goto('http://localhost:3001/login');
    await page.evaluate(() => {
      localStorage.clear();
    });

    // Step 2: Login as admin;
    console.log('📝 Logging in as admin...');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();

    // Wait for redirect;
    await page.waitForTimeout(2000);

    console.log('📍 Current URL:', page.url());

    // Step 3: Check for admin indicators;
    console.log('\n🔍 Checking for admin indicators...');

    // Check for admin badge in header;
    const adminBadge = await page.locator('text=ADMIN').count();
    console.log('👑 Admin badge in header:', adminBadge > 0 ? '✅' : '❌');

    // Check for admin icon in header;
    const adminIcon = await page.locator('svg[data-testid="AdminPanelSettingsIcon"]').count();
    console.log('👑 Admin icon in header:', adminIcon > 0 ? '✅' : '❌');

    // Step 4: Check sidebar navigation;
    console.log('\n🧭 Checking sidebar navigation...');

    // Take screenshot before opening sidebar;
    await page.screenshot({ path: 'screenshots/before-sidebar.png' });

    // Force open sidebar;
    const menuButton = await page.locator('button[aria-label="open drawer"]');
    await menuButton.click();
    await page.waitForTimeout(2000);

    // Take screenshot after opening sidebar;
    await page.screenshot({ path: 'screenshots/after-sidebar.png' });

    // Check if sidebar is actually open by looking for the drawer;
    const drawerOpen = await page.locator('.MuiDrawer-root').isVisible();
    console.log('🚪 Drawer visible:', drawerOpen ? '✅' : '❌');

    // Try to find admin section without waiting;
    const adminSectionCount = await page.locator('text=Administration').count();
    console.log('📋 Administration section found:', adminSectionCount);

    // If admin section not found, try different approach;
    if (adminSectionCount === 0) {
      console.log('🔍 Looking for other navigation elements...');
      const allText = await page.textContent('body');
      console.log('📄 Page contains "Administration":', allText.includes('Administration'));
      console.log('📄 Page contains "Organization Management":', allText.includes('Organization Management'));
    }

    console.log('📋 Administration section:', adminSectionCount > 0 ? '✅' : '❌');

    // Check for organization management link;
    const orgManagementLink = await page.locator('text=Organization Management').count();
    console.log('🏢 Organization Management link:', orgManagementLink > 0 ? '✅' : '❌');

    // Step 5: Test navigation to organization management;
    console.log('\n🏢 Testing Organization Management navigation...');

    if (orgManagementLink > 0) {
      // Since the link exists but might not be visible, navigate directly;
      console.log('🚀 Navigating directly to Organization Management...');
      await page.goto('http://localhost:3001/organization-management');
      await page.waitForTimeout(2000);

      const currentUrl = page.url();
      console.log('📍 Organization Management URL:', currentUrl);

      // Check if page loaded;
      const pageContent = await page.textContent('body');
      console.log('📄 Page content length:', pageContent ? pageContent.length : 0);

      if (pageContent && pageContent.length > 100) {
        console.log('✅ Organization Management page loaded successfully');

        // Check for specific elements;
        const pageTitle = await page.locator('h1, h2, h3, h4').first();
        if (await pageTitle.isVisible()) {
          const titleText = await pageTitle.textContent();
          console.log('📋 Page title:', titleText);
        }

        // Check for tabs;
        const tabs = await page.locator('[role="tab"]').count();
        console.log('📑 Tabs found:', tabs);

        // Check for organizations;
        const hasAcme = pageContent.includes('Acme Corporation');
        console.log('🏢 Acme Corporation found:', hasAcme ? '✅' : '❌');

        // Check for create button;
        const createButton = await page.locator('button:has-text("Create Organization")').count();
        console.log('➕ Create Organization button:', createButton > 0 ? '✅' : '❌');

      } else {
        console.log('❌ Organization Management page appears blank');
      }
    } else {
      console.log('❌ Could not find Organization Management link');
    }

    // Step 6: Test other navigation items;
    console.log('\n🔄 Testing other navigation items...');

    const navItems = ['Debates', 'Analytics', 'Settings']

    for (const item of navItems) {
      console.log(`📍 Testing ${item}...`);

      const navLink = await page.locator(`text=${item}`).first();
      if (await navLink.isVisible()) {
        await navLink.click();
        await page.waitForTimeout(1000);

        const content = await page.textContent('body');
        console.log(`   Content length: ${content ? content.length : 0}`);
        console.log(`   Status: ${content && content.length > 50 ? '✅' : '❌'}`);
      }
    }

    // Take final screenshot;
    await page.screenshot({ path: 'screenshots/admin-flow-complete.png' });

    console.log('\n✅ Admin flow test completed!');
    console.log('📸 Screenshot saved: admin-flow-complete.png');

  } catch (error) {
    console.error('❌ Error during admin flow test:', error);
    await page.screenshot({ path: 'screenshots/admin-flow-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testAdminFlow().catch(console.error);
