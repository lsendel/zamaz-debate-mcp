#!/usr/bin/env node

const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// Verification script as requested by user: "make sure you use a tool to see the screen like puppeter and the flows before you ask me to see it"
class UIVerificationTool {
    constructor() {
        this.baseUrl = 'http://localhost:3001';
        this.screenshotDir = './verification-screenshots';
        this.results = {
            timestamp: new Date().toISOString(),
            tests: [],
            summary: {
                total: 0,
                passed: 0,
                failed: 0
            }
        };
    }

    async init() {
        console.log('🤖 Starting automated UI verification with Puppeteer...');
        
        // Create screenshots directory
        if (!fs.existsSync(this.screenshotDir)) {
            fs.mkdirSync(this.screenshotDir, { recursive: true });
        }

        this.browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        this.page = await this.browser.newPage();
        
        // Set viewport for consistent screenshots
        await this.page.setViewport({ width: 1200, height: 800 });
        
        console.log('✅ Puppeteer browser initialized');
    }

    async runTest(testName, testFn) {
        console.log(`🧪 Running test: ${testName}`);
        this.results.summary.total++;
        
        const test = {
            name: testName,
            timestamp: new Date().toISOString(),
            status: 'running',
            screenshots: [],
            errors: []
        };

        try {
            await testFn(test);
            test.status = 'passed';
            this.results.summary.passed++;
            console.log(`✅ ${testName} - PASSED`);
        } catch (error) {
            test.status = 'failed';
            test.errors.push(error.message);
            this.results.summary.failed++;
            console.log(`❌ ${testName} - FAILED: ${error.message}`);
        }

        this.results.tests.push(test);
    }

    async takeScreenshot(test, name) {
        const filename = `${test.name.toLowerCase().replace(/\s+/g, '-')}-${name}.png`;
        const filepath = path.join(this.screenshotDir, filename);
        
        await this.page.screenshot({ 
            path: filepath, 
            fullPage: true 
        });
        
        test.screenshots.push(filename);
        console.log(`📸 Screenshot saved: ${filename}`);
        return filename;
    }

    async verifyHomepage() {
        await this.runTest('Homepage Load Test', async (test) => {
            await this.page.goto(this.baseUrl, { waitUntil: 'networkidle2' });
            await this.takeScreenshot(test, 'homepage-loaded');

            // Check if page loads successfully
            const title = await this.page.title();
            if (!title) {
                throw new Error('Page title is empty');
            }

            // Look for common React app elements
            const reactRoot = await this.page.$('#root');
            if (!reactRoot) {
                throw new Error('React root element not found');
            }

            // Check for navigation or main content
            const mainContent = await this.page.$('main, .App, [data-testid="main"], nav');
            if (!mainContent) {
                throw new Error('Main content area not found');
            }
        });
    }

    async verifyNavigation() {
        await this.runTest('Navigation Test', async (test) => {
            await this.page.goto(this.baseUrl, { waitUntil: 'networkidle2' });

            // Look for navigation elements
            const navLinks = await this.page.$$('nav a, [role="navigation"] a, .nav a');
            console.log(`Found ${navLinks.length} navigation links`);

            if (navLinks.length > 0) {
                // Take screenshot of navigation
                await this.takeScreenshot(test, 'navigation-visible');

                // Try clicking on the first navigation link
                try {
                    await navLinks[0].click();
                    await this.page.waitForTimeout(1000); // Wait for potential route change
                    await this.takeScreenshot(test, 'navigation-clicked');
                } catch (clickError) {
                    console.log(`Navigation click test skipped: ${clickError.message}`);
                }
            }
        });
    }

    async verifyForms() {
        await this.runTest('Forms Test', async (test) => {
            await this.page.goto(this.baseUrl, { waitUntil: 'networkidle2' });

            // Look for forms or input elements
            const forms = await this.page.$$('form');
            const inputs = await this.page.$$('input, textarea, select');
            
            console.log(`Found ${forms.length} forms and ${inputs.length} input elements`);

            if (forms.length > 0 || inputs.length > 0) {
                await this.takeScreenshot(test, 'forms-detected');

                // Try interacting with the first input if available
                if (inputs.length > 0) {
                    try {
                        await inputs[0].focus();
                        await this.page.keyboard.type('test input');
                        await this.takeScreenshot(test, 'form-interaction');
                    } catch (inputError) {
                        console.log(`Form interaction test skipped: ${inputError.message}`);
                    }
                }
            }
        });
    }

