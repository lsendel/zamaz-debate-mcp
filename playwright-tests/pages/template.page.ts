import { Page, Locator } from '@playwright/test';

export interface TemplateData {
  name: string;
  content?: string;
  category?: 'debate' | 'summary' | 'custom';
  description?: string;
  tags?: string[];
}

export class TemplatePage {
  readonly page: Page;
  readonly createButton: Locator;
  readonly searchInput: Locator;
  readonly categoryFilter: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createButton = page.getByRole('button', { name: 'Create Template' });
    this.searchInput = page.getByPlaceholder('Search templates');
    this.categoryFilter = page.getByRole('combobox');
  }

  async goto() {
    await this.page.goto('/');
  }

  async navigateToLibrary() {
    await this.page.getByRole('tab', { name: 'Library' }).click();
    await this.page.waitForSelector('text=Template Library', { state: 'visible' });
  }

  async createTemplate(data: TemplateData): Promise<TemplateData> {
    // Click create button
    await this.createButton.click();
    
    // Wait for dialog
    await this.page.waitForSelector('[role="dialog"]', { state: 'visible' });
    
    // Fill form
    await this.page.getByLabel('Template Name').fill(data.name);
    
    if (data.description) {
      await this.page.getByLabel('Description').fill(data.description);
    }
    
    if (data.content) {
      await this.page.getByLabel('Template Content').fill(data.content);
    } else {
      await this.page.getByLabel('Template Content').fill('Default template content');
    }
    
    // Select category if provided
    if (data.category) {
      await this.page.getByLabel('Category').click();
      const categoryName = data.category.charAt(0).toUpperCase() + data.category.slice(1);
      await this.page.getByRole('option', { name: categoryName }).click();
    }
    
    // Submit
    await this.page.getByRole('button', { name: 'Create Template' }).last().click();
    
    // Wait for dialog to close
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden' });
    
    // Wait for template to appear
    await this.page.waitForSelector(`text="${data.name}"`, { state: 'visible' });
    
    return data;
  }

  async searchTemplates(query: string) {
    await this.searchInput.fill(query);
    // Wait for debounce
    await this.page.waitForTimeout(600);
  }

  async filterByCategory(category: 'all' | 'debate' | 'summary' | 'custom') {
    await this.categoryFilter.click();
    const categoryName = category === 'all' ? 'All' : category.charAt(0).toUpperCase() + category.slice(1);
    await this.page.getByRole('option', { name: categoryName }).click();
  }

  async getTemplateCard(name: string): Promise<Locator> {
    return this.page.locator('.card').filter({ hasText: name });
  }

  async previewTemplate(name: string) {
    const card = await this.getTemplateCard(name);
    await card.getByRole('button', { name: 'Preview' }).click();
    await this.page.waitForSelector('[role="dialog"]', { state: 'visible' });
  }

  async editTemplate(name: string, updates: Partial<TemplateData>) {
    const card = await this.getTemplateCard(name);
    await card.getByRole('button', { name: 'Edit' }).click();
    
    // Wait for dialog
    await this.page.waitForSelector('[role="dialog"]', { state: 'visible' });
    
    // Update fields
    if (updates.name) {
      await this.page.getByLabel('Template Name').fill(updates.name);
    }
    
    if (updates.content) {
      await this.page.getByLabel('Template Content').fill(updates.content);
    }
    
    if (updates.description) {
      await this.page.getByLabel('Description').fill(updates.description);
    }
    
    // Submit
    await this.page.getByRole('button', { name: 'Update Template' }).click();
    
    // Wait for dialog to close
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden' });
  }

  async deleteTemplate(name: string) {
    const card = await this.getTemplateCard(name);
    
    // Set up dialog handler
    this.page.on('dialog', dialog => dialog.accept());
    
    // Click delete button
    await card.getByRole('button').filter({ has: this.page.locator('.lucide-trash-2') }).click();
    
    // Wait for card to disappear
    await card.waitFor({ state: 'hidden' });
  }

  async getVisibleTemplates(): Promise<string[]> {
    const cards = await this.page.locator('.card').all();
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

  async closeDialog() {
    await this.page.getByRole('button', { name: 'Close' }).click();
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden' });
  }
}