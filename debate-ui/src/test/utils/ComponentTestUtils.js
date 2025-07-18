/**
 * Component testing utilities for React Testing Library.
 * Provides helpers for common testing patterns and component interactions.
 */

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

// Default test theme
const testTheme = createTheme({
  palette: {
    mode: 'light',
  },
});

/**
 * Custom render function that includes providers
 */
export function renderWithProviders(
  ui,
  {
    preloadedState = {},
    store = configureStore({
      reducer: {
        // Add your reducers here for testing
        auth: (state = { user: null, token: null }) => state,
        debates: (state = { debates: [], loading: false }) => state,
        organizations: (state = { current: null, list: [] }) => state,
      },
      preloadedState,
    }),
    theme = testTheme,
    routerProps = {},
    ...renderOptions
  } = {}
) {
  function Wrapper({ children }) {
    return (
      <Provider store={store}>
        <BrowserRouter {...routerProps}>
          <ThemeProvider theme={theme}>
            <CssBaseline />
            {children}
          </ThemeProvider>
        </BrowserRouter>
      </Provider>
    );
  }
  
  return { store, ...render(ui, { wrapper: Wrapper, ...renderOptions }) };
}

/**
 * Creates a mock user for testing
 */
export function createMockUser(overrides = {}) {
  return {
    id: 'test-user-1',
    email: 'test@example.com',
    name: 'Test User',
    organizationId: 'test-org-1',
    roles: ['USER'],
    permissions: ['DEBATE_VIEW', 'DEBATE_CREATE'],
    ...overrides,
  };
}

/**
 * Creates a mock organization for testing
 */
export function createMockOrganization(overrides = {}) {
  return {
    id: 'test-org-1',
    name: 'Test Organization',
    displayName: 'Test Org',
    status: 'ACTIVE',
    subscription: {
      tier: 'PRO',
      maxUsers: 50,
      features: ['advanced_debates', 'analytics'],
    },
    ...overrides,
  };
}

/**
 * Creates a mock debate for testing
 */
