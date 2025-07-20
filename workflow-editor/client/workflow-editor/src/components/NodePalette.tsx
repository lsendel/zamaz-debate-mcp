import React from 'react';
import { motion } from 'framer-motion';

interface NodeType {
  type: string;
  label: string;
  icon: string;
  color: string;
  description: string;
}

const nodeTypes: NodeType[] = [
  {
    type: 'start',
    label: 'Start',
    icon: '▶',
    color: '#4CAF50',
    description: 'Begin workflow execution'
  },
  {
    type: 'task',
    label: 'Task',
    icon: '⚡',
    color: '#2196F3',
    description: 'Execute an action or process'
  },
  {
    type: 'decision',
    label: 'Decision',
    icon: '❓',
    color: '#FF9800',
    description: 'Conditional branching'
  },
  {
    type: 'end',
    label: 'End',
    icon: '■',
    color: '#757575',
    description: 'Terminate workflow'
  }
];

const NodePalette: React.FC = () => {
  const onDragStart = (event: React.DragEvent, nodeType: string) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div
      style={{
        position: 'relative',
        width: '200px',
        background: '#f5f5f5',
        borderRight: '1px solid #e0e0e0',
        padding: '20px',
        overflowY: 'auto',
      }}
    >
      <h3 style={{ margin: '0 0 20px 0', fontSize: '18px', color: '#333' }}>
        Node Palette
      </h3>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {nodeTypes.map((nodeType) => (
          <motion.div
            key={nodeType.type}
            draggable
            onDragStart={(event) => onDragStart(event as any, nodeType.type)}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            style={{
              background: 'white',
              border: `2px solid ${nodeType.color}`,
              borderRadius: '8px',
              padding: '12px',
              cursor: 'move',
              userSelect: 'none',
              boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
              <span style={{ fontSize: '20px', marginRight: '8px' }}>{nodeType.icon}</span>
              <span style={{ fontWeight: 'bold', color: nodeType.color }}>
                {nodeType.label}
              </span>
            </div>
            <div style={{ fontSize: '12px', color: '#666' }}>
              {nodeType.description}
            </div>
          </motion.div>
        ))}
      </div>
      
      <div style={{ marginTop: '30px', padding: '15px', background: '#e8f5e9', borderRadius: '8px' }}>
        <h4 style={{ margin: '0 0 10px 0', fontSize: '14px', color: '#2e7d32' }}>
          Quick Tips
        </h4>
        <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '12px', color: '#555' }}>
          <li>Drag nodes to canvas</li>
          <li>Connect nodes by dragging handles</li>
          <li>Click nodes to edit properties</li>
          <li>Delete with Del key</li>
        </ul>
      </div>
    </div>
  );
};

export default NodePalette;