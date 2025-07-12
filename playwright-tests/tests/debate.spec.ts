import { test, expect } from '@playwright/test';
import { DebatePage } from '../pages/debate.page';
import { TemplatePage } from '../pages/template.page';
import { OrganizationPage } from '../pages/organization.page';
import { randomString, generateDebateData } from '../utils/helpers';

test.describe('Debate Management', () => {
  let debatePage: DebatePage;
  let templatePage: TemplatePage;
  let organizationPage: OrganizationPage;

  test.beforeEach(async ({ page }) => {
    debatePage = new DebatePage(page);
    templatePage = new TemplatePage(page);
    organizationPage = new OrganizationPage(page);
    await debatePage.goto();
  });

  test('should create a new debate', async ({ page }) => {
    const debateData = generateDebateData();
    
    await debatePage.createDebate(debateData);
    
    // Verify debate was created
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateData.name)).toBeVisible();
  });

  test('should create debate with template', async ({ page }) => {
    // First create a template
    await templatePage.navigateToLibrary();
    const template = await templatePage.createTemplate({
      name: `Debate Template ${randomString()}`,
      content: 'Topic: {{ topic }}\nFormat: {{ format }}',
      category: 'debate'
    });
    
    // Go back to debates
    await debatePage.goto();
    
    // Create debate with template
    const debateData = {
      name: `Templated Debate ${randomString()}`,
      topic: 'AI Ethics in Healthcare',
      template: template.name
    };
    
    await debatePage.createDebate(debateData);
    
    // Verify debate was created
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateData.name)).toBeVisible();
  });

  test('should use template from gallery', async ({ page }) => {
    // Create a template first
    await templatePage.navigateToLibrary();
    const template = await templatePage.createTemplate({
      name: `Gallery Template ${randomString()}`,
      content: 'Quick debate template',
      category: 'debate'
    });
    
    // Use template from gallery
    await debatePage.useTemplateFromGallery(template.name);
    
    // Should open create debate dialog with template
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByLabel('Template')).toHaveValue(template.name);
    
    // Complete the debate creation
    const debateName = `From Gallery ${randomString()}`;
    await page.getByLabel('Debate Name').fill(debateName);
    await page.getByLabel('Topic').fill('Test Topic');
    await page.getByRole('button', { name: 'Create Debate' }).last().click();
    
    // Verify created
    await expect(page.getByRole('dialog')).not.toBeVisible();
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateName)).toBeVisible();
  });

  test('should search debates', async ({ page }) => {
    // Create multiple debates
    const debates = [];
    for (let i = 0; i < 3; i++) {
      const data = {
        name: i < 2 ? `Search Test ${randomString()}` : `Different ${randomString()}`,
        topic: 'Test Topic'
      };
      await debatePage.createDebate(data);
      debates.push(data);
    }
    
    // Search for "Search"
    await debatePage.searchDebates('Search');
    
    // Should show matching debates
    await expect(page.getByText(debates[0].name)).toBeVisible();
    await expect(page.getByText(debates[1].name)).toBeVisible();
    await expect(page.getByText(debates[2].name)).not.toBeVisible();
  });

  test('should filter debates by status', async ({ page }) => {
    // This test assumes debates have different statuses
    // In a real scenario, you'd create debates with different statuses
    
    await debatePage.navigateToDebates();
    
    // Filter by active
    await debatePage.filterDebatesByStatus('active');
    
    // Check that filter is applied
    const statusFilter = page.getByRole('combobox', { name: 'Status' });
    await expect(statusFilter).toHaveText(/Active/);
  });

  test('should display debate details', async ({ page }) => {
    const debateData = generateDebateData();
    await debatePage.createDebate(debateData);
    
    await debatePage.navigateToDebates();
    const debateCard = await debatePage.getDebateCard(debateData.name);
    
    // Check card displays key info
    await expect(debateCard).toContainText(debateData.name);
    await expect(debateCard).toContainText(debateData.topic);
  });

  test('should handle quick actions', async ({ page }) => {
    // Test Continue Active quick action
    const continueButton = page.locator('.quick-action-card').filter({ hasText: 'Continue Active' });
    await expect(continueButton).toBeVisible();
    
    // Test View History quick action
    const historyButton = page.locator('.quick-action-card').filter({ hasText: 'View History' });
    await expect(historyButton).toBeVisible();
    
    // Test Start New Debate quick action
    const newDebateButton = page.locator('.quick-action-card').filter({ hasText: 'Start New Debate' });
    await expect(newDebateButton).toBeVisible();
    
    // Click Start New Debate
    await newDebateButton.click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('heading', { name: /Create.*Debate/i })).toBeVisible();
  });

  test('should create debate in specific organization', async ({ page }) => {
    // Create a new organization
    const orgName = await organizationPage.createOrganization(`Debate Org ${randomString()}`);
    
    // Create debate
    const debateData = generateDebateData();
    await debatePage.createDebate(debateData);
    
    // Verify debate is in the current organization
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateData.name)).toBeVisible();
    
    // The debate should be associated with the current org
    const currentOrg = await organizationPage.getCurrentOrganization();
    expect(currentOrg).toContain(orgName);
  });

  test('should validate debate form', async ({ page }) => {
    await debatePage.newDebateButton.click();
    
    // Try to submit empty form
    await page.getByRole('button', { name: 'Create Debate' }).last().click();
    
    // Should remain in dialog (validation failed)
    await expect(page.getByRole('dialog')).toBeVisible();
    
    // Fill required fields
    await page.getByLabel('Debate Name').fill('Valid Debate');
    await page.getByLabel('Topic').fill('Valid Topic');
    
    // Should now submit successfully
    await page.getByRole('button', { name: 'Create Debate' }).last().click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });

  test('should handle debate with multiple participants', async ({ page }) => {
    const debateData = {
      name: `Multi Participant ${randomString()}`,
      topic: 'Complex Topic',
      participants: [
        { name: 'Claude Pro', position: 'In favor' },
        { name: 'GPT Con', position: 'Against' },
        { name: 'Gemini Moderator', position: 'Neutral' }
      ]
    };
    
    await debatePage.createDebate(debateData);
    
    // Verify debate created
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateData.name)).toBeVisible();
    
    // Open debate to see participants
    await debatePage.openDebate(debateData.name);
    
    // Should show all participants
    for (const participant of debateData.participants) {
      await expect(page.getByText(participant.name)).toBeVisible();
    }
  });
});