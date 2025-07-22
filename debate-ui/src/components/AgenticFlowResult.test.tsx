import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import AgenticFlowResult from './AgenticFlowResult';

describe('AgenticFlowResult Component', () => {
  const mockResults = {
    INTERNAL_MONOLOGUE: {
      flowType: 'INTERNAL_MONOLOGUE',
      finalAnswer: 'The capital of France is Paris.',
      reasoning: 'Step 1: Recall geography knowledge\nStep 2: France is a country in Europe\nStep 3: Its capital city is Paris',
      confidence: 95.0,
      executionTime: 1250,
      timestamp: new Date().toISOString(),
    },
    SELF_CRITIQUE_LOOP: {
      flowType: 'SELF_CRITIQUE_LOOP',
      finalAnswer: 'Climate change is primarily caused by human activities.',
      iterations: [
        {
          iteration: 1,
          response: 'Climate change is caused by various factors.',
          critique: 'This response is too vague and doesn\'t address the primary cause.',
          revision: 'Climate change is primarily caused by greenhouse gas emissions.',
        },
        {
          iteration: 2,
          response: 'Climate change is primarily caused by greenhouse gas emissions.',
          critique: 'Better, but should specify the source of these emissions.',
          revision: 'Climate change is primarily caused by human activities.',
        },
      ],
      confidence: 88.0,
      executionTime: 3500,
      timestamp: new Date().toISOString(),
    },
    MULTI_AGENT_RED_TEAM: {
      flowType: 'MULTI_AGENT_RED_TEAM',
      finalAnswer: 'The proposed solution is feasible with modifications.',
      perspectives: {
        architect: {
          role: 'Architect',
          perspective: 'The solution is well-designed and scalable.',
          confidence: 85,
        },
        skeptic: {
          role: 'Skeptic',
          perspective: 'There are potential security vulnerabilities that need addressing.',
          confidence: 70,
        },
        judge: {
          role: 'Judge',
          perspective: 'Overall feasible, but security concerns must be resolved.',
          confidence: 80,
        },
      },
      confidence: 78.0,
      executionTime: 4200,
      timestamp: new Date().toISOString(),
    },
    TOOL_CALLING_VERIFICATION: {
      flowType: 'TOOL_CALLING_VERIFICATION',
      finalAnswer: 'The calculation result is 42.',
      toolCalls: [
        {
          tool: 'calculator',
          input: '6 * 7',
          output: '42',
          timestamp: new Date().toISOString(),
        },
        {
          tool: 'web_search',
          input: 'meaning of life universe everything',
          output: 'Douglas Adams reference: 42',
          timestamp: new Date().toISOString(),
        },
      ],
      confidence: 100.0,
      executionTime: 2800,
      timestamp: new Date().toISOString(),
    },
    RAG_WITH_RERANKING: {
      flowType: 'RAG_WITH_RERANKING',
      finalAnswer: 'Based on the documents, the answer is...',
      retrievedDocuments: [
        { id: 'doc1', title: 'Document 1', relevance: 0.95, selected: true },
        { id: 'doc2', title: 'Document 2', relevance: 0.88, selected: true },
        { id: 'doc3', title: 'Document 3', relevance: 0.65, selected: false },
        { id: 'doc4', title: 'Document 4', relevance: 0.45, selected: false },
      ],
      confidence: 82.0,
      executionTime: 3200,
      timestamp: new Date().toISOString(),
    },
  };

  it('should render Internal Monologue flow results', () => {
    render(<AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />);

    expect(screen.getByText('Internal Monologue')).toBeInTheDocument();
    expect(screen.getByText('The capital of France is Paris.')).toBeInTheDocument();
    expect(screen.getByText(/95\.0%/)).toBeInTheDocument();
    expect(screen.getByText(/1\.25s/)).toBeInTheDocument();

    // Check reasoning tab
    const reasoningTab = screen.getByRole('tab', { name: /reasoning process/i });
    fireEvent.click(reasoningTab);
    expect(screen.getByText(/Step 1: Recall geography knowledge/)).toBeInTheDocument();
  });

  it('should render Self-Critique Loop flow results', () => {
    render(<AgenticFlowResult result={mockResults.SELF_CRITIQUE_LOOP} />);

    expect(screen.getByText('Self-Critique Loop')).toBeInTheDocument();
    expect(screen.getByText(/88\.0%/)).toBeInTheDocument();

    // Check iterations
    expect(screen.getByText('Iteration 1')).toBeInTheDocument();
    expect(screen.getByText('Iteration 2')).toBeInTheDocument();

    // Expand iteration details
    const iteration1Button = screen.getByRole('button', { name: /iteration 1/i });
    fireEvent.click(iteration1Button);

    expect(screen.getByText(/This response is too vague/)).toBeInTheDocument();
  });

  it('should render Multi-Agent Red-Team flow results', () => {
    render(<AgenticFlowResult result={mockResults.MULTI_AGENT_RED_TEAM} />);

    expect(screen.getByText('Multi-Agent Red-Team')).toBeInTheDocument();
    expect(screen.getByText(/78\.0%/)).toBeInTheDocument();

    // Check perspectives
    expect(screen.getByText('Architect')).toBeInTheDocument();
    expect(screen.getByText('Skeptic')).toBeInTheDocument();
    expect(screen.getByText('Judge')).toBeInTheDocument();

    // Check perspective content
    expect(screen.getByText(/well-designed and scalable/)).toBeInTheDocument();
    expect(screen.getByText(/security vulnerabilities/)).toBeInTheDocument();
  });

  it('should render Tool-Calling Verification flow results', () => {
    render(<AgenticFlowResult result={mockResults.TOOL_CALLING_VERIFICATION} />);

    expect(screen.getByText('Tool-Calling Verification')).toBeInTheDocument();
    expect(screen.getByText(/100\.0%/)).toBeInTheDocument();

    // Check tool calls
    expect(screen.getByText('Tool Calls')).toBeInTheDocument();
    expect(screen.getByText('calculator')).toBeInTheDocument();
    expect(screen.getByText('web_search')).toBeInTheDocument();

    // Check tool call details
    expect(screen.getByText('6 * 7')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('should render RAG with Re-ranking flow results', () => {
    render(<AgenticFlowResult result={mockResults.RAG_WITH_RERANKING} />);

    expect(screen.getByText('RAG with Re-ranking')).toBeInTheDocument();
    expect(screen.getByText(/82\.0%/)).toBeInTheDocument();

    // Check documents
    expect(screen.getByText('Retrieved Documents')).toBeInTheDocument();
    expect(screen.getByText('Document 1')).toBeInTheDocument();
    expect(screen.getByText('Document 2')).toBeInTheDocument();

    // Check relevance scores
    expect(screen.getByText(/95%/)).toBeInTheDocument();
    expect(screen.getByText(/88%/)).toBeInTheDocument();

    // Selected documents should be highlighted
    const selectedDocs = screen.getAllByTestId('selected-document');
    expect(selectedDocs).toHaveLength(2);
  });

  it('should handle tab navigation', async () => {
    const user = userEvent.setup();
    render(<AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />);

    // Initially on final answer tab
    expect(screen.getByText('The capital of France is Paris.')).toBeInTheDocument();

    // Click on reasoning tab
    const reasoningTab = screen.getByRole('tab', { name: /reasoning process/i });
    await user.click(reasoningTab);

    expect(screen.getByText(/Step 1: Recall geography knowledge/)).toBeInTheDocument();

    // Click on metadata tab
    const metadataTab = screen.getByRole('tab', { name: /metadata/i });
    await user.click(metadataTab);

    expect(screen.getByText(/execution time/i)).toBeInTheDocument();
    expect(screen.getByText(/timestamp/i)).toBeInTheDocument();
  });

  it('should display confidence indicators with appropriate colors', () => {
    // High confidence (green)
    const { rerender } = render(<AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />);
    let confidenceBadge = screen.getByText(/95\.0%/);
    expect(confidenceBadge.closest('.ant-badge')).toHaveClass('ant-badge-status-success');

    // Medium confidence (yellow)
    rerender(<AgenticFlowResult result={mockResults.MULTI_AGENT_RED_TEAM} />);
    confidenceBadge = screen.getByText(/78\.0%/);
    expect(confidenceBadge.closest('.ant-badge')).toHaveClass('ant-badge-status-warning');

    // Low confidence (red) - create a low confidence result
    const lowConfidenceResult = {
      ...mockResults.INTERNAL_MONOLOGUE,
      confidence: 45.0,
    };
    rerender(<AgenticFlowResult result={lowConfidenceResult} />);
    confidenceBadge = screen.getByText(/45\.0%/);
    expect(confidenceBadge.closest('.ant-badge')).toHaveClass('ant-badge-status-error');
  });

  it('should handle missing optional data gracefully', () => {
    const minimalResult = {
      flowType: 'CONFIDENCE_SCORING',
      finalAnswer: 'Simple answer',
      confidence: 75.0,
      timestamp: new Date().toISOString(),
    };

    render(<AgenticFlowResult result={minimalResult} />);

    expect(screen.getByText('Confidence Scoring')).toBeInTheDocument();
    expect(screen.getByText('Simple answer')).toBeInTheDocument();
    expect(screen.getByText(/75\.0%/)).toBeInTheDocument();

    // Should not crash when optional data is missing
    expect(screen.queryByText(/execution time/i)).toBeInTheDocument();
    expect(screen.getByText(/n\/a/i)).toBeInTheDocument();
  });

  it('should expand/collapse detailed information', async () => {
    const user = userEvent.setup();
    render(<AgenticFlowResult result={mockResults.SELF_CRITIQUE_LOOP} />);

    // Initially collapsed
    const iteration1Details = screen.queryByText(/This response is too vague/);
    expect(iteration1Details).not.toBeInTheDocument();

    // Click to expand
    const expandButton = screen.getByRole('button', { name: /iteration 1/i });
    await user.click(expandButton);

    // Now visible
    expect(screen.getByText(/This response is too vague/)).toBeInTheDocument();

    // Click to collapse
    await user.click(expandButton);

    // Hidden again
    expect(screen.queryByText(/This response is too vague/)).not.toBeInTheDocument();
  });

  it('should render flow-specific visualizations correctly', () => {
    // Tree of Thoughts specific
    const treeResult = {
      flowType: 'TREE_OF_THOUGHTS',
      finalAnswer: 'Best path found',
      thoughtTree: {
        root: 'Initial thought',
        branches: [
          { thought: 'Branch 1', score: 0.8 },
          { thought: 'Branch 2', score: 0.9 },
        ],
      },
      confidence: 85.0,
      timestamp: new Date().toISOString(),
    };

    render(<AgenticFlowResult result={treeResult} />);
    expect(screen.getByText('Tree of Thoughts')).toBeInTheDocument();
    expect(screen.getByText('Thought Tree')).toBeInTheDocument();
    expect(screen.getByText('Branch 1')).toBeInTheDocument();
    expect(screen.getByText('Branch 2')).toBeInTheDocument();
  });

  it('should handle error states in results', () => {
    const errorResult = {
      flowType: 'INTERNAL_MONOLOGUE',
      error: 'Failed to generate response',
      status: 'FAILED',
      confidence: 0,
      timestamp: new Date().toISOString(),
    };

    render(<AgenticFlowResult result={errorResult} />);

    expect(screen.getByText('Internal Monologue')).toBeInTheDocument();
    expect(screen.getByText(/failed/i)).toBeInTheDocument();
    expect(screen.getByText(/Failed to generate response/)).toBeInTheDocument();
  });

  it('should support copy-to-clipboard functionality', async () => {
    const user = userEvent.setup();
    const mockClipboard = {
      writeText: vi.fn().mockResolvedValue(undefined),
    };
    Object.assign(navigator, { clipboard: mockClipboard });

    render(<AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />);

    // Find and click copy button
    const copyButton = screen.getByRole('button', { name: /copy/i });
    await user.click(copyButton);

    // Verify clipboard was called
    expect(mockClipboard.writeText).toHaveBeenCalledWith(
      expect.stringContaining('The capital of France is Paris.')
    );

    // Should show success message
    expect(screen.getByText(/copied/i)).toBeInTheDocument();
  });

  it('should render execution metrics appropriately', () => {
    render(<AgenticFlowResult result={mockResults.TOOL_CALLING_VERIFICATION} />);

    // Click on metadata tab
    const metadataTab = screen.getByRole('tab', { name: /metadata/i });
    fireEvent.click(metadataTab);

    // Should show execution time
    expect(screen.getByText(/2\.8s/)).toBeInTheDocument();

    // Should show timestamp
    const timestamp = new Date(mockResults.TOOL_CALLING_VERIFICATION.timestamp);
    const formattedTime = timestamp.toLocaleTimeString();
    expect(screen.getByText(new RegExp(formattedTime.split(':')[0]))).toBeInTheDocument();
  });

  it('should handle ensemble voting visualization', () => {
    const ensembleResult = {
      flowType: 'ENSEMBLE_VOTING',
      finalAnswer: 'Majority vote result',
      votes: [
        { response: 'Option A', count: 3 },
        { response: 'Option B', count: 2 },
        { response: 'Option C', count: 1 },
      ],
      confidence: 60.0,
      timestamp: new Date().toISOString(),
    };

    render(<AgenticFlowResult result={ensembleResult} />);

    expect(screen.getByText('Ensemble Voting')).toBeInTheDocument();
    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('3 votes')).toBeInTheDocument();
    expect(screen.getByText(/majority/i)).toBeInTheDocument();
  });
});