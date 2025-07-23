const puppeteer = require('puppeteer');

async function checkUI() {
    let browser;
    try {
        console.log('🚀 Starting comprehensive UI check with Puppeteer...\n');
        
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        
        const page = await browser.newPage();
        
        // Set viewport
        await page.setViewport({ width: 1280, height: 800 });
        
        // Enable console logging
        page.on('console', msg => console.log('Browser console:', msg.text()));
        page.on('pageerror', error => console.log('Page error:', error.message));
        
        // Navigate to the UI
        console.log('📍 Navigating to http://localhost:3001...');
        const response = await page.goto('http://localhost:3001', { 
            waitUntil: 'networkidle0', 
            timeout: 30000 
        });
        
        console.log('✅ Page loaded with status:', response.status());
        
        // Get page info
        const pageTitle = await page.title();
        const pageURL = page.url();
        console.log('📄 Page Title:', pageTitle);
        console.log('🔗 Current URL:', pageURL);
        
        // Take initial screenshot
        await page.screenshot({ path: 'ui-check-1-initial.png', fullPage: true });
        console.log('📸 Initial screenshot saved as ui-check-1-initial.png\n');
        
        // Check what's visible on the page
        console.log('🔍 Checking page elements...');
        
        // Look for common UI elements
        const elements = {
            'Login form': 'form',
            'Email input': 'input[type="email"], input[name="email"], input[placeholder*="email" i]',
            'Password input': 'input[type="password"], input[name="password"]',
            'Username input': 'input[name="username"], input[placeholder*="username" i]',
            'Submit button': 'button[type="submit"], button:contains("Login"), button:contains("Sign in")',
            'Any button': 'button',
            'Any form': 'form',
            'Navigation': 'nav',
            'Header': 'header',
            'Main content': 'main, #root, .app, .App',
            'Error messages': '.error, .alert, [role="alert"]'
        };
        
        for (const [name, selector] of Object.entries(elements)) {
            try {
                const element = await page.$(selector);
                console.log(`  ${element ? '✅' : '❌'} ${name}: ${element ? 'Found' : 'Not found'}`);
            } catch (e) {
                console.log(`  ❌ ${name}: Error checking`);
            }
        }
        
        // Get all visible text
        console.log('\n📝 Visible text on page:');
        const bodyText = await page.evaluate(() => {
            const text = document.body.innerText || document.body.textContent || '';
            return text.trim();
        });
        console.log(bodyText ? bodyText.substring(0, 500) + (bodyText.length > 500 ? '...' : '') : '(No visible text found)');
        
        // Check for any inputs
        const allInputs = await page.$$eval('input', inputs => 
            inputs.map(input => ({
                type: input.type,
                name: input.name,
                placeholder: input.placeholder,
                id: input.id,
                visible: input.offsetParent !== null
            }))
        );
        
        if (allInputs.length > 0) {
            console.log('\n📋 Found inputs:');
            allInputs.forEach((input, i) => {
                console.log(`  ${i + 1}. Type: ${input.type}, Name: ${input.name}, Placeholder: ${input.placeholder}, Visible: ${input.visible}`);
            });
        }
        
        // Check for buttons
        const allButtons = await page.$$eval('button', buttons => 
            buttons.map(button => ({
                text: button.innerText,
                type: button.type,
                disabled: button.disabled,
                visible: button.offsetParent !== null
            }))
        );
        
        if (allButtons.length > 0) {
            console.log('\n🔘 Found buttons:');
            allButtons.forEach((button, i) => {
                console.log(`  ${i + 1}. Text: "${button.text}", Type: ${button.type}, Disabled: ${button.disabled}, Visible: ${button.visible}`);
            });
        }
        
        // Check page HTML structure
        const htmlStructure = await page.evaluate(() => {
            const getStructure = (element, depth = 0) => {
                if (depth > 3) return '';
                const indent = '  '.repeat(depth);
                let structure = `${indent}<${element.tagName.toLowerCase()}`;
                if (element.id) structure += ` id="${element.id}"`;
                if (element.className) structure += ` class="${element.className}"`;
                structure += '>\n';
                
                if (element.children.length === 0 && element.textContent) {
                    structure += `${indent}  ${element.textContent.trim().substring(0, 50)}\n`;
                }
                
                for (const child of element.children) {
                    structure += getStructure(child, depth + 1);
                }
                
                return structure;
            };
            
            return getStructure(document.body, 0).substring(0, 1000);
        });
        
        console.log('\n🏗️ HTML Structure:');
        console.log(htmlStructure);
        
        // Check for React app
        const hasReactRoot = await page.$('#root') !== null;
        const reactDevTools = await page.evaluate(() => {
            return window.React || window.__REACT_DEVTOOLS_GLOBAL_HOOK__;
        });
        
        console.log('\n⚛️ React Detection:');
        console.log('  Root element:', hasReactRoot ? 'Found' : 'Not found');
        console.log('  React detected:', reactDevTools ? 'Yes' : 'No');
        
        // Try to interact if login form is present
        const emailInput = await page.$('input[type="email"], input[name="email"], input[name="username"]');
        if (emailInput) {
            console.log('\n🔐 Attempting to fill login form...');
            await emailInput.type('demo@example.com');
            await page.screenshot({ path: 'ui-check-2-filled.png' });
            console.log('📸 Screenshot with filled form saved as ui-check-2-filled.png');
        }
        
        console.log('\n✅ UI check completed successfully!');
        console.log('🌐 You can access the UI at: http://localhost:3001');
        
    } catch (error) {
        console.error('\n❌ Error during UI check:', error.message);
        if (error.message.includes('net::ERR_CONNECTION_REFUSED')) {
            console.error('The UI server is not running. Please start it with "npm run dev"');
        }
    } finally {
        if (browser) {
            await browser.close();
        }
    }
}

checkUI();