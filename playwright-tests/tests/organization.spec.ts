import { test, expect, Page } from '@playwright/test';
import { OrganizationPage } from '../pages/organization.page';
import { randomString } from '../utils/helpers';

test.describe('Organization Management', () => {
  let organizationPage: OrganizationPage;

  test.beforeEach(async ({ page }) => {
    organizationPage = new OrganizationPage(page);
    await organizationPage.goto();
  });

  test('should create a new organization', async ({ page }) => {
    const orgName = `Test Org ${randomString()}`;
    
    // Open organization dropdown
    await organizationPage.openDropdown();
    
    // Click Create Organization
    await page.getByRole('button', { name: 'Create Organization' }).click();
    
    // Fill form
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.getByLabel('Organization Name').fill(orgName);
    
    // Submit
    await page.getByRole('button', { name: 'Create Organization' }).last().click();
    
    // Verify organization created
    await expect(page.getByRole('dialog')).not.toBeVisible();
    await expect(page.getByText(orgName)).toBeVisible();
  });

  test('should switch between organizations', async ({ page }) => {
    // Create two organizations first
    const org1 = await organizationPage.createOrganization(`Org 1 ${randomString()}`);
    const org2 = await organizationPage.createOrganization(`Org 2 ${randomString()}`);
    
    // Verify current org is org2 (last created)
    await expect(organizationPage.currentOrgButton).toContainText(org2);
    
    // Switch to org1
    await organizationPage.switchToOrganization(org1);
    await expect(organizationPage.currentOrgButton).toContainText(org1);
  });

  test('should display organization history', async ({ page }) => {
    // Create an organization
    const orgName = await organizationPage.createOrganization(`History Test ${randomString()}`);
    
    // Open history
    await organizationPage.openDropdown();
    await page.getByRole('button', { name: 'View History' }).click();
    
    // Check history dialog
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Organization History')).toBeVisible();
    await expect(page.getByText(`Activity history for ${orgName}`)).toBeVisible();
    
    // Should show organization creation event
    await expect(page.getByText(/Created organization/)).toBeVisible();
    
    // Close dialog
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });

  test('should persist organization selection', async ({ page, context }) => {
    const orgName = await organizationPage.createOrganization(`Persist Test ${randomString()}`);
    
    // Verify organization is selected
    await expect(organizationPage.currentOrgButton).toContainText(orgName);
    
    // Open new page in same context
    const newPage = await context.newPage();
    const newOrgPage = new OrganizationPage(newPage);
    await newOrgPage.goto();
    
    // Should still have same organization selected
    await expect(newOrgPage.currentOrgButton).toContainText(orgName);
    
    await newPage.close();
  });

  test('should show organization details in dropdown', async ({ page }) => {
    await organizationPage.openDropdown();
    
    // Check organization list shows details
    const orgItems = page.locator('button').filter({ hasText: /debates.*ago/ });
    const count = await orgItems.count();
    expect(count).toBeGreaterThan(0);
    
    // Check for debate count and last active info
    const firstOrg = orgItems.first();
    await expect(firstOrg).toContainText(/\d+ debates/);
    await expect(firstOrg).toContainText(/ago|Never/);
  });

  test('should handle organization with special characters', async ({ page }) => {
    const specialNames = [
      'Org with spaces',
      'Org-with-dashes',
      'Org_with_underscores',
      'Org & Special',
      'Org "Quoted"',
      "Org's Apostrophe"
    ];
    
    for (const name of specialNames) {
      const uniqueName = `${name} ${randomString()}`;
      await organizationPage.createOrganization(uniqueName);
      await expect(organizationPage.currentOrgButton).toContainText(uniqueName);
    }
  });

  test('should validate organization form', async ({ page }) => {
    await organizationPage.openDropdown();
    await page.getByRole('button', { name: 'Create Organization' }).click();
    
    // Try to submit empty form
    await page.getByRole('button', { name: 'Create Organization' }).last().click();
    
    // Should still be in dialog (validation failed)
    await expect(page.getByRole('dialog')).toBeVisible();
    
    // Fill with whitespace only
    await page.getByLabel('Organization Name').fill('   ');
    await page.getByRole('button', { name: 'Create Organization' }).last().click();
    
    // Should still be in dialog
    await expect(page.getByRole('dialog')).toBeVisible();
    
    // Cancel
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });

  test('should close dropdown when clicking outside', async ({ page }) => {
    await organizationPage.openDropdown();
    await expect(organizationPage.dropdown).toBeVisible();
    
    // Click outside
    await page.locator('body').click({ position: { x: 0, y: 0 } });
    
    // Dropdown should close
    await expect(organizationPage.dropdown).not.toBeVisible();
  });

  test('should show correct organization count', async ({ page }) => {
    await organizationPage.openDropdown();
    
    // Get initial count from badge
    const badge = page.locator('.badge').filter({ hasText: /^\d+$/ });
    const initialCount = await badge.textContent();
    const count = parseInt(initialCount || '0');
    
    // Create new organization
    await page.keyboard.press('Escape'); // Close dropdown first
    const newOrg = await organizationPage.createOrganization(`Count Test ${randomString()}`);
    
    // Check count increased
    await organizationPage.openDropdown();
    const newBadge = page.locator('.badge').filter({ hasText: /^\d+$/ });
    const newCount = await newBadge.textContent();
    expect(parseInt(newCount || '0')).toBe(count + 1);
  });
});