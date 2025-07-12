import { Page, Browser } from 'puppeteer';

export interface TestConfig {
  baseUrl: string;
  timeout: number;
  retryCount: number;
  screenshotDir: string;
  slowMo: number;
}

export const defaultConfig: TestConfig = {
  baseUrl: process.env.BASE_URL || 'http://localhost:3000',
  timeout: parseInt(process.env.TIMEOUT || '60000'),
  retryCount: parseInt(process.env.RETRY_COUNT || '3'),
  screenshotDir: process.env.SCREENSHOT_DIR || './screenshots',
  slowMo: parseInt(process.env.SLOWMO || '0')
};

// Wait utilities with proper error handling
export async function waitForSelector(
  page: Page,
  selector: string,
  options: { timeout?: number; visible?: boolean } = {}
): Promise<void> {
  const timeout = options.timeout || defaultConfig.timeout;
  try {
    await page.waitForSelector(selector, {
      timeout,
      visible: options.visible !== false
    });
  } catch (error) {
    await takeScreenshot(page, `error-selector-${selector.replace(/[^a-z0-9]/gi, '-')}`);
    throw new Error(`Failed to find selector "${selector}" within ${timeout}ms`);
  }
}

export async function waitForText(
  page: Page,
  text: string,
  options: { timeout?: number; selector?: string } = {}
): Promise<void> {
  const timeout = options.timeout || defaultConfig.timeout;
  const selector = options.selector || 'body';
  
  try {
    await page.waitForFunction(
      (sel: string, txt: string) => {
        const element = document.querySelector(sel);
        return element && element.textContent?.includes(txt);
      },
      { timeout },
      selector,
      text
    );
  } catch (error) {
    await takeScreenshot(page, `error-text-${text.replace(/[^a-z0-9]/gi, '-')}`);
    throw new Error(`Failed to find text "${text}" within ${timeout}ms`);
  }
}

export async function clickWithRetry(
  page: Page,
  selector: string,
  options: { retries?: number; timeout?: number } = {}
): Promise<void> {
  const retries = options.retries || 3;
  const timeout = options.timeout || 5000;
  
  for (let i = 0; i < retries; i++) {
    try {
      await waitForSelector(page, selector, { timeout });
      await page.click(selector);
      return;
    } catch (error) {
      if (i === retries - 1) {
        await takeScreenshot(page, `error-click-${selector.replace(/[^a-z0-9]/gi, '-')}`);
        throw error;
      }
      await page.waitForTimeout(1000);
    }
  }
}

export async function typeWithDelay(
  page: Page,
  selector: string,
  text: string,
  delay: number = 50
): Promise<void> {
  await waitForSelector(page, selector);
  await page.click(selector);
  await page.type(selector, text, { delay });
}

export async function waitForNavigation(
  page: Page,
  options: { timeout?: number; waitUntil?: 'load' | 'domcontentloaded' | 'networkidle0' | 'networkidle2' } = {}
): Promise<void> {
  const timeout = options.timeout || defaultConfig.timeout;
  const waitUntil = options.waitUntil || 'networkidle2';
  
  try {
    await page.waitForNavigation({ timeout, waitUntil });
  } catch (error) {
    await takeScreenshot(page, 'error-navigation');
    throw new Error(`Navigation failed within ${timeout}ms`);
  }
}

export async function waitForResponse(
  page: Page,
  urlPattern: string | RegExp,
  options: { timeout?: number; status?: number } = {}
): Promise<void> {
  const timeout = options.timeout || defaultConfig.timeout;
  const expectedStatus = options.status || 200;
  
  try {
    await page.waitForResponse(
      response => {
        const matches = typeof urlPattern === 'string' 
          ? response.url().includes(urlPattern)
          : urlPattern.test(response.url());
        return matches && response.status() === expectedStatus;
      },
      { timeout }
    );
  } catch (error) {
    await takeScreenshot(page, 'error-response');
    throw new Error(`Failed to receive response matching ${urlPattern} with status ${expectedStatus}`);
  }
}

