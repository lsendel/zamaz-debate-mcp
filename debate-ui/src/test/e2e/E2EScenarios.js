/**
 * End-to-End test scenarios for user journey testing.
 * Provides comprehensive user flow testing with Playwright.
 */

import { test, expect } from '@playwright/test';

/**
 * User Authentication Flow
 */
export class AuthenticationScenarios {
  constructor(page) {
    this.page = page;
  }

  async loginSuccessfully(email = 'test@example.com', password = 'password123') {
    await test.step('Navigate to login page', async () => {
      await this.page.goto('/login');
      await expect(this.page).toHaveTitle(/Login/);
    });

    await test.step('Fill login form', async () => {
      await this.page.fill('input[name="email"]', email);
      await this.page.fill('input[name="password"]', password);
    });

    await test.step('Submit login form', async () => {
      await this.page.click('button[type="submit"]');
      await this.page.waitForURL('/dashboard');
      await expect(this.page.locator('[data-testid="user-menu"]')).toBeVisible();
    });
  }

  async loginWithInvalidCredentials() {
    await test.step('Navigate to login page', async () => {
      await this.page.goto('/login');
    });

    await test.step('Submit invalid credentials', async () => {
      await this.page.fill('input[name="email"]', 'invalid@example.com');
      await this.page.fill('input[name="password"]', 'wrongpassword');
      await this.page.click('button[type="submit"]');
    });

    await test.step('Verify error message', async () => {
      await expect(this.page.locator('[data-testid="error-message"]')).toContainText('Invalid credentials');
    });
  }

  async logout() {
    await test.step('Open user menu', async () => {
      await this.page.click('[data-testid="user-menu"]');
    });

    await test.step('Click logout', async () => {
      await this.page.click('[data-testid="logout-button"]');
      await this.page.waitForURL('/login');
    });
  }

  async forgotPassword(email = 'test@example.com') {
    await test.step('Navigate to forgot password', async () => {
      await this.page.goto('/login');
      await this.page.click('[data-testid="forgot-password-link"]');
    });

    await test.step('Submit email for password reset', async () => {
      await this.page.fill('input[name="email"]', email);
      await this.page.click('button[type="submit"]');
    });

    await test.step('Verify success message', async () => {
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Password reset email sent');
    });
  }
}

/**
 * Debate Management Flow
 */
export class DebateScenarios {
  constructor(page) {
    this.page = page;
  }

  async createDebate(debateData = {}) {
    const {
      title = 'Test Debate',
      description = 'A test debate for E2E testing',
      participants = ['Claude', 'GPT-4'],
      rounds = 3
    } = debateData;

    await test.step('Navigate to create debate page', async () => {
      await this.page.click('[data-testid="create-debate-button"]');
      await expect(this.page.locator('h1')).toContainText('Create Debate');
    });

    await test.step('Fill debate form', async () => {
      await this.page.fill('input[name="title"]', title);
      await this.page.fill('textarea[name="description"]', description);
      await this.page.selectOption('select[name="rounds"]', rounds.toString());
    });

    await test.step('Add participants', async () => {
      for (const participant of participants) {
        await this.page.click('[data-testid="add-participant-button"]');
        await this.page.fill('[data-testid="participant-name"]:last-child', participant);
        await this.page.selectOption('[data-testid="participant-provider"]:last-child', 'anthropic');
      }
    });

    await test.step('Submit debate creation', async () => {
      await this.page.click('button[type="submit"]');
      await this.page.waitForURL(/\/debates\/[^/]+$/);
      await expect(this.page.locator('h1')).toContainText(title);
    });

    return {
      title,
      url: this.page.url()
    };
  }

  async startDebate() {
    await test.step('Start the debate', async () => {
      await this.page.click('[data-testid="start-debate-button"]');
      await expect(this.page.locator('[data-testid="debate-status"]')).toContainText('In Progress');
    });
  }

  async viewDebateList() {
    await test.step('Navigate to debates list', async () => {
      await this.page.click('[data-testid="debates-nav-link"]');
      await expect(this.page.locator('h1')).toContainText('Debates');
    });

    await test.step('Verify debates are displayed', async () => {
      await expect(this.page.locator('[data-testid="debate-card"]')).toHaveCount({ min: 1 });
    });
  }

  async searchDebates(searchTerm) {
    await test.step('Search for debates', async () => {
      await this.page.fill('[data-testid="debate-search"]', searchTerm);
      await this.page.press('[data-testid="debate-search"]', 'Enter');
    });

    await test.step('Verify search results', async () => {
      const debateCards = this.page.locator('[data-testid="debate-card"]');
      await expect(debateCards.first()).toContainText(searchTerm);
    });
  }

