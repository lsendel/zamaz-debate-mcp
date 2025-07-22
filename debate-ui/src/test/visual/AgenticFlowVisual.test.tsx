import { render } from '@testing-library/react';
import { vi, describe, it, expect, beforeAll } from 'vitest';
import { toMatchImageSnapshot } from 'jest-image-snapshot';
import AgenticFlowResult from '../../components/AgenticFlowResult';
import AgenticFlowAnalytics from '../../components/AgenticFlowAnalytics';

// Add image snapshot matcher
expect.extend({ toMatchImageSnapshot });

// Mock canvas for charts
beforeAll(() => {
  HTMLCanvasElement.prototype.getContext = vi.fn(() => ({
    fillRect: vi.fn(),
    clearRect: vi.fn(),
    getImageData: vi.fn(() => ({
      data: new Array(4),
    })),
    putImageData: vi.fn(),
    createImageData: vi.fn(() => []),
    setTransform: vi.fn(),
    drawImage: vi.fn(),
    save: vi.fn(),
    fillText: vi.fn(),
    restore: vi.fn(),
    beginPath: vi.fn(),
    moveTo: vi.fn(),
    lineTo: vi.fn(),
    closePath: vi.fn(),
    stroke: vi.fn(),
    translate: vi.fn(),
    scale: vi.fn(),
    rotate: vi.fn(),
    arc: vi.fn(),
    fill: vi.fn(),
    measureText: vi.fn(() => ({ width: 0 })),
    transform: vi.fn(),
    rect: vi.fn(),
    clip: vi.fn(),
  }));
});

