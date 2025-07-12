import { Page, Locator } from '@playwright/test';

export interface DebateData {
  name: string;
  topic: string;
  template?: string;
  participants?: Array<{
    name: string;
    position: string;
  }>;
}

export class DebatePage {
  readonly page: Page;
  readonly newDebateButton: Locator;
  readonly debatesTab: Locator;
  readonly galleryTab: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newDebateButton = page.getByRole('button', { name: 'New Debate' });
    this.debatesTab = page.getByRole('tab', { name: 'Debates' });
    this.galleryTab = page.getByRole('tab', { name: 'Gallery' });
  }

  async goto() {
    await this.page.goto('/');
  }

  async createDebate(data: DebateData): Promise<void> {
    // Click new debate button
    await this.newDebateButton.click();
    
    // Wait for dialog
    await this.page.waitForSelector('[role="dialog"]', { state: 'visible' });
    
    // Fill form
    await this.page.getByLabel('Debate Name').fill(data.name);
    await this.page.getByLabel('Topic').fill(data.topic);
    
    // Select template if provided
    if (data.template) {
      await this.page.getByLabel('Template').click();
      await this.page.getByRole('option', { name: data.template }).click();
    }
    
    // Add participants if provided
    if (data.participants) {
      for (const participant of data.participants) {
        await this.page.getByRole('button', { name: 'Add Participant' }).click();
        await this.page.getByLabel('Participant Name').last().fill(participant.name);
        await this.page.getByLabel('Position').last().fill(participant.position);
      }
    }
    
    // Submit
    await this.page.getByRole('button', { name: 'Create Debate' }).last().click();
    
    // Wait for dialog to close
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden' });
  }

  async navigateToDebates() {
    await this.debatesTab.click();
    await this.page.waitForSelector('text=/debates|No debates yet/', { state: 'visible' });
  }

  async navigateToGallery() {
    await this.galleryTab.click();
    await this.page.waitForSelector('text=Debate Templates', { state: 'visible' });
  }

  async getDebateCard(name: string): Promise<Locator> {
    return this.page.locator('.debate-card').filter({ hasText: name });
  }

  async openDebate(name: string) {
    const card = await this.getDebateCard(name);
    await card.click();
    await this.page.waitForSelector('.debate-view', { state: 'visible' });
  }

  async getActiveDebates(): Promise<string[]> {
    await this.navigateToDebates();
    const cards = await this.page.locator('.debate-card').all();
    const names: string[] = [];
    
    for (const card of cards) {
      const nameElement = await card.locator('h3').first();
      const name = await nameElement.textContent();
      if (name) {
        names.push(name);
      }
    }
    
    return names;
  }

  async searchDebates(query: string) {
    await this.navigateToDebates();
    const searchInput = this.page.getByPlaceholder('Search debates');
    await searchInput.fill(query);
    // Wait for search debounce
    await this.page.waitForTimeout(600);
  }

  async filterDebatesByStatus(status: 'all' | 'active' | 'completed' | 'draft') {
    await this.navigateToDebates();
    const statusFilter = this.page.getByRole('combobox', { name: 'Status' });
    await statusFilter.click();
    const statusName = status.charAt(0).toUpperCase() + status.slice(1);
    await this.page.getByRole('option', { name: statusName }).click();
  }

  async useTemplateFromGallery(templateName: string) {
    await this.navigateToGallery();
    
    // Find template card
    const templateCard = this.page.locator('.template-card').filter({ hasText: templateName });
    
    // Click use template button
    await templateCard.getByRole('button', { name: 'Use Template' }).click();
    
    // This should open the create debate dialog with template pre-filled
    await this.page.waitForSelector('[role="dialog"]', { state: 'visible' });
  }

  async continueDebate(name: string) {
    const continueCard = this.page.locator('.quick-action-card').filter({ hasText: 'Continue Active' });
    await continueCard.click();
    
    // Select debate from list
    await this.page.getByRole('option', { name: name }).click();
    
    // Should navigate to debate view
    await this.page.waitForSelector('.debate-view', { state: 'visible' });
  }

  async viewDebateHistory() {
    const historyCard = this.page.locator('.quick-action-card').filter({ hasText: 'View History' });
    await historyCard.click();
    
    // Should open history view
    await this.page.waitForSelector('.debate-history', { state: 'visible' });
  }
}