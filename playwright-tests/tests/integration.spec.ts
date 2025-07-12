import { test, expect } from '@playwright/test';
import { DebatePage } from '../pages/debate.page';
import { TemplatePage } from '../pages/template.page';
import { OrganizationPage } from '../pages/organization.page';
import { randomString, generateDebateData, generateTemplateContent } from '../utils/helpers';

test.describe('End-to-End Integration Tests', () => {
  let debatePage: DebatePage;
  let templatePage: TemplatePage;
  let organizationPage: OrganizationPage;

  test.beforeEach(async ({ page }) => {
    debatePage = new DebatePage(page);
    templatePage = new TemplatePage(page);
    organizationPage = new OrganizationPage(page);
    await page.goto('/');
  });

  test('complete workflow: organization → template → debate', async ({ page }) => {
    // Step 1: Create organization
    const orgName = `Integration Org ${randomString()}`;
    await organizationPage.createOrganization(orgName);
    await expect(organizationPage.currentOrgButton).toContainText(orgName);
    
    // Step 2: Create template
    await templatePage.navigateToLibrary();
    const templateData = {
      name: `Integration Template ${randomString()}`,
      content: generateTemplateContent('complex'),
      category: 'debate' as const,
      description: 'Template for integration testing'
    };
    await templatePage.createTemplate(templateData);
    
    // Step 3: Create debate using template
    await debatePage.goto();
    const debateData = {
      name: `Integration Debate ${randomString()}`,
      topic: 'Should AI be used in critical decision making?',
      template: templateData.name
    };
    await debatePage.createDebate(debateData);
    
    // Step 4: Verify everything is connected
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateData.name)).toBeVisible();
    
    // Step 5: Check organization history shows activity
    await organizationPage.openHistory();
    await expect(page.getByText(/Created template/)).toBeVisible();
    await expect(page.getByText(/Started debate/)).toBeVisible();
    await organizationPage.closeHistory();
  });

  test('template usage tracking across organizations', async ({ page }) => {
    // Create two organizations
    const org1 = await organizationPage.createOrganization(`Org 1 ${randomString()}`);
    const org2 = await organizationPage.createOrganization(`Org 2 ${randomString()}`);
    
    // Create template in org2
    await templatePage.navigateToLibrary();
    const template = await templatePage.createTemplate({
      name: `Shared Template ${randomString()}`,
      content: 'Shared template content',
      category: 'debate'
    });
    
    // Switch to org1
    await organizationPage.switchToOrganization(org1);
    
    // Template should still be visible (if templates are shared)
    // or not visible (if templates are org-specific)
    await templatePage.navigateToLibrary();
    
    // This behavior depends on your implementation
    // Adjust the test based on your business logic
  });

  test('search functionality across all entities', async ({ page }) => {
    const uniqueId = randomString();
    
    // Create entities with similar names
    const orgName = await organizationPage.createOrganization(`Search ${uniqueId} Org`);
    
    await templatePage.navigateToLibrary();
    const template = await templatePage.createTemplate({
      name: `Search ${uniqueId} Template`,
      content: 'Search test content'
    });
    
    const debate = generateDebateData();
    debate.name = `Search ${uniqueId} Debate`;
    await debatePage.createDebate(debate);
    
    // Search in templates
    await templatePage.navigateToLibrary();
    await templatePage.searchTemplates(uniqueId);
    await expect(page.getByText(template.name)).toBeVisible();
    
    // Search in debates
    await debatePage.searchDebates(uniqueId);
    await expect(page.getByText(debate.name)).toBeVisible();
  });

  test('keyboard shortcuts integration', async ({ page }) => {
    // Test global shortcuts
    await page.keyboard.press('Control+/');
    await expect(page.getByText('Keyboard Shortcuts')).toBeVisible();
    await page.keyboard.press('Escape');
    
    // Test navigation shortcuts (if implemented)
    // await page.keyboard.press('Alt+D'); // Go to debates
    // await expect(page.getByRole('tab', { name: 'Debates' })).toHaveAttribute('aria-selected', 'true');
    
    // await page.keyboard.press('Alt+T'); // Go to templates
    // await expect(page.getByRole('tab', { name: 'Library' })).toHaveAttribute('aria-selected', 'true');
  });

  test('error handling and recovery', async ({ page }) => {
    // Test creating organization with duplicate name
    const duplicateName = `Duplicate ${randomString()}`;
    await organizationPage.createOrganization(duplicateName);
    
    // Try to create another with same name
    await organizationPage.openDropdown();
    await organizationPage.createOrgButton.click();
    await page.getByLabel('Organization Name').fill(duplicateName);
    await page.getByRole('button', { name: 'Create Organization' }).last().click();
    
    // Should show error or handle gracefully
    // The exact behavior depends on your implementation
  });

  test('performance: handle large numbers of items', async ({ page }) => {
    // Create multiple templates
    await templatePage.navigateToLibrary();
    
    const templatePromises = [];
    for (let i = 0; i < 10; i++) {
      templatePromises.push(
        templatePage.createTemplate({
          name: `Perf Template ${i} ${randomString()}`,
          content: `Template content ${i}`
        })
      );
    }
    
    await Promise.all(templatePromises);
    
    // Verify page still responsive
    const startTime = Date.now();
    await templatePage.searchTemplates('Perf');
    const searchTime = Date.now() - startTime;
    
    // Search should be reasonably fast
    expect(searchTime).toBeLessThan(3000);
    
    // Should show all matching templates
    const visibleTemplates = await templatePage.getVisibleTemplates();
    const perfTemplates = visibleTemplates.filter(name => name.includes('Perf'));
    expect(perfTemplates.length).toBeGreaterThanOrEqual(10);
  });

  test('data persistence across page reloads', async ({ page }) => {
    // Create data
    const orgName = await organizationPage.createOrganization(`Persist ${randomString()}`);
    const debateData = generateDebateData();
    await debatePage.createDebate(debateData);
    
    // Reload page
    await page.reload();
    
    // Check organization persisted
    await expect(organizationPage.currentOrgButton).toContainText(orgName);
    
    // Check debate persisted
    await debatePage.navigateToDebates();
    await expect(page.getByText(debateData.name)).toBeVisible();
  });

  test('concurrent operations', async ({ page, context }) => {
    // Open two pages
    const page2 = await context.newPage();
    const orgPage2 = new OrganizationPage(page2);
    const debatePage2 = new DebatePage(page2);
    await page2.goto('/');
    
    // Create organization in page 1
    const orgName = await organizationPage.createOrganization(`Concurrent ${randomString()}`);
    
    // Page 2 should see the new organization
    await page2.reload();
    await orgPage2.openDropdown();
    const orgs = await orgPage2.getOrganizationList();
    const hasNewOrg = orgs.some(org => org.includes(orgName));
    expect(hasNewOrg).toBeTruthy();
    
    // Create debate in page 2
    const debate = generateDebateData();
    await debatePage2.createDebate(debate);
    
    // Page 1 should see the new debate
    await debatePage.navigateToDebates();
    await page.reload();
    await expect(page.getByText(debate.name)).toBeVisible();
    
    await page2.close();
  });

  test('mobile responsiveness workflow', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 812 });
    
    // Test core workflow on mobile
    const orgName = await organizationPage.createOrganization(`Mobile ${randomString()}`);
    await expect(organizationPage.currentOrgButton).toContainText(orgName);
    
    // Navigate using mobile UI
    await debatePage.navigateToDebates();
    await expect(page.getByText(/debates|No debates yet/)).toBeVisible();
    
    // Create debate on mobile
    const debate = generateDebateData();
    await debatePage.createDebate(debate);
    await expect(page.getByText(debate.name)).toBeVisible();
  });
});