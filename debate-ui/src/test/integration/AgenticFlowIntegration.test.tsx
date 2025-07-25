import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import DebateDetailPage from '../../components/DebateDetailPage';
import authSlice from '../../store/slices/authSlice';
import debateSlice from '../../store/slices/debateSlice';
import organizationSlice from '../../store/slices/organizationSlice';
import uiSlice from '../../store/slices/uiSlice';

// Mock all API clients
vi.mock('../../api/debateClient', () => ({
  default: {
    getDebate: vi.fn(),
    createAgenticFlow: vi.fn(),
    updateAgenticFlow: vi.fn(),
    listAgenticFlows: vi.fn(),
    executeAgenticFlow: vi.fn(),
    getFlowResults: vi.fn(),
  },
}));

vi.mock('../../api/organizationClient', () => ({
  default: {
    getOrganization: vi.fn(),
  },
}));

import debateClient from '../../api/debateClient';
import organizationClient from '../../api/organizationClient';

// Mock data
const mockDebate = {
  id: 'debate-123',
  topic: 'AI Safety and Ethics',
  status: 'IN_PROGRESS',
  currentRound: 2,
  totalRounds: 5,
  participants: [
    {
      id: 'p1',
      name: 'AI Assistant 1',
      type: 'AI',
      modelProvider: 'OpenAI',
      modelName: 'gpt-4',
    },
    {
      id: 'p2',
      name: 'AI Assistant 2',
      type: 'AI',
      modelProvider: 'Claude',
      modelName: 'claude-3',
    },
  ],
  messages: [
    {
      id: 'm1',
      participantId: 'p1',
      content: 'AI safety is paramount...',
      round: 1,
      timestamp: new Date().toISOString(),
    },
    {
      id: 'm2',
      participantId: 'p2',
      content: 'I agree, but we must also consider...',
      round: 1,
      timestamp: new Date().toISOString(),
    },
  ],
};

const mockFlows = [
  {
    id: 'flow-1',
    name: 'Deep Reasoning Flow',
    flowType: 'TREE_OF_THOUGHTS',
    description: 'Advanced reasoning for complex topics',
    configuration: { maxDepth: 3, branchingFactor: 3 },
    status: 'ACTIVE',
    participantIds: ['p1'],
  },
  {
    id: 'flow-2',
    name: 'Fact Checker',
    flowType: 'TOOL_CALLING_VERIFICATION',
    description: 'Verify claims with external tools',
    configuration: { allowedTools: ['web_search', 'calculator'] },
    status: 'ACTIVE',
    participantIds: ['p2'],
  },
];

const mockFlowResults = {
  m1: {
    flowId: 'flow-1',
    flowType: 'TREE_OF_THOUGHTS',
    finalAnswer: 'AI safety is paramount...',
    thoughtTree: {
      root: 'Consider AI safety',
      branches: [
        { thought: 'Existential risk', score: 0.9 },
        { thought: 'Alignment problem', score: 0.85 },
        { thought: 'Control problem', score: 0.8 },
      ],
    },
    confidence: 88.0,
    executionTime: 3500,
  },
  m2: {
    flowId: 'flow-2',
    flowType: 'TOOL_CALLING_VERIFICATION',
    finalAnswer: 'I agree, but we must also consider...',
    toolCalls: [
      {
        tool: 'web_search',
        input: 'recent AI safety research',
        output: 'Found 15 relevant papers...',
      },
    ],
    confidence: 92.0,
    executionTime: 2800,
  },
};

// Helper to create test store
function createTestStore(preloadedState = {}) {
  return configureStore({
    reducer: {
      auth: authSlice,
      debate: debateSlice,
      organization: organizationSlice,
      ui: uiSlice,
    },
    preloadedState: {
      auth: {
        isAuthenticated: true,
        user: { id: 'user-1', username: 'testuser' },
        ...preloadedState.auth,
      },
      organization: {
        currentOrganization: { id: 'org-1', name: 'Test Org' },
        ...preloadedState.organization,
      },
      ...preloadedState,
    },
  });
}

// Helper to render with providers
function renderWithProviders(component: React.ReactElement, store = createTestStore()) {
  return {
    ...render(
      <Provider store={store}>
        <BrowserRouter>{component}</BrowserRouter>
      </Provider>,
    ),
    store,
  };
}

