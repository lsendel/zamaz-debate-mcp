import { Page, Locator } from '@playwright/test';

export class OrganizationPage {
  readonly page: Page;
  readonly currentOrgButton: Locator;
  readonly dropdown: Locator;
  readonly createOrgButton: Locator;
  readonly viewHistoryButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.currentOrgButton = page.getByRole('button').filter({ hasText: /Organization/ });
    this.dropdown = page.locator('.absolute.top-full.left-0.mt-2.w-\\[240px\\]');
    this.createOrgButton = page.getByRole('button', { name: 'Create Organization' });
    this.viewHistoryButton = page.getByRole('button', { name: 'View History' });
  }

  async goto() {
    await this.page.goto('/');
  }

  async openDropdown() {
    await this.currentOrgButton.click();
    await this.page.waitForSelector('.absolute.top-full.left-0.mt-2.w-\\[240px\\]', { state: 'visible' });
  }

  async closeDropdown() {
    await this.page.keyboard.press('Escape');
  }

  async createOrganization(name: string): Promise<string> {
    await this.openDropdown();
    await this.createOrgButton.click();
    
    // Fill form
    await this.page.getByLabel('Organization Name').fill(name);
    
    // Submit
    await this.page.getByRole('button', { name: 'Create Organization' }).last().click();
    
    // Wait for dialog to close
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden' });
    
    // Wait for the new org to be selected
    await this.page.waitForFunction(
      (orgName) => {
        const button = document.querySelector('button');
        return button?.textContent?.includes(orgName);
      },
      name
    );
    
    return name;
  }

  async switchToOrganization(name: string) {
    await this.openDropdown();
    
    // Find and click organization
    const orgButton = this.page.locator('button').filter({ hasText: name }).first();
    await orgButton.click();
    
    // Wait for dropdown to close and org to be selected
    await this.page.waitForSelector('.absolute.top-full.left-0.mt-2.w-\\[240px\\]', { state: 'hidden' });
    await this.page.waitForFunction(
      (orgName) => {
        const button = document.querySelector('button');
        return button?.textContent?.includes(orgName);
      },
      name
    );
  }

  async getCurrentOrganization(): Promise<string> {
    const text = await this.currentOrgButton.textContent();
    return text || '';
  }

  async getOrganizationList(): Promise<string[]> {
    await this.openDropdown();
    
    // Get all organization buttons (excluding action buttons)
    const orgButtons = this.page.locator('button').filter({ hasText: /debates.*ago/ });
    const organizations = await orgButtons.allTextContents();
    
    await this.closeDropdown();
    return organizations;
  }

  async openHistory() {
    await this.openDropdown();
    await this.viewHistoryButton.click();
    await this.page.waitForSelector('[role="dialog"]', { state: 'visible' });
  }

  async closeHistory() {
    await this.page.keyboard.press('Escape');
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden' });
  }
}