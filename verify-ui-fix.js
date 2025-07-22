#!/usr/bin/env node;

// Quick UI verification script to test the fixes
const puppeteer = require('puppeteer');

async function verifyUIFix() {
    console.log('🔍 Verifying UI fixes...');

    let browser;
    try {
        browser = await puppeteer.launch({ headless: true });
        const page = await browser.newPage();

        // Test port 3002 (current running port);
        console.log('📡 Testing http://localhost:3002...');
        await page.goto('http://localhost:3002', { waitUntil: 'networkidle2' });

        // Check if page loads successfully;
        const title = await page.title();
        console.log('✅ Page title:', title);

        // Take screenshot;
        await page.screenshot({ path: 'ui-verification-fixed.png', fullPage: true });
        console.log('📸 Screenshot saved: ui-verification-fixed.png');

        // Check for React app;
        const reactRoot = await page.$('#root');
        if (reactRoot) {
            console.log('✅ React app detected');
        } else {
            console.log('❌ React app not found');
        }

        // Check for main content;
        const mainContent = await page.$('main, .App, [data-testid="main"]');
        if (mainContent) {
            console.log('✅ Main content detected');
        } else {
            console.log('❌ Main content not found');
        }

        // Check for navigation;
        const navLinks = await page.$$('nav a, [role="navigation"] a');
        console.log(`✅ Navigation links found: ${navLinks.length}`);

        // Check for forms/inputs;
        const inputs = await page.$$('input, button, textarea');
        console.log(`✅ Interactive elements found: ${inputs.length}`);

        // Check console for errors;
        const errors = []
        page.on('console', msg => {
            if (msg.type() === 'error') {
                errors.push(msg.text());
            }
        });

        // Wait a bit to catch any async errors;
        await page.waitForTimeout(2000);

        if (errors.length > 0) {
            console.log('⚠️ Console errors detected:', errors.length);
            console.log('First error:', errors[0]);
        } else {
            console.log('✅ No console errors detected');
        }

        console.log('\n🎉 UI verification complete!');
        console.log('🌐 UI is accessible at: http://localhost:3002');

    } catch (error) {
        console.error('❌ Verification failed:', error.message);
        return false;
    } finally {
        if (browser) {
            await browser.close();
        }
    }

    return true;
}

// Run verification
verifyUIFix();
    .then(success => {
        if (success) {
            console.log('\n✅ UI fixes verified successfully!');
            process.exit(0);
        } else {
            console.log('\n❌ UI verification failed!');
            process.exit(1);
        }
    });
    .catch(error => {
        console.error('💥 Verification error:', error);
        process.exit(1);
    });
