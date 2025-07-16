# Debate UI

React TypeScript UI for the Zamaz Debate MCP System.

## Features

- **Multi-tenant Support**: Organization switcher and isolated data
- **Debate Management**: Create, view, and manage debates
- **Live Updates**: WebSocket connection for real-time debate progress
- **Analytics Dashboard**: View statistics and performance metrics
- **Settings Management**: Configure organization, users, and API keys
- **Material-UI**: Modern, responsive design

## Prerequisites

- Node.js 16+ and npm
- Backend services running (ports 5002, 5005, 5013)

## Installation

```bash
npm install
```

## Development

```bash
npm start
# or
npm run dev
```

The UI will be available at http://localhost:3000

## API Proxying

The UI proxies API requests to backend services:
- `/api/organization` → http://localhost:5005
- `/api/debate` → http://localhost:5013
- `/api/llm` → http://localhost:5002
- `/ws` → ws://localhost:5013 (WebSocket)

## Build

```bash
npm run build
```

## Testing

```bash
npm test
```

## Environment Variables

Create a `.env.local` file for custom configuration:

```env
REACT_APP_API_URL=http://localhost:3000
PORT=3000
```

## Project Structure

```
src/
├── api/              # API client modules
├── components/       # React components
├── store/           # Redux store and slices
├── types/           # TypeScript types
├── utils/           # Utility functions
├── App.tsx          # Main app component
├── index.tsx        # Entry point
└── setupProxy.js    # API proxy configuration
```

## Key Components

- **Layout**: Main app layout with sidebar navigation
- **OrganizationSwitcher**: Switch between organizations
- **DebatesPage**: List and manage debates
- **CreateDebateDialog**: Create new debates with participants
- **DebateDetailPage**: View debate progress with live updates
- **AnalyticsPage**: View statistics and metrics
- **SettingsPage**: Manage organization and users

## State Management

Uses Redux Toolkit with slices for:
- Authentication (`authSlice`)
- Organizations (`organizationSlice`)
- Debates (`debateSlice`)
- UI state (`uiSlice`)

## WebSocket Integration

The debate detail page connects to WebSocket for live updates:
- Debate started/completed events
- Round started/completed events
- New responses from participants

## Styling

- Material-UI v5 components
- Emotion for styled components
- Responsive design with Grid system
- Custom theme configuration