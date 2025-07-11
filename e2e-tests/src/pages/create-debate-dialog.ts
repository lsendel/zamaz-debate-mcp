import { Page } from 'puppeteer';
import { PageHelpers } from '../helpers/page-helpers';

export class CreateDebateDialog {
  private helpers: PageHelpers;

  constructor(private page: Page) {
    this.helpers = new PageHelpers(page);
  }

  async isOpen(): Promise<boolean> {
    return await this.helpers.isVisible('[role="dialog"]');
  }

  async fillDebateName(name: string) {
    await this.helpers.clearAndType('input[id="name"]', name);
  }

  async fillTopic(topic: string) {
    await this.helpers.clearAndType('input[id="topic"]', topic);
  }

  async fillDescription(description: string) {
    await this.helpers.clearAndType('textarea[id="description"]', description);
  }

  async selectFormat(format: 'round_robin' | 'free_form' | 'oxford' | 'panel') {
    await this.helpers.waitAndClick('[data-testid="format-select"]');
    await this.helpers.clickElementWithText('[role="option"]', format.replace('_', ' '));
  }

  async setMaxRounds(rounds: number) {
    await this.helpers.clearAndType('input[type="number"][min="1"][max="20"]', rounds.toString());
  }

  async addParticipant() {
    await this.helpers.clickElementWithText('button', 'Add Participant');
  }

  async removeParticipant(index: number) {
    const removeButtons = await this.page.$$('button[aria-label*="Remove"]');
    if (removeButtons[index]) {
      await removeButtons[index].click();
    }
  }

  async fillParticipant(index: number, data: {
    name: string;
    position?: string;
    provider: 'llama' | 'claude' | 'openai' | 'gemini';
    model: string;
    temperature?: number;
    role?: 'debater' | 'moderator' | 'judge' | 'observer';
    systemPrompt?: string;
  }) {
    const participantCard = await this.page.$$('[data-testid="participant-card"]');
    if (!participantCard[index]) {
      throw new Error(`Participant ${index} not found`);
    }

    // Fill name
    const nameInputs = await this.page.$$('input[placeholder="Participant name"]');
    await nameInputs[index].click({ clickCount: 3 });
    await this.page.keyboard.press('Backspace');
    await this.page.keyboard.type(data.name);

    // Fill position if provided
    if (data.position) {
      const positionInputs = await this.page.$$('input[placeholder*="Pro-regulation"]');
      await positionInputs[index].click({ clickCount: 3 });
      await this.page.keyboard.press('Backspace');
      await this.page.keyboard.type(data.position);
    }

    // Select provider tab
    const providerTabSelector = `button[role="tab"][value="${data.provider}"]`;
    const providerTabs = await this.page.$$(providerTabSelector);
    if (providerTabs[index]) {
      await providerTabs[index].click();
      await this.page.waitForFunction(() => new Promise(resolve => setTimeout(resolve, 200)));
    }

    // Select model
    const modelSelects = await this.page.$$('[data-testid="model-select"]');
    if (modelSelects[index]) {
      await modelSelects[index].click();
      await this.helpers.clickElementWithText('[role="option"]', data.model);
    }

    // Set temperature if provided
    if (data.temperature !== undefined) {
      const tempInputs = await this.page.$$('input[type="number"][step="0.1"]');
      await tempInputs[index].click({ clickCount: 3 });
      await this.page.keyboard.press('Backspace');
      await this.page.keyboard.type(data.temperature.toString());
    }

    // Set role if provided
    if (data.role) {
      const roleSelects = await this.page.$$('[data-testid="role-select"]');
      if (roleSelects[index]) {
        await roleSelects[index].click();
        await this.helpers.clickElementWithText('[role="option"]', data.role);
      }
    }

    // Set system prompt if provided
    if (data.systemPrompt) {
      const promptTextareas = await this.page.$$('textarea[placeholder*="personality"]');
      await promptTextareas[index].click();
      await this.page.keyboard.type(data.systemPrompt);
    }
  }

  async getParticipantCount(): Promise<number> {
    const cards = await this.page.$$('[data-testid="participant-card"]');
    return cards.length;
  }

  async submit() {
    await this.helpers.clickElementWithText('button', 'Create Debate');
  }

  async cancel() {
    await this.helpers.clickElementWithText('button', 'Cancel');
  }

  async getValidationError(): Promise<string | null> {
    // Check for alert dialog
    try {
      await this.page.waitForFunction(
        () => window.alert.toString().includes('native code') === false,
        { timeout: 1000 }
      );
      return await this.page.evaluate(() => {
        const lastAlert = (window as any).__lastAlert;
        return lastAlert || null;
      });
    } catch {
      return null;
    }
  }
}