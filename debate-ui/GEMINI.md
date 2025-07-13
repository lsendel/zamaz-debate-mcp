# GEMINI.md - Debate UI Documentation

This document provides a concise overview of the Debate UI project, focusing on key architectural and functional aspects.

## Core Functionality

- **Debate Management**: Create, view, and manage debates.
- **Real-time Updates**: WebSocket integration for live updates on debate progress and status.
- **Multi-tenancy**: Support for multiple organizations with data isolation.
- **LLM Integration**: Connect to various LLM providers (Claude, Gemini, etc.) and test connectivity.
- **Templates**: Use pre-configured templates to quickly start new debates.

## UI Structure

The UI is divided into the following main sections:

- **Header**: Contains the logo, organization switcher, LLM test button, and "New Debate" button.
- **Main Content Area**: A tabbed interface for:
    - **Debates**: Lists all debates.
    - **Templates**: Manages debate templates.
    - **Settings**: Configuration options.
- **Dialogs**:
    - **Create Debate**: A multi-step form for creating a new debate.
    - **LLM Test**: A dialog to test LLM connectivity.

## Key Technical Details

- **State Management**: A combination of global state (current organization, user preferences) and local component state.
- **Styling**: Uses a dark mode theme with a blue-to-purple gradient for primary actions.
- **Responsiveness**: The UI is designed to be responsive across mobile, tablet, and desktop devices.
- **Accessibility**: ARIA labels and keyboard navigation are implemented.

## Development and Testing

- **Testing**: The project includes unit, integration, and end-to-end tests. Visual regression testing is also a priority.
- **Debugging**: The browser's developer tools are the primary debugging tool. The console can be used to check WebSocket status and application state.
- **Dependencies**: The project uses a standard Node.js and React stack. Key dependencies are listed in `package.json`.