  async filterDebatesByStatus(status) {
    await test.step('Apply status filter', async () => {
      await this.page.selectOption('[data-testid="status-filter"]', status);
    });

    await test.step('Verify filtered results', async () => {
      const statusBadges = this.page.locator('[data-testid="debate-status"]');
      const count = await statusBadges.count();
      
      for (let i = 0; i < count; i++) {
        await expect(statusBadges.nth(i)).toContainText(status);
      }
    });
  }

  async deleteDebate() {
    await test.step('Open debate actions menu', async () => {
      await this.page.click('[data-testid="debate-actions-menu"]');
    });

    await test.step('Click delete option', async () => {
      await this.page.click('[data-testid="delete-debate-option"]');
    });

    await test.step('Confirm deletion', async () => {
      await this.page.click('[data-testid="confirm-delete-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Debate deleted');
    });
  }

  async duplicateDebate() {
    await test.step('Open debate actions menu', async () => {
      await this.page.click('[data-testid="debate-actions-menu"]');
    });

    await test.step('Click duplicate option', async () => {
      await this.page.click('[data-testid="duplicate-debate-option"]');
    });

    await test.step('Verify debate is duplicated', async () => {
      await this.page.waitForURL(/\/debates\/[^/]+$/);
      await expect(this.page.locator('h1')).toContainText('Copy of');
    });
  }
}

/**
 * Organization Management Flow
 */
export class OrganizationScenarios {
  constructor(page) {
    this.page = page;
  }

  async switchOrganization(organizationName) {
    await test.step('Open organization selector', async () => {
      await this.page.click('[data-testid="organization-selector"]');
    });

    await test.step('Select organization', async () => {
      await this.page.click(`[data-testid="org-option-${organizationName}"]`);
    });

    await test.step('Verify organization switch', async () => {
      await expect(this.page.locator('[data-testid="current-organization"]')).toContainText(organizationName);
    });
  }

  async createOrganization(orgData = {}) {
    const {
      name = 'Test Organization',
      description = 'A test organization'
    } = orgData;

    await test.step('Navigate to create organization', async () => {
      await this.page.click('[data-testid="create-organization-button"]');
    });

    await test.step('Fill organization form', async () => {
      await this.page.fill('input[name="name"]', name);
      await this.page.fill('textarea[name="description"]', description);
    });

    await test.step('Submit organization creation', async () => {
      await this.page.click('button[type="submit"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Organization created');
    });
  }

  async inviteUser(email, role = 'USER') {
    await test.step('Navigate to team management', async () => {
      await this.page.click('[data-testid="team-nav-link"]');
    });

    await test.step('Open invite user dialog', async () => {
      await this.page.click('[data-testid="invite-user-button"]');
    });

    await test.step('Fill invitation form', async () => {
      await this.page.fill('input[name="email"]', email);
      await this.page.selectOption('select[name="role"]', role);
    });

    await test.step('Send invitation', async () => {
      await this.page.click('[data-testid="send-invite-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Invitation sent');
    });
  }

  async manageUserPermissions(userEmail, permissions) {
    await test.step('Find user in team list', async () => {
      const userRow = this.page.locator(`[data-testid="user-row"][data-email="${userEmail}"]`);
      await userRow.click('[data-testid="manage-permissions-button"]');
    });

    await test.step('Update permissions', async () => {
      for (const permission of permissions) {
        await this.page.check(`input[name="permission-${permission}"]`);
      }
    });

    await test.step('Save permissions', async () => {
      await this.page.click('[data-testid="save-permissions-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Permissions updated');
    });
  }
}

/**
 * Settings and Configuration Flow
 */
export class SettingsScenarios {
  constructor(page) {
    this.page = page;
  }

  async updateProfile(profileData = {}) {
    const {
      firstName = 'John',
      lastName = 'Doe',
      email = 'john.doe@example.com'
    } = profileData;

    await test.step('Navigate to profile settings', async () => {
      await this.page.click('[data-testid="user-menu"]');
      await this.page.click('[data-testid="profile-settings-link"]');
    });

    await test.step('Update profile information', async () => {
      await this.page.fill('input[name="firstName"]', firstName);
      await this.page.fill('input[name="lastName"]', lastName);
      await this.page.fill('input[name="email"]', email);
    });

    await test.step('Save profile changes', async () => {
      await this.page.click('[data-testid="save-profile-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Profile updated');
    });
  }

