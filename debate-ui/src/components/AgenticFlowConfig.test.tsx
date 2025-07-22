import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import AgenticFlowConfig from './AgenticFlowConfig';
import authSlice from '../store/slices/authSlice';
import debateSlice from '../store/slices/debateSlice';
import organizationSlice from '../store/slices/organizationSlice';
import uiSlice from '../store/slices/uiSlice';

// Mock API client
vi.mock('../api/debateClient', () => ({
  default: {
    createAgenticFlow: vi.fn(),
    updateAgenticFlow: vi.fn(),
    listAgenticFlows: vi.fn(),
    deleteAgenticFlow: vi.fn(),
  },
}));

import debateClient from '../api/debateClient';

// Helper function to create a test store
function createTestStore(preloadedState = {}) {
  return configureStore({
    reducer: {
      auth: authSlice,
      debate: debateSlice,
      organization: organizationSlice,
      ui: uiSlice,
    },
    preloadedState,
  });
}

// Helper function to render component with store
function renderWithStore(component: React.ReactElement, store = createTestStore()) {
  return {
    ...render(
      <Provider store={store}>
        {component}
      </Provider>
    ),
    store,
  };
}

describe('AgenticFlowConfig Component', () => {
  const mockDebateId = 'test-debate-123';
  const mockFlows = [
    {
      id: 'flow-1',
      name: 'Test Flow 1',
      flowType: 'INTERNAL_MONOLOGUE',
      description: 'Test description 1',
      configuration: { prefix: 'Think step by step' },
      status: 'ACTIVE',
    },
    {
      id: 'flow-2',
      name: 'Test Flow 2',
      flowType: 'SELF_CRITIQUE_LOOP',
      description: 'Test description 2',
      configuration: { maxIterations: 3 },
      status: 'ACTIVE',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    (debateClient.listAgenticFlows as any).mockResolvedValue(mockFlows);
  });

  it('should render flow configuration interface', async () => {
    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Should show loading initially
    expect(screen.getByText(/loading/i)).toBeInTheDocument();

    // Wait for flows to load
    await waitFor(() => {
      expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    });

    // Should display existing flows
    expect(screen.getByText('Test Flow 1')).toBeInTheDocument();
    expect(screen.getByText('Test Flow 2')).toBeInTheDocument();
  });

  it('should handle creating a new flow', async () => {
    const user = userEvent.setup();
    const newFlow = {
      id: 'flow-3',
      name: 'New Test Flow',
      flowType: 'CONFIDENCE_SCORING',
      description: 'New flow description',
      configuration: { threshold: 0.8 },
      status: 'ACTIVE',
    };

    (debateClient.createAgenticFlow as any).mockResolvedValue(newFlow);

    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    });

    // Click create button
    const createButton = screen.getByRole('button', { name: /create flow/i });
    await user.click(createButton);

    // Fill in form
    const nameInput = screen.getByLabelText(/flow name/i);
    await user.type(nameInput, 'New Test Flow');

    const descriptionInput = screen.getByLabelText(/description/i);
    await user.type(descriptionInput, 'New flow description');

    // Select flow type
    const flowTypeSelect = screen.getByLabelText(/flow type/i);
    await user.click(flowTypeSelect);
    const confidenceOption = await screen.findByText('Confidence Scoring');
    await user.click(confidenceOption);

    // Configure threshold
    const thresholdInput = screen.getByLabelText(/confidence threshold/i);
    await user.clear(thresholdInput);
    await user.type(thresholdInput, '0.8');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /save/i });
    await user.click(submitButton);

    // Verify API call
    await waitFor(() => {
      expect(debateClient.createAgenticFlow).toHaveBeenCalledWith({
        debateId: mockDebateId,
        name: 'New Test Flow',
        flowType: 'CONFIDENCE_SCORING',
        description: 'New flow description',
        configuration: { threshold: 0.8 },
      });
    });
  });

  it('should handle editing an existing flow', async () => {
    const user = userEvent.setup();
    const updatedFlow = {
      ...mockFlows[0],
      configuration: { prefix: 'Updated prefix' },
    };

    (debateClient.updateAgenticFlow as any).mockResolvedValue(updatedFlow);

    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for flows to load
    await waitFor(() => {
      expect(screen.getByText('Test Flow 1')).toBeInTheDocument();
    });

    // Click edit button for first flow
    const editButtons = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editButtons[0]);

    // Update configuration
    const prefixInput = screen.getByLabelText(/reasoning prefix/i);
    await user.clear(prefixInput);
    await user.type(prefixInput, 'Updated prefix');

    // Save changes
    const saveButton = screen.getByRole('button', { name: /save/i });
    await user.click(saveButton);

    // Verify API call
    await waitFor(() => {
      expect(debateClient.updateAgenticFlow).toHaveBeenCalledWith(
        'flow-1',
        expect.objectContaining({
          configuration: { prefix: 'Updated prefix' },
        })
      );
    });
  });

  it('should handle deleting a flow', async () => {
    const user = userEvent.setup();
    (debateClient.deleteAgenticFlow as any).mockResolvedValue(undefined);

    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for flows to load
    await waitFor(() => {
      expect(screen.getByText('Test Flow 1')).toBeInTheDocument();
    });

    // Click delete button for first flow
    const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
    await user.click(deleteButtons[0]);

    // Confirm deletion
    const confirmButton = await screen.findByRole('button', { name: /yes/i });
    await user.click(confirmButton);

    // Verify API call
    await waitFor(() => {
      expect(debateClient.deleteAgenticFlow).toHaveBeenCalledWith('flow-1');
    });
  });

  it('should display flow type specific configuration fields', async () => {
    const user = userEvent.setup();
    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    });

    // Click create button
    const createButton = screen.getByRole('button', { name: /create flow/i });
    await user.click(createButton);

    // Test Internal Monologue configuration
    const flowTypeSelect = screen.getByLabelText(/flow type/i);
    await user.click(flowTypeSelect);
    const internalOption = await screen.findByText('Internal Monologue');
    await user.click(internalOption);

    expect(screen.getByLabelText(/reasoning prefix/i)).toBeInTheDocument();

    // Switch to Self-Critique Loop
    await user.click(flowTypeSelect);
    const critiqueOption = await screen.findByText('Self-Critique Loop');
    await user.click(critiqueOption);

    expect(screen.getByLabelText(/max iterations/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/critique prompt/i)).toBeInTheDocument();

    // Switch to Tool-Calling Verification
    await user.click(flowTypeSelect);
    const toolOption = await screen.findByText('Tool-Calling Verification');
    await user.click(toolOption);

    expect(screen.getByLabelText(/allowed tools/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/verification prompt/i)).toBeInTheDocument();
  });

  it('should validate required fields', async () => {
    const user = userEvent.setup();
    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    });

    // Click create button
    const createButton = screen.getByRole('button', { name: /create flow/i });
    await user.click(createButton);

    // Try to submit without filling required fields
    const submitButton = screen.getByRole('button', { name: /save/i });
    await user.click(submitButton);

    // Should show validation errors
    await waitFor(() => {
      expect(screen.getByText(/flow name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/flow type is required/i)).toBeInTheDocument();
    });
  });

  it('should handle API errors gracefully', async () => {
    const user = userEvent.setup();
    const errorMessage = 'Failed to create flow';
    (debateClient.createAgenticFlow as any).mockRejectedValue(new Error(errorMessage));

    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    });

    // Create a flow
    const createButton = screen.getByRole('button', { name: /create flow/i });
    await user.click(createButton);

    // Fill minimal required fields
    const nameInput = screen.getByLabelText(/flow name/i);
    await user.type(nameInput, 'Test Flow');

    const flowTypeSelect = screen.getByLabelText(/flow type/i);
    await user.click(flowTypeSelect);
    const option = await screen.findByText('Internal Monologue');
    await user.click(option);

    // Submit
    const submitButton = screen.getByRole('button', { name: /save/i });
    await user.click(submitButton);

    // Should show error message
    await waitFor(() => {
      expect(screen.getByText(new RegExp(errorMessage, 'i'))).toBeInTheDocument();
    });
  });

  it('should handle participant-specific flow assignment', async () => {
    const user = userEvent.setup();
    const mockParticipants = [
      { id: 'p1', name: 'Participant 1' },
      { id: 'p2', name: 'Participant 2' },
    ];

    const store = createTestStore({
      debate: {
        currentDebate: {
          id: mockDebateId,
          participants: mockParticipants,
        },
      },
    });

    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />, store);

    // Wait for flows to load
    await waitFor(() => {
      expect(screen.getByText('Test Flow 1')).toBeInTheDocument();
    });

    // Click on assignment button for first flow
    const assignButtons = screen.getAllByRole('button', { name: /assign/i });
    await user.click(assignButtons[0]);

    // Should show participant selection
    expect(screen.getByText('Participant 1')).toBeInTheDocument();
    expect(screen.getByText('Participant 2')).toBeInTheDocument();

    // Assign to participant
    const participant1Checkbox = screen.getByRole('checkbox', { name: /participant 1/i });
    await user.click(participant1Checkbox);

    // Save assignment
    const saveAssignmentButton = screen.getByRole('button', { name: /save assignment/i });
    await user.click(saveAssignmentButton);

    // Verify assignment was saved
    await waitFor(() => {
      expect(debateClient.updateAgenticFlow).toHaveBeenCalledWith(
        'flow-1',
        expect.objectContaining({
          participantIds: ['p1'],
        })
      );
    });
  });

  it('should support flow templates', async () => {
    const user = userEvent.setup();
      {
        name: 'Fact-Checking Template',
        flowType: 'TOOL_CALLING_VERIFICATION',
        configuration: {
          allowedTools: ['web_search', 'calculator'],
          verificationPrompt: 'Verify all facts',
        },
      },
      {
        name: 'Deep Reasoning Template',
        flowType: 'TREE_OF_THOUGHTS',
        configuration: {
          branchingFactor: 3,
          maxDepth: 3,
        },
      },
    ];

    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Agentic Flow Configuration')).toBeInTheDocument();
    });

    // Click create from template
    const templateButton = screen.getByRole('button', { name: /create from template/i });
    await user.click(templateButton);

    // Should show template options
    expect(screen.getByText('Fact-Checking Template')).toBeInTheDocument();
    expect(screen.getByText('Deep Reasoning Template')).toBeInTheDocument();

    // Select a template
    const factCheckTemplate = screen.getByText('Fact-Checking Template');
    await user.click(factCheckTemplate);

    // Should populate form with template values
    await waitFor(() => {
      expect(screen.getByDisplayValue('TOOL_CALLING_VERIFICATION')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Verify all facts')).toBeInTheDocument();
    });
  });

  it('should handle flow activation/deactivation', async () => {
    const user = userEvent.setup();
    renderWithStore(<AgenticFlowConfig debateId={mockDebateId} />);

    // Wait for flows to load
    await waitFor(() => {
      expect(screen.getByText('Test Flow 1')).toBeInTheDocument();
    });

    // Find toggle switch for first flow
    const toggleSwitches = screen.getAllByRole('switch');
    expect(toggleSwitches[0]).toBeChecked(); // Should be active initially

    // Toggle off
    await user.click(toggleSwitches[0]);

    // Verify API call to deactivate
    await waitFor(() => {
      expect(debateClient.updateAgenticFlow).toHaveBeenCalledWith(
        'flow-1',
        expect.objectContaining({
          status: 'INACTIVE',
        })
      );
    });
  });
});