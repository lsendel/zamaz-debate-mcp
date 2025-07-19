import React from 'react'
import { WorkflowEditor } from './components/WorkflowEditor'
import { MapViewer } from './components/MapViewer'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Workflow Editor</h1>
      </header>
      <main className="app-main">
        <div className="editor-container">
          <WorkflowEditor />
        </div>
        <div className="map-container">
          <MapViewer />
        </div>
      </main>
    </div>
  )
}

export default App