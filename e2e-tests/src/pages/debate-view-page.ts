import { Page } from 'puppeteer';
import { PageHelpers } from '../helpers/page-helpers';

export class DebateViewPage {
  private helpers: PageHelpers;

  constructor(private page: Page) {
    this.helpers = new PageHelpers(page);
  }

  async isLoaded(): Promise<boolean> {
    return await this.helpers.isVisible('[data-testid="debate-header"]');
  }

  async getDebateTitle(): Promise<string> {
    return await this.helpers.getText('[data-testid="debate-title"]');
  }

  async getDebateTopic(): Promise<string> {
    return await this.helpers.getText('[data-testid="debate-topic"]');
  }

  async getDebateStatus(): Promise<string> {
    return await this.helpers.getText('[data-testid="debate-status-badge"]');
  }

  async startDebate() {
    await this.helpers.clickElementWithText('button', 'Start Debate');
  }

  async pauseDebate() {
    await this.helpers.clickElementWithText('button', 'Pause');
  }

  async resumeDebate() {
    await this.helpers.clickElementWithText('button', 'Resume');
  }

  async skipTurn() {
    await this.helpers.clickElementWithText('button', 'Skip Turn');
  }

  async waitForSpeakerIndicator(speakerName: string) {
    await this.helpers.waitForText(`${speakerName} is thinking...`);
  }

  async getParticipants(): Promise<Array<{
    name: string;
    position: string;
    role: string;
    provider: string;
    model: string;
    isSpeaking: boolean;
  }>> {
    await this.page.waitForSelector('[data-testid="participant-card"]');
    return await this.page.evaluate(() => {
      const cards = Array.from(document.querySelectorAll('[data-testid="participant-card"]'));
      return cards.map(card => ({
        name: card.querySelector('h4')?.textContent?.trim() || '',
        position: card.querySelector('.text-muted-foreground')?.textContent?.trim() || '',
        role: card.querySelector('[data-testid="role-badge"]')?.textContent?.trim() || '',
        provider: card.querySelector('[data-testid="provider"]')?.textContent?.trim() || '',
        model: card.querySelector('[data-testid="model"]')?.textContent?.trim() || '',
        isSpeaking: !!card.querySelector('[data-testid="speaking-badge"]')
      }));
    });
  }

  async getTurns(): Promise<Array<{
    participantName: string;
    turnType: string;
    content: string;
    round: number;
    turn: number;
  }>> {
    await this.page.waitForSelector('[data-testid="turn-message"]', { timeout: 5000 }).catch(() => {});
    return await this.page.evaluate(() => {
      const turns = Array.from(document.querySelectorAll('[data-testid="turn-message"]'));
      return turns.map(turn => {
        const metadata = turn.querySelector('[data-testid="turn-metadata"]')?.textContent || '';
        const roundMatch = metadata.match(/Round (\d+)/);
        const turnMatch = metadata.match(/Turn (\d+)/);
        
        return {
          participantName: turn.querySelector('[data-testid="participant-name"]')?.textContent?.trim() || '',
          turnType: turn.querySelector('[data-testid="turn-type"]')?.textContent?.trim() || '',
          content: turn.querySelector('[data-testid="turn-content"]')?.textContent?.trim() || '',
          round: roundMatch ? parseInt(roundMatch[1]) : 0,
          turn: turnMatch ? parseInt(turnMatch[1]) : 0
        };
      });
    });
  }

  async waitForTurn(turnNumber: number, timeout = 30000) {
    await this.page.waitForFunction(
      (turnNum) => {
        const turns = document.querySelectorAll('[data-testid="turn-message"]');
        return turns.length >= turnNum;
      },
      { timeout },
      turnNumber
    );
  }

  async getCurrentRound(): Promise<number> {
    const text = await this.helpers.getText('[data-testid="round-info"]');
    const match = text.match(/Round (\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  async getTotalRounds(): Promise<number> {
    const text = await this.helpers.getText('[data-testid="round-info"]');
    const match = text.match(/of (\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  async getTurnCount(): Promise<number> {
    const text = await this.helpers.getText('[data-testid="turn-count"]');
    const match = text.match(/(\d+) Turns/);
    return match ? parseInt(match[1]) : 0;
  }

  async isDebateComplete(): Promise<boolean> {
    const status = await this.getDebateStatus();
    return status.toLowerCase() === 'completed';
  }

  async waitForDebateCompletion(timeout = 120000) {
    await this.page.waitForFunction(
      () => {
        const statusBadge = document.querySelector('[data-testid="debate-status-badge"]');
        return statusBadge?.textContent?.toLowerCase() === 'completed';
      },
      { timeout }
    );
  }

  async scrollToLatestTurn() {
    await this.page.evaluate(() => {
      const scrollArea = document.querySelector('[data-testid="transcript-scroll-area"]');
      if (scrollArea) {
        scrollArea.scrollTop = scrollArea.scrollHeight;
      }
    });
  }

  async takeDebateScreenshot(name: string) {
    await this.helpers.takeScreenshot(`debate-${name}`);
  }
}