  async changePassword(currentPassword, newPassword) {
    await test.step('Navigate to security settings', async () => {
      await this.page.click('[data-testid="security-settings-tab"]');
    });

    await test.step('Fill password change form', async () => {
      await this.page.fill('input[name="currentPassword"]', currentPassword);
      await this.page.fill('input[name="newPassword"]', newPassword);
      await this.page.fill('input[name="confirmPassword"]', newPassword);
    });

    await test.step('Submit password change', async () => {
      await this.page.click('[data-testid="change-password-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Password updated');
    });
  }

  async configureNotifications(preferences) {
    await test.step('Navigate to notification settings', async () => {
      await this.page.click('[data-testid="notification-settings-tab"]');
    });

    await test.step('Update notification preferences', async () => {
      for (const [type, enabled] of Object.entries(preferences)) {
        const checkbox = this.page.locator(`input[name="notification-${type}"]`);
        if (enabled) {
          await checkbox.check();
        } else {
          await checkbox.uncheck();
        }
      }
    });

    await test.step('Save notification settings', async () => {
      await this.page.click('[data-testid="save-notifications-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('Notifications updated');
    });
  }

  async updateLLMProviderSettings(providers) {
    await test.step('Navigate to LLM settings', async () => {
      await this.page.click('[data-testid="llm-settings-tab"]');
    });

    await test.step('Configure LLM providers', async () => {
      for (const [provider, config] of Object.entries(providers)) {
        if (config.apiKey) {
          await this.page.fill(`input[name="${provider}-api-key"]`, config.apiKey);
        }
        if (config.enabled !== undefined) {
          const toggle = this.page.locator(`[data-testid="${provider}-toggle"]`);
          if (config.enabled) {
            await toggle.check();
          } else {
            await toggle.uncheck();
          }
        }
      }
    });

    await test.step('Save LLM settings', async () => {
      await this.page.click('[data-testid="save-llm-settings-button"]');
      await expect(this.page.locator('[data-testid="success-message"]')).toContainText('LLM settings updated');
    });
  }
}

/**
 * Real-time Features Flow
 */
export class RealTimeScenarios {
  constructor(page) {
    this.page = page;
  }

  async testLiveDebateUpdates() {
    await test.step('Join a live debate', async () => {
      await this.page.goto('/debates/live-debate-1');
      await expect(this.page.locator('[data-testid="debate-status"]')).toContainText('In Progress');
    });

    await test.step('Verify real-time turn updates', async () => {
      // Wait for a new turn to appear
      await expect(this.page.locator('[data-testid="turn-item"]')).toHaveCount({ min: 1 });
      
      // Verify the turn content updates in real-time
      await expect(this.page.locator('[data-testid="turn-content"]').first()).not.toBeEmpty();
    });

    await test.step('Test participant status updates', async () => {
      await expect(this.page.locator('[data-testid="participant-status"]')).toContainText('Active');
    });
  }

  async testCollaborativeEditing() {
    await test.step('Open debate template editor', async () => {
      await this.page.goto('/templates/collaborative-template');
      await expect(this.page.locator('[data-testid="editor"]')).toBeVisible();
    });

    await test.step('Make changes and verify real-time sync', async () => {
      await this.page.fill('[data-testid="template-content"]', 'Updated template content');
      
      // Verify changes are saved automatically
      await expect(this.page.locator('[data-testid="save-status"]')).toContainText('Saved');
    });
  }

  async testNotificationSystem() {
    await test.step('Trigger a notification event', async () => {
      // This would typically be triggered by another user's action
      await this.page.evaluate(() => {
        window.dispatchEvent(new CustomEvent('notification', {
          detail: { message: 'New debate invitation received', type: 'info' }
        }));
      });
    });

    await test.step('Verify notification appears', async () => {
      await expect(this.page.locator('[data-testid="notification-toast"]')).toContainText('New debate invitation');
    });

    await test.step('Dismiss notification', async () => {
      await this.page.click('[data-testid="dismiss-notification"]');
      await expect(this.page.locator('[data-testid="notification-toast"]')).not.toBeVisible();
    });
  }
}

/**
 * Error Handling and Edge Cases
 */
export class ErrorScenarios {
  constructor(page) {
    this.page = page;
  }

  async testNetworkErrorHandling() {
    await test.step('Simulate network failure', async () => {
      await this.page.route('**/api/**', route => route.abort());
    });

    await test.step('Attempt to load debates', async () => {
      await this.page.goto('/debates');
    });

    await test.step('Verify error message is displayed', async () => {
      await expect(this.page.locator('[data-testid="error-message"]')).toContainText('Network error');
    });

    await test.step('Test retry functionality', async () => {
      // Restore network
      await this.page.unroute('**/api/**');
      
      await this.page.click('[data-testid="retry-button"]');
      await expect(this.page.locator('[data-testid="debate-card"]')).toHaveCount({ min: 1 });
    });
  }

