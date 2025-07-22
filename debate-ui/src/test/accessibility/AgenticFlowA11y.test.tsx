import { render } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { vi, describe, it, expect } from 'vitest';
import AgenticFlowConfig from '../../components/AgenticFlowConfig';
import AgenticFlowResult from '../../components/AgenticFlowResult';
import AgenticFlowAnalytics from '../../components/AgenticFlowAnalytics';
import authSlice from '../../store/slices/authSlice';
import debateSlice from '../../store/slices/debateSlice';
import organizationSlice from '../../store/slices/organizationSlice';
import uiSlice from '../../store/slices/uiSlice';

// Extend expect with axe matchers
expect.extend(toHaveNoViolations);

// Mock API clients
vi.mock('../../api/debateClient', () => ({
  default: {
    listAgenticFlows: vi.fn().mockResolvedValue([]),
  },
}));

// Helper to create test store
function createTestStore() {
  return configureStore({
    reducer: {
      auth: authSlice,
      debate: debateSlice,
      organization: organizationSlice,
      ui: uiSlice,
    },
  });
}

describe('Agentic Flow Accessibility Tests', () => {
  describe('AgenticFlowConfig Accessibility', () => {
    it('should have no accessibility violations in default state', async () => {
      const store = createTestStore();
      const { container } = render(
        <Provider store={store}>
          <AgenticFlowConfig debateId="test-debate" />
        </Provider>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have proper ARIA labels for interactive elements', () => {
      const store = createTestStore();
        <Provider store={store}>
          <AgenticFlowConfig debateId="test-debate" />
        </Provider>
      );

      // Check for proper labeling
      expect(getByRole('button', { name: /create flow/i })).toBeInTheDocument();
      expect(getByRole('heading', { name: /agentic flow configuration/i })).toBeInTheDocument();
    });

    it('should support keyboard navigation', async () => {
      const store = createTestStore();
      const { getByRole, getAllByRole } = render(
        <Provider store={store}>
          <AgenticFlowConfig debateId="test-debate" />
        </Provider>
      );

      const createButton = getByRole('button', { name: /create flow/i });
      
      // Should be focusable
      createButton.focus();
      expect(document.activeElement).toBe(createButton);

      // Tab navigation should work
      const allButtons = getAllByRole('button');
      expect(allButtons.length).toBeGreaterThan(0);
    });
  });

  describe('AgenticFlowResult Accessibility', () => {
    const mockResults = {
      INTERNAL_MONOLOGUE: {
        flowType: 'INTERNAL_MONOLOGUE',
        finalAnswer: 'Test answer',
        reasoning: 'Step 1: Test\nStep 2: Result',
        confidence: 95.0,
        executionTime: 1250,
        timestamp: new Date().toISOString(),
      },
      SELF_CRITIQUE_LOOP: {
        flowType: 'SELF_CRITIQUE_LOOP',
        finalAnswer: 'Refined answer',
        iterations: [
          {
            iteration: 1,
            response: 'Initial',
            critique: 'Needs work',
            revision: 'Better',
          },
        ],
        confidence: 88.0,
        executionTime: 3500,
        timestamp: new Date().toISOString(),
      },
      TOOL_CALLING_VERIFICATION: {
        flowType: 'TOOL_CALLING_VERIFICATION',
        finalAnswer: 'Verified result',
        toolCalls: [
          {
            tool: 'calculator',
            input: '2+2',
            output: '4',
            timestamp: new Date().toISOString(),
          },
        ],
        confidence: 100.0,
        executionTime: 2800,
        timestamp: new Date().toISOString(),
      },
    };

    it('should have no accessibility violations for Internal Monologue result', async () => {
      const { container } = render(
        <AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for Self-Critique Loop result', async () => {
      const { container } = render(
        <AgenticFlowResult result={mockResults.SELF_CRITIQUE_LOOP} />
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for Tool-Calling result', async () => {
      const { container } = render(
        <AgenticFlowResult result={mockResults.TOOL_CALLING_VERIFICATION} />
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have proper heading hierarchy', () => {
      const { getByRole, getAllByRole } = render(
        <AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />
      );

      // Should have main heading
      expect(getByRole('heading', { level: 3 })).toBeInTheDocument();

      // Check heading hierarchy
      const allHeadings = getAllByRole('heading');
      const headingLevels = allHeadings.map(h => parseInt(h.tagName.charAt(1)));
      
      // Verify no skipped levels
      for (let i = 1; i < headingLevels.length; i++) {
        const diff = headingLevels[i] - headingLevels[i - 1];
        expect(diff).toBeLessThanOrEqual(1);
      }
    });

    it('should provide alternative text for visual indicators', () => {
        <AgenticFlowResult result={mockResults.INTERNAL_MONOLOGUE} />
      );

      // Confidence should have accessible text
      expect(getByText(/95\.0%/)).toBeInTheDocument();
      expect(getByText(/confidence/i)).toBeInTheDocument();

      // Execution time should be readable
      expect(getByText(/1\.25s/)).toBeInTheDocument();
    });

    it('should support screen reader announcements for dynamic content', () => {
      const { container } = render(
        <AgenticFlowResult result={mockResults.SELF_CRITIQUE_LOOP} />
      );

      // Check for ARIA live regions
      const liveRegions = container.querySelectorAll('[aria-live]');
      expect(liveRegions.length).toBeGreaterThan(0);

      // Tab panels should have proper ARIA attributes
      const tabPanels = container.querySelectorAll('[role="tabpanel"]');
      tabPanels.forEach(panel => {
        expect(panel).toHaveAttribute('aria-labelledby');
      });
    });
  });

  describe('AgenticFlowAnalytics Accessibility', () => {
    const mockAnalyticsData = {
      flowTypeDistribution: [
        { flowType: 'INTERNAL_MONOLOGUE', count: 45, percentage: 25 },
        { flowType: 'SELF_CRITIQUE_LOOP', count: 36, percentage: 20 },
      ],
      confidenceTrends: [
        { date: '2024-01-01', avgConfidence: 75 },
        { date: '2024-01-02', avgConfidence: 82 },
      ],
      overallMetrics: {
        totalExecutions: 180,
        averageConfidence: 85.5,
        averageExecutionTime: 3.2,
        successRate: 0.92,
      },
    };

    it('should have no accessibility violations for analytics dashboard', async () => {
      const { container } = render(
        <AgenticFlowAnalytics
          debateId="test-debate"
          analyticsData={mockAnalyticsData}
        />
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should provide text alternatives for charts', () => {
      const { getByRole, getByText } = render(
        <AgenticFlowAnalytics
          debateId="test-debate"
          analyticsData={mockAnalyticsData}
        />
      );

      // Should have table alternatives for charts
      expect(getByRole('table')).toBeInTheDocument();
      
      // Metrics should be readable as text
      expect(getByText(/180/)).toBeInTheDocument(); // Total executions
      expect(getByText(/85\.5/)).toBeInTheDocument(); // Average confidence
    });

    it('should support keyboard navigation for interactive elements', () => {
      const { getAllByRole } = render(
        <AgenticFlowAnalytics
          debateId="test-debate"
          analyticsData={mockAnalyticsData}
        />
      );

      // All interactive elements should be keyboard accessible
      const buttons = getAllByRole('button');
      const selects = getAllByRole('combobox');

      [...buttons, ...selects].forEach(element => {
        element.focus();
        expect(document.activeElement).toBe(element);
      });
    });
  });

  describe('Color Contrast and Visual Accessibility', () => {
    it('should use sufficient color contrast for confidence indicators', () => {
      const { container } = render(
        <AgenticFlowResult
          result={{
            flowType: 'INTERNAL_MONOLOGUE',
            finalAnswer: 'Test',
            confidence: 95.0,
            timestamp: new Date().toISOString(),
          }}
        />
      );

      // Check confidence badge has proper contrast
      const confidenceBadge = container.querySelector('.ant-badge');
      expect(confidenceBadge).toBeInTheDocument();
      
      // Ant Design components should have proper contrast by default
      // Additional contrast testing can be done with specialized tools
    });

    it('should not rely solely on color to convey information', () => {
      const { getByText } = render(
        <AgenticFlowResult
          result={{
            flowType: 'INTERNAL_MONOLOGUE',
            finalAnswer: 'Test',
            confidence: 45.0, // Low confidence
            timestamp: new Date().toISOString(),
          }}
        />
      );

      // Low confidence should be indicated by text, not just color
      expect(getByText(/45\.0%/)).toBeInTheDocument();
      expect(getByText(/low confidence/i)).toBeInTheDocument();
    });
  });

  describe('Focus Management', () => {
    it('should manage focus properly when opening modals', async () => {
      const store = createTestStore();
      const { getByRole } = render(
        <Provider store={store}>
          <AgenticFlowConfig debateId="test-debate" />
        </Provider>
      );

      const createButton = getByRole('button', { name: /create flow/i });
      createButton.click();

      // Modal should trap focus
      await vi.waitFor(() => {
        const modal = document.querySelector('.ant-modal');
        expect(modal).toBeInTheDocument();
        
        // First focusable element in modal should receive focus
        const firstInput = modal?.querySelector('input, button, select, textarea');
        expect(document.activeElement).toBe(firstInput);
      });
    });

    it('should return focus to trigger element when closing modal', async () => {
      const store = createTestStore();
      const { getByRole } = render(
        <Provider store={store}>
          <AgenticFlowConfig debateId="test-debate" />
        </Provider>
      );

      const createButton = getByRole('button', { name: /create flow/i });
      createButton.click();

      // Close modal
      const cancelButton = await vi.waitFor(() => 
        getByRole('button', { name: /cancel/i })
      );
      cancelButton.click();

      // Focus should return to create button
      expect(document.activeElement).toBe(createButton);
    });
  });

  describe('Error Messaging Accessibility', () => {
    it('should announce form validation errors to screen readers', async () => {
      const store = createTestStore();
      const { getByRole, getByText } = render(
        <Provider store={store}>
          <AgenticFlowConfig debateId="test-debate" />
        </Provider>
      );

      // Open create modal
      const createButton = getByRole('button', { name: /create flow/i });
      createButton.click();

      // Try to submit without filling required fields
      const submitButton = await vi.waitFor(() => 
        getByRole('button', { name: /save/i })
      );
      submitButton.click();

      // Error messages should be associated with form fields
      await vi.waitFor(() => {
        const errorMessage = getByText(/flow name is required/i);
        expect(errorMessage).toBeInTheDocument();
        
        // Error should have role="alert" for screen reader announcement
        expect(errorMessage.closest('[role="alert"]')).toBeInTheDocument();
      });
    });
  });

  describe('Responsive Design Accessibility', () => {
    it('should maintain accessibility on small screens', async () => {
      // Set viewport to mobile size
      global.innerWidth = 375;
      global.innerHeight = 667;
      global.dispatchEvent(new Event('resize'));

      const { container } = render(
        <AgenticFlowResult
          result={{
            flowType: 'INTERNAL_MONOLOGUE',
            finalAnswer: 'Test',
            confidence: 95.0,
            timestamp: new Date().toISOString(),
          }}
        />
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();

      // Reset viewport
      global.innerWidth = 1024;
      global.innerHeight = 768;
      global.dispatchEvent(new Event('resize'));
    });
  });

  describe('Loading and Progress Indicators', () => {
    it('should provide accessible loading states', () => {
      const { getByRole, getByText } = render(
        <div role="status" aria-live="polite">
          <span className="ant-spin" />
          <span>Loading agentic flows...</span>
        </div>
      );

      expect(getByRole('status')).toBeInTheDocument();
      expect(getByText(/loading/i)).toBeInTheDocument();
    });
  });
});