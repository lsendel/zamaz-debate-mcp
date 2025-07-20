import React, { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { motion } from 'framer-motion';

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

  const navigationItems = [
    { id: 'workflow-editor', label: '🔀 Workflow Editor', component: WorkflowEditor },
    { id: 'telemetry-dashboard', label: '📊 Telemetry Dashboard', component: TelemetryDashboard },
    { id: 'telemetry-map', label: '🗺️ Telemetry Map', component: TelemetryMap },
    { id: 'spatial-query', label: '🔍 Spatial Query', component: SpatialQueryBuilder },
    { id: 'stamford-sample', label: '🏢 Stamford Sample', component: StamfordGeospatialSample },
    { id: 'debate-sample', label: '💬 Debate Tree', component: DebateTreeMapSample },
    { id: 'decision-sample', label: '🌳 Decision Tree', component: DecisionTreeSample },
    { id: 'ai-document-sample', label: '📄 AI Document Analysis', component: AIDocumentAnalysisSample },
  ];

  const CurrentComponent = navigationItems.find(item => item.id === currentPage)?.component || WorkflowEditor;

  return (
    <QueryClientProvider client={queryClient}>
      <div className="app">
        <header className="app-header">
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="header-content"
          >
            <h1>🔄 Kiro Workflow Editor</h1>
            <p>Complete workflow automation with real-time telemetry and spatial visualization</p>
          </motion.div>
        </header>

        <nav className="app-navigation">
          <div className="nav-container">
            {navigationItems.map((item) => (
              <motion.button
                key={item.id}
                className={`nav-item ${currentPage === item.id ? 'active' : ''}`}
                onClick={() => setCurrentPage(item.id)}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                {item.label}
              </motion.button>
            ))}
          </div>
        </nav>

        <main className="app-main">
          <motion.div
            key={currentPage}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
            className="page-content"
          >
            <CurrentComponent />
          </motion.div>
        </main>

        <footer className="app-footer">
          <div className="footer-content">
            <p>
              Built with React, TypeScript, React-Flow, MapLibre GL JS, Spring Boot, GraphQL, 
              Neo4j, InfluxDB, PostGIS, and Kubernetes
            </p>
            <div className="footer-stats">
              <span>🏗️ Architecture: Hexagonal</span>
              <span>📡 Telemetry: 10Hz Real-time</span>
              <span>🗺️ Maps: OpenStreetMap</span>
              <span>🤖 AI: Integrated</span>
            </div>
          </div>
        </footer>

        <style>{`
          .app {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          }

          .app-header {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            padding: 30px 20px;
            text-align: center;
            box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
          }

          .header-content h1 {
            margin: 0 0 10px 0;
            font-size: 2.5rem;
            color: #333;
            font-weight: 700;
          }

          .header-content p {
            margin: 0;
            color: #666;
            font-size: 1.1rem;
          }

          .app-navigation {
            background: rgba(255, 255, 255, 0.9);
            backdrop-filter: blur(10px);
            padding: 15px 20px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            overflow-x: auto;
          }

          .nav-container {
            display: flex;
            gap: 10px;
            max-width: 1400px;
            margin: 0 auto;
            padding: 0 20px;
          }

          .nav-item {
            padding: 12px 20px;
            border: none;
            background: white;
            border-radius: 25px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 500;
            color: #555;
            transition: all 0.3s ease;
            white-space: nowrap;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          }

          .nav-item:hover {
            background: #f0f8ff;
            color: #2196F3;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(33, 150, 243, 0.2);
          }

          .nav-item.active {
            background: linear-gradient(135deg, #667eea, #764ba2);
            color: white;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
          }

          .app-main {
            flex: 1;
            padding: 20px;
            max-width: 1400px;
            margin: 0 auto;
            width: 100%;
          }

          .page-content {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 12px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
            min-height: 600px;
            overflow: hidden;
          }

          .app-footer {
            background: rgba(0, 0, 0, 0.8);
            color: white;
            padding: 20px;
            text-align: center;
          }

          .footer-content {
            max-width: 1400px;
            margin: 0 auto;
          }

          .footer-content p {
            margin: 0 0 15px 0;
            font-size: 14px;
            opacity: 0.9;
          }

          .footer-stats {
            display: flex;
            justify-content: center;
            gap: 30px;
            flex-wrap: wrap;
          }

          .footer-stats span {
            font-size: 13px;
            padding: 5px 15px;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 15px;
            backdrop-filter: blur(5px);
          }

          @media (max-width: 768px) {
            .header-content h1 {
              font-size: 2rem;
            }

            .nav-container {
              justify-content: flex-start;
              padding: 0 10px;
            }

            .nav-item {
              padding: 10px 16px;
              font-size: 13px;
            }

            .footer-stats {
              gap: 15px;
            }

            .footer-stats span {
              font-size: 12px;
              padding: 4px 12px;
            }
          }
        `}</style>
      </div>
    </QueryClientProvider>
  );
}

export default App;