export function createMockDebate(overrides = {}) {
  return {
    id: 'test-debate-1',
    title: 'Test Debate',
    description: 'A test debate for testing purposes',
    status: 'CREATED',
    organizationId: 'test-org-1',
    createdBy: 'test-user-1',
    participants: [
      {
        id: 'participant-1',
        name: 'Claude',
        provider: 'anthropic',
        model: 'claude-3-sonnet',
        position: 'pro',
      },
      {
        id: 'participant-2',
        name: 'GPT-4',
        provider: 'openai',
        model: 'gpt-4',
        position: 'con',
      },
    ],
    rounds: 3,
    currentRound: 0,
    turns: [],
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

/**
 * Helper to fill form fields
 */
export async function fillForm(fields) {
  const user = userEvent.setup();
  
  for (const [fieldName, value] of Object.entries(fields)) {
    const field = screen.getByLabelText(new RegExp(fieldName, 'i'));
    await user.clear(field);
    await user.type(field, value);
  }
}

/**
 * Helper to select from Material-UI Select components
 */
export async function selectFromMuiSelect(selectLabel, optionText) {
  const user = userEvent.setup();
  
  // Find and click the select
  const select = screen.getByLabelText(selectLabel);
  await user.click(select);
  
  // Wait for the options to appear and select the desired option
  await waitFor(() => {
    const option = screen.getByRole('option', { name: optionText });
    return user.click(option);
  });
}

/**
 * Helper to interact with Material-UI Autocomplete components
 */
export async function selectFromAutocomplete(label, optionText) {
  const user = userEvent.setup();
  
  const autocomplete = screen.getByLabelText(label);
  await user.click(autocomplete);
  await user.type(autocomplete, optionText);
  
  await waitFor(() => {
    const option = screen.getByRole('option', { name: optionText });
    return user.click(option);
  });
}

/**
 * Helper to wait for loading states to complete
 */
export async function waitForLoadingToFinish() {
  await waitFor(() => {
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
  });
}

/**
 * Helper to simulate API responses
 */
export function mockApiResponse(url, response, options = {}) {
  const { method = 'GET', status = 200, delay = 0 } = options;
  
  const createDelayedResponse = () => {
    if (delay > 0) {
      return new Promise(resolve => setTimeout(() => resolve(response), delay));
    }
    return Promise.resolve(response);
  };

  const createTextResponse = () => Promise.resolve(JSON.stringify(response));

  const createMockResponse = () => ({
    ok: status >= 200 && status < 300,
    status,
    json: createDelayedResponse,
    text: createTextResponse,
  });

  global.fetch = jest.fn().mockImplementation((requestUrl, requestOptions = {}) => {
    if (requestUrl.includes(url) && (requestOptions.method || 'GET') === method) {
      return Promise.resolve(createMockResponse());
    }
    return Promise.reject(new Error('Unhandled request'));
  });
}

/**
 * Helper to mock multiple API endpoints
 */
export function mockApiEndpoints(endpoints) {
  global.fetch = jest.fn().mockImplementation((url, options = {}) => {
    const method = options.method || 'GET';
    
    for (const endpoint of endpoints) {
      if (url.includes(endpoint.url) && method === endpoint.method) {
        const response = typeof endpoint.response === 'function' 
          ? endpoint.response(url, options)
          : endpoint.response;
          
        return Promise.resolve({
          ok: endpoint.status >= 200 && endpoint.status < 300,
          status: endpoint.status || 200,
          json: () => Promise.resolve(response),
          text: () => Promise.resolve(JSON.stringify(response)),
        });
      }
    }
    
    return Promise.reject(new Error(`Unhandled request: ${method} ${url}`));
  });
}

/**
 * Helper to test form validation
 */
export async function testFormValidation(formSubmitButton, expectedErrors) {
  const user = userEvent.setup();
  
  // Try to submit the form without filling required fields
  await user.click(formSubmitButton);
  
  // Check for validation errors
  for (const error of expectedErrors) {
    await waitFor(() => {
      expect(screen.getByText(error)).toBeInTheDocument();
    });
  }
}

/**
 * Helper to test keyboard navigation
 */
export async function testKeyboardNavigation(element, key, expectedResult) {
  const user = userEvent.setup();
  
  element.focus();
  await user.keyboard(key);
  
  if (typeof expectedResult === 'function') {
    expectedResult();
  } else {
    await waitFor(() => expectedResult);
  }
}

/**
 * Helper to test accessibility
 */
export function testAccessibility(component) {
  // Check for proper ARIA labels
  expect(component).toHaveAttribute('aria-label');
  
  // Check for keyboard accessibility
  expect(component).toHaveAttribute('tabIndex');
  
  // Check for semantic HTML
  const role = component.getAttribute('role');
  if (role) {
    expect(['button', 'link', 'textbox', 'combobox', 'listbox']).toContain(role);
  }
}

/**
 * Helper to simulate drag and drop
 */
export async function simulateDragAndDrop(sourceElement, targetElement) {
  // Note: userEvent not needed for this drag/drop implementation
  
  // Start drag
  fireEvent.dragStart(sourceElement, {
    dataTransfer: {
      effectAllowed: 'move',
      setData: jest.fn(),
      getData: jest.fn(),
    },
  });
  
  // Drag over target
  fireEvent.dragOver(targetElement);
  
  // Drop on target
  fireEvent.drop(targetElement, {
    dataTransfer: {
      getData: jest.fn(),
    },
  });
}

/**
 * Helper to test component rendering with different props
 */
export function testComponentWithProps(Component, propsVariations) {
  return propsVariations.map((props, index) => {
    const testName = props.testName || `variation ${index + 1}`;
    
    return {
      name: testName,
      test: () => {
        const { container } = renderWithProviders(<Component {...props} />);
        return container;
      },
    };
  });
}

/**
 * Helper to wait for animations to complete
 */
export async function waitForAnimation(duration = 300) {
  await new Promise(resolve => setTimeout(resolve, duration));
}

/**
 * Helper to test responsive behavior
 */
export function testResponsiveBehavior(Component, breakpoints) {
  return breakpoints.map(({ width, height, name }) => ({
    name: `renders correctly at ${name}`,
    test: () => {
      // Mock window dimensions
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: width,
      });
      Object.defineProperty(window, 'innerHeight', {
        writable: true,
        configurable: true,
        value: height,
      });
      
      // Trigger resize event
      fireEvent(window, new Event('resize'));
      
      const { container } = renderWithProviders(<Component />);
      return container;
    },
  }));
}

