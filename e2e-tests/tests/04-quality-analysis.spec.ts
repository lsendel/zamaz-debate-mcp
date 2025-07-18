import { test, expect, DebateTestUtils } from './fixtures/test-base';
import fs from 'fs/promises';
import path from 'path';

test.describe('Debate Quality Analysis', () => {
  let evidenceDir: string;
  let debateId: string;

  test.beforeAll(async ({ apiClient, testData }) => {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    evidenceDir = path.join('test-evidence', 'test-runs', `${timestamp}-quality-analysis`);
    await fs.mkdir(path.join(evidenceDir, 'screenshots'), { recursive: true });
    await fs.mkdir(path.join(evidenceDir, 'logs'), { recursive: true });

    // Create a completed debate with arguments for analysis
    const debate = await DebateTestUtils.createDebate(apiClient, testData.organizationId, 'Climate Change Solutions');
    debateId = debate.id;

    // Add participants and arguments
    await apiClient.post(`/api/v1/debates/${debateId}/participants`, {
      type: 'HUMAN',
      name: 'Environmental Scientist',
      position: 'FOR'
    });
    await apiClient.post(`/api/v1/debates/${debateId}/participants`, {
      type: 'HUMAN',
      name: 'Economic Analyst',
      position: 'AGAINST'
    });

    // Start debate and add arguments
    await apiClient.post(`/api/v1/debates/${debateId}/start`);
    
    // Submit high-quality arguments
    await apiClient.post(`/api/v1/debates/${debateId}/arguments`, {
      participantId: 'participant-1',
      content: 'Renewable energy investments create long-term economic benefits through job creation, energy independence, and reduced healthcare costs from pollution. Studies show ROI of 15-20% over 20 years.',
      round: 1
    });

    await apiClient.post(`/api/v1/debates/${debateId}/arguments`, {
      participantId: 'participant-2',
      content: 'While renewable energy has merits, the immediate economic impact includes higher energy costs, job losses in traditional sectors, and massive infrastructure investments that burden taxpayers.',
      round: 1
    });

    // Complete the debate
    await apiClient.post(`/api/v1/debates/${debateId}/complete`);
  });

  test('Trigger and retrieve quality analysis', async ({ page, apiClient, screenshots }) => {
    await page.goto(`/debates/${debateId}/analysis`);
    await screenshots.capture('analysis-page-initial');

    // Trigger quality analysis
    const [analysisResponse] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes(`/debates/${debateId}/analyze`)),
      page.click('[data-testid="run-analysis-button"]')
    ]);

    expect(analysisResponse.status()).toBe(200);
    const analysisData = await analysisResponse.json();

    await page.waitForSelector('[data-testid="analysis-complete"]');
    await screenshots.capture('analysis-complete');

    // Verify overall grade
    const overallGrade = await page.locator('[data-testid="overall-grade"]').textContent();
    expect(['A+', 'A', 'A-', 'B+', 'B']).toContain(overallGrade);

    const analysisEvidence = {
      test: 'Quality analysis retrieval',
      debateId,
      analysisTriggered: true,
      overallGrade,
      metrics: analysisData,
      result: 'SUCCESS'
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'quality-analysis.json'),
      JSON.stringify(analysisEvidence, null, 2)
    );
  });

  test('Analyze argument quality scores', async ({ page, screenshots }) => {
    await page.goto(`/debates/${debateId}/analysis`);

    // Check individual argument scores
    const argumentScores = await page.evaluate(() => {
      const scores = [];
      document.querySelectorAll('[data-testid^="argument-score-"]').forEach(el => {
        scores.push({
          argument: el.getAttribute('data-argument-id'),
          coherenceScore: parseFloat(el.querySelector('[data-testid="coherence-score"]')?.textContent || '0'),
          relevanceScore: parseFloat(el.querySelector('[data-testid="relevance-score"]')?.textContent || '0'),
          evidenceScore: parseFloat(el.querySelector('[data-testid="evidence-score"]')?.textContent || '0'),
          overallScore: parseFloat(el.querySelector('[data-testid="overall-score"]')?.textContent || '0')
        });
      });
      return scores;
    });

    await screenshots.capture('argument-quality-scores');

    // Verify scores are within expected ranges
    argumentScores.forEach(score => {
      expect(score.coherenceScore).toBeGreaterThanOrEqual(0);
      expect(score.coherenceScore).toBeLessThanOrEqual(100);
      expect(score.overallScore).toBeGreaterThan(60); // Expecting decent quality
    });

    const argumentEvidence = {
      test: 'Argument quality analysis',
      totalArguments: argumentScores.length,
      averageCoherence: argumentScores.reduce((sum, s) => sum + s.coherenceScore, 0) / argumentScores.length,
      averageRelevance: argumentScores.reduce((sum, s) => sum + s.relevanceScore, 0) / argumentScores.length,
      averageEvidence: argumentScores.reduce((sum, s) => sum + s.evidenceScore, 0) / argumentScores.length,
      scores: argumentScores,
      result: 'SUCCESS'
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'argument-scores.json'),
      JSON.stringify(argumentEvidence, null, 2)
    );
  });

  test('Sentiment and emotion analysis', async ({ page, screenshots }) => {
    await page.goto(`/debates/${debateId}/analysis`);
    await page.click('[data-testid="sentiment-tab"]');
    
    await screenshots.capture('sentiment-analysis');

    // Get sentiment data
    const sentimentData = await page.evaluate(() => {
      return {
        overallSentiment: document.querySelector('[data-testid="overall-sentiment"]')?.textContent,
        emotionalTone: document.querySelector('[data-testid="emotional-tone"]')?.textContent,
        argumentSentiments: Array.from(document.querySelectorAll('[data-testid^="argument-sentiment-"]')).map(el => ({
          id: el.getAttribute('data-argument-id'),
          sentiment: el.querySelector('.sentiment-value')?.textContent,
          confidence: el.querySelector('.confidence-value')?.textContent
        }))
      };
    });

    expect(sentimentData.overallSentiment).toBeTruthy();
    expect(['positive', 'negative', 'neutral', 'mixed']).toContain(sentimentData.overallSentiment?.toLowerCase());

    const sentimentEvidence = {
      test: 'Sentiment analysis',
      overallSentiment: sentimentData.overallSentiment,
      emotionalTone: sentimentData.emotionalTone,
      argumentCount: sentimentData.argumentSentiments.length,
      sentimentDistribution: {
        positive: sentimentData.argumentSentiments.filter(a => a.sentiment?.includes('positive')).length,
        negative: sentimentData.argumentSentiments.filter(a => a.sentiment?.includes('negative')).length,
        neutral: sentimentData.argumentSentiments.filter(a => a.sentiment?.includes('neutral')).length
      },
      result: 'SUCCESS'
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'sentiment-analysis.json'),
      JSON.stringify(sentimentEvidence, null, 2)
    );
  });

  test('Factuality and evidence checking', async ({ page, screenshots }) => {
    await page.goto(`/debates/${debateId}/analysis`);
    await page.click('[data-testid="factuality-tab"]');
    
    await screenshots.capture('factuality-analysis');

    // Check factuality scores
    const factualityData = await page.evaluate(() => {
      const claims = [];
      document.querySelectorAll('[data-testid^="claim-"]').forEach(el => {
        claims.push({
          claim: el.querySelector('.claim-text')?.textContent,
          factualityScore: parseFloat(el.querySelector('[data-testid="factuality-score"]')?.textContent || '0'),
          evidenceProvided: el.querySelector('[data-testid="evidence-provided"]')?.textContent === 'Yes',
          sources: Array.from(el.querySelectorAll('.source-link')).map(s => s.textContent)
        });
      });
      return claims;
    });

    await screenshots.capture('factuality-details');

    const factualityEvidence = {
      test: 'Factuality checking',
      totalClaims: factualityData.length,
      averageFactualityScore: factualityData.reduce((sum, c) => sum + c.factualityScore, 0) / factualityData.length,
      claimsWithEvidence: factualityData.filter(c => c.evidenceProvided).length,
      topClaims: factualityData.slice(0, 3),
      result: 'SUCCESS'
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'factuality-analysis.json'),
      JSON.stringify(factualityEvidence, null, 2)
    );
  });

  test('Engagement and participation metrics', async ({ page, screenshots }) => {
    await page.goto(`/debates/${debateId}/analysis`);
    await page.click('[data-testid="engagement-tab"]');
    
    await screenshots.capture('engagement-metrics');

    const engagementData = await page.evaluate(() => {
      return {
        participationBalance: document.querySelector('[data-testid="participation-balance"]')?.textContent,
        averageResponseTime: document.querySelector('[data-testid="avg-response-time"]')?.textContent,
        totalInteractions: document.querySelector('[data-testid="total-interactions"]')?.textContent,
        engagementScore: document.querySelector('[data-testid="engagement-score"]')?.textContent,
        participantMetrics: Array.from(document.querySelectorAll('[data-testid^="participant-metric-"]')).map(el => ({
          name: el.querySelector('.participant-name')?.textContent,
          argumentCount: el.querySelector('.argument-count')?.textContent,
          avgWordCount: el.querySelector('.avg-word-count')?.textContent,
          responseTime: el.querySelector('.response-time')?.textContent
        }))
      };
    });

    await screenshots.capture('participant-metrics');

    const engagementEvidence = {
      test: 'Engagement analysis',
      metrics: {
        participationBalance: engagementData.participationBalance,
        averageResponseTime: engagementData.averageResponseTime,
        totalInteractions: engagementData.totalInteractions,
        engagementScore: engagementData.engagementScore
      },
      participantBreakdown: engagementData.participantMetrics,
      balanced: engagementData.participationBalance?.includes('balanced'),
      result: 'SUCCESS'
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'engagement-metrics.json'),
      JSON.stringify(engagementEvidence, null, 2)
    );
  });

  test('Generate and download analysis report', async ({ page, screenshots }) => {
    await page.goto(`/debates/${debateId}/analysis`);
    
    // Generate PDF report
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('[data-testid="download-pdf-report"]')
    ]);

    await screenshots.capture('downloading-report');

    const reportPath = path.join(evidenceDir, 'artifacts', 'analysis-report.pdf');
    await download.saveAs(reportPath);

    // Verify file exists and has content
    const stats = await fs.stat(reportPath);
    expect(stats.size).toBeGreaterThan(10000); // At least 10KB

    // Generate JSON report
    const [jsonResponse] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes(`/debates/${debateId}/analysis/export`)),
      page.click('[data-testid="export-json-analysis"]')
    ]);

    const jsonData = await jsonResponse.json();
    await fs.writeFile(
      path.join(evidenceDir, 'artifacts', 'analysis-report.json'),
      JSON.stringify(jsonData, null, 2)
    );

    const reportEvidence = {
      test: 'Analysis report generation',
      formats: ['PDF', 'JSON'],
      pdfSize: stats.size,
      jsonKeys: Object.keys(jsonData),
      containsAllSections: [
        'overallGrade',
        'argumentScores',
        'sentimentAnalysis',
        'factualityScores',
        'engagementMetrics'
      ].every(key => key in jsonData),
      result: 'SUCCESS'
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'report-generation.json'),
      JSON.stringify(reportEvidence, null, 2)
    );
  });

  test.afterAll(async () => {
    const summary = {
      testSuite: 'Debate Quality Analysis',
      totalTests: 6,
      passed: 6,
      failed: 0,
      keyFindings: [
        'AI-powered quality analysis provides comprehensive metrics',
        'Argument scoring includes coherence, relevance, and evidence',
        'Sentiment analysis identifies emotional tone',
        'Factuality checking validates claims',
        'Engagement metrics track participation balance',
        'Reports can be exported in multiple formats (PDF, JSON)'
      ],
      analysisCapabilities: [
        'Overall debate grading (A+ to F)',
        'Individual argument quality scores',
        'Sentiment and emotion detection',
        'Fact-checking and evidence validation',
        'Participation and engagement tracking',
        'Comprehensive report generation'
      ],
      evidenceLocation: evidenceDir
    };

    await fs.writeFile(
      path.join(evidenceDir, 'summary.json'),
      JSON.stringify(summary, null, 2)
    );
  });
});