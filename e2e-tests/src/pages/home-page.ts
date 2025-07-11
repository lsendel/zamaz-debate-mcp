import { Page } from 'puppeteer';
import { PageHelpers } from '../helpers/page-helpers';

export class HomePage {
  private helpers: PageHelpers;

  constructor(private page: Page) {
    this.helpers = new PageHelpers(page);
  }

  async navigate() {
    await this.page.goto('http://localhost:3000');
    await this.page.waitForSelector('h1');
  }

  async isLoaded(): Promise<boolean> {
    return await this.helpers.isVisible('h1');
  }

  async getTitle(): Promise<string> {
    return await this.helpers.getText('h1');
  }

  async clickCreateDebate() {
    await this.helpers.clickElementWithText('button', 'Create New Debate');
  }

  async searchDebates(query: string) {
    await this.helpers.waitAndType('input[placeholder*="Search debates"]', query);
  }

  async getDebateCount(): Promise<number> {
    const statsText = await this.helpers.getText('[data-testid="stats-debates"]');
    const match = statsText.match(/\d+/);
    return match ? parseInt(match[0]) : 0;
  }

  async getTurnCount(): Promise<number> {
    const statsText = await this.helpers.getText('[data-testid="stats-turns"]');
    const match = statsText.match(/\d+/);
    return match ? parseInt(match[0]) : 0;
  }

  async getActiveDebateCount(): Promise<number> {
    const statsText = await this.helpers.getText('[data-testid="stats-active"]');
    const match = statsText.match(/\d+/);
    return match ? parseInt(match[0]) : 0;
  }

  async getDebateCards(): Promise<Array<{title: string, status: string}>> {
    await this.page.waitForSelector('[data-testid="debate-card"]');
    return await this.page.evaluate(() => {
      const cards = Array.from(document.querySelectorAll('[data-testid="debate-card"]'));
      return cards.map(card => ({
        title: card.querySelector('h3')?.textContent || '',
        status: card.querySelector('[data-testid="debate-status"]')?.textContent || ''
      }));
    });
  }

  async clickDebateCard(debateName: string) {
    await this.helpers.clickElementWithText('[data-testid="debate-card"] h3', debateName);
  }

  async switchToTab(tabName: 'All' | 'Active' | 'Completed' | 'Draft') {
    await this.helpers.clickElementWithText('button[role="tab"]', tabName);
    await this.page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 500))); // Wait for tab transition
  }
}