import { Page } from 'puppeteer';

export class PageHelpers {
  constructor(private page: Page) {}

  async waitAndClick(selector: string, options = {}) {
    await this.page.waitForSelector(selector, { visible: true, ...options });
    await this.page.click(selector);
  }

  async waitAndType(selector: string, text: string, options = {}) {
    await this.page.waitForSelector(selector, { visible: true, ...options });
    await this.page.click(selector);
    await this.page.keyboard.type(text);
  }

  async clearAndType(selector: string, text: string) {
    await this.page.waitForSelector(selector, { visible: true });
    await this.page.click(selector, { clickCount: 3 });
    await this.page.keyboard.press('Backspace');
    await this.page.keyboard.type(text);
  }

  async selectOption(selector: string, value: string) {
    await this.page.waitForSelector(selector, { visible: true });
    await this.page.select(selector, value);
  }

  async waitForText(text: string, options = {}) {
    await this.page.waitForFunction(
      (text) => document.body.innerText.includes(text),
      { timeout: 10000, ...options },
      text
    );
  }

  async waitForElementWithText(selector: string, text: string) {
    await this.page.waitForFunction(
      (selector, text) => {
        const elements = Array.from(document.querySelectorAll(selector));
        return elements.some(el => el.textContent?.includes(text));
      },
      {},
      selector,
      text
    );
  }

  async clickElementWithText(selector: string, text: string) {
    await this.waitForElementWithText(selector, text);
    await this.page.evaluate((selector, text) => {
      const elements = Array.from(document.querySelectorAll(selector));
      const element = elements.find(el => el.textContent?.includes(text));
      if (element) (element as HTMLElement).click();
    }, selector, text);
  }

  async getText(selector: string): Promise<string> {
    await this.page.waitForSelector(selector);
    return await this.page.$eval(selector, el => el.textContent || '');
  }

  async getValue(selector: string): Promise<string> {
    await this.page.waitForSelector(selector);
    return await this.page.$eval(selector, el => (el as HTMLInputElement).value);
  }

  async isVisible(selector: string): Promise<boolean> {
    try {
      await this.page.waitForSelector(selector, { visible: true, timeout: 1000 });
      return true;
    } catch {
      return false;
    }
  }

  async takeScreenshot(name: string) {
    await this.page.screenshot({ 
      path: `./screenshots/${name}-${Date.now()}.png`,
      fullPage: true 
    });
  }

  async waitForNetworkIdle() {
    // Puppeteer doesn't have waitForLoadState, use a different approach
    await this.page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 2000)));
  }
}