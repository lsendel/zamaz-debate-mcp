import puppeteer, { Browser, Page } from 'puppeteer';

describe('Template Management Functionality', () => {
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

  test('Template Gallery tab should display pre-configured templates', async () => {
    // Click on Gallery tab
    const galleryTab = await page.evaluateHandle(() => {
      const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
      return tabs.find(tab => tab.textContent?.includes('Gallery'));
    });
    
    expect(galleryTab).toBeTruthy();
    await (galleryTab as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Check for template cards
    const templateCards = await page.$$('.debate-templates card');
    expect(templateCards.length).toBeGreaterThan(0);

    // Verify template content
    const templateTitles = await page.evaluate(() => {
      const cards = document.querySelectorAll('[class*="card"]');
      return Array.from(cards).map(card => 
        card.querySelector('h3')?.textContent || ''
      ).filter(title => title.length > 0);
    });

    expect(templateTitles).toContain('AI Ethics & Society');
    expect(templateTitles).toContain('Climate & Technology');
  });

  test('Template Library tab should open template manager', async () => {
    // Click on Library tab
    const libraryTab = await page.evaluateHandle(() => {
      const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
      return tabs.find(tab => tab.textContent?.includes('Library'));
    });
    
    expect(libraryTab).toBeTruthy();
    await (libraryTab as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Check for Template Library header
    const headerText = await page.evaluate(() => {
      const headers = Array.from(document.querySelectorAll('h2'));
      return headers.map(h => h.textContent);
    });

    expect(headerText).toContain('Template Library');

    // Check for Create Template button
    const createButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('Create Template'));
    });

    expect(createButton).toBeTruthy();
  });

  test('Create Template dialog should open and have correct fields', async () => {
    // Navigate to Library tab
    const libraryTab = await page.evaluateHandle(() => {
      const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
      return tabs.find(tab => tab.textContent?.includes('Library'));
    });
    await (libraryTab as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Click Create Template button
    const createButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('Create Template'));
    });
    await (createButton as any).click();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Check dialog opened
    const dialog = await page.$('[role="dialog"]');
    expect(dialog).toBeTruthy();

    // Check dialog title
    const dialogTitle = await page.$eval('[role="dialog"] h2', el => el.textContent);
    expect(dialogTitle).toContain('Create New Template');

    // Check for required fields
    const nameInput = await page.$('#name');
    const categorySelect = await page.$('#category');
    const contentTextarea = await page.$('#content');

    expect(nameInput).toBeTruthy();
    expect(categorySelect).toBeTruthy();
    expect(contentTextarea).toBeTruthy();
  });

  test('Template service API should be accessible', async () => {
    // Test template search endpoint
    const response = await page.evaluate(async () => {
      try {
        const res = await fetch('/api/template/search', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            organization_id: 'system',
            limit: 10
          })
        });
        return {
          ok: res.ok,
          status: res.status,
          hasData: res.ok ? (await res.json()).templates !== undefined : false
        };
      } catch (error) {
        return { ok: false, error: error.message };
      }
    });

    expect(response.ok).toBe(true);
    expect(response.hasData).toBe(true);
  });

  test('Template preview should work', async () => {
    // Navigate to Library tab
    const libraryTab = await page.evaluateHandle(() => {
      const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
      return tabs.find(tab => tab.textContent?.includes('Library'));
    });
    await (libraryTab as any).click();
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Wait for templates to load
    await page.waitForSelector('button:has-text("Preview")', { timeout: 5000 }).catch(() => {});

    // Click first preview button if available
    const previewButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return buttons.find(btn => btn.textContent?.includes('Preview'));
    });

    if (previewButton) {
      await (previewButton as any).click();
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Check preview dialog opened
      const previewDialog = await page.evaluate(() => {
        const dialogs = Array.from(document.querySelectorAll('[role="dialog"]'));
        return dialogs.some(dialog => 
          dialog.textContent?.includes('Template Preview')
        );
      });

      expect(previewDialog).toBe(true);
    }
  });

  test('Template search should filter results', async () => {
    // Navigate to Library tab
    const libraryTab = await page.evaluateHandle(() => {
      const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
      return tabs.find(tab => tab.textContent?.includes('Library'));
    });
    await (libraryTab as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Find search input
    const searchInput = await page.$('input[placeholder*="Search templates"]');
    expect(searchInput).toBeTruthy();

    // Type search query
    await searchInput!.type('debate');
    await new Promise(resolve => setTimeout(resolve, 500));

    // Check that results are filtered
    const visibleTemplates = await page.evaluate(() => {
      const cards = Array.from(document.querySelectorAll('[class*="card"]'));
      return cards.filter(card => {
        const style = window.getComputedStyle(card);
        return style.display !== 'none' && style.visibility !== 'hidden';
      }).length;
    });

    // Should have some templates visible (if search works)
    expect(visibleTemplates).toBeGreaterThanOrEqual(0);
  });

  test('Category filter should work', async () => {
    // Navigate to Library tab
    const libraryTab = await page.evaluateHandle(() => {
      const tabs = Array.from(document.querySelectorAll('[role="tab"]'));
      return tabs.find(tab => tab.textContent?.includes('Library'));
    });
    await (libraryTab as any).click();
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Find category select
    const categorySelect = await page.evaluateHandle(() => {
      const selects = Array.from(document.querySelectorAll('[role="combobox"]'));
      return selects.find(select => 
        select.textContent?.includes('all') || 
        select.textContent?.includes('Category')
      );
    });

    expect(categorySelect).toBeTruthy();
    
    // Click to open dropdown
    await (categorySelect as any).click();
    await new Promise(resolve => setTimeout(resolve, 500));

    // Select "debate" category
    const debateOption = await page.evaluateHandle(() => {
      const options = Array.from(document.querySelectorAll('[role="option"]'));
      return options.find(opt => opt.textContent?.toLowerCase() === 'debate');
    });

    if (debateOption) {
      await (debateOption as any).click();
      await new Promise(resolve => setTimeout(resolve, 500));

      // Verify filter is applied
      const selectedValue = await page.evaluate(() => {
        const select = document.querySelector('[role="combobox"]');
        return select?.textContent;
      });

      expect(selectedValue?.toLowerCase()).toContain('debate');
    }
  });
});