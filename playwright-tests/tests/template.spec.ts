import { test, expect } from '@playwright/test';
import { TemplatePage } from '../pages/template.page';
import { randomString } from '../utils/helpers';

test.describe('Template Management', () => {
  let templatePage: TemplatePage;

  test.beforeEach(async ({ page }) => {
    templatePage = new TemplatePage(page);
    await templatePage.goto();
    await templatePage.navigateToLibrary();
  });

  test('should display template library', async ({ page }) => {
    // Check page elements
    await expect(page.getByRole('heading', { name: 'Template Library' })).toBeVisible();
    await expect(page.getByText('Manage and organize your debate templates')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Template' })).toBeVisible();
    
    // Check search and filter
    await expect(page.getByPlaceholder('Search templates')).toBeVisible();
    await expect(page.getByRole('combobox')).toBeVisible();
  });

  test('should create a new template', async ({ page }) => {
    const templateName = `Test Template ${randomString()}`;
    const templateContent = 'Hello {{ name }}, welcome to {{ topic }}!';
    
    // Click create button
    await page.getByRole('button', { name: 'Create Template' }).click();
    
    // Fill form
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.getByLabel('Template Name').fill(templateName);
    await page.getByLabel('Description').fill('Test template description');
    await page.getByLabel('Template Content').fill(templateContent);
    
    // Select category
    await page.getByLabel('Category').click();
    await page.getByRole('option', { name: 'Custom' }).click();
    
    // Submit
    await page.getByRole('button', { name: 'Create Template' }).last().click();
    
    // Verify template created
    await expect(page.getByRole('dialog')).not.toBeVisible();
    await expect(page.getByText(templateName)).toBeVisible();
  });

  test('should preview template', async ({ page }) => {
    // Create a template first
    const template = await templatePage.createTemplate({
      name: `Preview Test ${randomString()}`,
      content: 'Topic: {{ topic }}\nParticipant: {{ participant }}',
      category: 'debate'
    });
    
    // Find and click preview button
    const templateCard = page.locator('.card').filter({ hasText: template.name });
    await templateCard.getByRole('button', { name: 'Preview' }).click();
    
    // Check preview dialog
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Template Preview')).toBeVisible();
    await expect(page.getByText(template.name)).toBeVisible();
    
    // Should show rendered content with placeholders
    await expect(page.getByText(/Topic:.*\[topic\]/)).toBeVisible();
    await expect(page.getByText(/Participant:.*\[participant\]/)).toBeVisible();
    
    // Close preview
    await page.getByRole('button', { name: 'Close' }).click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });

  test('should edit template', async ({ page }) => {
    // Create a template
    const template = await templatePage.createTemplate({
      name: `Edit Test ${randomString()}`,
      content: 'Original content',
      category: 'custom'
    });
    
    // Find and click edit button
    const templateCard = page.locator('.card').filter({ hasText: template.name });
    await templateCard.getByRole('button', { name: 'Edit' }).click();
    
    // Edit form should be pre-filled
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByLabel('Template Name')).toHaveValue(template.name);
    await expect(page.getByLabel('Template Content')).toHaveValue('Original content');
    
    // Update content
    const newContent = 'Updated content {{ variable }}';
    await page.getByLabel('Template Content').fill(newContent);
    
    // Submit
    await page.getByRole('button', { name: 'Update Template' }).click();
    
    // Verify update
    await expect(page.getByRole('dialog')).not.toBeVisible();
    
    // Preview to verify content changed
    await templateCard.getByRole('button', { name: 'Preview' }).click();
    await expect(page.getByText(/Updated content/)).toBeVisible();
  });

  test('should delete template', async ({ page }) => {
    // Create a template
    const template = await templatePage.createTemplate({
      name: `Delete Test ${randomString()}`,
      content: 'To be deleted',
      category: 'custom'
    });
    
    // Find template card
    const templateCard = page.locator('.card').filter({ hasText: template.name });
    await expect(templateCard).toBeVisible();
    
    // Click delete button
    page.on('dialog', dialog => dialog.accept()); // Auto-accept confirm dialog
    await templateCard.getByRole('button').filter({ has: page.locator('.lucide-trash-2') }).click();
    
    // Verify deleted
    await expect(templateCard).not.toBeVisible();
  });

  test('should search templates', async ({ page }) => {
    // Create templates with different names
    const templates = [
      await templatePage.createTemplate({ name: `Search Alpha ${randomString()}`, content: 'Content A' }),
      await templatePage.createTemplate({ name: `Search Beta ${randomString()}`, content: 'Content B' }),
      await templatePage.createTemplate({ name: `Different ${randomString()}`, content: 'Content C' })
    ];
    
    // Search for "Search"
    await page.getByPlaceholder('Search templates').fill('Search');
    await page.waitForTimeout(500); // Wait for debounce
    
    // Should show only matching templates
    await expect(page.getByText(templates[0].name)).toBeVisible();
    await expect(page.getByText(templates[1].name)).toBeVisible();
    await expect(page.getByText(templates[2].name)).not.toBeVisible();
    
    // Clear search
    await page.getByPlaceholder('Search templates').clear();
    await page.waitForTimeout(500);
    
    // All should be visible again
    for (const template of templates) {
      await expect(page.getByText(template.name)).toBeVisible();
    }
  });

  test('should filter by category', async ({ page }) => {
    // Create templates in different categories
    const debateTemplate = await templatePage.createTemplate({
      name: `Debate Template ${randomString()}`,
      category: 'debate'
    });
    
    const customTemplate = await templatePage.createTemplate({
      name: `Custom Template ${randomString()}`,
      category: 'custom'
    });
    
    // Filter by debate category
    await page.getByRole('combobox').click();
    await page.getByRole('option', { name: 'Debate' }).click();
    
    // Should show only debate templates
    await expect(page.getByText(debateTemplate.name)).toBeVisible();
    await expect(page.getByText(customTemplate.name)).not.toBeVisible();
    
    // Switch to custom category
    await page.getByRole('combobox').click();
    await page.getByRole('option', { name: 'Custom' }).click();
    
    // Should show only custom templates
    await expect(page.getByText(debateTemplate.name)).not.toBeVisible();
    await expect(page.getByText(customTemplate.name)).toBeVisible();
    
    // Reset to all
    await page.getByRole('combobox').click();
    await page.getByRole('option', { name: 'All' }).click();
    
    // Both should be visible
    await expect(page.getByText(debateTemplate.name)).toBeVisible();
    await expect(page.getByText(customTemplate.name)).toBeVisible();
  });

  test('should display template metadata', async ({ page }) => {
    const template = await templatePage.createTemplate({
      name: `Metadata Test ${randomString()}`,
      content: '{{ var1 }} and {{ var2 }}',
      category: 'debate',
      tags: ['test', 'example']
    });
    
    // Find template card
    const templateCard = page.locator('.card').filter({ hasText: template.name });
    
    // Check metadata display
    await expect(templateCard.getByText('debate')).toBeVisible();
    await expect(templateCard.getByText(/2 vars/)).toBeVisible(); // Variable count
    await expect(templateCard.getByText(/0 uses/)).toBeVisible(); // Usage count
  });

  test('should handle template with complex Jinja2 syntax', async ({ page }) => {
    const complexTemplate = `
{% for participant in participants %}
  - {{ participant.name }}: {{ participant.position }}
{% endfor %}

{% if show_rules %}
## Rules
{% for rule in rules %}
{{ loop.index }}. {{ rule }}
{% endfor %}
{% endif %}
`;
    
    const template = await templatePage.createTemplate({
      name: `Complex Template ${randomString()}`,
      content: complexTemplate,
      category: 'debate'
    });
    
    // Preview should work without errors
    const templateCard = page.locator('.card').filter({ hasText: template.name });
    await templateCard.getByRole('button', { name: 'Preview' }).click();
    
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Template Preview')).toBeVisible();
    
    // Close preview
    await page.getByRole('button', { name: 'Close' }).click();
  });

  test('should validate template form', async ({ page }) => {
    await page.getByRole('button', { name: 'Create Template' }).click();
    
    // Try to submit empty form
    await page.getByRole('button', { name: 'Create Template' }).last().click();
    
    // Should remain in dialog (validation failed)
    await expect(page.getByRole('dialog')).toBeVisible();
    
    // Fill required fields
    await page.getByLabel('Template Name').fill('Valid Template');
    await page.getByLabel('Template Content').fill('Valid content');
    
    // Should now submit successfully
    await page.getByRole('button', { name: 'Create Template' }).last().click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });
});