describe('Agentic Flow Visual Regression Tests', () => {
  describe('Flow Result Visualizations', () => {
    it('should render Internal Monologue visualization correctly', async () => {
      const result = {
        flowType: 'INTERNAL_MONOLOGUE',
        finalAnswer: 'The answer is 42.',
        reasoning: 'Step 1: Consider the question\nStep 2: Apply logic\nStep 3: Reach conclusion',
        confidence: 95.0,
        executionTime: 1250,
        timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      
      // Wait for any animations
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'internal-monologue-result',
        failureThreshold: 0.01,
      });
    });

    it('should render Self-Critique Loop visualization correctly', async () => {
      const result = {
        flowType: 'SELF_CRITIQUE_LOOP',
        finalAnswer: 'Refined answer after critique',
        iterations: [
          {
            iteration: 1,
            response: 'Initial response',
            critique: 'Needs more detail',
            revision: 'More detailed response',
          },
          {
            iteration: 2,
            response: 'More detailed response',
            critique: 'Good but can be clearer',
            revision: 'Refined answer after critique',
          },
        ],
        confidence: 88.0,
        executionTime: 3500,
        timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'self-critique-loop-result',
        failureThreshold: 0.01,
      });
    });

    it('should render Multi-Agent Red-Team visualization correctly', async () => {
      const result = {
        flowType: 'MULTI_AGENT_RED_TEAM',
        finalAnswer: 'Consensus reached',
        perspectives: {
          architect: {
            role: 'Architect',
            perspective: 'Well-designed solution',
            confidence: 85,
          },
          skeptic: {
            role: 'Skeptic',
            perspective: 'Potential issues identified',
            confidence: 70,
          },
          judge: {
            role: 'Judge',
            perspective: 'Balanced assessment',
            confidence: 80,
          },
        },
        confidence: 78.0,
        executionTime: 4200,
        timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'multi-agent-result',
        failureThreshold: 0.01,
      });
    });

    it('should render Tool-Calling visualization correctly', async () => {
      const result = {
        flowType: 'TOOL_CALLING_VERIFICATION',
        finalAnswer: 'Verified result: 42',
        toolCalls: [
          {
            tool: 'calculator',
            input: '6 * 7',
            output: '42',
            timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
          },
          {
            tool: 'web_search',
            input: 'verify calculation',
            output: 'Confirmed correct',
            timestamp: new Date('2024-01-01T12:00:05Z').toISOString(),
          },
        ],
        confidence: 100.0,
        executionTime: 2800,
        timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'tool-calling-result',
        failureThreshold: 0.01,
      });
    });

    it('should render RAG with Re-ranking visualization correctly', async () => {
      const result = {
        flowType: 'RAG_WITH_RERANKING',
        finalAnswer: 'Answer based on documents',
        retrievedDocuments: [
          { id: 'doc1', title: 'Document 1', relevance: 0.95, selected: true },
          { id: 'doc2', title: 'Document 2', relevance: 0.88, selected: true },
          { id: 'doc3', title: 'Document 3', relevance: 0.65, selected: false },
          { id: 'doc4', title: 'Document 4', relevance: 0.45, selected: false },
          { id: 'doc5', title: 'Document 5', relevance: 0.30, selected: false },
        ],
        confidence: 82.0,
        executionTime: 3200,
        timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'rag-reranking-result',
        failureThreshold: 0.01,
      });
    });

    it('should render Tree of Thoughts visualization correctly', async () => {
      const result = {
        flowType: 'TREE_OF_THOUGHTS',
        finalAnswer: 'Best path conclusion',
        thoughtTree: {
          root: 'Initial problem',
          branches: [
            {
              thought: 'Approach A',
              score: 0.8,
              children: [
                { thought: 'Sub-approach A1', score: 0.85 },
                { thought: 'Sub-approach A2', score: 0.75 },
              ],
            },
            {
              thought: 'Approach B',
              score: 0.9,
              children: [
                { thought: 'Sub-approach B1', score: 0.95 },
                { thought: 'Sub-approach B2', score: 0.88 },
              ],
            },
          ],
        },
        confidence: 92.0,
        executionTime: 5500,
        timestamp: new Date('2024-01-01T12:00:00Z').toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'tree-of-thoughts-result',
        failureThreshold: 0.01,
      });
    });
  });

  describe('Analytics Visualizations', () => {
    it('should render flow analytics dashboard correctly', async () => {
      const analyticsData = {
        flowTypeDistribution: [
          { flowType: 'INTERNAL_MONOLOGUE', count: 45, percentage: 25 },
          { flowType: 'SELF_CRITIQUE_LOOP', count: 36, percentage: 20 },
          { flowType: 'TOOL_CALLING_VERIFICATION', count: 27, percentage: 15 },
          { flowType: 'MULTI_AGENT_RED_TEAM', count: 27, percentage: 15 },
          { flowType: 'RAG_WITH_RERANKING', count: 18, percentage: 10 },
          { flowType: 'OTHER', count: 27, percentage: 15 },
        ],
        confidenceTrends: [
          { date: '2024-01-01', avgConfidence: 75 },
          { date: '2024-01-02', avgConfidence: 78 },
          { date: '2024-01-03', avgConfidence: 82 },
          { date: '2024-01-04', avgConfidence: 85 },
          { date: '2024-01-05', avgConfidence: 88 },
        ],
        executionTimeByType: [
          { flowType: 'INTERNAL_MONOLOGUE', avgTime: 1.2 },
          { flowType: 'SELF_CRITIQUE_LOOP', avgTime: 3.5 },
          { flowType: 'TOOL_CALLING_VERIFICATION', avgTime: 2.8 },
          { flowType: 'MULTI_AGENT_RED_TEAM', avgTime: 4.2 },
          { flowType: 'TREE_OF_THOUGHTS', avgTime: 5.5 },
        ],
        overallMetrics: {
          totalExecutions: 180,
          averageConfidence: 85.5,
          averageExecutionTime: 3.2,
          successRate: 0.92,
        },
      };

      const { container } = render(
        <AgenticFlowAnalytics
          debateId="test-debate"
          analyticsData={analyticsData}
        />
      );
      
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'analytics-dashboard',
        failureThreshold: 0.01,
      });
    });

    it('should render flow comparison chart correctly', async () => {
      const comparisonData = {
        flows: [
          {
            name: 'Internal Monologue',
            confidence: 85,
            executionTime: 1.2,
            successRate: 0.95,
          },
          {
            name: 'Self-Critique Loop',
            confidence: 88,
            executionTime: 3.5,
            successRate: 0.92,
          },
          {
            name: 'Multi-Agent Red-Team',
            confidence: 82,
            executionTime: 4.2,
            successRate: 0.88,
          },
        ],
      };

      const { container } = render(
        <AgenticFlowAnalytics
          debateId="test-debate"
          comparisonData={comparisonData}
          view="comparison"
        />
      );
      
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'flow-comparison-chart',
        failureThreshold: 0.01,
      });
    });
  });

  describe('Confidence Indicator States', () => {
    it('should render high confidence indicator correctly', async () => {
      const result = {
        flowType: 'INTERNAL_MONOLOGUE',
        finalAnswer: 'High confidence answer',
        confidence: 95.0,
        timestamp: new Date().toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      const confidenceElement = container.querySelector('.confidence-indicator');
      
      expect(confidenceElement).toMatchImageSnapshot({
        customSnapshotIdentifier: 'high-confidence-indicator',
        failureThreshold: 0.01,
      });
    });

    it('should render medium confidence indicator correctly', async () => {
      const result = {
        flowType: 'INTERNAL_MONOLOGUE',
        finalAnswer: 'Medium confidence answer',
        confidence: 75.0,
        timestamp: new Date().toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      const confidenceElement = container.querySelector('.confidence-indicator');
      
      expect(confidenceElement).toMatchImageSnapshot({
        customSnapshotIdentifier: 'medium-confidence-indicator',
        failureThreshold: 0.01,
      });
    });

    it('should render low confidence indicator correctly', async () => {
      const result = {
        flowType: 'INTERNAL_MONOLOGUE',
        finalAnswer: 'Low confidence answer',
        confidence: 45.0,
        timestamp: new Date().toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={result} />);
      const confidenceElement = container.querySelector('.confidence-indicator');
      
      expect(confidenceElement).toMatchImageSnapshot({
        customSnapshotIdentifier: 'low-confidence-indicator',
        failureThreshold: 0.01,
      });
    });
  });

  describe('Error States', () => {
    it('should render error state visualization correctly', async () => {
      const errorResult = {
        flowType: 'INTERNAL_MONOLOGUE',
        status: 'FAILED',
        error: 'LLM service timeout',
        confidence: 0,
        timestamp: new Date().toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={errorResult} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'error-state-result',
        failureThreshold: 0.01,
      });
    });

    it('should render processing state visualization correctly', async () => {
      const processingResult = {
        flowType: 'TREE_OF_THOUGHTS',
        status: 'PROCESSING',
        confidence: null,
        timestamp: new Date().toISOString(),
      };

      const { container } = render(<AgenticFlowResult result={processingResult} />);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      expect(container.firstChild).toMatchImageSnapshot({
        customSnapshotIdentifier: 'processing-state-result',
        failureThreshold: 0.01,
      });
    });
  });
});