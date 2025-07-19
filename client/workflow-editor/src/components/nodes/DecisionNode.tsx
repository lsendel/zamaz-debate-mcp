import React from 'react';
import { Handle, Position } from 'react-flow-renderer';

const DecisionNode: React.FC<any> = ({ data }) => {
  return (
    <div style={{ 
      padding: '10px', 
      border: '2px solid #FF9800', 
      borderRadius: '0',
      background: '#FFF3E0',
      minWidth: '80px',
      textAlign: 'center',
      transform: 'rotate(45deg)'
    }}>
      <div style={{ transform: 'rotate(-45deg)' }}>Decision</div>
      <Handle type="target" position={Position.Left} />
      <Handle type="source" position={Position.Top} id="true" />
      <Handle type="source" position={Position.Bottom} id="false" />
    </div>
  );
};

export default DecisionNode;