describe('Agentic Flow Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (debateClient.getDebate as any).mockResolvedValue(mockDebate);
    (debateClient.listAgenticFlows as any).mockResolvedValue(mockFlows);
    (debateClient.getFlowResults as any).mockResolvedValue(mockFlowResults);
    (organizationClient.getOrganization as any).mockResolvedValue({
      id: 'org-1',
      name: 'Test Org',
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should load and display agentic flows in debate detail page', async () => {
    renderWithProviders(<DebateDetailPage />);

    // Wait for debate and flows to load
    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Verify flows are loaded
    expect(debateClient.listAgenticFlows).toHaveBeenCalledWith('debate-123');

    // Check if flow indicators are shown for participants
    expect(screen.getByText(/Tree of Thoughts/)).toBeInTheDocument();
    expect(screen.getByText(/Tool-Calling Verification/)).toBeInTheDocument();
  });

  it('should display flow results for messages', async () => {
    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI safety is paramount...')).toBeInTheDocument();
    });

    // Verify flow results are fetched
    expect(debateClient.getFlowResults).toHaveBeenCalledWith('debate-123');

    // Check confidence indicators
    expect(screen.getByText(/88\.0%/)).toBeInTheDocument();
    expect(screen.getByText(/92\.0%/)).toBeInTheDocument();

    // Click to view flow details
    const viewDetailsButtons = screen.getAllByRole('button', { name: /view flow details/i });
    fireEvent.click(viewDetailsButtons[0]);

    // Should show thought tree
    await waitFor(() => {
      expect(screen.getByText('Existential risk')).toBeInTheDocument();
      expect(screen.getByText('Alignment problem')).toBeInTheDocument();
    });
  });

  it('should allow configuring flows during debate', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Open flow configuration
    const configButton = screen.getByRole('button', { name: /configure flows/i });
    await user.click(configButton);

    // Should show flow configuration modal
    expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    expect(screen.getByText('Deep Reasoning Flow')).toBeInTheDocument();
    expect(screen.getByText('Fact Checker')).toBeInTheDocument();

    // Create new flow
    const createButton = screen.getByRole('button', { name: /create flow/i });
    await user.click(createButton);

    // Fill form
    const nameInput = screen.getByLabelText(/flow name/i);
    await user.type(nameInput, 'New Analysis Flow');

    const typeSelect = screen.getByLabelText(/flow type/i);
    await user.click(typeSelect);
    const selfCritiqueOption = await screen.findByText('Self-Critique Loop');
    await user.click(selfCritiqueOption);

    // Save
    const saveButton = screen.getByRole('button', { name: /save/i });
    await user.click(saveButton);

    // Verify API call
    await waitFor(() => {
      expect(debateClient.createAgenticFlow).toHaveBeenCalledWith({
        debateId: 'debate-123',
        name: 'New Analysis Flow',
        flowType: 'SELF_CRITIQUE_LOOP',
        configuration: expect.any(Object),
      });
    });
  });

  it('should handle real-time flow execution updates', async () => {
    const mockNewMessage = {
      id: 'm3',
      participantId: 'p1',
      content: 'New response with flow processing...',
      round: 2,
      timestamp: new Date().toISOString(),
    };

    const mockNewFlowResult = {
      flowId: 'flow-1',
      flowType: 'TREE_OF_THOUGHTS',
      status: 'PROCESSING',
      confidence: null,
      executionTime: null,
    };

    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Simulate new message with processing flow
    (debateClient.getDebate as any).mockResolvedValue({
      ...mockDebate,
      messages: [...mockDebate.messages, mockNewMessage],
    });

    (debateClient.getFlowResults as any).mockResolvedValue({
      ...mockFlowResults,
      m3: mockNewFlowResult,
    });

    // Trigger refresh
    const refreshButton = screen.getByRole('button', { name: /refresh/i });
    fireEvent.click(refreshButton);

    // Should show processing indicator
    await waitFor(() => {
      expect(screen.getByText(/processing/i)).toBeInTheDocument();
    });

    // Update with completed result
    const completedFlowResult = {
      ...mockNewFlowResult,
      status: 'SUCCESS',
      finalAnswer: 'New response with flow processing...',
      confidence: 90.0,
      executionTime: 3000,
    };

    (debateClient.getFlowResults as any).mockResolvedValue({
      ...mockFlowResults,
      m3: completedFlowResult,
    });

    // Should update to show completed result
    await waitFor(() => {
      expect(screen.queryByText(/processing/i)).not.toBeInTheDocument();
      expect(screen.getByText(/90\.0%/)).toBeInTheDocument();
    });
  });

  it('should handle flow execution errors gracefully', async () => {
    const errorFlowResult = {
      flowId: 'flow-1',
      flowType: 'TREE_OF_THOUGHTS',
      status: 'FAILED',
      error: 'LLM service unavailable',
      confidence: 0,
    };

    (debateClient.getFlowResults as any).mockResolvedValue({
      m1: errorFlowResult,
    });

    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Should show error state
    expect(screen.getByText(/failed/i)).toBeInTheDocument();
    expect(screen.getByText(/LLM service unavailable/)).toBeInTheDocument();

    // Should allow retry
    const retryButton = screen.getByRole('button', { name: /retry/i });
    expect(retryButton).toBeInTheDocument();
  });

  it('should support flow analytics view', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Open analytics
    const analyticsButton = screen.getByRole('button', { name: /flow analytics/i });
    await user.click(analyticsButton);

    // Should show analytics dashboard
    expect(screen.getByText('Agentic Flow Analytics')).toBeInTheDocument();
    expect(screen.getByText('Debate: AI Safety and Ethics')).toBeInTheDocument();

    // Should show flow performance metrics
    expect(screen.getByText('Average Confidence')).toBeInTheDocument();
    expect(screen.getByText('Average Execution Time')).toBeInTheDocument();
    expect(screen.getByText('Flow Type Distribution')).toBeInTheDocument();
  });

  it('should allow flow assignment changes during debate', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Open flow configuration
    const configButton = screen.getByRole('button', { name: /configure flows/i });
    await user.click(configButton);

    // Change assignment for existing flow
    const assignButtons = screen.getAllByRole('button', { name: /assign/i });
    await user.click(assignButtons[0]);

    // Should show participant checkboxes
    expect(screen.getByRole('checkbox', { name: /AI Assistant 1/i })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: /AI Assistant 2/i })).not.toBeChecked();

    // Change assignment
    const participant2Checkbox = screen.getByRole('checkbox', { name: /AI Assistant 2/i });
    await user.click(participant2Checkbox);

    // Save
    const saveAssignmentButton = screen.getByRole('button', { name: /save assignment/i });
    await user.click(saveAssignmentButton);

    // Verify API call
    await waitFor(() => {
      expect(debateClient.updateAgenticFlow).toHaveBeenCalledWith(
        'flow-1',
        expect.objectContaining({
          participantIds: ['p1', 'p2'],
        }),
      );
    });
  });

  it('should integrate flow recommendations based on debate context', async () => {
    const mockRecommendations = [
      {
        flowType: 'MULTI_AGENT_RED_TEAM',
        score: 0.95,
        reason: 'High-stakes ethical debate benefits from multiple perspectives',
      },
      {
        flowType: 'CONSTITUTIONAL_PROMPTING',
        score: 0.88,
        reason: 'Ensures responses align with safety principles',
      },
    ];

    (debateClient.getFlowRecommendations as any) = vi.fn().mockResolvedValue(mockRecommendations);

    renderWithProviders(<DebateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('AI Safety and Ethics')).toBeInTheDocument();
    });

    // Should show recommendations
    const recommendationsButton = screen.getByRole('button', { name: /view recommendations/i });
    fireEvent.click(recommendationsButton);

    await waitFor(() => {
      expect(screen.getByText('Recommended Agentic Flows')).toBeInTheDocument();
      expect(screen.getByText('Multi-Agent Red-Team')).toBeInTheDocument();
      expect(screen.getByText(/multiple perspectives/)).toBeInTheDocument();
    });

    // Apply recommendation
    const applyButton = screen.getByRole('button', { name: /apply recommendation/i });
    fireEvent.click(applyButton);

    // Should create flow based on recommendation
    await waitFor(() => {
      expect(debateClient.createAgenticFlow).toHaveBeenCalledWith(
        expect.objectContaining({
          flowType: 'MULTI_AGENT_RED_TEAM',
        }),
      );
    });
  });
});
