import React, { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { Button, Card, CardContent } from '@zamaz/ui';
import {
  Workflow,
  Activity,
  Map,
  Search,
  Building2,
  MessageSquare,
  GitBranch,
  FileText,
} from 'lucide-react';

// Main components
import WorkflowEditor from './components/WorkflowEditor';
import TelemetryDashboard from './components/TelemetryDashboard';
import TelemetryMap from './components/TelemetryMap';
import SpatialQueryBuilder from './components/SpatialQueryBuilder';

// Sample applications
import StamfordGeospatialSample from './samples/StamfordGeospatialSample';
import DebateTreeMapSample from './samples/DebateTreeMapSample';
import DecisionTreeSample from './samples/DecisionTreeSample';
import AIDocumentAnalysisSample from './samples/AIDocumentAnalysisSample';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

function App() {
  const [currentPage, setCurrentPage] = useState('workflow-editor');

  // Handler for spatial query creation
  const handleQueryCreate = (query: any) => {
    console.log('Spatial query created:', query);
    // In a real application, this would save the query to a backend or state management
  };

  const navigationItems = [
    { id: 'workflow-editor', label: 'Workflow Editor', icon: Workflow, component: WorkflowEditor },
    { id: 'telemetry-dashboard', label: 'Telemetry Dashboard', icon: Activity, component: TelemetryDashboard },
    { id: 'telemetry-map', label: 'Telemetry Map', icon: Map, component: TelemetryMap },
    { id: 'spatial-query', label: 'Spatial Query', icon: Search, component: null }, // Special case handled below
    { id: 'stamford-sample', label: 'Stamford Sample', icon: Building2, component: StamfordGeospatialSample },
    { id: 'debate-sample', label: 'Debate Tree', icon: MessageSquare, component: DebateTreeMapSample },
    { id: 'decision-sample', label: 'Decision Tree', icon: GitBranch, component: DecisionTreeSample },
    { id: 'ai-document-sample', label: 'AI Document Analysis', icon: FileText, component: AIDocumentAnalysisSample },
  ];

  const getCurrentComponent = () => {
    // Special handling for components that require props
    if (currentPage === 'spatial-query') {
      return <SpatialQueryBuilder onQueryCreate={handleQueryCreate} />;
    }
    
    const currentItem = navigationItems.find(item => item.id === currentPage);
    const Component = currentItem?.component;
    
    if (!Component) {
      return <WorkflowEditor />;
    }
    
    return <Component />;
  };

  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen flex flex-col bg-gradient-to-br from-primary-600 to-accent-700">
        {/* Header */}
        <header className="bg-white/95 backdrop-blur-lg shadow-lg">
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="max-w-7xl mx-auto px-4 py-6 text-center"
          >
            <h1 className="text-4xl font-bold text-gray-900 mb-2">
              🔄 Kiro Workflow Editor
            </h1>
            <p className="text-lg text-gray-600">
              Complete workflow automation with real-time telemetry and spatial visualization
            </p>
          </motion.div>
        </header>

        {/* Navigation */}
        <nav className="bg-white/90 backdrop-blur-md shadow-md sticky top-0 z-10">
          <div className="max-w-7xl mx-auto px-4 py-3 overflow-x-auto">
            <div className="flex gap-2">
              {navigationItems.map((item) => {
                const Icon = item.icon;
                return (
                  <motion.div
                    key={item.id}
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                  >
                    <Button
                      variant={currentPage === item.id ? 'primary' : 'secondary'}
                      onClick={() => setCurrentPage(item.id)}
                      leftIcon={<Icon className="h-4 w-4" />}
                      className="whitespace-nowrap"
                    >
                      {item.label}
                    </Button>
                  </motion.div>
                );
              })}
            </div>
          </div>
        </nav>

        {/* Main Content */}
        <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-6">
          <motion.div
            key={currentPage}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
          >
            <Card className="bg-white/95 backdrop-blur-sm shadow-xl">
              <CardContent className="p-0 min-h-[600px]">
                {getCurrentComponent()}
              </CardContent>
            </Card>
          </motion.div>
        </main>

        {/* Footer */}
        <footer className="bg-gray-900 text-white py-6 mt-8">
          <div className="max-w-7xl mx-auto px-4">
            <p className="text-center text-sm mb-4">
              Built with React, TypeScript, React-Flow, MapLibre GL JS, Spring Boot, GraphQL, 
              Neo4j, InfluxDB, PostGIS, and Kubernetes
            </p>
            <div className="flex flex-wrap justify-center gap-4 text-xs">
              <span className="px-3 py-1 bg-white/10 rounded-full backdrop-blur-sm">
                🏗️ Architecture: Hexagonal
              </span>
              <span className="px-3 py-1 bg-white/10 rounded-full backdrop-blur-sm">
                📡 Telemetry: 10Hz Real-time
              </span>
              <span className="px-3 py-1 bg-white/10 rounded-full backdrop-blur-sm">
                🗺️ Maps: OpenStreetMap
              </span>
              <span className="px-3 py-1 bg-white/10 rounded-full backdrop-blur-sm">
                🤖 AI: Integrated
              </span>
            </div>
          </div>
        </footer>
      </div>
    </QueryClientProvider>
  );
}

export default App;