/**
 * Helper to test error boundaries
 */
export function testErrorBoundary(ErrorBoundary, ComponentThatThrows) {
  // Suppress console.error for this test
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  
  const { getByText } = renderWithProviders(
    <ErrorBoundary>
      <ComponentThatThrows />
    </ErrorBoundary>
  );
  
  expect(getByText(/something went wrong/i)).toBeInTheDocument();
  
  consoleSpy.mockRestore();
}

/**
 * Helper to create mock Redux store with specific state
 */
export function createMockStore(initialState = {}) {
  return configureStore({
    reducer: {
      auth: (state = { user: null, token: null, loading: false }) => state,
      debates: (state = { debates: [], loading: false, error: null }) => state,
      organizations: (state = { current: null, list: [], loading: false }) => state,
      ui: (state = { theme: 'light', sidebarOpen: false }) => state,
      ...initialState.reducers,
    },
    preloadedState: initialState.preloadedState || {},
  });
}

/**
 * Helper to test async operations
 */
export async function testAsyncOperation(triggerAction, expectedResult) {
  const user = userEvent.setup();
  
  // Trigger the async operation
  if (typeof triggerAction === 'function') {
    await triggerAction(user);
  } else {
    await user.click(triggerAction);
  }
  
  // Wait for the expected result
  await waitFor(() => expectedResult());
}

/**
 * Helper for testing debounced inputs
 */
export async function testDebouncedInput(input, value, expectedAction, delay = 300) {
  const user = userEvent.setup();
  
  await user.type(input, value);
  
  // Wait for debounce delay
  await waitFor(() => expectedAction(), { timeout: delay + 100 });
}

/**
 * Helper to test component cleanup
 */
export function testComponentCleanup(Component, setupProps = {}) {
  const { unmount } = renderWithProviders(<Component {...setupProps} />);
  
  // Add any setup that creates side effects (timers, subscriptions, etc.)
  
  // Unmount the component
  unmount();
  
  // Verify cleanup (no memory leaks, cleared timers, etc.)
  // This is framework-specific and should be implemented based on your needs
}

/**
 * Common test data generators
 */
export const testData = {
  users: (count = 5) => Array.from({ length: count }, (_, i) => createMockUser({
    id: `user-${i + 1}`,
    email: `user${i + 1}@example.com`,
    name: `User ${i + 1}`,
  })),
  
  organizations: (count = 3) => Array.from({ length: count }, (_, i) => createMockOrganization({
    id: `org-${i + 1}`,
    name: `Organization ${i + 1}`,
    displayName: `Org ${i + 1}`,
  })),
  
  debates: (count = 10) => Array.from({ length: count }, (_, i) => createMockDebate({
    id: `debate-${i + 1}`,
    title: `Debate ${i + 1}`,
    description: `Description for debate ${i + 1}`,
    status: i % 3 === 0 ? 'IN_PROGRESS' : i % 3 === 1 ? 'COMPLETED' : 'CREATED',
  })),
};

/**
 * Performance testing helper
 */
export function measureRenderTime(Component, props = {}) {
  const start = performance.now();
  renderWithProviders(<Component {...props} />);
  const end = performance.now();
  return end - start;
}

/**
 * Helper to test component with different user permissions
 */
export function testWithPermissions(Component, permissionSets) {
  return permissionSets.map(({ permissions, expectedBehavior, testName }) => ({
    name: testName || `with permissions: ${permissions.join(', ')}`,
    test: () => {
      const mockUser = createMockUser({ permissions });
      const mockStore = createMockStore({
        preloadedState: {
          auth: { user: mockUser, token: 'test-token' },
        },
      });
      
      const { container } = renderWithProviders(<Component />, { store: mockStore });
      
      if (expectedBehavior) {
        expectedBehavior(container);
      }
      
      return container;
    },
  }));
}