  async testInvalidDataHandling() {
    await test.step('Submit form with invalid data', async () => {
      await this.page.goto('/debates/new');
      await this.page.fill('input[name="title"]', ''); // Empty title
      await this.page.click('button[type="submit"]');
    });

    await test.step('Verify validation errors', async () => {
      await expect(this.page.locator('[data-testid="validation-error"]')).toContainText('Title is required');
    });
  }

  async testUnauthorizedAccess() {
    await test.step('Clear authentication', async () => {
      await this.page.evaluate(() => localStorage.clear());
    });

    await test.step('Attempt to access protected route', async () => {
      await this.page.goto('/admin/users');
    });

    await test.step('Verify redirect to login', async () => {
      await expect(this.page).toHaveURL(/\/login/);
    });
  }

  async testResourceNotFound() {
    await test.step('Navigate to non-existent debate', async () => {
      await this.page.goto('/debates/non-existent-id');
    });

    await test.step('Verify 404 page is displayed', async () => {
      await expect(this.page.locator('h1')).toContainText('404');
      await expect(this.page.locator('[data-testid="not-found-message"]')).toContainText('Debate not found');
    });
  }
}

/**
 * Performance and Accessibility Scenarios
 */
export class PerformanceScenarios {
  constructor(page) {
    this.page = page;
  }

  async testPageLoadPerformance() {
    await test.step('Measure page load time', async () => {
      const startTime = Date.now();
      await this.page.goto('/debates');
      await this.page.waitForLoadState('networkidle');
      const loadTime = Date.now() - startTime;
      
      expect(loadTime).toBeLessThan(3000); // Should load within 3 seconds
    });
  }

  async testAccessibilityCompliance() {
    await test.step('Check keyboard navigation', async () => {
      await this.page.goto('/debates');
      
      // Test tab navigation
      await this.page.keyboard.press('Tab');
      await expect(this.page.locator(':focus')).toBeVisible();
    });

    await test.step('Verify ARIA labels', async () => {
      const buttons = this.page.locator('button');
      const count = await buttons.count();
      
      for (let i = 0; i < count; i++) {
        const button = buttons.nth(i);
        const ariaLabel = await button.getAttribute('aria-label');
        const text = await button.textContent();
        
        expect(ariaLabel || text).toBeTruthy();
      }
    });

    await test.step('Test screen reader compatibility', async () => {
      // Check for proper heading hierarchy
      const headings = this.page.locator('h1, h2, h3, h4, h5, h6');
      const count = await headings.count();
      expect(count).toBeGreaterThan(0);
    });
  }

  async testLargeDataSetHandling() {
    await test.step('Load page with many debates', async () => {
      // Mock API to return large dataset
      await this.page.route('**/api/debates', route => {
        const largeDataset = Array.from({ length: 1000 }, (_, i) => ({
          id: `debate-${i}`,
          title: `Debate ${i}`,
          status: 'CREATED',
          createdAt: new Date().toISOString(),
        }));
        
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ debates: largeDataset }),
        });
      });
      
      await this.page.goto('/debates');
    });

    await test.step('Verify virtualization works properly', async () => {
      // Should not render all 1000 items at once
      const visibleItems = this.page.locator('[data-testid="debate-card"]');
      const count = await visibleItems.count();
      expect(count).toBeLessThan(100); // Virtualized list should render only visible items
    });
  }
}

/**
 * Integration Test Suite
 */
export class IntegrationTestSuite {
  constructor(page) {
    this.page = page;
    this.auth = new AuthenticationScenarios(page);
    this.debates = new DebateScenarios(page);
    this.organizations = new OrganizationScenarios(page);
    this.settings = new SettingsScenarios(page);
    this.realTime = new RealTimeScenarios(page);
    this.errors = new ErrorScenarios(page);
    this.performance = new PerformanceScenarios(page);
  }

  async runCompleteUserJourney() {
    await test.step('Complete user journey test', async () => {
      // 1. Authentication
      await this.auth.loginSuccessfully();
      
      // 2. Create a debate
      const debate = await this.debates.createDebate({
        title: 'Integration Test Debate',
        description: 'Testing the complete user journey'
      });
      
      // 3. Start the debate
      await this.debates.startDebate();
      
      // 4. Switch organizations
      await this.organizations.switchOrganization('Test Organization');
      
      // 5. Update profile
      await this.settings.updateProfile({
        firstName: 'Integration',
        lastName: 'Test'
      });
      
      // 6. View debates and verify our created debate
      await this.debates.viewDebateList();
      await this.debates.searchDebates('Integration Test');
      
      // 7. Logout
      await this.auth.logout();
    });
  }
}