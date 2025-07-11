import puppeteer, { Browser, Page } from 'puppeteer';

describe('Organization Dropdown Button Functionality', () => {
  let browser: Browser;
  let page: Page;
  const UI_URL = process.env.UI_URL || 'http://localhost:3000';

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: process.env.HEADLESS !== 'false',
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
      defaultViewport: { width: 1280, height: 720 }
    });
  });

  afterAll(async () => {
    await browser.close();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.goto(UI_URL, { waitUntil: 'networkidle0' });
    await new Promise(resolve => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    await page.close();
  });

  test('Create Organization button should open dialog and create new organization', async () => {
    // First, open the dropdown
    const dropdownButton = await page.$('button[aria-expanded]');
    expect(dropdownButton).toBeTruthy();
    
    await dropdownButton!.click();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Check dropdown is open
    const isExpanded = await page.$eval('button[aria-expanded]', el => el.getAttribute('aria-expanded'));
    expect(isExpanded).toBe('true');

    // Click "Create Organization" button
    const createOrgButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('Create Organization'));
    });
    
    expect(createOrgButton).toBeTruthy();
    await (createOrgButton as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Check if dialog opened
    const dialog = await page.$('[role="dialog"]');
    expect(dialog).toBeTruthy();

    // Check dialog content
    const dialogTitle = await page.$eval('[role="dialog"] h2', el => el.textContent);
    expect(dialogTitle).toContain('Create New Organization');

    // Fill in organization name
    const input = await page.$('#org-name');
    expect(input).toBeTruthy();
    await input!.type('Test Organization ' + Date.now());

    // Submit the form
    const submitButton = await page.evaluateHandle(() => {
      const dialog = document.querySelector('[role="dialog"]');
      if (!dialog) return null;
      const buttons = Array.from(dialog.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('Create Organization'));
    });
    
    expect(submitButton).toBeTruthy();
    await (submitButton as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Verify dialog closed
    const dialogAfter = await page.$('[role="dialog"]');
    expect(dialogAfter).toBeFalsy();

    // Verify organization was created by checking localStorage
    const organizations = await page.evaluate(() => {
      const orgs = localStorage.getItem('organizations');
      return orgs ? JSON.parse(orgs) : [];
    });
    
    expect(organizations.length).toBeGreaterThan(1);
    const latestOrg = organizations[organizations.length - 1];
    expect(latestOrg.name).toContain('Test Organization');
  });

  test('New Debate button should open create debate dialog', async () => {
    // Find and click the "New Debate" button
    const newDebateButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('New Debate'));
    });
    
    expect(newDebateButton).toBeTruthy();
    await (newDebateButton as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Check if dialog opened
    const dialog = await page.$('[role="dialog"]');
    expect(dialog).toBeTruthy();

    // Verify it's the create debate dialog
    const dialogContent = await page.$eval('[role="dialog"]', el => el.textContent);
    expect(dialogContent).toMatch(/create.*debate/i);
  });

  test('View History button should execute without errors', async () => {
    // First, open the dropdown
    const dropdownButton = await page.$('button[aria-expanded]');
    expect(dropdownButton).toBeTruthy();
    
    await dropdownButton!.click();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Find and click "View History" button
    const viewHistoryButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('View History'));
    });
    
    expect(viewHistoryButton).toBeTruthy();
    
    // Set up console listener to catch any errors
    const consoleMessages: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleMessages.push(msg.text());
      }
    });

    await (viewHistoryButton as any).click();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Check for console errors
    const errorMessages = consoleMessages.filter(msg => msg.includes('error'));
    expect(errorMessages.length).toBe(0);

    // Verify dropdown closed after clicking
    const isExpandedAfter = await page.$eval('button[aria-expanded]', el => el.getAttribute('aria-expanded'));
    expect(isExpandedAfter).toBe('false');
  });

  test('All dropdown buttons should be clickable and visible', async () => {
    // Open dropdown
    const dropdownButton = await page.$('button[aria-expanded]');
    await dropdownButton!.click();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Check all interactive buttons in dropdown
    const buttonInfo = await page.evaluate(() => {
      const dropdown = document.querySelector('[aria-expanded="true"]')?.parentElement?.querySelector('div.absolute');
      if (!dropdown) return [];
      
      const buttons = Array.from(dropdown.querySelectorAll('button'));
      return buttons.map(btn => ({
        text: btn.textContent?.trim() || '',
        isVisible: window.getComputedStyle(btn).display !== 'none',
        hasClickHandler: btn.onclick !== null || btn.hasAttribute('onclick'),
        computedStyles: {
          pointerEvents: window.getComputedStyle(btn).pointerEvents,
          cursor: window.getComputedStyle(btn).cursor,
          opacity: window.getComputedStyle(btn).opacity,
          visibility: window.getComputedStyle(btn).visibility
        }
      }));
    });

    console.log('Button info:', buttonInfo);

    // Verify all buttons are properly styled for interaction
    buttonInfo.forEach(btn => {
      expect(btn.isVisible).toBe(true);
      expect(btn.computedStyles.pointerEvents).not.toBe('none');
      expect(btn.computedStyles.visibility).toBe('visible');
      expect(parseFloat(btn.computedStyles.opacity)).toBeGreaterThan(0);
    });

    // Check that we have the expected buttons
    const buttonTexts = buttonInfo.map(b => b.text);
    expect(buttonTexts.some(text => text.includes('Create Organization'))).toBe(true);
    expect(buttonTexts.some(text => text.includes('View History'))).toBe(true);
  });
});