export async function takeScreenshot(
  page: Page,
  name: string
): Promise<void> {
  try {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const filename = `${defaultConfig.screenshotDir}/${timestamp}-${name}.png`;
    await page.screenshot({ path: filename, fullPage: true });
    console.log(`Screenshot saved: ${filename}`);
  } catch (error) {
    console.error('Failed to take screenshot:', error);
  }
}

export async function waitForDebounce(ms: number = 500): Promise<void> {
  await new Promise(resolve => setTimeout(resolve, ms));
}

export async function retryOperation<T>(
  operation: () => Promise<T>,
  options: { retries?: number; delay?: number; onRetry?: (attempt: number, error: Error) => void } = {}
): Promise<T> {
  const retries = options.retries || defaultConfig.retryCount;
  const delay = options.delay || 1000;
  
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      if (attempt === retries) {
        throw error;
      }
      
      if (options.onRetry) {
        options.onRetry(attempt, error as Error);
      }
      
      console.log(`Attempt ${attempt} failed, retrying in ${delay}ms...`);
      await waitForDebounce(delay);
    }
  }
  
  throw new Error('This should never happen');
}

// Service health check utilities
export async function waitForService(
  url: string,
  options: { timeout?: number; retryInterval?: number } = {}
): Promise<void> {
  const timeout = options.timeout || 60000;
  const retryInterval = options.retryInterval || 2000;
  const startTime = Date.now();
  
  while (Date.now() - startTime < timeout) {
    try {
      const response = await fetch(`${url}/health`);
      if (response.ok) {
        const data = await response.json();
        if (data.status === 'healthy') {
          return;
        }
      }
    } catch (error) {
      // Service not ready yet
    }
    
    await waitForDebounce(retryInterval);
  }
  
  throw new Error(`Service at ${url} failed to become healthy within ${timeout}ms`);
}

export async function ensureServicesReady(): Promise<void> {
  console.log('Checking service health...');
  
  const services = [
    { name: 'UI', url: defaultConfig.baseUrl },
    { name: 'LLM Service', url: 'http://localhost:5002' },
    { name: 'Debate Service', url: 'http://localhost:5013' }
  ];
  
  for (const service of services) {
    console.log(`Waiting for ${service.name}...`);
    await waitForService(service.url);
    console.log(`✓ ${service.name} is ready`);
  }
}

// Element interaction helpers
export async function selectDropdownOption(
  page: Page,
  dropdownSelector: string,
  optionText: string
): Promise<void> {
  await clickWithRetry(page, dropdownSelector);
  await waitForDebounce(300);
  
  const optionSelector = `${dropdownSelector} option:contains("${optionText}")`;
  await waitForSelector(page, optionSelector);
  await page.select(dropdownSelector, optionText);
}

export async function fillForm(
  page: Page,
  fields: { selector: string; value: string }[]
): Promise<void> {
  for (const field of fields) {
    await typeWithDelay(page, field.selector, field.value);
    await waitForDebounce(100);
  }
}

// Assertion helpers
export async function expectTextVisible(
  page: Page,
  text: string,
  options: { timeout?: number } = {}
): Promise<void> {
  await waitForText(page, text, options);
}

export async function expectElementVisible(
  page: Page,
  selector: string,
  options: { timeout?: number } = {}
): Promise<void> {
  await waitForSelector(page, selector, { ...options, visible: true });
}

export async function expectElementCount(
  page: Page,
  selector: string,
  count: number
): Promise<void> {
  await page.waitForFunction(
    (sel: string, expectedCount: number) => {
      return document.querySelectorAll(sel).length === expectedCount;
    },
    { timeout: defaultConfig.timeout },
    selector,
    count
  );
}

// Page setup helper
export async function setupPage(browser: Browser): Promise<Page> {
  const page = await browser.newPage();
  
  // Set viewport
  await page.setViewport({ width: 1280, height: 720 });
  
  // Set default timeout
  page.setDefaultTimeout(defaultConfig.timeout);
  
  // Add console log handler
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.error('Browser console error:', msg.text());
    }
  });
  
  // Add page error handler
  page.on('pageerror', error => {
    console.error('Page error:', error.message);
  });
  
  // Add request failure handler
  page.on('requestfailed', request => {
    console.error('Request failed:', request.url(), request.failure()?.errorText);
  });
  
  return page;
}