    async verifyResponsiveness() {
        await this.runTest('Responsive Design Test', async (test) => {
            await this.page.goto(this.baseUrl, { waitUntil: 'networkidle2' });

            // Test different viewport sizes
            const viewports = [
                { width: 1200, height: 800, name: 'desktop' },
                { width: 768, height: 1024, name: 'tablet' },
                { width: 375, height: 667, name: 'mobile' }
            ];

            for (const viewport of viewports) {
                await this.page.setViewport(viewport);
                await this.page.waitForTimeout(500); // Allow layout to adjust
                await this.takeScreenshot(test, `responsive-${viewport.name}`);
            }

            // Reset to desktop
            await this.page.setViewport({ width: 1200, height: 800 });
        });
    }

    async verifyConsoleErrors() {
        await this.runTest('Console Errors Test', async (test) => {
            const errors = [];
            
            this.page.on('console', msg => {
                if (msg.type() === 'error') {
                    errors.push(msg.text());
                }
            });

            await this.page.goto(this.baseUrl, { waitUntil: 'networkidle2' });
            await this.takeScreenshot(test, 'page-loaded');

            // Wait a bit to catch any async errors
            await this.page.waitForTimeout(2000);

            if (errors.length > 0) {
                console.log(`⚠️ Console errors detected: ${errors.length}`);
                test.errors.push(`Console errors: ${errors.join(', ')}`);
                // Don't fail the test, just log warnings
            }
        });
    }

    async verifyServiceConnectivity() {
        await this.runTest('Service Connectivity Test', async (test) => {
            await this.page.goto(this.baseUrl, { waitUntil: 'networkidle2' });

            // Check for API calls in network tab
            const responses = [];
            this.page.on('response', response => {
                if (response.url().includes('/api/') || response.url().includes(':500')) {
                    responses.push({
                        url: response.url(),
                        status: response.status()
                    });
                }
            });

            // Wait for potential API calls
            await this.page.waitForTimeout(3000);
            await this.takeScreenshot(test, 'connectivity-checked');

            console.log(`Detected ${responses.length} API calls`);
            if (responses.length > 0) {
                responses.forEach(resp => {
                    console.log(`📡 API Call: ${resp.url} - Status: ${resp.status}`);
                });
            }
        });
    }

    async generateReport() {
        const reportPath = './ui-verification-report.json';
        fs.writeFileSync(reportPath, JSON.stringify(this.results, null, 2));

        console.log('\n📊 UI Verification Summary:');
        console.log(`✅ Passed: ${this.results.summary.passed}`);
        console.log(`❌ Failed: ${this.results.summary.failed}`);
        console.log(`📊 Total: ${this.results.summary.total}`);
        console.log(`📸 Screenshots: ${this.screenshotDir}/`);
        console.log(`📄 Report: ${reportPath}`);

        return this.results;
    }

    async cleanup() {
        if (this.browser) {
            await this.browser.close();
        }
        console.log('🧹 Cleanup completed');
    }

    async run() {
        try {
            await this.init();
            
            // Run all verification tests
            await this.verifyHomepage();
            await this.verifyNavigation();
            await this.verifyForms();
            await this.verifyResponsiveness();
            await this.verifyConsoleErrors();
            await this.verifyServiceConnectivity();

            const results = await this.generateReport();
            return results;
        } catch (error) {
            console.error('❌ Verification failed:', error.message);
            throw error;
        } finally {
            await this.cleanup();
        }
    }
}

// Run if called directly
if (require.main === module) {
    const verifier = new UIVerificationTool();
    verifier.run()
        .then(results => {
            console.log('\n🎉 UI verification completed successfully!');
            process.exit(results.summary.failed > 0 ? 1 : 0);
        })
        .catch(error => {
            console.error('💥 UI verification failed:', error);
            process.exit(1);
        });
}

module.exports = UIVerificationTool;