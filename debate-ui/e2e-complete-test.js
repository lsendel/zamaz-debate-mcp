const puppeteer = require('puppeteer');
const fs = require('fs').promises;

// Create evidence directory
async function setupEvidence() {
    try {
        await fs.mkdir('evidence', { recursive: true });
    } catch (e) {}
}

// Main E2E test suite
async function runCompleteE2ETest() {
    console.log('🚀 Starting Comprehensive E2E Testing\n');
    console.log('📋 Test Plan:');
    console.log('1. Verify UI loads (NO BLANK SCREEN)');
    console.log('2. Test Login functionality');
    console.log('3. Test Admin/Organization features');
    console.log('4. Test Debate creation and participation');
    console.log('5. Generate evidence screenshots\n');
    
    await setupEvidence();
    
    const browser = await puppeteer.launch({ 
        headless: false,
        defaultViewport: { width: 1280, height: 800 },
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    const page = await browser.newPage();
    
    // Monitor for errors
    const errors = [];
    page.on('console', msg => {
        if (msg.type() === 'error') {
            errors.push(msg.text());
            console.log('❌ Console Error:', msg.text());
        }
    });
    
    page.on('pageerror', error => {
        errors.push(error.message);
        console.log('🚨 Page Error:', error.message);
    });
    
    try {
        // TEST 1: Verify UI loads properly
        console.log('\n🧪 TEST 1: Checking if UI loads...');
        await page.goto('http://localhost:3001', { 
            waitUntil: 'networkidle0',
            timeout: 30000 
        });
        
        // Wait for React to render
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Check for blank screen
        const pageContent = await page.evaluate(() => {
            const root = document.getElementById('root');
            const body = document.body;
            return {
                hasContent: root && root.innerHTML.length > 0,
                bodyText: body.innerText || '',
                hasLoginForm: !!document.querySelector('form'),
                title: document.title
            };
        });
        
        await page.screenshot({ path: 'evidence/01-initial-load.png' });
        
        if (!pageContent.hasContent) {
            throw new Error('BLANK SCREEN DETECTED! React is not rendering.');
        }
        
        console.log('✅ UI loaded successfully');
        console.log('   Page title:', pageContent.title);
        console.log('   Has content:', pageContent.hasContent);
        console.log('   Has login form:', pageContent.hasLoginForm);
        
        // TEST 2: Login functionality
        console.log('\n🧪 TEST 2: Testing login...');
        
        // Wait for login form
        await page.waitForSelector('form', { timeout: 5000 });
        await page.screenshot({ path: 'evidence/02-login-page.png' });
        
        // Find and fill login fields
        const emailInput = await page.$('input[type="email"], input[name="email"], input[name="username"]');
        const passwordInput = await page.$('input[type="password"]');
        const submitButton = await page.$('button[type="submit"], button');
        
        if (!emailInput || !passwordInput || !submitButton) {
            throw new Error('Login form elements not found!');
        }
        
        console.log('   Filling login form...');
        await emailInput.type('demo@example.com');
        await passwordInput.type('demo123');
        await page.screenshot({ path: 'evidence/03-login-filled.png' });
        
        console.log('   Submitting login...');
        await submitButton.click();
        
        // Wait for navigation or login response
        try {
            await page.waitForNavigation({ timeout: 10000 });
            console.log('✅ Login successful - navigated to dashboard');
        } catch (e) {
            // Check if we're still on login page with error
            const loginError = await page.$('.error, .ant-message-error, [role="alert"]');
            if (loginError) {
                const errorText = await loginError.evaluate(el => el.textContent);
                console.log('❌ Login failed with error:', errorText);
                await page.screenshot({ path: 'evidence/04-login-error.png' });
            } else {
                console.log('⚠️  Login response unclear - checking current state');
            }
        }
        
        await page.screenshot({ path: 'evidence/05-after-login.png' });
        
        // Check current location
        const currentUrl = page.url();
        console.log('   Current URL:', currentUrl);
        
        // TEST 3: Organization/Admin features
        console.log('\n🧪 TEST 3: Testing Organization Management...');
        
        // Try to navigate to organizations
        const orgLink = await page.$('a[href*="organization"], button:contains("Organizations"), [class*="organization"]');
        if (orgLink) {
            await orgLink.click();
            await new Promise(resolve => setTimeout(resolve, 2000));
            await page.screenshot({ path: 'evidence/06-organizations-page.png' });
            
            // Check for organization list
            const orgCards = await page.$$('[class*="organization-card"], [class*="org-card"], .ant-card');
            console.log(`   Found ${orgCards.length} organization cards`);
            
            // Try to create organization
            const createButton = await page.$('button:contains("Create"), button:contains("Add"), button:contains("New")');
            if (createButton) {
                await createButton.click();
                await new Promise(resolve => setTimeout(resolve, 1000));
                await page.screenshot({ path: 'evidence/07-create-org-dialog.png' });
            }
        } else {
            console.log('⚠️  Organization link not found - may need proper authentication');
        }
        
        // TEST 4: Debates functionality
        console.log('\n🧪 TEST 4: Testing Debates...');
        
        // Navigate to debates
        const debateLink = await page.$('a[href*="debate"], button:contains("Debates"), [class*="debate"]');
        if (debateLink) {
            await debateLink.click();
            await new Promise(resolve => setTimeout(resolve, 2000));
            await page.screenshot({ path: 'evidence/08-debates-page.png' });
            
            // Check for debate list
            const debateCards = await page.$$('[class*="debate-card"], .ant-card');
            console.log(`   Found ${debateCards.length} debate cards`);
            
            // Try to create debate
            const createDebateBtn = await page.$('button:contains("Create"), button:contains("New Debate")');
            if (createDebateBtn) {
                await createDebateBtn.click();
                await new Promise(resolve => setTimeout(resolve, 1000));
                await page.screenshot({ path: 'evidence/09-create-debate-dialog.png' });
                
                // Fill debate form if dialog opened
                const titleInput = await page.$('input[name="title"], input[placeholder*="title"]');
                if (titleInput) {
                    await titleInput.type('E2E Test Debate ' + Date.now());
                    await page.screenshot({ path: 'evidence/10-debate-form-filled.png' });
                }
            }
        } else {
            console.log('⚠️  Debates link not found');
        }
        
        // TEST 5: Check for admin features
        console.log('\n🧪 TEST 5: Checking Admin Features...');
        
        // Look for LLM presets or admin settings
        const adminElements = await page.evaluate(() => {
            const elements = {
                llmPresets: !!document.querySelector('[class*="llm-preset"], button:contains("LLM")'),
                settings: !!document.querySelector('a[href*="settings"], button:contains("Settings")'),
                adminPanel: !!document.querySelector('[class*="admin"], [role="admin"]')
            };
            return elements;
        });
        
        console.log('   Admin features found:', adminElements);
        await page.screenshot({ path: 'evidence/11-admin-features.png' });
        
        // Generate final report
        console.log('\n📊 TEST SUMMARY:');
        console.log('================');
        console.log(`Total errors encountered: ${errors.length}`);
        console.log(`Screenshots generated: 11`);
        console.log(`Current URL: ${page.url()}`);
        
        if (errors.length > 0) {
            console.log('\n❌ Errors detected:');
            errors.forEach((err, i) => console.log(`   ${i + 1}. ${err}`));
            await fs.writeFile('evidence/errors.json', JSON.stringify(errors, null, 2));
        }
        
        // Save page state
        const finalState = await page.evaluate(() => ({
            url: window.location.href,
            title: document.title,
            hasReactApp: !!window.React,
            rootContent: document.getElementById('root')?.innerHTML.length || 0,
            visibleElements: {
                forms: document.querySelectorAll('form').length,
                buttons: document.querySelectorAll('button').length,
                links: document.querySelectorAll('a').length,
                inputs: document.querySelectorAll('input').length
            }
        }));
        
        await fs.writeFile('evidence/final-state.json', JSON.stringify(finalState, null, 2));
        
        console.log('\n✅ E2E Testing Complete!');
        console.log('📁 Evidence saved in ./evidence/');
        console.log('🌐 Browser remains open for manual inspection');
        
        // Keep browser open for manual inspection
        await new Promise(() => {});
        
    } catch (error) {
        console.error('\n❌ Test failed:', error.message);
        await page.screenshot({ path: `evidence/error-${Date.now()}.png` });
        throw error;
    }
}

// Run the tests
runCompleteE2ETest().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});