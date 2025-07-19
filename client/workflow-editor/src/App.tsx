import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import WorkflowEditor from './components/WorkflowEditor';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="App">
        <h1>Workflow Editor</h1>
        <WorkflowEditor />
      </div>
    </QueryClientProvider>
  );
}

export default App;